# 4.7 — Design a News Feed

> **Module 4 · Case Studies** · ~35 min read + coding assignment
> *Concepts exercised:* fan-out-on-write (push) vs fan-out-on-read (pull), the
> celebrity/hot-user hybrid, k-way merge, feed ranking, caching, the
> "precompute vs compute-on-demand" trade-off.

---

## The problem

A **news feed** is the scrolling list of posts you see on Twitter/X, Instagram, or Facebook:
the recent posts from everyone you **follow**, newest-first (or ranked). You post; your
followers see it. They post; you see it.

What makes it interesting at scale:
- **The graph is lopsided.** Most people follow a few hundred accounts. A celebrity has
  *100 million* followers. One tweet from them must reach all of them.
- **It's read-heavy.** People scroll far more than they post — often 100:1 reads to writes. The
  design must make the **read** (open the app, see your feed) fast and cheap.
- **Two expensive moments.** Either you do work when someone **posts** (push the post to all
  followers' feeds) or when someone **reads** (gather posts from everyone they follow). You
  can't make both free. Choosing where to pay is the whole game.

> **Analogy.** Imagine a neighborhood newsletter. Two ways to run it:
>
> **Push (fan-out-on-write):** every time you write an article, the print shop immediately
> mails a copy into the mailbox of every subscriber. When a subscriber wants to read, they just
> open their mailbox — instant, everything's already there. But if you have a *million*
> subscribers, one article means a million envelopes stuffed *right now*. Great for readers,
> brutal for popular writers.
>
> **Pull (fan-out-on-read):** nobody mails anything. When a subscriber wants to read, they walk
> to every writer they follow, ask "anything new?", and assemble their own edition on the spot.
> Writing is trivial (just pin it to your own board). But reading is a lot of legwork, *every
> single time*.
>
> Real systems do a **hybrid**: mail copies for normal writers (push), but for the
> mega-celebrity whose million envelopes would jam the print shop, let subscribers pull on read.
> You get the best of both — at the cost of having to detect "who's a celebrity."

---

## Step 1: Requirements (always start here)

**Functional**
- **Follow / unfollow** another user.
- **Post** a piece of content (text, with a timestamp and author).
- **Get feed** — the recent posts of everyone a user follows, **newest-first**, paginated.

**Non-functional**
- **Fast reads** — opening the feed is the most common action; target tens of milliseconds.
- **Scalable** — billions of follow edges; a single post may need to reach 100M followers.
- **Eventually consistent** — it's fine if your friend's post shows up a few seconds late. We
  trade strict freshness for availability and speed. (See *Module 2.9 — CAP/PACELC*.)
- **Available** — the feed loading is more important than it being perfectly complete this
  instant.

**Clarifying questions**
- *Chronological or ranked?* Pure reverse-chronological is simpler; ranked (ML score) is what
  big platforms ship. State which you're designing.
- *How many followees does a typical user have?* (Sets read cost.)
- *What's the max followers for one account?* (Reveals the celebrity problem.)
- *Read:write ratio?* (~100:1 justifies optimizing reads, i.e. leaning toward push.)
- *Feed depth?* Usually only the most recent ~hundreds of posts matter; you don't merge years.

---

## Step 2: Estimation

Say **200 million daily active users**, each posting **twice/day** and opening their feed
**10 times/day**.

```
posts/day              = 200e6 × 2                = 400 million posts/day
post writes/sec (avg)  = 400e6 / 86,400           ≈ 4,600 posts/sec
feed reads/day         = 200e6 × 10               = 2 billion reads/day
feed reads/sec (avg)   = 2e9 / 86,400             ≈ 23,000 reads/sec
read : write ratio                                 ≈ 5:1 here (often higher) → optimize reads
```

**The fan-out math (push):** average ~500 followers → each post writes to ~500 feeds →
`4,600 posts/sec × 500 = 2.3 million feed-writes/sec`. Heavy, but spread across a cache cluster
it's tractable — *until a celebrity posts*. One account with **100M followers** posting once =
**100 million feed-writes from a single action**. That single fact is why pure push doesn't
work, and why the hybrid exists.

Storage for feeds (push): if you cache ~800 post-IDs per user × 200M users × ~16 B/ID ≈ **2.5 TB**
of feed cache. Fits in a Redis cluster; the post *bodies* live once in a content store and are
hydrated on read.

---

## Step 3: High-level design

### The two strategies

```
PUSH (fan-out-on-write)                    PULL (fan-out-on-read)
─────────────────────────                  ─────────────────────────
Alice posts                                 Alice posts
   │                                            │
   ▼                                            ▼
write post once                             write post once to Alice's timeline
   │                                            (that's it — no fan-out)
   ▼
look up Alice's 500 followers
   │
   ▼                                        Bob opens his feed
copy post-id into each                          │
follower's precomputed feed                     ▼
                                            look up the 300 users Bob follows
Bob opens his feed                              │
   │                                            ▼
   ▼                                        read each of their recent timelines
read his ready-made feed  ◄── O(1), fast        │
(already assembled)                             ▼
                                            MERGE them newest-first  ◄── work happens here
                                            (k-way merge), cap at limit
```

- **Push** pays at write time, makes reads trivial. **Pull** pays at read time, makes writes
  trivial. This assignment implements **pull** because the merge is the instructive algorithm —
  and because it's the half of the hybrid that handles celebrities.

### Push vs Pull — the table interviewers want

| Dimension | Fan-out-on-write (push) | Fan-out-on-read (pull) |
|-----------|-------------------------|------------------------|
| Work at **post** time | High — write to every follower's feed | Tiny — append to your own timeline |
| Work at **read** time | Tiny — read a precomputed feed | High — gather + merge all followees |
| Read latency | Excellent (feed is ready) | Worse (merge on every open) |
| Celebrity post | Disastrous (100M writes) | Fine (no fan-out at all) |
| Inactive users | Wasteful (you maintain feeds nobody reads) | Efficient (compute only when asked) |
| Freshness | Can lag during big fan-outs | Always current at read time |
| Storage | High (a materialized feed per user) | Low (just timelines) |
| Best when | Most users, modest follower counts | Hot/celebrity accounts; or low read volume |

### The hybrid (what real systems actually do)

- **Push** for normal users — fast reads for the 99%.
- **Pull** for **celebrity/hot** accounts — when you read, your precomputed (push) feed is
  **merged on the fly** with the latest posts from the handful of celebrities you follow. So a
  celebrity post is written **once**, and only materializes into a feed when a follower actually
  looks. This dodges the 100M-write fan-out while keeping reads fast for everyone else.

### API

```
POST   /v1/posts           { "author":"u1", "text":"hi" }        → 201 { id, ts }
PUT    /v1/follows/{u2}     (as user u1)                          → 204
DELETE /v1/follows/{u2}     (as user u1)                          → 204
GET    /v1/feed?limit=20&cursor=...                               → 200 { posts[], next_cursor }
```

### Data model

```
follows        follower_id | followee_id            (the social graph; index both directions)
posts          id | author_id | text | ts           (the content, stored once)
timelines      author_id → [recent post-ids]         (per-author, newest-first; the pull source)
feeds (push)   user_id   → [recent post-ids]          (precomputed materialized feed; the push cache)
```

You index `follows` **both ways**: by `follower` (to build a pull feed: "who do I follow?") and
by `followee` (to fan out on write: "who follows me?").

---

## Step 4: Deep dives & trade-offs

### Deep dive A — Building a pull feed efficiently (the k-way merge)

Naive pull: gather *every* post from *every* followee, sort the whole pile by time, take the top
`limit`. If you follow 300 people with 800 posts each, that's sorting 240,000 posts to show 20.
Wasteful.

Better: each followee's timeline is **already sorted newest-first**. To merge `k` sorted lists
and take the top `limit`, use a **k-way merge with a heap** (`container/heap`):

```
1. seed a max-heap with the NEWEST post from each of the k timelines  (k items)
2. pop the newest overall → that's the next feed item
3. push the NEXT post from the timeline you just popped from
4. repeat until you have `limit` posts (or the heap is empty)
```

```
followee timelines (each newest-first):
  bob:   [t=30, t=20, t=10]
  carol: [t=25, t=5]
                                  heap (newest on top)
  seed:    bob@30, carol@25       → pop 30 (bob)  → push bob@20
  step:    bob@20, carol@25       → pop 25 (carol)→ push carol@5
  step:    bob@20, carol@5        → pop 20 (bob)  → push bob@10
  ...
  feed = [30, 25, 20, ...]        stop at `limit`
```

| Approach | Cost to produce top-L from k lists of n | Notes |
|----------|------------------------------------------|-------|
| Collect-all + full sort | O(k·n · log(k·n)) | Sorts posts you'll never show |
| **k-way merge (heap)** | **O(L · log k)** | Touches only what you return — what the assignment builds |

You only ever look at the head of each list, so you never load a followee's entire history.
**Tie-breaking** must be deterministic — equal timestamps break by post ID — or pagination
cursors drift and the same post can appear twice or vanish.

### Deep dive B — Caching & the read path

The feed is read constantly, so it lives in **cache** (Redis), not queried from the DB each time.

| What's cached | Why | Trade-off |
|---------------|-----|-----------|
| Materialized feed (push) | Read is O(1) — just return the list | Must be invalidated/updated on every relevant post |
| Per-author timelines | The pull source; merged on read | Merge cost on each read; mitigated by caching the merged result briefly |
| Post bodies | Hydrate IDs → full posts | Cheap; bodies are immutable, cache forever |

A common pattern: store **post-IDs** in the feed/timeline (small, cheap to fan out), then
**hydrate** them into full post objects from a content cache on read. IDs are tiny; bodies are
fetched once and shared.

### Deep dive C — Ranking (chronological vs scored)

Reverse-chronological is the simple default and what the assignment implements. Real feeds
**rank**: a model scores each candidate post (recency, your past engagement, author affinity,
predicted likelihood you interact) and sorts by score.

| Feed ordering | Pros | Cons |
|---------------|------|------|
| Reverse-chronological | Predictable, simple, "honest" | Misses content you'd care about; noisy if you follow many |
| Ranked (ML score) | Higher engagement, surfaces the good stuff | Complex, opaque, needs a feature pipeline; can create filter bubbles |

Even ranked feeds first **retrieve** a candidate set (often via the same merge), then **rank** —
so the merge you're building is step one regardless.

### Deep dive D — Edge cases that bite

- **Unfollow / delete:** the feed must reflect it. Pull handles this for free (you simply don't
  read that author anymore). Push must *remove* already-fanned-out posts or filter at read time.
  This asymmetry is a real point in push's debit column.
- **New follow backfill:** when you follow someone, do their *old* posts appear? In pull, yes,
  automatically (you now read their timeline). In push, you'd have to backfill — more work.
- **The "thundering herd" of a celebrity:** even in pull, a celebrity's timeline is read by
  everyone at once; cache that single hot timeline aggressively.

---

## In the wild

- **Twitter/X** pioneered the hybrid: fan-out-on-write for most, but pull-merge for high-follower
  accounts (the classic "fan-out service" + "timeline mixer").
- **Instagram** and **Facebook** layer heavy **ranking** on top of retrieval; the chronological
  merge is just the candidate-generation step.
- **Redis** is the workhorse for both materialized feeds and hot timelines; post bodies sit in a
  content store and are hydrated by ID.

---

## Interview angle

Open by naming the **two strategies** and the read:write ratio that pushes you toward optimizing
reads. Lay out the **push vs pull table** from memory. Then deliver the punchline: **the
celebrity problem breaks pure push** (100M writes from one tweet), so real systems go
**hybrid** — push for normal users, pull-merge for celebrities. That progression
(push → its celebrity flaw → hybrid) is the senior signal.

If asked to make the read efficient, describe the **k-way merge with a heap** over sorted
timelines (O(L log k), not a full sort) — exactly what you build in the assignment. Mention
**deterministic tie-breaking** for stable pagination, and **caching** post-IDs + hydrating bodies.

**Common follow-ups:**
- "A celebrity with 100M followers tweets — walk me through it." → don't fan out; mark them hot;
  merge their timeline into followers' feeds at read time.
- "How do you paginate without duplicates?" → cursor on `(ts, id)` with deterministic tie-break.
- "User unfollows someone — when does the feed update?" → pull: immediately; push: filter at read
  or remove on unfollow (call out the asymmetry).
- "Chronological or ranked?" → retrieve candidates via merge, then optionally rank; explain the
  engagement-vs-simplicity trade-off.

---

## Practice → the Go assignment

Now build the **pull** half — the part that powers the hybrid and contains the real algorithm.
Go to [`assignment/`](assignment/) and implement:

1. **`Follow` / `Unfollow`** — maintain the social graph.
2. **`Post`** — append to the author's timeline, kept **newest-first**.
3. **`GetFeed(user, limit)`** — a **k-way merge** (`container/heap`) over the followees'
   timelines, newest-first (Ts desc, ID desc tie-break), capped at `limit`. **No full sort.**

```bash
cd assignment
go test ./...          # red → implement → green
go test -race ./...    # if you add concurrency, protect shared state with the mutex
```

The tests are deterministic: posts carry explicit timestamps and IDs, so ordering and
tie-breaking are exact and flake-free. The interface is given; you fill in the `// TODO`s.
A reference solution is in [`solution/`](solution/) — try first, peek after.

**Next case study:** [4.8 — Chat System »](../08-chat-system/)
