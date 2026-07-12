# 2.8 — Consistent Hashing

> **Module 2 · Building Blocks** · ~25 min read
> *How do you split data across N machines so that adding or removing one machine moves
> almost nothing? The naive answer (`hash % N`) is a disaster. Consistent hashing is the fix —
> and it quietly powers caches, sharded databases, and load balancers everywhere.*

---

## The problem

You have too much data (or too much traffic) for one server, so you spread it across several.
The eternal question of sharding (from [Module 2.7](07-sharding-partitioning.md)): given a key
— a user ID, a cache key, a filename — **which server owns it?**

The obvious first idea is **modulo hashing**. Hash the key to a number, then take it modulo the
number of servers:

```
server = hash(key) % N
```

With `N = 4` servers, `hash("alice") % 4 = 2` → Alice lives on server 2. Deterministic, even,
dead simple. It works great... until `N` changes.

> **Analogy.** Imagine a cloakroom that assigns coats to pegs by **coat number mod number of
> pegs**. With 4 pegs, coat #10 → peg 2. Now you add a 5th peg. Suddenly coat #10 → peg 0,
> coat #11 → peg 1, and so on. *Almost every coat now hangs on the wrong peg.* To honor the new
> rule you'd have to physically re-hang nearly every coat in the room. That mass re-shuffle is
> exactly what kills modulo hashing in a distributed system.

---

## Why `hash % N` falls apart

Let's make the pain concrete. Keys 0–9, `N = 4`:

```
key:    0  1  2  3  4  5  6  7  8  9
key%4:  0  1  2  3  0  1  2  3  0  1     ← server assignment
```

Now a server dies, or you scale up. `N` becomes 5:

```
key:    0  1  2  3  4  5  6  7  8  9
key%5:  0  1  2  3  4  0  1  2  3  4     ← server assignment
        ✓  ✓  ✓  ✓  ✗  ✗  ✗  ✗  ✗  ✗
```

Only keys 0–3 stayed put. **6 of 10 keys moved.** In general, changing `N` remaps roughly
`(N-1)/N` of *all* keys — almost everything.

Why is that catastrophic?

- **For a cache:** every moved key is now a **cache miss**. One server change → a flood of
  misses → all that traffic stampedes the database. This is how a routine deploy takes down a
  backend.
- **For a sharded database:** moved keys mean **physically copying terabytes** of data between
  machines while serving live traffic. Slow, risky, sometimes impossible in a hurry.

We want a scheme where adding or removing a node moves **only the keys that node is responsible
for** — a small fraction — and leaves everything else untouched.

---

## Core idea: the hash ring

Consistent hashing makes one elegant move: **hash the servers onto the same space as the keys,
and arrange that space as a ring.**

1. Pick a hash function with a fixed output range, say `0` to `2³² − 1`.
2. Bend that range into a **circle**: the largest value wraps around to `0`.
3. Hash each **server** to a point on the ring (using its name or IP).
4. Hash each **key** to a point on the ring too.
5. **A key belongs to the first server you meet going clockwise** from the key's position.

That's the whole trick. The key isn't tied to "server number `k % N`" — it's tied to *a fixed
position on a circle*, and it's owned by whichever server happens to sit next to it. Adding or
removing a server changes the neighborhood of only **one arc** of the ring, not the whole thing.

---

## How it works

### The ring and ownership

Picture the ring as a clock face, `0` at the top, increasing clockwise. Four servers (A, B, C,
D) hash to four positions:

```
                       0 / 2³²
                         │
                  D      │      A
                    ╲    │    ╱
                      ╲  │  ╱
                        ╲│╱
          ───────────────●───────────────
                        ╱│╲
                      ╱  │  ╲
                    ╱    │    ╲
                  C      │      B
                         │
```

To place a key, hash it to a point and **walk clockwise to the next server**:

```
   key k1 lands here ─┐
                      ▼
   ... A ········ k1 ····· B ........ C ........ D ...(wrap)... A
                            ▲
                            owner of k1 = B (first server clockwise)
```

So each server owns the arc **between the previous server and itself**. Looking it up is "find
the next node clockwise" — in code, a sorted list of node positions plus a binary search.

### Adding a node moves only one arc

Suppose we add server **E** between A and B:

```
   before:   ... A ──────────────── B ........ C ........ D ...
                   \________________/
                   this whole arc → B

   after:    ... A ──────── E ────── B ........ C ........ D ...
                   \________/\_______/
                    now → E    still → B
```

Only keys that fell in the arc **just before E** move — from B to E. Every other key on the
ring is **completely undisturbed**. Servers C and D never even notice.

### Removing a node is the mirror image

If **C** dies, its arc simply gets absorbed by the **next server clockwise** (D). Keys that were
on C now resolve to D; nobody else is touched.

```
   before:   ... B ........ C ──────── D ...
                            \_________/
                            C's keys

   after:    ... B ···················· D ...
                            \__________/
                            now → D
```

**The headline result:** with N servers, adding or removing one moves on average only **K/N
keys** (K = total keys), versus nearly **all K** keys under `hash % N`. That single property is
why consistent hashing exists.

---

## The balance problem (and virtual nodes)

Basic consistent hashing has a flaw: with only a handful of servers placed at random points,
the arcs come out **uneven**. One server might land right after another and own a tiny sliver,
while another owns a huge stretch:

```
   ... A ─ B ───────────────────────────── C ─ D ...
        \_/\_____________________________/\_/
       tiny      C owns a HUGE arc       tiny
```

C is now a hotspot — it holds most of the data and takes most of the traffic. Worse, when a
server dies, **all** its load dumps onto its single clockwise neighbor instead of spreading out.

