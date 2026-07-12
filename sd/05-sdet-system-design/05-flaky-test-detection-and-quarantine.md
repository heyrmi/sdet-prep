# 5.5 — Flaky Test Detection & Quarantine

> **Module 5 · SDET System Design** · ~35 min read
> *Concepts exercised:* flakiness taxonomy, rerun signatures, statistical flake scoring, quarantine
> workflow, results DB schema, quality gates & flake budgets, ownership/alerting, dashboards.

---

## The problem

A test fails your CI. You re-run it. It passes. You merge. That test is **flaky** — it produces a
different result on the *same code*, non-deterministically. One flaky test is annoying. Ten thousand
tests with a 1% flake rate means **~100 spurious failures per full run**, and something worse
happens: engineers stop trusting red. They reflexively hit "re-run" on every failure — including the
*real* one. The suite that was supposed to protect production now trains people to ignore it. This
is the **"boy who cried wolf"** failure mode, and at scale it's an existential threat to a test
program.

You're designing the **flaky-test detection and quarantine system**: the pipeline that identifies
flaky tests statistically, isolates them so they can't block releases, tracks them to an owner, and
surfaces the trend — the thing Google, Meta, Uber, and Microsoft all built once their suites crossed
into the tens of thousands.

> **Analogy.** A smoke detector that goes off when you make toast. If it cries wolf often enough,
> people pull the battery — and now it won't warn them about a *real* fire. The fix isn't to ignore
> the detector; it's to identify the ones that false-alarm (detection), take them offline for repair
> without leaving the building unprotected (quarantine, while other detectors still guard the gate),
> and assign someone to fix them (ownership) — all while tracking how many detectors are
> misbehaving (flake budget).

---

## Step 1: The flakiness taxonomy (know your enemy)

You can't fix flakiness generically — you diagnose by *category*. The common root causes:

- **Async / timing (the #1 cause).** The test asserts before the app finished. `sleep(500)` "fixed
  it" until a slow CI node made 500 ms too short. Fix: wait for a **condition/event**, not a
  duration (ties to [5.4](04-design-for-testability.md)).
- **Test-order dependence / shared state.** Test B passes only if Test A ran first (leftover data,
  a mutated global, a cached login). Reordering or parallelizing exposes it. Fix: per-test isolation
  ([5.4 §5](04-design-for-testability.md)).
- **Concurrency / race conditions.** Two threads/tests touch the same resource; the winner varies.
  Sometimes the flake reveals a **real product race** — don't just paper over it.
- **Resource leaks / exhaustion.** Passes until the 400th test fills the disk / runs out of file
  handles / OOMs the browser. Flakiness correlated with *position in the run* is the tell.
- **External dependency / network.** A real 3rd-party call, DNS blip, or rate limit. Fix: stub it
  ([5.4 §2](04-design-for-testability.md)).
- **Non-determinism in the test.** Unseeded randomness, time-of-day/timezone, locale, floating point,
  unordered collection assumed ordered (map iteration).
- **Environment / infra.** Underpowered CI node, noisy neighbor, browser-version drift. Often
  presents as "flaky everywhere at once" (a fleet issue, not a test issue).

> **Interview signal.** Being able to *name and diagnose by category* — and knowing that timing and
> shared-state dominate — is what separates "reruns fix flakes" (junior) from "here's how I'd
> classify and attack them" (senior).

---

## Step 2: Detection

You can't quarantine what you can't detect. Two complementary mechanisms.

### 2a. Rerun signatures (immediate, per-run)

When a test fails, **automatically re-run it (bounded, e.g. up to 2×)** on the *same commit*:

```
   test result sequence on unchanged code:
   FAIL → PASS            → FLAKY-this-run  (record final_status = passed_on_retry)
   FAIL → FAIL → FAIL     → genuine FAIL    (block the gate)
   PASS                   → PASS            (green)
```

- A **flip** (fail then pass, same code) is the strongest same-run flake signal.
- Crucial: **record it, don't hide it.** `final_status = passed_on_retry` is *not* the same as
  `passed`. Silently greening reruns is how flakiness becomes invisible and metastasizes. The retry
  buys the release; the *record* feeds detection.

> **Trade-off — reruns mask real bugs.** Auto-rerun-until-green makes CI green but can hide a genuine
> product race that only manifests sometimes. Bound retries (≤2), always record the flip, and treat
> a **rising `passed_on_retry` rate as a product-quality alarm**, not a convenience.

### 2b. Statistical flake scoring (rolling, cross-run)

The robust signal is historical: over a rolling window, how often does a test **flip results on
unchanged code?**

- **Flip-rate / flakiness score:** for a test, look at consecutive runs where the *code path didn't
  change* (same commit, or the test's dependencies unchanged per impact analysis) and count
  transitions between pass and fail. `flake_score = flips / runs_in_window`.
- **A concrete rule:** over the last 100 relevant runs, if a test failed on ≥5 runs where the code
  under test was unchanged → flag flaky (score ≥ 0.05). Tune the threshold to your tolerance.
- **Confidence over volume:** don't quarantine on one flip — require N observations so you don't
  quarantine a test that legitimately failed once for a real bug. Some teams use a Bayesian/Wilson
  lower-bound on the flip rate to avoid over-reacting to small samples.
- **Cluster by `error_signature`** (normalized stack+message hash from
  [5.1 §3.5](01-design-a-test-automation-platform.md)): "these 40 flaky failures are all the same
  root cause" — fix one thing, clear forty.

```
   test_history over window:
   run:  1  2  3  4  5  6  7  8  9 10   (code unchanged throughout)
   res:  P  P  F  P  P  F  P  P  F  P
                ↑     ↑        ↑        3 flips / 10 runs = flake_score 0.30 → QUARANTINE
```

---

## Step 3: Results DB schema

Detection needs history, so the results store ([5.1 §3.5](01-design-a-test-automation-platform.md))
must record per-attempt granularity, not just final pass/fail.

```
runs(run_id PK, commit, branch, suite, started_at, finished_at,
     total, passed, failed, flaky, quarantined)

test_results(run_id, test_id, status, final_status, attempts,
             duration_ms, error_signature, code_changed BOOL,
             PRIMARY KEY(run_id, test_id))
   -- status: raw first-attempt outcome
   -- final_status: passed | failed | passed_on_retry | quarantined | timed_out
   -- code_changed: did this test's code/deps change vs prior run? (from impact analysis)

test_attempts(run_id, test_id, attempt_no, status, duration_ms, error_signature)
   -- one row per (re)run — the raw flip signal lives here

test_flake_state(test_id PK, owner_team, flake_score, window_runs, window_flips,
                 quarantined BOOL, quarantined_at, ticket_id,
                 consecutive_green, last_evaluated)
   -- the current verdict per test; updated by the nightly scorer

flake_events(id PK, test_id, event, ts)   -- audit: quarantined / un-quarantined / re-flagged
```

- `code_changed` is the linchpin: a fail *after* a code change is a candidate *real* failure; a flip
  on *unchanged* code is flakiness. Getting this from impact analysis
  ([5.2 §5](02-design-a-ci-cd-pipeline.md)) makes scoring far more accurate.
- `test_attempts` (per-attempt rows) is what makes flip-rate computable — don't collapse retries.
- A **nightly job** rolls `test_attempts` → `test_flake_state`, recomputes scores, and flips
  quarantine status.

---

## Step 4: The quarantine workflow

Quarantine = **the test still runs, but its result cannot block the gate.** This is the central
idea, and the subtle part is *why it keeps running*.

```
   detector marks test flaky (score ≥ threshold)
        │
        ▼
   QUARANTINE:  ┌─ still executes every run (keep collecting signal) ─┐
                │  result excluded from the pass/fail GATE decision    │
                │  surfaces on quarantine dashboard w/ owner + ticket  │
                └──────────────────────────────────────────────────────┘
        │
        ▼  auto-filed ticket → owning team fixes root cause
        ▼
   N consecutive green runs on unchanged code
        │
        ▼
   AUTO-UN-QUARANTINE  (test returns to gating; flake_event logged)
```

- **Why keep running it?** (a) So you know when it's *fixed* — if quarantined tests were skipped,
  you'd never see them go green and could never restore them. (b) It might still catch a real
  regression. (c) You keep measuring the flake to confirm the fix.
- **Auto-file a ticket** to the owning team on quarantine, with the failure history, `error_signature`
  cluster, and links to failing videos/traces. Quarantine without ownership just hides the problem.
- **Auto-un-quarantine** after N consecutive green runs (e.g. 20) on unchanged code — objective,
  no human forgetting to restore it.
- **Prevent abuse:** quarantine is a *pit stop, not a graveyard*. Cap the number/age of quarantined
  tests (see the flake budget); a test quarantined >30 days with no fix gets escalated or deleted —
  a permanently-quarantined test provides zero protection and should be treated as tech debt.

> **Trade-off — quarantine vs delete vs block.** *Block on flaky* (no quarantine) keeps the gate
> honest but destroys velocity and trust — one flaky test halts everyone. *Delete flaky tests* is
> fast but throws away coverage (and the bug it might catch). *Quarantine* is the middle path:
> preserve the test and its signal, unblock the gate, and force ownership — but you must bound it so
> quarantine doesn't become a coverage black hole.

---

## Step 5: Quality gates & flake budgets

Detection + quarantine feed the **gate** logic in CI/CD ([5.2 §6](02-design-a-ci-cd-pipeline.md)).

- **Gate excludes quarantined tests:** a run is "green to merge" if all *non-quarantined* tests pass.
  Quarantined failures are reported but don't block.
- **Flake budget:** a ceiling on acceptable flakiness — e.g. "quarantined tests ≤ 0.5% of the suite"
  and "new flakes introduced per week ≤ 10." Crossing the budget:
  - **warns** at first, then **fails the merge for the team** that owns the offending tests, forcing
    prioritization. A budget with no teeth is ignored.
- **Flake budget as a leading indicator:** a rising flake rate predicts a suite losing trust *before*
  it collapses. Track budget burn-down like an SLO error budget
  ([3.5](../03-distributed-systems/05-fault-tolerance.md) / SRE thinking).
- **Prevent new flakes at the door:** run a new/changed test **N times in the PR** (e.g. 10×); if it
  flips, block *before* it ever enters the suite — cheaper than quarantining it later.

---

## Step 6: Ownership & alerting

Flaky tests die only when someone owns them.

- **Ownership mapping:** every test maps to a team (via `CODEOWNERS`, directory→team, or metadata
  annotations). `test_flake_state.owner_team` drives routing.
- **Alerting, but *aggregated and actionable*:** don't page on every flaky failure (that's the
  crying-wolf you're trying to kill). Instead: a **weekly digest** per team ("you own 12 flaky
  tests, here are the top 3 by impact"), and an **immediate alert only when the flake budget is
  breached** or a *previously-stable* test suddenly starts flaking hard.
- **Impact-rank the backlog:** prioritize by how many *runs/engineers* a flake disrupts
  (`flips × runs_affected`), not raw count — fixing the one flake that blocks 200 pipelines/day beats
  fixing ten that fire monthly.
- **Blameless framing:** flakiness is a *system* property (timing, infra, shared state), rarely one
  engineer's fault. Alerting should route to fix, not blame.

---

## Step 7: Dashboards

What the platform surfaces (feeds from the results DB → OLAP store, [5.1 §3.5](01-design-a-test-automation-platform.md)):

- **Flakiest tests** (ranked by flake_score × impact), each with history sparkline, owner, ticket,
  and last N failures grouped by `error_signature`.
- **Quarantine board:** what's quarantined, since when, by whom-owned, age (flag the stale ones over
  budget/age).
- **Flake budget burn-down:** current flake % vs budget, trend over weeks — the health-of-the-suite
  headline metric.
- **Trend lines:** new flakes/week, mean-time-to-fix, quarantine in/out rates. A healthy program has
  quarantine *out-rate ≥ in-rate* (you're fixing faster than you're accruing).
- **Per-team scorecards:** owned flakes, budget status — makes the debt visible where it can be
  fixed.

---

## Trade-offs & key takeaways

- **Flakiness is an existential threat to trust:** unmanaged, it trains engineers to ignore red and
  lets real failures through ("cried wolf").
- **Diagnose by taxonomy** — timing and shared-state dominate; some flakes are *real product races*,
  so don't blindly paper over them.
- **Detect two ways:** immediate **rerun flip signatures** (bounded, always *recorded* as
  `passed_on_retry`) + rolling **statistical flake scoring** on unchanged-code flip rate.
- **Record per-attempt history** and a `code_changed` flag — you can't score flakiness without it.
- **Quarantine = run but don't gate**, with an **owner + auto-ticket + auto-un-quarantine** after N
  greens. Keep running it so you know when it's fixed.
- **Flake budgets with teeth** turn "someday" into "this sprint"; block new flakes at the PR by
  running changed tests N times.
- **Alert aggregated, not per-failure**, impact-rank the backlog, and keep it blameless.
- **Bound quarantine** — a permanently-quarantined test is coverage debt, not protection.

---

## In the wild

- **Google** — "Flaky Tests at Google" (they measure ~1.5% of tests as flaky and spend real compute
  detecting/quarantining them); dependency-aware to know when code actually changed.
- **Meta / Microsoft** — probabilistic flakiness prediction and automated quarantine at monorepo
  scale.
- **Uber, Dropbox, Spotify, LinkedIn** — published flaky-test detection + quarantine services and
  flake budgets.
- **Tooling:** BuildPulse, Trunk Flaky Tests, Datadog CI Test Visibility, CircleCI test insights,
  GitHub Actions flaky detection, Allure/ReportPortal flake analytics; pytest `--flake-finder`,
  JUnit `@RepeatedTest`, Playwright/Jest retry+report.

---

## SDET interview angle

This is a *signature* SDET question because pure-backend candidates rarely think about it. Lead with
**why flakiness is a trust/velocity problem** (cried wolf), give the **taxonomy** (timing +
shared-state dominate), then the **detection** (flip signatures + statistical scoring on unchanged
code), the **quarantine workflow** (run-but-don't-gate + ownership + auto-restore), and **flake
budgets**. The senior move is insisting reruns are *recorded, not hidden*, and that quarantine is
bounded with an owner.

**Common follow-ups:**

- *"How do you tell a flaky test from a real failure?"* → flip on *unchanged* code = flaky
  (`code_changed=false` + rerun passes); consistent fail after a change = real.
- *"Won't auto-retry hide real bugs?"* → yes if silent — bound retries, record `passed_on_retry`,
  alarm on rising retry rate (may be a product race).
- *"A test is quarantined forever — now what?"* → age/count cap in the flake budget → escalate,
  reassign, or delete; permanent quarantine = coverage debt.
- *"How do you stop new flaky tests entering the suite?"* → run changed tests N× in the PR; block if
  they flip.
- *"How do you make teams actually fix them?"* → flake budget with teeth (blocks the owning team's
  merges past the ceiling) + per-team scorecards + impact-ranked backlog.
- *"Design the schema."* → `test_attempts` (per-retry rows) + `test_flake_state` (score, owner,
  quarantine) + a `code_changed` flag; nightly scorer.

---

## Practice / self-check

1. Explain the "cried wolf" failure mode. Why is a 1% flake rate on 10,000 tests a crisis, not a
   nuisance?
2. Give four flakiness categories and a design fix for each. Which two dominate in practice?
3. Why must a rerun that passes be recorded as `passed_on_retry` rather than `passed`? What signal do
   you lose by hiding it?
4. Define a concrete statistical flake score and a quarantine threshold. Why require N observations
   before quarantining?
5. Why does a quarantined test keep *running* instead of being skipped? Give two reasons.
6. Design the results schema needed to compute flip-rate. Which column distinguishes flakiness from a
   real regression, and where does it come from?
7. Design a flake budget with teeth and an alerting policy that doesn't itself cry wolf.

---

## How this shows up in an SDET loop

A high-signal question that shows up as its own design round *and* as the natural deep-dive on the
[5.1 platform](01-design-a-test-automation-platform.md) ("okay, tests are flaky — what do you do?").
Interviewers use it to check whether you've operated a real suite at scale: the taxonomy, the
detect-record-don't-hide discipline, the run-but-don't-gate quarantine, and flake budgets are all
things you only internalize from having lived the pain. It's also where you demonstrate that you
treat test health as a measurable, owned, budgeted system property — the staff-level framing.

**End of Module 5.** Back to the [Module 5 index](README.md) · or the
[course README](../README.md).
