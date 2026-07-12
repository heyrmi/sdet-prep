# 0.3 — Back-of-the-Envelope Estimation

> **Module 0 · Foundations** · ~24 min read
> *The skill that turns "I don't know, a lot?" into "about 35,000 QPS at peak, ~150 TB over five
> years, so roughly 30 cache servers." It's just multiplication — but knowing **which** numbers
> to multiply is the whole game.*

---

## The problem

Someone asks: "Design Twitter." Before you draw a single box, you need to know if you're building
for **a thousand users or a billion.** The answer changes everything — one Postgres box vs. a
sharded, multi-region fleet. You can't make architecture decisions without a *rough* sense of the
load. And nobody is going to hand you exact numbers; you have to **estimate** them.

> **Analogy.** A caterer asked to feed "a wedding" doesn't freeze. They ask "how many guests?",
> assume "~1.5 plates each, 20% no-shows, peak demand at dinner," and arrive at "cook for 180."
> They're never *exactly* right — and it doesn't matter. Being **roughly right, fast** is what
> lets them order the right amount of food. System design estimation is identical: a few sane
> assumptions, some multiplication, an order-of-magnitude answer.

This is called **back-of-the-envelope estimation** (you could do it on the back of an envelope).
The point is **not precision** — it's getting within ~10× of reality so your design is sane. A
design built for 1,000 QPS when reality is 100,000 QPS will collapse; one built for the right
*order of magnitude* will hold.

You'll use the latency and size numbers from [Lesson 0.2](02-numbers-every-engineer-should-know.md)
constantly here — keep that table handy.

---

## Core idea: four quantities, one method

Almost every estimation reduces to four questions:

1. **QPS** — how many requests per second (and per second *at peak*)?
2. **Storage** — how many bytes, growing how fast, kept how long?
3. **Bandwidth** — how many bytes per second in/out of the system?
4. **Servers / memory** — how many machines, and how much cache RAM?

The method is always the same:

```
  state your assumptions  →  pick round numbers  →  multiply  →  sanity-check the magnitude
```

**Rule #1: round aggressively.** Use 100,000 seconds for a day (it's 86,400 — round to 10⁵).
Use powers of ten. The goal is mental arithmetic, not a spreadsheet.

**Rule #2: write down every assumption.** "Assume 2 KB per tweet, 10% of users post daily." In an
interview, *stating assumptions out loud is half the score* — it shows the interviewer how you
think and lets them correct you ("actually assume 50%") instead of marking you wrong.

---

## Building block 1: Estimating QPS

QPS = Queries Per Second. Start from users, end at per-second load.

The core formula:

```
  average QPS  =  (DAU × actions per user per day)  ÷  seconds per day
```

- **DAU** = Daily Active Users (the users who actually use it *today*, not total signups).
- **actions/user/day** = how many of the relevant operation each does (reads, writes, etc.).
- **seconds per day** = 86,400 — **round to 100,000 (10⁵)** for mental math.

> **Why 100,000?** 86,400 ≈ 10⁵ is close enough and turns division into "shift the decimal." This
> single trick is the most-used shortcut in all of estimation.

### Peak factor

Traffic is never flat. Everyone's awake and active at the same times (lunch, evening). Real systems
peak at roughly **2–5× the average.** Always compute average QPS, then multiply by a peak factor:

```
  peak QPS  =  average QPS  ×  peak factor (use 2–5×; default to ~3×)
```

You design capacity for the **peak**, not the average — the system has to survive its busiest
moment, not its typical one.

### Worked micro-example

A service with **10 million DAU**, each doing **20 actions/day**:

```
  total actions/day = 10M × 20 = 200M actions/day
  average QPS = 200,000,000 ÷ 100,000 ≈ 2,000 QPS
  peak QPS = 2,000 × 3 ≈ 6,000 QPS
```

Done in your head in ten seconds. That's the skill.

---

## Building block 2: Estimating storage

```
  storage  =  items written per day  ×  bytes per item  ×  retention (days)
```

The three knobs:

- **items/day** — usually `writes per second × 86,400`, or `DAU × writes per user per day`.
- **bytes/item** — from the size table in Lesson 0.2 (tweet ≈ 1 KB, user row ≈ 1 KB, photo ≈ few MB).
  **Include metadata and indexes**, and add headroom — real storage is bigger than the raw payload.
- **retention** — how long you keep it. "Forever" → pick a horizon like 5 years for the estimate.

> **Don't forget replication.** If you keep 3 copies for durability (standard), multiply the final
> number by ~3. Interviewers love to ask "and with replication?" Have the answer ready.

### Worked micro-example

100 million new items/day, ~1 KB each, kept for 5 years:

```
  per day  = 100M × 1 KB = 100 GB/day
  per year = 100 GB × 365 ≈ 36.5 TB/year
  5 years  ≈ 180 TB
  ×3 for replication ≈ 540 TB
```

That magnitude (hundreds of TB) immediately tells you: a single disk won't do it — you need
**sharding** (Module 2.7) and probably blob/object storage for any large media.

---

## Building block 3: Estimating bandwidth

Bandwidth is just **data size × rate**, split into ingress (in) and egress (out).

```
  write bandwidth  =  write QPS  ×  bytes per write
  read  bandwidth  =  read  QPS  ×  bytes per read
```

For read-heavy systems, **egress dominates** and is often the real cost (cloud providers charge
for data out). Media-heavy systems (video, images) live or die on bandwidth — which is exactly why
**CDNs** (Module 2.2) exist: serve big bytes from the edge instead of your origin.

### Worked micro-example

6,000 read QPS, each returning ~50 KB of data:

```
  read bandwidth = 6,000 × 50 KB = 300,000 KB/s ≈ 300 MB/s ≈ 2.4 Gbps
```

Now you know your egress tier needs multiple Gbps — a real CDN/LB consideration.

---

## Building block 4: Estimating cache memory (the 80/20 rule)

You rarely cache *everything* — you cache the **hot** data. The classic heuristic is the **80/20
rule (Pareto principle): ~80% of requests hit ~20% of the data.** So size your cache to hold that
hot ~20%.

```
  daily read volume (bytes)  =  read QPS × 86,400 × bytes per read
  cache size  ≈  20% × (daily volume of distinct items requested)
```

### Worked micro-example

Reads pull ~10 million distinct items/day at ~1 KB each = ~10 GB of distinct daily data. Cache 20%:

```
  cache ≈ 0.2 × 10 GB = 2 GB of hot data
```

2 GB fits comfortably in one Redis node's RAM — good. If it came out to 2 **TB**, you'd need a
*cluster* of cache nodes (and sharding the cache). The estimate decides the architecture.

> **Trade-off.** A bigger cache → higher **hit rate** → less DB load and lower latency, but more
> RAM cost. The 80/20 cut is the sweet spot: most of the benefit for a fraction of the memory.
> Past that you pay a lot of RAM for diminishing returns. (Eviction & hit-rate details: Module 2.3.)

---

## Building block 5: Estimating number of servers

Two independent angles — take the **larger** of the two:

**By throughput** (can the machines handle the QPS?):

```
  servers  =  peak QPS  ÷  QPS one server can handle
```

A modern server handles anywhere from ~1,000 to tens of thousands of simple QPS depending on the
work. Use a conservative round number (e.g. 1,000 QPS/server) unless you know better, and **add
headroom** — never plan to run at 100%; target ~50–70% utilization so a failed node doesn't tip
the rest over.

**By connections** (for long-lived connections like WebSockets):

```
  servers  =  concurrent connections  ÷  connections one server holds (~tens of thousands)
```

Chat/streaming systems are usually **connection-bound**, not QPS-bound — a different math (see
Module 4.8, Chat System).

---

## Full worked example: a Twitter-like service

Let's run the entire pipeline end to end. **State assumptions, then compute.** This is exactly how
you'd do it on a whiteboard.

### Assumptions

