# 4.5 — Design a Web Crawler

> **Module 4 · Case Studies** · ~32 min read + coding assignment
> *Concepts exercised:* BFS at scale, the URL frontier, politeness/rate limiting,
> dedup with Bloom filters, worker pools, the "know when you're done" problem.

---

## The problem

A **web crawler** (a "spider", or "bot") systematically downloads web pages and follows their
links to discover more pages. Search engines (Googlebot), price aggregators, archive.org, and
training-data pipelines all start with a crawler.

The loop is deceptively simple:

```
1. Take a URL from a queue ("the frontier").
2. Download the page.
3. Parse out the links.
4. Add new (unseen) links back to the queue.
5. Repeat until the queue is empty (or you've crawled enough).
```

That's **breadth-first search over the graph of the web**, where pages are nodes and links are
edges. The reason it's a hard system-design question is everything around that loop: the web is
effectively infinite, you must not hammer any one server, you must avoid re-crawling the same
page forever, and you must do it across thousands of machines without deadlocking or
double-working.

> **Analogy.** Exploring a vast museum with many connected rooms. You keep a **to-visit list**
> (the frontier). You enter a room, note every doorway leading out, and add the rooms you
> haven't seen yet to your list. You **cross off rooms you've already visited** so you don't
> loop forever. And you're **polite** — you don't barge through the same doorway a hundred times
> a second or the guard (the web server) throws you out. You stop when the to-visit list is
> empty.

---

## Step 1: Requirements (always start here)

**Functional**
- **Crawl** starting from a set of **seed URLs**, following links to discover more pages.
- **Store** the fetched content (HTML) for downstream use (indexing, archiving).
- **Respect `robots.txt`** — the file where a site declares what bots may fetch.
- **Dedup** — never fetch the same URL twice; ideally never store the same *content* twice.

**Non-functional**
- **Scale.** Billions of pages; must run distributed across many machines.
- **Politeness.** Never overload a host. Cap requests per host and obey crawl-delay. *This is the
  rule that gets crawlers banned if broken.*
- **Freshness.** Pages change; re-crawl important pages periodically (news > a static archive).
- **Robustness.** Survive crashes, redirects, slow/dead servers, malformed HTML, and **traps**
  (pages that generate infinite links).
- **Extensibility.** Easy to add new content types or parsers later.

**Clarifying questions to ask the interviewer**
- HTML only, or images/PDFs/JS-rendered pages too?
- One-shot crawl, or continuous with re-crawling for freshness?
- How big — thousands, or the whole web? (Single box vs distributed.)
- Do we store raw pages, or just extract links/text?

---

## Step 2: Estimation (back-of-the-envelope)

Say we want **1 billion pages per month**.

- **Fetch rate:** 1B / (30 × 86400 s) ≈ **~400 pages/sec** sustained. With latency per fetch in
  the hundreds of ms, you need **massive concurrency** (thousands of in-flight fetches) — hence a
  worker pool / many machines, not a serial loop.
- **Storage:** average HTML ~100 KB compressed to ~25 KB. 1B × 25 KB ≈ **25 TB/month** of raw
  content. Plus metadata. This lives in blob storage; the *crawler's* own state is much smaller.
- **URL-seen set:** 1B URLs × ~50 bytes ≈ **50 GB** just to remember what we've seen — too big to
  keep exactly in memory per machine, which is why we reach for a **Bloom filter** (below).

The two scaling pressures: **enough concurrency to hit the fetch rate**, and **a dedup structure
that fits in memory**.

---

## Step 3: High-level design

The classic components:

```
            ┌──────────────┐
   seeds ──►│ URL Frontier │◄──────────────┐  new, unseen URLs
            │ (the queue)  │                │
            └──────┬───────┘                │
                   │ next URL               │
                   ▼                        │
            ┌──────────────┐         ┌──────┴───────┐
            │   Fetcher    │         │  URL-seen    │  "have I queued this URL?"
            │ (downloads)  │         │  filter      │  (Bloom filter / set)
            └──────┬───────┘         └──────────────┘
                   │ raw HTML                ▲
                   ▼                         │ links
            ┌──────────────┐                 │
            │   Parser     │─── extract URLs ┘
            │ (find links) │
            └──────┬───────┘
                   │ content
                   ▼
            ┌──────────────┐         ┌──────────────┐
            │ Content store│◄────────│ Content-seen │  "have I stored this exact
            │  (blobs)     │         │  filter      │   page before?" (hash dedup)
            └──────────────┘         └──────────────┘
```

- **URL Frontier** — the to-visit queue. Far more than a plain FIFO at scale (see deep dive).
- **Fetcher** — downloads a page (honoring `robots.txt` and politeness).
- **Parser** — extracts links (and content) from the HTML.
- **URL-seen** — answers "have I already queued/visited this URL?" so we don't re-enqueue.
- **Content-seen** — answers "have I already stored this *exact content*?" (the same article on
  many URLs) using a hash of the body.

The Go assignment focuses on the **engine**: a BFS over an injected `Fetcher`, run by a **worker
pool**, with a **visited set** and **correct termination**.

---

## Step 4: Deep dive — BFS vs DFS

The traversal order matters.

| | **BFS (breadth-first)** ⭐ | **DFS (depth-first)** |
|---|---|---|
| Order | Level by level from the seed | Follow one path deep, then backtrack |
| Frontier | A **queue** (FIFO) | A **stack** (LIFO) |
| Behavior on the web | Stays near the seeds, spreads out — good coverage of "important" pages first | Can plunge deep into one site, easy to get stuck in a trap |
| Memory | Frontier can grow wide | Frontier is path-deep |
| Politeness | Naturally interleaves many hosts | Tends to hammer one host (you keep following its links) |

**BFS is the standard choice.** It surfaces high-value pages sooner (pages near popular seeds
tend to matter more) and naturally spreads load across hosts. The assignment implements BFS.

---

## Step 5: Deep dive — politeness (per-host rate limiting)

The cardinal rule: **never overload a single server.** If you fan out 1,000 workers and they all
hit `example.com` at once, you've launched an accidental DoS and you'll be blocked.

- **One connection / limited rate per host.** Cap concurrent requests *per host* and add a
  **crawl-delay** between hits to the same host (e.g. 1 request/sec, or whatever `robots.txt`
  says). This is exactly a **per-host [rate limiter](../01-rate-limiter/)**.
- **`robots.txt`** — fetch and cache it per host; obey `Disallow` rules and any `Crawl-delay`.
- The frontier must be **politeness-aware**: it can't just hand out the next URL; it must hand
  out the next URL *whose host is ready to be hit again*.

This is why the frontier is more than a queue.

---

## Step 6: Deep dive — the URL frontier design

At scale the frontier balances two goals that pull against each other:

1. **Priority** — crawl important / fresh pages sooner (a news homepage beats a random forum
   post). Implemented with **priority queues** (front queues): a prioritizer assigns each URL a
   priority, routing it to one of several queues.
2. **Politeness** — never two simultaneous hits to one host. Implemented with **per-host queues**
   (back queues): each back queue holds URLs for *one host*, and a scheduler with per-host timers
   releases a URL only when that host's delay has elapsed.

```
URLs ─► [prioritizer] ─► front queues (by priority) ─► [router] ─► back queues (one per host)
                                                                        │ each host's timer
                                                                        ▼ gates release
                                                                     to fetchers
```

So the frontier is a **two-level queue system**: priority front, politeness back. That two-stage
structure is the senior insight to mention.

---

## Step 7: Deep dive — dedup with Bloom filters

You must answer "have I seen this URL before?" **billions** of times. An exact hash set of all
URLs can be tens of GB — awkward to hold in memory per node.

A **Bloom filter** is a compact, probabilistic set membership structure (covered in
[2.13](../../02-building-blocks/13-probabilistic-structures.md)):

