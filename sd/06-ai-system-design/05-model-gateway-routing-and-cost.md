# 6.5 — Model Gateway: Routing, Caching & Cost

> **Prerequisites:** [2.2 Reverse Proxy & CDN](../02-building-blocks/02-reverse-proxy-cdn.md),
> [2.11 Rate Limiting](../02-building-blocks/11-rate-limiting.md),
> [3.5 Fault Tolerance](../03-distributed-systems/05-fault-tolerance.md).
> **Assignment:** [`05-model-gateway-assignment/`](05-model-gateway-assignment/assignment/)

---

## The problem

Forty services in your company call model providers directly. Then, in one week:

- A provider has a two-hour outage. Every one of the forty is down.
- Someone ships a prompt with a runaway loop and burns $40,000 overnight. Nobody notices for
  three days because spend is spread across forty invoices.
- A better/cheaper model ships. Migrating means forty pull requests across nine teams.
- Legal asks which customer data goes to which provider. Nobody can answer.

Every one of these is the same architectural mistake: **no seam between your application and the
model provider.** The gateway is that seam — and if you have ever built an API gateway
([1.6](../01-networking-and-communication/06-api-gateway-design.md)), you already know the shape.

---

## Core idea

One internal service that every caller goes through:

```
services ──► MODEL GATEWAY ──► providers (Anthropic / OpenAI / Bedrock / self-hosted)
                  │
                  ├── auth, per-tenant quotas
                  ├── routing (which model for this request?)
                  ├── caching (exact + semantic)
                  ├── retries, fallback chains, circuit breakers
                  ├── token accounting & budget enforcement
                  ├── PII redaction / policy checks
                  └── tracing, cost attribution, eval hooks
```

The value is not abstraction for its own sake. It is that **every cross-cutting concern gets one
implementation and one place to change it** — which is exactly the argument for an API gateway,
with two new concerns bolted on: *cost is per-request and variable*, and *responses are
non-deterministic*.

---

## Routing

Not every request needs your best model. Routing by difficulty is usually the single largest cost
lever available.

**Static routing** — map task type to model. `classification → small`, `summarise → mid`,
`code-review → large`. Crude, transparent, effective, and easy to reason about. Start here.

**Cascade** — try the cheap model, check confidence or validate the output, escalate on failure.
Works well when validation is cheap and objective (schema conforms? classifier abstained?). Costs
double on escalation, so it only pays when the small model handles a clear majority.

**Learned routing** — a classifier predicts which model suffices. Best savings, most machinery,
and it needs an eval set to train and to keep honest.

The honest framing for an interview: **routing is only safe if you can measure quality per route.**
Without an eval set, "route 80% to the small model" is a guess that silently degrades the product.
This is the point where [6.6](06-evaluating-and-observing-ai-systems.md) stops being optional.

---

## Caching

**Exact-match cache** — hash `(model, prompt, params)` → response. Trivially correct, and hit
rates are better than people expect: system prompts, retries, repeated FAQs, and identical
evaluation runs.

**Semantic cache** — embed the query; if a previous query is within a similarity threshold, return
its cached answer.

Semantic caching is genuinely dangerous and worth being able to discuss precisely, because the
failure is silent:

```
"How do I cancel my subscription?"      → cached answer
"How do I cancel my subscription?"      → HIT  ✓
"How do I NOT cancel my subscription?"  → HIT at 0.94 similarity  ✗  wrong answer
```

Embeddings are weak at negation, small numeric differences, and named entities — exactly the
distinctions that flip an answer. Controls:

- **Conservative thresholds** (0.97+ rather than 0.90), tuned against a labelled set.
- **Never cache personalised or authorised content across users.** Cache key must include the
  authorisation scope, or you have built a data-leak machine.
- **Short TTLs** where the underlying data moves.
- **Exclude high-stakes intents** entirely — medical, legal, financial, destructive actions.
- **Measure it**: sample hits and check them against a fresh generation. A semantic cache without
  a hit-quality metric is unmonitored risk.

The prefix caching from [6.1](01-llm-inference-and-serving.md) is a third, different thing —
provider-side reuse of the KV cache for a shared prompt prefix. It is safe (it changes cost, not
output) and you should structure prompts to exploit it: stable content first, variable content
last.

---

## Reliability

Providers have outages, rate limits, and latency spikes. The gateway is where you survive them.

- **Retries with exponential backoff and jitter** — but only on retryable errors. Retrying a
  content-policy rejection just burns money.
- **Fallback chains** — `primary → secondary provider → smaller model → cached/degraded answer`.
  Crucially, a fallback must be *semantically acceptable*, not merely available. Falling back to a
  model that produces a different output schema breaks the caller in a new way.
- **Circuit breakers** per provider. When one is failing, stop sending traffic; do not queue it
  up.
- **Timeouts that account for streaming.** A 30-second total timeout is wrong for a request whose
  first token arrives in 400 ms and streams for 40 seconds. Time out on *time-to-first-token* and
  on *inter-token gap*, not just total duration.
- **Idempotency keys** so a client retry does not double-charge.

---

## Cost control

The gateway is the only place that can see and shape total spend.

**Token accounting.** Record input tokens, output tokens, cached tokens, model, tenant, feature,
and trace ID for every call. Cost attribution is impossible to retrofit.

