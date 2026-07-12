# Meta — SDET / QA Model Answers

> **Companion to [`meta.md`](meta.md)** (the prompt-only bank). Model answers at the depth of the [JioStar worked deep-dive](jiostar-hotstar-framework-round.md).
> **Focus:** Product sense, hands-on coding, mobile + web automation.
> Behavioral answers use a **STAR skeleton with a worked example** — swap in your own real stories.
>
> Cross-references: [`framework/`](../../framework/) (Selenium/RestAssured/Appium/Gatling — note the `mobile/` Appium module), [`playwright/`](../../playwright/) (TS web+API), [`sdet/`](../../sdet/) (practical problems), [`sd/`](../../sd/) (System Design).

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

### Q1: How would you test Facebook Login functionality across web and mobile platforms?

Login is the front door — high traffic, high security, cross-platform. Meta cares about **product sense** here, so I frame by user + platform:

- **Functional (all platforms):** valid credentials → logged in; invalid password → correct error; non-existent user; empty fields; case-sensitivity of email; "remember me"/persistent session; logout.
- **Auth methods:** email/phone/username login, third-party/SSO, passwordless, 2FA/MFA, account recovery, biometric on mobile (Face ID/fingerprint).
- **Cross-platform parity:** the same account logs in on web, iOS, Android; session established correctly on each; **session continuity** (log in on web, open app — consistent state).
- **Security:** rate-limiting/lockout on repeated failures, no credential leakage in logs/URLs, secure token storage, session expiry/refresh, logout invalidates the session everywhere.
- **Edge/robustness:** slow network during login, mid-login app-kill recovery, expired session refresh, concurrent logins on multiple devices, suspicious-login challenge.
- **Automation:** log in once via API + inject session/token for tests not about login (fast, less flaky — [`jiostar`](jiostar-hotstar-framework-round.md) Q16.3); reserve UI login tests for validating the flow itself. Cross-platform via Selenium (web) + Appium (mobile) — framework has both.

### Q2: What test scenarios would you create for posting a status update?

Product-sense driven, covering the full compose→publish→display lifecycle:

- **Content types:** text only, text + photo(s), video, link (with preview unfurl), emoji, mentions (@friend), hashtags, feeling/activity, location, poll, GIF.
- **Validation:** empty post blocked, character limit, very long post, unicode/RTL/emoji, malicious input (XSS/script sanitized), disallowed content flagged.
- **Privacy/audience (Meta-critical):** post to Public/Friends/Only-me/Custom → verify **only** the intended audience can see it; changing audience after posting; tagged-people visibility.
- **Publish paths:** post now, schedule, save draft, edit after posting (edit history), delete.
- **Display:** appears correctly in own timeline and friends' feeds; media renders; link preview correct; counts (likes/comments) start at zero.
- **Edge:** post during network loss (queue + retry), duplicate-submit prevention, posting while rate-limited.

The privacy/audience dimension is the "product sense" signal — always call it out for a social product.

### Q3: How would you test Facebook Messenger's message delivery functionality?

Messaging is a distributed, real-time delivery problem:

- **Delivery correctness:** message sent → delivered to recipient → read receipts update through states (sending → sent → delivered → read); ordering preserved; no duplicates; no loss.
- **Real-time:** message arrives near-instantly when recipient is online (push/WebSocket); typing indicators; presence (online/last-active).
- **Offline/async:** recipient offline → message queued → delivered on reconnect + push notification; sender offline → message queued locally, sent on reconnect (correct order).
- **Content types:** text, emoji/reactions, images, video, voice notes, files, GIFs, links, replies/threads.
- **Group chats:** delivery to all members, member add/remove, correct per-member read state.
- **Edge:** send to a user who blocked you, deleted account, unsend/delete-for-everyone, network flap mid-send (no dup), multi-device sync (read on phone → read on web).
- **Reliability at scale:** see [Q18](#q18-how-would-you-validate-performance-of-a-real-time-messaging-system-at-global-scale). Verify delivery via backend/API, not just the UI.

### Q4: How would you validate News Feed ranking without knowing the exact expected order?

No deterministic oracle (identical to the Google search-ranking problem) — validate with **invariants, metamorphic tests, and metrics**:

1. **Invariants:** feed contains no duplicate posts; no blocked/unfollowed/hidden content; no content from users you've blocked; respects "see first" preferences; policy-violating content filtered.
2. **Metamorphic/directional:** interacting more with a friend surfaces their content higher over time; hiding a post reduces similar content; freshness — very old already-seen posts rank below new ones.
3. **Personalization:** two different users get meaningfully different feeds; a fresh user gets sensible defaults (cold start).
4. **Offline quality metrics:** on a labeled/engagement dataset compute ranking quality (NDCG-style, engagement prediction accuracy) and gate on no regression vs. baseline.
5. **Online A/B + guardrails:** the real oracle is user engagement (meaningful interactions, dwell, complaints) via interleaving/A-B experiments, with guardrails (no drop in integrity metrics like misinformation exposure).

Framing: **move from equality to statistical quality gates + invariants + human/behavioral ground truth.** Fairness/integrity dimension in [Q16](#q16-how-would-you-validate-correctness-fairness-and-reliability-of-a-machine-learning-driven-feed-ranking-system-architect).

---

## Automation & Frameworks

### Q5: Which parts of the Facebook News Feed would you prioritize for automation and why?

ROI-driven selection:

- **Automate (high value, stable-enough):** feed loads and renders, infinite scroll/pagination, core interactions (like, comment, share, react) at the **API level**, privacy/audience enforcement (critical + stable), notification triggers, and the feed-quality invariants from [Q4](#q4-how-would-you-validate-news-feed-ranking-without-knowing-the-exact-expected-order).
- **Automate at API/contract layer** (not UI): ranking-service contracts, engagement counters, content-fetch APIs — stable and fast.
- **Don't over-automate the UI:** the exact visual feed layout and rapidly-changing experimental components churn constantly — keep UI E2E thin (smoke + critical journeys), use visual-diff tests that are *reviewed* not hard-failed.
- **Keep manual/exploratory:** subjective ranking-quality judgment, brand-new experimental UI until it stabilizes.

Rationale: automate the **stable, high-frequency, high-risk** parts (privacy, delivery, counts); keep the **churny presentation** layer light. This is the same ROI logic as [`jiostar`](jiostar-hotstar-framework-round.md) Q14.

### Q6: How would you design automation for frequently changing UI components like social feeds?

Feeds change constantly (experiments, dynamic content), so design for **resilience** (mirrors [`jiostar`](jiostar-hotstar-framework-round.md) Q15.5 flags + Google's experimentation answer):

1. **Test behavior, not pixels:** assert on stable `data-testid`/accessibility hooks and outcomes ("like increments the count"), never on CSS/DOM structure or exact ordering.
2. **Pin variants/flags:** deterministically set the experiment bucket (cookie/header/internal override) so the test isn't randomized by A/B assignment.
3. **Dynamic-content tolerance:** feeds have unpredictable content — assert on *properties* (a post has an author, timestamp, actions) and *invariants* (no duplicates), not specific posts. Seed known test content where possible.
4. **Layered:** push counters/ranking/delivery logic to API/contract tests (stable); thin UI E2E.
5. **Centralized locators / Page Objects** so a UI change is a one-line fix — framework [`web/pages`](../../framework/).
6. **Quarantine lane** for experimental-component tests until they stabilize, then promote to gating.

### Q7: How would you ensure reliability of automation tests in a rapidly evolving product?

Reliability under rapid change = flake control + fast maintenance (see [`jiostar`](jiostar-hotstar-framework-round.md) Q14.2):

- **Isolation:** fresh session/state per test, unique test data (builders), ThreadLocal driver isolation, no ordering deps.
- **Synchronization:** auto/explicit waits only, never sleeps; Playwright's web-first assertions and auto-waiting reduce timing flakes (see [`playwright/`](../../playwright/)).
- **Stable selectors:** `data-testid` requested from devs; centralized so changes are cheap.
- **Service virtualization:** WireMock for unstable downstreams so a dependency's hiccup doesn't fail your test.
- **Flake dashboard + quarantine:** auto-quarantine tests failing >X%; bounded, tracked retries for true transients; fix root cause for anything chronically flaky.
- **Fast triage:** traces/screenshots/logs on every failure (trace viewer / Extent) to make maintenance quick.
- **Pyramid discipline:** as the product evolves, keep pushing coverage down so the fragile E2E surface stays small.

### Q8: How would you design a test infrastructure supporting thousands of mobile and web tests daily? (Senior)

**Requirements:** thousands of mobile + web tests/day, fast feedback, reliable, device coverage.

**Architecture:**
```
Test code (POM/Screens + API + data builders)
  → Parallel runner (TestNG parallel / Playwright workers)
  → ThreadLocal driver isolation
  → Web: Selenium Grid 4 (autoscaling K8s) / cloud browsers
  → Mobile: Appium + device farm (BrowserStack/SauceLabs/self-hosted STF)
  → Test impact analysis + sharding
  → Result store + dashboards + flake detection
  → CI gating (presubmit fast / continuous full)
```

**Key decisions:**
1. **Unified abstraction, platform-specific drivers:** shared business tests run via Web (Selenium) or Mobile (Appium) drivers — the framework's `web/` and `mobile/` modules do exactly this; `MobileElement implements WebElement` so base actions are shared ([`jiostar`](jiostar-hotstar-framework-round.md) Q1.2).
2. **Device farm for scale:** real + emulated devices; parallel across device configs; a device-selection strategy (cover top devices by usage, not all).
3. **ThreadLocal + independent tests** for safe parallelism — [`jiostar`](jiostar-hotstar-framework-round.md) Q3.
4. **Test impact analysis + sharding** to hit a wall-clock budget without running everything.
5. **Flake management** (quarantine, dashboards) — mobile is inherently flakier, so this is essential.
6. **Config-driven** platform/device/env selection via system properties.

**Trade-off:** real-device fidelity (flakier, slower, costlier) vs. emulator speed — I'd run smoke on emulators every commit and the critical suite on real devices for top device configs.

---

## API Testing

### Q9: How would you validate an API responsible for creating social media posts?

- **Status & schema:** 201 on create; schema validation on the created-post response (id, author, content, timestamp, audience, media refs); `additionalProperties:false`.
- **Correctness:** created post persisted correctly (reconcile via GET / DB); audience/privacy field honored; media attachments linked; mentions/hashtags parsed.
- **Validation/negative:** empty content, over-limit content, invalid media ref, missing auth (401), posting as another user (403), malformed payload (400), XSS/script content sanitized.
- **Idempotency:** duplicate-submit (same client token) doesn't create two posts.
- **Privacy (critical):** the audience field actually restricts visibility — verify via a second user's fetch API that a Friends-only post is not returned to a non-friend.
- **Non-functional:** latency SLO, rate limiting (429), pagination on list endpoints.

RestAssured patterns: [`sdet/.../api/`](../../sdet/) and framework [`api/client`](../../framework/).

### Q10: How would you test APIs supporting likes, comments, and reactions?

Engagement APIs are **counter + concurrency** heavy:

- **Correctness:** like increments count by exactly 1; unlike decrements; a user can like only once (idempotent — second like doesn't double-count); reactions (love/haha/wow) change type, not add duplicates; comment creates a comment and increments the count.
- **Concurrency (key):** N users liking the same post simultaneously → count == N exactly (no lost updates); rapid like/unlike toggling settles to the correct state. Test with parallel requests — [`sdet/.../multithreading/`](../../sdet/).
- **Consistency:** count shown == actual number of like records (reconcile); counters (often eventually consistent/cached) converge within the staleness window.
- **Edge:** like a deleted post, comment on a post you can't see (privacy), self-like, reaction on your own content, blocked-user interaction, unicode/emoji comments.
- **Notifications:** liking triggers a notification to the author ([Q11](#q11-how-would-you-validate-that-notifications-are-delivered-correctly-to-millions-of-users)).
- **Non-functional:** these are hot endpoints — latency SLO and rate limiting.

### Q11: How would you validate that notifications are delivered correctly to millions of users?

Notification delivery = fan-out at massive scale:

- **Correctness:** the right event triggers the right notification to the right user(s) (like → author, comment → author + thread participants, mention → mentioned user); content/deep-link correct.
- **Fan-out:** a post by a user with millions of followers — verify the fan-out delivers to all (or the pull-model works), no duplicates, no misdelivery to wrong users.
- **Dedup & batching:** 100 likes in a minute → batched ("X and 99 others"), not 100 separate notifications; dedup on retries.
- **Multi-channel/multi-device:** push (iOS APNs / Android FCM), in-app, email — respecting user preferences; delivered to all the user's devices; read-state syncs across devices.
- **Timing/ordering:** delivered within SLA; reasonable ordering; respects quiet hours / DND settings.
- **Reliability:** offline user gets the notification on reconnect; delivery failures retried; no lost notifications (reconcile events → delivered).
- **Scale/perf:** load test the fan-out pipeline (Kafka → workers → push gateways) — see [Q12](#q12-how-would-you-test-reliability-of-notification-delivery-services-across-regions).

### Q12: How would you test reliability of notification delivery services across regions? (Senior)

Multi-region delivery reliability:

1. **Geo-routing & latency:** users served from the nearest region; delivery SLA met per region.
2. **No loss/dup across regions:** an event in region A delivers exactly once to a user currently in region B (idempotent consumers, dedup keys); reconcile produced vs. delivered.
3. **Failover:** a region/notification-worker outage → traffic reroutes, queued events replay on recovery with no lost or duplicate notifications; measure failover time.
4. **Partition:** event bus partitioned between regions → notifications queue and eventually deliver (bounded delay), no permanent loss.
5. **Ordering under failover:** re-delivery after failure preserves acceptable ordering / dedups correctly.
6. **Load per region:** each region handles its peak; global fan-out events (viral post) don't overwhelm one region.
7. **Monitoring:** synthetic notification probes per region; delivery-rate and latency dashboards; alert on delivery-rate drop.

Techniques: chaos/fault injection, service virtualization for push gateways, and reconciliation. Building blocks in [`sd/03-distributed-systems`](../../sd/).

---

## Database / Data Testing

### Q13: How would you verify that a user's profile information is correctly stored and retrieved?

- **Round-trip:** update profile fields (name, bio, DOB, email, phone, avatar, privacy settings) via UI/API → query DB → assert exact persistence (no truncation, correct encoding for unicode/emoji, correct types).
- **Retrieval consistency:** GET returns what was stored; cached reads match DB after an update (cache invalidation); different surfaces (web/app) show the same data.
- **Privacy enforcement:** fields marked private aren't returned to non-authorized viewers (verify via another user's fetch).
- **Integrity:** referential integrity (profile ↔ user id), audit/history if versioned, no orphan records.
- **Edge:** very long bio, special characters, empty optional fields, concurrent profile edits (last-writer-wins or conflict handling).

SQL/JDBC practice: [`sdet/.../database/`](../../sdet/).

### Q14: How would you investigate discrepancies between analytics dashboards and backend data?

Systematic reconciliation (same method as Google's [Q14](google-model-answers.md#q14-how-would-you-investigate-inconsistent-reporting-data-between-two-systems)):

1. **Pin** a specific metric + time window + dimension where dashboard ≠ backend; quantify the delta.
2. **Establish source of truth** (raw events) and reconcile both against it.
3. **Bisect the pipeline:** event emission → ingestion (Kafka) → stream/batch processing → warehouse → dashboard query. Compare counts at each boundary to localize the divergence.
4. **Common causes:** timezone/day-boundary differences, dedup logic mismatch, bot/filter differences, sampling in one path, late/dropped events, join fan-out double-counting, caching in the dashboard, or metric-definition mismatch (DAU defined differently).
5. **Fix + guard:** add a continuous reconciliation check (daily diff with tolerance) so it's caught immediately next time.

### Q15: How would you validate data consistency between user actions and reporting systems?

Ensuring every user action is faithfully reflected in reporting:

- **Event integrity:** a user action (like, post, click) emits exactly one correctly-schematized event (no loss, no dup — idempotency keys).
- **Pipeline reconciliation:** produced == processed == stored counts at each stage; sample-based deep verification (raw action → final report).
- **Invariants:** reported totals are non-negative, monotonic where expected, segment sums == total, DAU ≤ MAU.
- **Late/out-of-order:** windowing/watermarks handle late events; dedup handles duplicates (Flink/Kafka-Streams semantics).
- **Golden dataset:** replay a known synthetic action stream with a known-correct report and assert exact computation.
- **Cross-check:** independent batch recompute vs. streaming metric should converge.

Data-processing practice: [`sdet/.../dataprocessing/`](../../sdet/).

---

## System Design & Quality Strategy

### Q16: How would you design a testing strategy for Facebook News Feed serving billions of users? (Senior)

Feed = ranking service + content fetch + delivery at extreme scale.

- **Ranking quality:** invariants + metamorphic + offline metrics + online A/B, per [Q4](#q4-how-would-you-validate-news-feed-ranking-without-knowing-the-exact-expected-order).
- **Content correctness:** feed shows only permitted content (privacy/audience honored, blocked/hidden filtered, integrity/policy filtering applied) — this is a P0 correctness suite.
- **Freshness/consistency:** new posts appear within SLA; counts (likes/comments) eventually consistent within bound; cross-device consistency.
- **Contract tests** between feed service and its dependencies (ranking, content store, ads, integrity) so integration breaks are caught pre-E2E.
- **Scale/perf:** load test feed generation at peak QPS with realistic user graphs (celebrity/high-fanout users are hot spots).
- **Resilience:** ranking-service degradation → fall back to a simpler chronological/cached feed, never a blank feed; component failures degrade gracefully.
- **Integrity guardrails:** misinformation/harmful-content exposure metrics as launch guardrails.
- **Production:** real-user monitoring, A/B with guardrails, anomaly detection on engagement + integrity metrics.

Pyramid: heavy unit + API/contract, thin E2E, plus continuous production experimentation and monitoring.

### Q17: How would you test a feature rollout being released gradually to different user segments? (Senior)

Gradual rollout (a Meta specialty) = feature-flag + segment correctness + safety:

1. **Flag correctness:** users in the target segment get the feature; users outside don't; the flag can be toggled instantly (kill switch); default (flag off) behavior unchanged.
2. **Segment targeting:** correct users are bucketed (by geo, cohort, %); a user's assignment is **sticky** (doesn't flip mid-session); no leakage across segments.
3. **Both variants pass:** parameterized tests run the full suite for flag-on and flag-off ([`jiostar`](jiostar-hotstar-framework-round.md) Q15.5).
4. **Interaction/regression:** the new feature doesn't break existing functionality for users who don't have it; interactions between overlapping flags are sane.
5. **Rollout mechanics:** ramp 1% → 5% → 25% → 100% with automated guardrail monitoring (error rate, engagement, crash rate); automated rollback on breach.
6. **Metrics/experiment integrity:** the A/B measurement fires for the correct variant; assignment consistent for analysis.
7. **Data compatibility:** if the feature writes new data, old clients (flag off) still read/write correctly (forward/backward compatibility).

### Q18: How would you validate consistency of user data replicated across multiple data centers? (Senior)

(Meta runs a heavily replicated, region-aware data layer — TAO-style.)

- **Replication completeness:** every write to the primary appears in all replica DCs; reconcile counts/checksums per DC.
- **Replication lag:** measure write→visible-in-replica latency; assert within SLO; a growing lag is an early-warning signal.
- **Consistency model:** test to the stated guarantee — read-your-writes for the writing user (often via sticky routing to the primary or a cache), bounded staleness for cross-region reads.
- **Cache coherence:** the read cache (TAO-like) is invalidated on write; no stale reads beyond the window; the thundering-herd/cache-miss path is correct.
- **Conflict handling:** concurrent cross-DC writes resolve per policy (last-writer-wins/versioning), no silent lost update.
- **Failover/DR:** primary DC loss → replica promoted, no data loss beyond RPO, recovery within RTO.
- **Partition healing:** DCs reconnect → reconcile with no permanent divergence (anti-entropy).

Consistency models: [`sd/03-distributed-systems`](../../sd/). Same core techniques as eventual-consistency testing.

### Q19: How would you validate correctness, fairness, and reliability of a machine-learning-driven feed ranking system? (Architect)

ML feed ranking needs testing beyond traditional software (parallels [Google's ML-ranking answer](google-model-answers.md#q25-also-q26-how-would-you-validate-fairness-correctness-and-reliability-of-a-machine-learning-based-ranking-system)):

**Correctness:** offline metrics (engagement-prediction accuracy, ranking quality) on held-out data as launch gates; metamorphic tests (directional input→score changes); golden scenarios as smoke tests.

**Fairness/Integrity (heightened for a social feed):**
- **Disparate impact:** ranking quality/exposure parity across user groups (demographics, regions, languages) — compute per-group metrics, assert within tolerance.
- **Integrity guardrails:** the model must not amplify misinformation, hate, or harmful content for engagement — measure harmful-content exposure as a hard guardrail; counterfactual tests (swapping a sensitive attribute shouldn't change ranking when it shouldn't).
- **Filter-bubble / feedback loops:** monitor for the model narrowing content diversity over time.

**Reliability:**
- **Serving:** inference latency SLO at billions-of-requests scale; graceful fallback to a simpler/cached ranking if the model service degrades.
- **Training-serving skew:** features computed identically offline and online; input validation; drift detection in production.
- **Reproducibility:** versioned model + features + data for bisection.
- **A/B + guardrails:** online engagement vs. control, gated by integrity guardrails so an engagement win that harms integrity is blocked.

Architect framing: **ML quality is a continuous loop (offline gate → shadow/A-B → prod monitoring → retrain), and for a social feed the integrity/fairness guardrails are as important as the engagement metrics.**

---

## Performance & Reliability

### Q20: How would you validate performance of a real-time messaging system at global scale? (Senior)

Real-time messaging = latency + throughput + connection scale:

**1. Model load:** millions of concurrent persistent connections (WebSocket/MQTT), realistic message rates, group-chat fan-out, presence updates, geographic spread.

**2. Test types:**
- **Connection scale:** ramp to millions of concurrent connections — verify connection servers hold, memory/FD limits, and reconnection storms (after a server restart, all clients reconnect) don't cause cascade.
- **Latency:** message send→deliver P50/P95/P99 within SLA (real-time = sub-second); measure under load, not idle.
- **Throughput:** messages/sec ceiling; fan-out to large groups.
- **Spike:** viral event / mass reconnection surge.
- **Soak:** hours of sustained connections → no leaks.

**3. What to measure:** delivery latency percentiles, delivery success rate, connection setup time, reconnection behavior, message-loss rate (must be ~zero), resource saturation on connection + Kafka + push layers.

**4. Resilience under load:** kill a connection server mid-load → clients reconnect and resume, no lost messages; failover holds SLOs.

**Tooling:** specialized load generators for persistent connections (a normal HTTP load tool won't model WebSockets well) from a distributed fleet. This is the streaming/real-time analogue of [`jiostar`](jiostar-hotstar-framework-round.md) Q16.2 (view counter) and Q15.3 (live scale). Nuance: **tail latency and zero message-loss matter more than average.**

### Q21: How would you assess quality risks before launching a new feature to hundreds of millions of users? (Architect)

Risk assessment + mitigation at Meta scale:

**Identify:**
- **Blast radius:** hundreds of millions means even a 0.1% bug affects hundreds of thousands — assess worst-case impact.
- **Risk areas:** privacy/integrity (a privacy bug is catastrophic for a social product), data correctness, performance at scale, cross-platform parity (web/iOS/Android), interaction with existing features/flags.
- **Historical:** what broke in similar launches?

**Mitigate:**
- **Gradual rollout:** the [Q17](#q17-how-would-you-test-a-feature-rollout-being-released-gradually-to-different-user-segments-senior) ramp (1% → 100%) with automated guardrail monitoring and instant rollback — this is Meta's core safety mechanism.
- **Feature flags / kill switches:** instant disable without a deploy.
- **Guardrail metrics:** error rate, crash rate, engagement, and **integrity/privacy** metrics gate each ramp step.
- **Dogfooding + limited-cohort testing** before public ramp.
- **Load/perf validation** at projected scale ([Q20](#q20-how-would-you-validate-performance-of-a-real-time-messaging-system-at-global-scale)).
- **Privacy review:** explicit audience/visibility test suite as a launch blocker.
- **War room + monitoring** for the ramp; blameless postmortems feeding back into tests.

Architect framing: **at this scale you can't pre-test everything — you de-risk with gradual rollout + guardrails + instant rollback, and you treat privacy/integrity as launch-blocking, not nice-to-have.**

---

## Domain-Specific

### Q22: How would you test friend request workflows and privacy settings?

- **Friend request lifecycle:** send → pending → accept/decline/cancel/ignore; can't send duplicate; can't friend yourself; unfriend; block (removes friendship + prevents future requests).
- **State correctness:** both users' friend lists update consistently; counts correct; mutual-friends computed right.
- **Privacy settings (core):** "who can send me requests" (everyone/friends-of-friends) enforced; blocked users can't send; privacy of friend list (public/friends/only-me) enforced when others view it.
- **Notifications:** request → notification to recipient; acceptance → notification to sender.
- **Edge:** request to a deactivated/deleted account, request after being blocked, race (both send simultaneously → one friendship), unfriend-then-refriend.
- **Verification:** confirm via the other user's API view that privacy is actually enforced, not just the UI.

### Q23: How would you test photo and video upload functionality under different network conditions?

(Directly parallels YouTube upload — [Google Q23](google-model-answers.md#q23-how-would-you-test-youtube-video-uploads-across-different-devices-and-network-conditions).)

- **Upload correctness:** formats/sizes/resolutions; transcoding produces target renditions; thumbnails/metadata; multi-photo albums.
- **Network conditions (CDP / device-farm shaping):** slow 3G, high latency, packet loss, mid-upload disconnect → **resumable upload** resumes (no restart), no corruption; background upload on mobile.
- **Device matrix:** iOS/Android/web, low-end vs high-end, app-killed-mid-upload recovery.
- **Edge:** cancel, size/format limits (clear error), duplicate upload, simultaneous multi-file, near-storage-quota.
- **Post-upload:** media displays correctly in feed, correct privacy/audience applied, tagging works.
- **Performance:** upload + processing time SLAs.

### Q24: How would you test a recommendation system suggesting friends, groups, and content? (Senior)

People-You-May-Know / group / content recs — same non-deterministic-oracle + ML approach:

- **Structural:** correct count, no duplicates, no already-friends/already-joined/blocked entities, respects slots.
- **Relevance (directional/metamorphic):** mutual-friend-heavy suggestions rank higher; interacting with a topic surfaces related groups/content; cold-start gives sensible popular defaults.
- **Business/privacy rules:** don't suggest people the user blocked or who blocked them; respect "don't suggest me to others"; no sensitive-inference suggestions (a big integrity concern — don't reveal private connections).
- **Fairness/integrity:** don't over-suggest along sensitive lines; monitor feedback loops.
- **Quality metrics:** precision@k, acceptance rate, diversity vs. baseline (offline gate); A/B on acceptance rate (online oracle).
- **Reliability/scale:** latency SLO; graceful fallback if the rec service degrades.

### Q25: How would you define a quality strategy for Facebook Messenger supporting billions of messages daily? (Architect)

Full-lifecycle strategy for a real-time messaging product:

**1. Shift-left:** testability hooks (delivery-state introspection, feature flags), unit + component tests owned by dev, contract tests between messaging service and dependencies (presence, push, media, encryption).

**2. Functional & integration:** the delivery-correctness matrix from [Q3](#q3-how-would-you-test-facebook-messengers-message-delivery-functionality) — ordering, no-loss, no-dup, read receipts, groups, multi-device sync, offline queueing.

**3. End-to-end encryption (if E2EE):** verify messages are encrypted in transit/at rest, keys managed correctly, and the server can't read content — a correctness *and* security requirement.

**4. Non-functional:** latency + connection-scale load testing ([Q20](#q20-how-would-you-validate-performance-of-a-real-time-messaging-system-at-global-scale)); delivery reliability across regions ([Q12](#q12-how-would-you-test-reliability-of-notification-delivery-services-across-regions)).

**5. Resilience:** connection-server failure → reconnect + resume, no loss; region failover; graceful degradation (send queues when offline).

**6. Release safety:** gradual rollout + guardrails (delivery rate, latency, crash rate) + instant kill switch.

**7. Production quality:** synthetic message probes, delivery-rate/latency/error dashboards, anomaly detection, reconciliation (sent events == delivered), on-call runbooks.

**8. Ownership:** dev teams own components; a central QE function owns cross-service E2E and quality gates; SRE owns reliability.

Architect framing: **for a billions-of-messages real-time system, quality = strong contracts + relentless focus on zero-loss/correct-ordering delivery + connection-scale performance + gradual-rollout safety + deep production observability.**

---

## Situational / Behavioral

> **Format:** STAR (Situation, Task, Action, Result). Meta values **product sense, impact, and data-driven decisions** — foreground *your* actions, quantify results, and show you think about the user. The examples below are **templates** — replace with your own real stories.

### Q26: Tell me about a time when you found a bug that significantly impacted user experience.

- **S:** A feature (say a feed/messaging change) was ready to ship after passing standard checks.
- **T:** As SDET I owned the quality signal and looked beyond the happy path.
- **A:** During testing I found a UX-impacting bug — e.g., a privacy/audience setting not being honored on one platform, or messages arriving out of order under a specific network flap. I reproduced it deterministically, quantified the user impact (how many users, what they'd experience), captured evidence, and filed it with a minimal repro; paired with the dev on the fix.
- **R:** Caught before it reached users (or mitigated fast if in prod). I added a regression test so it can't recur. *(Show product empathy — you understood the user harm, not just the technical fault.)*

### Q27: Tell me about a time when product requirements changed after testing had already started.

- **S:** Mid-cycle, the PM changed the feature's behavior/scope after I'd built test coverage.
- **T:** Adapt the test strategy quickly without losing quality or wasting prior work.
- **A:** I reassessed impact — which existing tests still applied, which needed changes, which were now obsolete. I prioritized re-testing the changed areas and their interactions, updated the acceptance criteria with the PM, and communicated the revised risk/timeline. Because I'd layered tests (API/contract heavy, thin UI), most of my coverage survived the change.
- **R:** We absorbed the change without slipping quality; the layered design limited rework. *(Shows adaptability + good test architecture that tolerates change.)*

### Q28: Tell me about a time when you influenced a product decision using quality data.

- **S:** A product decision (ship a feature / choose an approach) was being made on intuition.
- **T:** Bring objective quality data to the decision.
- **A:** I gathered data — e.g., A/B results showing the new flow had a higher error/abandonment rate, or defect/crash metrics for a specific platform, or performance benchmarks. I presented it clearly tied to user impact and business metrics, and proposed a data-backed alternative (delay, fix-first, or ship behind a flag to a cohort).
- **R:** The decision changed based on the data (we fixed first / rolled out gradually), improving the user outcome. *(Meta prizes data-driven decisions — show you influenced product, not just reported bugs.)*

### Q29: Tell me about a time when you had to make a difficult quality decision affecting a product launch. (Senior)

- **S:** A launch date was set, but I had a significant quality concern (e.g., a privacy/integrity risk or perf regression at scale).
- **T:** Decide and advocate for the right launch posture under pressure.
- **A:** I quantified the risk (blast radius, likelihood, user/business harm) and proposed a concrete path: gradual rollout to a small cohort with guardrail monitoring and instant rollback, rather than a hard block or a full launch. I escalated with data and a recommendation, not just a concern.
- **R:** We shipped safely to a small segment first; guardrails caught (or confirmed the absence of) the issue before full ramp. *(Shows judgment: not a binary go/no-go, but a risk-managed rollout.)*

### Q30: Tell me about a time when you drove quality improvements across multiple teams without direct authority. (Architect)

- **S:** Cross-team integration issues (say, feed ↔ ranking ↔ integrity services) caused recurring escapes, but I owned none of those teams.
- **T:** Drive a systemic quality improvement through influence.
- **A:** I led with data — a dashboard tracing escapes to missing contract tests and their cost. I built a working proof-of-concept (consumer-driven contract tests on one boundary that caught a real break), socialized the results, wrote the shared standard, and made adoption frictionless (templates, CI integration, pairing with teams).
- **R:** Adoption spread across the key boundaries; cross-team escapes dropped measurably. *(Influence = data + a working example + removing adoption friction, not authority.)*

---

_Model answers for interview prep. For Meta, foreground product sense, user impact, and data-driven reasoning; adapt the behavioral examples to your own experience._
