# 7.1 — Consistency Checking & Linearizability

> **Prerequisites:** [2.9 CAP / PACELC](../02-building-blocks/09-cap-pacelc-consistency.md),
> [3.1 Consensus](../03-distributed-systems/01-consensus-raft-paxos.md).
> **Assignment:** [`01-linearizability-assignment/`](01-linearizability-assignment/assignment/)

---

## The problem

You are testing a distributed key-value store. Three clients concurrently do this:

```
client A:  write(x, 1)
client B:  write(x, 2)
client C:  read(x)  ->  ?
```

What should C read? **1 and 2 are both correct**, and so is the value from before either write, if
C's read happened first. There is no expected value to assert.

This is the wall every conventional testing instinct hits. `assertEquals` needs a single right
answer, and concurrent systems do not have one. So you must invert the question:

> Instead of *"is this the right answer?"*, ask *"is there ANY valid ordering of these operations
> that explains what we observed?"* If no such ordering exists, the system is broken — and you
> have proved it, not guessed it.

That inversion is the foundation of Jepsen and of every serious distributed-systems test suite.

---

## Core idea: histories

You stop asserting on individual calls and start recording a **history** — every operation with
its invocation time, completion time, and result.

```
    time ──────────────────────────────────────────────►

A:      ├─ write(x,1) ─┤
B:               ├──── write(x,2) ────┤
C:                  ├─ read(x) -> 1 ─┤
```

Operations that overlap in time are **concurrent**: the system may order them however it likes.
Operations that do not overlap have a **real-time ordering** that must be respected — if A's write
completed before C's read began, the system may not pretend otherwise.

A history is **linearizable** if there exists some total order of the operations such that:

1. Each operation appears to take effect **instantaneously at some point between its invocation
   and its completion**, and
2. That order is consistent with a **sequential specification** of the object (for a register:
   every read returns the value of the most recent preceding write).

Linearizability is the strongest single-object consistency model. It is what "strong consistency"
means when someone says it precisely.

### Reading the diagram above

Is that history legal? Yes — order it `write(x,1)`, `read(x)->1`, `write(x,2)`. B's write overlaps
C's read, so B may be placed after it. Now change C's result:

```
A:      ├─ write(x,1) ─┤
B:               ├──── write(x,2) ────┤
C:                                        ├─ read(x) -> 1 ─┤
```

Here C begins *after* B completes. Any valid order must place both writes before the read, and
the last write wins — so returning 1 is impossible under any ordering. **That is a linearizability
violation**, and it is exactly the class of bug that a "run the tests again" approach never
catches, because when it does fail it looks like flakiness.

---

## Checking a history

The naive algorithm is to try every permutation. With *n* operations that is *n!* — unusable past
about ten operations.

The standard approach is the **Wing & Gong linearization algorithm**, a backtracking search:

```
linearizable(history, state):
    if history is empty: return true
    for each operation op that is "minimal" (no operation must precede it):
        if op is applied to state and its result matches what we observed:
            if linearizable(history - op, newState): return true
            # else backtrack and try a different minimal operation
    return false
```

Two things make it tractable in practice:

- **Real-time constraints prune hard.** Only operations that have already been invoked and are not
  forced to come later are candidates, which cuts the branching factor enormously.
- **Memoisation on (remaining operations, state).** The same subproblem recurs constantly.

It is still exponential in the worst case, which is why real checkers bound history length —
Jepsen tests generate many short histories rather than one enormous one. That is a deliberate
trade, and it is worth being able to state: *short histories, many of them, is the practical shape
of consistency checking.*

### Beyond registers: Elle

Registers are the easy case. For **transactional** databases, checking every ordering is hopeless.
Jepsen's **Elle** checker instead infers **dependencies** between transactions from what they read
and wrote, builds a dependency graph, and looks for **cycles**.

A cycle in the dependency graph means "T1 must come before T2, and T2 must come before T1" — which
is impossible, so the history is not serializable. The power of this approach is that it does not
just say *no*: the cycle **localises** the anomaly, telling you which transactions conflict and
how. That is the difference between "your database is wrong" and a bug report someone can act on.

Elle also names the anomalies it finds using the standard taxonomy — G0 (dirty write), G1a (aborted
read), G1b (intermediate read), G1c (cyclic information flow), G2 (anti-dependency cycle) — which
is the vocabulary to know if this comes up.

---

## The other consistency models

Linearizability is not always the right thing to check, because it is not always what the system
claims.

| Model | Guarantee | Check |
|---|---|---|
| **Linearizable** | Real-time order respected, single total order | Wing-Gong search |
| **Sequential** | Single total order; per-client order preserved; real time ignored | Similar search, weaker constraints |
| **Serializable** | Transactions appear in *some* serial order (no real-time requirement) | Cycle detection (Elle) |
| **Strict serializable** | Serializable **and** real-time ordered | Both |
| **Snapshot isolation** | Reads from a consistent snapshot; write-write conflicts prevented | Cycle detection, allowing G2-item |
| **Causal** | Causally related operations ordered; concurrent ones may differ | Track causal dependencies |
| **Eventual** | Replicas converge, eventually | Convergence check after quiescence |

