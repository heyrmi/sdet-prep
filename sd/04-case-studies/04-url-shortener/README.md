# 4.4 — Design a URL Shortener

> **Module 4 · Case Studies** · ~30 min read + coding assignment
> *Concepts exercised:* base62 encoding, hashing vs counters, read-heavy caching,
> 301 vs 302 redirects, collision handling, the "generate vs dedup" trade-off.

---

## The problem

A **URL shortener** (TinyURL, bit.ly, the `t.co` links in tweets) takes a long URL and gives
you a short one:

```
https://www.example.com/some/very/long/path?utm=abc&ref=xyz   →   https://bit.ly/3xK9aFq
```

Visiting the short link **redirects** your browser to the original. That's the whole product:
**shorten** a URL, then **resolve** it back later. Around that core you bolt on the usual
extras — custom aliases ("vanity" links like `bit.ly/my-talk`), link expiry, and click
analytics.

Why this is a classic interview question: it looks trivial ("just put it in a map!") but the
moment you ask "how do we generate the short code at scale, and how do we serve billions of
redirects with low latency?" it opens up encoding schemes, caching, and database choice — a
clean tour of system-design fundamentals.

> **Analogy.** A coat check at a theater. You hand over your coat (the long URL); the attendant
> gives you a small numbered ticket (the short code). The ticket is tiny and easy to carry. Later
> you present the ticket and they fetch your exact coat. The hard parts are the same: tickets
> must be **unique**, **short**, and the lookup from ticket → coat must be **fast** even when the
> cloakroom holds millions of coats.

---

## Step 1: Requirements (always start here)

**Functional**
- **Shorten:** given a long URL, return a unique short code (e.g. `3xK9aFq`).
- **Redirect:** given a short code, send the browser to the original URL.
- **Custom alias:** let users request a specific code (`bit.ly/my-talk`), rejected if taken.
- **Expiry (optional):** links can have a TTL after which they 404.
- **Analytics (optional):** count clicks, maybe by time/region/referrer.

**Non-functional**
- **Read-heavy.** Far more redirects than creations (people share one link, thousands click it).
  Optimize the *read* path above all else.
