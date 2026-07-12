# 4.9 — Design Search Autocomplete (Typeahead)

> **Module 4 · Case Studies** · ~30 min read + coding assignment
> *Concepts exercised:* the trie (prefix tree), top-k by popularity, precompute-vs-compute,
> building from query logs, prefix sharding, multi-layer caching, frequency updates, and the
> read-heavy latency trade-off.

---

*You type "new y" into a search box and before your finger leaves the "y", a list appears:
"new york weather", "new york times", "new year". It feels like the box read your mind. It
didn't — it walked a tree, and it did so in under ten milliseconds across billions of past
searches. Let's build that tree.*

---

## The problem

**Autocomplete** (typeahead, search-as-you-type) suggests the most likely completions of what a
user is typing, ranked by popularity, updating on **every keystroke**. Google, Amazon, IDEs, and
your phone's keyboard all do it.

The defining constraint is brutal: it must respond **on every keystroke**, faster than the user
can type the next letter. That's a latency budget of single-digit to low-double-digit
milliseconds — including the network round trip. You cannot run a database query per keystroke
across billions of phrases. The whole design is a hunt for a data structure and a caching scheme
fast enough to keep up with fingers.

> **Analogy.** A trie is the **tabbed dividers in an old paper dictionary**. You don't read the
> whole book to find "cardinal" — you flip to the "C" tab, then "Ca", then "Car", narrowing by
> one letter at a time until you're standing in a tiny slice of the book. Every shared prefix is
> filed under the same divider, so "car", "card", and "cardinal" all live down the same path.
> Autocomplete is just: walk to the divider for what you've typed, then look at the most popular
> entries filed beneath it.

---

## Step 1: Requirements (always start here)

**Functional**
- Given a **prefix**, return the **top-k** most likely completions (k is small, ~5–10).
- Ranking is by **popularity** (how often a phrase has been searched), most popular first.
- Suggestions update **as the user types** (per keystroke).

**Non-functional**
- **Very low latency** — the hard requirement. Suggestions should appear in well under ~100ms
  end-to-end; the server side should be a few milliseconds.
- **Scale** — billions of stored phrases, very high query volume (every keystroke from every
  user is a request).
- **Freshness** — newly trending queries should start appearing, but **not instantly**; a few
  minutes/hours of staleness is fine. This looseness is what makes the system affordable.

**Clarifying questions to ask the interviewer**
- How many suggestions (k)? Usually ~5–10.
- Case sensitive? Handle spaces/punctuation? (Usually normalize: lowercase, trim.)
- How fresh must suggestions be — real-time, or is hourly rebuild acceptable?
- Personalized or global rankings? (Personalization multiplies storage and complexity.)
- Spell correction / fuzzy matching, or exact prefix only? (Big scope difference — clarify.)

---

## Step 2: Estimation (back-of-envelope)

- **Queries:** say a service with **5 billion searches/day**. Each search is ~20 keystrokes, and
  most trigger a suggestion request → on the order of **~10^11 suggestion lookups/day**, i.e.
  **~1M+ lookups/sec**. This is an enormously **read-heavy** system.
