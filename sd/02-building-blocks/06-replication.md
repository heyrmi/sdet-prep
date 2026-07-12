# 2.6 — Replication

> **Module 2 · Building Blocks** · ~30 min read
> *Keeping more than one copy of your data is how systems survive crashes and serve millions of
> reads. But the moment there's more than one copy, a hard question appears: what happens when the
> copies disagree? Replication is the art of managing that disagreement.*

---

## The problem

Your app runs on one database. Two things keep you up at night:

1. **It might die.** A disk fails, the machine reboots, the data center loses power. With one copy,
   you're down — and if the disk is gone, your data is *gone*.
2. **It can't keep up with reads.** Most apps read far more than they write (the homepage, a
   profile, a product page — read thousands of times for every write). One machine can only serve
   so many reads per second.

Both problems have the same fix: **keep multiple copies of the data on multiple machines.** That's
**replication**. But copies introduce a brand-new problem — keeping them in agreement — which is the
real substance of this lesson.

> **Analogy.** A popular recipe. The head chef keeps the *master* copy in a binder (the source of
> truth). Line cooks each get a *photocopy* so they can all cook at once without crowding the
> binder. When the chef tweaks the recipe, the photocopies must be updated — and for a few minutes,
> a cook reading an old photocopy is working from stale instructions. Replication is exactly this:
> copies for capacity and safety, plus the lag and conflicts that come from keeping them in sync.

---

## Core idea

**Replication** = maintaining copies of the same data on multiple nodes. You get three big wins:

- **High availability:** if one node dies, another has the data and can take over.
- **Read scalability:** spread read traffic across many copies.
- **Lower latency:** put a copy geographically near users.

The central challenge is **how writes propagate to the copies** and **what consistency readers see**
in the meantime. Three architectures answer this differently.

---

## Architecture 1: Leader–Follower (master–slave)

The most common setup. One node is the **leader** (a.k.a. primary/master); the rest are
**followers** (replicas/slaves/secondaries).

- **All writes go to the leader.** The leader applies the write, then ships it to the followers.
- **Reads can go to any node** — typically followers, to offload the leader.

```
                writes (only here)
   clients ───────────────────────► [ LEADER ]
       │                                 │ replicates changes
       │ reads (spread out)              ├──────────► [ Follower 1 ]
       └─────────────────────────────────┤
                                          └──────────► [ Follower 2 ]
   reads ◄───────────────────────────────────────────  (followers serve reads)
```

This cleanly separates the **write path** (one leader, no write conflicts) from the **read path**
(many followers, scalable). It's how Postgres, MySQL, MongoDB, and Redis all do replication by
default. The cost: the leader is a **write bottleneck** and a **special node** — if it dies, nobody
can write until a follower is promoted (see *Failover* below).

---

## Architecture 2: Multi-Leader

Multiple nodes accept writes. Common across **data centers** (one leader per region) so writes are
fast and local everywhere.

```
   US users ──writes──► [ Leader (US) ] ◄──── sync ────► [ Leader (EU) ] ◄──writes── EU users
```

- **Win:** writes are local/fast in each region; a region staying up can keep writing even if the
  link between regions breaks.
- **Cost — write conflicts.** Two leaders can edit *the same record* at the same time and only
  discover the clash later. Now you need a **conflict-resolution** strategy: last-write-wins (can
  lose data), application-defined merge, or CRDTs. This is genuinely hard, so multi-leader is used
  selectively (multi-region, offline-capable apps like collaborative editors).

---

## Architecture 3: Leaderless

No special leader. The client (or a coordinator) writes to **several nodes at once** and reads from
**several nodes at once**, using **quorums** to stay consistent. Popularized by Amazon's Dynamo;
used by Cassandra and Riak.

```
   write → sends to N=3 replicas, waits for W=2 acks
   read  → asks N=3 replicas, waits for R=2 answers, takes the newest
   (if R + W > N, a read is guaranteed to overlap the latest write)
```

- **Win:** no single point of failure for writes; tunable consistency via `N`, `W`, `R`; very high
  availability.
- **Cost:** the client deals with conflicting/stale responses; needs **read repair** and
  **anti-entropy** background syncing to converge. We unpack quorums fully in
  [CAP, PACELC & consistency models](09-cap-pacelc-consistency.md).

