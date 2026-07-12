# 2.10 — Message Queues & Event Streaming

> **Module 2 · Building Blocks** · ~30 min read
> *Two services should not have to stare at each other waiting. Put a buffer between them and
> magic happens: they scale independently, survive each other's outages, and absorb traffic
> spikes. This is the story of queues — and of their bigger cousin, the log-based stream.*

---

## The problem

Back in [Module 0.1](../00-foundations/01-scale-zero-to-millions.md), Step 6, we hit a wall: a
user uploads a video, and transcoding it takes minutes. If the web request waits for that work,
the user stares at a spinner and the request times out. We can't make transcoding instant — so we
need to **stop making the user wait for it**.

More generally, when service A calls service B directly and waits for a reply (**synchronous**),
they're shackled together:

- If B is **slow**, A is slow.
- If B is **down**, A's request **fails**.
- If traffic **spikes**, B gets hammered all at once and falls over.
- A and B must scale in **lockstep**.

We want to **decouple** them: A hands off work and moves on; B does the work whenever it can,
at its own pace.

> **Analogy.** A busy restaurant kitchen. Waiters (producers) don't hand each plate-order
> directly to a specific cook and stand there waiting. They clip the order to a **rail** (the
> queue). Cooks (consumers) grab the next ticket whenever they're free. If the kitchen gets
> slammed, tickets pile up on the rail — the waiters keep taking orders, nothing is lost, and
> the kitchen works through the backlog. The rail **decouples** the dining room's pace from the
> kitchen's pace. That rail is a message queue.

---

## Core idea: producer, broker, consumer

A **message queue** sits between services as a buffer. Three roles:

- **Producer** — creates a **message** (a small piece of data: "transcode video 42") and sends
  it. Then it's free; it does **not** wait for the work to finish.
- **Broker** — the queue server itself. It **stores** messages durably until a consumer takes
  them. (RabbitMQ, Amazon SQS, etc.)
- **Consumer (worker)** — pulls messages off the broker and **does the work**.

```
   [ Producer ] ──produce──► [ Broker / Queue ] ──consume──► [ Consumer ]
   (web server)              (stores messages)               (worker)
                              [ m1 | m2 | m3 | ... ]
```

What this buys you (the same wins from Module 0.1, now named):

- **Asynchronicity** — the producer responds to the user instantly ("got it, processing!").
- **Decoupling** — producer and consumer don't know about each other; either can be redeployed,
  scaled, or restarted independently.
- **Buffering / load-leveling** — a traffic spike fills the queue instead of crushing the
  workers. The queue absorbs the burst.
- **Resilience** — if all workers are down, messages **wait safely** in the broker until workers
  return. Nothing is lost.

---

## Two delivery shapes: point-to-point vs pub/sub

### Point-to-point (work queue)

One message is delivered to **exactly one** consumer. Multiple workers can pull from the same
queue, but each message is handled **once** — perfect for distributing tasks.

```
                              ┌──► [ Worker 1 ]   (gets m1)
   [ Producer ] ──► [ Queue ] ┼──► [ Worker 2 ]   (gets m2)
                              └──► [ Worker 3 ]   (gets m3)
        each message goes to ONE worker; add workers to go faster
```

Use for: background jobs, task processing, the video-transcode example.

### Publish/subscribe (pub/sub)

One message is delivered to **every** interested subscriber. The producer publishes to a
**topic**; each subscriber gets its **own copy**.

```
                                ┌──► [ Email service ]   (gets a copy)
   [ Publisher ] ──► [ Topic ] ─┼──► [ Analytics ]       (gets a copy)
        "order placed"          └──► [ Warehouse ]       (gets a copy)
        one event → fan-out to ALL subscribers
```

Use for: broadcasting events ("user signed up," "order placed") to many independent consumers —
the backbone of **event-driven architectures**.

---

## Delivery semantics: the hard part

Networks fail, consumers crash mid-work, acknowledgements get lost. So how many times does a
message actually get delivered? Three possible guarantees:

| Semantic | Guarantee | What can go wrong | Cost |
|----------|-----------|-------------------|------|
| **At-most-once** | 0 or 1 delivery | Messages can be **lost** | Cheapest, simplest |
| **At-least-once** | 1 or more deliveries | Messages can be **duplicated** | Common default |
| **Exactly-once** | Exactly 1 effect | (hard / expensive to achieve) | Most complex |

- **At-most-once:** fire and forget; never retry. Fast, but a crash means a lost message. OK for
  disposable data (e.g. some metrics).
- **At-least-once:** retry until acknowledged. You'll **never lose** a message, but a lost *ack*
  causes a **redelivery** → the consumer may see the same message twice. This is the pragmatic
  default for most systems.