- **Corpus:** maybe **~10–100 million distinct popular phrases** worth suggesting (the long tail
  of one-off queries isn't worth storing). Average ~20 bytes each → a few GB of raw phrases; a
  trie with overhead is larger but still fits in memory on a beefy box or a handful of shards.
- **Writes:** frequency updates are batched from query logs, not applied per search. The serving
  structure is **read-mostly and rebuilt/updated offline**, which is the key to hitting the
  latency budget.

The takeaway: **reads dwarf writes by orders of magnitude.** That justifies precomputing
aggressively and serving from memory.

---

## Step 3: High-level design

### The data structure: a trie (prefix tree)

A **trie** stores strings by sharing prefixes. Each node is a character; following a path from the
root spells out a prefix; a node flagged as a *word end* completes a stored phrase and carries its
**frequency**.

```
                (root)
               /   |   \
              c    d    ...
              |
              a
             / \
            r   t          words: "car"(freq 30), "card"(8),
           / \   \                "cart"(12), "cat"(20), "care"(5)
          d   t   .(cat=20)
        (.)   (.)
     card=8  cart=12
          \
           e .(care=5)

Walk the prefix "car" → land on the 'r' node → everything beneath
(plus "car" itself if it's a word) are the candidates:
  car=30, card=8, cart=12, care=5  → top-2 = [car, cart]
```

To serve a query: **walk** the trie following each character of the prefix (O(length of prefix),
independent of corpus size), then **collect the words beneath** that node and return the top-k by
frequency. Walking is dirt cheap; collecting is where the cost — and the next design decision —
lives.

### API sketch

```
GET /suggest?q=new+y&k=10
→ 200 OK
  { "prefix": "new y",
    "suggestions": ["new york weather", "new york times", "new year"] }
```

Stateless, cacheable, idempotent — a perfect fit for a CDN/cache in front (see Step 4c).

### Where the data comes from: query logs

You don't hand-author suggestions; you **mine them from what people actually search**. A pipeline:

```
[ search logs ] → [ aggregate: count phrase frequencies, filter junk ]
               → [ rank, keep top phrases ] → [ build trie ] → ship to serving nodes
```

This runs offline on a schedule (hourly/daily). Today's trie reflects yesterday's (or last hour's)
popularity — exactly the staleness we said was acceptable, and exactly what lets serving stay fast.

---

## Step 4: Deep dive — store top-k at nodes, or compute on the fly?

The big trade-off. When someone types "car", do you (a) walk to the node and DFS the whole subtree
to find the top-k right then, or (b) **precompute and cache the top-k at every node** so the answer
is sitting right there?

| Approach | Query cost | Memory | Update cost | Best when |
|----------|-----------|--------|-------------|-----------|
| **Compute on the fly** (DFS subtree, rank) | Slow for short prefixes — a 1-letter prefix may scan millions of nodes | Small (just the trie) | Cheap — just bump a counter | Small corpus, or you can cache results |
| **Precompute top-k at each node** | **O(1)** — answer is stored on the node ⭐ | Larger — k phrases cached per node | Expensive — changing a freq may need to update many ancestors' lists | Read-heavy at scale (the usual case) |

For a system doing 1M+ lookups/sec, **precomputing top-k at each node wins**: the read is a single
lookup, no subtree scan. You pay for it in memory and in a heavier offline build, which is fine
because builds are rare and reads are constant. This is the recurring system-design move:
**precompute on the rare path (write/build) to make the hot path (read) trivial.**

> **Trade-off in one line.** Compute-on-the-fly trades query latency for cheap updates;
> precompute-at-node trades memory and update cost for blazing reads. Read-heavy systems almost
> always choose the latter.
>
> **In the assignment**, you'll build the *compute-on-the-fly* version (walk + collect + sort top-k).
> It's the right place to start: it makes the trie mechanics concrete. Then read this table and
> understand exactly what you'd change to scale it — cache the top-k on each node and rebuild
> offline. Knowing both, and when to switch, is the interview win.

---

## Step 4b: Deep dive — sharding by prefix

One machine can't hold a giant trie or absorb 1M+ QPS. Split it.

The natural key is the **prefix**: route all queries starting with `a–f` to shard 1, `g–m` to
shard 2, and so on (or shard on the first 1–2 characters). A query for "new york" goes straight to
the "n" shard.

| Sharding choice | Pro | Con |
|-----------------|-----|-----|
| By first letter | Simple routing | Skew — "s"/"c" prefixes are far busier than "x"/"z" |
| By first 2–3 chars / hash of prefix | Evens out load | Slightly more complex routing table |

> **Trade-off — skew.** Letter-frequency is wildly uneven, so naive first-letter sharding creates
> hot shards. Shard on a longer prefix or a hash to balance, accepting a more complex routing layer.
> (This echoes the partitioning lesson, 2.7.)

---

## Step 4c: Deep dive — caching

The cheapest request is the one you never compute. Suggestion responses are read-heavy, identical
across users (for global rankings), and tolerant of staleness — a caching dream.

- **CDN / edge cache** for popular prefixes: "ne", "new", "you" are typed constantly; cache the
  response at the edge with a short TTL. A huge fraction of traffic never reaches your trie.
- **In-memory result cache** on serving nodes for hot prefixes.
- The whole **trie itself lives in memory** — it's the serving structure, not on disk.

> **Trade-off — TTL vs freshness.** A longer cache TTL means more cache hits (cheaper, faster) but
> staler suggestions. Because freshness requirements here are loose (minutes is fine), you lean
> toward longer TTLs and reap big hit rates.

---

## Step 4d: Deep dive — updating frequencies

Rankings must drift as the world changes ("oscar nominees" spikes every February). Two models:

| Update model | Mechanism | Trade-off |
|--------------|-----------|-----------|
| **Batch rebuild** (common) | Recount from logs offline, rebuild/swap the trie periodically | Simple, consistent, but stale between builds |
| **Incremental bump** | Increment a phrase's frequency live as searches arrive | Fresher, but re-ranking cached top-k lists on the hot path is costly; risks contention |

Most systems lean **batch** for the bulk and sprinkle in incremental bumps for trending detection.
Your assignment implements the bump primitive (`Bump` = +1, insert-if-new), so you feel how a live
frequency update changes ranking — then you understand why doing that *per node, per keystroke, at
scale* is the part you'd push offline.

---

## In the wild

- **Google** serves autocomplete from precomputed, sharded, heavily cached structures built from
  query logs, blended with freshness/trending signals and personalization.
- **Elasticsearch** ships "completion suggesters" backed by an in-memory FST (finite-state
  transducer) — a compressed cousin of the trie that shares suffixes too, saving memory.
- **Redis** is commonly used for sorted-set-based prefix/leaderboard suggestion caches.

---

## Interview angle

Lead with the **trie** and *why*: prefix walk is O(prefix length), independent of corpus size.
Then raise the central trade-off unprompted — **precompute top-k at each node vs compute on the
fly** — and pick precompute for a read-heavy system, naming the memory/build cost you accept. Show
scale range with **prefix sharding (and its skew problem)** and **multi-layer caching with loose
TTLs because freshness is forgiving**. Mention that suggestions are **mined from query logs and
rebuilt offline**, which is what makes serving fast. The senior signal is connecting "read-heavy +
stale-tolerant" to "so precompute and cache aggressively."

**Common follow-ups:**
- "A 1-character prefix would scan millions of nodes — how do you keep it fast?" → precompute and
  store the top-k *on each node*, so the answer is O(1) and no subtree scan happens at query time.
- "How do new trending queries appear?" → mined from logs in periodic offline rebuilds; optionally
  a faster trending lane, accepting minutes of lag.
- "One machine can't hold it — how do you split?" → shard by prefix; watch for letter-frequency
  skew and shard on a longer prefix or hash to balance.
- "How do you update popularity without slowing reads?" → batch rebuild + swap for the bulk; keep
  the hot read path free of heavy re-ranking.

---

## Practice → the Go assignment

Now build the core data structure. Go to [`assignment/`](assignment/) and implement a **trie with
frequencies**:

1. `Insert(word, freq)` — add a word, *setting* (overwriting) its frequency.
2. `Bump(word)` — increment a word's frequency by 1, inserting it with freq 1 if new (a live
   popularity update).
3. `Suggest(prefix, k)` — return up to k words with that prefix, ranked by **frequency DESC**,
   tie-broken **lexicographically ASC**. This is the compute-on-the-fly version: walk to the
   prefix node, collect the words beneath, sort, take the top-k.

```bash
cd assignment
go test ./...          # red → implement → green
```

The tests are **deterministic** — fixed words and frequencies, exact expected orderings (including
ties, k-limits, empty/no-match, and "the prefix is itself a word"). The interface is given; you
fill in the `// TODO`s. A reference solution is in [`solution/`](solution/) — try first, peek
after.

**Next case study:** [4.10 — YouTube / Video Streaming »](../10-video-streaming/)
