# 3.6 — Observability: Logs, Metrics & Traces

> **Module 3 · Distributed Systems** · ~30 min read
> *You built a system spread across dozens of services. At 3 a.m. it's "slow" and users are angry.
> You can't SSH into 200 boxes. The only way to find the problem is if the system was already
> telling you what it's doing — through* logs, metrics, and traces*. You can't fix what you can't
> see.*

---

## The problem

On a single server, debugging is easy: read the log file, watch CPU, attach a debugger. Now you
have a load balancer, twelve microservices, three databases, a cache, and a message queue. A user
reports "checkout is slow." Where is the slowness?

- Is it the LB? One of the twelve services? Which one?
- Is the database slow, or is the *network* between two services slow?
- Is it slow for *everyone*, or just users in one region, or just 1% of requests?
- It was fine an hour ago — what changed?

You cannot answer these by logging into machines. The system is too big and the problem is
ephemeral. You need the system to **continuously emit signals about its own behavior** so that when
something breaks, the evidence is already collected.

> **Analogy.** A hospital patient. A doctor doesn't diagnose by guessing — they read the **chart**
> (logs: discrete events — "administered medication at 2:14"), the **vital-signs monitor** (metrics:
> numbers over time — heart rate, blood pressure trending), and an **X-ray that follows one system
> through the body** (traces: the path a single thing takes). Each answers a different question.
> Together they let you diagnose without cutting the patient open. Observability is instrumentation
> for your system's vital signs.

**Monitoring vs observability.** *Monitoring* asks known questions ("is CPU > 90%?"). *Observability*
is the broader property: enough signal to answer questions you **didn't anticipate** — to debug
*novel* problems. Monitoring is a subset of observability.

---

## Core idea: the three pillars

Three complementary signal types, each answering a different question:

```
   LOGS     →  "What exactly happened (and why)?"   discrete events, rich detail
   METRICS  →  "How much / how often / how fast?"   numbers aggregated over time
   TRACES   →  "Where did the time go, across services?"  one request's full journey
```

You need all three. Metrics tell you *something* is wrong (error rate spiked). Traces tell you
*where* (the payment service's DB call). Logs tell you *why* (a specific exception on a specific
row). Used together, they take you from "users are angry" to "here's the bug" fast.

---

## How it works

### Pillar 1: Logs (and structured logging)

A **log** is a timestamped record of a discrete event. The crucial upgrade for distributed systems
is **structured logging** — emit logs as machine-parseable key-value data (usually JSON), not free
text:

```
   ✗ unstructured:  "User 123 failed login from 10.0.0.4 at 02:14"
   ✓ structured:    {"ts":"02:14:03","level":"WARN","event":"login_failed",
                     "user_id":123,"ip":"10.0.0.4","trace_id":"abc-789"}
```

Why structured wins: you can **search, filter, and aggregate** across millions of log lines from
hundreds of services ("show all `login_failed` for `user_id=123` in the last hour"). Logs are
shipped to a central store (ELK/OpenSearch, Loki, Splunk) so you never log into a box.

Trade-off: logs are **high-volume and expensive** (storage, ingestion cost). You can't log
everything at full detail forever. Use **levels** (DEBUG/INFO/WARN/ERROR), sample noisy logs, and
keep the rich detail for errors. **Always include a `trace_id`** (see Pillar 3) so a log line ties
back to the request it belongs to.

### Pillar 2: Metrics

A **metric** is a number measured over time, stored cheaply as a **time series**. Metrics are tiny
compared to logs, so you keep them at high resolution for a long time and graph trends. Four common
types:

- **Counter** — only goes up (total requests, total errors). You graph its *rate* (requests/sec).
- **Gauge** — goes up and down (current memory, active connections, queue depth).
- **Histogram** — distribution of values into buckets (request durations) → lets you compute
  **percentiles**.
- **Summary** — similar, with client-side quantiles.

**Two recipes for *which* metrics to collect:**

**RED** (for **request-driven services**):
```
   Rate      — requests per second
   Errors    — failed requests per second
   Duration  — latency distribution (percentiles)
```

**USE** (for **resources** like CPU, disk, queues):
```
   Utilization — how busy (% used)
   Saturation  — how much queued/waiting (the backlog)
   Errors      — error count
```

