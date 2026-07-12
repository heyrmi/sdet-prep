# 3.2 — Storage Engines: B-Tree vs LSM-Tree

> **Module 3 · Distributed Systems** · ~30 min read
> *Every database, under the hood, is just a program that puts bytes on disk and gets them back
> fast. There are two dominant ways to do it — the* B-Tree *(read-optimized, used by Postgres &
> MySQL) and the* LSM-Tree *(write-optimized, used by Cassandra & RocksDB). Understanding the
> difference is understanding why one database is great for your workload and another is a
> nightmare.*

---

## The problem

Your database stores millions of key-value pairs (a row is just a key → its columns). Two things
must be fast:

- **Writes:** "save this new order" — and it must survive a power cut the instant after.
- **Reads:** "find the order with id `5912`" — without scanning all million rows.

The challenge is the **disk**. Recall from [Module 0.2](../00-foundations/02-numbers-every-engineer-should-know.md):
memory is ~100,000× faster than disk, and disks have a quirk — **sequential** writes (laying bytes
down in a continuous streak) are dramatically faster than **random** writes (jumping all over to
update bytes in scattered places). Even on SSDs, random writes cause extra work and wear.

So a storage engine is a balancing act between three forces:
1. **Find data fast** (don't scan everything).
2. **Write data fast** (prefer sequential over random I/O).
3. **Survive crashes** (a confirmed write must not vanish).

> **Analogy.** Imagine keeping a library's records.
> - **B-Tree style:** a meticulously sorted card catalog. To add a card you find its exact slot
>   and insert it — readers always find things instantly, but inserting means physically shuffling
>   to keep order.
> - **LSM-Tree style:** you scribble every change into a notebook as fast as you can (super fast to
>   write), and periodically you stop and *merge* the notebook into the sorted catalog. Writing is
>   effortless; reading might mean checking the notebook *and* the catalog.
>
> Two philosophies: **keep it sorted as you go** vs **dump fast, sort later.**

---

## Core idea

Both engines must let you look up a key without scanning everything, which means keeping data
**sorted** or **indexed**. They differ in *when* they pay the cost of staying sorted:

- **B-Tree:** pays the cost **on every write**, updating data *in place* so the structure is
  always sorted and ready to read. Read-optimized.
- **LSM-Tree:** *defers* the cost. It writes everything sequentially as an append, never updating
  in place, and reorganizes later in the background. Write-optimized.

This single choice — **update-in-place vs append-only** — cascades into every other property.

---

## How it works: the B-Tree

A **B-Tree** organizes data into fixed-size **pages** (typically 4 KB or 8 KB), arranged as a
shallow, wide tree. Each page holds sorted keys plus pointers to child pages. To find a key, you
start at the root and follow pointers down — each step narrows the range, like a binary search
but branching many ways at once.

```
                    ┌─────────────────────┐
       root page    │  [ 30 | 60 | 90 ]   │      (keys partition the range)
                    └──┬─────┬─────┬─────┬─┘
            <30 ┌──────┘   30-60   60-90  └──── >90
                ▼          ▼         ▼          ▼
            ┌───────┐  ┌───────┐ ┌───────┐  ┌───────┐
   leaves   │ 5  12 │  │ 35 50 │ │ 70 85 │  │ 95 99 │   ← actual rows live here
            └───────┘  └───────┘ └───────┘  └───────┘
```

- **Shallow:** with hundreds of keys per page, even billions of rows sit ~3–4 levels deep. A
  lookup is ~3–4 page reads. **Reads are fast and predictable.**
- **In-place update:** to change a value, you find its page and **overwrite it where it sits**.
  This is a *random* write to disk.
- **Splits:** if a page fills up, it splits into two and the parent gets a new pointer. This keeps
  the tree balanced.

### How a B-Tree survives crashes: the WAL

Overwriting a page in place is dangerous — a crash mid-write can corrupt the page (a "torn write").
The fix is the **write-ahead log (WAL)**: before touching the real page, the engine appends the
intended change to a sequential log on disk. If it crashes, on restart it **replays** the WAL to
finish or redo the operation. The WAL is the durability backbone of B-Tree databases.

> You build a WAL yourself in a later assignment — it's a foundational primitive.

**Verdict:** B-Trees give **excellent, predictable reads** and **in-place updates**, at the cost of
**random-write I/O** and **write amplification** (a tiny logical change can rewrite a whole page,
plus the WAL entry).

---

## How it works: the LSM-Tree

**LSM** stands for **Log-Structured Merge-Tree**. Its mantra: *never update in place; only ever
append, and merge later.*

The write path has three pieces:

1. **WAL (commit log):** every write is first appended to a sequential on-disk log (for crash
   recovery), then…
2. **Memtable:** the write goes into an in-memory sorted structure (e.g. a balanced tree / skip
   list). Writes are now **blazing fast** — just memory plus a sequential log append. No random
   disk I/O.
3. **SSTable flush:** when the memtable fills up, it's flushed to disk **in one sequential write**
   as an immutable, sorted file called an **SSTable** (Sorted String Table). Once written, an
   SSTable is **never modified.**

```
   WRITE PATH                                   READ PATH (newest → oldest)

   write ─► WAL (append, durability)            look in memtable ──hit?──► return
        └─► Memtable (in-RAM, sorted)                 │ miss
                 │ full                                ▼
                 ▼ flush (sequential)            check SSTables newest→oldest
            ┌──────────┐                          (bloom filter skips most)
            │ SSTable  │  immutable, sorted             │
            ├──────────┤                                ▼
            │ SSTable  │  older                     found newest version
            ├──────────┤
            │ SSTable  │  oldest
            └──────────┘
```

### Updates and deletes are just appends

You never overwrite. To "update" a key, you append a *new* version to the memtable. To "delete,"
you append a special marker called a **tombstone**. The newest version always wins, and old
versions get cleaned up later. This is why writes are so cheap — they're all sequential appends.

### Reads: the catch

A key might be in the memtable, or in *any* of the SSTables, with the newest version winning. A
naive read could check every SSTable — slow. Two tricks rescue it:

- **Bloom filters.** Each SSTable has a **Bloom filter** — a tiny probabilistic structure that
  answers "is key `K` *possibly* in this file?" with **no false negatives**. If it says "no," you
  *skip that file entirely* without touching disk. This is the single biggest reason LSM reads
  stay fast. (Full treatment: [Module 2.13](../02-building-blocks/13-probabilistic-structures.md).)
- **Sorted files + sparse index.** Within an SSTable, keys are sorted, so once the Bloom filter
  says "maybe," a small in-memory index jumps you near the right spot.

### Compaction: paying the deferred cost

Over time you accumulate many SSTables full of stale versions and tombstones. **Compaction** is a
background process that **merges** several SSTables into fewer, larger ones, keeping only the
newest version of each key and dropping tombstoned data. It's a merge-sort of already-sorted
files — sequential and efficient.

```
   Compaction (merge + dedupe + drop tombstones):

   SSTable A: [a=1, c=3, e=5]  ┐
   SSTable B: [a=9, b=2, e=∅ ] ┼──► merged: [a=9, b=2, c=3]   (a updated, e deleted)
                               ┘
```

Compaction is the LSM's hidden cost: it consumes disk I/O and CPU in the background, and a badly
tuned compaction can cause latency spikes. It's the price you pay for cheap writes.

There are two common compaction strategies, and the choice is itself a trade-off:

- **Size-tiered:** merge SSTables of similar size into a bigger one. Cheaper on write I/O, but you
  end up with several large overlapping files → **higher read and space amplification**.
- **Leveled:** keep files organized into levels of increasing size with non-overlapping key ranges
  per level. **Lower read/space amplification** (a key lives in at most one file per level), at the
  cost of **more write amplification** (data is rewritten as it moves down levels).

Write-heavy workloads often pick size-tiered; read-heavy LSM workloads lean leveled. This single
knob is why "tune your LSM" is a real job — you're trading the three amplifications against each
other for your specific workload.

---

## Read vs write amplification

These two terms explain the whole trade-off. **Amplification** = work the engine actually does
per unit of logical work you requested.

- **Write amplification:** how many *physical* bytes get written per logical byte. B-Trees pay it
  up front (rewrite a full page + WAL on every change). LSM-Trees defer it but pay it during
  **compaction** (the same data gets rewritten each time it's merged to a deeper level).
- **Read amplification:** how many *places* you must check to answer one read. B-Trees are great
  here — one path down the tree. LSM-Trees may consult the memtable plus several SSTables (Bloom
  filters keep this low, but it's still more than a B-Tree).
- **Space amplification:** how much extra disk you use. LSM holds multiple versions and tombstones
  until compaction (extra space, transiently); B-Trees can leave pages partially empty after
  splits and deletes (fragmentation).

The slogan: **B-Trees optimize reads and pay on writes; LSM-Trees optimize writes and pay on reads
(and on background compaction).**

---

## Comparison

| | B-Tree | LSM-Tree |
|---|--------|----------|
| Write style | In-place update (random I/O) | Append-only (sequential I/O) |
| Write speed | Slower (random writes) | **Fast** (sequential, in-memory first) |
| Read speed | **Fast & predictable** (~3–4 page reads) | Good, but may check several SSTables |
| Write amplification | Moderate, paid up front | Deferred to compaction |
| Read amplification | Low | Higher (mitigated by Bloom filters) |
| Space | Fragmentation from splits | Stale versions + tombstones until compaction |
| Crash recovery | WAL replay | WAL replay + immutable SSTables |
| Background work | Minimal | Compaction (CPU/IO spikes) |
| Best for | Read-heavy, point lookups, range scans, transactions | Write-heavy, high ingest, time-series, logs |

### Which databases use which?

| Engine | Databases / stores |
|--------|--------------------|
| **B-Tree** | PostgreSQL, MySQL/**InnoDB**, most traditional RDBMS, MongoDB (WiredTiger default) |
| **LSM-Tree** | **Cassandra**, **RocksDB**, **LevelDB**, ScyllaDB, HBase, InfluxDB, and as a RocksDB-backed engine inside CockroachDB/TiKV |

RocksDB and LevelDB are *embeddable* LSM engines — many bigger databases use one of them as their
storage layer rather than reinventing it.

---

## Trade-offs & key takeaways

- **Match the engine to the workload.** Write-heavy ingest (logs, metrics, events, IoT)? LSM
  shines. Read-heavy with lots of point lookups, range queries, and transactions? B-Tree is the
  comfortable default.
- **LSM trades predictable latency for write throughput.** Compaction can cause occasional latency
  spikes (a tail-latency concern — see [observability](06-observability.md)). B-Trees are steadier.
- **Bloom filters are what make LSM reads viable** — without them, every read could touch every
  SSTable.
- **Both depend on a WAL for durability.** The sequential append-then-confirm pattern is universal:
  "write the intent to a log first, apply later."
- **You don't usually pick the engine directly** — you pick a *database*, which picks the engine.
  But knowing *why* Cassandra eats writes and Postgres excels at complex reads lets you choose the
  right database.

---

## In the wild

- **Cassandra** and **ScyllaDB** use LSM-Trees, which is why they handle enormous write volumes
  (think time-series, event logs) so well.
- **RocksDB** (a Facebook fork of Google's **LevelDB**) is the LSM workhorse embedded inside
  CockroachDB, TiKV, Kafka Streams state stores, and countless others.
- **PostgreSQL** and **MySQL/InnoDB** use B-Trees — the reason they're the go-to for transactional,
  read-rich applications with complex queries and joins.

---

## Interview angle

If asked "how would you store this data?" or "why is Cassandra good for writes but Postgres for
complex reads?", reach for the **B-Tree vs LSM-Tree** distinction. Lead with the core idea —
**update-in-place (read-optimized) vs append-only + compaction (write-optimized)** — then bring in
**write/read amplification** to justify the trade-off, and name **Bloom filters** as the thing that
keeps LSM reads fast. Tying the choice back to the *workload* (read-heavy vs write-heavy) is the
senior signal; reciting the data structure alone is not.

**Common follow-ups:**
- "Why are LSM writes faster than B-Tree writes?" → sequential appends + in-memory memtable vs
  random in-place page updates.
- "Then why aren't LSM reads slow?" → Bloom filters skip most SSTables; sorted files + sparse
  index do the rest.
- "What is compaction and what does it cost?" → background merge of SSTables (dedupe + drop
  tombstones); costs CPU/IO and can cause latency spikes.
- "What guarantees durability in both?" → the write-ahead log: append intent first, apply after,
  replay on crash.

---

## Self-check

1. Why are sequential writes so much cheaper than random writes, and how does each engine exploit
   (or pay) that fact?
2. In an LSM-Tree, what happens on a write, and why is it fast? Where does the deferred cost show
   up later?
3. What problem do Bloom filters solve for LSM reads, and why is "no false negatives" the property
   that matters?
4. You're building a metrics ingestion pipeline (millions of writes/sec, mostly recent reads).
   Which engine, and why?
5. Both engines use a WAL. What is it for, and what happens to it on crash recovery?

---

**Next:** [3.3 — Distributed Transactions (2PC, Saga, Outbox) »](03-distributed-transactions.md)
