# 4.15 — Design Ad Click Aggregation

> **Module 4 · Case Studies** · ~35 min read + coding assignment
> *Concepts exercised:* stream processing, tumbling windows, exactly-once via dedup, top-K,
> late events & watermarks, the lambda-vs-kappa & "fast-but-approximate vs slow-but-exact"
> trade-off.

---

## The problem

When someone clicks an ad, the platform must **count that click** — accurately, because clicks
are **money** (advertisers are billed per click, publishers are paid). At scale that's a torrent
of events: count **clicks per ad per minute**, in **near-real-time**, surface the **top-N ads**,
and — critically — **count each real click exactly once** even though the delivery system may hand
you the same event two or three times. Think Google Ads, Facebook Ads, any ad exchange.

Why it's hard:
- **Volume.** Millions of clicks per second across billions of ads.
- **Money on the line.** Over-count → overbill the advertiser (lawsuit). Under-count → underpay
  the publisher (fraud complaint). This is closer to **accounting** than to the "trends are fine"
  world of the metrics lesson.
- **Messy delivery.** Network retries, queue redelivery, and client bugs all produce
  **duplicates**. Events arrive **out of order** and sometimes **late**.

> **Analogy.** A turnstile at a stadium counting fans through a gate. You want an accurate count
> per gate per minute. But the turnstile sometimes double-clicks on one person, and some fans
> trickle in late through a side door after the official count "closed." You solve double-counting
> by stamping each fan's **ticket with a unique barcode** and refusing to count a barcode twice —
> that's exactly the **event-ID dedup** at the heart of this system.

---

## Step 1: Requirements (always start here)

**Functional**
- **Count clicks per ad per time window** (e.g. per minute).
- **Exactly-once counting**: a duplicate event (same event ID) is counted once.
- **Top-N ads** by click count within a window.
- Near-real-time: results visible within seconds, not hours.

**Non-functional**
- **Massive write throughput** (the ingestion firehose).
- **Correctness** — clicks are billed, so the count must be right (or auditable/correctable).
- **Handle late & out-of-order events** without silently dropping or double-counting them.
- **Fault-tolerant**: a crashed aggregator must not lose or duplicate counts on restart.

> **Contrast with 4.14 (metrics).** Metrics tolerate dropped samples — we watch trends. Ad clicks
> do **not** — every click is revenue. Same streaming machinery, opposite tolerance for error.
> That difference drives the whole exactly-once obsession here.

---

## Step 2: Estimation (back-of-envelope)

Say **1 billion clicks/day**.

```
clicks/sec (avg)  = 1e9 / 86,400 ≈ 11,600/sec
clicks/sec (peak) ≈ 5× avg       ≈ 58,000/sec
```

Each event is small — `{event_id, ad_id, user, ts}` ≈ ~100 bytes raw.

```
raw ingest:   1e9 × 100 B ≈ 100 GB/day  (before any aggregation)
aggregated:   clicks-per-ad-per-minute is TINY by comparison —
              1M ads × 1,440 min/day × ~20 B ≈ 28 GB/day, and queryable instantly
```

The lesson of the numbers: **aggregate early.** You never serve dashboards off the 100 GB/day raw
firehose; you serve them off the small per-ad-per-minute rollup. Keep the raw events for
**reconciliation/audit**, query the aggregates.

---

## Step 3: High-level design

### The stream-processing pipeline

```
 ┌────────┐  click   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
 │ click  │ events   │ message queue│   │  aggregator  │   │ aggregate    │
 │ source │ ───────► │ (Kafka-like, │──►│  • dedup set │──►│ store        │
 │ (web/  │          │  partitioned │   │  • per-window│   │ (ad×minute → │
 │  app)  │          │  by ad_id)   │   │    counters  │   │   count)     │
 └────────┘          └──────────────┘   └──────┬───────┘   └──────┬───────┘
                                               │                  │
                                               ▼                  ▼
                                        ┌──────────────┐   ┌──────────────┐
                                        │ raw event log│   │ query / TopK │
                                        │ (for audit/  │   │  dashboards  │
                                        │ reconcile)   │   └──────────────┘
                                        └──────────────┘
```

1. **Click source** emits a `ClickEvent` with a **unique event ID**.
2. A **partitioned queue** (Kafka-style) buffers the firehose and decouples producers from the
   aggregator. **Partition by `ad_id`** so all clicks for one ad land on one consumer — that lets
   that consumer count locally without cross-machine coordination.