| Assumption | Value | Reasoning |
|------------|-------|-----------|
| DAU | 300 million | given / typical for a large social app |
| Tweets posted per user per day | 0.5 (avg) | most users read far more than they post |
| Read:write ratio | 100:1 | social feeds are heavily read-dominated |
| Size of a tweet (text + metadata) | ~1 KB | text is small; IDs/timestamps/flags add up |
| Media: % of tweets with an image | 10% | |
| Average image size | ~200 KB | compressed |
| Retention | 5 years | "keep forever" → pick a horizon |
| Replication factor | 3 | durability |
| Peak factor | 3× | typical daily peak |

### Step 1 — Write QPS

```
  tweets/day = 300M × 0.5 = 150M tweets/day
  avg write QPS = 150,000,000 ÷ 100,000 ≈ 1,500 writes/sec
  peak write QPS = 1,500 × 3 ≈ 4,500 writes/sec
```

### Step 2 — Read QPS

Read:write is 100:1, so:

```
  avg read QPS  = 1,500 × 100 = 150,000 reads/sec
  peak read QPS = 150,000 × 3 ≈ 450,000 reads/sec
```

That ~450K peak read QPS is the headline number — it screams **heavy caching + read replicas +
fan-out strategy** (News Feed, Module 4.7). No single DB serves that.

### Step 3 — Storage (tweets text)

```
  per day  = 150M × 1 KB = 150 GB/day
  per year = 150 GB × 365 ≈ 55 TB/year
  5 years  ≈ 275 TB
  ×3 replication ≈ ~825 TB  (call it ~1 PB)
```

### Step 4 — Storage (media)

```
  images/day = 10% × 150M = 15M images/day
  per day = 15M × 200 KB = 3,000,000,000 KB ≈ 3 TB/day
  per year ≈ 3 TB × 365 ≈ ~1.1 PB/year
  5 years ≈ ~5.5 PB
  ×3 replication ≈ ~16 PB
```

Media dwarfs text by ~20×. Lesson: **media goes in blob/object storage + CDN**, never in your
primary DB. (Object Storage, Module 4.16.)

### Step 5 — Bandwidth

```
  read egress (text) = 150,000 reads/sec × 1 KB ≈ 150 MB/s ≈ 1.2 Gbps  (average)
  at peak (×3)        ≈ 450 MB/s ≈ 3.6 Gbps
```

Media egress is far larger and is exactly what the CDN absorbs — your origin should rarely serve
raw images at this scale.

### Step 6 — Cache memory (80/20)

Cache the hot 20% of *one day's* distinct tweets read:

```
  distinct tweets read/day ~ tens of millions; assume ~100M distinct × 1 KB = ~100 GB/day
  cache 20% ≈ 20 GB of hot tweets
```

~20 GB easily fits across a small Redis cluster (a handful of nodes for redundancy + headroom).
The point: caching the hot set is *cheap* relative to the 450K read QPS it absorbs.

### Step 7 — Servers (rough)

```
  read tier: peak 450K read QPS ÷ ~1,000 QPS/server (conservative) ≈ 450 servers
  add ~40% headroom → ~600+ read-serving instances
```

(With aggressive caching, the DB sees a small fraction of that — most reads die at the cache.)

### The one-page summary

| Quantity | Average | Peak |
|----------|---------|------|
| Write QPS | ~1,500 | ~4,500 |
| Read QPS | ~150,000 | ~450,000 |
| Text storage (5y, ×3) | ~825 TB (~1 PB) | — |
| Media storage (5y, ×3) | ~16 PB | — |
| Read egress (text) | ~1.2 Gbps | ~3.6 Gbps |
| Hot cache | ~20 GB | — |

**What the numbers told us, before drawing anything:** read-dominated → cache + replicas; media
huge → blob storage + CDN; PB-scale → sharding; 450K read QPS → fan-out feed design. **The
estimate *is* the start of the architecture.**

---

## The reusable cheat-sheet

Pin this. Walk it top to bottom in any estimation question.

**Constants to memorize**

