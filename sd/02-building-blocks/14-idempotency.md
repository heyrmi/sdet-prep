# 2.14 — Idempotency & Exactly-Once

> **Module 2 · Building Blocks** · ~28 min read
> *Networks lie. A request can succeed and still look like it failed, so clients retry — and now
> the payment runs twice. Idempotency is the discipline of making "do it again" mean "no harm
> done." It's the quiet foundation under every reliable payment, queue, and API.*

---

## The problem

A user taps **"Pay $50."** Your server charges the card, then — just before the response comes
back — the network drops. The user's app sees a timeout. Did it work? Nobody knows. The app does
the natural thing: it **retries.** Now the card is charged **$100**.

```
   App ──"charge $50"──► Server   (charges the card ✓)
   App ◄──── X timeout ──         (response lost — app thinks it failed)
   App ──"charge $50"──► Server   (charges AGAIN ✗ → $100 total)
```

The server did nothing wrong each time. The disaster comes from a fact you cannot escape in
distributed systems: **you can't tell "the request failed" apart from "the request succeeded but
the reply got lost."** A timeout is ambiguous. The only safe client behavior is **retry** — which
means your server *will* receive duplicate requests. The question is whether duplicates do
**damage**.

> **Analogy.** An elevator call button. Press it once, press it ten times — the elevator still
> comes exactly once. The button is **idempotent**: repeating the action doesn't change the result
> beyond the first press. Now imagine a vending machine that dispensed a snack *and charged you*
> every time you pressed, even after it jammed and you pressed again. That machine is **not**
> idempotent — and it's exactly the bug above. We want our payment endpoint to behave like the
> elevator button, not the broken vending machine.

---

## Core idea: what "idempotent" means

An operation is **idempotent** if performing it **multiple times has the same effect as performing
it once.** The *result* of the first successful application is preserved; later identical
applications change nothing further.

Some operations are **naturally idempotent**:

- `SET balance = 100` — run it 5 times, balance is still 100. (Setting an absolute value.)
- HTTP `GET`, `PUT`, `DELETE` — fetching, replacing, or deleting a specific resource. `DELETE /x`
  twice leaves `x` deleted either way.

Some are **naturally NOT idempotent** — the dangerous ones:

- `balance = balance + 50` — run it twice, you added 100. (Relative change / accumulation.)
- HTTP `POST` (create a new order, charge a card) — each call *creates a new thing*.

> **Note on HTTP verbs.** The spec *defines* `GET`/`PUT`/`DELETE` as idempotent and `POST` as not.
> That's a contract telling clients and proxies "it's safe to retry these automatically." Our job
> is to make the operations that *aren't* naturally idempotent (mostly `POST`s) **behave** as if
> they were.

---

## Why it matters: retries + at-least-once delivery

Idempotency isn't a nice-to-have; it's forced on you by how reliable systems work.

Recall from [Module 2.10](10-message-queues-streaming.md) that the pragmatic default for message
queues is **at-least-once delivery**: the broker re-delivers any message it didn't get an ack for.
A consumer that crashes after doing the work but *before* acking will see the **same message
again**. Same story for HTTP retries, load balancer retries, and client back-off logic.

```
   At-least-once world:   every message / request may arrive 1, 2, or more times.
   Your only defense:     make processing it twice harmless → idempotency.
```

So the rule of thumb across the whole course:

> **You don't get exactly-once delivery for free** (it's effectively impossible over an unreliable
> network). What you build instead is **at-least-once delivery + an idempotent consumer**, which
> yields **exactly-once *effects*.** That equation is the practical definition of "exactly-once,"
> and it's worth stating verbatim in an interview.

```
   exactly-once effect  =  at-least-once delivery  +  idempotent processing
```

---

## Making POSTs idempotent: the idempotency key

For naturally-idempotent operations you're done. The hard case is **`POST` / create / charge**.
The standard technique is an **idempotency key**.

**The contract:** the *client* generates a unique key (a UUID — see
[Module 2.12](12-unique-id-generation.md)) for each *logical* operation and sends it with the
request, usually as an `Idempotency-Key` header. On a retry of the *same* logical operation, it
resends the **same key.** The server uses the key to recognize and de-duplicate.