- **Exactly-once:** the holy grail. Truly exactly-once *delivery* over an unreliable network is
  effectively impossible; what real systems provide is **at-least-once delivery + idempotent
  processing**, which yields exactly-once *effects*.

> **The practical truth:** aim for **at-least-once + idempotency.** Make your consumer safe to
> run on the same message twice — e.g. dedupe on a message ID, or use upserts. Then duplicates
> become harmless. We dedicate [Module 2.14](14-idempotency.md) to this.

---

## Acks, retries & dead-letter queues

How does a queue know a message was handled? **Acknowledgements (acks).**

```
1. Consumer receives m1 → message becomes "invisible" to others (in-flight)
2. Consumer does the work
3. Consumer sends ACK   → broker deletes m1 (done!)
   ── OR ──
3'. Consumer crashes / no ACK before timeout
                        → broker makes m1 visible again → REDELIVERED (retry)
```

This is why at-least-once happens: the broker re-delivers anything it didn't get an ack for. But
what about a **"poison" message** — one that crashes the consumer *every* time? It would retry
forever, blocking the queue. The fix:

**Dead-letter queue (DLQ).** After N failed attempts, the broker moves the message to a separate
queue for inspection instead of retrying endlessly.

```
   [ Queue ] ──► [ Worker ]  fails... retry... fails... (× N)
       │                              │
       │                              ▼ exceeded max retries
       └──────────────────────► [ Dead-Letter Queue ]  (park it; alert a human)
```

A DLQ keeps one bad message from clogging the pipeline and gives you a place to debug failures.

---

## Ordering & backpressure

**Ordering.** A single queue with one consumer processes in order. But add multiple consumers
for throughput and ordering breaks — m1, m2, m3 may finish in any order. If order matters (e.g.
"create account" must precede "update account"), you need **partitioned/keyed ordering**: route
all messages for the same key (same user) to the same consumer, preserving order *per key* while
still parallelizing *across* keys. (Streaming systems, below, make this a first-class feature.)

**Backpressure.** What if producers are faster than consumers, forever? The queue grows without
bound until it runs out of memory/disk. **Backpressure** is the system pushing back:

- **Bounded queues** — reject or block producers when full ("slow down").
- **Drop / sample** — discard low-value messages under load.
- **Autoscale consumers** — add workers when the backlog grows (the natural fix).

A growing queue depth is a **signal**: consumers can't keep up. Monitor it.

---

## Queues vs log-based streaming

So far we've described **traditional queues** (RabbitMQ, SQS): a message is consumed, acked, and
**deleted**. Great for tasks. But a different model — the **log-based stream** (Kafka, AWS
Kinesis, Pulsar) — changed how we think about events.

### The log: don't delete, just append

A streaming platform stores messages as an **append-only log** that is **not deleted on read**.
Consumers track *where they are* with an **offset** (a position/bookmark in the log).

```
   Append-only log (a partition):

   offset:  0    1    2    3    4    5    6    7   ──► (new messages appended here)
            [m0][m1][m2][m3][m4][m5][m6][m7]
                            ▲                ▲
              Consumer A at offset 3    Consumer B at offset 7
              (reading older data)      (caught up)
```

Because nothing is deleted on read, two superpowers emerge:

- **Replay.** A consumer can **rewind its offset** and reprocess old events — re-run analytics,
  recover from a bug, bootstrap a brand-new service from history.
- **Multiple independent consumers.** Each consumer (group) has its **own offset**, so many
  teams read the same log at their own pace without affecting each other.

### Partitions & consumer groups

A topic is split into **partitions** for parallelism. Order is guaranteed **within a partition**
(by key), not across the whole topic. A **consumer group** shares the work: each partition is
read by exactly one consumer in the group, so you scale by adding partitions + consumers.

```
   Topic "orders"
   ├── Partition 0: [.....]  ──► Consumer 1 ┐
   ├── Partition 1: [.....]  ──► Consumer 2 ├─ Consumer Group "billing"
   └── Partition 2: [.....]  ──► Consumer 3 ┘
        key (user_id) decides partition → same user always in order
```

### Retention

Instead of "delete on ack," logs keep data by a **retention policy** — e.g. 7 days, or until a
size cap, or forever (compacted). The log becomes a durable, replayable source of truth, not
just a transient pipe.

### Side-by-side

