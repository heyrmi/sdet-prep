# 0.2 — Numbers Every Engineer Should Know

> **Module 0 · Foundations** · ~22 min read
> *You can't design a system you can't estimate, and you can't estimate without a feel for the
> numbers. This lesson hands you the handful of figures that let you say "that won't fit in RAM"
> or "that'll need 50 servers" in your head, in seconds.*

---

## The problem

Imagine an architect who has no sense of how heavy a brick is, how far steel can span, or how
much a truck of concrete costs. Every decision would be a guess. They'd propose a glass roof
that collapses or a foundation ten times bigger than needed.

Software engineers are often exactly that architect. Asked "should we cache this in memory or
read it from disk each time?" they shrug — because they don't have a *physical* feel for how
much slower disk is than memory. (It's about **100,000× slower.** We'll prove it below.)

> **Analogy.** Picture distances scaled to human time. If grabbing a value from **CPU cache**
> took **1 second**, then reading from **RAM** would take ~1.5 minutes, reading from a **fast
> SSD** would take ~hours, and a **single network round trip across the planet** would take
> **years**. Same operations, wildly different cost. Good engineers carry this map in their head.

This lesson is a reference you'll come back to. The goal isn't to memorize every digit — it's to
internalize the **orders of magnitude** so you can reason quickly. Nobody cares if RAM is 90ns
or 120ns; everyone cares that it's *nanoseconds* while a disk seek is *milliseconds* — a million
times slower.

---

## Core idea: orders of magnitude, not exact figures

Every number here is approximate and changes with hardware generations. What *doesn't* change is
the **ratio** between tiers. Memory is faster than disk. Disk is faster than network. Local is
faster than remote. Sequential is faster than random. Internalize the *gaps*, not the decimals.

First, the units of time we'll use:

| Unit | Symbol | Seconds | "If 1ns = 1 second" |
|------|--------|---------|---------------------|
| nanosecond | ns | 10⁻⁹ | 1 second |
| microsecond | µs | 10⁻⁶ (1,000 ns) | ~16.7 minutes |
| millisecond | ms | 10⁻³ (1,000 µs) | ~11.6 days |
| second | s | 1 (1,000 ms) | ~31.7 years |

That last column is the intuition pump. A millisecond *feels* small to a human, but to a CPU
that does things in nanoseconds, a millisecond is **eleven days**. This is why "it only hits the
disk once per request" can quietly destroy your latency.

---

## Latency numbers every programmer should know

This is the famous table (popularized by Jeff Dean / Peter Norvig), rounded to memorable values
for modern hardware:

| Operation | Latency | In "ns = 1s" human scale |
|-----------|---------|--------------------------|
| L1 cache reference | ~1 ns | 1 second |
| Branch mispredict | ~3 ns | 3 seconds |
| L2 cache reference | ~4 ns | 4 seconds |
| Mutex lock/unlock | ~17 ns | 17 seconds |
| Main memory (RAM) reference | ~100 ns | ~1.5 minutes |
| Compress 1 KB with a fast algorithm | ~2 µs | ~33 minutes |
| Read 1 MB sequentially from RAM | ~3 µs | ~50 minutes |
| SSD random read | ~16 µs | ~4.4 hours |
| Read 1 MB sequentially from SSD | ~50 µs | ~14 hours |
| Round trip within same datacenter | ~0.5 ms | ~5.8 days |
| Read 1 MB sequentially from disk (HDD) | ~1–2 ms | ~2–3 weeks |
| Disk (HDD) seek | ~2–10 ms | ~2–4 months |
| Round trip, same region (city to city) | ~1–10 ms | weeks to months |
| Round trip, cross-region / cross-country | ~50–80 ms | ~1.5–2.5 years |
| Round trip, intercontinental | ~100–150 ms | ~3–5 years |

A few things to actually remember from this table:

- **L1/L2 cache: single-digit nanoseconds.** Effectively free; the CPU lives here.
- **RAM: ~100 ns.** Fast, but ~100× slower than L1. This is your "memory access" baseline.
- **SSD random read: ~16 µs** ≈ **160× slower than RAM.**
- **HDD seek: ~2–10 ms** ≈ **100,000× slower than RAM**, because a physical arm has to move.
- **Same-datacenter round trip: ~0.5 ms.** This is the tax on *every* call to another service,
  cache, or database over the network — even a "fast" one.
- **Cross-region round trip: ~50–150 ms.** Now dominated by the **speed of light**, which no
  amount of money can fix. Light covers ~200 km per ms in fiber; New York ↔ London is ~5,500 km,
  so the physics alone forces ~28 ms *each way* before any processing.

> **The one ratio to tattoo on your brain: memory is ~100,000× faster than disk** (for random
> access). This single fact justifies the entire existence of **caching** (Module 2.3). When you
> move hot data from disk into RAM, you're not making it "a bit faster" — you're collapsing
> *months* of human-scale time into *minutes*.

