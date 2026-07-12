# 5.1 — Design a Test Automation Platform

> **Module 5 · SDET System Design** · ~35 min read
> *Concepts exercised:* job queueing, work sharding, result stores, artifact/blob storage, flaky
> detection, quarantine, dashboards, horizontal autoscaling, back-of-envelope capacity math.

---

## The problem

Your company has 100,000 automated tests — UI (Selenium/Playwright), API (RestAssured), and a
few thousand unit/integration suites. Today they run on one bloated Jenkins job that takes **4
hours**, flakes constantly, and nobody trusts the red X. Engineers merge on hope. When a test
fails, there's no screenshot, no video, no history — just a wall of stack traces someone has to
re-run locally to reproduce.

You've been asked to design the **internal test automation platform**: a service that teams call
to run tests at scale, get fast trustworthy results, store rich artifacts, and surface *why*
something failed — the same class of system that Google's TAP, Uber's test infra, and vendors
like BrowserStack/LambdaTest/CircleCI build.

> **Analogy.** Think of a busy restaurant kitchen. Orders (test runs) stream in. A single cook
> (one CI agent) is a bottleneck; you want a *line* of cooks (worker nodes) each handling part of
> the order in parallel (sharding), a ticket rail that tracks every dish (result store), a pass
> window with photos of plating gone wrong (artifacts), and a manager who spots the dish that
> comes back to the kitchen 30% of the time (flaky detection) and pulls it off the menu
> (quarantine) until the recipe is fixed.

This is fundamentally a **distributed job-processing system** with a test-domain flavor. If you've
read [2.10 Message Queues](../02-building-blocks/10-message-queues-streaming.md) and
[4.6 Notification System](../04-case-studies/06-notification-system/), much of the shape will feel
familiar — the SDET twist is in *test-awareness*: sharding by test, flake scoring, quarantine,
and artifact-heavy results.

---

## Step 1: Requirements (always start here)

**Functional**

- **Trigger a run** via API/CLI/CI webhook: "run suite X at commit `abc123` on Chrome 120."
- **Schedule & queue** runs; support priorities (a release-blocking run jumps a nightly).
- **Shard** a large suite across many worker nodes and run in parallel.
- **Collect results** per test: pass/fail/skip, duration, error, stack trace, retries.
- **Store artifacts**: screenshots, videos, HAR/network logs, Playwright/Selenium traces, logs.
- **Report**: a run summary + per-test drill-down, plus historical trends and dashboards.
- **Detect flaky tests** automatically and support **quarantine** (run but don't block).
- **Retry** policy (bounded) with clear signal distinguishing "passed on retry" from "passed."

**Non-functional**

- **Scale:** 100k tests, thousands of runs/day, spiky (everyone merges before lunch).
- **Latency of feedback:** a PR gate should finish in **≤10 min**, not hours. Wall-clock, not CPU.
- **Reliability:** a crashed worker must not lose a run; work is retried elsewhere.
- **Durability:** results + artifacts retained (hot 30–90 days, cold in object storage after).
- **Cost:** worker fleet is the dominant cost; scale to zero when idle.
- **Multi-tenancy:** many teams share the platform; one team's flood can't starve another.

**Explicitly out of scope (say this):** we're not writing the test *frameworks* — teams bring
their own (JUnit/TestNG/pytest/Playwright). We orchestrate, distribute, store, and report.

---

## Step 2: Back-of-the-envelope estimation

Let's size it. This is the part interviewers love and candidates skip.

**Volume**

- 100,000 tests in the full regression suite.
- Suppose **200 CI runs/day** trigger the *full* suite (releases, nightlies, big PRs), plus
  ~2,000 partial runs (per-PR, impacted subset ~2,000 tests each).
- Full-suite test executions/day ≈ `200 × 100k = 20,000,000`.
- Partial ≈ `2,000 × 2,000 = 4,000,000`.
- **Total ≈ 24M test executions/day** → `24M / 86,400s ≈ 278 test-starts/sec` average, but the
  peak is ~5× average (lunchtime merge storm) → **~1,400 test-starts/sec peak**.

**Shard / node math**

- Say each UI test averages **8 s** wall-clock (includes browser startup amortized), each API/unit
  test **0.3 s**. Assume the full suite is 30% UI (30k × 8s = 240,000 s) + 70% fast (70k × 0.3s =
  21,000 s) = **261,000 test-seconds ≈ 72.5 test-hours per full run**.
- Serial, that's 72 hours — unacceptable. Target wall-clock: **10 minutes** = 600 s.
- Shards needed ≈ `261,000 s / 600 s ≈ 435 parallel shards`. Round to **~450 workers** for one
  full run to finish in 10 min.
- If each worker node runs **4 concurrent browser sessions** (4 vCPU, ~2 GB each), you need
  `450 / 4 ≈ 113 nodes` for a single full run. For a handful of concurrent full runs + PR traffic
  at peak, plan a fleet that autoscales to **~300–500 nodes** and back to near-zero overnight.

**Storage (artifacts dominate)**

- Screenshot ≈ 200 KB; capture on failure only. Video (UI, on fail) ≈ 5 MB for a 30 s clip; trace
  (Playwright) ≈ 2 MB. Logs ≈ 50 KB/test.
- Failures are the expensive ones. Assume **2% fail** and capture full artifacts on those:
  `24M × 2% = 480,000 failing executions/day`.
- Per failure ≈ 200 KB + 5 MB + 2 MB + 50 KB ≈ **~7.25 MB**.
- Daily artifact volume ≈ `480k × 7.25 MB ≈ 3.5 TB/day`.
- At 30-day hot retention that's ~**105 TB** hot; push older to cold object storage (S3
  IA/Glacier) — the classic tiering from [4.16 Object Storage](../04-case-studies/16-object-storage/).
