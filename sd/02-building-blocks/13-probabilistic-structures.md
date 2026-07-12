# 2.13 — Probabilistic Data Structures

> **Module 2 · Building Blocks** · ~30 min read
> *Sometimes a slightly-wrong answer that fits in a kilobyte beats a perfect answer that needs a
> gigabyte. Probabilistic data structures trade a sliver of accuracy for enormous savings in
> memory — and at scale, that trade is often a steal.*

---

## The problem

You're building a web crawler (we will, in [Module 4.5](../04-case-studies/05-web-crawler/)). It
finds URLs and must answer one question billions of times: *"have I already crawled this URL?"*
The obvious solution is a `HashSet` of every URL seen.

Do the math. A billion URLs, ~100 bytes each ⇒ **~100 GB** of RAM just to remember what you've
seen. That doesn't fit on one machine. The exact answer is *correct* but *unaffordable*.

Now ask: **what if a tiny error rate were acceptable?** If the structure occasionally says "yes, I
think I've seen this" when you actually haven't, the worst case is you re-crawl one page — wasteful,
but harmless. In exchange, you could shrink 100 GB down to **~1–2 GB**. That bargain — **approximate
answers for massive memory savings** — is the whole idea behind probabilistic data structures.

> **Analogy.** A nightclub bouncer with a perfect memory of every face who ever entered would need
> a superhuman brain (the exact `HashSet`). Instead he keeps a rough mental impression: "tall guy,
> red jacket, beard — yeah, I think he's been here." Occasionally he's wrong and waves through
> someone new who *fits the impression* (a **false positive**). But he never forgets someone who
> truly came in (**no false negatives**), and his "memory" fits in one human head. That impression
> is a Bloom filter.

Three structures answer three different questions, all with the same bargain:

| Question | Structure |
|----------|-----------|
| "Have I seen `X`?" (set membership) | **Bloom filter** |
| "How many times has `X` occurred?" (frequency) | **Count-Min Sketch** |
| "How many *distinct* things have I seen?" (cardinality) | **HyperLogLog** |

---

## Bloom filter: probabilistic set membership

### Core idea

A Bloom filter is a **bit array** of `m` bits (all 0 initially) plus `k` independent **hash
functions**. It answers "is `X` in the set?" — but a *yes* means "**probably** yes" and a *no*
means "**definitely** no."

**To add an element:** hash it with all `k` functions, each producing an index in `[0, m)`. Set
those `k` bits to 1.

```
   Add "apple":  h1→2  h2→5  h3→9     set bits 2, 5, 9
   Add "mango":  h1→5  h2→7  h3→0     set bits 5, 7, 0

   bit array (m = 10):
   index:  0  1  2  3  4  5  6  7  8  9
           1  0  1  0  0  1  0  1  0  1
           ▲     ▲        ▲     ▲     ▲
           mango apple  shared  mango apple
```

**To test an element:** hash it the same `k` ways. If **all** `k` bits are 1, answer "probably
present." If **any** bit is 0, answer "definitely absent."

```
   Test "apple": check bits 2, 5, 9 → all 1 → "probably present" ✓ (it is)
   Test "grape": check bits 1, 4, 8 → bit 1 is 0 → "DEFINITELY absent" ✓
   Test "lemon": h1→2 h2→5 h3→7 → all happen to be 1 → "probably present" ✗ FALSE POSITIVE
```

### The two crucial guarantees

- **No false negatives.** If you added `X`, its `k` bits are set; a test will always find them. The
  filter never says "no" to something genuinely present. *This is the property you build on.*
- **Possible false positives.** A test can say "yes" for something never added, because *other*
  elements happened to set all `k` of its bits (collision). The error is one-directional.

### Why this is so useful

A "definitely no" is **trustworthy and cheap**, so you put a Bloom filter **in front of an
expensive lookup** as a fast reject:

```
   "Is this in the database/cache?"
        │
        ▼
   [ Bloom filter ]
        │
        ├─ says NO  ──► definitely absent → skip the disk/network lookup entirely (the win!)
        └─ says YES ──► maybe present → do the real lookup to confirm
```

Most "is it there?" questions in big systems are answered "no" — and the Bloom filter answers those
instantly without touching disk. The occasional false positive just costs you one real lookup that
turns up empty.

### The false-positive rate (math intuition, no heavy proofs)

After inserting `n` elements into `m` bits with `k` hashes, the probability that any given bit is
*still* 0 is roughly `(1 − 1/m)^(kn) ≈ e^(−kn/m)`. A false positive needs **all `k`** checked bits
to be 1, so:

```
   false-positive rate  ≈  (1 − e^(−kn/m))^k
```

You don't need to derive it — just feel the three levers:

