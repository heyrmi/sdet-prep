# 4.6 — Design a Notification System

> **Module 4 · Case Studies** · ~35 min read + coding assignment
> *Concepts exercised:* fan-out, provider adapters, message queues between components,
> retries + dead-letter queues, idempotency/dedup, rate limiting, user preferences,
> the "at-least-once vs exactly-once" trade-off.

---

## The problem

A **notification system** delivers a message to a user over one or more **channels** —
mobile **push**, **SMS**, **email** (and sometimes in-app, Slack, webhooks). One event
("your order shipped") may go out as a push *and* an email, to millions of users, reliably,
without spamming anyone twice.

Why it's harder than it looks:
- **Third parties fail.** You don't deliver push/SMS/email yourself — you call Apple (APNs),
  Twilio, Amazon SES. They time out, rate-limit you, and have outages. You must **retry**.
- **Retries cause duplicates.** If you retry but the first attempt actually succeeded, the
  user gets the message twice. You need **deduplication**.
- **Volume is spiky.** A marketing blast or an outage alert can enqueue millions of messages
  in seconds. The sending must be **decoupled** from the triggering so a spike doesn't topple
  your app servers.
- **Users have preferences.** "Email me, don't text me." "No promos after 9pm." Respecting
  these is a product requirement, not an afterthought.