- **Result metadata** (rows in a DB): 24M rows/day × ~1 KB ≈ **24 GB/day** of structured data —
  small, but it's your query hot path, so index it well.

**Takeaway numbers to say out loud:** ~24M executions/day, ~1,400 starts/sec peak, ~450 shards to
hit a 10-min full run, ~3.5 TB/day of artifacts. These drive every design choice below.

---

## Step 3: High-level architecture

```
   ┌──────────┐   POST /runs        ┌─────────────┐
   │  CI /    │ ──────────────────► │  API /      │   validate, auth, resolve suite
   │  CLI /   │                     │  Trigger    │──► split into SHARDS
   │  Webhook │ ◄──────────────────  │  Service    │      (test list → N buckets)
   └──────────┘   runId, status      └──────┬──────┘
                                            │ enqueue shard jobs
                                            ▼
                                   ┌──────────────────┐
                                   │  Scheduler /     │  priority, fairness,
                                   │  Job Queue       │  per-tenant quotas
                                   │  (Kafka/SQS)     │
                                   └────────┬─────────┘
                          pull shard jobs   │
              ┌───────────────┬─────────────┼──────────────┐
              ▼               ▼             ▼               ▼
        ┌─────────┐    ┌─────────┐   ┌─────────┐     ┌─────────┐
        │ Worker  │    │ Worker  │   │ Worker  │ ... │ Worker  │   (autoscaled fleet)
        │ (runs   │    │ (runs   │   │ (runs   │     │ (runs   │
        │ tests)  │    │ tests)  │   │ tests)  │     │ tests)  │
        └────┬────┘    └────┬────┘   └────┬────┘     └────┬────┘
             │ results      │             │ artifacts     │
             ▼              ▼             ▼               ▼
     ┌────────────────┐            ┌──────────────────┐
     │ Result Store   │            │ Artifact Store   │  (S3/GCS + CDN)
     │ (Postgres +    │            │ screenshots,     │
     │  OLAP/ES)      │            │ video, traces    │
     └───────┬────────┘            └────────┬─────────┘
             │                              │ presigned URLs
             ▼                              ▼
     ┌──────────────────────────────────────────────┐
     │  Reporting / Dashboard / Flake-Detection Svc  │
     └──────────────────────────────────────────────┘
```

Components, one at a time.

### 3.1 Trigger / API service

The front door. Exposes a small REST API:

```
POST /v1/runs
  { "suite": "checkout-regression", "commit": "abc123",
    "browser": "chrome:120", "shards": "auto", "priority": "gate" }
  → 202 { "runId": "run_9f3", "status": "queued" }

GET  /v1/runs/{runId}            → status, pass/fail counts, links
GET  /v1/runs/{runId}/tests      → per-test results (paginated)
GET  /v1/tests/{testId}/history  → flake trend for one test
```

