# 3.1 — Replication & Consensus (Raft, Paxos)

> **Module 3 · Distributed Systems** · ~32 min read
> *The hardest problem in distributed systems sounds trivial: get a few machines to agree on a
> single value, even though some of them crash and the network drops messages. This is*
> consensus*, and once you can do it, you can build leader election, distributed locks, and
> rock-solid replicated databases on top of it.*

---

## The problem

You run three copies of a database so that losing one machine doesn't lose your data. A write
comes in: "set `balance = 100`." You want all three copies to end up with the same value. Easy,
right? Send the write to all three.

Now reality intrudes:

- One copy is **slow** and hasn't applied the write yet.
- The network **drops** the message to the second copy.
- The machine sending the writes **crashes** halfway through, having told copy 1 but not copies
  2 and 3.

Now your three copies disagree: one says `100`, two say the old value. Which one is *correct*?
If a client reads from each, it gets different answers. Worse, if two machines each think they're
in charge and both accept writes, you get **two conflicting histories** that can never be merged.
This is the nightmare scenario called **split-brain**.

> **Analogy.** A group chat planning where to eat, but messages arrive out of order, some never
> arrive, and people drop off Wi-Fi mid-sentence. Alice texts "let's do tacos," Bob (offline)
> texts "let's do sushi," and now half the group shows up at each place. You need a *protocol*
> so the group reaches **one** decision that everyone honors — even with flaky phones. That
> protocol is consensus.

**Consensus** is the problem of getting a set of unreliable machines to **agree on a single
value** (or a single ordered sequence of values) such that:

- **Agreement:** all non-faulty machines decide the *same* value.
- **Validity:** the value decided was actually proposed by someone (no inventing values).
- **Termination:** they eventually decide (they don't hang forever).

Sounds simple. It is famously, provably hard the moment failures enter the picture.

---

## Core idea

The single most useful framing is the **replicated state machine**.

> A **state machine** is anything that starts in some state and moves to a new state by applying
> commands in order. A bank account is a state machine: start at `$0`, apply `+100`, apply `-30`,
> you're at `$70`. The key insight: **if two machines start in the same state and apply the exact
> same commands in the exact same order, they end in the same state.**

So replication reduces to one thing: **make every replica agree on the same ordered log of
commands.** That ordered log is the only thing we have to agree on. Each replica then just
replays the log locally and arrives at identical state.

```
   Agree on this ordered log, and every replica is identical:

   index:    1        2        3        4
            ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐
   log:     │SET   │ │ADD   │ │DEL   │ │SET   │
            │x=5   │ │y+=1  │ │z     │ │x=9   │
            └──────┘ └──────┘ └──────┘ └──────┘
                 │
                 ▼  apply in order
   Replica A ───┐
   Replica B ───┼──► all reach identical state
   Replica C ───┘
```

That's it. **Consensus = agreeing on the log.** Raft and Paxos are two algorithms that solve it.

### The FLP result and "majority quorums"

A famous theorem (FLP, 1985) proves that in a fully asynchronous network, **no consensus
algorithm can guarantee both safety and liveness** if even one machine can fail. The practical
escape hatch: use **timeouts** to detect "probably dead" nodes and accept that progress *pauses*
during weird network conditions, while never producing a *wrong* answer. Real algorithms keep
**safety** always and give up **liveness** temporarily.

The other key tool is the **quorum** — a majority. With 5 nodes, a quorum is 3. The magic of
majorities: **any two majorities must overlap in at least one node.** That shared node "remembers"
the last decision, so a new decision can't contradict an old one. This overlap is why an odd
number of nodes (3, 5, 7) is standard — you want to tolerate `f` failures with `2f + 1` nodes.

| Total nodes | Quorum (majority) | Failures tolerated |
|-------------|-------------------|--------------------|
| 3 | 2 | 1 |
| 5 | 3 | 2 |
| 7 | 4 | 3 |

> Note 4 nodes also has quorum 3 but still only tolerates 1 failure — same fault tolerance as 3
> nodes but more machines to coordinate. That's why **even node counts are wasteful**; always go
> odd.

---

## How it works: Raft

Paxos came first (1989) and is correct but notoriously hard to understand. **Raft** (2014) was
designed explicitly *for understandability* and is now the default in industry. We'll do Raft in
depth, then sketch Paxos.

Raft splits the problem into three sub-problems: **leader election**, **log replication**, and
**safety**. At any moment, each node is in one of three roles:

- **Leader** — the single node that handles all client writes and tells followers what to log.
- **Follower** — passive; just accepts entries from the leader and votes in elections.
- **Candidate** — a follower that timed out and is trying to become leader.

```
        times out, starts election
   ┌──────────┐ ───────────────────► ┌────────────┐
   │ FOLLOWER │                       │ CANDIDATE  │
   └──────────┘ ◄─────────────────── └────────────┘
        ▲   discovers leader / higher term   │ wins majority of votes
        │                                     ▼
        │           steps down          ┌──────────┐
        └────────────────────────────── │  LEADER  │
                                         └──────────┘
```

### Terms: logical time

Raft divides time into **terms**, numbered 1, 2, 3, … Each term begins with an election. A term
has **at most one leader**. Every message carries the sender's term number, and the rule is
brutal and simple: **if you ever see a higher term than your own, you immediately step down to
follower and adopt that term.** Terms act as a logical clock that lets nodes detect stale leaders.

### 1) Leader election

Every follower runs an **election timeout** — a random duration (say 150–300 ms). If it goes that
long without hearing from a leader (via heartbeats), it assumes the leader is dead and starts an
election:

1. Increment its term, become **candidate**, vote for itself.
2. Send `RequestVote` RPCs to all other nodes.
3. Each node votes **yes** for the first candidate it sees in a given term (and only if the
   candidate's log is at least as up-to-date as its own — more on that under Safety).
4. If the candidate collects votes from a **majority**, it becomes **leader** and starts sending
   heartbeats. If it sees a higher term, it reverts to follower. If nobody wins (split vote), the
   timeout fires again and a new term begins.

```
   Term 4 election (5 nodes, leader L3 just died)

   N1 (timeout!) ──RequestVote(term=4)──► N2  ✓ vote
                 ──RequestVote(term=4)──► N4  ✓ vote
                 ──RequestVote(term=4)──► N5  (no answer, slow)
   N1 has 3 votes (itself + N2 + N4) = majority of 5  →  N1 is LEADER for term 4
```

> **Why randomized timeouts?** If every follower timed out at the *same* instant, they'd all
> become candidates at once, split the vote evenly, and nobody would win — repeatedly. By
> randomizing the timeout, usually *one* node times out first, grabs the votes, and wins before
> others wake up. This tiny trick is what makes Raft elections reliably terminate. It's the same
> idea as **jitter** in retry backoff.

### 2) Log replication

Once elected, the leader is the sole entry point for writes:

1. Client sends a command to the leader.
2. Leader **appends** it to its own log (uncommitted) and sends `AppendEntries` RPCs to followers.
3. Followers append it to their logs and reply success.
4. Once a **majority** have stored the entry, the leader marks it **committed**, applies it to its
   state machine, and replies to the client. It tells followers the new commit index on the next
   `AppendEntries`.

```
   Client: "SET x=9"
                                 ┌──────────── majority stored? ────────┐
   Leader  log: [.. | x=9 ]      │                                       ▼
      │  AppendEntries(x=9)      │                                  COMMIT x=9
      ├──────────────────────►  Follower A: [.. | x=9 ] ✓ ────────►  apply, reply OK to client
      ├──────────────────────►  Follower B: [.. | x=9 ] ✓ ────────►
      └──────────────────────►  Follower C: (slow, catches up later)
```

`AppendEntries` doubles as the **heartbeat**: even with no new commands, the leader pings
followers periodically to suppress their election timeouts. The RPC also carries the index/term
of the *preceding* entry, so a follower can reject if its log doesn't match — the leader then
walks backward until logs align and overwrites the follower's divergent tail. This **log matching**
guarantees all logs are identical up to the commit point.

### 3) Safety: the election restriction

Here's the subtle danger: what if a node with an *incomplete* log becomes leader and overwrites
committed entries? Raft prevents this with the **election restriction**: a candidate can only win
if its log is **at least as up-to-date** as a majority of voters' logs (judged by the term and
index of the last entry). Because a committed entry lives on a majority, and a winning candidate
needs a majority of votes, the two majorities overlap — so the new leader is *guaranteed* to
already have every committed entry. **Committed entries are never lost.** This is the heart of
Raft's correctness.

### Membership changes (briefly)

Adding or removing nodes is dangerous: if some nodes switch to the new membership before others,
you could briefly have *two* disjoint majorities → split-brain. Raft handles this with **joint
consensus**, a transition phase where decisions require a majority of *both* the old and new
configurations simultaneously, so no two conflicting majorities can ever form. The practical
takeaway: change membership one node at a time and let the protocol handle the overlap.

---

## How it works: Paxos (high level)

**Paxos** solves the same problem but is structured differently and is famously hard to follow.
The core (single-decree Paxos) agrees on *one* value through two phases with **proposers**,
**acceptors**, and **learners**:

1. **Prepare phase:** a proposer picks a unique, increasing proposal number `n` and asks a
   majority of acceptors to "promise" not to accept anything older than `n`. Acceptors reply with
   any value they've already accepted.
2. **Accept phase:** if the proposer hears back from a majority, it asks them to accept value `v`
   (using the highest-numbered value already seen, if any — this is what preserves agreement).
   Once a majority accepts, the value is chosen.

```
   Proposer ──Prepare(n)──►  majority of Acceptors ──Promise(n, prevValue)──►
   Proposer ──Accept(n,v)──► majority of Acceptors ──Accepted──►  value v chosen
```

To agree on a *log* (not a single value), you run this repeatedly — that's **Multi-Paxos**, which
elects a stable leader to skip the prepare phase on every entry, at which point it looks a lot
like Raft. The big difference is pedagogical: Raft prescribes a clear leader-based structure;
Paxos describes a more general mechanism that's easy to get subtly wrong in implementation.

| | Raft | Paxos (Multi-Paxos) |
|---|------|---------------------|
| Designed for | Understandability | Generality / correctness proof |
| Leadership | Strong, explicit leader | Leader is an optimization bolted on |
| Log handling | Built-in, contiguous log | Per-slot agreement, gaps possible |
| Reputation | "The one you can actually implement" | "Correct but a PhD to read" |
| Used by | etcd, Consul, CockroachDB, TiKV | Google Chubby, Spanner, Cassandra (lightweight txns) |

Both rely on the *same* underlying truth: **majority quorums that overlap.** Raft is the
recommended starting point; reach for Paxos lineage mainly when you're inheriting it.

---

## Trade-offs & key takeaways

- **Consensus buys you strong consistency at the cost of latency and availability.** Every commit
  needs a round trip to a majority. If you can't reach a majority (network partition isolates you
  with a minority), you **stop accepting writes** — by design. That's CP in CAP terms: it sacrifices
  availability to never be wrong.
