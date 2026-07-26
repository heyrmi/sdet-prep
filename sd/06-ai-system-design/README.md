# Module 6 — AI System Design

> **The round that didn't exist three years ago.** Generative-AI system design is now a standard
> part of senior loops — sometimes as its own round, more often folded into the system-design
> round as "…and now the product has an AI feature." The distributed-systems fundamentals from
> Modules 0–3 still carry you, but the failure modes, the cost model, and the correctness story
> are all different.

---

## Why this module exists

A conventional service is deterministic, cheap per request, and either right or wrong. An
LLM-backed service is none of those:

| | Conventional service | LLM-backed service |
|---|---|---|
| **Determinism** | Same input → same output | Same input → *different* output |
| **Latency** | Milliseconds, roughly flat | Seconds, proportional to output length |
| **Cost per request** | Fractions of a cent, ~flat | Cents to dollars, scales with tokens |
| **Correctness** | Assertable | Graded, on a distribution |
| **Failure mode** | 500, timeout | Confident, fluent, wrong |
| **Capacity unit** | CPU / connections | GPU memory, dominated by KV cache |

That last row is the one candidates miss. You do not scale an inference service by adding
replicas until the CPU graph looks calm — you scale it by understanding what is consuming GPU
memory, and almost all of it is the KV cache.

And the row that matters most for *you*: **"confident, fluent, wrong"** is a QA problem with no
precedent in ordinary testing. That is why this module is followed by
[`sdet/aiqa/`](../../sdet/src/main/java/ra/hul/sdet/aiqa/) — the testing side of the same coin.

---

## The lessons

| # | Lesson | Core concepts | Assignment |
|---|--------|--------------|-----------|
| 6.1 | [LLM Inference & Serving](01-llm-inference-and-serving.md) | tokens, prefill vs decode, KV cache, continuous batching, paged attention, quantization, speculative decoding, GPU capacity math | [`01-inference-scheduler-assignment/`](01-inference-scheduler-assignment/assignment/) — continuous batching scheduler with KV-cache admission control |
| 6.2 | [Embeddings & Vector Search](02-embeddings-and-vector-search.md) | embedding models, ANN indexes (HNSW, IVF-PQ), recall/latency/memory trade-offs, pgvector vs dedicated stores, filtering, re-embedding migrations | — |
| 6.3 | [RAG Architecture](03-rag-architecture.md) | chunking, offline index pipeline vs online query pipeline, hybrid retrieval, Reciprocal Rank Fusion, cross-encoder reranking, context assembly, citations | [`03-rag-retrieval-assignment/`](03-rag-retrieval-assignment/assignment/) — BM25 + vector hybrid search, RRF, reranking, context budgeting |
| 6.4 | [Agents, Tool Calling & MCP](04-agents-tool-calling-and-mcp.md) | the agent loop, tool schemas, supervisor/worker topologies, state & memory handoff, context saturation, MCP, human-in-the-loop, blast radius | — |
| 6.5 | [Model Gateway: Routing, Caching & Cost](05-model-gateway-routing-and-cost.md) | multi-provider abstraction, model routing, fallback chains, semantic caching, token budgets, rate-limit handling, per-tenant quotas | [`05-model-gateway-assignment/`](05-model-gateway-assignment/assignment/) — router with fallback, semantic cache, and token budget enforcement |
| 6.6 | [Evaluating & Observing AI Systems](06-evaluating-and-observing-ai-systems.md) | offline evals vs online monitoring, golden datasets, LLM-as-judge and its biases, drift, tracing, guardrails, the eval-first workflow | — |

### Working the assignments

```bash
cd sd/06-ai-system-design/01-inference-scheduler-assignment/assignment
go test ./...          # red — every function is a TODO
# implement until green; the reference is in ../solution/
```

---

## How they fit together

```
                        6.5 Model Gateway
              (routing, fallback, cache, budget)
                              │
              ┌───────────────┼───────────────┐
              ▼                               ▼
   6.1 Inference & Serving            6.4 Agents & Tool Calling
   (KV cache, batching, GPUs)         (loop, tools, state, MCP)
              ▲                               │
              │ retrieved context             │ tools may retrieve
              │                               │
   6.3 RAG Architecture ◄────────────────────┘
   (chunk, hybrid search, rerank)
              ▲
              │ built on
   6.2 Embeddings & Vector Search
   (ANN indexes, recall vs latency)

   6.6 Evaluation & Observability  ──►  wraps ALL of the above
```

- **6.1** is the physics: what a GPU can actually serve, and why.
- **6.2 → 6.3** is the retrieval stack. In 2026 the bottleneck in a RAG system is almost never
  generation — it is retrieval quality.
- **6.4** is where non-determinism compounds: an agent that is 95% reliable per step is 60%
  reliable over ten steps.
- **6.5** is the production seam — the thing that makes provider outages and cost spikes
  survivable.
- **6.6** closes the loop, and is the natural bridge into the AI testing pillar.

---

## Suggested order

Read **6.1 first** — the capacity model underpins every cost and latency argument you will make
later. Then **6.2 → 6.3** together (they are really one stack). Then **6.4**, then **6.5**.
Finish with **6.6**, and go straight from it into
[`sdet/aiqa/`](../../sdet/src/main/java/ra/hul/sdet/aiqa/) while the vocabulary is fresh.

After each lesson, close the file and do the back-of-envelope out loud. For this module in
particular, interviewers listen for **cost per request** and **GPU memory per concurrent
session** — those two numbers are where hand-waving becomes visible.

**Start here:** [6.1 — LLM Inference & Serving »](01-llm-inference-and-serving.md)
