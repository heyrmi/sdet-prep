# 5.3 — Design Test Infrastructure at Scale

> **Module 5 · SDET System Design** · ~35 min read
> *Concepts exercised:* Selenium Grid, browser/device farms, containerized browsers, ephemeral
> environments, session routing, autoscaling, video capture, cost modeling, concurrent-session
> back-of-envelope.

---

## The problem

Your test *platform* ([5.1](01-design-a-test-automation-platform.md)) can schedule and shard 100k
tests — but every UI test needs an actual **browser** (Chrome, Firefox, Safari, at specific
versions), and some need a real or emulated **device** (Android/iOS). Where do those thousands of
concurrent browsers come from? Today engineers run Chrome on their laptops, versions drift, "works
on my machine" is the norm, and the shared Selenium Grid falls over at 50 sessions.

You're designing the **test infrastructure**: the fleet that supplies clean, reproducible,
disposable browser and device sessions on demand — and the **ephemeral environments** the app
under test runs in. This is what Sauce Labs, BrowserStack, and LambdaTest sell, and what companies
build internally with Selenium Grid / Selenoid / Moon on Kubernetes.

> **Analogy.** A car-rental fleet at an airport. Travelers (tests) don't own cars; they request one
> of a specific model (Chrome 120), drive it, and return it — and it's **cleaned and reset** for the
> next renter (fresh, isolated session). The company sizes the fleet for peak demand, keeps a few
> warm and ready (pre-warmed pool), and scraps cars that won't start (unhealthy nodes). Nobody
> renting cares *which* physical car they get, only the model and that it's clean.

The core requirements: **isolation** (one test's browser state never leaks into another's),
**reproducibility** (Chrome 120 means Chrome 120, everywhere), **disposability** (a fresh
environment per session, torn down after), and **elastic scale + cost control** (hundreds of
sessions at peak, near-zero at 3 a.m.).

---

## Step 1: Requirements

**Functional**

- Provide **browser sessions** on demand: request `{browser: chrome, version: 120, os: linux}`,
  get a WebDriver/CDP endpoint.
- Support a **matrix**: multiple browsers × versions × OSes; plus **mobile** (emulators/simulators
  and real devices).
- **Route** each session to a healthy node with the right capabilities.
- **Isolate** every session (fresh profile, clean cookies/cache, no cross-talk).
- **Capture** video, logs, and traces per session for debugging.
- Provide **ephemeral app environments** for e2e (a fresh instance of the app-under-test + its
  dependencies).

**Non-functional**

- **Scale:** hundreds-to-thousands of concurrent sessions at peak.
- **Fast provisioning:** session start ≤ a few seconds (a slow grid throttles the whole platform).
- **Reliability:** a dead node must not strand a session; health-check and evict.
- **Cost-efficient:** browsers are RAM-hungry; idle capacity is burned money — autoscale to ~zero.
- **Reproducible & isolated:** pinned versions, one session per container.

---

## Step 2: The evolution — why containerized browsers won

**Classic Selenium Grid (Hub + static Nodes).** A central Hub receives session requests and routes
to registered Nodes (VMs/machines each running browsers). It works, but the nodes are
**long-lived and stateful**: browser crashes leak memory, profiles accumulate cruft, versions drift
across nodes, and the Hub is a scaling/SPOF bottleneck. Sessions are *not* cleanly isolated — node
#7 has slightly different state than node #3.

**Containerized browsers (Selenoid / Moon / Zalenium, Selenium Grid 4 on K8s).** Each session runs
in a **fresh, disposable Docker container** — browser + driver baked into a versioned image
(`selenoid/chrome:120.0`). Request a session → spin a container → run → **destroy it**. This nails
the three requirements at once:

- **Isolation:** one container per session; nothing survives teardown. No cross-test contamination.
- **Reproducibility:** the browser version *is* the image tag. `chrome:120` is byte-identical
  everywhere.
- **Disposability & scale:** containers start in ~1–3 s, and Kubernetes/an autoscaler can create and
  destroy thousands.

