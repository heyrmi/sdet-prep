# 4.14 — Design a Metrics Monitoring & Alerting System

> **Module 4 · Case Studies** · ~35 min read + coding assignment
> *Concepts exercised:* time-series data model, push vs pull collection, tumbling-window
> aggregation, downsampling & retention, alerting rules, the "resolution vs cost" trade-off.

---

## The problem

A **metrics monitoring system** collects numbers from thousands of machines over time —
CPU %, request rate, error count, queue depth — stores them efficiently, lets you **query and
graph** them, and **fires alerts** when something crosses a threshold ("error rate > 5% for 5
minutes → page someone"). Think Prometheus, Datadog, Grafana, CloudWatch.

Why you need one:
- **You can't fix what you can't see.** When latency spikes at 3 a.m., metrics tell you *which*
  service, *when*, and *how bad*.
- **Capacity planning.** "Are we running out of memory next month?" is a question about trends.
- **Alerting.** Humans can't stare at dashboards; the system watches and wakes someone up.

> **Analogy.** A hospital patient monitor. Sensors sample vital signs (heart rate, oxygen)
> **many times per second**, the screen plots the last few minutes, and an **alarm** beeps when a
> reading leaves the safe band. You don't keep every millisecond forever — older data is
> summarized ("average heart rate per hour") so the chart stays readable and storage stays sane.
> A metrics system is that monitor, for fleets of servers instead of one patient.

The whole field has one dominant data shape — the **time series** — and one dominant tension:
**high resolution costs a fortune; coarse data is cheap but blurry.** Everything below is a
negotiation between those two.

---

## Step 1: Requirements (always start here)

**Functional**
- **Collect** metrics from many sources (servers, containers, apps).
- **Store** them as time series so we can query historical ranges.
- **Query** with aggregation over time windows (avg CPU per minute over the last hour).
- **Alert**: evaluate rules on a schedule; fire when a condition holds.
- **Dashboards**: render time-series graphs.

**Non-functional**
- **Write-heavy.** Far more samples are written than queried — ingestion is the hot path.
- **Scalable.** 10,000 hosts × 1,000 metrics each, every 10 seconds, is ~1M writes/sec.
- **Time-bounded value.** A 2-second-old metric is gold; a 2-year-old one only matters as a
  trend. This justifies **downsampling and retention**.
- **Highly available** for alerting — if the monitoring is down during an outage, you're blind.
- **Approximate is usually fine.** Dropping the odd sample doesn't matter; we're watching trends,
  not doing accounting. (Contrast with the next case study, ad-click *billing*, where it does.)

---

## Step 2: Estimation (back-of-envelope)

Say **10,000 hosts**, each emitting **1,000 metrics** every **10 seconds**.

```
writes/sec = 10,000 hosts × 1,000 metrics ÷ 10 s = 1,000,000 samples/sec
```

Each raw sample, naively stored, is ~16 bytes (8-byte timestamp + 8-byte float). But time-series
databases **compress hard** — timestamps are evenly spaced (delta-of-delta encoding) and values
change little (XOR encoding, the Gorilla/Facebook trick). Real-world compression hits **~1.3–2
bytes per sample.**

```
raw:        1M/s × 16 B  = 16 MB/s  ≈ 1.4 TB/day
compressed: 1M/s × 1.5 B = 1.5 MB/s ≈ 130 GB/day  ← still a lot if kept at full resolution
```

This is exactly why we **downsample**: keep 10-second resolution for 2 days, 1-minute for a
month, 1-hour for a year. The trend survives; the storage bill collapses.

---

## Step 3: High-level design

### The pipeline

```
  ┌──────────┐   metrics    ┌─────────────┐   ┌──────────────────┐
  │ targets  │ ───────────► │  collector  │──►│ time-series DB    │
  │(servers, │  (push/pull) │ (ingestion) │   │ (TSDB, compressed)│
  │ apps)    │              └─────────────┘   └────────┬─────────┘
  └──────────┘                                         │
                                          ┌────────────┼─────────────┐
                                          ▼            ▼             ▼
                                   ┌────────────┐ ┌─────────┐ ┌─────────────┐
                                   │ query/API  │ │ alerting│ │ downsampler │
                                   └─────┬──────┘ │ engine  │ │ + retention │
                                         ▼        └────┬────┘ └─────────────┘
                                   ┌───────────┐       ▼
                                   │ dashboards│   ┌────────┐
                                   └───────────┘   │ notify │ (page/Slack/email)
                                                   └────────┘
```

### The data model — the time series

This is the single most important idea. A **time series** is an append-only stream of
`(timestamp, value)` points, identified by a **metric name plus a set of labels**:

```
http_requests_total{service="checkout", method="POST", status="500"}
└──── metric name ──┘ └──────────────── labels (key/value) ────────┘

   → [ (1718450000, 4201), (1718450010, 4218), (1718450020, 4233), ... ]
        timestamp   value    timestamp   value
```

- **Metric name** = what is measured (`http_requests_total`).
- **Labels** = the dimensions you slice by (`service`, `status`). The full set of name + labels
  defines a unique series. `status="500"` and `status="200"` are **two different series.**
- **Sample** = one `(timestamp, value)`. Timestamps usually align to a scrape interval.

> **Trade-off — labels are powerful but explode.** Every distinct label combination is its own
> series ("cardinality"). Adding a `user_id` label to a metric with 10M users creates 10M series
> and can melt the database. Rule of thumb: labels are for **bounded, low-cardinality**
> dimensions (service, region, status code), never for unbounded IDs.

### Metric types (vocabulary you'll hear)

| Type | Meaning | Example | How you query it |
|------|---------|---------|------------------|
| **Counter** | Only goes up; resets on restart | total requests, total errors | take the **rate** (per-second increase) |
| **Gauge** | Goes up and down | memory in use, queue depth, temperature | read it directly; avg/min/max |
| **Histogram** | Buckets of observations | request latency distribution | percentiles (p50, p99) |

The assignment builds a **gauge-style aggregation** (`Query`) and a **counter rate** (`Rate`).

### A minimal query API

```
GET /query?metric=cpu_percent
          &labels=host:web-1
          &from=1718450000&to=1718453600
          &window=60          # tumbling window width, seconds
          &agg=avg            # sum | avg | count | max | min
→ [ {windowStart: 1718450000, value: 41.2},
    {windowStart: 1718450060, value: 43.8}, ... ]
```

---

## Step 4: Deep dives

### 4a. Push vs Pull collection

How do metrics get from a server into the collector? Two camps, and this is *the* classic
interview debate for this system.

- **Push** (StatsD, Datadog agent, Graphite): each target **sends** its metrics to the collector.
- **Pull** (Prometheus): the collector **scrapes** each target's `/metrics` HTTP endpoint on a
  schedule.

> **Analogy.** Push = employees mail you their timesheets (you hope they remember). Pull = you
> walk the floor and check on each person yourself (you always know who's missing).

| Dimension | **Push** | **Pull** |
|-----------|----------|----------|
| Who initiates | The target | The collector (scraper) |
| Service discovery | Target must know collector address | Collector must know target list |
| "Is the target up?" | Hard — silence is ambiguous (dead? or just quiet?) | Easy — a failed scrape = target down |
| Short-lived jobs | Natural (push before exit) | Hard (job may die before scrape) → needs a *push gateway* |
| Firewall/NAT | Target reaches out (NAT-friendly) | Collector must reach target |
| Load control | Target can flood the collector | Collector controls scrape rate |
| Examples | StatsD, Datadog, Graphite | Prometheus, most Kubernetes setups |

**There is no winner.** Prometheus chose pull because "did the scrape succeed?" is a free,
reliable health check, and the collector controls the rate. Push wins for **ephemeral** workloads
(a batch job that runs for 3 seconds) and for crossing network boundaries. Many real stacks do
both: pull for long-lived services, a **push gateway** for short-lived jobs.

### 4b. Tumbling windows & aggregation (what you'll build)

Raw samples are noisy and dense. We **bucket** them into fixed-width, non-overlapping windows
and reduce each bucket to one number. These are **tumbling windows** — back-to-back, no overlap,
no gaps:

```
samples:   • •   ••  •      •  ••    •
time:    ──┬────────┬────────┬────────┬──►
           0        10       20       30      (windowSec = 10)
window:  [  W0    )[  W1    )[  W2    )
agg=avg:   41.2      —(empty) 43.8
                     ↑ no samples → no Point (documented choice)
```

A sample at timestamp `ts` belongs to window `(ts / windowSec) * windowSec`. Compare with the
**sliding window** from the rate-limiter lesson: sliding windows *overlap* and move continuously;
tumbling windows snap to fixed boundaries. Tumbling is the right model for charting because each
pixel column on a graph is one fixed bucket.

> **Design choice — empty windows: skip or zero-fill?** If a window has no samples, do you emit
> nothing, or emit a point with value 0? Both are valid:
> - **Skip** (what the assignment does): the graph shows a gap, which honestly says "no data."
>   Cheaper; no fake numbers.
> - **Zero-fill / interpolate**: the line stays continuous, nicer to look at, but a gap from a
>   *crashed exporter* now looks like a real zero — misleading.
>
> State the choice explicitly. We **skip empty windows** and test it.

The aggregation functions:

| Agg | Reduces a window's values to... | Typical use |
|-----|---------------------------------|-------------|
| `Sum` | total | total bytes sent this minute |
| `Avg` | mean | average CPU this minute |
| `Count` | number of samples | how many requests landed |
| `Max` | largest | peak memory |
| `Min` | smallest | lowest free disk |

### 4c. Counters and `Rate`

A counter only ever increases (until the process restarts). The raw value
(`http_requests_total = 4,233,901`) is useless on a chart — what you want is the **slope**: how
fast is it climbing?

```
rate = (last_value − first_value) / (last_time − first_time)
```

If a request counter goes from 100 at t=0 to 120 at t=10s, the rate is `(120−100)/(10−0) = 2
requests/sec`. That's the `Rate` function you'll implement. (Real systems also handle the
**counter reset** on restart — when the value suddenly drops, treat it as a reset rather than a
huge negative rate. We keep the assignment simple and assume monotonic input.)

### 4d. Downsampling & retention

Keeping every 10-second sample for a year is wasteful — nobody zooms into one minute from eight
months ago. So we **roll up** older data into coarser windows and **expire** the rest:

| Age of data | Resolution kept | Why |
|-------------|-----------------|-----|
| 0–2 days | 10 s (raw) | debugging a live incident needs detail |
| 2–30 days | 1 min | trends, week-over-week comparison |
| 30 days–1 yr | 1 hour | capacity planning, seasonality |
| > 1 yr | dropped (or archived cold) | rarely queried; not worth the SSD |

> **Trade-off — resolution vs cost.** This is the heart of the system. High resolution = precise
> incident forensics but huge storage and slow long-range queries. Downsampling trades fine
> detail (which you only need recently) for cheap, fast long-range trends. The hospital monitor
> analogy again: per-second for the last few minutes on screen, hourly averages in the chart.

### 4e. Alerting

An **alerting rule** is a query plus a condition plus a duration:

```
ALERT HighErrorRate
  IF   rate(http_errors_total[5m]) / rate(http_requests_total[5m]) > 0.05
  FOR  5m                  # condition must hold continuously, to avoid flapping
  THEN page on-call
```

The **alert evaluator** runs each rule on a fixed schedule (say every 15 s), evaluates the query
over recent data, and tracks state: `inactive → pending (condition true but FOR window not yet
satisfied) → firing`. The **`FOR` duration** is crucial — without it, a single noisy spike pages
someone at 3 a.m. for nothing ("flapping"). A separate concern is **notification routing and
deduplication** (group related alerts, silence during maintenance, escalate if unacked) —
usually a dedicated component (e.g. Prometheus *Alertmanager*).

> **Trade-off — sensitive vs noisy.** Tight thresholds + short `FOR` catch problems fast but page
> on every blip (alert fatigue → people ignore pages → real incidents missed). Loose thresholds +
> long `FOR` are quiet but slow to fire. Tuning this is an ongoing operational art.

---

## In the wild

- **Prometheus** — pull-based, label-rich time-series model, its own query language (PromQL),
  built-in alert evaluation; the de-facto open-source standard, especially in Kubernetes.
- **Gorilla / InfluxDB / TimescaleDB** — TSDBs built around heavy time-series compression
  (Facebook's Gorilla paper introduced the delta-of-delta + XOR scheme worth ~1.4 bytes/sample).
- **Datadog / CloudWatch** — push-based, managed SaaS; agents ship metrics to the vendor.
- **Grafana** — the dashboard layer that queries any of the above and draws the graphs.

---

## Interview angle

Lead with the **time-series data model**: metric name + labels + timestamp + value, and that
labels define unique series (so **cardinality** is the thing that breaks you). Then nail the
**push vs pull** trade-off — it's the signature debate; mention that pull gives a free up/down
health check while push handles short-lived jobs. Show depth with **downsampling + retention**
("resolution vs cost") and the **counter rate** concept (you graph the slope, not the raw value).
Close with **alerting**: the `FOR` duration to prevent flapping, and the sensitive-vs-noisy
trade-off.

**Common follow-ups:**
- "Your `user_id` label has 10M values — what happens?" → cardinality explosion; one series per
  combination melts the TSDB. Keep labels low-cardinality and bounded.
- "How do you store 1M samples/sec affordably?" → columnar TSDB with delta-of-delta + XOR
  compression (~1.5 B/sample), plus downsampling of older data.
- "An alert fires and clears every minute — what's wrong?" → flapping; add/raise the `FOR`
  duration and/or hysteresis so the condition must hold before paging.
- "Push or pull — which and why?" → no universal answer; pull for the health-check signal and
  scrape-rate control, push for ephemeral jobs and NAT traversal; many stacks do both.

---

## Practice → the Go assignment

Now build the core of a TSDB. Go to [`assignment/`](assignment/) and implement, in order:

1. `Record(name, value, tsSec)` — append a sample to an in-memory series (concurrency-safe).
2. `Query(name, from, to, windowSec, agg)` — bucket in-range samples into **tumbling windows**
   and reduce each with `Sum | Avg | Count | Max | Min`. Empty windows emit **no** point.
3. `Rate(name, from, to)` — treat the series as a counter; return per-second increase.

```bash
cd assignment
go test ./...          # red → implement → green
go test -race ./...    # must pass: Record is called from many goroutines
```

The interface is given; you fill in the `// TODO`s. A reference solution is in
[`solution/`](solution/) — try first, peek after.

**Next case study:** [4.15 — Ad Click Aggregation »](../15-ad-click-aggregation/)
