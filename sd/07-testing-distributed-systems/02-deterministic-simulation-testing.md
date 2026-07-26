# 7.2 — Deterministic Simulation Testing

> **Prerequisites:** [7.1 Consistency Checking](01-consistency-checking-and-linearizability.md),
> [5.4 Design for Testability](../05-sdet-system-design/04-design-for-testability.md).

---

## The problem

Your distributed system fails once every few thousand runs. You cannot reproduce it. The logs show
the symptom but not the cause. Someone files it as flaky, adds a retry, and moves on.

Two years later it takes down production, and the post-mortem discovers it was the same bug.

The reason it could never be debugged is that **the interleaving that caused it was never
recorded**. Thread scheduling, network delivery order, timer firing, disk latency — all
nondeterministic, all different on every run. The failing execution existed once and can never be
recreated.

**Deterministic Simulation Testing (DST)** removes that entire category of problem by making the
execution reproducible from a seed.

---

## Core idea

Replace every source of nondeterminism with a **seeded, controllable simulation**:

| Real world | Simulated |
|---|---|
| OS thread scheduler | A deterministic scheduler that picks the next task from a seeded PRNG |
| Wall-clock time | Virtual time that only advances when the simulation says so |
| Network | An in-process message bus that reorders, delays, drops and duplicates — by seed |
| Disk I/O | An in-memory store that can inject latency, errors, and torn writes — by seed |
| `rand()` | The same seeded PRNG |

Now an entire run — every interleaving, every message reorder, every fault — is a pure function of
one integer.

```
seed 8f3a2c ──► simulation ──► FAILURE: linearizability violation at op 4,127

# and then, forever after:
$ ./sim --seed 8f3a2c
FAILURE: linearizability violation at op 4,127      ← identical, every time
```

That is the whole trick, and its consequences are larger than they first appear:

- **Bugs reproduce on demand.** A failure becomes a regression test by writing down a number.
- **You can bisect.** Same seed, older commit — you find the introducing change in minutes.
- **You can shrink.** Search for a smaller seed or a shorter operation count that still fails, and
  hand a human a ten-operation counterexample instead of a ten-thousand-operation log.
- **Debugging is possible at all.** Attach a debugger, add prints, re-run — the run is identical.
  You cannot do this with a heisenbug; observing it changes it.
- **Time is free.** Simulated time advances instantly, so you can explore a simulated week of
  clock skew and partitions in seconds of CPU. This is the multiplier that makes DST worth the
  engineering cost.

---

## What it requires from the system

DST is not something you can bolt on. It imposes a real architectural constraint:

> **All nondeterminism must enter through injectable seams.**

Concretely: no `time.Now()` in business logic, no direct `rand()`, no raw sockets, no unmanaged
background threads. Everything goes through an interface the simulator can substitute.

This is precisely the seam discipline from
[5.4](../05-sdet-system-design/04-design-for-testability.md), taken to its logical end. And it is
the point where you get to make the argument that defines a test architect: **testability is a
design property, not a testing activity.** A team that wants DST must build for it from the start;
a team that did not cannot retrofit it cheaply.

The costs are real and should be stated honestly:

- **Significant upfront investment.** FoundationDB's team spent roughly 18 months building their
  simulator before the database read or wrote a real disk.
- **Simulation fidelity risk.** Your simulated network is not the real network. Bugs that depend
  on real hardware behaviour — a NIC's actual reordering, a disk's actual fsync semantics — can
  hide behind a too-friendly simulation.
- **Discipline to maintain.** One `time.Now()` slipped into a hot path silently breaks
  determinism, and you often will not notice until a seed stops reproducing.

---

## Coverage: the part people miss

Determinism gives you reproducibility. It does not, by itself, give you coverage — running seed 1
a million times explores one execution a million times.

What makes DST powerful is **searching the space of executions**, which the deterministic
scheduler makes possible:

- **Randomised exploration** — millions of different seeds, each a different interleaving.
- **Biased schedulers** — deliberately prefer the orderings humans get wrong: preempt right after
  a state mutation but before its commit, deliver messages out of order, fire timeouts at the
  worst moment.
- **Fault injection as part of the schedule** — partitions, crashes, and clock skew placed at
  chosen points rather than hoped for.