It authenticates (service tokens per team), enforces **per-tenant rate limits/quotas** (see
[2.11 Rate limiting](../02-building-blocks/11-rate-limiting.md)), resolves the suite to a concrete
**test list** at that commit, and creates a `run` record. It is **stateless** and horizontally
scaled behind a load balancer. Design it **idempotent**: the same `(commit, suite, CI-build-id)`
should return the existing `runId`, not spawn a duplicate — reuse the idempotency-key pattern from
[2.14 Idempotency](../02-building-blocks/14-idempotency.md).

### 3.2 Sharding / test splitting

The core scaling move: **split N tests into K shards** and run shards in parallel. Naive
round-robin (`test i → shard i % K`) is easy but ignores that tests have wildly different
durations — one shard may get all the 60-second UI tests and become the straggler that defines
wall-clock time.

Better: **duration-aware bin-packing** using historical per-test timings from the result store.
Sort tests by expected duration descending, greedily assign each to the currently-shortest shard
(LPT — longest-processing-time-first). This keeps shard finish-times within a few percent of each
other and minimizes the tail.

```
   tests (with historical durations)      3 shards, LPT packing
   [60,55,40,30,20,10,8,5]      ──►   S1: 60 + 10       = 70
                                       S2: 55 + 8 + 5    = 68
                                       S3: 40 + 30 + 20  = 90  ← straggler; rebalance
```

Dynamic alternative: a **work-stealing queue** — instead of pre-assigning, put every test (or
small batch) on a queue and let idle workers pull the next one. Self-balancing, no historical data
needed, naturally handles a worker dying mid-shard. Cost: more coordination chatter and you lose
neat "this shard = this worker" locality. Most mature platforms use **hybrid**: coarse pre-shard
for locality + a shared queue tail for stragglers.

> **Trade-off.** Static bin-packing gives predictable, cache-friendly shards but needs good timing
> data and rebalances poorly when a test suddenly slows. Dynamic work-stealing is robust and
> self-balancing but chattier and harder to attribute ("which worker ran test X?"). State the
> pick and why.

### 3.3 Scheduler & job queue

Shard jobs land on a durable queue (Kafka, SQS, or Redis Streams). The scheduler adds:

- **Priority lanes:** `gate` (release-blocking PR) > `nightly` > `experimental`. A gate run must
  not wait behind a 4-hour nightly.
- **Fairness / multi-tenancy:** weighted fair queuing so Team A's 500-run flood doesn't starve
  Team B. Per-tenant concurrency caps.
- **Visibility timeout / lease:** a worker leases a shard; if it doesn't ack completion within a
  timeout (crash, OOM), the shard is redelivered to another worker. This is what makes worker
  death survivable — the same at-least-once delivery semantics from
  [4.13 Message Queue](../04-case-studies/13-message-queue/). Because redelivery means a shard can
  run twice, **test execution must be idempotent** in the result store (upsert by
  `(runId, testId, attempt)`).

### 3.4 Worker fleet

Stateless workers (containers on Kubernetes / ECS / a VM autoscaling group). Each worker:

1. Pulls a shard job (list of test IDs + commit + environment spec).
2. Checks out/pulls the test code artifact at that commit (from a cache — see below).
3. Spins up the runtime (browser via [5.3 test infra](03-design-test-infrastructure-at-scale.md),
   or a JVM/pytest process).
4. Runs each test, streaming results and uploading artifacts as it goes.
5. Acks the shard; the queue releases the lease.

**Autoscaling signal:** queue depth / oldest-message-age, *not* CPU. If shards are waiting, add
workers; when the queue drains, scale toward zero. Use **spot/preemptible** instances for the bulk
(tests are retryable, so a reclaimed node just redelivers its shard) and a small on-demand baseline
for latency-critical gate runs.

### 3.5 Result store

Two shapes of query, so often **two stores**:

- **Transactional / recent:** Postgres (or MySQL) holds `runs`, `test_results`, `attempts`. Serves
  "show me run_9f3" and powers the gate decision. Partition `test_results` by time; it grows ~24M
  rows/day.
- **Analytical / historical:** an OLAP store (ClickHouse, BigQuery) or search index
  (Elasticsearch/OpenSearch) for "flake rate of test X over 30 days," "slowest 100 tests,"
  full-text over error messages. Feed it from the transactional store via CDC or a stream.

Minimal schema:

```
runs(run_id PK, suite, commit, branch, trigger, status, started_at, finished_at,
     total, passed, failed, flaky, quarantined)

test_results(run_id, test_id, status, duration_ms, attempts, final_status,
             error_signature, artifact_bucket_key, PRIMARY KEY(run_id, test_id))

test_history(test_id, day, runs, fails, flakes, flake_score)   -- rolled up nightly
```

`error_signature` = a normalized hash of the error (stack top-frames + message with IDs/timestamps
stripped). It's the join key that clusters "the same failure" across runs — essential for flake
detection and for not paging ten people about one root cause.

### 3.6 Artifact storage

Artifacts are **big and write-once, read-rarely** — a textbook object-storage workload, not a DB.

- Workers upload directly to **S3/GCS** using **presigned URLs** (the API service hands the worker
  a short-lived upload URL; bytes never transit your API tier). Store only the *key* in the result
  DB.
- **Key layout:** `s3://ci-artifacts/{yyyy}/{mm}/{dd}/{runId}/{testId}/{video|trace|shot}.{ext}` —
  date-prefixed for cheap lifecycle rules.
- **Lifecycle tiering:** hot (Standard) 30 days → Infrequent-Access 90 days → Glacier/delete.
  Governs the 105 TB hot figure from the estimate.
- **Serve via CDN** with presigned read URLs so the dashboard loads a failure video fast without
  proxying terabytes through your app (see [2.2 Reverse proxy & CDN](../02-building-blocks/02-reverse-proxy-cdn.md)).
- **Capture policy is a cost lever:** always capturing video for 24M executions would be ~120 TB/day.
  Capturing on **failure + first-retry only** cuts that ~50×. Say this trade-off explicitly.

### 3.7 Reporting & dashboards

- **Run view:** pass/fail/flaky/quarantined counts, duration, the straggler shard, links to the
  worst failures with inline screenshots/video.
- **Test view:** history sparkline, flake score, owning team, last N failures grouped by
  `error_signature`.
- **Trends:** suite duration over time (catch the slow creep), flake budget burn-down, most-flaky
  and slowest tests (the backlog for the quality team).
- Real-time updates while a run is in flight (SSE/WebSocket, per
  [1.5 realtime](../01-networking-and-communication/05-polling-sse-websockets.md)).

### 3.8 Flaky detection & quarantine (SDET-specific)

A **flaky test** passes and fails on the *same code* — nondeterministic. It's the single biggest
threat to trust in a test platform: engineers learn to ignore red, and then a *real* red slips
through. This gets a full lesson ([5.5](05-flaky-test-detection-and-quarantine.md)); the platform
hooks are:

- **Retry-signature detection:** if a test fails then passes on retry at the same commit, flag it
  as *flaky-this-run* (don't just silently green it — record `final_status=passed_on_retry`).
- **Statistical flake score:** over a rolling window, `flake_score = flip_rate` (transitions
  between pass/fail on unchanged code). Above a threshold (say, fails on >5% of runs where code
  didn't change) → mark flaky.
- **Quarantine:** flaky tests still *run* (so we keep collecting signal and know when they're fixed)
  but their result **does not block** the gate. They surface on a "quarantine" dashboard with an
  owner and an auto-filed ticket. Auto-un-quarantine after N consecutive green runs.

> **Trade-off — retries hide bugs.** Blindly retrying until green makes the suite "pass" but
> masks genuine race conditions in the *product*. Bound retries (≤2), always *record* that a retry
> happened, and treat a rising `passed_on_retry` rate as a defect signal — not a success metric.

---

## Step 4: Scaling to 100k tests — the failure modes

- **The straggler shard.** Wall-clock = the slowest shard. Duration-aware packing + a work-stealing
  tail + splitting any single test that alone exceeds the target shard time.
- **Queue backpressure at peak.** 1,400 starts/sec at lunch. Autoscale on queue age; use priority
  lanes so gates stay fast; shed/deprioritize `experimental` runs first.
- **Worker crashes / spot reclaim.** Lease + visibility timeout redelivers the shard. Idempotent
  upserts make re-execution safe.
- **Artifact write storms.** Direct-to-S3 presigned uploads keep bytes off your API tier; S3
  scales horizontally by key prefix.
- **Result-store hot partition.** Time-partition `test_results`; keep the OLAP copy for heavy
  analytics so dashboards don't hammer the transactional DB.
- **Poison test that hangs a worker.** Per-test and per-shard **timeouts**; kill and record as
  `timed_out`, release the worker.
- **Thundering herd on shared test data.** Covered in [5.4 Design for testability](04-design-for-testability.md)
  — seed per-shard data namespaces so parallel shards don't collide.

---

## Trade-offs & key takeaways

- A test platform is a **distributed job system** (queue + workers + result store + blob store) with
  three SDET-specific organs: **sharding by test**, **flake detection**, and **quarantine**.
- **Wall-clock feedback time is the product.** Everything — sharding, autoscaling, priority lanes —
  serves the "≤10-minute trustworthy result" goal.
- **Shard by historical duration**, not test count, and keep a work-stealing tail for stragglers.
- **Separate structured results (DB) from artifacts (object storage + CDN).** Never store a video
  blob in Postgres.
- **Capture artifacts on failure/first-retry only** — capture policy is your biggest storage lever.
- **Leases + idempotent upserts** make worker death and spot reclaim survivable.
- **Retries can mask product bugs** — bound them, always record them, watch the trend.

---

## In the wild

- **Google TAP + Forge** run millions of test targets/day with aggressive dependency-based test
  selection (only run what a change affects — see [5.2](02-design-a-ci-cd-pipeline.md)).
- **Uber, Meta, LinkedIn** publish about internal flaky-test detection and auto-quarantine at scale.
- **BrowserStack / LambdaTest / Sauce Labs / CircleCI** sell the worker fleet + browser farm +
  artifact/reporting layers as a product; **Testkube / Argo Workflows** do Kubernetes-native test
  orchestration.
- **Playwright/Cypress dashboards** and **Allure / ReportPortal** cover the reporting + flake
  analytics slice.

---

## SDET interview angle

This is *the* flagship SDET system-design question. Interviewers want to see you (a) recognize it's
a distributed job system, (b) do the **shard math** out loud, and (c) go deep on the test-specific
parts — flake detection, quarantine, artifact strategy — that a pure backend candidate would miss.

**Drive it in this order:** requirements → estimate (shard count!) → queue+workers+stores diagram →
sharding strategy → flake/quarantine → failure modes. Lead the estimate with "how many shards to
hit a 10-minute full run" — that single calculation signals seniority.

**Common follow-ups:**

- *"A full run takes 4 hours. Get it under 10 minutes."* → shard math: `total_test_seconds /
  target_wall_clock = shards`; then autoscale workers to supply them.
- *"How do you keep one flaky test from blocking every release?"* → quarantine: run but don't gate,
  with an owner and auto-un-quarantine.
- *"A worker dies mid-shard — do we lose those results?"* → lease/visibility timeout redelivers;
  idempotent upserts make re-run safe.
- *"Storage cost is exploding."* → capture-on-failure policy, lifecycle tiering to cold storage,
  direct-to-S3 uploads.
- *"How do you decide which tests to run for a PR?"* → test impact analysis / selection — hand off
  to [5.2](02-design-a-ci-cd-pipeline.md).

---

## Practice / self-check

1. A full suite is 261,000 test-seconds. You want a 12-minute wall clock. How many parallel shards?
   If each worker runs 5 sessions, how many nodes?
2. Round-robin sharding gives you a straggler shard that runs 3× longer than the others. Why, and
   what two techniques fix it?
3. Why store artifacts in object storage + CDN instead of the result database? What key layout makes
   lifecycle tiering cheap?
4. A worker is reclaimed (spot) halfway through its shard. Trace exactly what happens so no results
   are lost and nothing is double-counted.
5. Explain the difference between `passed`, `passed_on_retry`, and `flaky` in your result schema.
   Why is a rising `passed_on_retry` rate a warning sign about the *product*, not just the tests?
6. Your capture-video-always policy produces 120 TB/day. Propose a policy that cuts it ~50× and say
   what you lose.

---

## How this shows up in an SDET loop

Expect this as the **60-minute system-design round** for senior/staff SDET and "Test Engineering
Infrastructure" roles at FAANG-tier companies. The bar is: treat it as real distributed systems
(queues, leases, autoscaling, blob storage) *and* show test-domain judgment (sharding by duration,
flake budgets, quarantine, artifact economics). Weaker candidates design a fancy Jenkins; stronger
ones design a job platform and name the trade-offs.

**Next:** [5.2 — Design a CI/CD Pipeline »](02-design-a-ci-cd-pipeline.md)
