# 2.11 — Rate Limiting

> **Module 2 · Building Blocks** · ~28 min read
> *Every public service has a finite amount of capacity, money, and patience. A rate limiter is
> the polite-but-firm doorman that says "you've had enough for now" — protecting the system from
> abuse, runaway cost, and one noisy client starving everyone else.*

---

## The problem

You ship an API. It works. Then one of these happens:

- A buggy client gets stuck in a retry loop and hammers you **500 times a second**.
- A scraper tries to download your entire catalog in an afternoon.
- An attacker brute-forces passwords against your login endpoint.
- One enterprise customer's batch job floods the service and **everyone else** goes slow.
- Your handler calls an LLM API that charges per token, and the bill quietly explodes.

In every case the *code* is fine. The problem is **volume** — too many requests, too fast. You
need a way to say "I'll serve you up to *this much*, and past that, you wait." That mechanism is
a **rate limiter**: it caps how many requests a client may make in a window of time, and rejects
the excess (usually with HTTP `429 Too Many Requests`).

> **Analogy.** A nightclub with a capacity sign and a bouncer. The bouncer lets people in up to
> capacity; beyond that, you wait outside. Different venues have different rules — "max 100 inside
> at once" vs "max 10 people through the door per minute." Those rules are exactly the different
> *algorithms* we'll meet below. The bouncer isn't being mean; he's keeping the building safe and
> fair for everyone already inside.

Why you'll always want one:

- **Prevent abuse / DoS** — one client can't drown the service.
- **Control cost** — downstream APIs, LLM tokens, bandwidth, and DB load all cost money.
- **Fairness** — one noisy tenant can't starve the rest.
- **Security** — slow brute-force logins and credential stuffing to a crawl.

---

## Core idea

A rate limiter keeps a small piece of **per-client state** (a counter, or a "bucket" of
allowance) and, for each incoming request, asks one question: *given what this client has already
done in the recent past, is this request within the limit?* If yes, allow it and update the
state. If no, reject it.

Two design decisions define every limiter:

1. **The key** — *what* are we counting per? Per user ID, per IP, per API key, per
   `(user, endpoint)` pair. This decides who shares a budget.
2. **The algorithm** — *how* do we count and decide? (Token bucket, sliding window, etc.)

Everything else — where it runs, how it's stored — flows from those two.

---

## Where does it live?

A limiter can sit at several layers, and big systems often use **more than one**:

```
   Client ──► [ API Gateway ] ──► [ Service A ] ──► [ Service B ]
              limit per IP/key      limit per user     limit calls to a
              (coarse, at edge)     (business rule)     downstream dependency
```

1. **Client-side.** The client throttles itself (e.g. a mobile app debounces taps). Nice for UX,
   but **trivially bypassed** — never trust it as your only line of defense.
2. **API gateway / edge.** A dedicated layer (NGINX, Envoy, Kong, Cloudflare, AWS API Gateway)
   limits traffic *before* it reaches your app. Stops abuse cheaply, at the edge, before it costs
   you compute. The common first line at scale.
3. **Service-side middleware.** Inside your app, before the handler — where you know the
   authenticated user and the business rules ("free tier: 100/day; pro: 10,000/day").
4. **Protecting a dependency.** A service limits *its own* calls to a fragile downstream (a
   payment processor, a third-party API) so it doesn't overwhelm it.