```
   test ──► [ Router / Grid Hub ]  (Selenoid / Moon / Grid4)
                    │  match capabilities {chrome:120}
                    ▼  create ephemeral container
        ┌───────────────────────────────────────────┐
        │  K8s / Docker host fleet (autoscaled)      │
        │  ┌────────┐ ┌────────┐ ┌────────┐          │
        │  │chrome  │ │firefox │ │chrome  │  ...      │  one container = one session
        │  │:120    │ │:121    │ │:119    │           │  destroyed after the test
        │  │+ video │ │+ video │ │+ video │           │
        │  └────────┘ └────────┘ └────────┘           │
        └───────────────────────────────────────────┘
```

> **Trade-off — containers vs real devices/browsers.** Containerized **Linux** Chrome/Firefox are
> cheap, fast, and infinitely reproducible — but they can't tell you how the site renders on **real
> Safari on a real iPhone** or a specific Samsung GPU. Real-device/real-browser farms (or cloud
> vendors) catch platform-specific bugs but are expensive, slower to provision, and harder to keep
> clean. Run the **bulk** on containers and a **thin top layer** on real devices/browsers for
> compatibility coverage.

---

## Step 3: Session routing

The router/grid is the traffic cop. For each request it must:

1. **Match capabilities** — find nodes offering `chrome:120` on `linux` with free capacity.
2. **Load-balance** — pick the least-loaded eligible host (browsers are heavy; packing them evenly
   avoids OOM). See [2.1 Load balancing](../02-building-blocks/01-load-balancing.md).
3. **Enforce quotas** — per-team concurrency caps so one suite can't grab the whole grid (fairness,
   as in [5.1](01-design-a-test-automation-platform.md)).
4. **Maintain session affinity** — a WebDriver session is *stateful*: every command
   (`click`, `findElement`) for session `S` must reach the **same** browser container for the
   session's life. The router keeps a `sessionId → container` map; unlike stateless HTTP, you can't
   round-robin mid-session.
5. **Health-check & evict** — probe nodes; a crashed/hung browser is killed, its session failed
   fast (so the platform retries elsewhere) rather than hanging for a timeout.

> **Why session affinity matters (interview detail).** Selenium/WebDriver is a stateful protocol
> over HTTP. The first request creates a session and returns a `sessionId`; every subsequent command
> carries it and *must* land on the same browser instance. A naive stateless load balancer that
> spreads a session's commands across containers breaks every test. The grid pins the session — this
> is exactly the sticky-session concern from load balancing, made mandatory.

---

## Step 4: Ephemeral test environments