### Why sequential beats random (everywhere)

Notice that reading **1 MB sequentially** from SSD (~50 µs) is far cheaper *per byte* than many
random reads. On a spinning disk the difference is brutal: a sequential scan avoids the ~2–10 ms
seek that random access pays *every time*. This is why databases and storage engines work so hard
to turn random writes into sequential ones — it's the entire motivation behind **LSM-trees and
write-ahead logs** (Module 3.2).

```
  Random reads (HDD):  seek ─ read ─ seek ─ read ─ seek ─ read   (arm moves each time, ~ms apart)
  Sequential read:     seek ─ read─read─read─read─read─read       (one seek, then stream)
```

---

## Powers of two: the data-size table

Computers count in binary, so storage and memory sizes are powers of two. But here's the part
people get wrong: **networking, disk marketing, and back-of-envelope math usually use powers of
ten.** Knowing both — and that they're *close but not equal* — keeps you from being off by 7%+.

| Power | Exact value | Approx | Name (binary) | Name (decimal, ~) |
|-------|-------------|--------|---------------|-------------------|
| 2¹⁰ | 1,024 | ~1 thousand | 1 KiB | 1 KB |
| 2²⁰ | 1,048,576 | ~1 million | 1 MiB | 1 MB |
| 2³⁰ | 1,073,741,824 | ~1 billion | 1 GiB | 1 GB |
| 2⁴⁰ | 1,099,511,627,776 | ~1 trillion | 1 TiB | 1 TB |
| 2⁵⁰ | ~1.13 × 10¹⁵ | ~1 quadrillion | 1 PiB | 1 PB |

**The shortcut for estimation:** treat 2¹⁰ ≈ 10³ (a thousand), 2²⁰ ≈ 10⁶ (a million), 2³⁰ ≈ 10⁹
(a billion), 2⁴⁰ ≈ 10¹² (a trillion). The real values are ~2.4% bigger per step (1.024×), so the
gap compounds: a "1 TB" disk in decimal is only ~0.91 TiB to your OS. For estimation it doesn't
matter; for capacity planning it does.

A handy memory aid for the prefixes, ascending: **K**ilo, **M**ega, **G**iga, **T**era, **P**eta,
**E**xa — *"Kids Make Great Teachers, Period. Excellent."*

### Powers of two worth recognizing on sight

| 2ⁿ | Value | Where you'll see it |
|----|-------|---------------------|
| 2⁷ | 128 | Max value of a signed byte (−128..127) |
| 2⁸ | 256 | Values in one byte; ASCII range |
| 2¹⁶ | 65,536 | Max TCP ports; `int16` range |
| 2³² | ~4.29 billion | IPv4 address space; `int32`; the "4 billion row" ceiling |
| 2⁶³ | ~9.2 × 10¹⁸ | Max value of a signed 64-bit int |
| 2⁶⁴ | ~1.8 × 10¹⁹ | Full 64-bit space; effectively unlimited IDs |

> **Why 2³² matters in interviews.** When someone says "we'll have more than 4 billion items,"
> that's code for "a 32-bit ID will overflow — we need 64 bits." Recognizing 2³² ≈ 4.3B instantly
> is a small but real signal of fluency. (Exactly why URL shorteners and ID generators use 64-bit
> schemes — Modules 4.3 and 4.4.)

---

## How big is "one thing"? Data sizes of common values

To estimate storage you need a rough size for the *unit* you're storing. Memorize these:

| Thing | Typical size | Notes |
|-------|--------------|-------|
| ASCII character | 1 byte | One byte = 8 bits |
| Unicode (UTF-8) character | 1–4 bytes | ASCII is 1 byte; emoji can be 4 |
| Boolean | 1 byte | (1 bit logically, but usually stored as a byte) |
| Integer (`int32`) | 4 bytes | Up to ~2.1 billion (signed) |
| Long / `int64` / timestamp | 8 bytes | Unix epoch ms fits easily |
| UUID / GUID | 16 bytes | 128-bit; ~36 chars if stored as a string (avoid that) |
| Unix timestamp (seconds) | 4 bytes | Overflows in 2038 if 32-bit! Use 8 bytes |
| IPv4 address | 4 bytes | 32 bits |
| IPv6 address | 16 bytes | 128 bits |
| A tweet (280 chars + metadata) | ~300 bytes – 1 KB | text is small; metadata dominates |
| A typical web page (HTML) | ~50–100 KB | before images |
| A high-res photo | ~1–5 MB | |
| A minute of 1080p video | ~50–100 MB | huge — why video needs CDNs + transcoding |