RED watches your *services*; USE watches your *machines*. Together they cover "are requests OK?" and
"are resources healthy?"

### Percentiles & tail latency (recap)

**Never trust averages for latency.** An average hides the worst experiences. If 99 requests take
10 ms and one takes 5 s, the average (~60 ms) looks fine while one user waited 5 seconds. Track
**percentiles** instead:

```
   p50 (median): half of requests are faster than this
   p95:  95% are faster — the "typical bad" experience
   p99:  99% are faster — the tail
   p999: 99.9% — the worst 0.1%, but at scale that's a LOT of requests
```

**Tail latency matters more than you'd think:** if a single user request fans out to 100 backend
calls and *each* has a 1% chance of being slow, the odds that *at least one* is slow — making the
*whole* request slow — is ~63%. At scale, the tail becomes the common case. This is why we obsess
over p99, not the average. (First met in [Module 0.2](../00-foundations/02-numbers-every-engineer-should-know.md).)

### Pillar 3: Distributed tracing

A single user request hops through many services. A **trace** stitches that whole journey into one
view. The mechanism:

- When a request enters the system, it's assigned a unique **trace ID**.
- Each unit of work (a service handling the request, a DB call) is a **span**, with its own span ID,
  a parent span ID, and a start/end time. Spans nest into a tree.
- The trace ID (and current span ID) is **propagated** to every downstream call — passed in
  headers — so every service tags its spans with the same trace ID. This is **context propagation**,
  and it's the part you must wire through your code/middleware.

```
   trace_id = abc-789

   [ API Gateway          ============================ ] 420ms
      [ Auth service   == ] 30ms
      [ Order service       ====================== ] 350ms   ◄── the culprit
         [ DB query              =========== ] 280ms          ◄── here's the time
         [ Cache lookup    = ] 5ms
      [ Notification    == ] 20ms
```

Instantly you *see* where the 420 ms went: the Order service's DB query ate 280 ms. No guessing,
no SSH. Tools: Jaeger, Zipkin, Tempo, and the vendor-neutral **OpenTelemetry** standard that's
become the way to instrument all three pillars.

The big win: **traces connect the pillars.** Put the `trace_id` in your structured logs and on your
metrics' exemplars, and you can pivot from "p99 latency spiked" → the slow trace → the exact log
lines for that request. That's the whole game.

---

## SLI, SLO & error budgets

Observability data is only useful if you've defined **what "good" means.**

- **SLI (Service Level Indicator):** a measured number that reflects user happiness — e.g. "% of
  requests served < 300 ms," "% of requests that succeed."
- **SLO (Service Level Objective):** the *target* for an SLI — e.g. "99.9% of requests succeed over
  30 days." This is your internal reliability goal.
- **SLA (Service Level Agreement):** a *contract* with customers (with penalties) — usually looser
  than your SLO so you have a safety margin.
- **Error budget:** the flip side of an SLO. A 99.9% target means **0.1% is your budget to spend** —
  ~43 minutes/month of allowed failure. This reframes reliability brilliantly: if you've got budget
  left, ship features faster; if you've burned it, freeze and stabilize. It turns "reliability vs
  velocity" from an argument into a number.

```
   SLO 99.9%  →  error budget 0.1%  →  ~43 min/month
   budget left  → ship fast
   budget gone  → stop shipping, fix reliability
```

---

## Alerting & dashboards

Collecting signal is pointless if no one is told when it matters.

**Alert on symptoms, not causes.** Page humans for things **users feel** — "error rate > 1%,"
"p99 latency > 1 s," "checkout success rate dropped." Don't page on every cause ("CPU at 85%") —
high CPU might be totally fine if users are happy. Cause-based alerts create **noise**, and noise
leads to **alert fatigue**, where on-call engineers start ignoring pages (and miss the real one).

```
   ✗ cause alert:    "CPU > 80%"          → maybe harmless; noisy
   ✓ symptom alert:  "p99 checkout latency > 1s for 5 min"  → users hurting; page now
```

