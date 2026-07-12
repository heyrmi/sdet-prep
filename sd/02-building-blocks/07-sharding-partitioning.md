# 2.7 — Sharding & Partitioning

> **Module 2 · Building Blocks** · ~32 min read
> *Replication makes copies of the whole dataset. Sharding does the opposite — it **splits** the
> dataset so no single machine has to hold or write all of it. It's how you scale **writes and
> storage** past one box. It's also one of the hardest, most irreversible decisions you'll make.*

---

## The problem

You've replicated your database, so reads are fast and you survive crashes. But two walls remain:

1. **Storage.** Your data is 50 TB. No single machine holds 50 TB comfortably (and every replica
   would need to hold *all* 50 TB — replication doesn't help here; it copies the whole thing).
2. **Writes.** Every write still goes to the **one leader**. Replication scaled reads, but a single
   leader can only absorb so many writes per second.

Both walls have one fix: stop putting all the data on one machine. **Split it.** Give each machine a
**subset** of the data. Now ten machines hold 5 TB each and absorb a tenth of the writes each. That
split is **partitioning**, and when partitions live on separate machines, we call it **sharding**.

> **Analogy.** A library with too many books for one room. Replication is photocopying the *entire*
> library into several rooms — great for letting more people read at once, useless for the
> *capacity* problem (each room still needs space for every book). **Sharding** is splitting the
> collection across rooms: A–F in room 1, G–M in room 2, and so on. Each room holds a fraction. The
> catch: now you need a clear rule for *which room a book is in* — and woe to you if everyone wants
> a book from the same room at once.

---

## Core idea: vertical vs horizontal partitioning

"Partitioning" gets used two ways. Pin them down:

- **Vertical partitioning** — split a table by **columns**. Put hot, small, frequently-read columns
  in one place and big, rarely-read columns (a giant `bio` text, a blob) in another.

```
  Original users table
  ┌────┬───────┬────────┬──────────────────────┐
  │ id │ name  │ email  │ huge_profile_blob     │
  └────┴───────┴────────┴──────────────────────┘
        split vertically ↓
  ┌────┬───────┬────────┐     ┌────┬──────────────────────┐
  │ id │ name  │ email  │     │ id │ huge_profile_blob     │
  └────┴───────┴────────┘     └────┴──────────────────────┘
       hot, frequently read         cold, rarely read
```

- **Horizontal partitioning (sharding)** — split a table by **rows**. Each shard holds a *subset of
  the rows*, with the **same columns**. This is what people almost always mean by "sharding," and
  it's the rest of this lesson.

```
        split horizontally (by rows) ↓
  ┌── Shard A ──┐  ┌── Shard B ──┐  ┌── Shard C ──┐
  │ users 1–999 │  │ users 1000- │  │ users 2000- │
  └─────────────┘  └─────────────┘  └─────────────┘
```

The column that decides which shard a row goes to is the **shard key** (a.k.a. partition key).
Choosing it is the most consequential decision in the whole topic — everything below circles back to
it.

---

## Sharding strategies

How do you map a row to a shard? Three classic strategies.

### 1) Range-based sharding

Assign **contiguous ranges** of the shard key to shards. E.g. users `A–H` → shard 0, `I–P` → shard
1, `Q–Z` → shard 2.

```
  key:  A........H | I........P | Q........Z
        └─ Shard 0 ┘ └─ Shard 1 ┘ └─ Shard 2 ┘
```

- ✅ **Range queries are efficient** ("all users M–O" hit one or two shards). Sorted order
  preserved.
- ❌ **Hotspots are easy to create.** If keys aren't evenly distributed (lots of users whose names
  start with "S", or time-ordered keys where *all new writes* land in the latest range), one shard
  gets hammered while others idle.

### 2) Hash-based sharding

Run the shard key through a **hash function** and use the result to pick a shard, e.g.
`shard = hash(user_id) % N`.

```
  hash(user_id) % 4  →  0 | 1 | 2 | 3
  spreads keys uniformly across 4 shards regardless of their original values
```

- ✅ **Even distribution** — the hash scrambles keys, so load spreads uniformly and hotspots from
  skewed key values disappear.
- ❌ **Range queries are destroyed** — adjacent keys land on random shards, so "users M–O" must
  query *every* shard. And the naive `% N` has a fatal flaw on rebalancing (next section).

### 3) Directory / lookup-based sharding

Keep an explicit **lookup table** that maps each key (or key range) to a shard. The map is the
source of truth.

