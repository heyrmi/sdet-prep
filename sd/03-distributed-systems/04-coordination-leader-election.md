# 3.4 — Coordination & Leader Election

> **Module 3 · Distributed Systems** · ~30 min read
> *Once you have many machines, they constantly need to agree on small but critical facts: who's in
> charge, who holds the lock, where does shard 7 live, what's the current config. Rather than each
> system reinventing consensus, we lean on a* coordination service *— ZooKeeper, etcd, or Consul —
> the reliable shared brain for a cluster.*

---

## The problem

You're running a fleet of worker servers. Exactly **one** of them should run the nightly billing
job — run it on two and you double-charge customers; run it on zero and billing silently stops. So
the workers must **elect a leader** among themselves. But they're separate machines on a flaky
network. How do they pick one, agree on it, and — critically — handle the leader *dying* without
two workers both deciding "I'm the leader now"?

Other facts the fleet must share:
- "Which servers are currently alive?" (membership)
- "Which node owns shard 7?" (where data lives)
- "What's the current feature-flag config?" (everyone must see the same value)
- "Who currently holds the lock on this resource?" (mutual exclusion)

Each of these is a tiny consensus problem. You *could* solve each with a Raft library
([Module 3.1](01-consensus-raft-paxos.md)), but that's a lot of dangerous plumbing to repeat. So
the industry extracted it into a reusable building block.

> **Analogy.** A construction site needs a shared **whiteboard** that everyone trusts absolutely:
> "Today's foreman is Maria," "Crane #2 is reserved by the east crew until noon," "Today's plan is
> version 14." If everyone scribbled on their own notepad, chaos. Instead there's *one*
> authoritative board, kept consistent, that everyone reads from and updates carefully. A
> coordination service is that whiteboard for a distributed system.

---

## Core idea

A **coordination service** is a small, highly-consistent, highly-available datastore whose entire
job is to be the **single source of truth** for cluster-wide facts. Internally it runs a consensus
protocol (Raft or a Paxos variant) across an odd number of nodes (3 or 5), so it survives failures
and never gives two different answers. You then build coordination *primitives* — locks, leader
election, membership — on top of its handful of features.

The crucial mindset shift: **don't store your application data here.** It's deliberately small and
consistency-over-throughput. It holds *metadata about coordination*, not your users' rows.

The three big players:

| Service | Consensus | Notable use |
|---------|-----------|-------------|
| **ZooKeeper** | Zab (Paxos-like) | Kafka (older versions), HBase, Hadoop |
| **etcd** | Raft | Kubernetes' brain; Cloud-native standard |
| **Consul** | Raft | Service discovery + health checking + KV |

They differ in ergonomics, but the **core capabilities** are the same.

---

## How it works: what these services provide

### 1) A consistent key-value store

A simple hierarchical namespace of keys → values, with **linearizable** reads/writes (everyone sees
the same value, in the same order). This is the foundation — you store config, leader identity,
lock state, and membership info as keys.

### 2) Watches

Instead of polling "did this key change yet? did it change yet?", a client registers a **watch** on
a key and the service **pushes a notification** when it changes. This is what makes coordination
*reactive*: the moment the leader key changes, every follower is told.

```
   Client ──watch("/leader")──► Coordination service
                                       │  ("/leader" changes)
   Client ◄──── notification! ─────────┘   → react immediately
```

### 3) Ephemeral nodes (the killer feature)

An **ephemeral node** is a key tied to a client's active **session**. The client holds a session via
periodic heartbeats. **If the client dies or its session expires, the service automatically deletes
that key.** This gives you *automatic failure detection* baked into the data model: a key exists
**if and only if** its owner is alive.

```
   Worker A creates ephemeral key "/workers/A"  (held alive by heartbeat)
   Worker A 💥 crashes → heartbeat stops → session expires
   → service auto-deletes "/workers/A"
   → watchers are notified A is gone
```

This single primitive is the engine behind locks, leader election, and membership.

---

## Distributed locks & the fencing-token problem

A **distributed lock** ensures only one client touches a resource at a time. The naive recipe with
a coordination service:

1. Try to create an ephemeral key `/lock/resource`.
2. Success → you hold the lock. (If the key exists, someone else holds it; watch and wait.)
3. When done (or if you crash), the ephemeral key disappears and the next waiter gets it.

This is clean — failure auto-releases the lock. But there's a deep, subtle danger.

### Why locks aren't enough: the fencing problem

Suppose client A acquires the lock, then **pauses** — a long GC pause, or it gets stuck on a slow
network call. The coordination service, hearing no heartbeat, decides A is dead and **expires its
lock**. Client B now legitimately acquires the lock. *Then A wakes up*, still believing it holds
the lock, and writes to the resource — at the same time as B. **Two writers. Corruption.**

```
   A acquires lock ──► A pauses (GC) ──────────────► A resumes, writes! ✗
                            │ lock expires
                            ▼
                       B acquires lock ──► B writes ✗   (now both write — conflict!)
```

The fix is a **fencing token**: each time the lock is granted, the service hands out a
**monotonically increasing number**. Every write to the protected resource must include its token,
and the resource **rejects any write with a token lower than the highest it has seen.**

```
   A gets token 33, pauses.
   B gets token 34, writes with 34 → storage records "highest = 34".
   A wakes, writes with 33 → storage sees 33 < 34 → REJECTED. ✗ blocked.
```

Now A's stale write is harmlessly rejected. The lesson: **a lock alone doesn't guarantee mutual
exclusion under pauses; you need fencing at the resource.** This is a favorite interview "gotcha."

---

## Leader election patterns

Leader election is a lock with a name. Common recipes built on the primitives above:

- **Ephemeral-node lock.** Whoever successfully creates `/leader` (an ephemeral key) *is* the
  leader. When that node dies, the key vanishes and the watchers race to recreate it. Simple but
  can cause a **herd** (everyone wakes at once).
- **Sequential ephemeral nodes (the "lock queue").** Each candidate creates an ephemeral key with a
  monotonic sequence number (`/election/n_0001`, `/n_0002`, …). The **lowest** number is the
  leader. Each node watches only the node *just ahead of it*, so when the leader dies, only the
  *next* node wakes — no herd. This is ZooKeeper's recommended recipe.

```
   /election/
     n_0001  ← lowest = LEADER
     n_0002  ← watches n_0001 only
     n_0003  ← watches n_0002 only

   n_0001 dies → only n_0002 is notified → becomes leader. No stampede.
```

Always pair leader election with **fencing tokens** so a deposed-but-paused leader can't act.

---

## Service discovery & config management

These are the other two everyday jobs:

**Service discovery.** In a dynamic fleet, instances come and go constantly (autoscaling,
deploys, crashes). How does service X find a healthy instance of service Y? Each instance
**registers** itself (often as an ephemeral key, with a health check); clients **look up / watch**
the list of healthy instances. When an instance dies, its ephemeral entry disappears and callers
stop routing to it. Consul specializes in this (with built-in health checks and DNS).

```
   Service Y instances register ──► [ Consul/etcd ]
   Service X asks "healthy Y?"  ──►  returns live list (dead ones auto-removed)
```

**Config management.** Store configuration (feature flags, limits, connection strings) as keys.
Services **watch** them and update live — flip a flag once, every service reacts within
milliseconds, with everyone guaranteed to see the *same* value (no split config). Far safer than
redeploying or each service reading a possibly-stale local file.

> **A worked example tying it together.** A sharded database wants exactly one node responsible for
> compaction per shard. Each node creates an *ephemeral sequential* key under `/compactor/shard-7/`;
> the lowest sequence number wins and becomes that shard's compactor, receiving **fencing token 41**.
> It watches the node just ahead of it. If the winner suffers a long GC pause, its session expires,
> its ephemeral key vanishes, the next node is notified and takes over with **token 42**, and writes
> the compaction-progress marker. When the paused node wakes and tries to write with token 41, the
> shard rejects it (`41 < 42`). One service, four primitives — ephemeral nodes, sequential ordering,
> watches, and fencing — composing into a safe, self-healing leader. That composition *is* the value
> of a coordination service.