**Test against what the system claims, not against the strongest model.** Reporting a
"linearizability violation" in a database that only advertises snapshot isolation is not a bug
report; it is a misunderstanding. Conversely, a database that advertises strict serializability
and fails a linearizability check has a genuine, serious defect — and this is precisely how
Jepsen has found real bugs in many well-known systems.

---

## What Jepsen actually does

Worth knowing concretely, because "we'd use Jepsen" is a common answer and a shallow one:

1. **Set up** a cluster of N nodes.
2. **Generate** a randomised workload of operations across many clients.
3. **Nemesis**: concurrently inject faults — network partitions, process kills, clock skew, pauses.
4. **Record** the complete history: every invocation, completion, and result — including
   **indeterminate** operations that timed out.
5. **Heal** the faults and let the cluster recover.
6. **Check** the recorded history against the claimed consistency model.

Step 4 has the subtlety that catches people out. **A timed-out operation is not a failed
operation.** The write may have been applied; you simply do not know. It must be recorded as
*indeterminate* and the checker must consider both possibilities. Treating a timeout as "didn't
happen" produces false violation reports, and treating it as "happened" hides real ones. Being
able to say this out loud demonstrates you have thought about the problem properly rather than
read the marketing page.

---

## Trade-offs & key takeaways

- **Assert possibility, not equality.** Record a history and prove no valid ordering explains it.
- **Concurrent operations may be ordered freely; non-overlapping ones may not.** Real-time
  constraints are what give the checker its teeth.
- **Checking is exponential**, so run many short histories rather than one long one.
- **Cycle detection (Elle) scales where permutation search does not**, and localises the anomaly.
- **Check against the claimed model.** Strongest-model-always is a bug report nobody will accept.
- **Timeouts are indeterminate, not failed.** This distinction is where naive harnesses go wrong.
- **A violation is a real, reproducible bug** — not a flake. Which is exactly why teams who lack
  these tools misfile these bugs as flakes for years.

---

## Interview angle

**"How would you test a distributed key-value store?"**

A weak answer lists integration tests. The strong shape:

1. **Name the property.** "What does it claim — linearizable? snapshot isolation? I'd test against
   the documented guarantee."
2. **History-based checking.** Explain that there is no single expected value under concurrency,
   so you record histories and check them for the existence of a valid ordering.
3. **Fault injection.** These bugs surface under partitions, pauses, and clock skew — a workload
   without a nemesis mostly tests the happy path.
4. **The timeout subtlety.** Indeterminate operations must be modelled as such.
5. **Scale the approach.** Many short randomised histories; seed the generator so a failure
   reproduces ([7.2](02-deterministic-simulation-testing.md)).
6. **Report usefully.** A cycle or a minimal counterexample history, not "it broke".

**Follow-ups:**
- *"Your checker is too slow."* → Shorten histories, memoise on (remaining ops, state), or switch
  from permutation search to dependency-cycle detection.
- *"It fails once in 500 runs."* → That is not flakiness, that is a real bug with a narrow
  interleaving. Seed the run so it reproduces, then shrink the history to a minimal case.
- *"How do you test eventual consistency?"* → Stop the workload, heal the faults, wait for
  quiescence, then assert convergence across replicas — plus check that no causal guarantee was
  violated en route.

**How this shows up in an SDET loop:** it usually arrives as *"we have a data-consistency bug that
only happens in production"*. The answer they are listening for is a reproducible harness —
history recording plus fault injection plus a checker — rather than more logging.

---

## Self-check

1. Why can't you use `assertEquals` on a concurrent register?
2. Define linearizability in terms of invocation and completion times.
3. In the second diagram above, why is `read(x) -> 1` impossible?
4. Why is permutation search infeasible, and what two things make the real algorithm tractable?
5. What does a cycle in Elle's dependency graph prove, and why is that more useful than a boolean?
6. Why must a timed-out operation be recorded as indeterminate?
7. Why is checking linearizability against a snapshot-isolation database a mistake?

---

## Practice → the coding assignment

[`01-linearizability-assignment/`](01-linearizability-assignment/assignment/)

```bash
cd sd/07-testing-distributed-systems/01-linearizability-assignment/assignment
go test ./...
```

You will build a real linearizability checker: history modelling, concurrency detection, the
Wing-Gong backtracking search with memoisation, indeterminate-operation handling, and a minimal
counterexample reporter.

**Next:** [7.2 — Deterministic Simulation Testing »](02-deterministic-simulation-testing.md)