```
  ┌──────────── Directory ────────────┐
  │  key range    →  shard            │
  │  A–H          →  Shard 0          │
  │  I–P          →  Shard 3          │   ← flexible: any mapping you want
  │  Q–Z          →  Shard 1          │
  └────────────────────────────────────┘
```

- ✅ **Maximum flexibility** — move any key to any shard; rebalance by editing the map; mix
  strategies.
- ❌ The **directory is a new single point of failure** and an extra lookup on every request (so it
  must be highly available and cached).

### Strategy comparison

| Strategy | Distribution | Range queries | Rebalancing | Main risk |
|----------|-------------|---------------|-------------|-----------|
| Range | uneven (skew-prone) | ✅ efficient | split/move ranges | hotspots |
| Hash | even | ❌ hit all shards | hard with `% N` | no range queries |
| Directory | flexible (you choose) | depends on mapping | easy (edit map) | directory is SPOF/extra hop |

---

## Choosing a shard key

The shard key makes or breaks the system. A good shard key has three properties:

1. **High cardinality** — many distinct values, so data can spread finely. (Sharding on a boolean
   gives you two shards forever.)
2. **Even distribution** — values spread load uniformly; no value is wildly more popular.
3. **Aligns with your queries** — most queries should be answerable from a **single shard** by
   including the shard key. Otherwise every query fans out to all shards (slow, expensive).

These can conflict. `user_id` distributes evenly *and* most queries are per-user — often a great
key. But `timestamp` as a shard key is a trap: it has high cardinality yet **all new writes target
the newest shard** (a moving hotspot), and you've created the celebrity problem in disguise.

> **The shard key is effectively permanent.** Changing it means re-sharding the entire dataset —
> a massive, risky migration. Choose it as if you can't change it, because practically you can't.

---

## Hotspots & the celebrity problem

Even a "good" key can fail when **load is skewed by data, not just by key distribution**. The
classic example: shard a social network by `user_id`. Most users are fine — but a celebrity with
50 million followers lives on **one shard**, and every read of their feed hammers **that one
machine**. The shard is balanced by *row count* but melting under *traffic*.

```
   Shard 0   Shard 1   Shard 2 (celebrity!)   Shard 3
   ▓▓        ▓▓        ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓     ▓▓
   ok        ok        🔥 overloaded 🔥          ok
```

This is the **hotspot / celebrity problem.** Mitigations:

- **Add randomness to the key** for hot entities (e.g. `celebrity_id:00`..`:09`) to split their data
  across 10 shards — at the cost of having to gather all 10 on read.
- **Cache hot keys** aggressively so most reads never reach the shard.
- **Dedicate** special handling (or a separate store) for known mega-entities.

No shard key fully escapes skew; you plan for hotspots, you don't eliminate them.

---

## Rebalancing pain

Add a shard (or one fills up) and you must **move data** to it. This is where naive hashing bites.
With `shard = hash(key) % N`, going from `N=4` to `N=5` changes the result for **almost every key**
— so you'd remap and physically move *nearly the entire dataset* at once. Catastrophic.

```
  hash(key)=17:   17 % 4 = 1     →  add a shard  →  17 % 5 = 2     (moved!)
  Changing N reshuffles ~ALL keys, not just the new shard's share.
```

This single problem is so important it has its own lesson and fix:
**[Consistent hashing](08-consistent-hashing.md)** rearranges the math so that adding/removing a
shard moves only a *small fraction* (~1/N) of keys instead of nearly all of them. When you hit
rebalancing pain, that's the door you walk through.

Beyond key remapping, rebalancing is operationally hard regardless: you must move terabytes
**without downtime**, keep reads/writes correct *during* the move, and avoid overwhelming the
network. Live re-sharding is one of the gnarliest operations in distributed databases.

---

## Cross-shard joins & transactions

Here's the price you pay for splitting the data: operations that span shards get **much** harder.

