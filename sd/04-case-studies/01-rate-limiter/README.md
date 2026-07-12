# 4.1 — Design a Rate Limiter

> **Module 4 · Case Studies** · ~30 min read + coding assignment
> *Concepts exercised:* token bucket, leaky bucket, fixed/sliding window, distributed counters,
> race conditions, the "approximate vs exact" trade-off.

---

## The problem

A **rate limiter** caps how many requests a client can make in a window of time — e.g. "100
requests per minute per user." Past the limit, extra requests are rejected (usually HTTP
`429 Too Many Requests`).

Why you need one:
- **Prevent abuse / DoS** — one client can't drown the service.
- **Control cost** — downstream APIs, LLM tokens, and DB load all cost money.
- **Fairness** — one noisy tenant can't starve everyone else.
- **Security** — slow down brute-force login and scraping.

> **Analogy.** A nightclub with a capacity sign and a bouncer. The bouncer (rate limiter) lets
> people in up to capacity; beyond that, you wait outside (`429`). Different policies = different
> bouncer rules: "max 100 inside" vs "max 10 entering per minute."

---

## Step 1: Requirements (always start here)

**Functional**
- Limit requests per client (by user ID, IP, or API key).
- Return `429` when the limit is exceeded, ideally with a `Retry-After` hint.
- Configurable rules (e.g. different limits per endpoint/tier).

**Non-functional**
- **Low latency** — the limiter is on *every* request's hot path; it must be fast.
- **Accurate enough** — exact counting is nice but often we trade a little accuracy for speed.
- **Distributed** — many app servers must share one logical limit (a user hitting Server A then
  Server B still shares one budget).
- **Fail open or closed?** If the limiter's datastore is down, do you allow all traffic (fail
  open, prioritize availability) or block it (fail closed, prioritize protection)? A trade-off
  to state explicitly.

---

## Step 2: Where does it live?

Three common placements:
1. **Client-side** — easily bypassed; never trust it as the only layer.
2. **Server-side middleware** — in your app, before the handler. Simple, what we'll build.
3. **API gateway / dedicated middleware** — a separate layer (e.g. NGINX, Envoy, Kong, a
   cloud gateway) that limits before traffic even reaches your app. Common at scale.

There's no single right answer — many systems do gateway-level *and* per-service limiting.

---

## Step 3: The algorithms (the heart of the topic)

### 1) Token Bucket  ⭐ (most popular)

Imagine a bucket that holds up to `B` tokens. Tokens are **refilled at a steady rate** `r`
(e.g. 10 tokens/second). Each request **removes one token**. No token? Rejected.

- Allows **bursts** up to the bucket size `B`, then settles to the steady rate `r`.
- Two parameters: capacity `B` (burst) and refill rate `r` (sustained).
- Tiny memory: just `(tokens, last_refill_time)` per client.

```
refill +r/sec
      │
      ▼
  ┌────────┐   request takes 1 token
  │ ●●●●●  │ ───────────────────────► allowed if a token is available
  │  (B)   │   empty? ──────────────► 429
  └────────┘
```

This is what AWS, Stripe, and most APIs use. **It's the default answer in an interview.**

### 2) Leaky Bucket

A queue that processes requests at a **fixed rate** (the "leak"). Requests fill the queue; if
the queue is full, new ones are dropped. Smooths bursts into a steady output stream. Good when
the *downstream* needs a constant rate (e.g. a payment processor). Downside: no burst allowance,
and queued requests add latency.

### 3) Fixed Window Counter

Count requests per fixed clock window (e.g. per minute). Reset to 0 each window.

- Dead simple, tiny memory (`count`, `window_start`).
- **Flaw:** burst at a window boundary. With a limit of 100/min, a client can send 100 at
  `12:00:59` and 100 at `12:01:00` — **200 requests in ~1 second**. The boundary doubles the
  effective rate.

### 4) Sliding Window Log

Store a **timestamp for every request**. To check, drop timestamps older than the window and
count what remains. **Perfectly accurate**, but memory grows with request volume (you store
every request) — expensive at scale.

### 5) Sliding Window Counter ⭐ (great accuracy/cost balance)

