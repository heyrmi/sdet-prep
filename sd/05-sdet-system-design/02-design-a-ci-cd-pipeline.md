# 5.2 — Design a CI/CD Pipeline

> **Module 5 · SDET System Design** · ~35 min read
> *Concepts exercised:* stage graphs, artifact promotion, caching, parallelization, quality gates,
> test impact analysis, deployment strategies (canary/blue-green), rollback, secrets, failure
> isolation, pipeline-as-code.

---

## The problem

Every merge to `main` should become a safe production deploy **without a human babysitting it** —
and if something's wrong, it should be *caught early and cheaply*, not in production at 2 a.m. Today
your team merges, then someone manually builds, manually runs tests locally, manually SSHes to a box
and copies a jar. Deploys are scary, batched into a Friday "release train," and rollbacks mean
git-reverting and repeating the whole ritual.

You're asked to design the **CI/CD pipeline**: the automated path from a git push to a validated
production deployment, with test gates, artifact promotion, and safe rollout. This is the
**delivery backbone** every product team rides on — think GitHub Actions, GitLab CI, Jenkins,
Buildkite, Argo/Spinnaker, plus Google's and Netflix's internal delivery platforms.

> **Analogy.** A factory assembly line with quality inspectors at each station. Raw material (source
> code) enters; each station adds something and *inspects* before passing it on. A part that fails
> inspection is pulled **before** it reaches the next, more expensive station — you don't paint a
> car door that failed the weld test. The finished car (a release artifact) is the *same physical
> object* that passed every station — you don't rebuild it at the showroom (**build once, promote
> the artifact**).