| Thing | Use |
|-------|-----|
| Seconds/day | 86,400 → round to **100,000 (10⁵)** |
| Seconds/month | ~2.6 million |
| Peak factor | **2–5×** (default ~3×) |
| Read:write ratio (social/web) | often **10:1 to 1000:1** — ask! |
| 80/20 rule | cache the hot ~20% of data |
| Replication factor | **3** (standard durability) |
| QPS per commodity server | ~1,000 (conservative) to tens of thousands |
| Connections per server | tens of thousands (for WebSockets) |
| Utilization target | ~**50–70%** (leave headroom) |

**Sizes** (from [Lesson 0.2](02-numbers-every-engineer-should-know.md)): char 1 B · int 4 B · long/timestamp 8 B
· UUID 16 B · tweet/row ~1 KB · web page ~100 KB · photo ~few MB · minute of 1080p ~50–100 MB.

**The checklist**

```
1. CLARIFY  → DAU? actions/user? read:write ratio? item size? retention? media?
2. QPS      → (DAU × actions) ÷ 100,000 = avg ;  × peak factor = peak
3. STORAGE  → items/day × bytes/item × retention days  (× replication!)
4. BANDWIDTH→ QPS × bytes/op   (separate read vs write; egress often dominates)
5. CACHE    → 20% of hot daily data
6. SERVERS  → peak QPS ÷ per-server QPS  (+ headroom);  or connections ÷ per-server
7. SANITY   → is the magnitude believable? does it change the architecture?
```

> **Golden rule of estimation:** *state assumptions, round hard, multiply, sanity-check.* If your
> answer is within ~10× of reality, you've won — it'll lead you to the right architecture.

---

## Trade-offs & key takeaways

- **Estimation precedes design.** The numbers decide single-box vs. sharded fleet, SQL vs. blob
  store, cache or not. Do it *first*.
- **Round aggressively** (86,400 → 10⁵, powers of ten). Speed beats false precision.
- **State every assumption out loud** — it's how interviewers calibrate and correct you.
- **Always design for peak, not average** (2–5× factor), and leave server headroom (~50–70% util).
- **Don't forget replication (×3)** for storage, and that **media dwarfs text** — push it to blob
  storage + CDN.
- **Read:write ratio is the single most clarifying question** for most systems — it drives the
  entire read/cache/replica strategy.

---

## In the wild

- Real capacity planning at scale uses load tests and historical telemetry — but every plan
  *starts* with exactly this envelope math to pick the ballpark.
- Cloud cost models are literally these formulas: egress GB, stored GB-months, request counts.
  Estimating bandwidth and storage up front is estimating your **bill**.
- SREs use **utilization headroom** (the ~50–70% target) so that losing a node or an AZ doesn't
  cascade — a direct application of "design for peak + redundancy."

---

## Interview angle

This is graded on **process, not the final digit.** Narrate it: "I'll assume 300M DAU and a 100:1
read:write ratio — does that sound right? ... so ~1,500 writes/sec average, ~4,500 at peak ... at
100:1 that's ~450K peak reads/sec, which tells me we *must* lean on caching and read replicas."
Tying each number to an architectural consequence is the senior signal.

**Common follow-ups:**
- "What if I told you reads are actually 1000:1?" → recompute read QPS ×10; even more caching, CDN,
  precomputed feeds.
- "How much storage in 5 years, *with* replication?" → ×3 the raw number; don't forget it.
- "How many cache servers?" → size the hot 20%, divide by per-node RAM, add redundancy.
- "Where does the media go?" → blob/object storage + CDN, never the primary DB — and show the media
  size dwarfing the text.

---

## Self-check

1. A photo-sharing app has 50M DAU; each uploads 2 photos/day at ~2 MB. Estimate raw storage per
   day, per year, and over 3 years with 3× replication.
2. Why do we design for **peak** QPS rather than average, and what peak factor is a sane default?
3. You computed 600,000 peak read QPS. Name two architectural moves the number forces, and one
   clarifying question whose answer would change it the most.
4. Reads pull ~40 GB of distinct items per day. Using the 80/20 rule, roughly how much cache RAM do
   you provision, and what does the answer tell you about one node vs. a cluster?
5. Why is 86,400 routinely rounded to 100,000 in this kind of math, and what does that buy you?

---

**Next:** [0.4 — A Framework for System Design Interviews »](04-interview-framework.md)