- **Swarm testing** — vary the *configuration* across runs (which fault types are enabled, which
  operations are in the workload), because a run that enables everything at once often explores
  less than a set of runs each enabling a focused subset.

The mental model worth carrying: **DST turns "we hope CI catches it" into "we are systematically
searching for it, and when we find it we can hand you the seed."**

---

## In the wild

- **FoundationDB** popularised the approach: a full deterministic simulator built before the
  database was trusted with real data. The result is the frequently-cited anecdote that Kyle
  Kingsbury (of Jepsen) declined to test FoundationDB on the grounds that their own simulator had
  already stressed it harder than Jepsen would.
- **Antithesis** — a commercial platform from the FoundationDB team that deterministically
  simulates a set of Docker containers at the hypervisor level, so *any* system can be tested this
  way without rewriting it around seams. Notably, recent Jepsen releases added support for running
  Jepsen **inside** Antithesis — the two techniques compose rather than compete.
- **TigerBeetle**, **WarpStream**, **RisingWave**, **Resonate** — modern infrastructure built with
  DST from the start; WarpStream has written publicly about applying it across an entire SaaS.
- **TLA+ / P** — a related but distinct idea: model checking verifies a *specification*
  exhaustively, whereas DST tests the *implementation* on sampled executions. They are
  complementary. TLA+ proves your design is right; DST catches the fact that your code does not
  match your design.

---

## Trade-offs & key takeaways

- **One seed reproduces an entire execution.** That is the single property everything else follows
  from.
- **Reproducibility ≠ coverage.** You still need many seeds, biased schedulers, and swarm
  configuration.
- **Simulated time is free time** — a week of chaos in seconds.
- **It demands seam discipline.** DST is only possible in a system designed to be simulated.
- **Fidelity is a real risk.** The simulator is a model, and models are wrong somewhere.
- **A DST failure is never flaky.** If it reproduces from a seed, it is a bug — full stop.
- **It composes with 7.1**: DST provides the reproducible executions; the consistency checker
  provides the oracle that decides whether each one was legal.

---

## Interview angle

**"Our distributed system has a bug that happens once a week in production and we can't reproduce
it. What do you do?"**

This is the prompt DST exists for. The shape:

1. **Name the real problem.** The failing interleaving is not recorded, so it cannot be recreated.
   Adding logs is a way to spend three months not fixing it.
2. **Propose determinism.** Seeded scheduler, virtual clock, simulated network and disk — every
   run a pure function of a seed.
3. **Be honest about cost.** This is an architectural change requiring seams throughout, and it
   is months of work, not a sprint. Say so; pretending otherwise is a bigger red flag than the
   cost itself.
4. **Offer the incremental path.** Full DST may be out of reach, so start with what is not:
   inject the clock, seed the randomness, add a fault-injection layer at the network boundary,
   record histories, and check them ([7.1](01-consistency-checking-and-linearizability.md)). Most
   of the value, a fraction of the cost.
5. **Explain the search.** Many seeds, biased scheduling toward known-dangerous orderings, swarm
   configuration.
6. **Close the loop.** A failing seed becomes a permanent regression test, and shrinking gives a
   minimal counterexample a developer can actually read.

**Follow-ups:**
- *"How do you know the simulator matches reality?"* → You do not, fully. Mitigate by keeping the
  simulator's fault model grounded in observed production incidents, and by continuing to run real
  chaos experiments ([7.3](03-fault-injection-and-chaos.md)) as a fidelity check.
- *"We can't afford 18 months."* → Point 4. The incremental path is the answer, and knowing it
  distinguishes a pragmatist from someone reciting a blog post.
- *"How is this different from TLA+?"* → Model checking verifies the design; DST tests the
  implementation. You want both, and they catch different bugs.

---

## Self-check

1. Why can't a heisenbug be debugged by adding print statements?
2. List the five sources of nondeterminism DST replaces.
3. Why does reproducibility alone not give you coverage?
4. What architectural property must a system have for DST to be possible?
5. Why is simulated time such a large multiplier on what you can test?
6. What is the fidelity risk, and how would you mitigate it?
7. Give the incremental path for a team that cannot fund a full simulator.

---

**Next:** [7.3 — Fault Injection & Chaos »](03-fault-injection-and-chaos.md)
