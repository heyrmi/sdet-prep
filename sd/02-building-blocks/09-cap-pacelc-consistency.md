# 2.9 — CAP, PACELC & Consistency Models

> **Module 2 · Building Blocks** · ~30 min read
> *Once your data lives on more than one machine, you face a law of physics you cannot cheat:
> when the network breaks, you must choose between staying consistent and staying available.
> CAP names that choice; PACELC completes it; consistency models give you the vocabulary to
> reason precisely instead of hand-waving "eventually."*

---

## The problem

The moment you replicate data — copies on multiple servers (recall replication in
[Module 2.6](06-replication.md)) — a hard question appears: **what happens when those copies
can't talk to each other?** Networks are not reliable. Cables get cut, switches reboot, a data
center loses its link. When that happens, your replicas drift apart. Do you keep serving
requests (and risk handing back stale or conflicting data), or do you refuse to answer (and
become unavailable) until they reconcile?

You cannot have it both ways during a network split. This isn't a tooling limitation you can
engineer around — it's a fundamental trade-off. CAP is the theorem that pins it down.

> **Analogy.** Two ticket clerks, one in each of two buildings, sell seats for the same concert
> from a shared seating chart. Normally they phone each other after every sale so the chart stays
> in sync. Then the phone line goes dead (a **partition**). Now each clerk must pick a policy:
> **(a) keep selling** — fast and available, but they might sell the same seat twice
> (inconsistent), or **(b) stop selling** until the line is back — consistent (no double-booking)
> but unavailable. There is no third option that is both correct and open during the outage.
> That dilemma *is* CAP.

---

## Core idea: the CAP theorem

CAP says a distributed data store can offer at most **two** of these three guarantees:

- **C — Consistency.** Every read sees the most recent write (or an error). All clients see the
  same data at the same time. *(Note: this is a stricter "C" than the C in database ACID —
  same word, different meaning. Here it means linearizability, defined below.)*
- **A — Availability.** Every request gets a non-error response — though not necessarily the
  latest data. The system stays up and answering.
- **P — Partition tolerance.** The system keeps working even when network messages between
  nodes are dropped or delayed (a **partition**).

### The part everyone gets wrong

"Pick two of three" makes it sound like you sit down and choose C and A, or C and P, or A and P,
like options on a menu. **That's the wrong reading.** Here's the reality:

> In any real distributed system, **partitions will happen** — networks fail. So **P is not
> optional**; you must tolerate partitions. That means the *real* choice is only ever between
> **C and A**, and only **during a partition.**

```
   Network healthy?
        │
   ┌────┴────┐
  yes        no  ← a PARTITION is happening
   │          │
 serve both   you MUST drop one:
 C and A       ┌────────────┐
 happily       │  keep C    │ → refuse/limit some requests  (CP)
               │   OR       │
               │  keep A    │ → answer with maybe-stale data (AP)
               └────────────┘
```

So "CP vs AP" really means: **when a partition strikes, do you sacrifice availability to stay
consistent (CP), or sacrifice consistency to stay available (AP)?** When the network is *fine*,
this choice doesn't even apply — you get both. CAP is a statement about behavior **during
failure**, not about everyday operation.

---

## CP vs AP, with examples

### CP — choose Consistency, give up Availability (during a partition)

If the system can't guarantee a read is current, it would rather **return an error or block**
than hand back stale data. Use this when wrong data is dangerous: bank balances, inventory,
locks, configuration.

```
   Client ──► Node (can't reach its peers) ──► "sorry, unavailable / try later"
                                                (refuses rather than risk staleness)
```

