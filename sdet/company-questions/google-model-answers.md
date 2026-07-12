# Google — SDET / QA Model Answers

> **Companion to [`google.md`](google.md)** (the prompt-only bank). These are model answers written at the depth of the [JioStar worked deep-dive](jiostar-hotstar-framework-round.md).
> **Focus:** Scale + correctness, deep test-design thinking, distributed systems.
> Behavioral answers use a **STAR skeleton with a worked example** — treat the example as a template and swap in your own real story.
>
> Cross-references: runnable framework code in [`framework/`](../../framework/), the Playwright TS framework in [`playwright/`](../../playwright/), practical SDET problems in [`sdet/`](../../sdet/), and the System Design course in [`sd/`](../../sd/).

---

## Table of Contents

1. [Testing Fundamentals](#testing-fundamentals)
2. [Automation & Frameworks](#automation--frameworks)
3. [API Testing](#api-testing)
4. [Database / Data Testing](#database--data-testing)
5. [System Design & Quality Strategy](#system-design--quality-strategy)
6. [Performance & Reliability](#performance--reliability)
7. [Domain-Specific](#domain-specific)
8. [Situational / Behavioral](#situational--behavioral)

---

## Testing Fundamentals

### Q1: How would you test Google Search to ensure users receive relevant and accurate results?

Google Search has no single "correct" output, so I test it along **properties and invariants** rather than exact-match assertions.

- **Functional correctness:** query returns a non-empty result set within SLA; the results page renders (organic results, snippets, knowledge panel, pagination); special query types resolve (calculator `2+2`, unit conversion, weather, sports scores, spelling correction "recieve" → "receive").
- **Relevance (the hard part):** use a **golden set** of curated query→expected-top-results pairs, human-rated relevance judgments (the classic search-quality rater approach), and **metrics** like NDCG, MRR, and precision@k rather than "does result #1 equal X." I'd assert that a known navigational query ("facebook") returns the official domain in the top result, and that a known-answer factual query returns the right entity in the knowledge panel.
- **Robustness:** empty query, very long query, non-ASCII/RTL scripts, emoji, injection-looking strings, misspellings, and zero-result queries all degrade gracefully.
- **Non-functional:** latency (P50/P95/P99), personalization vs. incognito consistency, localization (google.co.in vs .com), and safe-search filtering.

The senior insight: for ML/relevance systems you validate **statistical quality over a distribution of queries**, not deterministic per-query equality. See the ranking question in [System Design](#q26-how-would-you-validate-fairness-correctness-and-reliability-of-a-machine-learning-based-ranking-system) for the fairness/correctness angle.

### Q2: What test cases would you design for Google's autocomplete suggestions?

Autocomplete is a low-latency prediction feature, so I test **behavior, quality, and safety**:

- **Functional:** typing "weath" surfaces relevant suggestions; suggestions update on each keystroke; selecting a suggestion (click / arrow-key + Enter) performs the search; ESC dismisses; the dropdown caps at N suggestions.
- **Latency:** suggestions appear within a tight budget (~100ms) and don't lag behind fast typing — debouncing must not drop the final keystroke.
- **Correctness/relevance:** prefix match ("new y" → "new york"), trending/personalized ordering, spelling tolerance.
- **Safety & policy (critical at Google):** offensive, hateful, dangerous, or defamatory suggestions must be filtered; PII must not be auto-suggested. This is a compliance requirement, so it gets its own suppression test suite.
- **i18n/edge:** IME input for CJK languages, RTL, emoji, single character, whitespace-only, extremely fast backspacing.
- **Accessibility:** ARIA combobox roles, screen-reader announcements, full keyboard navigation.

### Q3: How would you test Gmail's email compose and send functionality?

I'd structure this as a state machine over the compose lifecycle:

- **Compose basics:** To/Cc/Bcc validation, subject, rich-text body (bold, lists, links), plain-text toggle, multiple recipients, contact autocomplete.
- **Attachments:** add/remove, size limit enforcement (25MB → Drive link fallback), disallowed types, multiple attachments, inline images.
- **Drafts & autosave:** draft persists on navigate-away and browser crash; autosave fires periodically; editing from two tabs resolves consistently.
- **Send paths:** send, send + schedule, undo-send window, send to invalid address (bounce handling), send with empty subject/body (warning).
- **Delivery verification (the SDET part):** don't stop at "the UI said sent." Verify via the Gmail API / IMAP that the message landed in Sent, arrived in the recipient's inbox, headers/threading are correct, and the body/attachments are intact. UI + backend assertion is what separates an SDET answer from a manual-QA answer.
- **Cross-cutting:** offline compose then sync, spam/phishing classification, i18n subject lines, threading/reply-all, undo-send.

### Q4: How would you validate correctness of search ranking results without knowing the exact expected output?

This is the core Google test-design question. You can't assert `result[0] == expected`, so validate with **oracles other than exact equality**:

1. **Property-based / metamorphic testing** — relationships that must hold regardless of the exact ranking:
   - Adding a more specific term should not make a clearly-relevant page disappear.
   - Query "A B" and "B A" for order-insensitive intents should return largely overlapping top-k (Jaccard overlap above a threshold).
   - A duplicate query in the same session returns a stable ordering.
2. **Golden set + human judgments** — a curated set of queries with rater-labeled relevance grades; compute **NDCG@k, MRR, precision@k** and gate on regression against a baseline, not absolute values.
3. **Invariants** — no duplicate URLs, no broken/blocked results in top-k, safe-search removes flagged content, official sources rank for navigational queries.
4. **A/B and interleaving in production** — the ultimate oracle for relevance is user behavior (click-through, dwell time, abandonment). Offline metrics gate the launch; online interleaving experiments confirm real improvement.

The framing to give the interviewer: **"With no deterministic oracle, I move from equality assertions to statistical quality gates and metamorphic invariants, and I treat human relevance judgments as the ground truth."**

---

## Automation & Frameworks

### Q5: How would you decide which Google Search test cases should be automated?

I use an ROI model, not "automate everything." Automate when **(value of catching the bug) × (run frequency) > (build + maintenance cost)**:

- **Automate:** high-value stable paths run every commit — core query→results, latency SLO checks, API contract tests, the safe-search suppression suite, autocomplete smoke, and the golden-set relevance regression. These are deterministic, high-frequency, and expensive to miss.
- **Don't automate (or defer):** one-off exploratory checks, rapidly-churning experimental UI, subjective visual/relevance judgments best done by human raters, and anything where the locator/DOM changes daily (bad ROI).
- **Decision factors:** stability of the feature, frequency of execution, business risk, determinism of the oracle, and maintenance cost. The `Q5` reasoning in [`jiostar`](jiostar-hotstar-framework-round.md) Q14.x on flaky tests reinforces this: automate the stable core, keep the churny edge manual until it settles.

### Q6: How would you design automation for a feature that changes frequently due to experimentation?

Frequent A/B experimentation is the norm at Google, so I design for **variant-awareness and resilience**:

1. **Pin the variant** — set the experiment/feature-flag deterministically for the test (cookie, header, or an internal override endpoint) so the test isn't at the mercy of random bucketing. This is exactly the feature-flag override pattern in [`jiostar`](jiostar-hotstar-framework-round.md) Q15.5.
2. **Test the contract, not the pixels** — assert on stable `data-testid` hooks and behavioral outcomes ("clicking play starts playback"), not on CSS/layout that changes per experiment. Request `data-testid` from devs.
3. **Parameterize across variants** — a data-driven test that runs the same assertions for each active variant.
4. **Decouple layers** — push most coverage to API/contract level (stable) and keep only a thin UI E2E layer (churny).
5. **Self-healing / fast-fail locators** — centralize locators so a UI change is a one-line fix; add visual-diff tests that are *reviewed*, not hard-failed, during heavy experimentation.
6. **Quarantine lane** — new experimental UI tests run in a non-blocking lane until the feature stabilizes, then promote to the gating suite.

### Q7: How would you ensure your automation suite remains reliable as the application scales?

Reliability at scale = attacking flakiness systematically and keeping the pyramid healthy:

- **Isolation:** each test owns its data and state (fresh session per test, like `@BeforeMethod` in the framework's [`BaseTest`](../../framework/)), no ordering dependencies, ThreadLocal driver isolation for parallel runs.
- **Synchronization:** explicit/fluent waits only, never `Thread.sleep()` — see [`jiostar`](jiostar-hotstar-framework-round.md) Q7.1.
- **Flake tracking:** every test emits a pass/fail history; a flake-rate dashboard flags tests failing >X% and auto-quarantines them so they don't block the pipeline while still being visible for repair. Retry (bounded, tracked) absorbs true transients but never masks a >10%-flaky test.
- **Test data:** synthetic, hermetic data via factories/builders; service virtualization (WireMock) for unstable downstreams so tests don't depend on a live dependency's health.
- **Pyramid discipline:** as the app grows, aggressively shift coverage down to unit/API so the E2E count stays small and stable.
- **Ownership & SLOs:** each suite has an owner and a "green rate" SLO; the platform team monitors infra flakiness (grid capacity, network) separately from product flakiness.

### Q8: How would you design a test infrastructure capable of executing hundreds of thousands of tests daily? (Senior)

This is a platform-design answer. Google's real answer is TAP/Forge-style; I'd describe the same principles:

**Requirements:** 100k+ tests/day, fast feedback (<10 min for presubmit), reliable, cost-efficient, multi-team.

**Architecture:**
```
Code change → CI trigger → Test Impact Analysis (which tests are affected?)
   → Build graph (Bazel-style, cache hits skip unchanged targets)
   → Distributed test runner (sharded across a large worker pool / K8s)
   → Result aggregation → dashboards + flake detection → gating decision
```

**Key decisions:**
1. **Test Impact Analysis** — never run all tests on every change; use the dependency graph to run only affected tests presubmit, run the full suite continuously in the background. This is the single biggest lever.
2. **Hermetic, cacheable builds** — Bazel-style remote build cache so unchanged targets are never rebuilt/retested; content-addressable caching gives massive speedups.
3. **Massive sharding** — split tests into shards executed in parallel across a worker farm; target a fixed wall-clock budget by scaling shard count.
4. **Tiered suites** — presubmit (fast, must pass to merge) vs. postsubmit/continuous (full, finds regressions) vs. nightly (slow, cross-browser, load).
5. **Flakiness as a first-class metric** — automated flake detection reruns and quarantines; a flaky test can't block a merge but is tracked to an owner.
6. **Result store + dashboards** — a central results DB (like ResultStore) for history, trends, and bisection to find the culprit change.
7. **Elastic infra** — containerized runners on autoscaling K8s; Selenium Grid 4 / cloud device farms for browser and device coverage.

The trade-off to name: **latency vs. cost vs. coverage** — TIA + caching buys latency and cost; the continuous full run buys coverage. See [`sd/`](../../sd/) for the distributed-systems building blocks.

---

## API Testing

### Q9: How would you validate an API that returns search results for a query?

- **Status & schema:** 200 for valid query; JSON schema validation on the response (result array, each item has url/title/snippet/rank) — the schema-validation approach from [`jiostar`](jiostar-hotstar-framework-round.md) Q9.2. `additionalProperties: false` to catch leaked fields.
- **Correctness:** result count respects the `limit`/pagination params; `offset`/`page` returns disjoint, ordered results; relevance sanity via a golden query (navigational query returns known domain in top-k).
- **Parameters:** query encoding (spaces, unicode, `+`, `&`), filters (language, safe-search, time range), sort options.
- **Negative/edge:** empty query (400 or empty result set per contract), overly long query, missing required params, malformed params → correct error codes and messages.
- **Non-functional:** latency SLO, rate-limiting (429 with Retry-After), auth/quota enforcement, caching headers/ETags.
- **Contract:** version the response; run consumer-driven contract tests so a backend change that breaks the schema fails before release.

Practical RestAssured patterns live in [`sdet/src/main/java/ra/hul/sdet/api/`](../../sdet/) and the framework's [`api/client`](../../framework/).

### Q10: How would you test an API that aggregates data from multiple backend services?

An aggregator's risk is **partial failure and fan-out consistency**:

1. **Happy path:** all downstreams healthy → aggregated response is complete and correctly merged/deduplicated.
2. **Partial failure (the key scenarios) — use service virtualization (WireMock):**
   - One downstream is **slow** → does the aggregator time out that call and return partial data, or block? Verify the timeout budget and graceful degradation.
   - One downstream **errors (500)** → does the aggregator return a partial result with a clear indicator, or fail the whole request? Assert the documented behavior.
   - One downstream returns **empty/malformed** → aggregator handles it without crashing.
3. **Consistency:** if two downstreams report overlapping data, dedup/merge rules are correct and deterministic.
4. **Performance:** aggregate latency ≈ max(downstream latencies) if parallelized (not the sum) — a fan-out that's serial is a bug.
5. **Resilience patterns:** verify circuit-breaker, retry-with-backoff, and fallback/cached responses. This mirrors [`jiostar`](jiostar-hotstar-framework-round.md) Q16.4 microservices testing and the WireMock stubbing pattern there.

### Q11: How would you validate correctness of a recommendation API serving personalized content?

Personalization has no fixed oracle, so I combine deterministic and statistical checks:

- **Structural:** schema valid, correct number of recommendations, no duplicates, no already-consumed/blocked items, respects `limit`.
- **Personalization signal:** two clearly-different user profiles get **meaningfully different** result sets (assert on divergence, not exact contents); a fresh/cold-start user gets sensible popular defaults; opting out returns non-personalized results.
- **Business rules:** no age-restricted content for minors, no out-of-region content, diversity constraints (not all from one category), freshness.
- **Metamorphic:** a user who "likes" item X shifts recommendations toward X's category (directional assertion).
- **Statistical/offline metrics:** precision@k, recall, coverage, diversity over a labeled dataset — gate on regression vs. baseline.
- **Stability & latency:** same user + same context returns stable results within a session; SLO on P95.

The through-line, same as search ranking (Q4): **validate distributions and invariants, not exact equality.**

### Q12: How would you test reliability of an API deployed across multiple geographic regions? (Senior)

Multi-region reliability testing covers **routing, consistency, and failover**:

1. **Geo-routing:** requests from region R hit the nearest region (verify via response headers / latency); latency SLO met per region; DNS/GeoDNS/anycast routes correctly.
2. **Data consistency:** if the API is backed by replicated storage, test read-after-write and eventual-consistency windows across regions (see [Q17](#q17-how-would-you-validate-eventual-consistency-in-a-distributed-storage-system)).
3. **Failover:** take a region offline (chaos) → traffic reroutes to the next region with no user-visible errors; measure failover time and error budget consumed; verify no split-brain.
4. **Correctness under partition:** simulate a network partition between regions → the API stays available per its CAP posture (AP vs CP) and reconciles when healed.
5. **Config drift:** all regions run the same version/config (a common cause of region-specific bugs); canary a new version region-by-region.
6. **Monitoring:** synthetic probes from every region continuously; per-region SLO dashboards and error-budget alerting.

Tooling: multi-region synthetic monitoring, chaos engineering (fault injection), and load from distributed geos. Building blocks in [`sd/03-distributed-systems`](../../sd/) and [`sd/04-case-studies`](../../sd/).

---

## Database / Data Testing

### Q13: How would you verify that search analytics data is being stored correctly?

End-to-end from event emission to storage:

- **Event capture:** perform a known search → verify an analytics event is emitted with correct schema (query, timestamp, user/session id, result count, latency).
- **Pipeline integrity:** the event flows through the ingestion pipeline (e.g., Kafka → processor → warehouse) without loss — reconcile counts at each stage (produced == consumed == stored).
- **Storage correctness:** query the analytics store (BigQuery/warehouse) and assert the row matches the emitted event; timestamps in correct timezone; no truncation of long queries; PII is redacted/hashed per policy.
- **Aggregations:** daily counts, top-queries rollups match a recomputation from raw events (sample-based reconciliation).
- **Edge cases:** late-arriving events, duplicate events (dedup), out-of-order events, schema evolution (new field added) don't break the pipeline.

### Q14: How would you investigate inconsistent reporting data between two systems?

A systematic reconciliation investigation:

1. **Reproduce & quantify** — pin a specific metric, time window, and dimension where the two systems disagree; quantify the delta (is it 0.1% or 40%?).
2. **Define the source of truth** — which system is authoritative? Reconcile both against raw events if possible.
3. **Bisect the pipeline** — compare counts at each stage: ingestion → transformation → storage → reporting. The stage where numbers diverge localizes the bug.
4. **Common root causes:** different timezone/day boundaries, different dedup logic, different filters (bots excluded in one system not the other), late/dropped events, join fan-out/duplication, sampling in one system, or a schema/units mismatch.
5. **Fix & guard:** once found, add a **continuous reconciliation check** (automated daily diff with tolerance) so the divergence is caught immediately next time.

The senior move is turning a one-off investigation into a **standing data-quality assertion**.

### Q15: How would you validate user activity metrics generated from billions of events?

At this scale you can't check every row, so you validate with **statistical and reconciliation techniques**:

- **Reconciliation:** produced == processed == stored counts at pipeline boundaries (with tolerance for in-flight); use idempotency keys to detect double-counting.
- **Sampling:** deep-verify a statistically significant random sample end-to-end (raw event → final metric).
- **Invariants & bounds:** metrics can't be negative, daily active ≤ monthly active, sum of segments == total, day-over-day change within expected band (anomaly detection flags spikes/drops).
- **Golden datasets:** replay a known synthetic event stream with a known correct answer and assert the pipeline computes it exactly.
- **Deduplication & late data:** verify windowing, watermarks, and dedup logic under out-of-order and duplicate events (Flink/Kafka-Streams semantics).
- **Cross-check:** compare against an independent aggregation path (e.g., a batch recompute vs. the streaming metric) — they should converge.

Data-processing practice code is in [`sdet/src/main/java/ra/hul/sdet/dataprocessing/`](../../sdet/).

---

## System Design & Quality Strategy

### Q16: How would you design a testing strategy for Google Search indexing billions of web pages? (Senior)

I'd decompose the crawl→index→serve pipeline and test each stage plus end-to-end:

**Pipeline:** Crawler → Parser/Renderer → Indexer → Index storage (sharded) → Serving.

**Strategy per stage:**
- **Crawler:** respects robots.txt, crawl-rate limits, dedup of URLs, handles redirects/errors/traps (infinite calendars), politeness, freshness (recrawl cadence by page importance).
- **Parser/Renderer:** correctly extracts content from HTML/JS-rendered pages, handles malformed HTML, extracts links/metadata, canonicalization.
- **Indexer:** every crawled+eligible page becomes findable; tokenization/normalization correct; spam/policy filtering applied; no data loss (reconcile crawled count vs. indexed count).
- **Index storage:** sharding correctness, replication, no lost documents, consistency across replicas.
- **Serving:** a page known to be indexed is retrievable by a targeted query (freshness SLA from crawl → searchable).

**Cross-cutting:**
- **Freshness pipeline test:** publish a known page → measure time until it appears in results.
- **Scale/reconciliation:** count invariants across stages (crawled ≥ indexed, with documented drop reasons).
- **Quality gates:** golden-set relevance metrics before shipping ranking/index changes.
- **Chaos:** shard failure → serving degrades gracefully, no full-index outage.

**Test pyramid framing:** unit tests per component, integration tests per stage boundary, a small set of true E2E "publish→search" tests, plus continuous production monitoring of freshness and coverage.

### Q17: How would you test a distributed caching system used by millions of users? (Senior)

Caches are about **correctness, consistency, and behavior under pressure**:

- **Correctness:** get-after-set returns the value; TTL expiry works; delete/invalidate removes it; cache-aside/read-through/write-through semantics behave per design.
- **Consistency with source of truth:** on DB update, cache invalidation/refresh happens — no stale reads beyond the allowed window; test the classic **read-your-writes** and **thundering-herd on cache miss** (many concurrent misses shouldn't stampede the DB — verify request coalescing / single-flight).
- **Eviction:** under memory pressure the eviction policy (LRU/LFU) behaves; hot keys stay, cold keys evicted; no crash on full cache.
- **Distribution:** consistent hashing distributes keys evenly; adding/removing a node reshards with minimal key movement and no data loss; hot-key/hot-shard handling.
- **Failure modes:** a cache node dies → reads fall through to DB (degraded latency, correct results), not errors; cache unavailable entirely → system still serves from source; split-brain avoided.
- **Performance:** hit ratio under realistic access patterns, P99 latency, throughput ceiling, behavior at cache-cold start (cold cache stampede).

See the distributed cache and consistent-hashing material in [`sd/02-building-blocks`](../../sd/) and [`sd/04-case-studies`](../../sd/).

### Q18: How would you validate eventual consistency in a distributed storage system? (Senior)

Eventual consistency means "reads converge after writes stop," so I test the **convergence window and anomalies**:

1. **Convergence:** write to node A, read from node B repeatedly → measure time-to-converge; assert it's within the documented staleness bound.
2. **Read-your-writes / monotonic reads:** if the system claims session guarantees, verify a client that wrote sees its own write, and never sees data go "backwards."
3. **Conflict resolution:** concurrent writes to the same key from different nodes → verify the resolution rule (last-write-wins by timestamp, vector clocks, CRDT merge) produces the correct, deterministic result and no lost update silently disappears without the documented policy.
4. **Partition healing:** partition the cluster, write on both sides, heal → verify reconciliation matches the conflict policy (no split-brain divergence remains).
5. **Anti-entropy/repair:** read-repair and background sync eventually make all replicas identical — verify with a full-replica diff after quiescence.
6. **Anomaly probes:** continuous "canary key" writer/reader that alerts if staleness exceeds the SLO in production.

Frame it explicitly in **CAP/PACELC** terms and test to the system's stated guarantees, not to strong-consistency expectations. Consistency models are covered in [`sd/03-distributed-systems`](../../sd/).

### Q19: How would you define a quality strategy for Google Search from crawling to ranking and result delivery? (Architect)

A quality strategy is broader than tests — it's **how the org guarantees quality across the lifecycle**:

**1. Shift-left (prevent):** testability requirements in design (feature flags, override hooks, observability); unit + component tests owned by dev teams; contract tests at every service boundary; code review + static analysis.

**2. Pipeline (per stage):** the crawl→index→serve coverage from [Q16](#q16-how-would-you-design-a-testing-strategy-for-google-search-indexing-billions-of-web-pages-senior), with reconciliation invariants between stages.

**3. Relevance quality (the differentiator):** golden-set offline metrics (NDCG/MRR) as launch gates; human rater judgments as ground truth; **online A/B + interleaving experiments** as the final oracle; guardrail metrics so a ranking win doesn't regress latency or safety.

**4. Non-functional gates:** latency SLOs (P50/P95/P99), five-nines availability, capacity/load testing for peak QPS, safe-search/policy suppression suites.

**5. Release safety:** canary → gradual rollout → automated rollback on guardrail breach; feature flags for instant kill-switch.

**6. Production quality (can't test everything pre-prod at this scale):** synthetic monitoring (queries from every geo), real-user monitoring, freshness probes, anomaly detection, error budgets, and blameless postmortems feeding back into tests.

**7. Ownership model:** dev teams own component quality; a central QE/SRE function owns cross-cutting E2E, quality dashboards, and release gates.

The architect-level point: at Google's scale you **shift quality left (prevention) and right (production monitoring)** because exhaustive pre-prod testing is impossible — you gate on statistical quality and catch the rest in production with fast rollback.

---

## Performance & Reliability

### Q20: How would you test a service expected to process millions of requests per second? (Senior)

**1. Model the load:** derive a realistic traffic profile (RPS, request mix, payload sizes, key distribution incl. hot keys, geographic spread) from production telemetry — don't test a uniform synthetic load that hides hot-shard problems.

**2. Test types:**
- **Load test:** sustained expected peak → verify latency SLOs (P50/P95/P99) and error rate hold.
- **Stress test:** ramp past peak to find the breaking point and confirm graceful degradation (shed load, return 429, not fall over).
- **Soak test:** hours at high load → catch memory leaks, connection-pool exhaustion, GC pathologies.
- **Spike test:** instantaneous surge (Prime-Day-style) → autoscaling reacts in time, no cascading failure.

**3. Tooling:** Gatling / k6 / distributed load generators (a single box can't produce millions of RPS — generate from a fleet). The framework has Gatling simulations in [`framework/src/main/java/ra/hul/framework/performance/simulations/`](../../framework/).

**4. What to measure:** throughput ceiling, latency percentiles under load, error rate, resource saturation (CPU/mem/network/connection pools), and downstream impact (does load on this service starve its dependencies?).

**5. Resilience:** combine with chaos — kill instances mid-load and verify SLOs hold; verify autoscaling, circuit breakers, and backpressure.

Key nuance: **tail latency (P99+) matters more than average** at scale, and you must test with a realistic key distribution or you'll miss hot-shard bottlenecks.

### Q21: How would you measure and improve quality for a globally distributed service with five-nines availability requirements? (Architect)

Five-nines = ~5.26 minutes downtime/year, so quality is measured in **SLIs/SLOs and error budgets**, and improved via engineering discipline:

**Measure:**
- Define **SLIs** (availability, latency, correctness/freshness) and **SLOs** per SLI; track the **error budget** (1 − SLO). Burn-rate alerting.
- Multi-region synthetic + real-user monitoring; per-region and global dashboards.
- Track MTTD (detect) and MTTR (recover) — at five-nines, recovery speed dominates.

**Improve:**
- **Redundancy:** multi-region active-active, no single point of failure, N+1 capacity.
- **Progressive delivery:** canary + gradual rollout + automated rollback; feature flags as kill switches (most outages are self-inflicted by deploys).
- **Chaos engineering:** continuously inject failures (instance/AZ/region loss, latency, packet loss) in production-like environments to prove resilience before real failures happen.
- **Fast recovery:** automated failover, runbooks, one-click rollback; practice with game days.
- **Error-budget policy:** when the budget is exhausted, freeze feature launches and spend on reliability — this aligns product and reliability incentives.
- **Blameless postmortems:** every incident produces action items and new tests/monitors.

The architect framing: **you don't achieve five-nines by testing harder pre-prod; you achieve it with redundancy, progressive delivery, fast automated recovery, and error-budget-driven prioritization.** SRE material and case studies in [`sd/`](../../sd/).

---

## Domain-Specific

### Q22: How would you test Google Drive file synchronization across multiple devices?

Sync is a distributed-consistency problem in disguise:

- **Basic sync:** create/edit/delete/rename/move on device A propagates to device B within SLA; folder hierarchy preserved.
- **Conflict resolution:** edit the same file offline on two devices, both reconnect → verify the conflict policy (both-copies / last-writer-wins / merge) — no silent data loss.
- **Offline → online:** queue changes offline, reconnect → all changes sync in correct order.
- **Partial/interrupted sync:** kill the network mid-upload → resume without corruption or duplicate.
- **Large/edge files:** very large files, many small files, special characters in names, deep nesting, near-quota, zero-byte files.
- **Multi-device consistency:** N devices editing → all converge to identical state (eventual consistency, [Q18](#q18-how-would-you-validate-eventual-consistency-in-a-distributed-storage-system) techniques).
- **Integrity:** checksums match after sync; no truncation; permissions/sharing state syncs correctly.

### Q23: How would you test YouTube video uploads across different devices and network conditions?

- **Upload correctness:** various formats/codecs/resolutions/durations; verify transcoding produces all target renditions; thumbnail/metadata/captions processed.
- **Network conditions (use CDP / device-farm network shaping):** slow 3G, high latency, packet loss, mid-upload disconnect → **resumable upload** picks up where it left off (no restart from zero), no corruption.
- **Device matrix:** Android/iOS/web/TV, low-end vs high-end devices, background upload, app-killed-mid-upload recovery.
- **Edge cases:** upload cancel, quota/size limits, duplicate upload, unsupported format (clear error), simultaneous multi-file upload.
- **Post-upload:** video is playable in all renditions, ABR switching works, DRM if applicable, correct privacy setting applied on publish.
- **Performance:** time-to-upload and time-to-processing SLAs.

The network-throttling and ABR/playback techniques mirror [`jiostar`](jiostar-hotstar-framework-round.md) Q15.1 and Q17.3 (CDP network emulation).

### Q24: How would you validate data replication across multiple data centers? (Senior)

- **Replication completeness:** every write to the primary appears in every replica DC (reconcile row/object counts and checksums per DC).
- **Replication lag:** measure write→visible-in-replica latency; assert within SLO; alert on lag spikes (a growing lag is an early outage signal).
- **Consistency model:** test to the stated guarantee — strong (synchronous, read any DC returns latest) vs. eventual (bounded staleness). Read-your-writes across DCs if promised.
- **Conflict handling:** concurrent writes in two DCs → correct resolution ([Q18](#q18-how-would-you-validate-eventual-consistency-in-a-distributed-storage-system)).
- **Failover/DR:** primary DC dies → a replica is promoted, no data loss beyond RPO, recovery within RTO; failback works after the DC returns.
- **Partition:** DCs can't talk → each behaves per CAP posture; heal → reconcile with no divergence.
- **Integrity:** periodic full-replica diff / anti-entropy verification.

### Q25 (also Q26): How would you validate fairness, correctness, and reliability of a machine-learning-based ranking system? (Architect)

ML systems need testing dimensions beyond traditional software:

**Correctness:**
- Offline evaluation on a held-out labeled set: NDCG/MRR/precision@k vs. baseline (gate on no regression).
- Metamorphic tests: perturbing an input in a known direction changes the score in the expected direction.
- Golden queries with known-correct top results as smoke tests.

**Fairness (increasingly a hard requirement):**
- **Disparate-impact analysis:** does ranking quality differ across protected groups (demographics, geography, language)? Compute per-group metrics and assert parity within tolerance.
- **Representation/exposure:** doesn't systematically under-rank content from certain groups; test with counterfactual inputs (swap a sensitive attribute, ranking shouldn't change if it shouldn't matter).
- **Feedback-loop guard:** monitor for the model amplifying its own biases over time (popularity → more exposure → more popularity).

**Reliability:**
- **Serving:** latency SLO for inference at scale; graceful fallback to a simpler model / cached ranking if the model service is down.
- **Data/skew:** training-serving skew detection (features computed identically in both); input data validation (schema, ranges, missing-feature handling).
- **Model quality monitoring in prod:** drift detection (input distribution shift), online metrics (CTR, dwell) vs. offline predictions, A/B before full rollout.
- **Reproducibility:** versioned model + data + features so a result can be reproduced and a regression bisected.

**Strategy:** ML quality is a **continuous loop** — offline gates → shadow/A-B → prod monitoring → retrain — not a one-time test pass. The fairness/bias dimension is what elevates this to architect level. (This is the same statistical-oracle philosophy as [Q4](#q4-how-would-you-validate-correctness-of-search-ranking-results-without-knowing-the-exact-expected-output).)

---

## Situational / Behavioral

> **Format:** STAR (Situation, Task, Action, Result). The examples below are **templates** — replace with your own real story. Interviewers probe for specifics, so keep concrete numbers and your personal actions ("I did," not "we did").

### Q27: Tell me about a time when you found a defect that significantly impacted users.

- **S:** On a streaming/checkout feature (use your real product), a change to the session-handling logic was scheduled for release.
- **T:** As the SDET on the release, I owned the go/no-go quality signal.
- **A:** During exploratory testing across regions, I noticed intermittent 401s only when the auth token refreshed near expiry under a specific timezone offset — a race in token refresh. I reproduced it deterministically by pinning the clock near expiry, captured logs/HAR proving the root cause, filed a P1 with a minimal repro, and paired with the dev to confirm the fix.
- **R:** We caught it pre-release; based on traffic it would have logged out ~X% of users during peak. I then added an automated regression test around token-refresh-near-expiry so it can never regress silently.

*Pick a story where you found it, proved it, quantified impact, and closed the loop with a regression test.*

### Q28: Tell me about a time when requirements were ambiguous and you had to define the testing strategy.

- **S:** A new feature (e.g., a recommendations widget) arrived with a one-line spec and no acceptance criteria.
- **T:** I had to define what "working correctly" even meant before I could test it.
- **A:** I ran a clarification pass with PM/dev (drafted the acceptance criteria myself and got sign-off), identified the risky areas (personalization correctness, latency, empty-state), chose oracles for the non-deterministic parts (invariants + metrics rather than exact match — same reasoning as [Q4](#q4-how-would-you-validate-correctness-of-search-ranking-results-without-knowing-the-exact-expected-output)), and built a layered test plan (API/contract first, thin UI E2E).
- **R:** The written acceptance criteria became the shared source of truth; we shipped with a clear quality bar and caught two spec gaps before code was written.

### Q29: Tell me about a time when you improved test coverage without significantly increasing execution time.

- **S:** The regression suite was UI-heavy and slow (say 45 min) with coverage gaps at the API layer.
- **T:** Increase meaningful coverage without blowing the time budget.
- **A:** I applied the test pyramid — moved a set of validations from slow UI E2E down to fast API/contract tests, replaced redundant UI paths with parameterized data-driven cases, and parallelized. I also removed duplicate tests covering the same path (measured via coverage overlap).
- **R:** Coverage of edge cases went up materially while wall-clock time dropped (e.g., 45 → 12 min) because the new coverage lived at a faster layer. The [`jiostar`](jiostar-hotstar-framework-round.md) Q16.3 shift-left reasoning is exactly this.

### Q30: Tell me about a time when you had to make a difficult trade-off between quality and delivery timelines. (Senior)

- **S:** A launch date was fixed, but full regression + a known non-blocking flaky area couldn't complete in time.
- **T:** Decide what quality signal was sufficient to ship responsibly.
- **A:** I did **risk-based prioritization**: ran the full smoke + high-risk regression (payment/auth/data-integrity), consciously deferred low-risk cosmetic areas, and documented the residual risk explicitly for the release decision. I proposed a feature flag so we could ship dark and enable gradually with production monitoring as the safety net.
- **R:** We hit the date with the critical paths fully validated; the flag + monitoring caught one minor issue in canary before full rollout. The key is I **made the trade-off explicit and data-informed**, not silent.

### Q31: Tell me about a time when you influenced engineering teams to improve quality without having direct authority over them. (Architect)

- **S:** Multiple teams owned services in a flow; flaky cross-team E2E tests and missing contract tests caused frequent integration breakages, but I owned none of those teams.
- **T:** Drive a quality improvement across teams through influence, not mandate.
- **A:** I led with **data** — built a dashboard showing integration failures traced to missing contracts and their cost in engineer-hours. I ran a lightweight proof-of-concept (introduced consumer-driven contract tests on one boundary, showed it caught a real break), then socialized the results, wrote the shared standard, and offered to pair with teams to adopt it. I made adoption easy (templates, CI integration) rather than demanding it.
- **R:** Contract tests were adopted across the key boundaries; cross-team integration breakages dropped measurably. The lesson: **influence at scale comes from data + a working example + reducing adoption friction**, not authority.

---

_Model answers for interview prep. Adapt the behavioral examples to your own experience — interviewers reward specificity and honesty._
