# Module 5 — SDET System Design

> **Design the systems that test the systems.** Modules 0–4 teach you to design products (rate
> limiters, feeds, payment systems). This module turns the lens around: how do you design the
> **test automation, CI/CD, and quality infrastructure** that a whole engineering org rides on?

---

## Why this module exists

A senior SDET interview loop almost always has a **system-design round** — but the prompts are
test-flavored: *"design a test automation platform,"* *"design CI/CD,"* *"how would you build a
browser farm,"* *"how do you handle flaky tests at scale?"* These aren't different from "real"
system design — they're the same distributed-systems muscles (queues, sharding, blob storage,
autoscaling, quality gates) applied to the testing domain. This module builds that muscle.

Each lesson follows the **same 7-part shape** as the rest of the course (problem → core idea →
mechanics → trade-offs → in-the-wild → interview angle → practice), with an explicit **SDET
interview angle** and a **"how this shows up in an SDET loop"** note.

**Every lesson now ships a Go assignment**, exactly like Modules 2 and 4. Reading a design and
being able to *build* its core are different skills, and only the second one survives an
interviewer asking "okay, how would you actually implement the quarantine policy?"

**Prerequisites:** you'll get the most out of these after Module 2 (building blocks — queues,
caching, load balancing) and skimming Module 4's case-study format. The lessons cross-link heavily
back into those.

---

## The lessons

| # | Lesson | Core concepts exercised | Assignment |
|---|--------|--------------------------|-----------|
| 5.1 | [Design a Test Automation Platform](01-design-a-test-automation-platform.md) | job queue, duration-aware sharding, worker fleet, result store, artifact/blob storage, flake detection, quarantine, autoscaling, shard math | [`01-test-platform-assignment/`](01-test-platform-assignment/assignment/) — LPT shard balancing, critical path, optimal worker count, capability constraints |
| 5.2 | [Design a CI/CD Pipeline](02-design-a-ci-cd-pipeline.md) | stage DAG, build-once-promote, caching, parallelization, quality gates, test impact analysis, canary/blue-green/rollback, secrets, failure isolation | [`02-quality-gates-assignment/`](02-quality-gates-assignment/assignment/) — gate engine with advisory/blocking/mandatory tiers, **DORA metrics**, performance bands |
| 5.3 | [Design Test Infrastructure at Scale](03-design-test-infrastructure-at-scale.md) | Selenium Grid, containerized browsers (Selenoid/Moon), session routing & affinity, ephemeral environments, autoscaling, video capture, concurrent-session sizing, build-vs-buy | [`03-device-pool-assignment/`](03-device-pool-assignment/assignment/) — expiring leases, least-loaded allocation, health ejection, per-tenant fairness, autoscale |
| 5.4 | [Design for Testability](04-design-for-testability.md) | seams & dependency injection, test hooks & observability, fault injection, test data management & seeding, contract testing (Pact), chaos & load harnesses | [`04-testability-seams-assignment/`](04-testability-seams-assignment/assignment/) — fake clock, seeded PRNG, fault injector, reset registry |
| 5.5 | [Flaky Test Detection & Quarantine](05-flaky-test-detection-and-quarantine.md) | flakiness taxonomy, rerun signatures, statistical flake scoring, quarantine workflow, results DB schema, flake budgets, ownership/alerting, dashboards | [`05-flaky-quarantine-assignment/`](05-flaky-quarantine-assignment/assignment/) — same-commit flake scoring, quarantine state machine, impact ranking |

### Working the assignments

```bash
cd sd/05-sdet-system-design/05-flaky-quarantine-assignment/assignment
go test ./...          # red — every function is a TODO
go test -race ./...    # several of these are concurrent by design
# implement until green; the reference is in ../solution/
```

Each assignment starts red and each solution is verified green in CI, so a broken reference
can't sit here unnoticed.

---

## How they fit together

```
   5.2 CI/CD Pipeline  ── gates & triggers ──►  5.1 Test Automation Platform
        │                                            │  runs tests on...
        │  test impact analysis                      ▼
        ▼                                       5.3 Test Infrastructure
   quality gates ◄── flake budget ──┐          (browsers, devices, envs)
                                      │
   5.5 Flaky Detection & Quarantine ─┘  ◄── depends on ──  5.4 Design for Testability
        (results schema, scoring,                          (seams, data isolation,
         quarantine, budgets)                               contract tests, fault injection)
```

- **5.1** is the flagship — the platform everything else supports.
- **5.2** is the delivery backbone that triggers 5.1's runs and enforces 5.5's flake budgets.
- **5.3** supplies the browsers/devices/environments 5.1's workers need.
- **5.4** is the design philosophy that makes systems cheap and reliable to test at all.
- **5.5** closes the loop: keeping the suite *trustworthy* so the gates in 5.2 mean something.

---

## Suggested order

Read **5.1 first** (it anchors the vocabulary), then **5.2**, then **5.3** (deep dive on where the
browsers come from), then **5.4** (the "how do we test *any* system" lens), and finish with **5.5**
(the topic that most distinguishes SDET candidates). After each, close the file and explain the
design out loud — especially the back-of-envelope numbers and the trade-offs, since that's exactly
what an interviewer is listening for.

**Start here:** [5.1 — Design a Test Automation Platform »](01-design-a-test-automation-platform.md)