A clever hybrid. Keep the current and previous fixed-window counts, and **weight the previous
window by how far we've slid into the current one**:

```
estimated = current_count + previous_count * (overlap fraction of previous window)
```

Example: limit 100/min. At `12:01:15` (25% into the current minute), if previous minute had 80
and current has 10:

```
estimated = 10 + 80 * (1 - 0.25) = 10 + 60 = 70   → under 100, allow
```

Smooths the boundary problem of fixed windows with **O(1) memory** and only a tiny approximation
error. Excellent default for distributed limiters.

### Quick comparison

| Algorithm | Burst handling | Memory | Accuracy | Notes |
|-----------|----------------|--------|----------|-------|
| Token bucket | Allows bursts up to B | O(1) | High | Most common; tune burst vs rate |
| Leaky bucket | Smooths to constant rate | O(queue) | High | Adds latency; constant output |
| Fixed window | Boundary bursts (2×) | O(1) | Low | Simplest; flawed at edges |
| Sliding log | Exact | O(requests) | Exact | Accurate but memory-heavy |
| Sliding counter | Smooth | O(1) | ~High | Best balance for scale ⭐ |

---

## Step 4: Making it distributed

A single in-memory limiter works for one server. With many servers behind a load balancer, each
would keep its *own* count — a user gets `N ×` the intended limit. **The counter must be shared.**

The standard answer: keep counters in a **central fast store, usually Redis.**

- Token bucket / counters live in Redis keyed by client ID.
- **Atomicity matters.** Naive `GET` then `SET` has a **race condition**: two servers read
  `count=99`, both think there's room, both write `100` — the limit is breached. Fix with
  atomic operations: Redis `INCR`, or a **Lua script** that does check-and-update atomically.
- **Latency:** every request now does a network hop to Redis. Mitigate with pipelining, local
  pre-checks, or sharding the Redis tier.

> **Trade-off — exact vs approximate at scale.** Perfectly synchronized global counts are
> expensive. Many large systems accept slight over-counting by using **local limits per node**
> (e.g. global 1000/s split as 100/s across 10 nodes) and only loosely syncing. Cheaper and
> lower latency, at the cost of precision. State this trade-off in interviews.

---

## Step 5: Response design

When rejecting, be a good API citizen:
- Status `429 Too Many Requests`.
- `Retry-After: 5` (seconds) so clients back off intelligently.
- Headers like `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`.

---

## In the wild

- **Stripe** uses token-bucket-style limiting and publishes rate-limit headers.
- **NGINX** ships a leaky-bucket limiter (`limit_req`).
- **Cloud gateways** (AWS API Gateway, Cloudflare) offer token-bucket limiting at the edge.

---

## Interview angle

Lead with **token bucket** and explain burst vs sustained rate. Mention the **fixed-window
boundary bug** to show depth, then offer **sliding window counter** as the refinement. For
"make it work across many servers," go to **Redis + atomic ops (Lua/INCR)** and call out the
**race condition** explicitly — that's the senior signal. Close with the **fail-open vs
fail-closed** decision and the **approximate-at-scale** trade-off.

**Common follow-ups:**
- "Two requests arrive at the exact same moment on two servers — how do you avoid double-spend?"
  → atomic check-and-decrement (Lua script / `INCR`).
- "Redis is down. Now what?" → fail open vs closed; local fallback limits.
- "How would you support different limits per tier/endpoint?" → rule config keyed by
  `(client, route)`; a small policy lookup before the limiter.

---

## Practice → the Go assignment

Now build it. Go to [`assignment/`](assignment/) and implement, in order:

1. A **Token Bucket** limiter — `Allow()` that refills lazily based on elapsed time.
2. A **Fixed Window** counter — and observe the boundary bug in a test.
3. A **Sliding Window Counter** — the weighted hybrid.
4. Make the token bucket **concurrency-safe** (the tests run with `-race`).

```bash
cd assignment
go test ./...          # red → implement → green
go test -race ./...    # must pass: the limiter is shared across goroutines
```

The interface is given; you fill in the `// TODO`s. A reference solution is in
[`solution/`](solution/) — try first, peek after.

**Next case study:** [4.2 — Consistent Hashing / Key-Value Store »](../02-key-value-store/)