```
   Attempt 1:  POST /charge   Idempotency-Key: 7f3a...   → server charges, stores result under 7f3a...
   (timeout — client never saw the response)
   Attempt 2:  POST /charge   Idempotency-Key: 7f3a...   → server SEES key 7f3a..., does NOT recharge,
                                                            returns the stored original result
```

Server-side flow:

```
1. Read the idempotency key from the request.
2. Look it up in a dedup store (table / Redis).
   ├─ FOUND  → this is a duplicate. Return the saved response. Do NOT re-run the side effect.
   └─ ABSENT → first time. Atomically claim the key, perform the operation, store the result
               under the key, then return it.
```

The make-or-break detail is **step 2's atomicity.** If two retries arrive *simultaneously*, both
might find the key absent and both charge the card — the same race condition from
[Module 2.11](11-rate-limiting.md). You must **claim the key atomically**: e.g. a unique constraint
on the key column (the second `INSERT` fails), `INSERT ... ON CONFLICT DO NOTHING`, or Redis
`SET key value NX`. Whoever wins the claim does the work; the loser waits and returns the stored
result.

---

## The dedup table

Concretely, you keep a **dedup (idempotency) table**:

```
   idempotency_keys
   ┌──────────────┬───────────┬───────────────────────┬─────────────┐
   │ key (UNIQUE) │ status    │ response (stored)      │ created_at  │
   ├──────────────┼───────────┼───────────────────────┼─────────────┤
   │ 7f3a...      │ completed │ {"charge_id":"ch_99"}  │ 12:00:01    │
   │ a1b2...      │ in_progress (claimed, working...)  │ 12:00:04    │
   └──────────────┴───────────┴───────────────────────┴─────────────┘
```

- A **`UNIQUE`** constraint on `key` is what makes the claim atomic — the database rejects a second
  insert of the same key.
- Storing the **response** lets you return the *identical* result on a retry, not just "already
  done."
- A **status** (`in_progress` → `completed`) handles concurrent retries: the second one sees
  `in_progress` and waits/polls rather than re-charging.

For event/message consumers the same idea appears as a **processed-IDs set**: before acting on
message `m`, check "have I already processed `m.id`?" — often a Bloom filter or a Redis set (this is
exactly the dedup the notification and ad-click systems lean on, Modules 4.6 and 4.15).

---

## Natural vs synthetic idempotency

Two ways to get there — prefer the first when you can:

- **Natural idempotency.** Design the operation so repeating it is inherently safe, no bookkeeping:
  - **Upsert** instead of insert (`INSERT ... ON CONFLICT DO UPDATE`) — re-running converges to the
    same row.
  - **Set absolute state**, not deltas (`status = 'shipped'` rather than "advance to next status").
  - Use a **client-provided unique business key** (e.g. `order_id`) as the primary key — a duplicate
    create just collides harmlessly.
- **Synthetic idempotency.** When the operation *can't* be made naturally safe (charging a card has a
  real external side effect), bolt on the **idempotency-key + dedup-table** machinery above.

> The senior instinct: **reach for natural idempotency first.** A `PUT` that sets a value, or an
> upsert keyed on a business ID, needs no dedup table and no race-prone claim logic. Synthetic keys
> are the tool for the irreducibly non-idempotent side effects (payments, sending an email, calling
> a third party).

---

## TTL: don't keep keys forever

The dedup store would grow without bound, so idempotency keys carry a **TTL (time-to-live)** — keep
them just long enough to cover the realistic retry window (minutes to a day or two), then expire
them. Stripe, for instance, retains idempotency keys for **24 hours**.

The trade-off to state: **too short** a TTL and a late retry (a client that backed off for an hour)
sneaks past as a "new" request → a double charge. **Too long** and your dedup store bloats. Size the
TTL to your **maximum realistic retry horizon** and no longer.

```
   key created ──────[ TTL: 24h, retries deduped ]──────► key expires
                                                          (a retry after this is treated as new)
```

---

## Trade-offs & key takeaways