| | **Queue** (RabbitMQ, SQS) | **Log stream** (Kafka, Kinesis) |
|---|---|---|
| Storage model | Delete on consume | Append-only, retained |
| Consumer tracking | Broker tracks acks | Consumer tracks **offset** |
| Replay old messages | No (it's gone) | **Yes** (rewind offset) |
| Multiple consumers of same msg | Pub/sub copies it | Each group has own offset |
| Ordering | Per queue (hard at scale) | **Per partition** (built-in) |
| Throughput | High | **Very high** (sequential disk I/O) |
| Best for | Task/job distribution, RPC-ish work | Event streams, analytics, audit, replay |

---

## When to use which

- **Reach for a queue (RabbitMQ/SQS)** when you have **discrete tasks** to distribute to
  workers: send this email, resize this image, process this payment job. You consume, you're
  done, you forget it. Simpler operationally.
- **Reach for a log stream (Kafka)** when you have a **continuous stream of events** that many
  consumers care about, when you need **replay** (re-run a pipeline, onboard a new service from
  history), high **throughput**, or **ordered** per-key processing. The cost is more operational
  complexity (partitions, offsets, brokers to run).

> The line blurs — SQS can do a lot, Kafka can act like a queue — but the mental model holds:
> **queue = transient work distribution; log = durable, replayable event stream.**

---

## Trade-offs & key takeaways

- **Queues decouple producers from consumers** in time, scale, and failure. That's the core win.
- **You don't get exactly-once for free.** Aim for **at-least-once + idempotent consumers**; that
  yields exactly-once *effects* in practice.
- **Always have a DLQ.** Poison messages will happen; don't let one block the pipeline.
- **Ordering and throughput fight each other.** Parallel consumers break global order; recover it
  with **per-key partitioning**.
- **Watch queue depth.** A growing backlog is your early warning that consumers can't keep up —
  the cue to add workers (backpressure).
- **Logs add replay and multi-consumer independence** at the cost of operational complexity.
  Choose the simplest tool that meets the need.

---

## In the wild

- **RabbitMQ** — feature-rich traditional broker (routing, priorities, point-to-point & pub/sub).
- **Amazon SQS / SNS** — managed queue (SQS) and pub/sub (SNS); SQS offers a built-in DLQ.
- **Apache Kafka** — the dominant log-based streaming platform; partitions, offsets, consumer
  groups, configurable retention. LinkedIn built it; nearly everyone uses it now.
- **AWS Kinesis / Google Pub/Sub / Apache Pulsar** — managed/alternative streaming platforms.
- **Event sourcing & CDC** — systems that store *every change as an event in a log* lean entirely
  on the streaming model's replay and retention.

---

## Interview angle

Whenever a design has **slow work, spiky traffic, or fan-out to many consumers**, propose a
queue and *say why*: decoupling, buffering, async response. The senior move is naming the
**delivery semantics** ("at-least-once, so I'll make the consumer idempotent") and the
**DLQ** for poison messages. If the question involves **replay, analytics, or many independent
consumers**, pivot to **Kafka** and explain **partitions for ordered parallelism** and
**offsets for independent, replayable consumption.** Stating *queue vs log* and *why* is exactly
the trade-off reasoning interviewers reward.

**Common follow-ups:**
- *"A message got processed twice — why, and how do you cope?"* → Lost ack → redelivery
  (at-least-once); make the consumer idempotent (dedupe on message ID / upsert).
- *"How do you guarantee ordering?"* → Per-key partitioning so a key's messages go to one
  consumer in order; accept that global order across partitions isn't guaranteed.
- *"Producers outpace consumers forever — what happens?"* → Backlog grows; apply backpressure
  (bounded queue, autoscale consumers, shed load).
- *"Queue or Kafka here?"* → Discrete tasks → queue; replayable event stream / many consumers /
  high throughput → Kafka.
- *"What's a DLQ for?"* → Park messages that fail repeatedly so one poison message can't block
  the pipeline; alert and debug.

---

## Self-check

1. Name three concrete benefits a queue gives you over service A calling service B directly.
2. Explain at-least-once vs exactly-once. Why do most systems settle for "at-least-once +
   idempotency," and what does that achieve?
3. What is a dead-letter queue, and what failure does it prevent?
4. In a log stream, what is an *offset*, and what two capabilities does keeping data (instead of
   deleting on read) unlock?
5. You need ordered processing per user but high overall throughput. How do partitions get you
   both?

---

## Practice

The Go assignment for this topic is the full **Distributed Message Queue** case study, where
you'll build a log-based broker with partitions, offsets, and consumer groups — and implement
at-least-once delivery with acks and retries. Head to
**[4.13 — Distributed Message Queue](../04-case-studies/13-message-queue/)** and bring this
lesson with you; the design doc there picks up exactly where this leaves off.

**Next:** [2.11 — Rate Limiting »](11-rate-limiting.md)