### Architecture comparison

| Architecture | Who accepts writes | Write conflicts? | Best for | Cost |
|--------------|--------------------|-----------------|----------|------|
| Leader–follower | one leader | none (single writer) | most apps; read scaling | leader is bottleneck + SPOF for writes |
| Multi-leader | several leaders | **yes** — must resolve | multi-region, offline edits | conflict resolution is hard |
| Leaderless (quorum) | any replica | yes — quorums + repair | extreme availability/scale | client-side complexity |

---

## Sync vs async replication: the durability/latency trade-off

Whatever the architecture, when the leader gets a write, **when does it tell the client "done"?**

- **Synchronous:** the leader waits for the follower(s) to confirm they've stored the write, *then*
  acknowledges the client.
  - ✅ **Durable / safe:** the data is on ≥2 machines before the client is told "saved." If the
    leader dies right after, the write survives on the follower.
  - ❌ **Slow:** the client waits for a network round-trip to the follower. If the follower is slow
    or down, the write **stalls**.
- **Asynchronous:** the leader acknowledges the client **immediately**, then ships the write to
  followers in the background.
  - ✅ **Fast:** no waiting on followers; the leader keeps moving even if a follower lags.
  - ❌ **Risky:** if the leader dies *before* the write reaches a follower, that write is **lost** —
    even though the client was told "saved."

```
  SYNC                                    ASYNC
  client → leader → follower (wait ack)   client → leader (ack now!)
         ◄──── ack (slow, safe) ──────             └──► follower (later, maybe lost on crash)
```

This is a fundamental trade-off you'll state explicitly:

> **Synchronous = durability over latency. Asynchronous = latency over durability.**

A common middle ground is **semi-synchronous**: keep *one* follower synchronous (so every
acknowledged write exists on at least two machines) and let the rest catch up asynchronously. You
get most of the safety for a fraction of the latency cost.

---

## Replication lag & its consistency anomalies

With async replication (the common default), there's a window where a write is on the leader but
**not yet** on a follower. That window is **replication lag**. During it, a read from a lagging
follower returns **stale data**. This causes specific, nameable bugs:

### Read-your-writes (read-after-write) consistency

A user updates their profile (write → leader), then immediately reloads (read → a lagging follower)
and sees the **old** profile. *"My change vanished!"*

```
  t0: user writes new name to LEADER         ✅ stored on leader
  t1: user reloads → reads from FOLLOWER      ❌ follower hasn't caught up → shows old name
```

**Fix:** guarantee a user can always read their *own* recent writes — e.g. read from the leader for
a short window after a write, or route a user's reads to a follower known to be caught up. This is
the same anomaly we flagged back in [Scale from zero to millions](../00-foundations/01-scale-zero-to-millions.md).

### Monotonic reads

A user reads a value (from an up-to-date follower → sees new data), then reads again (from a lagging
follower → sees **older** data). Time appears to **run backwards**. *"The comment was there, now it's
gone?"*

**Fix:** **monotonic reads** — ensure a given user always reads from the same follower (or one at
least as current as their last read), so they never go backward in time.

> These aren't exotic. They're the everyday cost of async replication, and naming them in an
> interview shows you understand eventual consistency in practice, not just theory.

---

## Failover & leader promotion (and the split-brain risk)

When the **leader dies**, the system must pick a new one — **failover**:

1. **Detect** the leader is down (usually a timeout — it stopped responding to heartbeats).
2. **Choose** a new leader (often the follower with the most up-to-date data).
3. **Promote** it and **reconfigure** clients/followers to point at the new leader.

This sounds clean but hides nasty problems:

- **Lost writes:** with async replication, writes the old leader accepted but hadn't shipped yet are
  **gone** when a less-current follower is promoted.
- **Split-brain — the scary one.** If the old leader didn't actually crash but was just *unreachable*
  (network glitch), you can end up with **two nodes both believing they're the leader**, both
  accepting writes, diverging. Data corruption follows.

```
   network partition!
        ┌──────────── X ────────────┐
   [ Leader A ]                 [ Leader B ]   ← both think they're leader 😱
   accepts writes               accepts writes
        └────────── diverge ────────┘
```