> **Estimation trick.** When you don't know an exact size, **round to a power-of-two-friendly
> number** and move on. "A user row? Call it ~1 KB." "A tweet? Call it ~300 bytes or ~1 KB to be
> safe." The art of back-of-envelope math (next lesson) is being *roughly right fast*, not
> precisely right slowly.

---

## Availability math: "the nines"

**Availability** is the percentage of time a system is up and serving correctly. It's almost
always quoted in "nines."

> **Analogy.** Availability is like a restaurant's promise of being open. "Open 99% of the year"
> sounds great until you realize that's **3.65 days closed** with no warning — possibly your
> busiest weekend. "Five nines" is being closed only ~5 minutes a year.

The key insight: **each extra nine costs roughly 10× more effort** (redundancy, failover,
testing, on-call) while shrinking downtime by 10×.

| Availability | "Nines" | Downtime / year | Downtime / month | Downtime / day |
|--------------|---------|-----------------|------------------|----------------|
| 90% | one nine | ~36.5 days | ~3 days | ~2.4 hours |
| 99% | two nines | ~3.65 days | ~7.2 hours | ~14.4 min |
| 99.9% | three nines | ~8.76 hours | ~43.8 min | ~1.44 min |
| 99.99% | four nines | ~52.6 min | ~4.38 min | ~8.6 sec |
| 99.999% | five nines | ~5.26 min | ~26 sec | ~0.86 sec |
| 99.9999% | six nines | ~31.5 sec | ~2.6 sec | ~0.086 sec |

How to compute it yourself: a year is ~525,600 minutes (365 × 24 × 60). Downtime = (1 − A) ×
525,600. For 99.9%: 0.001 × 525,600 ≈ 526 min ≈ 8.76 hours. Easy to redo in an interview.

### Availability of a chain vs. redundancy

This is where it gets non-obvious and *very* interview-relevant.

**Components in series (a request needs ALL of them):** multiply their availabilities. If your
request passes through an LB (99.99%), a web server (99.9%), and a DB (99.9%), the combined
availability is:

```
0.9999 × 0.999 × 0.999 ≈ 0.9979  → ~99.79%  (worse than any single part!)
```

More dependencies in the critical path = *lower* combined availability. This is a core argument
for keeping the request path short and for graceful degradation.

**Redundant components in parallel (you need only ONE to work):** multiply their *failure*
rates. Two servers each at 99% give combined availability:

```
1 − (0.01 × 0.01) = 1 − 0.0001 = 0.9999  → 99.99%   (much better!)
```

Two cheap "two nines" boxes, run in parallel, give you "four nines." **Redundancy is how you buy
availability** — the entire motivation for replication, multiple servers behind a load balancer,
and multi-AZ deployments (Modules 2.1, 2.6, 3.5).

> **Trade-off.** Each nine is exponentially more expensive. Most businesses target 99.9%–99.99%.
> Going for five nines means automated failover, no-downtime deploys, multi-region, and serious
> on-call investment. Don't promise nines you can't afford to operate.

---

## Throughput (QPS) vs. latency

These two get conflated constantly. They are different axes.

> **Analogy.** A highway. **Latency** is how long *your* car takes to drive from A to B.
> **Throughput** is how many cars *total* pass a point per hour. Adding lanes raises throughput
> without making any single car faster. A faster speed limit lowers latency. You can have high
> throughput *and* high latency at once (a packed, fast-flowing freeway).

- **Latency** — time for **one** operation to complete. Measured in ms/µs. "How slow is it?"
- **Throughput** — operations completed **per unit time**. Measured in QPS (queries/sec), RPS
  (requests/sec), or items/sec. "How much can it handle?"

| Metric | Question it answers | Unit | How to improve |
|--------|---------------------|------|----------------|
| Latency | How long does one request take? | ms, µs | Cache, faster code, fewer hops, closer data |
| Throughput | How many requests/sec can we serve? | QPS, RPS | Add servers, parallelize, batch, shard |

A useful relationship is **Little's Law**: `concurrency = throughput × latency`. If each request
takes 100 ms (0.1 s) and you want 1,000 QPS, you need ~100 requests in flight at once
(`1000 × 0.1`). This tells you how many threads/connections/workers to provision. Lower the
latency and you need less concurrency for the same throughput — another reason latency matters
beyond user happiness.

---

## Percentiles: why p99 (tail latency) is the number that matters

Here's the trap: **averages lie.** If 99 requests take 10 ms and one takes 5,000 ms, the average
is ~60 ms — a number that describes *no actual request*. The average hides the user who waited 5
seconds.

> **Analogy.** A coffee shop where most orders take 2 minutes, but one barista occasionally
> disappears for 15 minutes. The *average* wait looks fine. But if you're the unlucky customer in
> the slow line, "average" is no comfort. **Percentiles describe the unlucky customers.**

A **percentile** `pN` is the value below which N% of measurements fall:

| Percentile | Meaning | Who it represents |
|------------|---------|-------------------|
| **p50** (median) | half of requests are faster than this | the "typical" user |
| **p90** | 90% are faster; 1 in 10 is slower | a common slow case |
| **p95** | 95% are faster; 1 in 20 is slower | noticeable slow tail |
| **p99** | 99% are faster; 1 in 100 is slower | the tail; SLO target |
| **p99.9** | 999 in 1,000 are faster | the *far* tail; power users feel this |

### Why the tail dominates at scale

The cruel math: if **one in a hundred** requests is slow (p99), and a single page load makes
**100 backend calls** (very normal — feeds, recommendations, ads, sidebars), then the *probability
that at least one of those calls hits the slow tail* is:

```
1 − (0.99)¹⁰⁰ ≈ 1 − 0.366 = 0.634  →  ~63% of page loads hit the slow path
```

So your "1% slow" backend makes **the majority of full pages slow.** This is **tail-latency
amplification**, and it's why big companies obsess over p99/p99.9 rather than averages. The more
fan-out a request has, the more the tail controls the user experience.

> **Key takeaway.** Optimize and set SLOs (Service Level Objectives) on **p99/p99.9**, not the
> average. "Our average is 20 ms" can hide a p99 of 2 seconds that's ruining your busiest users.

```
  count
    │      ████
    │     ██████
    │    ████████
    │   ██████████              ▏ long tail ▏
    │  ████████████▏▏▏▏▏▏▏▏▏▏▏▏▏▏▏▏▏▏▏▏▏▏▏▏▏▏▏▏▏▏  ← p99 / p99.9 live way out here
    └─────────────────────────────────────────────► latency
        p50         p90  p95      p99           p99.9
```

---

## Trade-offs & key takeaways

- **Internalize ratios, not digits.** RAM ~100ns, SSD ~16µs, disk seek ~ms, same-DC hop ~0.5ms,
  cross-region ~50–150ms. Each tier is roughly 100–1000× the one above it.
- **Memory is ~100,000× faster than disk** — the one-line justification for caching.
- **Sequential ≫ random**, especially on disk — the justification for logs and LSM-trees.
- **Powers of two:** 2¹⁰≈1K, 2²⁰≈1M, 2³⁰≈1B, 2⁴⁰≈1T. Know 2³²≈4.3B (the IPv4 / 32-bit ceiling).
- **Availability multiplies in series (worse) and de-multiplies in parallel (redundancy → better).**
  Each nine costs ~10× more.
- **Latency ≠ throughput.** Different problems, different fixes. `concurrency = throughput × latency`.
- **Tail latency (p99/p99.9) is what users feel at scale.** Averages lie. Fan-out amplifies tails.

---

## In the wild

- **Google** publishes the canonical "latency numbers" and famously designs around tail latency —
  techniques like hedged requests (send a duplicate to a second replica if the first is slow) exist
  specifically to cut p99.
- **Amazon** found every 100 ms of added latency measurably cut sales — latency is revenue.
- **Cloud SLAs** are written in nines: many managed services promise 99.9% or 99.99% monthly, with
  service credits when they miss. Read the fine print — "monthly" nines reset the clock.
- **Netflix / Twitter** dashboards lead with p50/p95/p99, not averages.

---

## Interview angle

Interviewers don't expect exact figures, but they *love* candidates who reach for the right order
of magnitude unprompted: "a cross-region call is ~100 ms, so doing three of them serially blows our
latency budget — let's parallelize or co-locate." That's the signal.

**Common follow-ups:**
- "Roughly how much slower is disk than memory?" → ~100,000× for random access; the reason caches exist.
- "Your service is at 99.9%. A new dependency is also 99.9%. What's the combined availability?" →
  multiply in series: ~99.8% — *worse*, because failures add up across the chain.
- "Average latency is 30 ms but users complain it's slow. What do you check?" → the p99/p99.9; the
  average is hiding a fat tail.
- "Why does a page that makes 100 service calls feel slow even with a fast p99?" → tail-latency
  amplification: `1 − 0.99¹⁰⁰ ≈ 63%` of pages hit the slow path.

---

## Self-check

1. Same-datacenter round trip vs. cross-region round trip — what are the rough numbers, and which
   one is bounded by physics you can't buy your way out of?
2. You store 200 million UUIDs as raw 16-byte values. About how much storage is that? (Show the
   power-of-two reasoning.)
3. A request passes through four services, each 99.9% available, all required. Is the end-to-end
   availability higher or lower than 99.9%, and why?
4. Your p50 is 15 ms and your p99 is 1,200 ms. Which number should your alerting be based on, and
   what real-world phenomenon makes p99 so important when a page fans out to many calls?
5. Why do storage engines work so hard to turn random writes into sequential ones?

---

**Next:** [0.3 — Back-of-the-Envelope Estimation »](03-back-of-envelope-estimation.md)