CI ("Continuous Integration") = merge and validate small changes often. CD ("Continuous
Delivery/Deployment") = every validated change is automatically shippable (delivery) or actually
shipped (deployment). SDET owns the **quality gates** that decide whether a change is allowed to
proceed.

---

## Step 1: Requirements

**Functional**

- **Trigger** on VCS events: push to a branch, PR opened/updated, tag, or schedule.
- **Stages** in a dependency graph: build → unit → integration → e2e → deploy (with fan-out).
- **Quality gates**: block promotion on failing tests, coverage below threshold, security scan
  findings, or a blown flaky budget.
- **Artifact management**: build once, store, and **promote the same artifact** dev→staging→prod.
- **Deploy strategies**: canary, blue-green, rolling — with automated **rollback**.
- **Test selection**: for a PR, run only the tests a change can affect (impact analysis), not all
  100k.
- **Secrets**: inject credentials safely, never in logs or the repo.
- **Observability**: per-run status, logs, timings, and *why* a stage failed.

**Non-functional**

- **Fast feedback:** PR pipeline ≤10–15 min or engineers context-switch and stop trusting it.
- **Reliable & deterministic:** same commit → same result (hermetic builds); no "works on the
  runner" flakiness.
- **Isolated failures:** one flaky e2e shouldn't fail a build that was actually fine; one team's
  runner outage shouldn't block another's.
- **Secure:** least-privilege credentials, artifact integrity, no secret leakage.
- **Scalable:** hundreds of concurrent pipelines at merge-storm peak.

---

## Step 2: The stage graph

The heart of CI/CD is a **DAG of stages**, ordered cheapest/fastest-first so failures are caught
before expensive stages run — the **test pyramid** rendered as a pipeline.

```
   push / PR
      │
      ▼
  ┌────────┐   ┌───────────┐   ┌───────────────┐   ┌──────────┐   ┌──────────────┐
  │ BUILD  │──►│ UNIT TESTS│──►│ INTEGRATION   │──►│  E2E     │──►│  DEPLOY      │
  │ compile│   │ (sec, 1000s│   │ (svc + DB,    │   │ (browser │   │ canary →     │
  │ + lint │   │  parallel) │   │  containers)  │   │  UI/API) │   │ blue-green)  │
  └────────┘   └───────────┘   └───────────────┘   └──────────┘   └──────────────┘
     fast, cheap  ◄───────── cost & time per test increases ─────────►  slow, costly
     runs on every push        run on PR / merge         run pre-prod / post-deploy
```

**Why this order (the pyramid economics):** a unit test costs milliseconds and pinpoints the bug; an
e2e test costs seconds-to-minutes and only says "checkout is broken somewhere." Running the
thousands of cheap tests first means 90% of regressions die in the first 30 seconds, and you only
spend expensive e2e minutes on changes that already passed the cheap gates. Inverting this — e2e
first — burns money and slows feedback.

- **Build:** compile, lint, static analysis (SAST), produce **one immutable artifact** (a container
  image, jar, or bundle) tagged with the commit SHA. This artifact is what every later stage tests
  and what eventually deploys.
- **Unit:** thousands of fast, isolated, parallel tests. No network, no DB.
- **Integration:** service + real dependencies spun up ephemerally (Testcontainers, docker-compose)
  — DB, message broker, downstream stub. Slower, catches wiring bugs units can't.
- **E2E:** full-stack in a deployed-like environment; UI (Playwright/Selenium) and API journeys.
  Fewest, slowest, most valuable — and most flake-prone (hence [5.5](05-flaky-test-detection-and-quarantine.md)).
- **Deploy:** progressive rollout with health checks and automatic rollback.

Stages **fan out** (run 8 shards of unit tests in parallel — see
[5.1](01-design-a-test-automation-platform.md)) and **fan in** (all shards must pass to proceed).

---

## Step 3: Build once, promote the artifact

The cardinal rule: **the binary you tested is the binary you ship.** Rebuilding per environment
means dev, staging, and prod each get a *different* artifact — a dependency could resolve
differently, and you'd be shipping something no test ever saw.

```
   BUILD (once, at commit abc123)
      │  produces  app:abc123  (immutable, content-addressed image)
      ▼
   push to artifact registry  ──────────────────────────────────────┐
      │                                                              │
      ▼ promote (metadata only — no rebuild)                         │
   [ dev ] ──gate──► [ staging ] ──gate──► [ prod ]                  │
   deploy app:abc123  deploy app:abc123    deploy app:abc123  ◄──────┘
   same bytes everywhere; each arrow is a quality gate, not a rebuild
```

- **Immutability & provenance:** tag by commit SHA (not `latest`), sign the artifact, and record a
  build attestation/SBOM (supply-chain integrity — SLSA). Promotion is just "this SHA is now
  approved for the next environment," a metadata change.
- **Rollback becomes trivial:** the previous good artifact still exists in the registry; roll back =
  redeploy `app:prev_sha`. No git gymnastics, no rebuild.

---

## Step 4: Caching & parallelization (making it fast)

Two levers keep a pipeline under the 10-minute budget.

**Caching** — don't redo work whose inputs didn't change:

- **Dependency cache:** `~/.m2`, `node_modules`, Go module cache, keyed by a hash of the lockfile.
  Cache hit turns a 3-minute `npm ci` into 5 seconds.
- **Build-layer cache:** Docker layer cache / Bazel/Gradle remote build cache. Only recompile
  changed modules.
- **Test-result cache:** if a module's inputs are byte-identical to a previous green run, skip its
  tests (Bazel does this — hermetic, content-addressed). Huge at monorepo scale.

> **Trade-off — caching vs correctness.** A stale or poisoned cache produces a *green build of code
> that's actually broken* — the scariest failure because it's silent. Mitigate with strict cache
> keys (hash *all* inputs, including tool versions), scoped/immutable cache entries, and the ability
> to bust the cache. Never key a cache on a mutable tag like `latest`.

**Parallelization** — do independent work at once:

- **Shard** big test suites across runners (duration-aware, per [5.1](01-design-a-test-automation-platform.md)).
- **Run independent stages concurrently** (lint ∥ unit) when the DAG allows.
- **Matrix builds:** the same suite across Chrome/Firefox/Safari or JDK 17/21 in parallel cells.

---

## Step 5: Test selection / impact analysis (the scale unlock)

Running all 100k tests on every one-line PR is wasteful and slow. **Test impact analysis (TIA)**
runs only the tests a change can actually affect.

- **Build a dependency graph:** map source files/modules → the tests that exercise them (from
  coverage data or static dependency analysis). A change to `PaymentService.java` maps to the ~300
  tests that touch payments, not the 100k full suite.
- **On a PR:** diff the changed files, walk the graph, run that subset (+ a small always-on smoke
  set). Full suite still runs on merge-to-main and nightly as a safety net.
- **Payoff:** ties directly to the [5.1 estimate](01-design-a-test-automation-platform.md) — the 2%
  of tests a typical PR needs turns a 10-minute gate into a 90-second one and slashes fleet cost.

> **Trade-off — selection can miss a regression.** The dependency graph is imperfect (reflection,
> config, data-driven coupling). A change might break a test TIA didn't select. Mitigate with a
> conservative graph (over-select when unsure), an always-on smoke suite, and the full suite on
> merge/nightly so nothing escapes for long. Google's TAP is built on exactly this bet: precise
> dependency selection + periodic full runs.

---

## Step 6: Quality gates

A **gate** is a pass/fail predicate that must be green to promote. Owned by SDET/quality.

- **Test gates:** unit/integration/e2e for this stage must pass (excluding *quarantined* flaky
  tests, which run but don't block — [5.5](05-flaky-test-detection-and-quarantine.md)).
- **Coverage gate:** e.g. "≥80% line coverage, and **no decrease** vs base branch." The
  *no-decrease* form is better than an absolute number — it prevents erosion without blocking on a
  legacy module nobody will backfill.
- **Flaky-budget gate:** if the run's flaky rate exceeds a budget (say >2% of tests flaked), fail
  or warn — a rising flake rate is a leading indicator of a rotting suite.
- **Security gates:** SAST/dependency-CVE scan (block on Critical), secret-scanning, license check.
- **Performance gate:** a smoke perf/load check — p99 latency or throughput must not regress beyond
  a threshold (ties to the framework's Gatling/performance module).

> **Trade-off — strict gates vs velocity.** Gates that are too strict (100% coverage, zero flakes)
> get bypassed with `--force` or "skip CI," destroying their value. Gates too loose let regressions
> through. Tune to *deltas* (no *decrease*, no *new* criticals) rather than absolutes, and make the
> failure message tell the engineer exactly what to fix.

---

## Step 7: Deployment strategies & rollback

Once gates pass, ship — progressively, with an automated safety net. (These build on
[3.5 Fault tolerance](../03-distributed-systems/05-fault-tolerance.md).)

**Rolling** — replace instances a few at a time. Simple, no extra capacity, but a bad version is
briefly live for some users and rollback is gradual.

**Blue-green** — run two full environments. `Blue` serves prod; deploy the new version to `green`,
smoke-test it, then **flip the load balancer** to `green`. Instant cutover, instant rollback (flip
back). Cost: 2× capacity during the switch.

```
   [ LB ] ──100%──► BLUE  (v1, live)         Step 1: deploy v2 to GREEN, smoke it
             ┄┄┄┄┄► GREEN (v2, warming)      Step 2: flip LB ──100%──► GREEN
                                             Rollback: flip back to BLUE (seconds)
```

**Canary** — release to a **small % of traffic** (1% → 5% → 25% → 100%), watching error rate,
latency, and business metrics at each step. If a metric regresses, **auto-rollback** and never
progress. Safest for large user bases; needs solid metrics and automated analysis (Argo Rollouts,
Spinnaker/Kayenta).

```
   v2 at 1% ──metrics OK?──► 5% ──OK?──► 25% ──OK?──► 100%
              │ regressed          │ regressed
              └───► auto-rollback ◄┘   (halt, drain v2, page on-call)
```

**Automated rollback** is the point: the pipeline watches health signals post-deploy and reverts to
the last-good artifact automatically, because it still exists in the registry (Step 3). Rollback
should be **faster and lower-risk than rolling forward** with a hotfix.

> **Trade-off.** Rolling = cheap, slow to roll back. Blue-green = instant flip, 2× cost, and
> stateful concerns (DB migrations must be backward-compatible across both colors). Canary = safest
> but needs mature observability and takes longer to reach 100%. Pick by blast-radius tolerance.

---

## Step 8: Secrets management

Pipelines need credentials (registry push, deploy keys, DB passwords for integration tests) —
and are a prime leak vector.

- **Never in the repo.** Use a secrets store (Vault, AWS Secrets Manager, cloud KMS) and inject at
  runtime as env vars scoped to the job.
- **Least privilege & short-lived:** prefer OIDC federation (the runner exchanges a short-lived
  token with the cloud, no long-lived static keys) over stored static credentials.
- **Mask in logs:** the CI system redacts known secret values from output; still, never `echo` them.
- **Scope by stage:** the unit-test stage doesn't need prod deploy keys. A compromised test job
  shouldn't be able to touch production. This is also **failure isolation** — a poisoned dependency
  in a PR build can't exfiltrate prod secrets it was never given.

---

## Step 9: Failure isolation & pipeline-as-code

**Failure isolation** — a failure should be *attributable and contained*:

- **Distinguish infra failure from test failure.** "Runner ran out of disk" is not "your code is
  broken." Classify exit reasons; auto-retry infra failures, never silently retry genuine test
  failures.
- **Quarantine flaky e2e** so one nondeterministic test doesn't red-X an otherwise-good pipeline.
- **Fail fast, report fully:** stop promoting on first gate failure, but still collect *all* results
  in that stage (don't abort the other shards) so the engineer sees every problem at once, not one
  per push.
- **Blast-radius containment:** per-team runner pools/quotas so Team A's flood or broken self-hosted
  runner can't starve Team B (multi-tenant fairness, per [5.1](01-design-a-test-automation-platform.md)).

**Pipeline-as-code** — the pipeline definition lives in the repo (`.github/workflows`,
`.gitlab-ci.yml`, `Jenkinsfile`), versioned with the code it builds.

> **Trade-off — pipeline-as-code vs central UI config.** *As-code:* versioned, reviewable, diffable,
> travels with the branch, reproducible — but logic sprawls into YAML that's hard to test and easy
> to duplicate across repos. *Central UI/config:* consistent and governable org-wide, but opaque,
> unversioned, and a single point of human error ("who changed the prod gate?"). Mature orgs do
> **both**: as-code per-repo pipelines that *inherit* from centrally-governed, versioned reusable
> templates (reusable workflows / shared libraries) so guardrails are enforced without copy-paste.

---

## Trade-offs & key takeaways

- CI/CD is a **DAG of stages, cheapest-first**, so regressions die before expensive stages run.
- **Build once, promote the immutable artifact** — the bytes you tested are the bytes you ship;
  rollback = redeploy the previous SHA.
- **Caching and parallelization** keep the pipeline fast; a poisoned cache is the silent killer —
  hash all inputs into the key.
- **Test impact analysis** runs the ~2% of tests a PR needs; full suite on merge/nightly catches
  what selection misses.
- **Quality gates** should test *deltas* (no coverage decrease, no new criticals, flake budget) not
  brittle absolutes, or people bypass them.
- **Canary/blue-green + automated rollback** make deploys boring; rolling forward a hotfix should
  never be the only recovery path.
- **Secrets:** least-privilege, short-lived (OIDC), scoped per stage, masked in logs.
- **Isolate failures:** separate infra flakiness from real failures; quarantine flaky tests;
  per-team quotas.

---

## In the wild

- **GitHub Actions / GitLab CI / Buildkite / CircleCI** — hosted pipeline engines with reusable
  workflows and matrix builds.
- **Google TAP** — dependency-based test selection over a giant monorepo + periodic full runs.
- **Netflix Spinnaker** and **Argo Rollouts/Kayenta** — canary analysis and progressive delivery.
- **Bazel** — hermetic, content-addressed builds with remote build+test caching (skip unchanged
  targets).
- **HashiCorp Vault** / cloud KMS + **OIDC federation** — short-lived pipeline credentials.

---

## SDET interview angle

The interviewer wants to see you own the **quality gates** and reason about **fast, trustworthy
feedback**, not just draw boxes. Emphasize the pyramid ordering ("why unit before e2e — economics"),
**build-once-promote**, **test impact analysis** for speed, and **canary + auto-rollback** for
safe delivery. Naming the caching-poisoning and gate-bypass trade-offs is a strong senior signal.

**Common follow-ups:**

- *"PR feedback takes 40 minutes; devs are ignoring CI."* → parallel shards + dependency cache +
  test impact analysis to run the relevant subset; full suite on merge.
- *"A flaky e2e keeps failing green PRs."* → quarantine (run, don't block) + flake budget gate.
- *"Staging passed but prod broke — same code, different behavior."* → build-once-promote; you
  probably rebuilt per environment or drifted config. Also canary would've caught it at 1%.
- *"How do you roll back fast?"* → previous immutable artifact is still in the registry; redeploy
  its SHA / flip blue-green — no rebuild.
- *"Where do secrets live?"* → external store + OIDC short-lived tokens, scoped per stage, masked.
- *"Coverage gate blocks a one-line hotfix on legacy code."* → gate on *no decrease*, not an
  absolute; separate legacy debt from new-code coverage.

---

## Practice / self-check

1. Why run unit tests before e2e? Put concrete cost/time numbers on why the inverse is wasteful.
2. Explain "build once, promote the artifact." What class of bug does rebuilding-per-environment
   introduce, and how does this rule make rollback trivial?
3. A cache turned a broken build green. How did that happen, and how do you key caches to prevent
   it?
4. Design a coverage gate that improves quality without blocking a hotfix to an untested legacy
   module.
5. Contrast canary, blue-green, and rolling deploys on cost, rollback speed, and blast radius. When
   would you pick each?
6. Test impact analysis skipped a test that a change actually broke. How is that possible, and what
   two safety nets keep the regression from reaching prod?
7. Argue both sides of pipeline-as-code vs central UI configuration, then propose a hybrid.

---

## How this shows up in an SDET loop

A very common design/deep-dive round for SDET and DevOps-leaning test roles. Interviewers probe the
**gate logic** (what blocks a merge and why), **feedback speed** (selection, caching, sharding), and
**safe delivery** (canary/rollback). They'll also ask you to debug a *specific* pipeline pain
(flaky gate, slow PR, prod-only bug) — the follow-ups above are the real questions. Show you think
in trade-offs and deltas, not rigid rules.

**Next:** [5.3 — Design Test Infrastructure at Scale »](03-design-test-infrastructure-at-scale.md)