**Budgets and quotas.** Per tenant, per feature, per environment. Hard caps on non-production —
the $40,000 overnight bill is almost always a runaway loop in a test environment.

**The estimate that matters in an interview:**

```
1M requests/day
  prompt  3,000 tokens  (system + RAG context + history)
  output    400 tokens

input  : 1M x 3,000 = 3.0B tokens/day
output : 1M x   400 = 0.4B tokens/day

At $3 / $15 per million tokens (in/out):
  input  3,000 x $3  = $9,000/day
  output   400 x $15 = $6,000/day
  ────────────────────────────────
  ~$15,000/day  ≈  $5.5M/year
```

Now the levers become concrete, and each traces back to an earlier lesson:
- Trim context from 3,000 → 1,200 tokens via better retrieval ([6.3](03-rag-architecture.md)):
  **saves ~$5,400/day**.
- Route 70% to a model 10x cheaper: **saves ~$9,000/day**.
- 25% cache hit rate: **saves ~$3,750/day**.
- Prompt-prefix caching on the stable system prompt: often 50–90% off the *input* portion of
  cached calls.

Being able to produce this table on a whiteboard, and to say which lever you would pull first and
what it costs in quality, is the core of the "cost" follow-up.

---

## Governance

- **PII redaction** before egress, if policy requires it.
- **Data residency and retention** — which provider, which region, is data retained for training?
  Enterprise agreements differ, and legal will ask.
- **Prompt and config versioning.** A prompt is code. It needs review, versioning, staged rollout,
  and rollback. Deploying a prompt change to 100% of traffic with no rollback path is the AI
  equivalent of a Friday-night schema migration.
- **Feature flags per model version**, so you can shift traffic gradually and compare.

---

## Trade-offs & key takeaways

- **The gateway is a seam**, and the case for it is the API-gateway case plus variable cost plus
  non-determinism.
- **Routing is the biggest cost lever**, and it is only safe with an eval set.
- **Semantic caching fails silently.** Conservative thresholds, authorisation-scoped keys,
  excluded intents, measured hit quality.
- **Fallbacks must be semantically acceptable**, not just reachable.
- **Time out on TTFT and inter-token gap**, not total duration.
- **Cost attribution must be built in from day one.**
- **Prompts are code**: versioned, reviewed, staged, rollback-able.

The counter-argument, which you should raise yourself: a gateway is a **single point of failure**
and a latency hop. It must be simpler and more reliable than the things behind it. If it grows
business logic, you have rebuilt the monolith in the worst possible place.

---

## Interview angle

**"40 services call LLM providers directly. Design what replaces that."**

1. **Name the four failure modes** from the top of this lesson — they justify the whole design.
2. **Thin gateway**, stateless, horizontally scaled, with a stable internal API that outlives any
   provider.
3. **Routing** — static tiers to start, with per-route eval coverage before any aggressive
   downshift.
4. **Caching** — exact by default; semantic only for well-understood intents, with the safety
   controls named explicitly.
5. **Reliability** — retries on retryable errors only, per-provider circuit breakers, an explicit
   fallback chain, streaming-aware timeouts.
6. **Cost** — full token accounting, per-tenant budgets, hard caps in non-prod, and the
   back-of-envelope above.
7. **Migration** — you cannot big-bang 40 services. Shim the SDK, move the highest-spend services
   first, run in shadow mode to compare before cutting over.
8. **Risks** — SPOT failure, added latency, scope creep. Say them before you are asked.

**Follow-ups:**
- *"Provider is down. What happens?"* → Circuit breaker opens, fallback chain engages, degraded
  responses are labelled as such. Do not silently serve a worse answer as if it were normal.
- *"Costs tripled and nobody knows why."* → That is the pre-gateway state. Post-gateway, you query
  spend by tenant/feature/model/prompt-version and find it in minutes.
- *"A user saw someone else's answer."* → Semantic cache without authorisation in the key. Now
  explain how you would have caught it.

**How this shows up in an SDET loop:** the gateway is the most testable component in an AI stack
because it is *deterministic* — routing rules, cache-key construction, fallback ordering, budget
enforcement and redaction are all ordinary logic with ordinary assertions. It is also the natural
place to inject faults for testing everything downstream of it.

---

## Self-check

1. Give four concrete failures that a model gateway prevents.
2. Why is routing unsafe without an eval set?
3. Give a query pair that a semantic cache gets dangerously wrong, and explain why embeddings do.
4. What must be in a semantic cache key besides the query?
5. Why is a total-duration timeout wrong for a streaming response?
6. What does "a fallback must be semantically acceptable" mean in practice?
7. At 1M requests/day with 3,000-token prompts, which cost lever would you pull first and what
   does it cost you?

---

## Practice → the coding assignment

[`05-model-gateway-assignment/`](05-model-gateway-assignment/assignment/)

```bash
cd sd/06-ai-system-design/05-model-gateway-assignment/assignment
go test ./...
```

You will implement tiered routing, a fallback chain with circuit breaking, an
authorisation-scoped semantic cache, and token-budget enforcement.

**Next:** [6.6 — Evaluating & Observing AI Systems »](06-evaluating-and-observing-ai-systems.md)