- **It does not scale writes.** All writes funnel through one leader and one log. Consensus is for
  *coordination*, not for high-throughput data paths. You shard to scale, running one consensus
  group per shard.
- **Odd node counts only.** `2f + 1` nodes tolerate `f` failures. 3 or 5 is the sweet spot;
  more nodes = more durability but slower commits (bigger quorum to wait for).
- **Don't write your own.** Consensus is a minefield of edge cases. Use a battle-tested library or
  service (etcd, ZooKeeper, Consul). Implementing Raft yourself is a great *learning* exercise and
  a bad *production* decision.

---

## When you actually need consensus (and when you don't)

This is the most practical question. **Consensus is expensive — use it sparingly.**

**You need it for:**
- **Leader election** — picking the single primary in a replicated database or job scheduler.
- **Configuration / metadata** — the source of truth for "which shard lives where," cluster
  membership, feature flags that must be globally consistent.
- **Distributed locks / leases** — "only one worker may process this at a time."

**You usually do NOT need it for:**
- **Bulk application data** — replicate it with leader-follower replication and accept eventual
  consistency where you can (it's far cheaper). See [Module 2.6](../02-building-blocks/06-replication.md).
- **Caches, analytics, logs** — approximate and eventually-consistent is fine.
- **Anything you can make commutative or idempotent** — if order doesn't matter, you don't need to
  agree on order. See [Module 2.14](../02-building-blocks/14-idempotency.md).

The pattern in big systems: a *small* consensus group (3–5 nodes) holds the critical metadata and
elects leaders, while the *bulk* data is replicated and sharded with cheaper mechanisms that
*trust* that metadata. Consensus is the thin, reliable backbone — not the whole skeleton.

---

## In the wild

- **etcd** (Raft) is the brain of **Kubernetes** — it stores all cluster state and uses Raft so
  that state is consistent even as control-plane nodes fail.
- **Consul** and **CockroachDB** and **TiKV** use Raft for replication and leader election.
- **Google Chubby** (Paxos) provides locks and metadata for Google's infrastructure; **Spanner**
  uses Paxos groups per shard.
- **Apache ZooKeeper** uses **Zab**, a Paxos-like protocol, and underpins Kafka, HBase, and more
  (see [3.4](04-coordination-leader-election.md)).

---

## Interview angle

When a design needs a single source of truth — "who is the leader," "what's the current config,"
"grant exactly one lock" — say **"I'd use a consensus system like etcd/ZooKeeper (Raft/Paxos)."**
Then show depth: explain the **replicated state machine** framing, that consensus means **agreeing
on an ordered log**, and that it relies on **majority quorums** so any two quorums overlap. Mention
that it's **CP** (sacrifices availability under partition) and that it **doesn't scale writes**, so
you keep the consensus group small and use it only for coordination. Naming **randomized election
timeouts** and the **election restriction** signals you actually understand Raft, not just the
buzzword.

**Common follow-ups:**
- "Why an odd number of nodes?" → `2f+1` tolerates `f` failures; even counts add cost without
  added fault tolerance; quorums must overlap.
- "What happens during a network partition?" → the minority side can't reach a quorum and stops
  accepting writes; the majority side keeps going; no split-brain because only one side has a
  majority.
- "How does Raft prevent a stale node from becoming leader and losing data?" → the election
  restriction: a candidate needs an up-to-date log to win a majority, and committed entries live
  on a majority, so the two overlap.
- "Two leaders at once — how?" → only transiently; the old leader has a stale **term** and is
  rejected the moment it contacts a node that has seen the newer term.

---

## Self-check

1. Why is consensus reducible to "agree on an ordered log"? What property of state machines makes
   this work?
2. Why must any two majorities of a cluster share at least one node, and why does that matter for
   safety?
3. What problem do **randomized election timeouts** solve in Raft, and what would happen without
   them?
4. Give one thing you *should* use consensus for and one thing you *shouldn't*, with reasoning.
5. A 5-node Raft cluster splits 2-vs-3 by a partition. Which side keeps serving writes, and why
   is there no split-brain?

---

**Next:** [3.2 — Storage Engines: B-Tree vs LSM-Tree »](02-storage-engines.md)
