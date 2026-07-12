# 4.2 — Design a Distributed Key-Value Store

> **Module 4 · Case Studies** · ~35 min read + coding assignment
> *Concepts exercised:* consistent hashing (ring + virtual nodes), replication,
> quorum consistency (W + R > N), tunable consistency, conflict resolution (vector
> clocks vs last-write-wins), gossip membership, hinted handoff, the
> CAP/availability-vs-consistency trade-off.

---

## The problem

A **key-value store** is the simplest possible database: you `put(key, value)` and
later `get(key)`. No tables, no joins, no query language — just a giant distributed
hash map. Think Amazon's shopping cart, a session store, or a feature-flag service.

The catch is the word **distributed**. One machine can't hold all the data, can't
serve all the traffic, and *will* eventually crash. So we spread the data across many
machines and keep copies, and suddenly we face every hard problem in distributed
systems at once: *Which* machine owns a key? What happens when a machine dies? What
if two clients write the same key at the same time on different machines?

This case study is the home for **consistent hashing** — the technique that decides
which machine owns which key, and (crucially) lets you add and remove machines
without reshuffling almost all your data. It's modeled on Amazon's **Dynamo** paper,
the design that inspired Cassandra, Riak, and DynamoDB.

> **Analogy.** Imagine a library with millions of books spread across many branches.
> You need a rule for *which branch holds which book* so anyone can find a book
> without searching every branch. A bad rule: "branch number = book ID mod (number
> of branches)." It works — until you open one new branch, and now the *mod* changes
> for almost every book, forcing you to physically move nearly the entire collection.
> Consistent hashing is the smart rule that only moves a small slice of books when a
> branch opens or closes.

---

## Step 1: Requirements (always start here)

**Functional**
- `get(key) -> value` and `put(key, value)`.
- Keys and values are opaque blobs (often size-limited, e.g. ≤ 10 KB).
- Data is spread across many nodes (no single machine holds everything).

**Non-functional** (these drive every design decision)
- **High availability** — writes and reads should (almost) always succeed, even
  during node failures. Dynamo famously chose "always writable" (your shopping cart
  must accept items even mid-failure).
- **Scalability** — add nodes to grow capacity; the system rebalances smoothly.
- **Tunable consistency** — let the *caller* trade consistency for latency/availability.
- **Partition tolerance** — the network *will* split; the system must keep working.

**Clarifying questions to ask the interviewer**
- *Read-heavy or write-heavy?* Shapes replica counts and caching.
- *How strong does consistency need to be?* Strong (banking) vs eventual (cart) is the
  single biggest fork in the design.
- *Value sizes?* Small blobs vs large objects change the storage engine.
- *Single datacenter or global?* Cross-region replication adds latency and conflicts.

> **The CAP reminder.** During a network partition you can have **C**onsistency or
> **A**vailability, not both. Dynamo-style stores deliberately pick **A** (stay up,
> reconcile later). A system like a SQL primary picks **C** (refuse writes rather than
> diverge). Neither is "right" — it's the central trade-off. (See Module 2.9.)

---

## Step 2: Estimation (back-of-envelope)

Say we're storing 100 million users' session data, ~1 KB each.

- **Raw data:** 100M × 1 KB = **100 GB**. With **3× replication** → **300 GB**.
- Comfortably fits on a handful of commodity nodes (say 6 nodes × 100 GB), but we'll
  want more for headroom, throughput, and to tolerate failures.
- **Traffic:** 100M sessions, 10 reads + 1 write per session per day
  ≈ 1.1B ops/day ≈ **~13k ops/sec average**, with peaks several times higher. A
  single node does maybe a few thousand ops/sec → we need to **shard across nodes**
  for throughput, not just storage.

Conclusion: we need **partitioning** (for both capacity and QPS) and **replication**
(for durability and availability). That's the whole game.

---

## Step 3: High-level design

### API sketch

```
put(key, value)            -> ok / error
get(key)                   -> value (or "not found", or a set of conflicting values)
```

Optionally the API exposes consistency knobs per request:

```
put(key, value, W)   // wait for W replicas to ack before returning success
get(key, R)          // read from R replicas and reconcile
```

### Data model

It's literally a map. Internally each node persists its slice of the map to disk
(typically an **LSM-tree** — see Module 3.2 — because writes are cheap and sequential).

### Component diagram

Every node is identical — there is **no leader, no central router**. A client can
talk to *any* node, which acts as the **coordinator** for that request: it figures out
which nodes own the key and forwards reads/writes to them.

```
                 ┌─────────── client ───────────┐
                 │  put("cart:42", ...)          │
                 ▼                               
        ┌──────────────────┐
        │  Node A (coord.)  │  hashes key -> finds owners on the ring
        └────────┬─────────┘
                 │ replicate to N owners (here N=3)
      ┌──────────┼───────────┐
      ▼          ▼           ▼
  ┌───────┐  ┌───────┐  ┌───────┐
  │ Node C│  │ Node D│  │ Node E│   ← the 3 consecutive ring nodes that own this key
  └───────┘  └───────┘  └───────┘
      ▲          ▲           ▲
      └────── gossip (membership + health) ──────┘   all nodes, peer-to-peer
```

