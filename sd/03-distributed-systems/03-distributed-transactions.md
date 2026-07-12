# 3.3 — Distributed Transactions (2PC, Saga, Outbox)

> **Module 3 · Distributed Systems** · ~32 min read
> *On one database, a transaction is easy: everything commits, or nothing does. Spread that change
> across two services or two shards, and "all-or-nothing" becomes one of the hardest problems in
> distributed systems. This lesson covers the classic answer (2PC), why it's dangerous, and the
> patterns modern systems actually use instead (Sagas and the Outbox).*

---

## The problem

A user transfers $100 from their checking account to their savings account. On a single database,
you wrap it in a transaction:

```
BEGIN
  UPDATE checking SET balance = balance - 100
  UPDATE savings  SET balance = balance + 100
COMMIT
```

The database guarantees **atomicity**: both updates happen, or neither does. You can never lose
$100 into the void, even if the power dies mid-way.

Now split the system: checking lives in the **Payments service**, savings in the **Banking
service** — separate databases, separate machines. You debit checking. Then you call Banking to
credit savings… and **Banking is down**. Now $100 has vanished from checking and never arrived in
savings. There's no single `COMMIT` spanning two databases. Each can only commit *itself*.

> **Analogy.** A wedding vow. The officiant needs *both* people to say "I do" for the marriage to
> be valid. If one says yes and the other hesitates, you can't have a "half marriage." But unlike a
> wedding, our two parties are in different buildings, the phone lines are flaky, and either person
> might faint right after saying yes but before signing the register. How do you guarantee they
> *both* commit, or *both* walk away — across an unreliable network?

A **distributed transaction** is the attempt to make a change that spans multiple independent
systems **atomic**: all of them apply it, or none do.

---

## Core idea

There are fundamentally two strategies, and they sit at opposite ends of a trade-off:

1. **Try to keep true atomicity** across systems with a coordination protocol — **Two-Phase
   Commit (2PC)**. Strong guarantee, but it *blocks* and creates a fragile dependency. Best within
   one trusted boundary (e.g. shards of one database).
2. **Give up instant atomicity** and instead build a sequence of local transactions that, taken
   together, leave the system consistent *eventually* — **Sagas**. Each step commits locally; if a
   later step fails, you run **compensating** steps to undo the earlier ones. This is how most
   microservice architectures actually work.

The deep lesson: across service boundaries, **you usually trade strict atomicity for eventual
consistency**, and you lean on **idempotency** and reliable messaging to make it safe.

---

## How it works: Two-Phase Commit (2PC)

2PC introduces a **coordinator** that drives all the **participants** (the databases/services)
through two phases.

**Phase 1 — Prepare (voting).** The coordinator asks every participant: "Can you commit this? Lock
the rows, write to your log, and promise me you *can* commit if I tell you to." Each participant
does the work but does **not** commit yet, and votes **YES** or **NO**.

**Phase 2 — Commit (or abort).** If *all* voted YES, the coordinator tells everyone "COMMIT." If
*any* voted NO (or timed out), it tells everyone "ABORT." Participants finish accordingly and
release locks.

```
   PHASE 1: PREPARE                         PHASE 2: COMMIT

   Coordinator                              Coordinator
      │  prepare?                              │  all said YES → commit!
      ├──────────► Participant A ──YES──►      ├──────────► A ──ack──►
      ├──────────► Participant B ──YES──►      ├──────────► B ──ack──►
      └──────────► Participant C ──YES──►      └──────────► C ──ack──►
                                               (if any said NO → ABORT all)
```

When everyone votes YES, the outcome is guaranteed — a YES vote is a binding promise.

### The fatal flaw: blocking on coordinator failure

Here's the killer scenario. All participants vote YES and are now sitting in the **prepared**
state — rows locked, waiting for the verdict. Then the **coordinator crashes** before sending the
commit/abort.

```
   A, B, C: all PREPARED (rows LOCKED), waiting...
   Coordinator: 💥 crashed before deciding

   → A, B, C cannot commit (didn't hear "commit")
   → A, B, C cannot abort  (they promised YES; aborting could violate atomicity)
   → They BLOCK, holding locks, possibly for a very long time.
```

The participants are **stuck**. They can't safely commit or abort on their own, so they hold their
locks and wait, freezing those rows for everyone. 2PC is therefore a **blocking protocol** with the
coordinator as a **single point of failure**. This is why 2PC is avoided across loosely-coupled
services and is mostly seen *within* a single distributed database (e.g. across shards), where the
coordinator is reliable and recoverable.

### 3PC, briefly