- **Low latency** on redirects — it's on the critical path of someone loading a page.
- **High availability.** A dead shortener breaks every link anyone ever made. Links must not rot.
- **Short codes.** 7 characters is plenty (we'll see why in estimation).
- **Not easily guessable** (mild requirement) — sequential codes leak how many links exist and
  let people enumerate others' links.

**Clarifying questions to ask the interviewer**
- Read:write ratio? (Drives the whole design — assume ~**100:1**.)
- How many new URLs per month / over the product's lifetime? (Sizes the key space.)
- Custom aliases supported? Expiry? Analytics? (Scope control.)
- Can the same long URL map to one code (dedup) or always a fresh code? (Affects storage + the
  data model — we'll dedup.)
- How short must codes be, and must they be unguessable?

---

## Step 2: Estimation (back-of-the-envelope)

Assume **100 million** new URLs per month.

- **Writes:** 100M / month ≈ 100M / (30 × 86400 s) ≈ **~40 writes/sec**.
- **Reads:** at 100:1, ≈ **~4,000 redirects/sec**. *Read-heavy — cache the reads.*
- **Storage:** over 5 years, 100M × 12 × 5 = **6 billion** URLs. Say ~500 bytes per row
  (short code + long URL + metadata) → 6B × 500B ≈ **3 TB**. Fits comfortably on a sharded DB.
- **Key space:** how long must the code be? With a **base62** alphabet `[0-9A-Za-z]` (62
  symbols):

  | Code length | Combinations (62ⁿ) |
  |-------------|--------------------|
  | 5 | ~916 million |
  | 6 | ~56 billion |
  | **7** | **~3.5 trillion** ⭐ |
  | 8 | ~218 trillion |

  **7 characters → 3.5 trillion** codes — orders of magnitude more than the 6B we'll ever need.
  **7 is the standard answer.**

The takeaway that drives every later decision: **this system reads ~100× more than it writes.**

---

## Step 3: High-level design

### API sketch

```
POST /shorten
  body: { "url": "https://example.com/long...", "alias": "my-talk"(optional), "ttl": 3600(optional) }
  201:  { "code": "3xK9aFq", "short": "https://sho.rt/3xK9aFq" }
  409:  alias already taken

GET /{code}
  302 Found
  Location: https://example.com/long...
  (or 404 if unknown/expired)
```

### 301 vs 302 — a real trade-off

When you redirect, you choose a status code:

| | **301 Moved Permanently** | **302 Found (temporary)** |
|---|---|---|
| Browser/CDN caches it? | Yes, aggressively | No (re-asks each time) |
| Redirect latency | Faster (cached after first hit) | One round-trip every click |
| You see every click? | **No** — cached hits skip your server | **Yes** — every click reaches you |
| Good for | Reducing load, static mappings | **Analytics**, expiry, changeable targets |

A pure load story prefers **301**. But most shorteners want **click analytics** and the ability
to expire/disable links, so they use **302** to make sure every click comes back to them. *State
this trade-off explicitly in an interview* — it's the kind of detail that signals depth.

### Data model

One table is the whole product:

```
urls
  code        VARCHAR(7)  PRIMARY KEY   -- the short code, e.g. "3xK9aFq"
  long_url    TEXT        NOT NULL
  created_at  TIMESTAMP
  expires_at  TIMESTAMP   NULL          -- for TTL
  -- optional: owner_id, click_count
```

For **dedup** (same long URL → same code) we also index the long URL, e.g. a `hash(long_url)`
column with a unique index, so a repeat submission can find the existing row instead of minting
a new code.

### Component diagram

```
                         ┌──────────────────────────────────────┐
   client ──POST /shorten─►                                      │
                         │     App / API servers (stateless)     │
   client ──GET /{code}──►                                      │
                         └───────┬───────────────────┬──────────┘
                            read │                    │ write
                                 ▼                    ▼
                         ┌───────────────┐    ┌────────────────┐
                         │  Cache (Redis)│    │  ID generator  │
                         │  code→longURL │    │ (counter/Snow- │
                         │  hot links    │    │  flake)        │
                         └───────┬───────┘    └────────────────┘
                            miss │                    │
                                 ▼                    ▼
                         ┌──────────────────────────────────────┐
                         │   Database (code → long_url),         │
                         │   sharded by code, replicated         │
                         └──────────────────────────────────────┘
```

The redirect path is: **cache hit → done** (the common case); **miss → DB → fill cache → done**.

---

## Step 4: Deep dive — how do we generate the short code?

This is the heart of the problem. Two families of approaches.

### Approach A — Hash the long URL, take a prefix

Run the long URL through a hash (MD5, SHA-1), then base62-encode and **keep the first 7 chars**.

- ✅ **Stateless** — no shared counter; any server computes the same code from the same URL.
- ✅ **Natural dedup** — the same URL hashes to the same code for free.
- ❌ **Collisions.** Two different URLs can share a 7-char prefix. You must check the DB: if the
  code exists for a *different* URL, perturb the input (append a salt and re-hash) and retry.
  Collisions are rare with 3.5T slots but you must handle them, which costs an extra read.
- ❌ Codes look random (fine) but the collision-check read is on the write path.

### Approach B — Encode a unique number (counter) in base62 ⭐

Keep a **monotonic counter**. Each new URL gets the next integer ID; the short code is that ID
**base62-encoded**.

- ✅ **No collisions ever** — each ID is unique by construction, so each code is unique.
- ✅ **Short codes naturally** — ID `1` → `"1"`, ID `1,000,000,000` → 6 chars. Codes grow only
  as you mint more.
- ❌ **Sequential / guessable** — `abc`, `abd`, `abe`… leaks volume and enables enumeration.
  Mitigations: start the counter at a large offset, or scramble IDs with a reversible
  permutation before encoding.
- ❌ **You need a distributed unique counter.** A single DB auto-increment is a bottleneck and a
  single point of failure. The scalable answer is the **Unique ID Generator** from
  [4.3](../03-unique-id-generator/) — Snowflake-style IDs, or handing each app server a *range*
  of IDs to mint locally (a "ticket server" / range allocation).

**Most interviewers want Approach B + base62**, with the distributed-ID discussion pointing at
Snowflake. It's clean, collision-free, and connects to the wider curriculum.

### What is base62?

Base62 is just **counting in a 62-symbol alphabet** instead of 10 (decimal) or 16 (hex). The
alphabet is `0-9` then `A-Z` then `a-z` = 62 characters. To encode a number you do exactly what
you did in grade school converting to another base: repeatedly take `n % 62` to get the next
digit, then `n /= 62`, until `n` is 0.

```
encode(125):
  125 % 62 = 1  → '1' ;  125 / 62 = 2
    2 % 62 = 2  → '2' ;    2 / 62 = 0  (stop)
  digits collected low→high: ['1','2'] → reverse → "21"
decode("21"): 2*62 + 1 = 125  ✓
```

Why base62 over hex? **Density.** More symbols per character means shorter codes for the same
number. And unlike base64 it avoids `+`, `/`, `=` — every character is URL-safe with no escaping.
You implement `Encode`/`Decode` in the assignment.

### Avoiding duplicates (the dedup trade-off)

Should submitting the **same** long URL twice give the same code or a new one?

| | **Dedup (one code per URL)** | **Always mint a new code** |
|---|---|---|
| Storage | Smaller (no duplicate rows) | Grows with every submit |
| Extra read on write | Yes — look up the URL first | No |
| Per-user analytics / expiry | Harder (one code is shared) | Easy (each link independent) |
| Simplicity | A reverse index on the URL | Trivial |

We'll implement **dedup**: keep a **reverse map `longURL → code`** so an identical URL returns
the code we already minted. It saves storage and is the common interview expectation — just call
out that products which need *per-link* analytics or expiry often choose to mint fresh codes
instead.

---

## Step 5: Deep dive — serving reads fast (caching)

Redirects dominate traffic (~100:1) and the working set is **hot**: a small fraction of links
get most clicks (a viral link, a pinned tweet). That's a textbook **cache** case.

- Put a **cache** (Redis/Memcached) in front of the DB keyed by `code → long_url`.
- On redirect: check cache → hit, redirect immediately; miss → read DB, **fill the cache**
  (cache-aside / lazy loading), then redirect. Use an **LRU**-style eviction so hot links stay
  resident.
- Mappings are **immutable** (a code always points to the same URL), so cache invalidation —
  normally the hard part — is trivial here. Set a TTL and you're done.

**DB choice.** The access pattern is a simple key→value lookup by primary key, no joins, with
huge scale and a need for horizontal sharding. That favors a **key-value / wide-column store**
(DynamoDB, Cassandra) **sharded by code**. A SQL DB works fine too at moderate scale and gives
you the unique index for dedup easily. *The trade-off:* SQL = easy uniqueness + transactions but
harder to shard; NoSQL = trivial horizontal scale but you enforce uniqueness yourself. Either is
defensible — say why you'd pick one given the scale.

---

## In the wild

- **bit.ly** uses 7-character base62 codes and supports custom aliases + rich analytics (hence
  302-style redirects so every click is recorded).
- **Twitter `t.co`** wraps every shared link to measure clicks and screen for malware.
- **TinyURL** popularized the pattern; many internal "go links" at companies are the same idea.
- A common scaling pattern is **range/ticket allocation**: a central service hands each app
  server a block of IDs (say 1,000 at a time) to mint locally, avoiding a per-request hop to a
  shared counter.

---

## Interview angle

Open by stating it's **read-heavy (~100:1)** and that the design optimizes redirects. Give the
API (`POST /shorten`, `GET /{code}`) and immediately raise **301 vs 302**, choosing 302 for
analytics. For code generation, present **both** hashing (stateless, but collisions) and
**counter + base62** (collision-free, but needs a distributed ID source) — then pick **base62 of
a distributed ID** and point at **Snowflake / range allocation**. Add **caching of hot
redirects** and note that immutable mappings make invalidation easy. Close with the **dedup
trade-off** and **DB choice** (KV/wide-column sharded by code).

**Common follow-ups:**
- "How do you make codes unguessable while using a counter?" → large starting offset, or a
  reversible permutation/encryption of the ID before base62.
- "Two servers mint at the same instant — how do you avoid duplicate codes?" → don't share a
  naive counter; use Snowflake-style IDs or per-server ID ranges (no coordination per request).
- "How do custom aliases coexist with generated codes?" → check the alias against the same key
  space; reject (`409`) if taken; reserve a namespace or just store it as a normal `code` row.
- "How would you add analytics without slowing redirects?" → log click events to a queue/stream
  asynchronously; aggregate offline (see [Ad Click Aggregation](../15-ad-click-aggregation/)).

---

## Practice → the Go assignment

Now build the core. Go to [`assignment/`](assignment/) and implement, in order:

1. **`Encode(n uint64) string` / `Decode(s string) (uint64, error)`** over the `[0-9A-Za-z]`
   alphabet — the round-trip must be exact (including `0 → "0"`).
2. A **`Shortener`** backed by an in-memory map with an **injectable ID source** (so tests are
   deterministic): `Shorten` returns the base62 of the next ID; `Resolve` looks a code back up.
3. **`ShortenCustom`** — accept a caller-chosen alias, rejecting it if already taken.
4. **Dedup** — store a reverse map so the *same* long URL returns the *same* code.
5. Make `Shorten` **concurrency-safe** (tests run with `-race`): every concurrent call must get
   a unique, resolvable code.

```bash
cd assignment
go test ./...          # red → implement → green
go test -race ./...    # must pass: Shorten is called from many goroutines
```

The interface is given; you fill in the `// TODO`s. A reference solution is in
[`solution/`](solution/) — try first, peek after.

**Next case study:** [4.5 — Web Crawler »](../05-web-crawler/)