3. The **aggregator** maintains a **dedup set** of seen event IDs and **per-window counters**.
4. Aggregates land in a small **store**; the raw log is kept separately for reconciliation.

### Data model & API (what you'll build)

```go
type ClickEvent struct { ID, AdID string; TsSec int64 }   // ID is the dedup key
type AdCount   struct { AdID string; Count int64 }

ProcessEvent(e ClickEvent)                                   // ingest; duplicates ignored
CountForWindow(adID string, windowStartSec, windowSec) int64 // clicks for one ad in a window
TopK(windowStartSec, windowSec int64, k int) []AdCount       // hottest k ads, ranked
```

Counts live keyed by **(window, ad)**. The window for an event at `ts` of width `w` starts at
`(ts / w) * w` — a **tumbling window**, same bucketing math as the metrics lesson.

---

## Step 4: Deep dives

### 4a. The exactly-once challenge (the core of this system)

"Exactly-once" is famously the hardest guarantee in distributed systems. The three delivery
semantics:

| Semantics | Meaning | Failure behaviour | Cost |
|-----------|---------|-------------------|------|
| **At-most-once** | fire and forget | may **lose** events | cheap, lossy |
| **At-least-once** | retry until acked | may **duplicate** events | cheap, what queues give you |
| **Exactly-once** | each event counted once | neither lost nor duplicated | expensive — needs dedup or transactions |

Real queues give you **at-least-once** cheaply (they retry on failure, so you sometimes get a
duplicate). The pragmatic path to exactly-once is: accept at-least-once delivery, then **dedup at
the consumer** using a unique, **idempotent** event ID.

> **Analogy.** The barcode on the stadium ticket again. The turnstile may scan a ticket twice
> (at-least-once), but the system **remembers scanned barcodes** and refuses the second scan. The
> net effect — each fan counted once — is exactly-once, achieved by being **idempotent** on the
> ID. (See building-block 2.14, *Idempotency*.)

In the assignment, `ProcessEvent` keeps a `seen` set of IDs; a repeat ID is silently ignored.
That's the whole trick, and it's what production systems do — the hard part at scale is keeping
that dedup set bounded.

> **Trade-off — the dedup set grows forever.** Remembering every event ID for all time is
> impossible at a billion/day. Production systems **bound** the set: only dedup within a time
> window (drop IDs older than, say, 24h), and/or use a **probabilistic structure** like a Bloom
> filter (building-block 2.13) that may have rare false positives (dropping a real click) in
> exchange for tiny memory. Exact dedup (a hash set) costs memory; approximate dedup costs a
> little accuracy. State which you'd pick and why.

### 4b. Tumbling windows for time bucketing

We count **per ad per minute**, so events bucket into fixed, non-overlapping **tumbling
windows** — identical math to the metrics lesson:

```
events:   c c    c   c c       c    c
time:   ──┬─────────┬─────────┬─────────┬──►
          0         60        120       180     (windowSec = 60)
window: [   W0    )[   W1    )[   W2    )
          count=3    count=2    count=2
```

The assignment stores counts at **1-second granularity** at ingest time and **sums the seconds**
inside the requested window at query time. Why not bucket directly into minute-windows on the way
in? Because then ingestion is locked to one window size; storing fine-grained and rolling up at
query time lets the *same* data answer "per minute" *and* "per 5 minutes" — exactly the
downsampling flexibility from the metrics lesson.

### 4c. Top-K ads

Given a window, return the `k` ads with the most clicks, ranked **count DESC, then ad ID ASC** to
make ties deterministic. The assignment does the simple, correct thing: aggregate the window into
a map, sort, take the first `k`.

> **Trade-off — full sort vs streaming heap vs sketch.** Sorting every ad is O(N log N) and fine
> when N (distinct ads in a window) is small. At huge scale you'd keep a **min-heap of size k**
> (O(N log k)) or, for approximate top-K over an unbounded stream, a **Count-Min Sketch +
> heap**: tiny memory, occasional rank error. Same theme as everywhere — exact costs more.

### 4d. Late events & watermarks

Events arrive out of order, and some show up **after** their window's count was already reported.
When do you declare a window "done"?

A **watermark** is the system's assertion: *"I believe I've seen all events up to time T."* When
the watermark passes a window's end, that window is closed and emitted.

```
window [0,60)        watermark moves →
events: ...58  61   59(late!)   62 ...
                 │      │
            window 0    └─ arrives AFTER we thought window 0 was done
            "closed"        → it's a LATE event
```

How you treat a late event is a policy decision:

| Late-event policy | What happens | Trade-off |
|-------------------|--------------|-----------|
| **Drop** | ignore events past the watermark | simplest; undercounts billing — bad here |
| **Allowed lateness** | hold windows open a bit longer before closing | catches stragglers; delays results |
| **Restate / amend** | re-open and correct the already-emitted count | correct, but downstream must handle updates |

For **billing**, you cannot simply drop; you allow lateness and/or **reconcile** (below). For a
live "trending ads" dashboard, dropping a few stragglers is acceptable.

### 4e. Lambda vs Kappa (reconciliation)

How do you get *both* fast results *and* eventually-correct results?

- **Lambda architecture** — run **two** pipelines: a **fast/streaming** path for near-real-time
  approximate counts, and a **slow/batch** path that reprocesses the raw event log periodically
  for the authoritative number. The batch result **reconciles** (overwrites) the fast one. Two
  codebases to maintain, but you get speed *and* a correct ledger.
- **Kappa architecture** — **one** streaming pipeline; to recompute, you **replay** the event log
  through the same code. Simpler (one codebase), and feasible because the raw log is retained.

> **Trade-off — speed vs correctness, and one pipeline vs two.** Streaming gives you a number in
> seconds but it may be slightly off (late events, dedup-window misses). Batch over the raw log is
> slow but exact. Lambda buys both at the cost of **two systems doing the same logic** (and the
> bugs that come from them disagreeing). Kappa keeps it to one system + replay. For ad billing the
> reconciled, audited number from the **raw log** is the source of truth; the fast number is just
> for live dashboards.

---

## In the wild

- **Kafka / Kinesis / Pulsar** — the partitioned, replayable event log at the front; partition by
  `ad_id` so each consumer owns a slice and counts locally.
- **Flink / Spark Streaming / Kafka Streams** — stream processors with built-in tumbling windows,
  watermarks, and (Flink) checkpoint-based exactly-once.
- **Count-Min Sketch & Bloom filters** — approximate top-K and approximate dedup when exact state
  is too big (see building-block 2.13).
- **Google Ads / ad exchanges** — run reconciliation/lambda-style batch jobs over the raw click
  log to produce the billed number, separate from the real-time dashboard estimate.

---

## Interview angle

Lead with the **pipeline** (queue → aggregator → store) and immediately call out **why a queue**:
it absorbs the firehose and lets you **partition by `ad_id`** so each consumer counts locally.
The senior signal is **exactly-once via event-ID dedup** — explain that queues give at-least-once,
so you make the consumer **idempotent** on a unique ID, and then volunteer the catch: the **dedup
set grows unbounded**, so you bound it by time window or use a Bloom filter (approximate). Then
handle **late/out-of-order events** with **watermarks** and an allowed-lateness/reconcile policy
(you can't just drop billable clicks). Close with **lambda vs kappa** — fast streaming estimate
reconciled against an exact batch pass over the retained raw log.

**Common follow-ups:**
- "The same click event arrives 3 times — how do you count it once?" → dedup set keyed on a unique
  event ID; ProcessEvent is idempotent on ID.
- "That dedup set is now petabytes. Fix it." → bound dedup to a time window; or a Bloom filter
  (tiny memory, rare false positive = rare dropped click) — name the accuracy trade-off.
- "A click arrives 10 minutes after its window closed — now what?" → watermark + allowed lateness,
  or restate/reconcile; for billing you must not silently drop it.
- "How do you get a real-time number *and* a correct billed number?" → lambda (fast stream +
  authoritative batch reconcile) or kappa (one pipeline + replay the log).
- "How do you find the top-10 ads without sorting millions?" → min-heap of size k, or Count-Min
  Sketch + heap for an approximate top-K.

---

## Practice → the Go assignment

Now build the aggregator. Go to [`assignment/`](assignment/) and implement, in order:

1. `ProcessEvent(e)` — ingest a click; **ignore duplicate IDs** (exactly-once via a dedup set);
   record the click at its second. Make it **concurrency-safe**.
2. `CountForWindow(adID, windowStart, windowSec)` — sum clicks for one ad in a tumbling window.
3. `TopK(windowStart, windowSec, k)` — the k hottest ads, ranked **count DESC, ad ID ASC**.

```bash
cd assignment
go test ./...          # red → implement → green
go test -race ./...    # must pass: ProcessEvent is called from many goroutines, dedup must hold
```

The interface is given; you fill in the `// TODO`s. A reference solution is in
[`solution/`](solution/) — try first, peek after.

**Next case study:** [4.16 — Object Storage (S3-like) »](../16-object-storage/)