The two big questions are **(1) which nodes own a key** (partitioning) and
**(2) how many copies and how we keep them consistent** (replication + quorum). The
rest of this lesson is those two deep dives.

---

## Step 4: Partitioning with consistent hashing (the core)

### Why not just `hash(key) % N`?

The obvious scheme: number your nodes 0..N-1, and store each key on node
`hash(key) % N`. It balances load beautifully. It also breaks catastrophically the
moment `N` changes. Add one node (N: 4 → 5) and the modulus changes for *almost every
key* — measured below, ~80% of keys would need to move to a different node. For a
300 GB dataset that's a storm of data shuffling that can take the cluster down.

### The ring

Consistent hashing fixes this. Picture the output of the hash function as a **circle**
(a "ring") of values, say 0 to 2³²-1, wrapping around at the top.

1. **Place each node on the ring** by hashing its name: `hash("node-a") -> some point`.
2. **Place each key on the ring** by hashing the key.
3. A key is owned by the **first node you meet going clockwise** from the key's point.

```
                hash space wraps 0 ── 2^32-1
                         ┌──────────────┐
                    A ●  │              │
                   ╱     │              │  ● B
        key k ────►•     │   (ring)     │
                   ╲     │              │
                    C ●  │              │  ● D
                         └──────────────┘
   k hashes here, walk clockwise → first node hit is B → B owns k
```

Now add a new node E. It lands somewhere on the ring and steals **only the keys
between its predecessor and itself** — every other key stays exactly where it was.
**Adding/removing a node only remaps ~1/N of keys, not all of them.** That's the
entire magic.

### The balance problem → virtual nodes

With only a few nodes placed at random points, the ring is **lumpy**: one node might
own a huge arc and another a tiny sliver, so load is uneven. Worse, when a node dies,
*all* its load dumps onto its single clockwise neighbor.

The fix: **virtual nodes** (a.k.a. "vnodes" or "replicas" — confusingly *not* the same
as data replicas). Each physical node is placed on the ring **many times** (e.g. 100–200
points) under names like `node-a#0`, `node-a#1`, …. With hundreds of small arcs per
node averaged out:

- Load is **smooth** across physical nodes.
- When a node dies, its many small arcs are absorbed by **many different** neighbors,
  spreading the load instead of crushing one machine.

| Knob | Effect of increasing it |
|------|--------------------------|
| Number of physical nodes (N) | More capacity & throughput; smaller fraction remapped per change |
| Virtual nodes per physical node | Smoother load balance; faster, more even rebalancing — but more memory and slightly slower lookups |

> **Trade-off.** More vnodes = better balance but more ring entries to store and sort.
> A few hundred per node is a common sweet spot. Your assignment lets you *measure*
> the balance directly.

### Lookups are a binary search

The ring is stored as a **sorted array of hash points**. To find a key's owner: hash
the key, then **binary-search** for the first ring point ≥ that hash (wrapping to index
0 if you fall off the end). That's `O(log V)` where V is the total number of vnodes.
This is exactly what you'll implement.

---

## Step 5: Replication

Owning a key on one node isn't durable — that node can die. So we store each key on
**N consecutive nodes** walking clockwise from the key's position (N is the
**replication factor**, commonly 3).

```
key k → walk clockwise → first DISTINCT physical nodes: B, D, A  (N=3)
        these three are the "preference list" for k
```

Note the subtlety: as you walk the ring you'll hit **multiple vnodes of the same
physical node**; you must skip duplicates and collect N *distinct physical* nodes.
(Your `GetNodes` does exactly this.) The first node is the **primary**; the rest are
replicas. This set is Dynamo's **preference list** for the key.

---

## Step 6: Consistency via quorum (tunable)

Now there are N copies. When do we call a write "done," and how do we read?

Define three numbers:
- **N** — number of replicas.
- **W** — write quorum: how many replicas must ack before a write returns success.
- **R** — read quorum: how many replicas we read from and reconcile.

The key inequality: **if `W + R > N`, the read and write sets are guaranteed to
overlap**, so any read sees at least one replica that has the latest write — that's
**strong-ish consistency**. If `W + R ≤ N`, you might read stale data — **eventual
consistency**, but faster.

| Config (N=3) | Behavior | Good for |
|--------------|----------|----------|
| W=3, R=1 | Writes slow & durable; reads fast | Read-heavy, can't lose writes |
| W=1, R=3 | Writes fast & always-available; reads slow | Write-heavy (Dynamo cart) |
| W=2, R=2 | Balanced; W+R=4 > 3 → overlap guaranteed | The common default ⭐ |
| W=1, R=1 | Fastest, weakest; W+R=2 ≤ 3 → may read stale | Latency-critical, tolerant of staleness |

> **Trade-off in one line.** Bigger W/R = stronger consistency but higher latency and
> lower availability (more replicas must be reachable). The beauty of Dynamo-style
> stores is the caller **tunes this per request**.

---

