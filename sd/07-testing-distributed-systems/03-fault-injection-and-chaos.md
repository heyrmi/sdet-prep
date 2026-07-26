# 7.3 — Fault Injection & Chaos

> **Prerequisites:** [3.5 Fault Tolerance](../03-distributed-systems/05-fault-tolerance.md),
> [3.6 Observability](../03-distributed-systems/06-observability.md),
> [7.2 Deterministic Simulation](02-deterministic-simulation-testing.md).

---

## The problem

Every distributed system has failure-handling code: retries, timeouts, failovers, circuit
breakers, reconciliation loops. It is usually the least-tested code in the system, and it runs
only when things are already going badly.

The result is the classic incident shape: **the failure was survivable, and the recovery logic
made it worse.** A retry storm turns a slow dependency into a dead one. A failover promotes a
replica that was behind. A circuit breaker never closes because its health check goes through the
thing that is broken.

Fault injection is how you find that out on a Tuesday afternoon instead of at 3 a.m.

---

## Core idea

> **Chaos engineering is not "break things randomly."** It is an *experiment*: state a hypothesis
> about how the system behaves under a specific fault, inject that fault with a bounded blast
> radius, and compare the observed behaviour to the hypothesis.

The formal shape, worth reciting because it is what separates the discipline from the caricature:

1. **Define steady state** as a measurable output, not an internal metric. "99.5% of checkouts
   complete in under 2s" — not "CPU is below 70%". A system can be perfectly healthy internally
   and useless to users.
2. **Hypothesise** that steady state persists through the fault.
3. **Inject** a realistic fault, starting in a non-production environment and with the smallest
   blast radius that can teach you anything.
4. **Measure**. Did steady state hold? How long to detect? How long to recover?
5. **Learn**, fix, and automate the experiment as a regression test.

Step 1 is the one teams skip, and skipping it is fatal: without a pre-agreed steady-state
definition, every result is argued about after the fact.

---

## The fault taxonomy

Know these by name, and know which are commonly *missed*:

| Fault | What it exposes | Commonly missed? |
|---|---|---|
| **Process crash** | Restart, state recovery, leader re-election | No — everyone tests this |
| **Process pause** (SIGSTOP, long GC, VM migration) | Timeout tuning, lease expiry, split brain | **Yes** — and it is nastier than a crash |
| **Network partition** | Quorum behaviour, split brain, CAP choices | Partly |
| **Asymmetric partition** (A sees B, B cannot see A) | Failure detectors that assume symmetry | **Yes** |
| **Packet loss / delay / reorder** | Retry logic, idempotency, ordering assumptions | **Yes** |
| **Clock skew** | Lease validity, TTLs, ordering by timestamp | **Yes** |
| **Slow dependency** (not down — *slow*) | Timeout budgets, bulkheads, queue growth | **Yes** — the most common real outage |
| **Disk full / slow / errors** | Write paths, WAL handling, error propagation | **Yes** |
| **Partial failure** (some requests fail) | Health checks, load-balancer ejection | **Yes** |

Two of these deserve emphasis because they cause disproportionate real-world damage:

**A pause is worse than a crash.** A crashed node is gone and everyone agrees. A paused node
*comes back* believing it is still the leader, still holding a lease that expired 30 seconds ago,
and immediately writes. Every lease-based design must be tested against pauses specifically, and
most are not.

**Slow is worse than down.** A dead dependency fails fast and circuit breakers engage. A
dependency answering in 8 seconds instead of 80 milliseconds keeps every caller's thread and
connection occupied, queues grow, memory grows, upstream timeouts cascade, and the whole system
falls over from something that never actually failed. Latency injection finds more real bugs than
kill-the-node ever will.

---

## Blast radius

The discipline that makes this safe, and the thing an interviewer will probe:

- **Start in staging.** Production experiments come after you have stopped finding bugs in staging.
- **Smallest useful scope.** One instance before one AZ; one AZ before a region; 1% of traffic
  before 100%.
- **A working abort.** An automatic stop condition on the steady-state metric, plus a manual kill
  switch — and the abort path must itself be tested, because an abort that depends on the
  component you just broke is not an abort.
- **Announce before you automate.** Run it as an announced game day first, so the on-call
  understands what they are seeing. Continuous unannounced chaos is an end state, not a start.
- **Never during an incident**, and not during a freeze.

Saying "and I'd bound the blast radius, with a tested abort" unprompted is worth more in an
interview than naming five chaos tools.

---

## Where it runs

