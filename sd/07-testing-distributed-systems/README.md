# Module 7 — Testing Distributed Systems

> **The bridge module.** Module 4 taught you to *design* seventeen distributed systems. This one
> asks the question that follows immediately, and that almost nobody prepares for:
> **how would you know if any of them were correct?**

---

## Why this module exists

Ask a strong SDET candidate to test a REST API and you get a good answer. Ask them to test a
distributed key-value store and the answer usually collapses into "write integration tests and run
them in CI."

That does not work, for a specific reason:

> **The bugs that matter in distributed systems only appear under interleavings you cannot
> reproduce by running tests.** A lost update requires a particular ordering of two writes and a
> network partition, at a particular moment. Running the suite a thousand times might never hit
> it. Running it in production hits it at 3 a.m. on a Sunday.

Conventional testing samples the space of executions essentially at random, and the space is
astronomically large. The techniques in this module attack that directly: they either **search the
space systematically** (deterministic simulation), **inject the conditions that expose the bugs**
(fault injection), or **check the results for impossibility** rather than for expected values
(consistency checking).

This is the single strongest differentiator available to an SDET candidate interviewing above
mid-level, and — per the audit that motivated this repo — it is the topic most conspicuously
missing from standard prep.

---

## The lessons

| # | Lesson | Core concepts | Assignment |
|---|--------|--------------|-----------|
| 7.1 | [Consistency Checking & Linearizability](01-consistency-checking-and-linearizability.md) | histories, concurrent operations, linearizability, the Wing-Gong search, Elle and cycle detection, what Jepsen actually does | [`01-linearizability-assignment/`](01-linearizability-assignment/assignment/) — a real linearizability checker over operation histories |
| 7.2 | [Deterministic Simulation Testing](02-deterministic-simulation-testing.md) | seeded schedulers, virtual time, controllable entropy, FoundationDB / Antithesis / WarpStream, why reproducibility changes everything | — |
| 7.3 | [Fault Injection & Chaos](03-fault-injection-and-chaos.md) | the fault taxonomy, partitions vs pauses vs clock skew, blast radius, steady-state hypotheses, game days, chaos in CI | — |

### Working the assignment

```bash
cd sd/07-testing-distributed-systems/01-linearizability-assignment/assignment
go test ./...      # red — implement until green
```

---

## How this fits the rest of the repo

```
   Module 3 (distributed systems)  ──► the properties you must verify
   Module 4 (17 case studies)      ──► the systems under test
              │
              ▼
   Module 7  THIS MODULE           ──► how you would actually verify them
              │
              ├─► 5.4 Design for Testability   (seams, fault injection, fake clocks)
              └─► 5.5 Flaky Test Quarantine    (a DST failure is never "flaky" — it reproduces)
```

The connection to [5.4](../05-sdet-system-design/04-design-for-testability.md) is not incidental.
Deterministic simulation is *only possible* if the system has the seams 5.4 describes — an
injected clock, injected randomness, injected I/O. A system that calls `time.Now()` directly
cannot be simulated, and that is a design defect that shows up as an untestability problem.

---

## The one-paragraph version

If you remember nothing else: **conventional testing asserts that outputs match expectations;
distributed-systems testing asserts that observed histories are *possible*.** You do not know what
the right answer is — many are legal — so instead you record what happened and prove no valid
serial ordering explains it. That inversion is the whole subject.

**Start here:** [7.1 — Consistency Checking & Linearizability »](01-consistency-checking-and-linearizability.md)