- **Retries are inevitable** because a timeout can't be distinguished from a lost response. Your
  job is to make duplicate processing **harmless**, not to prevent duplicates.
- **Exactly-once effect = at-least-once delivery + idempotent consumer.** Memorize this; true
  exactly-once delivery is a myth over unreliable networks.
- **Prefer natural idempotency** (upserts, absolute state, business keys) — no extra machinery,
  no races.
- **For irreducible side effects, use an idempotency key + dedup table.** The claim **must be
  atomic** (`UNIQUE` constraint / `INSERT ON CONFLICT` / Redis `NX`) or concurrent retries race.
- **Store the original response** so retries return identical results, and **TTL the keys** —
  long enough to cover real retries, short enough not to bloat.
- **`GET`/`PUT`/`DELETE` are idempotent by contract; `POST` is the one you must engineer.**

---

## In the wild

- **Stripe** — the canonical example: an `Idempotency-Key` header on requests, dedup with stored
  responses, keys retained ~24h. Their docs are a great reference design.
- **PayPal, Adyen, Square** — same idempotency-key pattern on payment APIs.
- **AWS** — many SDKs send a `ClientRequestToken` / client token for idempotent creates; SQS
  FIFO queues offer a `MessageDeduplicationId`.
- **Kafka** — the **idempotent producer** (`enable.idempotence`) dedups retried produces via a
  producer ID + sequence number, giving exactly-once *within* a producer session.
- **HTTP / REST design** — well-behaved APIs document which endpoints are idempotent so clients and
  proxies retry safely.

---

## Interview angle

The moment a design mentions **payments, retries, queues, or at-least-once delivery**, raise
idempotency *unprompted* — it's a top senior signal. Walk the failure: timeout → ambiguous → client
retries → duplicate. Then give the fix: **idempotency key + dedup table**, stressing the **atomic
claim** (the race), the **stored response**, and the **TTL**. Tie it back to messaging with the
mantra **"exactly-once = at-least-once + idempotent consumer."** Bonus points for preferring
**natural idempotency** (upsert / business key) when the operation allows it.

**Common follow-ups:**

- *"A payment got charged twice — root cause and fix?"* → retry after a lost ack/response;
  idempotency key + atomic dedup so the second attempt returns the first result without re-charging.
- *"Two retries hit the server at the exact same time."* → race on the dedup check; make the claim
  atomic (`UNIQUE` constraint / `SET NX`); the loser returns the stored result.
- *"Can you guarantee exactly-once delivery?"* → not delivery; you get exactly-once *effects* via
  at-least-once + idempotent processing.
- *"How long do you keep idempotency keys?"* → a TTL covering the realistic retry window (e.g. 24h);
  trade double-charge risk against store bloat.

---

## Self-check

1. Why can't a client tell "my request failed" apart from "it succeeded but the response was lost,"
   and why does that force you to make the server idempotent?
2. Give one naturally-idempotent operation and one that isn't. How would you make the
   non-idempotent one safe to retry?
3. Walk through the idempotency-key flow on a `POST /charge`, including what happens on a retry with
   the same key. Where is the atomicity critical, and why?
4. Explain "exactly-once = at-least-once delivery + idempotent consumer." Why don't we just build
   true exactly-once delivery?
5. What goes wrong if an idempotency key's TTL is too short? Too long?

---

## Practice

There's no standalone Go assignment for this lesson — idempotency is a **discipline** woven through
the case studies rather than a single data structure. You'll *apply* it directly when you build the
**[Notification System](../04-case-studies/06-notification-system/)** (dedup so a user isn't paged
twice), the **[Ad Click Aggregation](../04-case-studies/15-ad-click-aggregation/)** pipeline
(exactly-once counting on an at-least-once stream), and the
**[Payment System](../04-case-studies/17-payment-system/)** (idempotency keys + ledgers). When you
reach those, return to this lesson — the dedup-table and atomic-claim pattern here *is* the answer
their tests expect.

**Next:** [3.1 — Replication & Consensus (Raft, Paxos) »](../03-distributed-systems/01-consensus-raft-paxos.md)
