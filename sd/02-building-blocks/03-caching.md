# 2.3 — Caching (strategies, eviction, pitfalls)

> **Module 2 · Building Blocks** · ~26 min read + coding assignment
> *We've cached at the edge (CDN). Now we cache in the heart of the system — between your app and
> your database — where the right pattern can cut latency 100× and the wrong one can serve stale
> data, stampede your database, or quietly lose writes.*

---

## The problem

Your database is the source of truth, but it's *slow* relative to memory. Many requests ask for
the **same** data over and over — the homepage, a hot product, a celebrity's profile. Re-running
the same query, hitting disk every time, is wasteful and doesn't scale: reads pile up and the DB
becomes the bottleneck ([Module 0, Step 4](../00-foundations/01-scale-zero-to-millions.md)).

A **cache** keeps a copy of frequently-used data in fast storage so most reads never touch the
slow store. The catch — and the entire reason this is a hard topic — is that a cache is a
*second copy* of the truth, and **two copies can disagree.**

> **Analogy.** A cache is the **notepad on your desk**. The filing cabinet (database) is the
> authoritative record, but walking to it for every fact is slow, so you jot the facts you use
> often on the notepad. Lightning fast — until someone updates the filing cabinet and your notepad
> still says the old thing. Now you have to decide: when do you refresh the notepad, and what do
> you erase when it fills up? That's caching in two sentences.

---

## Core idea: why memory beats disk

Caches are fast because they live in **RAM**, and RAM is dramatically faster than disk or a
network round-trip to a database.

```
   Read 1 MB from RAM   ~ microseconds
   Read 1 MB from SSD   ~ ~100× slower
   Round-trip to DB     ~ network + query parse + disk  → milliseconds
```

(See [Module 0.2 — Numbers Every Engineer Should Know](../00-foundations/02-numbers-every-engineer-should-know.md).)
The price of that speed: RAM is **small and volatile** (limited capacity, gone on restart). So a
cache can't hold everything — which forces the two questions that define caching:

1. **How do reads and writes flow** through cache and DB? → *caching strategies.*
2. **What do we throw out when the cache is full?** → *eviction policies.*

---

## Caching strategies (read & write patterns)

A "strategy" is just the rule for *who reads/writes the cache and the DB, and in what order.*

### Cache-aside (lazy loading) ⭐ most common
The application manages the cache directly. On a read: check cache; on a miss, load from DB and
populate the cache. On a write: write the DB and **invalidate** (or update) the cache entry.

```
   READ:  app → cache? HIT → return
                       MISS → DB → put in cache → return
   WRITE: app → DB → delete (invalidate) cache key
```
- ✅ Simple, only-requested data is cached, resilient to cache outages (just slower).
- ❌ First read of any key is a miss (cold cache); a small consistency window if you update
  instead of invalidate.

### Read-through
Like cache-aside, but the **cache library** handles the DB load on a miss, not your app. Your code
just calls `cache.Get(key)` and the cache fetches-and-stores behind the scenes.
- ✅ Cleaner app code; caching logic lives in one place.
- ❌ Needs a cache that supports it; same cold-start misses as cache-aside.

### Write-through
On a write, the cache writes to the DB **synchronously**, then returns. Cache and DB stay in sync.

```
   WRITE: app → cache → DB (synchronously) → return
```
- ✅ Cache is always fresh; reads after a write are correct.
- ❌ Every write pays the DB latency (slower writes); you cache data that may never be read.

### Write-back (write-behind)
On a write, update the **cache only** and acknowledge immediately; flush to the DB **asynchronously**
later (batched).

```
   WRITE: app → cache → return (DB updated later, in the background)
```
- ✅ Very fast writes, absorbs write bursts, batches DB writes.
- ❌ **Risk of data loss** if the cache dies before flushing; more complex. Use only where some
  loss is tolerable or the cache is durable.