**Three-Phase Commit** adds an extra "pre-commit" phase and timeouts so participants can make
progress on their own if the coordinator dies, removing the indefinite block. In exchange it's more
complex, chattier (an extra round trip), and *still* fails under network partitions. It's rarely
used in practice — interesting to mention, not something you'll reach for.

---

## How it works: the Saga pattern

A **Saga** abandons global atomicity. Instead it's a **sequence of local transactions**, where
each step commits on its own service immediately. If step *N* fails, you run a **compensating
transaction** for each completed step in reverse — a business-level "undo."

> **Compensating ≠ rollback.** A database rollback erases as if it never happened. A compensating
> transaction is a *new* transaction that semantically reverses the effect (refund the charge,
> cancel the reservation, restock the item). The original action *did* happen and may be visible to
> others in between — Sagas accept that intermediate, eventually-consistent reality.

Order example: *Reserve inventory → Charge payment → Ship.* If shipping fails:

```
   Forward:   [Reserve stock]──► [Charge card]──► [Create shipment] ✗ fails!
                    │                  │
   Compensate: [Restock] ◄──────── [Refund] ◄────── (undo completed steps, reverse order)
```

There are two ways to coordinate the steps:

**Orchestration** — a central **orchestrator** explicitly tells each service what to do next and
triggers compensations on failure.

```
        ┌──────────────┐
        │ Orchestrator │ ──1──► Inventory
        │  (the brain) │ ──2──► Payment
        │              │ ──3──► Shipping
        └──────────────┘  ◄─── on failure, fire compensations
```

- ✅ Logic is centralized and easy to follow/debug; clear place to see the whole flow.
- ❌ The orchestrator is a component you must build and keep available; risk of a "god service."

**Choreography** — no central brain. Each service **emits events**, and others **react**.
"OrderCreated" → Inventory reserves and emits "StockReserved" → Payment charges and emits
"PaymentDone" → Shipping ships.

```
   Inventory ──StockReserved──► Payment ──PaymentDone──► Shipping
        ▲                          │ (on failure)
        └──── PaymentFailed ───────┘  triggers compensation upstream
```

- ✅ Fully decoupled; no central dependency; services evolve independently.
- ❌ The flow is *implicit* — spread across services and events. Hard to see "what happens overall,"
  easy to create accidental loops, harder to debug.

| | Orchestration | Choreography |
|---|---------------|--------------|
| Control | Central orchestrator | Distributed via events |
| Visibility | Whole flow in one place | Emergent, scattered |
| Coupling | Services coupled to orchestrator | Services loosely coupled |
| Best for | Complex flows, many steps | Simple flows, few services |

Rule of thumb: **few simple steps → choreography; many steps with complex branching → orchestration.**

---

## The Transactional Outbox pattern

Sagas (and event-driven systems generally) hit a subtle, vicious bug. A service must do two things:
**update its own database** and **publish an event/message** (so the next step runs). These are two
different systems, so they can't share one transaction:

```
   1. UPDATE orders SET status='paid'   ✅ committed to DB
   2. publish "PaymentDone" to queue     💥 crash here!
   → DB says paid, but no one was ever told. The saga stalls forever.
```

Or the reverse — publish succeeds, DB commit fails, and now you've announced something that didn't
happen. This is the **dual-write problem**.

The **Transactional Outbox** solves it elegantly. Instead of publishing directly, you write the
event into an **`outbox` table in the same database, in the same local transaction** as your data
change. Now the data update and the "intent to publish" commit **atomically** — one local
transaction, no distributed coordination. A separate **relay** process then reads the outbox and
publishes to the queue, marking rows as sent.

```
   ┌─ ONE local transaction ──────────────┐
   │  UPDATE orders SET status='paid'      │   ← both commit together, atomically
   │  INSERT INTO outbox (event=...)       │
   └───────────────────────────────────────┘
                    │
                    ▼  separate relay polls / tails the log
            outbox table ──► publish to queue ──► mark sent ──► downstream consumers
```

Because the DB write and the outbox write are atomic, the event is recorded **if and only if** the
data changed. The relay guarantees the event is *eventually* delivered (retrying on failure). The
relay may deliver the **same event more than once** (at-least-once delivery) — which leads us
directly to idempotency.

---

## Idempotency's role

Every reliable distributed flow ends up delivering some messages **more than once** (retries,
relay re-sends, network ambiguity). The defense is **idempotency**: processing the same message
twice has the **same effect as processing it once**.

- Charge a card? Attach an **idempotency key** so the second attempt with the same key returns the
  first result instead of double-charging.
- Apply an event? Track processed event IDs and **skip duplicates**.