---

## The split-brain risk

**Split-brain** is the cardinal sin of coordination: a network partition splits the cluster, and
**both halves elect their own leader**, each accepting writes, diverging into two conflicting
histories that can't be merged.

```
        ┌─── partition ───┐
   [N1 N2]            [N3 N4 N5]
   "we elect L=N1!"   "we elect L=N3!"
     ✗ two leaders, two truths → corruption
```

The defense is exactly the quorum logic from [3.1](01-consensus-raft-paxos.md): a leader is only
valid if it holds a **majority**. In a 5-node cluster split 2-vs-3, the 2-node side **cannot** reach
a majority and refuses to lead; only the 3-node side proceeds. Because two majorities must overlap,
**at most one leader can exist.** This is why coordination services use an **odd number of nodes**
and why they choose **consistency over availability** — the minority side goes *unavailable* rather
than risk a second leader. Fencing tokens are the second line of defense if a stale leader lingers.

---

## Trade-offs & key takeaways

- **Don't reinvent consensus** — use etcd/ZooKeeper/Consul for coordination. But also **don't
  overuse it**: it's a small, consistency-first store, not your application database. Putting it on
  the hot path of every request makes *it* your bottleneck and SPOF.
- **Ephemeral nodes + watches** are the magic that turns a KV store into locks, leader election, and
  membership with automatic failure detection.
- **A lock is not enough — fencing tokens are.** Pauses and clock skew mean a "released" lock can
  still have a stale holder; reject stale tokens at the resource.
- **Odd node counts and majority quorums** prevent split-brain; the minority side sacrifices
  availability to stay correct (CP).
- **Watch config dynamically** instead of polling or redeploying.

---

## In the wild

- **Kubernetes** stores *all* cluster state in **etcd** and uses it for leader election among
  controller-manager replicas.
- **Apache Kafka** historically used **ZooKeeper** for broker membership, controller election, and
  config (newer Kafka replaces it with its own Raft, **KRaft** — note the trend of folding
  coordination *in*).
- **Consul** and **etcd** are go-to choices for **service discovery** and dynamic config in
  microservice and cloud-native stacks. **HashiCorp Vault** uses Consul/etcd for HA coordination.

---

## Interview angle

When your design needs "exactly one of these," "where does X live," or "flip this everywhere at
once," reach for a **coordination service (etcd/ZooKeeper/Consul)** rather than hand-rolling it.
Show you understand *why*: it runs **consensus internally** (so it's consistent and HA), offers
**ephemeral nodes + watches** (auto failure detection + reactive updates), and uses **majority
quorums** to prevent **split-brain**. The standout senior moment is volunteering the **fencing
token** problem unprompted — "a distributed lock alone isn't safe under a GC pause; I'd add a
monotonic fencing token the resource validates."

**Common follow-ups:**
- "How do you do leader election?" → ephemeral sequential nodes; lowest sequence wins; each watches
  the one ahead to avoid a herd.
- "Client holding the lock pauses for 30s — what breaks?" → its lock can expire and another client
  acquires it; the paused client's later write must be fenced out by token.
- "How do you avoid two leaders during a partition?" → majority quorum; minority can't lead; odd
  node count; CP trade-off.
- "Should you store app data in ZooKeeper/etcd?" → no; it's for small coordination metadata, not
  throughput.

---

## Self-check

1. What does an **ephemeral node** give you that a plain key doesn't, and how do locks and leader
   election exploit it?
2. Explain the **fencing-token** problem: why is acquiring a distributed lock *not* sufficient for
   mutual exclusion, and how does the token fix it?
3. Why do coordination services use an odd number of nodes, and what do they sacrifice during a
   network partition?
4. How does the sequential-ephemeral-node leader election avoid a "herd" when the leader dies?
5. Why is it a mistake to store your application's bulk data in etcd or ZooKeeper?

---

**Next:** [3.5 — Failure, Redundancy & Fault Tolerance »](05-fault-tolerance.md)