Good alerts are **actionable** (a human can do something), **urgent** (needs attention *now* —
otherwise it's a ticket, not a page), and tied to **SLOs / error-budget burn**. The human side is
**on-call**: a rotation of engineers ready to respond, with **runbooks** (documented response
steps) so a 3 a.m. page has a clear playbook.

**Dashboards** are for *exploration and context*, not paging — the RED/USE graphs, error-budget
burn-down, and traffic trends you scan during an incident or review. The pattern: an **alert** tells
you *something's wrong* and points you at a **dashboard** to investigate, where you pivot via
`trace_id` into **traces and logs** for root cause.

---

## Comparison

| Pillar | Question answered | Volume / cost | Cardinality friendly? | Best for |
|--------|-------------------|---------------|-----------------------|----------|
| **Logs** | What exactly happened & why | High | Yes (rich detail) | Root-cause detail, audit |
| **Metrics** | How much / fast / often | Low (cheap) | No (avoid high-cardinality labels) | Trends, alerting, SLOs |
| **Traces** | Where did time go across services | Medium (often sampled) | Per-request | Latency breakdown, dependency mapping |

---

## Trade-offs & key takeaways

- **You can't fix what you can't see** — instrument *before* the incident; you can't add
  observability retroactively to a problem that already happened.
- **All three pillars, working together.** Metrics detect, traces localize, logs explain. Tie them
  with a shared **trace ID**.
- **Observability costs money** (storage, ingestion, processing) — sample logs/traces, avoid
  high-cardinality metric labels (a label per user ID will blow up your metrics system), keep
  retention sensible.
- **Percentiles over averages** — the average lies; the tail is where users suffer, and at scale the
  tail is common.
- **Alert on symptoms, define SLOs, budget your errors** — this aligns reliability with the
  business and protects on-call humans from noise.
- **OpenTelemetry** is the vendor-neutral standard for emitting all three — instrument once.

---

## In the wild

- **Prometheus** (metrics, pull-based) + **Grafana** (dashboards) is the de-facto open-source
  metrics stack; **Prometheus** popularized the RED/USE-style approach.
- **Jaeger / Zipkin / Tempo** for distributed tracing; **OpenTelemetry** unifies instrumentation
  across all three pillars.
- **The ELK stack** (Elasticsearch, Logstash, Kibana) / OpenSearch / **Loki** / Splunk for
  centralized log search.
- **Google's SRE practice** originated SLIs/SLOs/error budgets; they're now industry standard.

---

## Interview angle

Whenever a design is "done," a strong candidate adds: "and here's how I'd observe it." Name the
**three pillars** and what each is *for* (metrics detect, traces localize, logs explain), insist on
**percentiles (p99) over averages** for latency, and describe **distributed tracing with trace-ID
propagation** as the way to debug across services. Bring in **SLIs/SLOs/error budgets** to show you
think about reliability as a measurable target, and **symptom-based alerting** to show you respect
on-call humans. Calling out the **cost trade-offs** (sampling, cardinality) is a senior signal.

**Common follow-ups:**
- "A request is slow — how do you find where?" → distributed trace; look at the span tree for the
  expensive span; pivot to that service's logs via trace ID.
- "Why not just average your latency?" → averages hide the tail; track p95/p99; at fan-out scale the
  tail dominates.
- "What would you alert on?" → user-facing symptoms tied to SLOs (error rate, p99 latency), not raw
  causes like CPU; avoid alert fatigue.
- "Why structured logs?" → searchable/aggregatable across services; correlatable via trace ID.
- "What's an error budget and why is it useful?" → the allowed unreliability under an SLO; turns the
  reliability-vs-velocity debate into a number.

---

## Self-check

1. What distinct question does each pillar (logs, metrics, traces) answer, and how do they combine
   to debug a "checkout is slow" report?
2. What is a `trace_id`, what is context propagation, and why is it the linchpin of debugging across
   services?
3. Why should you alert on p99 latency rather than average latency? What does "tail latency at
   fan-out" mean?
4. Explain SLI vs SLO vs SLA, and what an error budget lets a team decide.
5. Why is "alert on symptoms, not causes" the rule, and what failure mode does it prevent?

---

**Next:** [4.1 — Design a Rate Limiter »](../04-case-studies/01-rate-limiter/README.md)