> **Trade-off.** Edge limiting is cheap and coarse (per IP — but a NAT'd office shares one IP).
> Service limiting is precise (per authenticated user, per route) but the request has already
> traveled deeper into your stack. Most production systems do **both**: a blunt edge limit to
> shed obvious floods, plus fine-grained per-user limits inside.

---

## The algorithms (conceptual)

Five classics. You don't need to memorize the code — understand the *behavior* and *cost* of
each, because that's the trade-off conversation.

### Token bucket ⭐ (the default answer)

A bucket holds up to `B` tokens and is **refilled at a steady rate** `r` (e.g. 10 tokens/sec).
Each request **removes one token**. No token available? Rejected. Unused tokens accumulate up to
the cap `B`.

```
   refill +r/sec
        │
        ▼
   ┌─────────┐   request takes 1 token
   │ ● ● ● ● │ ───────────────────────►  allowed if a token is available
   │   (B)   │   empty? ──────────────►  429
   └─────────┘
```

- Allows **bursts** up to `B`, then settles to the sustained rate `r`.
- Two knobs: capacity `B` (how big a burst you tolerate) and refill rate `r` (long-run rate).
- Tiny state: just `(tokens, last_refill_time)` per client.

This is what AWS, Stripe, and most APIs use. **Lead with it in an interview.**

### Leaky bucket

A queue drained at a **fixed rate** (the "leak"). Requests fill the queue; if it's full, new ones
are dropped. Output is a perfectly smooth, constant stream — great when the *downstream* needs a
steady rate (e.g. a payment processor). Downsides: **no burst allowance**, and queued requests add
latency.

### Fixed window counter

Count requests per aligned clock window (e.g. per minute); reset to 0 each window.

- Dead simple, tiny state (`count`, `window_start`).
- **The boundary bug:** with a limit of 100/min, a client sends 100 at `12:00:59` and 100 more at
  `12:01:00` — **200 requests in ~1 second**. The window edge effectively doubles the rate.

```
   minute 0                    │ minute 1
   .................[100 reqs]  │ [100 reqs].................
                       12:00:59 │ 12:01:00
                                └─ 200 requests in ~1 second!
```

### Sliding window log

Store a **timestamp for every request**. To decide, drop timestamps older than the window and
count what's left. **Perfectly accurate**, but memory grows with request volume — you literally
store every request. Expensive at scale.

### Sliding window counter ⭐ (best accuracy/cost balance)

A clever hybrid: keep the **current and previous** fixed-window counts, and weight the previous
window by how far you've slid into the current one.

```
   estimated = current_count + previous_count × (fraction of previous window still in view)
```

Example: limit 100/min. At `12:01:15` (25% into the current minute), previous minute had 80,
current has 10:

```
   estimated = 10 + 80 × (1 − 0.25) = 10 + 60 = 70   →  under 100, allow
```

Smooths the boundary bug with **O(1) memory** and only a tiny approximation error. Excellent
default for distributed limiters.

### Quick comparison

| Algorithm | Burst handling | Memory | Accuracy | Notes |
|-----------|----------------|--------|----------|-------|
| Token bucket | Allows bursts up to `B` | O(1) | High | Most common; tune burst vs rate ⭐ |
| Leaky bucket | Smooths to constant rate | O(queue) | High | Adds latency; constant output |
| Fixed window | Boundary bursts (≈2×) | O(1) | Low | Simplest; flawed at edges |
| Sliding log | Exact | O(requests) | Exact | Accurate but memory-heavy |
| Sliding counter | Smooth | O(1) | ~High | Best balance for scale ⭐ |

---

## Making it distributed

A single in-memory limiter is perfect for **one** server. But you run many app servers behind a
load balancer (remember [Module 0.1](../00-foundations/01-scale-zero-to-millions.md)?), and each
keeps its *own* count. A user bouncing across `N` servers gets up to `N ×` the intended limit.

```
                       ┌─► [ Server A ]  count: 100  ┐
   User ──► [ LB ] ────┼─► [ Server B ]  count: 100  ├─► user sent 300, "limit" was 100!
                       └─► [ Server C ]  count: 100  ┘
```

The fix: keep counters in a **central, fast, shared store — usually Redis** (in-memory, sub-ms,
and it offers atomic operations).

```
   [ Server A ] ┐
   [ Server B ] ┼──► [ Redis ]   key: ratelimit:user:42 → one shared count
   [ Server C ] ┘
```

### The race condition (the senior-signal detail)

The naive approach — `GET` the count, check it, `SET` the new value — has a **race**. Two servers
read `count = 99` at the same instant, both conclude "there's room," both write `100`. Two
requests slipped through on the same slot. The limit is breached.

```
   Server A: GET → 99    Server B: GET → 99
   Server A: 99 < 100 ✓   Server B: 99 < 100 ✓     ← both think they're fine
   Server A: SET 100      Server B: SET 100          ← two requests used one slot
```

The fix is **atomic check-and-update** so no two operations interleave:

- Redis `INCR` (atomic increment) for counter-based windows, plus `EXPIRE` to roll the window.
- A **Lua script** for anything needing read-modify-write logic (token bucket refill, sliding
  window weighting). Redis runs the whole script atomically — no interleaving possible.

> **Trade-off — exact vs approximate at scale.** Perfectly synchronized global counts cost a
> network round-trip to Redis on *every* request — that's latency on your hot path, plus Redis
> becomes a bottleneck and a dependency. Many large systems accept slight over-counting by giving
> each node a **local share** of the budget (global 1000/s = 100/s on each of 10 nodes) and only
> loosely syncing. Cheaper and faster, at the cost of precision. **State this trade-off out loud.**

---

## When the limiter's store is down: fail open or fail closed?

Redis goes down. Now your limiter can't check anything. You must choose, *in advance*:

- **Fail open** — allow all traffic through. Prioritizes **availability**; an outage of the
  limiter doesn't take down your service. Risk: an attacker who knocks out Redis gets unlimited
  access.
- **Fail closed** — reject all traffic. Prioritizes **protection**; nothing slips past the limit.
  Risk: a limiter blip becomes a full outage for legitimate users.

```
   Redis down ──► fail OPEN  ──► serve everyone   (availability first)
              └─► fail CLOSED ──► reject everyone  (protection first)
```

There's no universal right answer — it depends on what the limiter protects. A login brute-force
guard might fail **closed** (security matters most); a general API limiter often fails **open**
with a local in-memory fallback so users aren't punished for *your* infrastructure hiccup. The
key interview move is to **name the choice and justify it.**

---

## Responding well: 429 and Retry-After

When you reject, be a good API citizen so clients can back off intelligently instead of hammering
you harder:

- Status **`429 Too Many Requests`**.
- **`Retry-After: 5`** — seconds (or an HTTP date) telling the client when to try again.
- Informational headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`.

```
   HTTP/1.1 429 Too Many Requests
   Retry-After: 5
   X-RateLimit-Limit: 100
   X-RateLimit-Remaining: 0
   X-RateLimit-Reset: 1718450400
```

A client that respects `Retry-After` becomes part of the solution. A client ignorant of it keeps
retrying instantly and makes the overload worse — which is exactly why limiting at the **edge**
(so the flood never reaches your app) matters.

---

## Trade-offs & key takeaways

- **A rate limiter sits on every request's hot path** — it must be **fast** and **cheap**. Latency
  here is latency everywhere.
- **Token bucket is the default**; it cleanly separates *burst* (`B`) from *sustained rate* (`r`).
- **Fixed window is simplest but has the boundary bug;** sliding window counter fixes it with
  O(1) memory — the best balance at scale.
- **Distributed limiting needs a shared store and atomic ops.** The naive GET/SET race is the
  classic bug; solve it with `INCR` or a Lua script.
- **Exact global counts cost latency.** Local per-node budgets trade a little precision for a lot
  of speed — a deliberate trade-off.
- **Decide fail-open vs fail-closed before the outage**, based on what you're protecting.
- **Reject with `429` + `Retry-After`** so well-behaved clients back off instead of pile on.

---

## In the wild

- **Stripe** uses token-bucket-style limiting and publishes `X-RateLimit-*` headers.
- **NGINX** ships a leaky-bucket limiter (`limit_req`); **Envoy** and **Kong** offer pluggable
  limiters at the gateway.
- **Cloudflare / AWS API Gateway** do token-bucket limiting at the edge, before traffic reaches
  origin servers.
- **GitHub** publishes per-hour limits and `Retry-After` on its REST API.
- **Redis** is the de-facto backing store for distributed limiters; the popular pattern is a small
  Lua script for atomic check-and-update.

---

## Interview angle

Lead with **token bucket** and explain burst (`B`) vs sustained rate (`r`). Mention the
**fixed-window boundary bug** to show depth, then offer **sliding window counter** as the
refinement. For "make it work across many servers," go to **Redis + atomic ops (Lua/`INCR`)** and
call out the **race condition** explicitly — that's the senior signal. Close with the
**fail-open vs fail-closed** decision and the **approximate-at-scale** (local budgets) trade-off.

**Common follow-ups:**

- *"Two requests arrive at the same instant on two servers — how do you avoid double-spend?"*
  → atomic check-and-decrement (Lua script / `INCR`).
- *"Redis is down — now what?"* → fail open vs closed; local in-memory fallback limits.
- *"Different limits per tier/endpoint?"* → rule config keyed by `(client, route)`; a small policy
  lookup before the limiter.
- *"The limiter itself is becoming a bottleneck."* → local per-node budgets, pipelining, sharding
  the Redis tier, or limiting at the edge.

---

## Self-check

1. Why is the token bucket said to allow *bursts*? Which parameter controls burst size and which
   controls the long-run rate?
2. Describe the fixed-window boundary bug concretely. How does the sliding window counter fix it,
   and at what (small) cost?
3. You run 10 app servers, each with its own in-memory counter. A user is limited to 100/min but
   sends 600 successfully. What happened, and what's the standard fix?
4. Explain the GET-then-SET race in a distributed limiter and the atomic operation that prevents it.
5. Your limiter's Redis instance just died. Walk through the fail-open and fail-closed choices —
   when would you pick each?

---

## Practice → the full coding assignment

This is a big enough topic to earn its own end-to-end case study. Head to the **Rate Limiter case
study**, where you'll implement — behind one `Limiter` interface — a **token bucket**, a **fixed
window** (and watch the boundary bug appear in a test), and a **sliding window counter**, then make
the token bucket **concurrency-safe** (the tests run with `-race`).

```bash
cd ../04-case-studies/01-rate-limiter/assignment
go test ./...          # red → implement → green
go test -race ./...    # the limiter is shared across goroutines
```

Go to **[4.1 — Design a Rate Limiter](../04-case-studies/01-rate-limiter/)**. The design doc there
picks up exactly where this lesson leaves off.

**Next:** [2.12 — Distributed Unique IDs »](12-unique-id-generation.md)