- **More bits `m`** (relative to `n`) ⇒ fewer collisions ⇒ **lower** error. Memory buys accuracy.
- **Too few hashes `k`** ⇒ coarse fingerprint, more accidental matches. **Too many `k`** ⇒ the array
  fills with 1s too fast. There's a **sweet spot.**

The optimal settings (worth memorizing the *shape*, not the algebra) for a target error `p` and
expected count `n`:

```
   optimal m = − (n · ln p) / (ln 2)²        (bits needed)
   optimal k =   (m / n) · ln 2              (hashes; round to nearest int, ≥ 1)
```

Rule of thumb: about **10 bits per element** gives a **~1%** false-positive rate with ~7 hash
functions. That's ~1.2 GB for a billion items — versus 100 GB for the exact set. **That's the deal.**

### A practical trick: double hashing

Computing `k` truly independent hashes is wasteful. Instead, compute **two** base hashes `h1` and
`h2` and derive the rest cheaply:

```
   g_i(x) = h1(x) + i · h2(x)   (mod m),   for i = 0, 1, ..., k−1
```

This gives `k` well-distributed indices from just two hash computations — the technique you'll
implement in the assignment.

### The catch

You **cannot delete** from a standard Bloom filter — clearing a bit might unset a bit shared with
another element, creating a false *negative* (which would break the core guarantee). The fix is a
**Counting Bloom filter**: replace each bit with a small counter, increment on add, decrement on
delete. It supports deletion at the cost of more memory.

---

## Count-Min Sketch: frequency estimation

### The question

"How many times have I seen `X`?" — e.g. *how often was this IP requested* (DDoS detection), *which
search terms are trending*, *which products are hot right now*. Counting exactly means a counter per
distinct key — and there can be billions of keys.

### Core idea

Like a Bloom filter, but with **counters instead of bits**, laid out as a 2-D grid: `d` rows, each a
row of `w` counters, each row paired with its own hash function.

```
   To COUNT "X": hash X with each of the d row-hashes, INCREMENT that cell in each row.
   To QUERY "X": hash X the same way, read the d cells, return the MINIMUM.

           col→  0    1    2    3    4   ... (w wide)
   row 0  [ h0 ]  3    0    7    1    2
   row 1  [ h1 ]  0    7    1    9    0     query "X": cells = 7, 7, 8 → min = 7
   row 2  [ h2 ]  1    0    8    2    4
                  ▲ each row hashes X to one cell; increment on add
```

**Why the minimum?** Hash collisions can only *add* to a counter (some *other* key landed in the
same cell), never subtract. So every cell is an **overestimate**. Taking the **minimum** across
rows gives the tightest (least-inflated) estimate — and using `d` independent rows makes it unlikely
that *every* row collided for the same key.