**Fixes:** a **majority quorum** for electing a leader (a minority partition can't elect one), and
**fencing** (a promoted leader gets a new "epoch" token; the old leader's stale writes are rejected).
Doing failover *correctly* is a consensus problem — which is exactly why systems use
[Raft/Paxos](../03-distributed-systems/01-consensus-raft-paxos.md). Many teams keep a human in the
loop for promotion precisely because automatic failover can trigger split-brain.

---

## How replication actually travels: log shipping

How does a follower learn what the leader did? The leader keeps a **replication log** — an ordered
record of every change — and **ships it** to followers, who **replay** it to reach the same state.
The same write-ahead log that gives a single database durability becomes the stream that keeps
replicas in sync.

| Method | What's shipped | Notes |
|--------|----------------|-------|
| **Statement-based** | the SQL statement itself | compact, but non-deterministic SQL (`NOW()`, `RANDOM()`) diverges |
| **Write-ahead log (WAL) shipping** | the low-level physical change | exact and reliable; tied to storage format/version |
| **Logical / row-based** | the resulting row changes | portable across versions; great for replication *between different systems* (CDC) |

Followers replay the log **in order**, which is why a single-leader system has a clean, total order
of writes — and why followers naturally lag by however long the log takes to travel and apply.

---

## Trade-offs & key takeaways

- **Replication buys availability, read scaling, and lower latency** — at the price of keeping
  copies consistent.
- **Leader–follower** is the default: simple, no write conflicts, scales reads. The leader is a
  write bottleneck and a failover risk.
- **Multi-leader / leaderless** scale writes and availability further but inherit **conflict
  resolution** complexity.
- **Sync vs async = durability vs latency.** Semi-sync is the pragmatic middle.
- **Replication lag** causes real anomalies — **read-your-writes** and **monotonic reads** — with
  known fixes.
- **Failover is dangerous:** lost writes and **split-brain**. Use quorums and fencing; this is a
  consensus problem.
- Replication scales **reads**, not writes/storage — for those you need
  [sharding](07-sharding-partitioning.md).

---

## In the wild

- **PostgreSQL** ships WAL to followers; supports sync and async streaming replication and
  cascading replicas.
- **MySQL** offers async and **semi-synchronous** replication; row- and statement-based binlog
  formats.
- **MongoDB** replica sets do automatic, quorum-based leader election (a built-in consensus
  protocol) to avoid split-brain.
- **Cassandra** is leaderless with tunable quorum reads/writes (`N/R/W`) and read repair.
- **Redis** uses async leader–follower replication with **Sentinel** for failover.

---

## Interview angle

When you propose replicas to "scale reads," immediately volunteer the catch: **replication lag and
read-your-writes consistency.** Saying "I'll add read replicas, but a user who just posted should
read from the leader briefly so they see their own write" shows you understand the second-order
effect. For "what if the database dies?", walk **failover → leader promotion → split-brain risk →
quorum + fencing**. And whenever you say "replication," be ready to pick **sync vs async** and
justify it as **durability vs latency**.

**Common follow-ups:**
- "User updates their profile and sees the old version on reload — why, and how do you fix it?" →
  replication lag; read-your-writes via leader reads or a caught-up follower.
- "The leader crashes — walk me through what happens." → detect, promote most-current follower,
  reconfigure; risk of lost async writes; split-brain if the old leader wasn't really dead.
- "Sync or async replication here?" → durability vs latency; semi-sync as the compromise.
- "Replicas scaled your reads, but writes are still maxed out. Now what?" → replication can't scale
  writes; shard the data.

---

## Self-check

1. Why does replication scale reads but **not** writes? What scales writes instead?
2. Explain the durability/latency trade-off between synchronous and asynchronous replication.
3. What is replication lag, and which two consistency anomalies does it cause? Give a fix for each.
4. What is split-brain, how does it arise during failover, and how do quorums + fencing prevent it?
5. Why can statement-based replication diverge where WAL/row-based replication doesn't?

---

**Next:** [2.7 — Sharding & partitioning »](07-sharding-partitioning.md)