**Deterministic simulation** ([7.2](02-deterministic-simulation-testing.md)) — the cheapest and
most thorough place, if the system supports it. Faults are part of the schedule, so a failure
reproduces from a seed.

**Integration tests with an injection layer** — a fault-injection proxy (Toxiproxy and similar)
between services, driven from the test. Realistic enough to catch timeout and retry bugs, cheap
enough to run in CI. **This is the highest value-per-effort option for most teams**, and the one
to propose when full DST is out of reach.

**Staging chaos** — real infrastructure, real faults, on a schedule.

**Production chaos** — the end state. Requires mature observability, automated aborts, and
organisational trust. Not a starting point, and proposing it as one signals inexperience.

**Game days** — humans in the loop, testing the *response* as well as the system: runbooks,
alerting, escalation paths, and whether the dashboard everyone relies on works when the thing it
monitors is down.

---

## What to actually assert

Injecting a fault is easy. Deciding whether the outcome was correct is the hard part, and it is
where the SDET adds value:

- **Availability**: did steady state hold, within the error budget?
- **Correctness**: was data lost, duplicated, or corrupted? This is where
  [7.1](01-consistency-checking-and-linearizability.md) plugs in — record the history and check
  it, because "it stayed up" is not the same as "it stayed correct."
- **Detection time**: how long until alerts fired? An undetected fault is worse than a loud one.
- **Recovery time**: how long to steady state after the fault was removed? Did it self-heal, or
  did a human intervene?
- **Blast containment**: did the failure stay in its bulkhead, or leak into unrelated features?
- **Observability**: could you tell what was wrong *from the dashboards alone*? A game day that
  reveals your dashboards are useless has paid for itself.

The pairing to remember: **chaos supplies the conditions, consistency checking supplies the
verdict.** Chaos without an oracle only tells you the system did not crash.

---

## Trade-offs & key takeaways

- **It is an experiment, not vandalism.** Hypothesis, bounded injection, measurement, learning.
- **Steady state must be user-visible and defined in advance.**
- **Pauses and latency are the underrated faults** — and the ones that cause most real outages.
- **Bound the blast radius and test the abort path.**
- **Staging first, production last.** Announced game days before continuous automation.
- **Chaos needs an oracle.** Pair it with history checking, or you are only testing for crashes.
- **Assert on detection and recovery time**, not just survival.
- **Automate every experiment that ever found a bug** — that is how it becomes a regression suite
  rather than a stunt.

---

## Interview angle

**"How would you verify our system survives an AZ failure?"**

1. **Define steady state first** — user-visible SLI, agreed before the experiment.
2. **State the hypothesis** explicitly: "checkout success stays above 99.5%, p99 under 2s, with
   recovery inside 60 seconds."
3. **Start small**: one instance, then one AZ in staging, then a fraction of production traffic.
4. **Inject realistically** — not just termination, but the AZ going *slow and partially
   reachable*, which is what actually happens.
5. **Measure four things**: did steady state hold, time to detect, time to recover, and was any
   data lost or duplicated.
6. **Guardrails**: automatic abort on the SLI, tested manual kill switch, announced window.
7. **Automate it** once it passes, so a regression in failover logic is caught by CI rather than
   by the next real AZ event.

**Follow-ups:**
- *"We can't run chaos in production."* → Fine, and common. Fault-injection proxies in integration
  tests plus staging chaos get most of the value. Production chaos is an end state.
- *"We killed a node and everything was fine."* → Then the experiment was too weak. Try a pause
  instead of a kill, an asymmetric partition instead of a clean one, and latency instead of
  failure.
- *"How do you stop it causing an incident?"* → Blast radius, automated abort on the steady-state
  metric, a tested kill switch, and never during an incident or a freeze.
- *"How do you know the system stayed CORRECT, not just up?"* → Record the operation history
  through the fault and check it against the claimed consistency model. This is the follow-up that
  separates a chaos enthusiast from a test architect.

---

## Self-check

1. What makes chaos engineering an experiment rather than random destruction?
2. Why must steady state be user-visible and defined beforehand?
3. Why is a process pause more dangerous than a crash?
4. Why does a slow dependency cause worse outages than a dead one?
5. What is wrong with an abort mechanism that runs through the component under test?
6. Name four things to assert besides "it stayed up".
7. Why is chaos without a consistency oracle only half a test?

---

**Module complete.** Back to the [Module 7 index »](README.md), or on to the assignment:
[`01-linearizability-assignment/`](01-linearizability-assignment/assignment/)