**Examples:** ZooKeeper, etcd, Consul (coordination — they'd rather be down than wrong),
HBase, Google Spanner (CP-leaning), traditional single-leader RDBMS in a strict setup.

### AP — choose Availability, give up Consistency (during a partition)

The system **always answers**, even if some replicas are out of date, and reconciles later. Use
this when being up matters more than every read being perfectly fresh: shopping carts, social
feeds, "likes," product catalogs, DNS.

```
   Client ──► Node (can't reach its peers) ──► answers anyway with its local copy
                                                (may be slightly stale; fixes up later)
```

**Examples:** Cassandra, DynamoDB (tunable, AP-leaning by default), Riak, Amazon's classic
Dynamo. Amazon famously chose availability for the shopping cart — better to occasionally show a
slightly stale cart than to ever block a customer from buying.

> **There's rarely a global "right" answer — even within one company.** A bank might run its
> ledger CP and its marketing recommendations AP. Choose per *use case*, by the cost of being
> stale vs the cost of being down.

---

## PACELC: the half of the story CAP omits

CAP only describes the **partition** case. But partitions are rare. What governs your system the
other 99.9% of the time, when the network is healthy? CAP is silent. **PACELC** fills the gap:

> **If** there is a **P**artition, trade off between **A** and **C** *(this is just CAP)* —
> **E**lse (normal operation), trade off between **L**atency and **C**onsistency.

```
   PACELC
   ┌──────────────── if Partition ────────────────┐   ┌──── Else (normal) ────┐
   │   choose Availability  ──vs──  Consistency    │   │  Latency ──vs── Consistency │
   └───────────────────────────────────────────────┘   └─────────────────────────────┘
```

The "else" half is the everyday trade-off you actually live with: **strong consistency costs
latency.** To guarantee every read is current, you must coordinate across replicas (wait for a
majority to agree) *on every operation* — and waiting is latency. If you relax consistency, you
can answer from the nearest replica immediately.

Classifying real systems with PACELC is more honest than CAP alone:

| System | Partition: A vs C | Else: L vs C | PACELC label |
|--------|-------------------|--------------|--------------|
| DynamoDB / Cassandra (default) | choose **A** | choose **L** | **PA / EL** (fast & available) |
| Spanner | choose **C** | choose **C** | **PC / EC** (consistent, pays latency) |
| MongoDB (default) | choose **C** | choose **L** | **PC / EL** |
| etcd / ZooKeeper | choose **C** | choose **C** | **PC / EC** |

The big insight: a system can be CP under partition yet *still* trade consistency for latency in
normal times (PC/EL), or pay for consistency always (PC/EC). CAP couldn't express that. PACELC
can.

---

## The consistency spectrum

"Consistent" and "eventual" aren't binary — there's a **spectrum** from strongest (and most
expensive) to weakest (and cheapest/fastest). Knowing the names lets you ask for *exactly* the
guarantee you need.

```
 STRONGER (more coordination, more latency)                 WEAKER (faster, cheaper)
 ◄─────────────────────────────────────────────────────────────────────────────►
 Linearizable → Sequential → Causal → Read-your-writes/Monotonic → Eventual
```

- **Linearizability (strong).** The gold standard. Every operation appears to happen
  **instantaneously** at a single point in time, and once a write completes, every subsequent
  read everywhere sees it. The system behaves *as if there's one copy of the data.* Most
  intuitive, most expensive.
- **Sequential consistency.** All clients see operations in the **same order**, and each
  client's own operations keep their order — but that global order need not match real-world
  ("wall clock") time. Slightly weaker than linearizable.
- **Causal consistency.** Operations that are **causally related** (B was written after reading
  A) are seen in that order by everyone. Unrelated operations can be seen in any order. A great
  practical middle ground — e.g. you never see a reply to a comment before the comment itself.
- **Read-your-writes consistency.** A client always sees **its own** writes in subsequent reads
  (even if others don't yet). Why "I posted but don't see my post" feels broken — that's a
  read-your-writes violation. (Recall the replication-lag example in
  [Module 0.1](../00-foundations/01-scale-zero-to-millions.md).)
- **Monotonic reads.** Once a client has seen a value, it will **never see an older** value on a
  later read. No "time travel" backwards. (Without it, refreshing can show new data, then old
  data, then new again.)
- **Eventual consistency (weakest).** If writes stop, **all replicas eventually converge** to
  the same value — but until then reads may be stale or even conflicting. Cheapest and fastest;
  the default for AP systems.

> Read-your-writes and monotonic reads are **session guarantees** — promises about *one user's*
> experience. They're cheap to add and dramatically improve perceived correctness without paying
> for full linearizability. A common, pragmatic sweet spot.

---

## Quorums: tuning consistency with a dial

How do AP-style systems *get* stronger guarantees when they want them? **Quorums.** With data
replicated across **N** nodes, you require:

- **W** = number of replicas that must **acknowledge a write** before it's "done."
- **R** = number of replicas you must **read from** and compare to answer a read.

The magic rule:

```
   if  W + R > N   →  the read set and write set always OVERLAP
                      →  every read is guaranteed to see the latest write
                      →  strong consistency
```

Why it works: if writes touch W nodes and reads touch R nodes and `W + R > N`, then by the
pigeonhole principle at least **one node** is in both sets — so a read always catches at least
one copy that has the newest write.

```
   N = 3 replicas.  Pick W = 2, R = 2.   W + R = 4 > 3  ✓ overlap guaranteed

   write goes to:   [✓][✓][ ]      read asks:   [?][?][ ]
                     A  B  C                      A  B
                          └──────── B is in both → read sees the latest write
```

This makes consistency a **tunable dial**, not a fixed property:

| W | R | Behavior |
|---|---|----------|
| W = N, R = 1 | Fast reads, slow writes; strong if all writes succeed | Read-heavy, can't lose writes |
| W = 1, R = N | Fast writes, slow reads | Write-heavy workloads |
| W = R = ⌈(N+1)/2⌉ (quorum) | Balanced; `W+R>N` holds → strong | The common "just works" default |
| W + R ≤ N | No overlap guarantee → **eventual** consistency | Maximize availability/latency |

This is **tunable consistency** — Cassandra and DynamoDB let you choose W and R **per request**,
so one query can be strongly consistent and another eventually consistent, in the same system.
That's the practical embodiment of "consistency is a trade-off you choose, not a fixed setting."

---

## Trade-offs & key takeaways

- **CAP's real lesson:** since partitions are inevitable, your true choice is **C vs A during a
  partition** — CP or AP. Stating that crisply is the whole game.
- **PACELC is the grown-up version:** even with no partition, **stronger consistency costs more
  latency.** This trade-off is with you every day, not just during outages.
- **Consistency is a spectrum, not a switch.** Name the exact level you need
  (read-your-writes? causal? linearizable?) rather than saying "consistent" or "eventual."
- **Quorums (`W + R > N`) turn the dial.** They let an AP-capable system deliver strong reads on
  demand — at the price of latency and reduced availability when nodes are down.
- **Choose per use case.** Ledgers and locks want CP/strong; carts and feeds want AP/eventual.
  The same company runs both.

---

## In the wild

- **Amazon Dynamo (the paper)** popularized AP + tunable quorums; **DynamoDB** and **Cassandra**
  expose W/R per request.
- **Google Spanner** delivers near-linearizable global consistency using synchronized clocks
  (TrueTime) — and openly accepts the latency cost (PC/EC).
- **ZooKeeper / etcd / Consul** are CP by design — coordination services would rather be
  unavailable than hand out stale leadership/lock info.
- **DNS** is the textbook eventually-consistent, highly available (AP) system: updates propagate
  slowly, but it's essentially always up.

---

## Interview angle

When a question involves replicated data — *any* "design X at scale" — interviewers want to hear
**CAP reasoning applied to a concrete choice.** Don't recite "pick two of three." Say:
*"Partitions are unavoidable, so the real question is C vs A during a partition. For [this
feature], stale data is [dangerous/harmless], so I'd go [CP/AP]."* Then level up with **PACELC**
("and even normally, strong consistency costs latency, so I'd...") and, if pushed, reach for
**quorums** (`W + R > N`) to show you can tune it. Naming a specific **consistency model**
("read-your-writes is enough here") is a strong senior signal.

**Common follow-ups:**
- *"Is the CAP 'C' the same as ACID 'C'?"* → No — CAP's C is linearizability; ACID's C is
  preserving invariants within a transaction. Same letter, different idea.
- *"Your replica is behind and a user can't see their own post. Fix it?"* → That's a
  read-your-writes violation; route the user's reads to the leader or to a replica known to have
  their write, or pin the session.
- *"Set W and R for N=3 to get strong consistency."* → `W=2, R=2` (any `W+R>N`).
- *"Can you have C and A at the same time?"* → Yes — **when there's no partition.** CAP only
  forces the choice during one.

---

## Self-check

1. Why is it misleading to say a system "chooses C and A and gives up P"? What's the choice
   *really* between, and *when*?
2. Give one feature you'd build CP and one you'd build AP, and justify each by the cost of
   staleness vs downtime.
3. What does PACELC add that CAP leaves out? Phrase the "else" trade-off in one sentence.
4. For N = 5 replicas, give a (W, R) pair that guarantees strong reads, and explain why it works.
5. A user sees their new comment on refresh, then it vanishes on the next refresh, then returns.
   Which consistency guarantee is being violated?

---

## Practice

There's no standalone Go assignment for this lesson — it's the conceptual backbone for several
that follow. You'll put quorums and consistency choices into *working code* in
**[4.2 — Consistent Hashing / Key-Value Store](../04-case-studies/02-key-value-store/)**, where
you implement tunable W/R replication, and again when you build the
**[Raft-based consensus](../03-distributed-systems/01-consensus-raft-paxos.md)** that delivers
strong consistency. For now: reread this with the replication lesson
([2.6](06-replication.md)) open side by side — CAP is what replication's trade-offs add up to.

**Next:** [2.10 — Message Queues & Event Streaming »](10-message-queues-streaming.md)
