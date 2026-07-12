# 4.13 — Design a Distributed Message Queue (Kafka-like)

> **Module 4 · Case Studies** · ~40 min read + coding assignment
> *Concepts exercised:* the append-only partitioned log, offsets, producers & consumers,
> consumer groups, delivery semantics (at-least-once / at-most-once / exactly-once),
> idempotency, retention & replay, replication, the "ordering vs parallelism" trade-off.

---

## The problem

A **message queue** lets one service hand work to another *without waiting for it*. A producer
drops a message in; a consumer picks it up later. This **decouples** the two: the producer
doesn't care who reads it, when, or whether the reader is even online right now.

Why you reach for one:
- **Decoupling** — the web server that takes an order doesn't have to *also* send the email,
  update analytics, and ping the warehouse synchronously. It writes one message and returns.
- **Buffering / smoothing** — a traffic spike fills the queue instead of crushing a slow
  downstream. The queue absorbs bursts; consumers drain at their own pace.
- **Fan-out** — many independent consumers can each read the same stream (billing, search
  indexing, audit) without the producer knowing they exist.
- **Durability & replay** — if a consumer crashes, the messages are still there; it resumes
  where it left off. New consumers can re-read history from the start.

We'll design a **Kafka-style** queue specifically — a **durable, partitioned, append-only log** —
because it's the model that scales and the one interviews ask about.