- It can say **"definitely not seen"** or **"probably seen."**
- **No false negatives** (it never wrongly says "new" for something seen), but **possible false
  positives** (it may wrongly say "seen" for something new).
- Uses a fraction of the memory of an exact set.

The trade-off: a false positive means you **skip** a genuinely new URL — you under-crawl slightly.
For a web crawler that's an acceptable price for fitting the dedup set in memory. (For
**content** dedup, hash the page body and check a similar set so the same article on many URLs
isn't stored repeatedly.)

> In the Go assignment we use an **exact in-memory set** (a map / `sync.Map`) because the test
> graphs are small and we want *exact* "each URL once" guarantees. A Bloom filter is the
> production swap-in when the set no longer fits in RAM.

---

## Step 8: Deep dive — traps, dynamic content, and going distributed

- **Crawler traps.** Calendars with "next month" links forever, or URLs with infinite query
  permutations. Defend with **max depth**, **max URLs per host**, URL-length caps, and dropping
  obviously parameter-exploding URLs.
- **Dynamic / JS-rendered content.** Much of the modern web renders client-side. A plain fetcher
  sees an empty shell; you need a **headless browser** to execute JS — far more expensive, so
  you do it selectively.
- **Distributed crawling.** Shard the frontier and the seen-set **by host** (consistent hashing)
  so one machine owns a host — that makes per-host politeness easy to enforce locally and avoids
  cross-machine coordination on every URL. Workers pull from their shard; newly found URLs are
  routed to the owning shard.

---

## In the wild

- **Googlebot / Bingbot** crawl continuously with priority + freshness scheduling and strict
  politeness; they obey `robots.txt` and publish their IP ranges.
- **Common Crawl** publishes petabytes of crawled web data used to train models.
- **The Internet Archive's** Heritrix crawler powers the Wayback Machine.
- Open-source **Apache Nutch / Scrapy** implement the frontier + fetcher + parser pipeline.

---

## Interview angle

Frame it as **BFS over the web graph** and draw the **frontier → fetcher → parser → seen-sets**
pipeline. The three details that signal depth: (1) **politeness** as a **per-host rate limiter**,
and a **frontier that's a two-level queue** (priority front + per-host back queues); (2) **dedup
with a Bloom filter** and the false-positive trade-off; (3) **distributed crawling by sharding on
host** so politeness stays local. Mention **traps** and **JS rendering** as robustness concerns.

**Common follow-ups:**
- "How do you avoid hammering one website?" → per-host concurrency cap + crawl-delay; the back
  queues enforce it.
- "The URL-seen set won't fit in memory — now what?" → Bloom filter (accept rare false positives
  → slight under-crawl); shard by host across machines.
- "How do you know when the crawl is *done*?" → track outstanding work (in-flight + queued); done
  when it reaches zero. (This is the crux of the coding assignment.)
- "Same article lives on 5 URLs — don't store it 5 times." → content-seen set keyed by a hash of
  the body.
- "A page errors or times out." → skip it gracefully, with retries/backoff; one bad page must not
  stall the crawl.

---

## Practice → the Go assignment

Now build the engine. Go to [`assignment/`](assignment/) and implement a **concurrent** crawler
over an **injected `Fetcher`** (so tests are deterministic — no real network):

1. A **worker pool** of `workers` goroutines pulling URLs from a frontier channel.
2. A **visited set** (mutex-guarded map or `sync.Map`) so each URL is fetched **at most once**.
3. **Correct termination** — the hard part. Track outstanding work so the crawl shuts down
   cleanly when the frontier drains: **no deadlock, no premature exit.** Use a `WaitGroup` or a
   counter to solve the classic "how do I know I'm done?" problem.
4. **Graceful errors** — a URL whose fetch returns an error is skipped, not fatal.

```bash
cd assignment
go test ./...          # red → implement → green
go test -race ./...    # must pass: the visited set is shared across workers
```

The interface is given; you fill in the `// TODO`s. A reference solution is in
[`solution/`](solution/) — try first, peek after.

**Next case study:** [4.6 — Notification System »](../06-notification-system/)