**The fix: virtual nodes (vnodes).** Instead of placing each physical server at *one* point,
place it at *many* points — hash `"C#1"`, `"C#2"`, ..., `"C#150"` and scatter all of them
around the ring. Each physical server is now represented by many small arcs sprinkled
everywhere:

```
   without vnodes:   ... A ───── B ─────────────── C ─── D ...   (lumpy)

   with vnodes:      ... A B C D A C B D A D B C A B C D ...      (smooth, interleaved)
                         each letter = one vnode of that server
```

Two big wins:

- **Even distribution.** With ~100–200 vnodes per server, the law of large numbers smooths the
  arcs out; each physical server ends up owning roughly its fair share.
- **Smooth failure.** When a server dies, its many little arcs are absorbed by **many different
  neighbors**, so the lost load spreads across the surviving servers instead of crushing one.

The cost is more bookkeeping (more points in the sorted ring) and a little memory — a cheap
price for balance. The number of vnodes is a **tuning knob**: more vnodes → smoother balance →
more metadata.

---

## Lookup, step by step

Putting it together, here's how a single lookup runs:

```
1. position = hash(key)                 # a point on the ring
2. find the smallest node-position ≥ position   # binary search the sorted ring
3. if none (key is past the last node), wrap to the first node
4. that node (its physical server) owns the key
```

With the ring stored as a **sorted array of vnode positions**, step 2 is an `O(log V)` binary
search (V = total vnodes). Add/remove a server = insert/delete its vnodes and re-sort — cheap
and local.

---

## Comparison

| Scheme | Keys moved when N changes | Balance | Lookup cost | Complexity |
|--------|---------------------------|---------|-------------|------------|
| `hash % N` (modulo) | ~all keys (≈ (N−1)/N) | Even | O(1) | Trivial |
| Consistent hashing (no vnodes) | ~K/N keys | **Uneven** (lumpy arcs) | O(log N) | Moderate |
| Consistent hashing + vnodes | ~K/N keys | **Even** | O(log V) | Moderate+ |
| Lookup table / directory | Configurable (manual) | Fully controllable | O(1) | High (must maintain map) |

> A **directory-based** scheme (a service that explicitly maps each key range → node) gives you
> total control and is what some big systems actually use — but you pay for it with a
> coordination service to keep that map consistent. Consistent hashing buys you "good enough,
> automatic" balance with no central map.

---

## Trade-offs & key takeaways

- **The one thing to remember:** consistent hashing minimizes **remapping**. Changing the
  cluster moves ~K/N keys instead of ~all of them. That's the entire point.
- **Virtual nodes are not optional in practice.** Without them, distribution is too lumpy and
  failures are too concentrated. Real systems use 100s of vnodes per server.
- **It does not give you perfectly even balance** — it gives you *statistically* even, and only
  if you use enough vnodes. Hot *keys* (one wildly popular item) are still a separate problem
  consistent hashing doesn't solve.
- **Weighted nodes are easy.** A beefier server can get *more* vnodes, so it owns
  proportionally more of the ring. Heterogeneous clusters become simple.
- **It's not a silver bullet for stateful data.** Moving cache keys is cheap (just re-cache on
  miss). Moving *database* keys still means copying data — consistent hashing reduces *how
  much* you copy, not the fact that you must.

---

## In the wild

- **Amazon Dynamo / DynamoDB & Apache Cassandra** partition data across nodes using consistent
  hashing with virtual nodes — the foundational use case.
- **Memcached clients** (e.g. `ketama`) use consistent hashing so that adding/removing a cache
  node doesn't blow away the whole cache.
- **Discord, Riak, Couchbase, ScyllaDB** and many sharded stores rely on ring-based placement.
- **Load balancers / proxies** (e.g. Envoy's "ring hash," NGINX's `hash ... consistent`) use it
  for **sticky routing** — sending a given client or key to the same backend even as the backend
  pool changes.

---

## Interview angle

The classic opener: *"You're caching across 10 servers with `hash % N`. You add one server.
What happens?"* The senior answer is immediate: **almost every key remaps → mass cache misses →
the database gets stampeded.** Then you introduce the ring, explain "first node clockwise,"
state the **K/N** remapping result, and — crucially — bring up **virtual nodes** unprompted to
fix balance and failure concentration. Mentioning vnodes is the signal that you've actually used
this, not just read about it.

**Common follow-ups:**
- *"Why virtual nodes?"* → Even distribution + spreading a failed node's load across many
  neighbors instead of one.
- *"How do you give a bigger machine more data?"* → Assign it more vnodes (weighting).
- *"How is a lookup actually implemented?"* → Sorted array of vnode positions + binary search
  for the next position clockwise, wrapping at the end.
- *"Does this fix hot keys?"* → No. A single super-popular key still lands on one node;
  you need replication or key-splitting for that.

---

## Self-check

1. With `hash % N` and 4 servers, you add a 5th. Roughly what fraction of keys get remapped, and
   why is that especially bad for a *cache*?
2. On the ring, how do you decide which server owns a given key?
3. You add one server to a 10-server ring. About how many keys move, and which ones?
4. What problem do virtual nodes solve, and what do they cost?
5. A teammate says "consistent hashing gives perfectly even load." Where are they wrong?

---

## Practice

The Go assignment for consistent hashing lives in **Case Study 4.2**, where you'll build a hash
ring with virtual nodes as the partitioning layer of a distributed key-value store. Head to
**[4.2 — Consistent Hashing / Key-Value Store](../04-case-studies/02-key-value-store/)** and
implement the ring, `AddNode` / `RemoveNode`, and `GetNode(key)` — then watch the tests confirm
that adding a node moves only ~K/N keys.

**Next:** [2.9 — CAP, PACELC & Consistency Models »](09-cap-pacelc-consistency.md)
