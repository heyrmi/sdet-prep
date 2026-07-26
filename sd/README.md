# The System Design Course (Free & Open, Go-based)

> A from-scratch, best-in-class system design curriculum — built to rival the paid
> ByteByteGo course — with **lessons in Markdown** and **hands-on coding assignments in Go**.
> Researched and synthesized from the
> [System Design Primer](https://github.com/donnemartin/system-design-primer),
> [System Design 101](https://github.com/ByteByteGoHq/system-design-101),
> the ByteByteGo Vol. 1 & 2 chapter structure, and primary engineering sources.

---

## Who this is for

You can write code, but **distributed systems, databases-at-scale, and "how do big systems
actually work" are new to you.** Every lesson builds from first principles with analogies
before introducing jargon. You don't need prior systems knowledge — only the willingness to
read carefully and *write code*.

The single most important idea in this whole course:

> **Everything is a trade-off.** There is no "best" database, cache, or architecture — only
> the right choice *given your constraints* (read/write ratio, latency budget, consistency
> needs, scale, team size, money). Senior engineers don't memorize answers; they reason about
> trade-offs out loud. That's the skill we're building.

---

## How the course works

```
sd/
├── README.md                         ← you are here (curriculum + study plan)
├── 00-foundations/                   ← how to think, how to estimate, the interview framework
├── 01-networking-and-communication/  ← how machines talk: IP/DNS/TCP, HTTP, RPC, gRPC, WebSockets
├── 02-building-blocks/               ← the LEGO bricks: LB, cache, DB, sharding, queues, hashing...
├── 03-distributed-systems/           ← the hard parts: consensus, storage engines, transactions
├── 04-case-studies/                  ← put it together: ~design real systems end-to-end
│   └── NN-system-name/
│       ├── README.md                 ← the design lesson (requirements → estimate → design → deep dive)
│       ├── assignment/               ← Go starter code + a test suite that defines "done"
│       └── solution/                 ← reference Go solution (peek only after you try!)
├── 05-sdet-system-design/            ← test-flavored system design for the SDET loop
├── 06-ai-system-design/              ← LLM serving, RAG, agents, model gateway, evals
├── 07-testing-distributed-systems/   ← how you would actually VERIFY modules 03 & 04
└── GLOSSARY.md                       ← every term, one-line definition
```

Modules 05, 06 and 07 follow the same shape as 02 and 04: lessons plus `assignment/` +
`solution/` Go exercises.

**Each lesson MD follows the same shape**, so you always know where you are:

1. **The problem** — what real-world pain this concept solves (with an analogy)
2. **Core idea** — the concept from first principles
3. **How it works** — mechanics, diagrams (ASCII), variations
4. **Trade-offs** — when to use it, when *not* to, what it costs
5. **In the wild** — how real companies use it
6. **Interview angle** — how this shows up in interviews + common follow-ups
7. **Practice** — pointer to the Go assignment + self-check questions

### The Go assignments

You learn systems by *building the primitives*, not by reading about them. Each assignment:

- Ships with a `go.mod`, **starter code** (with `// TODO:` markers), and a **test suite**.
- Tests define "done" — run `go test ./...` until green.
- Has a **reference solution** in `solution/`. Try first; the struggle is the learning.

```bash
cd 04-case-studies/01-rate-limiter/assignment
go test ./...          # red → write code → green
```

You'll implement real things: an LRU cache, a token-bucket rate limiter, consistent hashing,
a Snowflake ID generator, a write-ahead log, a Raft-lite leader election, an LSM-tree memtable,
a URL shortener service, a sharded key-value store, and more.

---

## The curriculum

### Module 0 — Foundations (*start here*)
| # | Lesson | You'll be able to... |
|---|--------|----------------------|
| 0.1 | [Scale from zero to millions of users](00-foundations/01-scale-zero-to-millions.md) | Evolve a single server into a scalable architecture, one bottleneck at a time |
| 0.2 | [Numbers every engineer should know](00-foundations/02-numbers-every-engineer-should-know.md) | Reason about latency, throughput, and the powers of two |
| 0.3 | [Back-of-the-envelope estimation](00-foundations/03-back-of-envelope-estimation.md) | Estimate QPS, storage, and bandwidth for any system |
| 0.4 | [A framework for system design interviews](00-foundations/04-interview-framework.md) | Drive any design question with a repeatable 4-step method |

### Module 1 — Networking & Communication
| # | Lesson |
|---|--------|
| 1.1 | [How the internet works: IP, DNS, TCP & UDP](01-networking-and-communication/01-internet-ip-dns-tcp-udp.md) |
| 1.2 | [HTTP, HTTPS & REST APIs](01-networking-and-communication/02-http-https-rest.md) |
| 1.3 | [RPC and gRPC](01-networking-and-communication/03-rpc-grpc.md) |
| 1.4 | [GraphQL vs REST](01-networking-and-communication/04-graphql.md) |
| 1.5 | [Real-time: polling, SSE & WebSockets](01-networking-and-communication/05-polling-sse-websockets.md) |
| 1.6 | [API gateways & API design](01-networking-and-communication/06-api-gateway-design.md) |

### Module 2 — Building Blocks
| # | Lesson |
|---|--------|
| 2.1 | [Load balancing](02-building-blocks/01-load-balancing.md) |
| 2.2 | [Reverse proxies & CDNs](02-building-blocks/02-reverse-proxy-cdn.md) |
| 2.3 | [Caching (strategies, eviction, pitfalls)](02-building-blocks/03-caching.md) |
| 2.4 | [Databases: SQL vs NoSQL](02-building-blocks/04-sql-vs-nosql.md) |
| 2.5 | [Database indexing & storage](02-building-blocks/05-indexing.md) |
| 2.6 | [Replication](02-building-blocks/06-replication.md) |
| 2.7 | [Sharding & partitioning](02-building-blocks/07-sharding-partitioning.md) |
| 2.8 | [Consistent hashing](02-building-blocks/08-consistent-hashing.md) |
| 2.9 | [CAP, PACELC & consistency models](02-building-blocks/09-cap-pacelc-consistency.md) |
| 2.10 | [Message queues & event streaming (Kafka)](02-building-blocks/10-message-queues-streaming.md) |
| 2.11 | [Rate limiting](02-building-blocks/11-rate-limiting.md) |
| 2.12 | [Distributed unique IDs](02-building-blocks/12-unique-id-generation.md) |
| 2.13 | [Probabilistic data structures (Bloom, HLL, Count-Min)](02-building-blocks/13-probabilistic-structures.md) |
| 2.14 | [Idempotency & exactly-once](02-building-blocks/14-idempotency.md) |

### Module 3 — Distributed Systems Deep Dives
| # | Lesson |
|---|--------|
| 3.1 | [Replication & consensus (Raft, Paxos)](03-distributed-systems/01-consensus-raft-paxos.md) |
| 3.2 | [Storage engines: B-Tree vs LSM-Tree](03-distributed-systems/02-storage-engines.md) |
| 3.3 | [Distributed transactions (2PC, Saga, outbox)](03-distributed-systems/03-distributed-transactions.md) |
| 3.4 | [Coordination & leader election](03-distributed-systems/04-coordination-leader-election.md) |
| 3.5 | [Failure, redundancy & fault tolerance](03-distributed-systems/05-fault-tolerance.md) |
| 3.6 | [Observability: logs, metrics, traces](03-distributed-systems/06-observability.md) |

### Module 4 — Case Studies (design + build)
Each is a full design lesson **plus** a Go assignment.

| # | System | Core concepts exercised |
|---|--------|--------------------------|
| 4.1 | [Rate Limiter](04-case-studies/01-rate-limiter/) | token bucket, sliding window, distributed counters |
| 4.2 | [Consistent Hashing / Key-Value Store](04-case-studies/02-key-value-store/) | partitioning, replication, quorum, vector clocks |
| 4.3 | [Unique ID Generator](04-case-studies/03-unique-id-generator/) | Snowflake, clock skew, coordination |
| 4.4 | [URL Shortener](04-case-studies/04-url-shortener/) | hashing, base62, read-heavy caching |
| 4.5 | [Web Crawler](04-case-studies/05-web-crawler/) | BFS at scale, politeness, dedup, frontier |
| 4.6 | [Notification System](04-case-studies/06-notification-system/) | fan-out, queues, retries, idempotency |
| 4.7 | [News Feed](04-case-studies/07-news-feed/) | fan-out on write vs read, feed ranking |
| 4.8 | [Chat System](04-case-studies/08-chat-system/) | WebSockets, presence, message ordering |
| 4.9 | [Search Autocomplete (Typeahead)](04-case-studies/09-autocomplete/) | tries, top-k, prefix sharding |
| 4.10 | [YouTube / Video Streaming](04-case-studies/10-video-streaming/) | blob storage, transcoding, CDN, adaptive bitrate |
| 4.11 | [Google Drive / File Storage](04-case-studies/11-file-storage/) | chunking, dedup, sync, metadata |
| 4.12 | [Proximity Service (Yelp)](04-case-studies/12-proximity-service/) | geohash, quadtree, spatial indexing |
| 4.13 | [Distributed Message Queue](04-case-studies/13-message-queue/) | logs, offsets, consumer groups, delivery semantics |
| 4.14 | [Metrics Monitoring & Alerting](04-case-studies/14-metrics-monitoring/) | time-series, aggregation, push vs pull |
| 4.15 | [Ad Click Aggregation](04-case-studies/15-ad-click-aggregation/) | stream processing, exactly-once, dedup |
| 4.16 | [Object Storage (S3-like)](04-case-studies/16-object-storage/) | buckets, durability, erasure coding |
| 4.17 | [Payment System](04-case-studies/17-payment-system/) | idempotency, reconciliation, ledgers, exactly-once |

> The course is built to grow. More case studies (Nearby Friends, Google Maps, Hotel
> Reservation, Digital Wallet, Stock Exchange, Distributed Email, Gaming Leaderboard) follow
> the same template and can be added as you progress.

### Module 5 — SDET System Design (design the systems that test the systems)
Test-flavored system design for the senior SDET interview loop. **Every lesson ships a Go
assignment**; see the [module intro](05-sdet-system-design/).

| # | Lesson | Core concepts exercised | Assignment |
|---|--------|--------------------------|-----------|
| 5.1 | [Design a Test Automation Platform](05-sdet-system-design/01-design-a-test-automation-platform.md) | job queue, duration-aware sharding, worker fleet, result store, artifact storage, flake detection, quarantine, shard math | LPT shard balancing, critical path, optimal worker count |
| 5.2 | [Design a CI/CD Pipeline](05-sdet-system-design/02-design-a-ci-cd-pipeline.md) | stage DAG, build-once-promote, caching, test impact analysis, quality gates, canary/blue-green/rollback, secrets | gate engine + **DORA metrics** and performance bands |
| 5.3 | [Design Test Infrastructure at Scale](05-sdet-system-design/03-design-test-infrastructure-at-scale.md) | Selenium Grid, containerized browsers, session routing, ephemeral environments, autoscaling, concurrent-session sizing | expiring leases, health ejection, per-tenant fairness |
| 5.4 | [Design for Testability](05-sdet-system-design/04-design-for-testability.md) | seams & DI, test hooks, fault injection, test data management, contract testing (Pact), chaos & load harnesses | fake clock, seeded PRNG, fault injector, reset registry |
| 5.5 | [Flaky Test Detection & Quarantine](05-sdet-system-design/05-flaky-test-detection-and-quarantine.md) | flakiness taxonomy, flake scoring, quarantine workflow, results schema, flake budgets, ownership & dashboards | same-commit flake scoring, quarantine state machine, impact ranking |

### Module 6 — AI System Design (the round that didn't exist three years ago)
Generative-AI system design is now standard in senior loops. See the
[module intro](06-ai-system-design/).

| # | Lesson | Core concepts exercised | Assignment |
|---|--------|--------------------------|-----------|
| 6.1 | [LLM Inference & Serving](06-ai-system-design/01-llm-inference-and-serving.md) | prefill vs decode, **KV cache capacity math**, continuous batching, paged attention, quantization, speculative decoding | continuous-batching scheduler with KV admission control |
| 6.2 | [Embeddings & Vector Search](06-ai-system-design/02-embeddings-and-vector-search.md) | ANN indexes (HNSW, IVF-PQ), recall/latency/memory trade-offs, filtered ANN, re-embedding migrations | — |
| 6.3 | [RAG Architecture](06-ai-system-design/03-rag-architecture.md) | chunking, hybrid retrieval, **Reciprocal Rank Fusion**, cross-encoder reranking, context assembly, citations | BM25 + vector hybrid search, RRF, rerank, context budgeting |
| 6.4 | [Agents, Tool Calling & MCP](06-ai-system-design/04-agents-tool-calling-and-mcp.md) | the agent loop, tool schemas, supervisor topologies, memory handoff, context saturation, MCP, blast radius | — |
| 6.5 | [Model Gateway: Routing, Caching & Cost](06-ai-system-design/05-model-gateway-routing-and-cost.md) | multi-provider routing, fallback chains, semantic caching, token budgets, cost modelling | router, fallback + circuit breaker, semantic cache, budgets |
| 6.6 | [Evaluating & Observing AI Systems](06-ai-system-design/06-evaluating-and-observing-ai-systems.md) | golden datasets, LLM-as-judge and its biases, eval gates, drift, tracing | → the [`aiqa/`](../sdet/src/main/java/ra/hul/sdet/aiqa/) pillar |

### Module 7 — Testing Distributed Systems (the bridge module)
Module 4 taught you to *design* seventeen distributed systems. This one asks how you would know
if any of them were correct. See the [module intro](07-testing-distributed-systems/).

| # | Lesson | Core concepts exercised | Assignment |
|---|--------|--------------------------|-----------|
| 7.1 | [Consistency Checking & Linearizability](07-testing-distributed-systems/01-consistency-checking-and-linearizability.md) | histories, linearizability, the Wing-Gong search, Elle and cycle detection, indeterminate operations | **a real linearizability checker** with shrinking |
| 7.2 | [Deterministic Simulation Testing](07-testing-distributed-systems/02-deterministic-simulation-testing.md) | seeded schedulers, virtual time, FoundationDB / Antithesis, reproducing a heisenbug from a seed | — |
| 7.3 | [Fault Injection & Chaos](07-testing-distributed-systems/03-fault-injection-and-chaos.md) | fault taxonomy, pauses vs crashes, blast radius, steady-state hypotheses, game days | — |

---

## Suggested study plan

You're newish to backend, so **don't skip Modules 0–2.** They're the vocabulary everything
else assumes.

- **Weeks 1–2:** Module 0 + Module 1. Do the foundations assignments.
- **Weeks 3–5:** Module 2 (building blocks). Build the LRU cache, rate limiter, consistent
  hashing, Snowflake ID. This is where most of the "aha" happens.
- **Weeks 6–7:** Module 3. Heavier; reread as needed.
- **Weeks 8+:** One case study at a time. Read the design, *then* do the Go assignment, *then*
  compare with the solution and ask "what would I have missed in an interview?"

**Golden rule:** after each lesson, close the file and explain the concept out loud as if
teaching a friend. If you can't, reread. This is the single highest-ROI study habit.

---

## Prerequisites & setup

- **Go 1.21+** — [install](https://go.dev/dl/). Check with `go version`.
- A terminal and an editor. That's it. No cloud accounts, no paid services.

```bash
# from any assignment folder:
go mod tidy      # fetch deps (most assignments use only the std lib)
go test ./...    # run the tests that define "done"
go test -race ./... # catch data races (important for concurrency assignments!)
```

---

## How to get the most out of this

1. **Read actively.** Keep a notebook. Sketch the diagrams yourself.
2. **Always do the assignment before reading the solution.** Friction = learning.
3. **Explain trade-offs out loud.** Interviews and real design reviews are conversations.
4. **Revisit.** System design clicks on the second and third pass, not the first.

Let's build. Start with **[Module 0, Lesson 0.1](00-foundations/01-scale-zero-to-millions.md).**