- **Cross-shard joins.** In a single database, joining `orders` to `users` is one SQL statement. If
  `orders` and `users` are sharded by *different* keys, the join touches many machines. You end up
  doing the join in **application code** (fetch from shard A, fetch from shard B, merge), which is
  slower and more error-prone. Mitigation: **co-locate related data** on the same shard by sharding
  them on the same key (shard both by `user_id` so a user's orders sit with the user). Or
  **denormalize** to avoid the join entirely.
- **Cross-shard transactions.** An atomic operation across shards (debit an account on shard 1,
  credit one on shard 2) can no longer use a simple local transaction. You need a **distributed
  transaction** protocol (two-phase commit, or a Saga) — which is slower, can block, and is hard to
  get right. We cover these in
  [Distributed transactions](../03-distributed-systems/03-distributed-transactions.md).

> **The golden rule of shard design:** pick a shard key so that the **vast majority of operations
> stay within a single shard.** Cross-shard work is the tax you're trying to minimize, not solve.

---

## Secondary indexes on sharded data: local vs global

You shard by `user_id`, but you also need to query by `email`. The secondary index on `email` can
be built two ways:

- **Local (document-partitioned) index:** each shard indexes only *its own* rows by email. Writes
  are cheap (one shard). But a query by email doesn't know which shard the user is on, so it must
  **scatter-gather** — ask *every* shard and combine. Cheap writes, expensive reads.

```
  query by email → ask Shard0, Shard1, Shard2, Shard3 → merge   (scatter/gather)
```

- **Global (term-partitioned) index:** maintain one index for `email` across all shards, itself
  partitioned (e.g. by email's hash). A read goes to exactly one index partition (fast). But a
  *write* must update both the data shard **and** the (possibly remote) index partition — costlier
  writes, and keeping them consistent is hard. Expensive writes, cheap reads.

```
  global index on email → one lookup → tells you the exact data shard   (fast read, costly write)
```

| Index type | Read by secondary key | Write cost | Trade-off |
|------------|----------------------|------------|-----------|
| Local | scatter-gather (all shards) | cheap (one shard) | fast writes, slow reads |
| Global | single lookup | costly (data + index) | slow writes, fast reads |

Same recurring theme as plain [indexing](05-indexing.md): you optimize reads or writes, rarely both.

---

## Trade-offs & key takeaways

- **Sharding scales writes and storage** (the thing replication can't). It's the last resort, not
  the first — exhaust caching, replicas, and a bigger box first.
- **Vertical** = split by columns; **horizontal (sharding)** = split by rows.
- **Range** keeps order but risks hotspots; **hash** spreads evenly but kills range queries;
  **directory** is flexible but adds a SPOF/hop.
- **The shard key is near-permanent.** Choose for high cardinality, even distribution, and
  query alignment (most ops on one shard).
- **Hotspots/celebrities** happen even with good keys — plan to mitigate, not eliminate.
- **Rebalancing is painful;** naive `% N` moves almost everything — [consistent hashing](08-consistent-hashing.md)
  is the fix.
- **Cross-shard joins/transactions are the tax** — minimize them by co-locating related data.
- **Secondary indexes** force a local (fast write) vs global (fast read) choice.

---

## In the wild

- **Vitess** (YouTube/MySQL) shards MySQL transparently and powers planet-scale deployments.
- **MongoDB** has built-in sharding with a chosen shard key, balancer, and both range and hash
  strategies — choosing the key is the documented make-or-break decision.
- **Cassandra/DynamoDB** partition by a hash of the partition key and are explicit about avoiding
  hot partitions.
- **Instagram** famously sharded Postgres by user and embeds shard info inside generated IDs so a
  row's home shard is derivable from its ID.

---

## Interview angle

The moment you say "we'll shard," the interviewer's real question is **"on what key, and what
breaks?"** Lead with a shard-key choice and justify it with the three properties (cardinality, even
distribution, query alignment). Then *proactively* raise the pain: **hotspots/celebrity problem**,
**rebalancing** (and name consistent hashing as the fix), and **cross-shard joins/transactions**
(and how you'd co-locate data to avoid them). Stating the costs before you're asked is the senior
signal — anyone can say "shard it."

**Common follow-ups:**
- "You sharded a social app by `user_id`. A celebrity joins — what happens?" → hot shard; mitigate
  with key salting, caching, dedicated handling.
- "You need a 5th shard. With `hash % 4`, what's the problem?" → almost all keys remap → consistent
  hashing.
- "How do you query by email when you sharded by user_id?" → local index (scatter-gather) vs global
  index (single lookup, costly writes).
- "How do you do an atomic transfer across two shards?" → distributed transaction (2PC/Saga) — with
  its blocking/complexity costs; better to co-locate if possible.

---

## Self-check

1. Why does replication fail to solve the storage and write-throughput walls that sharding solves?
2. Compare range vs hash sharding: which preserves range queries, which avoids value-skew hotspots,
   and why can't you easily have both?
3. What three properties make a good shard key? Why is `timestamp` usually a bad one?
4. Explain the celebrity problem and two ways to mitigate it.
5. Local vs global secondary indexes on a sharded table — what does each optimize, and what does it
   cost?

---

**Next:** [2.8 — Consistent hashing »](08-consistent-hashing.md)