### Write-around
On a write, go **straight to the DB and skip the cache**; the cache fills only on later reads
(via cache-aside).
- ✅ Avoids flooding the cache with write-once-read-never data.
- ❌ A read right after a write is a miss (the new data isn't cached yet).

### Refresh-ahead
The cache **proactively reloads** popular entries *before* they expire, so hot keys never go cold.
- ✅ Hides miss latency for predictable hot keys.
- ❌ Wasted refreshes if the prediction is wrong; added complexity.

| Strategy | Who loads on miss | Write path | Best for |
|----------|-------------------|-----------|----------|
| Cache-aside | App | DB then invalidate cache | General purpose (default) ⭐ |
| Read-through | Cache library | DB then invalidate cache | Clean code, lib supports it |
| Write-through | — | Cache → DB (sync) | Read-after-write correctness |
| Write-back | — | Cache now, DB later (async) | Write-heavy, loss-tolerant |
| Write-around | App | DB only (skip cache) | Write-once-read-rarely data |
| Refresh-ahead | Cache (proactively) | (paired with a read strategy) | Predictable hot keys |

A very common production combo: **cache-aside reads + write-around (or invalidate-on-write)**.

---

## Eviction policies: what to throw out when full

RAM fills up. When it does, the cache must evict something to make room. The policy decides *what*.

- **LRU (Least Recently Used) ⭐** — evict the entry untouched for the longest. Great default;
  assumes recently-used things will be used again (temporal locality). *This is what you'll build.*
- **LFU (Least Frequently Used)** — evict the entry with the fewest accesses. Keeps long-term
  popular items but needs counters and can cling to once-hot items that have gone cold.
- **FIFO (First In First Out)** — evict the oldest-inserted, regardless of use. Simple, but a
  popular item inserted long ago gets dumped even if it's hot. Rarely ideal.
- **TTL (Time To Live)** — every entry expires after a fixed time. Often *combined* with the
  others: TTL bounds staleness, LRU/LFU bounds memory.

```
   LRU:  ... [B][A][C][D]  ← access A → ... [B][C][D][A]; evict from the LEFT (least recent)
   LFU:  evict the entry with the smallest hit counter
   FIFO: evict in insertion order, like a plain queue
   TTL:  entry vanishes N seconds after it was written, no matter what
```

| Policy | Evicts | Pro | Con |
|--------|--------|-----|-----|
| LRU | Least recently used | Matches temporal locality; cheap | Mishandles one-off scans of many keys |
| LFU | Least frequently used | Keeps long-term hot items | Counters; slow to forget old favorites |
| FIFO | Oldest inserted | Trivial | Ignores actual usage |
| TTL | Anything past its age | Bounds staleness | Picks by time, not value |

---

## Cache invalidation: keeping copies honest

The cache is a *second copy*, so when the DB changes, the cached copy is now wrong (**stale**).
Three broad approaches:

- **TTL expiry** — let entries auto-expire. Simple; staleness is bounded by the TTL. The most
  common answer.
- **Explicit invalidation on write** — when you update the DB, delete (or overwrite) the cache key.
  Fresher, but you must remember it everywhere, and there's a race: another thread can re-cache
  the old value between your DB write and your delete.
- **Versioning** — bake a version/hash into the key so new data uses a new key and old entries are
  simply never read again (the CDN trick from [Module 2.2](02-reverse-proxy-cdn.md)).

> **The honest truth:** there's no cheap way to guarantee every cached copy is fresh at all times.
> You choose how much staleness you tolerate (TTL length) and how much complexity you'll pay
> (explicit invalidation). It's a trade-off, not a solved problem — hence the famous joke.

---

## The famous pitfalls (and how to survive them)

### Cache stampede / thundering herd
A hot key expires (or the cache restarts). Suddenly **thousands of requests miss at once**, all
rush to the database for the same key, and the DB — which the cache was protecting — falls over.

```
   key "homepage" expires at 12:00:00
   12:00:00.001 → 5,000 requests all MISS → 5,000 identical DB queries → 💥
```

Mitigations:
- **Request coalescing (single-flight).** Let only the *first* misser hit the DB; everyone else
  waits for that one result and shares it. (Go's `singleflight` does exactly this.)
- **Locking.** The first misser takes a lock to recompute; others briefly serve stale or wait.
- **TTL jitter.** Don't expire many keys at the same instant — add randomness (e.g. `TTL ±10%`) so
  expirations spread out instead of synchronizing into a herd.
- **Refresh-ahead** for known-hot keys, so they're refreshed *before* they ever expire.

### Hotspot keys (hot partition)
One key is *so* popular that the single cache node holding it becomes a bottleneck (the celebrity
profile everyone loads). Mitigations: **replicate** the hot key across nodes, add a tiny local
in-process cache in front of the shared cache, or split the value into shards.

### Other classics
- **Cache penetration** — requests for keys that *don't exist* always miss and always hit the DB.
  Fix: cache the "not found" result (a negative cache) or front it with a
  **Bloom filter** ([Module 2.13](13-probabilistic-structures.md)).
- **Stale reads** — accepted as a trade-off when you choose TTL-based invalidation.

---

## Distributed caching: when one cache node isn't enough

A single cache server has limited RAM and is a SPOF. At scale you run a **cluster** of cache nodes
and spread keys across them. The obvious "`hash(key) % N`" assignment breaks horribly when `N`
changes (add one node and *almost every key* moves, causing a mass miss storm). The standard fix is
**consistent hashing**, which moves only a small fraction of keys when the cluster changes — it has
its own full lesson in [Module 2.8](08-consistent-hashing.md). For now, just know: *distributed
caches use consistent hashing to decide which node owns each key.*

```
   Naive:  node = hash(key) % N      → change N → ~all keys remap (miss storm)
   Better: consistent hashing        → change N → ~1/N of keys remap
```

---

## Redis vs Memcached

The two dominant in-memory caches. Both are fast key-value stores; the difference is breadth.

| | **Redis** | **Memcached** |
|---|-----------|---------------|
| Data types | Rich (strings, hashes, lists, sets, sorted sets, streams) | Strings/blobs only |
| Persistence | Optional (snapshots, append-only log) | None (pure cache) |
| Replication / HA | Built-in (replicas, Sentinel, Cluster) | Not built-in |
| Threading | Mostly single-threaded core | Multi-threaded |
| Extra powers | Pub/sub, Lua scripts, atomic ops, TTLs, rate limiting | Dead-simple, multi-threaded throughput |
| Pick when | You want features, structures, persistence, HA | You want a plain, blazing key-value cache |

Rule of thumb: **default to Redis** (you'll likely want one of its features eventually); reach for
Memcached when you truly only need a simple, multi-threaded, sharded blob cache. Note Redis's
atomic ops and Lua scripting are exactly what made the distributed
**[rate limiter](../04-case-studies/01-rate-limiter/)** safe — caches are general-purpose tools.

---

## Trade-offs & key takeaways

- **A cache is a second copy of the truth** — speed bought with the risk of staleness.
- **Cache-aside is the default** read/write pattern; choose write-through for read-after-write
  correctness, write-back for fast loss-tolerant writes, write-around for write-once data.
- **LRU is the default eviction policy;** LFU for long-term popularity, TTL to bound staleness
  (often combined).
- **Invalidation is the hard part** — pick a staleness budget (TTL) and decide if you also
  invalidate on write.
- **Stampede, hotspot keys, and penetration** are the famous failure modes — know coalescing,
  TTL jitter, replication, and negative caching.
- **Distributed caches need consistent hashing** to avoid miss storms when nodes change.
- **Redis = features; Memcached = simplicity.** Default to Redis.

---

## In the wild

- **Redis** powers caching, sessions, leaderboards, and rate limiting almost everywhere; managed
  as AWS ElastiCache / Google Memorystore / Azure Cache.
- **Facebook** famously scaled **Memcached** to enormous fleets and wrote about the stampede and
  consistency problems that come with it.
- **Go's `golang.org/x/sync/singleflight`** is the canonical request-coalescing tool for beating
  stampedes in app code.

---

## Interview angle

When reads dominate, **add a cache** and immediately name the pattern (**cache-aside**) and the
eviction policy (**LRU**). Score points by raising **invalidation** and the **freshness vs
hit-ratio** trade-off before you're asked. The senior signals: anticipate **cache stampede**
("a hot key expires and the DB gets hammered") with **coalescing + TTL jitter**, mention
**hotspot keys**, and note that a *distributed* cache relies on **consistent hashing**. Close by
choosing **Redis vs Memcached** with a reason.

**Common follow-ups:**
- "The cache restarts and every request hits the DB at once — what happens and how do you prevent
  it?" → thundering herd; coalescing, jitter, warm standby.
- "User updates their profile but still sees the old one — bug or trade-off?" → stale read from
  TTL/eventual invalidation; a deliberate trade-off.
- "How do you add a cache node without a miss storm?" → consistent hashing.
- "Write-through vs write-back?" → sync correctness vs async speed + loss risk.

---

## Practice → the Go assignment

You learn caching by **building the eviction engine that powers it.** Your assignment is an
**LRU cache** in Go with **O(1) `Get` and `Put`** — the exact structure behind "Step 4: Add a
cache" in [Module 0](../00-foundations/01-scale-zero-to-millions.md) and behind every LRU policy
in the table above.

Go to **[`03-caching-assignment/`](03-caching-assignment/)** and implement, in `assignment/lru.go`:

1. `NewLRU(capacity int) *LRU` — a capacity-bounded cache.
2. `Get(key string) (int, bool)` — return the value and `true` on a hit (and mark it
   most-recently-used), or `(0, false)` on a miss.
3. `Put(key string, value int)` — insert/update, mark most-recently-used, and **evict the
   least-recently-used entry** when full.

The trick (spelled out in the starter comments) is the classic combo:

```
   hash map:           key -> *node                       (O(1) lookup)
   doubly linked list: MRU <-> ... <-> LRU  (sentinels)   (O(1) reorder + evict)
```

Every access moves a node to the **front**; eviction removes from the **back**. The cache is
**mutex-protected**, and the suite includes a **`-race`** concurrency test — so all shared-state
access must be under the lock.

```bash
cd 02-building-blocks/03-caching-assignment/assignment
go test ./...          # red → implement → green
go test -race ./...    # must pass: the cache is shared across goroutines
```

The interface is given; you fill in the `// TODO`s. A reference solution is in
[`03-caching-assignment/solution/`](03-caching-assignment/solution/) — try first, peek after.

---

## Self-check

1. You add a cache in front of the DB with cache-aside. A user updates a row. What two ways can the
   cached value become correct again, and what's the trade-off between them?
2. Write-through vs write-back: which risks losing data, which slows down every write, and why?
3. Your cache is full. Under LRU vs FIFO, which is more likely to keep a frequently-used item that
   happened to be inserted long ago?
4. A single hot key expires and 10,000 requests stampede the DB. Name two independent mitigations
   and what each one does.
5. Why does `hash(key) % N` make adding a cache node painful, and what replaces it?

---

**Next:** [2.4 — Databases: SQL vs NoSQL »](04-sql-vs-nosql.md)