- **Never underestimates** — the true count is always ≤ the reported count (analogous to the
  Bloom filter's one-sided error).
- Error shrinks as you add **more columns `w`** (less collision) and **more rows `d`** (more chances
  to dodge collisions). Fixed memory regardless of how many distinct keys you track.

Great for "**heavy hitters**" — the few high-frequency items, where a small absolute error doesn't
change the verdict.

---

## HyperLogLog: cardinality estimation

### The question

"How many **distinct** things have I seen?" — *unique visitors today*, *distinct search queries*,
*unique IPs*. Exactly: a `HashSet` of everything ⇒ memory grows with the number of uniques (back to
the 100 GB problem).

### Core idea (the intuition)

Hash each element to a random-looking bit string and watch the **longest run of leading zeros** you
ever see. Leading zeros are rare: ~½ of random hashes start with a 1 (zero leading zeros), ~¼ start
`01`, ~⅛ start `001`... So if you've observed a hash with `n` leading zeros, you've *probably*
seen on the order of **2ⁿ** distinct items — like flipping coins and noting the longest streak of
heads to guess how many times you flipped.

```
   "Longest leading-zero run = 3" hints at ~2³ = 8 distinct items.
   HyperLogLog uses many independent "buckets" and averages, slashing the variance.
```

One bucket is wildly noisy, so HyperLogLog splits hashes into many buckets (using the first few
bits to pick a bucket), tracks the max leading-zero run **per bucket**, and combines them with a
harmonic mean. The payoff is spectacular: count **billions of distinct items to ~2% error using
only ~12 KB** of memory — essentially constant, no matter how many uniques. It also **merges**:
combine two HLLs to get the union's cardinality, perfect for distributed counting.

---

## Comparison table

| Structure | Answers | Error type | Memory | Classic uses |
|-----------|---------|-----------|--------|--------------|
| **Bloom filter** | Membership ("seen `X`?") | False positives only; **no false negatives** | ~10 bits/elem (≈1%) | Cache/DB existence checks, dedup, crawler URL seen |
| **Count-Min Sketch** | Frequency ("how often `X`?") | **Overestimates only** | Fixed grid `d × w` | Heavy hitters, trending terms, DDoS by IP |
| **HyperLogLog** | Cardinality ("how many distinct?") | ~2% relative, both directions | ~12 KB regardless of `n` | Unique visitors/queries, distinct counts |

The shared DNA: **fixed (small) memory, a controlled error, and a one-directional guarantee you can
design around** (Bloom never says false-no; Count-Min never undercounts).

---

## Trade-offs & key takeaways

- **The core trade:** a small, bounded error for **massive** memory savings. Worth it precisely when
  exact answers are too big to fit and a little error is harmless.
- **Bloom: no false negatives, possible false positives.** Use a "definitely no" as a free,
  trustworthy fast-reject *in front of* an expensive lookup.
- **Tune Bloom with `m` and `k`.** ~10 bits/element ≈ 1% error; more bits = more accuracy. Standard
  Bloom filters **can't delete** (use Counting Bloom if you must).
- **Count-Min never undercounts** (take the min across rows); ideal for heavy hitters.
- **HyperLogLog counts distinct in near-constant tiny memory** (~12 KB) and **merges** — perfect for
  distributed uniques.
- **Know which question each answers** — membership vs frequency vs cardinality. Picking the wrong
  one is the common mistake.

---

## In the wild

- **Bloom filters in LSM-tree databases** (Cassandra, RocksDB, LevelDB, HBase) — before reading an
  on-disk SSTable, check a Bloom filter; a "no" skips a costly disk seek. A huge real-world win
  (ties into storage engines, [Module 3.2](../03-distributed-systems/02-storage-engines.md)).
- **CDNs / caches** — "is this URL even cacheable / known?" answered without a backend hit.
- **Web crawlers** — the "URLs already seen" frontier dedup (Module 4.5).
- **Chrome's Safe Browsing** historically used a Bloom filter to check URLs against a malware list
  locally; only a "maybe" triggered a server round-trip.
- **Redis** ships **HyperLogLog** (`PFADD`/`PFCOUNT`) and, via RedisBloom, Bloom and Count-Min.
- **Databases & analytics** (Presto, BigQuery `APPROX_COUNT_DISTINCT`) use HyperLogLog for fast
  distinct counts.

---

## Interview angle

When a design involves **"have we seen this?"**, **"how many distinct?"**, or **"what's trending?"**
*at large scale*, reach for the matching probabilistic structure and **state the trade-off**: "an
exact set is ~100 GB; a Bloom filter is ~1 GB at 1% false positives, and a false positive only costs
one wasted lookup — acceptable here." Naming **no-false-negatives** and where you'd put the filter
(a fast reject before disk/network) is the senior signal. For dedup/cache-existence reach for
**Bloom**; for unique counts, **HyperLogLog**; for heavy hitters, **Count-Min**.

**Common follow-ups:**

- *"What's the downside of a Bloom filter, and why is it still safe?"* → false positives; safe
  because there are **no false negatives**, so a "no" is authoritative and a "yes" just triggers a
  confirming lookup.
- *"How do you size a Bloom filter?"* → from target error `p` and expected `n`: `m ≈ −n·ln p/(ln 2)²`,
  `k ≈ (m/n)·ln 2`; ~10 bits/elem ≈ 1%.
- *"Can you delete from a Bloom filter?"* → not a standard one (would risk false negatives); use a
  **Counting** Bloom filter.
- *"Count unique visitors for a billion-event stream cheaply?"* → HyperLogLog, ~12 KB, ~2% error,
  and it merges across shards.

---

## Self-check

1. Why does a Bloom filter have **no false negatives** but **possible false positives**? Which
   property do real systems build on, and how?
2. You expect 1M elements and want ~1% false positives. Roughly how many bits and how many hash
   functions, and what's the memory ballpark?
3. Explain double hashing — why compute only two base hashes for `k` indices?
4. In a Count-Min Sketch, why do you take the **minimum** of the cells on a query, and why can it
   never *under*count?
5. Which structure answers "how many *distinct* users today," and roughly how much memory does it
   need for a billion uniques?

---

## Practice → the coding assignment

Time to build the most important one yourself. In **[`13-probabilistic-assignment/`](13-probabilistic-assignment/)**
you'll implement a **Bloom filter** in Go: a bit array with `k` hashes via **double hashing**
(`fnv` for the two base hashes), plus a constructor that computes the **optimal `m` and `k`** from a
target false-positive rate. The tests verify the core guarantee (**no false negatives**) and that
the tuned false-positive rate stays within bounds.

```bash
cd 13-probabilistic-assignment/assignment
go test ./...     # red → implement → green
```

The interface is given; you fill in the `// TODO`s. A reference solution sits in
[`13-probabilistic-assignment/solution/`](13-probabilistic-assignment/solution/) — try first, peek
after.

**Next:** [2.14 — Idempotency & Exactly-Once »](14-idempotency.md)