## Step 7: Conflict resolution

With W=1 (or during partitions), two clients can write the *same key on different
replicas* with neither seeing the other. Now two replicas disagree. Who wins?

**Option A — Last-Write-Wins (LWW).** Tag each write with a timestamp; the highest
timestamp wins. Dead simple, but **silently drops** the "losing" write, and depends on
synchronized clocks (which lie — see Module 4.3 on clock skew). Cassandra defaults to
LWW.

**Option B — Vector clocks.** Attach a small map `{node -> counter}` to each value that
records the causal history of writes. On read, you can tell whether one version
**descends from** another (keep the newer) or whether they **truly conflict**
(concurrent edits). For genuine conflicts the store returns *both* versions and lets
the **application merge** them (Dynamo's shopping cart merges by unioning items, so
nothing is lost).

| Method | Pros | Cons |
|--------|------|------|
| Last-write-wins | Trivial, O(1) | Loses data on conflict; trusts clocks |
| Vector clocks | Detects true conflicts; no lost updates | More metadata; app must merge; vectors can grow |

> Conceptually: LWW asks "which is newer?"; vector clocks ask "are these even
> comparable?" That distinction is the senior-level point to make.

---

## Step 8: Membership & failure handling

**How do nodes know who's in the cluster?** With no central coordinator, nodes use
**gossip**: periodically each node picks a few random peers and exchanges
"here's who I think is alive and their ring positions." Like rumor spreading,
membership and health info reaches everyone in `O(log N)` rounds, with no single point
of failure.

**Temporary failures → hinted handoff.** Suppose a write should go to nodes B, D, A but
D is briefly down. Rather than fail the write (we promised high availability!), the
coordinator sends D's copy to a **substitute** node with a "hint" that it belongs to D.
When D recovers, the substitute hands the data back. The write succeeded; the cluster
healed itself.

**Permanent failures → replica sync.** To repair replicas that have silently drifted,
nodes compare data efficiently using **Merkle trees** (hash trees): they exchange top
hashes and only dig into subtrees that differ, so they transfer just the keys that are
actually out of sync instead of comparing everything.

---

## In the wild

- **Amazon Dynamo** — the original paper (2007): consistent hashing + vnodes, quorum
  W/R/N, vector clocks, gossip, hinted handoff. Powered Amazon's cart.
- **Apache Cassandra** — Dynamo's partitioning/replication + a richer data model;
  defaults to LWW, tunable consistency levels (ONE/QUORUM/ALL).
- **Riak** — closest to the Dynamo paper, vector clocks and all.
- **DynamoDB** (the AWS product) — managed, related lineage, with stronger options.
- **Discord, Cassandra at scale** — consistent hashing under enormous write loads.

---

## Interview angle

Lead with **partitioning** and explain *why `hash % N` is bad* (massive remap on
resize) before introducing the **ring**; that contrast is what shows you understand the
problem, not just the buzzword. Add **virtual nodes** for balance. Then layer on
**replication (N copies on consecutive ring nodes)** and **quorum (W + R > N)** as the
tunable consistency dial. Mention **vector clocks vs LWW** for conflicts, and close
with **gossip + hinted handoff** for failures. Always frame choices as the
**availability-vs-consistency (CAP)** trade-off.

**Common follow-ups:**
- "What happens when you add a node?" → only ~1/N of keys remap; name the predecessor
  arc; mention vnodes spreading the new load.
- "Two clients write the same key during a partition — who wins?" → LWW (lossy) vs
  vector clocks (detect conflict, app merges).
- "How do nodes discover each other without a master?" → gossip protocol.
- "A node is down for 30 seconds — do writes fail?" → no: hinted handoff to a stand-in.
- "How strong is your consistency?" → "tunable; W+R>N gives read-your-writes overlap."

---

## Practice → the Go assignment

Now build the heart of it. Go to [`assignment/`](assignment/) and implement a
**consistent hash ring with virtual nodes** (`module kvstore`):

1. `NewRing(replicas)` — `replicas` = virtual nodes per physical node.
2. `AddNode` / `RemoveNode` — place/remove vnodes; keep a **sorted** point slice.
3. `GetNode(key)` — the owning physical node, found by **binary search** clockwise.
4. `GetNodes(key, n)` — `n` **distinct** physical nodes (the replication preference list).

The tests verify the properties that *matter*, not just correctness:
- keys map **consistently**;
- adding a node (4 → 5) remaps **well under 50%** of 10,000 keys (vs ~80% for `mod`);
- `GetNodes` returns **n distinct** physical nodes, primary first;
- with enough vnodes the load is **roughly balanced** (each of 5 nodes gets > 5%).

```bash
cd assignment
go test ./...     # red → implement → green
go vet ./...      # the tests compile against your starter signatures
```

Use `hash/crc32` (provided) and `sort.Search` / `sort.Slice` — std lib only. The
interface is given; you fill in the `// TODO`s. A reference solution is in
[`solution/`](solution/) — try first, peek after.

**Next case study:** [4.3 — Unique ID Generator »](../03-unique-id-generator/)