> **Analogy.** Think of a **shared numbered notebook**. Producers only ever **append** new lines
> to the bottom (you can't insert in the middle or erase). Each reader keeps a **bookmark** —
> "I've read up to line 1,204." Readers move at their own speed; a slow reader doesn't block a
> fast one; a new reader can start from line 1 and read the whole history. That notebook is **the
> log**, the line number is the **offset**, and the bookmark is the **committed offset**. Hold
> that picture — the entire design is "make that notebook fast, durable, and parallel."

---

## Step 1: Requirements (always start here)

**Functional**
- **Produce** a message to a named **topic**.
- **Consume** messages from a topic, in order, resuming from where you stopped.
- **Consumer groups:** a group of consumers cooperatively splits the work; each message in the
  topic is processed by *one* member of the group.
- **Replay:** a new consumer (or group) can re-read from the beginning.
- **Retention:** keep messages for a configurable window (time or size), not forever.

**Non-functional**
- **High throughput** — millions of messages/sec; this is the headline feature of a log.
- **Durability** — an acknowledged message must survive a broker crash (→ replication).
- **Horizontal scalability** — add brokers to add capacity.
- **Ordering** — at least *within a partition* (global ordering is expensive; see the trade-off).
- **Availability** — the cluster tolerates node failures.

> **The defining trade-off, stated up front:** **strict global ordering vs parallelism.** If
> every message must be processed in one total order, you can only have *one* consumer — no
> parallelism. Kafka's answer is to **partition**: order is guaranteed *within* a partition, and
> partitions run in parallel. You trade global order for scale. Almost everything below follows
> from this one decision.

---

## Step 2: Estimation (back-of-envelope)

- **Throughput:** say 1M messages/sec, each ~1 KB → **~1 GB/sec** ingest. No single disk or NIC
  handles that, so you **must** spread load across partitions and brokers. This is *why*
  partitioning exists, not just a nicety.
- **Partitions:** if one partition sustains ~10 MB/sec of sequential writes, 1 GB/sec needs
  ~100 partitions. Partition count is your unit of parallelism — pick it generously up front
  (raising it later reshuffles key→partition mapping).
- **Retention:** keep 7 days. 1 GB/sec × 86,400 s × 7 ≈ **~600 TB**, before replication. With
  3× replication that's ~1.8 PB. Retention and replication factor dominate your storage bill —
  they're explicit knobs, not free.
- **Sequential disk is the trick:** appending to the end of a file is nearly as fast as memory
  and pegs the disk's sequential bandwidth. The log is fast *because* it only ever appends.

---

## Step 3: High-level design

### The append-only partitioned log

A **topic** is split into **partitions**. Each partition is an **append-only log**: messages get
appended at the end and assigned a monotonically increasing **offset** (0, 1, 2, …). Offsets are
*per partition*, not global.

```
topic "orders", 3 partitions

 P0:  ┌────┬────┬────┬────┐         offsets →  0    1    2    3
      │ m  │ m  │ m  │ m  │  append at the tail ──────────────►
      └────┴────┴────┴────┘
 P1:  ┌────┬────┐                              0    1
      │ m  │ m  │
      └────┴────┘
 P2:  ┌────┬────┬────┐                         0    1    2
      │ m  │ m  │ m  │
      └────┴────┴────┘
        ▲                  ▲
        │ committed=1      │ a consumer reads from its offset forward
   consumer group "billing" bookmark for P2
```

**Which partition does a message go to?** If the message has a **key**, the broker hashes it:
`partition = hash(key) % numPartitions`. This guarantees **all messages with the same key land
in the same partition** — and therefore stay **in order** relative to each other. (Same
customer id → same partition → that customer's events are ordered.) No key? Round-robin for
balance.

### Producers, consumers, and consumer groups

- **Producer:** appends to a topic; the broker picks the partition and returns `(partition, offset)`.
- **Consumer:** reads a partition forward from an offset.
- **Consumer group:** the killer feature. Multiple consumers share a group name; the broker
  **divides the partitions among the members** so each partition is owned by exactly one member.

```
topic with 4 partitions, group "workers" with 2 members:

   P0 ─┐
   P1 ─┴──►  consumer A      each partition → exactly one consumer
   P2 ─┐                     more members = more parallelism (up to #partitions)
   P3 ─┴──►  consumer B
```

This is how you scale consumption: add members to a group, up to the partition count. Beyond
that, extra members sit idle — **partition count caps your consumer parallelism**.

### API

```
CreateTopic(name, partitions)
Produce(topic, key, value)         → (partition, offset)
group := Join(group, topic)        → a consumer with an assigned subset of partitions
records := consumer.Poll(max)      → next un-consumed records for its partitions
consumer.Commit()                  → persist progress (the bookmark) for the group
```

### Offsets and commits

The broker does **not** track "has this consumer seen this message?" per message — that wouldn't
scale. Instead each **group** stores one number per partition: the **committed offset** = "the
next offset this group still needs." Consuming advances a *private* cursor; **`Commit`** publishes
that cursor to the group. This tiny piece of bookkeeping is what makes replay, resume, and the
delivery semantics below all work.

---

## Step 4: Deep dive — delivery semantics

What happens when things crash *between* processing a message and recording that you did? This
is the most important part of the topic. Three guarantees:

| Semantic | What it means | How | Cost / risk |
|----------|---------------|-----|-------------|
| **At-most-once** | Each message delivered 0 or 1 times | **Commit the offset *before* processing** | Fast, simple; **may lose** messages if you crash after commit, before work |
| **At-least-once** | Each message delivered 1+ times | **Commit *after* processing** | Never lost; **may duplicate** if you crash after work, before commit ⭐ default |
| **Exactly-once** | Each message effects the system once | At-least-once **+ idempotency** (or transactions) | Correct but **expensive/complex** |

### At-least-once + idempotency (the practical default)

Almost everyone runs **at-least-once** because losing messages is usually worse than processing
one twice. You commit *after* the work succeeds:

```
loop:
  records = poll()
  process(records)     // do the work
  commit()             // only now record progress
```

If you crash after `process` but before `commit`, the next consumer re-reads those records — a
**duplicate**. You neutralize duplicates by making `process` **idempotent**: doing it twice has
the same effect as once. The standard trick is a **dedup key** — tag each message with a unique
id, and the consumer skips ids it has already applied (e.g. "payment 9f3a already settled").

> **"Exactly-once" is mostly a lie you engineer around.** True end-to-end exactly-once needs
> distributed transactions across the broker *and* your datastore (Kafka offers this within
> Kafka). In practice the cheaper, robust answer is **at-least-once delivery + idempotent
> consumers** = *effectively-once*. Say that in an interview; it's the senior framing.

### Retention & replay

Messages aren't deleted when read — they're kept for a **retention window** (e.g. 7 days or 500
GB), then the oldest are dropped. Because the log persists independent of any reader:

- A **crashed consumer** resumes from its committed offset — no data lost.
- A **brand-new group** starts at offset 0 and **replays the entire history** (great for
  backfilling a new search index or reprocessing after a bug fix).
- Two groups on the same topic read **independently** — each has its own bookmarks.

You implement exactly these behaviors (commit/resume, redelivery, fresh-group replay) in the
assignment.

---

## Step 4b: Deep dive — replication & durability

A single broker holding a partition is a single point of failure. So each partition is
**replicated** across N brokers (typically 3):

- One replica is the **leader**; it takes all reads and writes for that partition.
- The others are **followers** that copy the leader's log.
- A write is acknowledged once enough replicas have it (the in-sync set). If the leader dies, a
  follower is **promoted**.

> **Trade-off — `acks` / durability vs latency.** Wait for *all* replicas before acking →
> maximum durability, higher latency. Ack from the leader *only* → faster, but a leader crash
> before replication loses the message. Ack from a *quorum* is the usual middle ground. This is
> the same availability-vs-durability dial you've seen throughout the course.

---

## Step 4c: Deep dive — ordering, keys, and rebalancing

- **Ordering is per-partition only.** Need a customer's events ordered? Key by `customer_id` so
  they share a partition. You do **not** get a global order across partitions — and you rarely
  need one.
- **Hot keys / skew.** If one key is wildly more frequent (a celebrity user), its partition
  becomes a hotspot. Mitigation: a smarter key, or splitting that key's traffic (at the cost of
  its strict ordering).
- **Rebalancing.** When a consumer joins or leaves a group, partitions are **re-assigned** among
  the current members. Assignment must be **deterministic** so every broker/consumer agrees on
  who owns what. (The assignment uses a simple round-robin; real Kafka has pluggable strategies.)

---

## In the wild

- **Apache Kafka** is the canonical partitioned log: topics, partitions, offsets, consumer
  groups, replication, configurable retention — the model this lesson mirrors.
- **AWS Kinesis** is a managed log with the same shape (shards ≈ partitions).
- **Apache Pulsar** separates serving from storage (BookKeeper) but keeps the log abstraction.
- **RabbitMQ** is a different beast — a *broker* with smart routing and per-message acks (closer
  to the leaky-bucket/queue model), not a replayable log. Good for task queues, not for replay.

---

## Interview angle

Lead with **the partitioned append-only log** and **why partition** — it's the
**ordering-vs-parallelism** trade-off and it explains throughput, scaling, and ordering all at
once. Explain **offsets** and **committed offsets** (the bookmark), then **consumer groups** as
the unit of parallel consumption (capped by partition count). The depth signal is **delivery
semantics**: contrast at-most/at-least/exactly-once by *when you commit relative to processing*,
then land on **at-least-once + idempotent consumers = effectively-once**. Add **replication +
acks** for durability. Mention **hot-key skew** and **deterministic rebalancing** if pushed.

**Common follow-ups:**
- "How do you guarantee ordering?" → only within a partition; key by the entity you need ordered.
- "A consumer crashes mid-batch — duplicates or loss?" → depends on commit timing; at-least-once
  (commit after) risks duplicates → make the consumer idempotent with a dedup key.
- "How do you get exactly-once?" → at-least-once + idempotency, or transactional commits; explain
  it's *effectively*-once and why true exactly-once is expensive.
- "How do you scale consumers?" → more members in the group, up to #partitions; beyond that they
  idle, so size partitions for your future parallelism.
- "How is the message durable?" → replicated partitions with a leader and an `acks` policy.

---

## Practice → the Go assignment

Now build an in-memory Kafka-lite. Go to [`assignment/`](assignment/) and implement, in order:

1. **`CreateTopic` / `Produce`** — partition by `hash(key)`; append to that partition's log and
   return `(partition, offset)`. Same key → same partition → preserved order.
2. **Consumer groups** — `Join` assigns a deterministic subset of partitions; `Poll` returns the
   next un-consumed records for the consumer's partitions.
3. **`Commit` + resume** — persist the group's offsets; a new consumer resumes after them.
4. **At-least-once + replay** — uncommitted records are **redelivered**; a brand-new group reads
   from offset 0.

```bash
cd assignment
go test ./...          # red → implement → green
go test -race ./...    # must pass: Produce is called from many goroutines
```

The interface is given; you fill in the `// TODO`s. A reference solution is in
[`solution/`](solution/) — try first, peek after.

**Next case study:** [4.14 — Metrics Monitoring & Alerting »](../14-metrics-monitoring/)