Idempotency is what lets you retry fearlessly, and retries are what make eventual consistency
*actually* converge. It's so central it gets its own lesson:
[Module 2.14 — Idempotency & exactly-once](../02-building-blocks/14-idempotency.md).

---

## Eventual consistency

Sagas and the outbox don't give you a single instant where everything is consistent. For a brief
window, inventory might be reserved while payment is still pending, or an event might be in flight.
The system is **eventually consistent**: given no new changes, all parts *converge* to a consistent
state. The trade-off you're explicitly accepting:

- ✅ **Availability & decoupling** — services commit locally and don't block on each other; no
  fragile global lock.
- ❌ **Temporary inconsistency** — clients may observe intermediate states; you must design the UX
  and business rules to tolerate "pending" states and to converge correctly.

This connects to **CAP/PACELC** ([Module 2.9](../02-building-blocks/09-cap-pacelc-consistency.md)):
across services, you almost always favor **availability + eventual consistency** over the strict,
blocking consistency of 2PC.

---

## Comparison

| Approach | Atomicity | Blocking? | Coupling | Use when |
|----------|-----------|-----------|----------|----------|
| **2PC** | Strong (true atomic) | **Yes** (coordinator SPOF) | Tight | Within one DB / across shards, trusted boundary |
| **3PC** | Strong | Less (timeouts) | Tight | Rarely; academic improvement on 2PC |
| **Saga (orchestration)** | Eventual, via compensation | No | Medium | Complex multi-step business flows across services |
| **Saga (choreography)** | Eventual, via compensation | No | Loose | Simple flows, few services |
| **Outbox** | (Mechanism, not a flow) | No | Loose | Reliably publishing events alongside a DB write |

---

## Trade-offs & key takeaways

- **Avoid distributed transactions if you can.** The cheapest distributed transaction is the one
  you designed away — by keeping related data in one service/database so a *local* transaction
  suffices.
- **2PC is strong but blocking.** Reserve it for within-database scenarios (cross-shard commits)
  where the coordinator is reliable, *not* across loosely-coupled microservices.
- **Sagas trade atomicity for availability.** You accept temporary inconsistency and design
  **compensating transactions** to undo. Orchestration for complex flows, choreography for simple.
- **The outbox kills the dual-write bug** by making "change data" and "record the event" one local
  transaction. Pair it with **at-least-once** delivery and **idempotent** consumers.
- **Idempotency is non-negotiable.** Anything that retries must be safe to repeat.

---

## In the wild

- **Microservice e-commerce / travel booking** (order → payment → inventory → shipping) are the
  canonical Saga use case.
- **The Outbox pattern** is widely implemented with change-data-capture tools like **Debezium**
  reading the database log and publishing to **Kafka**.
- **Distributed databases** (Spanner, CockroachDB, MySQL Cluster) use **2PC internally** across
  their shards — where the coordinator is part of the trusted, recoverable system.

---

## Interview angle

When a design spans services or shards and someone asks "how do you keep this consistent?", the
worst answer is "wrap it in a transaction" (you can't, across services). The strong answer:
"True 2PC is **blocking** and makes the coordinator a SPOF, so across services I'd use a **Saga**
with **compensating transactions**, accept **eventual consistency**, publish events reliably with
the **Transactional Outbox** to avoid the dual-write problem, and make every consumer **idempotent**
so at-least-once delivery is safe." That single sentence hits every senior signal.

**Common follow-ups:**
- "Why not just use 2PC everywhere?" → it blocks holding locks if the coordinator dies; SPOF; bad
  fit for loosely-coupled services.
- "A step in your Saga fails after others committed — now what?" → run compensating transactions in
  reverse to semantically undo.
- "You update the DB then publish an event — what can go wrong?" → dual-write problem; crash
  between the two; fix with the Outbox.
- "Your relay published the same event twice." → fine, because consumers are idempotent (dedupe by
  event ID / idempotency key).
- "Orchestration or choreography?" → choreography for simple flows; orchestration when the flow is
  complex and you need central visibility.

---

## Self-check

1. Why can't you wrap two updates in two different databases in a single `COMMIT`?
2. Walk through the 2PC failure where the coordinator crashes after everyone votes YES. Why are the
   participants stuck?
3. How does a compensating transaction differ from a database rollback?
4. What exactly is the dual-write problem, and how does the Transactional Outbox eliminate it?
5. Why is idempotency a *prerequisite* for the outbox + at-least-once delivery approach to be
   correct?

---

**Next:** [3.4 — Coordination & Leader Election »](04-coordination-leader-election.md)