E2E tests need the **app under test** running somewhere realistic. Two failure modes to avoid:
a single shared "staging" (tests collide, flake, and pollute each other's data) or per-test full
cloud stacks (too slow/expensive).

The modern answer: **ephemeral, on-demand environments**, one per PR or per test run, torn down
after.

- **Namespace/container per PR:** spin the app + its dependencies (DB, cache, broker) into an
  isolated Kubernetes namespace or a docker-compose stack, seed data, run e2e, destroy. Preview
  environments (Vercel/Netlify previews, Argo, Qovery) do this per PR.
- **Testcontainers for integration:** the test process itself boots real dependencies in throwaway
  containers (a real Postgres, a real Kafka) and tears them down — no shared DB to corrupt. This is
  the integration-stage backbone from [5.2](02-design-a-ci-cd-pipeline.md).
- **Data isolation:** each environment gets its own seeded, namespaced data so parallel runs never
  share a row (see [5.4 Test data management](04-design-for-testability.md)).

> **Trade-off — shared vs ephemeral environments.** A shared long-lived staging is cheap and always
> "there," but it's a flakiness and contention magnet: concurrent tests mutate the same data, one
> team's deploy breaks another's run, and state drifts. Ephemeral environments are isolated,
> reproducible, and disposable — at the cost of provisioning time and orchestration complexity.
> The isolation almost always pays for itself in reduced flakiness.

---

## Step 5: Video, logs & traces

When a UI test fails, a stack trace alone is nearly useless ("element not found" — *why?*). Rich
capture is what makes remote/headless failures debuggable, and it's expected of a serious grid.

- **Video** of the whole session (the container records the browser display). On failure you *watch*
  what happened. Selenoid/Moon record per-session MP4s automatically.
- **Browser + driver logs**, console errors, and the full **network HAR**.
- **Traces** (Playwright trace / Selenium DevTools/CDP capture) — a time-travel snapshot of DOM,
  network, and actions.
- Uploaded to the **artifact store** ([5.1 §3.6](01-design-a-test-automation-platform.md)) via
  presigned URLs; linked from the report.

> **Cost lever (same as 5.1).** Recording video for *every* session at thousands/day is huge storage.
> Default to **record-and-retain-on-failure only** (record always, keep only if the test failed, or
> record only failing retries). This is the single biggest storage saving in test infra.

---

## Step 6: Autoscaling & the concurrent-session back-of-envelope

Browsers are **RAM- and CPU-hungry**, and demand is bursty (idle overnight, slammed at merge time).
Size the fleet from concurrency, and autoscale on **pending session queue depth**, not CPU.

**Resource per session (rule of thumb):** a headless Chrome container needs ~**1 vCPU and ~1.5–2 GB
RAM** to be reliable under real page loads. Call it **2 GB + 1 vCPU** per session.

**How many concurrent sessions do we need?** From the [5.1 estimate](01-design-a-test-automation-platform.md):
30,000 UI tests in a full run, ~8 s each, target 10-minute (600 s) wall clock.

- Total UI test-seconds ≈ `30,000 × 8 = 240,000 s`.
- Concurrent sessions to finish in 600 s ≈ `240,000 / 600 = 400 concurrent browsers`.
- Add headroom for PR traffic + a couple of overlapping runs at peak → design for **~600 concurrent
  sessions**.

**Fleet sizing:**

- RAM: `600 × 2 GB = 1,200 GB ≈ 1.2 TB`. CPU: `600 vCPU`.
- On 16-vCPU / 64 GB nodes: RAM-bound at `64/2 = 32` sessions/node, CPU-bound at `16` sessions/node
  → **CPU-bound at ~16 sessions/node**. Need `600 / 16 ≈ 38 nodes` at peak.
- **Autoscale:** baseline of a few warm nodes; scale out on queue depth to ~40 at peak; scale to
  near-zero overnight. Keep a small **pre-warmed pool** so the first tests of a run don't pay
  cold-start.

**Cost intuition:** 38 × 16-vCPU nodes running ~4 peak hours/day on spot (~$0.15/vCPU-hr) ≈
`38 × 16 × 0.15 × 4 ≈ $365/day` for the container fleet — versus a cloud device farm charging
per-minute per-session that, at 240,000 UI-test-seconds/day (~67 session-hours) times several
runs, can be **10–50× more**. That gap is the classic **build-vs-buy** decision.

> **Trade-off — build vs buy.** Self-hosted containerized grid on spot instances is far cheaper per
> session at high volume and keeps data in-house, but you own uptime, image maintenance, version
> matrices, and real-device labs. Cloud farms (BrowserStack/Sauce/LambdaTest) give instant access to
> hundreds of real browser/OS/device combos with zero ops — great for low/medium volume and
> compatibility breadth, expensive at 100k-test scale. Many orgs do **hybrid**: self-hosted
> containers for the high-volume Linux-Chrome bulk, cloud for the real-Safari/real-device long tail.

---

## Step 7: Mobile & device farms

Mobile adds real hardware constraints containers can't fully model.

- **Emulators/simulators** (Android AVD, iOS Simulator) run in the cloud/CI — cheaper, scalable,
  reproducible, but *not* real hardware (no real GPU quirks, sensors, or carrier network).
- **Real device farms** — racks of physical phones with USB/OTG connections, exposed via
  Appium/STF (Open STF), or AWS Device Farm / Firebase Test Lab / BrowserStack App Automate. Catch
  device-specific bugs; expensive, need reservation/queueing, physical maintenance (batteries,
  reboots).
- **Routing** parallels browsers: match `{platform: android, version: 13, device: Pixel 7}`, pin the
  Appium session to that device, capture video, release and **factory-reset** state after. Real
  devices need a **reset/cleanup** step (uninstall app, clear data) since you can't just destroy a
  container.

> **Trade-off.** Emulators = scale and cost; real devices = fidelity. Run functional/regression on
> emulators; reserve real devices for a curated matrix of high-market-share models and known-quirky
> hardware.

---

## Trade-offs & key takeaways

- **Containerized, disposable browsers** (Selenoid/Moon/Grid4-on-K8s) beat static grids by nailing
  **isolation + reproducibility + scale** — one fresh container per session, destroyed after.
- **Session affinity is mandatory:** WebDriver is stateful; every command for a session must reach
  the same container. Not a stateless HTTP load-balance.
- **Ephemeral environments** (per-PR namespaces, Testcontainers) beat a shared staging on flakiness
  and isolation, at the cost of provisioning time.
- **Capture video/traces, keep on failure** — the biggest debuggability win and the biggest storage
  lever.
- **Size the fleet from concurrent sessions** (240k UI-test-seconds / 600 s target ≈ 400–600
  browsers), autoscale on **queue depth**, use spot + a warm pool.
- **Build vs buy vs hybrid:** self-host the high-volume Linux-Chrome bulk; use cloud farms for real
  Safari/device breadth.
- **Mobile:** emulators for scale, real devices for fidelity; real devices need explicit reset.

---

## In the wild

- **Selenium Grid 4** — native Kubernetes/Docker support, distributed router + node roles.
- **Selenoid / Moon (Aerokube)** — one Docker/K8s container per session with built-in video; Moon is
  the K8s-native, autoscaling variant.
- **BrowserStack / Sauce Labs / LambdaTest** — cloud browser + real-device farms with the matrix,
  video, and routing as a service.
- **AWS Device Farm / Firebase Test Lab / Open STF** — real mobile-device labs.
- **Testcontainers** — throwaway real dependencies per test; **Argo/Qovery/Vercel previews** —
  ephemeral per-PR app environments.

---

## SDET interview angle

The interviewer probes whether you understand *why* containerized/disposable beats static grids
(**isolation, reproducibility, disposability**), the **session-affinity** subtlety (this catches
people), and the **concurrent-session sizing math**. Bringing up **ephemeral environments**, the
**video-on-failure** cost lever, and a reasoned **build-vs-buy** stance signals real operating
experience.

**Common follow-ups:**

- *"How many browser containers to run 30k UI tests in 10 minutes?"* → `test-seconds / target ≈ 400`
  concurrent; then node math from RAM/CPU per session.
- *"Why not just a load balancer in front of the browser nodes?"* → WebDriver is stateful; session
  affinity — every command must hit the same container.
- *"Tests pass alone but flake when run in parallel."* → shared environment/data contention →
  ephemeral per-run environments + namespaced seed data.
- *"Grid cost is out of control."* → spot instances + autoscale-to-zero on queue depth + record
  video only on failure + hybrid build/buy.
- *"How do you test real Safari/iOS?"* → containers can't; cloud real-device farm for the thin
  compatibility layer.
- *"A browser node hangs and stalls a run."* → health-check + evict + fail-fast so the platform
  retries the session elsewhere.

---

## Practice / self-check

1. Give three concrete advantages of one-container-per-session over a static Selenium Grid node.
   Map each to a requirement (isolation / reproducibility / scale).
2. Why can't a plain stateless load balancer sit in front of your browser containers? What does the
   grid keep to make WebDriver work?
3. You need to finish 45,000 UI tests (avg 6 s) in 15 minutes. How many concurrent browsers? At 16
   sessions/node, how many nodes?
4. Compare a shared staging environment with per-PR ephemeral environments for e2e. Which reduces
   flakiness and why?
5. Your grid costs 30× a self-hosted estimate. Walk through build-vs-buy: when is the cloud farm
   still the right call?
6. Why record video for every session but *retain* only on failure? What does this save, and what
   do you risk losing?

---

## How this shows up in an SDET loop

Often folded into the [5.1 platform question](01-design-a-test-automation-platform.md) as a deep
dive ("okay, where do the browsers actually come from?") or asked standalone for infra-heavy test
roles. Interviewers want the **containerization rationale**, the **session-affinity gotcha**, the
**sizing math**, and a **build-vs-buy** opinion with numbers. It's also a favorite because it
connects test-domain knowledge (Selenium/Appium) to real distributed-systems concepts (routing,
autoscaling, statefulness).

**Next:** [5.4 — Design for Testability »](04-design-for-testability.md)