> **Analogy.** Think of a **mailroom in a big office**. A request slip ("notify Jia her order
> shipped") drops into an **inbox tray** (a queue). Clerks (workers) pick up slips one at a
> time. Each clerk looks up *how* Jia wants to be reached (her preferences), then hands the
> letter to the right **courier**: the post office for mail, a phone for SMS, a runner for
> hand-delivery. If a courier is busy, the clerk tries again later — but stamps each slip with
> an ID so the same letter never gets sent twice. Slips that fail all day end up in a
> **"problems" bin** (the dead-letter queue) for a human to inspect. The trigger (someone
> filling a slip) is fully decoupled from delivery (clerks working the tray) — that's why a
> sudden pile of slips doesn't stop people from filing new ones.

---

## Step 1: Requirements (always start here)

**Functional**
- Send a notification to a user over one or more channels: push, SMS, email.
- Support different **triggers**: a service event ("payment failed"), a scheduled job
  ("weekly digest"), a marketing campaign.
- Respect **user preferences** (opt-in/opt-out per channel, quiet hours).
- **Retry** transient failures; surface permanent failures somewhere a human can see.
- **Don't send duplicates** for the same logical notification.

**Non-functional**
- **Reliable** — at-least-once delivery is the realistic floor; we layer dedup on top to get
  *effectively* once.
- **Scalable** — handle bursts of millions; sending throughput is bounded by the providers,
  not by us.
- **Low coupling** — a slow email provider must not slow down push, and must not block the
  service that triggered the notification.
- **Observable** — we need delivery rates, failure rates, and per-provider latency.

**Clarifying questions (ask these out loud in an interview)**
- *Which channels?* (Each has its own provider, latency, and failure mode.)
- *What's the delivery guarantee?* Almost always **at-least-once + dedup**. True exactly-once
  across a third party you don't control is effectively impossible — name that.
- *Is ordering required?* Usually no for notifications; saying so simplifies the design.
- *Transactional vs promotional?* Transactional ("OTP code") is latency-sensitive and must not
  be dropped; promotional can be batched, throttled, and is subject to opt-outs.
- *Read scale?* "Soft" things like delivery receipts and an in-app inbox add a read path.

---

## Step 2: Estimation

Suppose **10 million users**, each receiving on average **5 notifications/day**, fanned out to
**2 channels** each.

```
notifications/day      = 10e6 users × 5            = 50 million logical notifications/day
channel sends/day      = 50e6 × 2 channels         = 100 million sends/day
average sends/sec      = 100e6 / 86,400            ≈ 1,160 sends/sec (avg)
peak (≈10× average)    ≈ 11,600 sends/sec
```

A single send to a provider takes ~50–300 ms (a network round trip). At 11.6k sends/sec with
~200 ms each, you need roughly `11,600 × 0.2 ≈ 2,300` concurrent in-flight sends. That's the
sizing argument for a **worker pool + queue**: you decouple "accept the request instantly"
from "do ~2,300 slow provider calls in parallel."

Storage is modest: a notification record (id, user, channels, body, status) is a few hundred
bytes; 50M/day × ~300 B ≈ **15 GB/day** of event log, easily trimmed or archived.

---

## Step 3: High-level design

### The pipeline

```
                                 ┌────────────────────────────────────────────┐
  service event ─┐               │                Notification Service         │
  scheduler ─────┼──► API /      │                                             │
  campaign ──────┘    enqueue ──►│  ┌────────────┐   ┌──────────────────────┐  │
                                 │  │ dedup check │──►│   message queue       │  │
                                 │  │ + prefs     │   │  (jobs to deliver)    │  │
                                 │  └────────────┘   └──────────┬───────────┘  │
                                 │                              │              │
                                 │              ┌───────────────┴───────────┐  │
                                 │              ▼          ▼          ▼      │  │
                                 │          worker      worker      worker   │  │ pool
                                 │              │          │          │      │  │
                                 │              ▼          ▼          ▼      │  │
                                 │        ┌─────────┐ ┌─────────┐ ┌────────┐ │  │
                                 │        │ push    │ │ SMS     │ │ email  │ │  │ adapters
                                 │        │ adapter │ │ adapter │ │ adapter│ │  │
                                 │        └────┬────┘ └────┬────┘ └───┬────┘ │  │
                                 └─────────────┼───────────┼──────────┼──────┘  │
                                               ▼           ▼          ▼
                                            APNs/FCM     Twilio      SES   (3rd-party providers)
                                               │ (on permanent failure)
                                               ▼
                                        ┌──────────────┐
                                        │ dead-letter  │  ← humans/alerts inspect
                                        │ queue (DLQ)  │
                                        └──────────────┘
```

Two ideas do most of the work:

1. **A queue between "accept" and "deliver."** Enqueuing is fast and never blocks the caller.
   Workers drain the queue at whatever rate the providers allow. A spike just lengthens the
   queue; it doesn't crash anything. (See *Module 2.10 — Message Queues*.)
2. **Provider adapters behind one interface.** Each channel has different auth, payload, and
   error semantics. Hide them behind a common `Sender` so the core logic is channel-agnostic.

### API

```
POST /v1/notifications
{
  "id":       "evt-9c1f...",          // caller-supplied idempotency key
  "user_id":  "u_123",
  "channels": ["push", "email"],       // or omit and let preferences decide
  "template": "order_shipped",
  "data":     { "order": "A-77" }
}
→ 202 Accepted   { "id": "evt-9c1f...", "status": "queued" }
```

`202 Accepted` (not `200 OK`) is the honest answer: "I've taken responsibility for this; I
haven't delivered it yet." The `id` is the **idempotency key** — submit it twice and you get
one delivery (more on this below).

### Data model

```
notifications
  id (PK, = idempotency key) | user_id | template | data | created_at | status

delivery_attempts
  id | notification_id (FK) | channel | attempt_no | provider | status | error | at

user_preferences
  user_id | channel | enabled | quiet_hours_start | quiet_hours_end
```

`delivery_attempts` is your audit trail: it tells you "we tried email 3 times, all failed with
provider 5xx" — invaluable when debugging "I never got my email."

---

## Step 4: Deep dives & trade-offs

### Deep dive A — Fan-out: how one event becomes many sends

A single logical notification fans out two ways:

- **Across channels** — one event → push **and** email **and** SMS.
- **Across users** — one campaign → millions of per-user notifications.

For per-user fan-out at campaign scale, you don't enqueue 10M jobs from one request thread;
you enqueue a **batch job** that a fleet of workers expands into per-user notifications. Keep
the unit of work small (one user, one channel) so a failure retries cheaply and workers stay
balanced.

| Fan-out style | What it produces | Watch out for |
|---------------|------------------|---------------|
| Per-channel | N sends from 1 event | Partial success: push ok, email fails — track per channel |
| Per-user (campaign) | Millions of notifications | Expand lazily via workers, not in the request path |

### Deep dive B — Why a queue (and what kind)

The queue is the shock absorber between a bursty producer and rate-limited providers.

| Approach | Pros | Cons |
|----------|------|------|
| Synchronous (call provider in request) | Simplest; immediate result | A slow provider blocks the caller; a spike crashes you; no retry buffer |
| In-process queue + worker pool | Decoupled; absorbs bursts; easy retries | Bounded by one machine; lost if the process dies |
| Durable queue (Kafka/SQS/RabbitMQ) | Survives crashes; horizontal scale; replay | More infra; at-least-once means you must dedup |

The assignment builds the **in-process queue + worker pool** (a buffered channel + goroutines)
— the exact pattern, minus durability. In production you swap the channel for a durable broker;
the worker logic barely changes.

### Deep dive C — Retries, backoff, and the dead-letter queue

Providers fail transiently (timeouts, 429s, brief outages). The rule: **retry transient
failures with exponential backoff and jitter**, give up after `maxAttempts`, and route the
final failure to a **dead-letter queue (DLQ)**.

```
attempt 1 ──fail──► wait ~1s  ──► attempt 2 ──fail──► wait ~2s ──► attempt 3 ──fail──► DLQ
```

| Decision | Option A | Option B | Trade-off |
|----------|----------|----------|-----------|
| Backoff | Fixed delay | Exponential + jitter | Exponential avoids hammering a struggling provider; jitter avoids a synchronized retry stampede |
| Give-up | Retry forever | Cap at maxAttempts → DLQ | Forever risks an infinite loop on a poison message; a cap + DLQ is observable and bounded |
| Transient vs permanent | Retry everything | Inspect the error | A `400 bad phone number` will never succeed — retrying wastes work; only retry 5xx/timeouts/429 |

The DLQ is where "we genuinely couldn't deliver this" goes. Alert on its depth — a growing DLQ
usually means a provider is down or a template is broken.

### Deep dive D — Idempotency & dedup (the senior signal)

At-least-once delivery means the **same job can run twice** (a worker crashes after sending but
before acking; a client retries the API call). Without protection, the user gets two texts.

The fix is an **idempotency key** — the caller-supplied notification `id`. Before sending, check
a **dedup store**: "have I already delivered `id`?" If yes, drop it. If no, send, then record
`id` as done.

```
process(id):
    if seen(id):  return            # already delivered → drop
    deliver(...)                    # the slow provider call(s)
    mark_seen(id)                   # record success so a retry is a no-op
```

| Aspect | Choice | Trade-off |
|--------|--------|-----------|
| Where to dedup | In-memory set | Fast, but lost on restart and not shared across servers |
|  | Redis `SETNX` w/ TTL | Shared + fast; TTL bounds memory; tiny chance of a key expiring too early |
|  | A unique DB constraint | Durable + exact; a write per notification |
| Exactly-once? | Aim for it | True exactly-once *through a third party you don't control* is impossible — you get **at-least-once + dedup = effectively-once**. Say this in interviews. |

A subtle ordering point: should you mark seen *before* or *after* sending? Mark **after** a
confirmed success. If you mark before and the send fails, you've lost the message. Marking after
means a crash mid-send can cause a duplicate — which is exactly what at-least-once accepts, and
why the dedup check exists in the first place. (The assignment marks done only after all channels
succeed.)

### Deep dive E — User preferences & rate limiting

- **Preferences** gate fan-out: before enqueuing a channel, check the user opted in and you're
  not inside their quiet hours. Cache preferences aggressively — they're read on every send and
  change rarely.
- **Per-user rate limiting** prevents notification spam ("don't send the same user more than 5
  promos/day"). This is literally the rate limiter from *Module 4.1*, keyed by `(user, category)`.
- **Per-provider rate limiting** keeps you under the provider's own limits (Twilio caps SMS/sec).
  Exceed it and *they* 429 you — so you self-throttle to stay friends.

---

## In the wild

- **APNs / FCM** are the push providers (Apple, Google). You hold device tokens and POST to them.
- **Twilio / Vonage** for SMS; **Amazon SES / SendGrid** for email — all behind adapters.
- **Amazon SNS** and **Knock**/**Courier** are managed fan-out-to-channels services that
  implement exactly this pipeline so you don't have to.
- **Kafka / SQS / RabbitMQ** are the usual durable queues between accept and deliver; SQS even
  has a native DLQ feature and a "redrive" to replay it.

---

## Interview angle

Lead with the **pipeline**: accept → queue → worker pool → provider adapters, and explain *why*
the queue exists (decouple bursty producers from rate-limited, flaky providers). Then go deep on
the two things interviewers probe:

1. **Reliability:** at-least-once + **retries with exponential backoff** + a **DLQ** for poison
   messages. Distinguish transient (retry) from permanent (don't) failures.
2. **Idempotency:** caller-supplied key + a dedup store, and the honest framing that
   exactly-once through a third party is **at-least-once + dedup = effectively-once**. This is
   the senior signal.

Close with **user preferences** and **per-user rate limiting** to show you remember it's a
product, not just a pipe.

**Common follow-ups:**
- "A worker crashes after sending but before acking — what happens?" → the job is redelivered;
  the dedup check makes the second run a no-op.
- "Email is down for an hour — does push still go out?" → yes, channels are independent adapters
  draining the same pool (or separate per-channel queues so one slow provider can't starve the
  others — a good refinement to mention).
- "How do you stop a buggy campaign from texting everyone twice?" → idempotency keys + per-user
  rate limits + a kill switch on the campaign job.
- "Transactional OTP vs marketing blast — same path?" → often split into priority queues so a
  10M-row campaign can't delay a login code.

---

## Practice → the Go assignment

Now build the core of the pipeline. Go to [`assignment/`](assignment/) and implement:

1. A **`Sender` interface** and pluggable **channel adapters** (`RegisterChannel`).
2. A **worker pool** (a buffered channel + goroutines) that `Notify` enqueues onto.
3. **Retries** up to `maxAttempts`, with an **injected `sleep`** so tests never really wait.
4. **Idempotency**: a notification `id` already delivered is never sent again (a dedup set).
5. A **`Wait()` / `Shutdown()`** that drains the queue cleanly.

```bash
cd assignment
go test ./...          # red → implement → green
go test -race ./...    # must pass: Notify is called from many goroutines
```

The tests are deterministic — time and retries are controlled via the injected `sleep` (a no-op
in tests), so the suite is fast and flake-free. The interface is given; you fill in the `// TODO`s.
A reference solution is in [`solution/`](solution/) — try first, peek after.

**Next case study:** [4.7 — News Feed »](../07-news-feed/)
