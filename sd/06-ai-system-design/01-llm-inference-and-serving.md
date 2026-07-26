# 6.1 — LLM Inference & Serving

> **Prerequisites:** [2.1 Load Balancing](../02-building-blocks/01-load-balancing.md),
> [2.3 Caching](../02-building-blocks/03-caching.md).
> **Assignment:** [`01-inference-scheduler-assignment/`](01-inference-scheduler-assignment/assignment/)

---

## The problem

You are asked to serve a 70-billion-parameter model to 10,000 concurrent users with a p95
time-to-first-token under 500 ms. Your instinct from every previous system says: measure a single
request, divide, add replicas, put a load balancer in front.

That instinct produces a wrong answer here, for one reason: **an LLM request is not one unit of
work.** It is a short compute-bound phase followed by a long memory-bound phase, and the two have
completely different scaling behaviour. Treat them as one thing and you will size the fleet wrong
by an order of magnitude in either direction.

The second surprise is that **throughput is bounded by memory, not by FLOPs**. The GPU is usually
sitting idle waiting on memory bandwidth. Your job is to keep it fed.

---

## Core idea: prefill and decode are different systems

Generation happens in two phases.

**Prefill** processes the entire prompt at once. Every token attends to every other token, and it
all happens in parallel — one big matrix multiply. This phase is **compute-bound**: it saturates
the GPU's arithmetic units. Prefill cost grows with prompt length. It produces exactly one token,
and its duration *is* your time-to-first-token.

**Decode** generates the rest, one token at a time. Each new token attends to everything before
it. There is no parallelism across tokens — token 501 cannot start until token 500 exists. This
phase is **memory-bandwidth-bound**: for every single token, the GPU must stream the model's
weights through, and doing that for one token wastes almost all of its compute.

```
Prompt: "Summarise this 2,000-word document: ..."

PREFILL                          DECODE
2,000 tokens, one pass           400 tokens, 400 passes
compute-bound, parallel          memory-bound, sequential
~200 ms                          ~4,000 ms
   │                                │
   └─► first token out              └─► the other 399
       (this is your TTFT)              (this is your total latency)
```

Two consequences fall straight out:

1. **Time-to-first-token and total latency are separate SLOs** with separate fixes. TTFT is a
   prefill problem (shorter prompts, prefix caching). Total latency is a decode problem (fewer
   output tokens, faster memory, speculative decoding).
2. **Streaming is not a nicety, it is the product.** Perceived latency is TTFT, not total time.
   A system that streams at 200 ms TTFT feels instant even if it takes 6 seconds to finish.

---

## The KV cache: your real capacity unit

During decode, every new token attends to every previous token. Recomputing those attention keys
and values each step would be quadratic and absurd. So you cache them — the **KV cache**.

This cache is the single most important number in LLM serving, because it lives in GPU memory,
it grows with every generated token, and it is *per concurrent request*.

```
KV cache bytes = 2 (K and V)
               x layers
               x kv_heads x head_dim
               x sequence_length
               x bytes_per_element
```

Worked example — a 70B-class model, 80 layers, 8 KV heads (grouped-query attention), head_dim
128, fp16 (2 bytes):

```
per token = 2 x 80 x 8 x 128 x 2 bytes = 327,680 bytes ≈ 0.33 MB

8,000-token context ≈ 2.6 GB   ← for ONE request
```

An 80 GB A100/H100 holds ~140 GB of weights for a 70B model in fp16 — it does not fit, so you are
already sharding across GPUs or quantizing. Say quantization to int8 leaves you ~10 GB of headroom
on each of 8 GPUs. That headroom divided by 2.6 GB is **your concurrency limit: about 30
simultaneous long-context requests.** Not 10,000.

**This is the back-of-envelope interviewers are listening for.** "How many concurrent users per
GPU?" has a real answer, and it is KV-cache arithmetic, not vibes.

Three levers follow immediately:

- **Grouped-query attention (GQA)** — fewer KV heads than query heads. The example above is
  already using it; without GQA that model would use 8x more KV cache.
- **Quantize the cache** — fp8 or int8 KV halves or quarters it, at some quality cost.
- **Shorten context** — this is why RAG's job is to put *few, relevant* chunks in the prompt
  rather than *many, plausible* ones. Retrieval quality is a capacity decision, not just an
  accuracy one.

---

## Continuous batching

Batching many requests together is how you make decode efficient: the weights stream through once
and serve the whole batch. But naive **static batching** waits for a full batch, runs it to
completion, and only then starts the next one:

```
STATIC BATCHING (bad)
req A ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ (300 tokens)
req B ▓▓▓░░░░░░░░░░░░░░░ (30 tokens — done at step 30, then idles 270 steps)
req C ▓▓▓▓▓░░░░░░░░░░░░░ (50 tokens — idles 250 steps)
                        ↑ nothing new can start until A finishes
```

Output lengths vary by 10x, so the whole batch runs at the speed of its longest member and most
slots sit idle.

**Continuous batching** (a.k.a. iteration-level scheduling) instead makes a scheduling decision
*every single decode step*. A finished sequence leaves the batch immediately, and a waiting
request takes its slot:

```
CONTINUOUS BATCHING (good)
slot 1 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓  A
slot 2 ▓▓▓ D D D D E E E E  B done → D admitted → D done → E admitted
slot 3 ▓▓▓▓▓ F F F F F F F  C done → F admitted
                            ↑ the GPU never idles on a finished slot
```

In practice this is a **2–4x throughput improvement** over static batching, and it is the single
biggest architectural win in modern inference servers. It is also what the module assignment asks
you to build.

The subtlety is **admission control**. You cannot admit a request just because a slot is free —
you must also have KV-cache memory for its *projected* length. Over-admit and you hit an
out-of-memory mid-generation, which forces you to either evict (throw away work) or crash. The
scheduler's real job is deciding who to admit given a memory budget, not just who is next.

### Paged attention

The KV cache for a sequence grows unpredictably — you don't know the output length in advance.
Allocating a contiguous block for the worst case wastes enormous memory to internal fragmentation.

**Paged attention** borrows the OS virtual-memory trick: allocate the KV cache in fixed-size
blocks (say 16 tokens each), with a block table mapping logical positions to physical blocks.
Blocks need not be contiguous. Fragmentation drops from ~60–80% waste to a few percent, and as a
bonus, **prefix sharing becomes free** — two requests with the same system prompt can point at the
same physical blocks instead of duplicating them.

This is the core innovation behind vLLM, and it is why prefix caching is cheap on modern servers.

### Prefill/decode disaggregation

Since prefill is compute-bound and decode is memory-bound, running them on the same GPU means each
phase interferes with the other — a long prefill stalls every in-flight decode, spiking p95 latency
for everyone (the "head-of-line blocking" problem, dressed in new clothes).

**Disaggregation** runs them on separate worker pools sized independently, shipping the KV cache
between them. It costs a transfer but lets you tune each pool for its own bottleneck. This is what
NVIDIA Dynamo and recent vLLM/SGLang deployments do at scale.

---

## The other levers

| Technique | What it does | Cost |
|---|---|---|
| **Quantization** (int8, fp8, int4) | Shrinks weights and/or KV cache; more fits, memory bandwidth goes further | Small, measurable quality loss — you must eval it, not assume it |
| **Speculative decoding** | A small "draft" model proposes k tokens; the big model verifies them in one pass | 2–3x decode speedup when the draft is good; wasted work when it is not |
| **Prefix caching** | Reuse KV for a shared prompt prefix (system prompt, few-shot examples) | Near-free with paged attention; huge for chat and agents |
| **Distillation / smaller model** | Use a 7B where a 70B is overkill | Requires an eval set to prove it is good enough — see 6.6 |
| **Structured output constraints** | Grammar-constrained decoding for JSON | Slight throughput cost, eliminates a whole class of parse failures |

The engineering judgment interviewers probe: **when is a smaller model the right answer?** Very
often. A distilled 7B serving 90% of traffic with a 70B fallback for hard cases is usually cheaper
*and* faster than a 70B everywhere — but you can only make that call if you have an eval set that
tells you which 10% is hard.

---

## Serving stacks in the wild (2026)

- **vLLM** — the most widely deployed open-source server. Originated paged attention; strong
  continuous batching, prefix caching, broad model support.
- **SGLang** — strong on structured generation and complex multi-call programs; RadixAttention for
  aggressive prefix reuse.
- **TensorRT-LLM** — highest raw throughput on NVIDIA hardware; heavier to operate, compile-ahead
  model engines.
- **NVIDIA Dynamo** — distributed serving with prefill/decode disaggregation as a first-class
  concept.
- **Managed APIs** (Anthropic, OpenAI, Bedrock, Vertex) — you rent all of the above. The design
  conversation shifts from GPU memory to **rate limits, cost, routing, and fallback** — which is
  [6.5](05-model-gateway-routing-and-cost.md).

---

## Trade-offs & key takeaways

- **Prefill ≠ decode.** Compute-bound vs memory-bound, parallel vs sequential, TTFT vs total
  latency. Almost every serving decision follows from this split.
- **The KV cache is your capacity unit.** Concurrency per GPU = free memory ÷ KV per request.
  Learn to do this arithmetic out loud.
- **Continuous batching is the default**, not an optimisation. Static batching wastes 50–75% of
  your fleet.
- **Admission control is the hard part** — a slot being free does not mean memory is available.
- **Streaming changes the SLO that matters.** Optimise TTFT for perceived speed, total tokens for
  cost.
- **Cost is a design constraint, not an afterthought.** Per-request cost scales with tokens, and
  tokens are something your architecture controls (prompt size, retrieval quality, output limits).

---

## Interview angle

**"Design an LLM-powered support assistant for 10,000 concurrent users."**

Do not start with the model. Start with:

1. **Traffic shape.** Concurrent *users* is not concurrent *requests* — a chat user is idle most
   of the time. 10,000 users at a 5% duty cycle is ~500 in-flight requests.
2. **Token budget per request.** Say 3,000-token prompt (system + retrieved context + history),
   400-token response.
3. **KV math.** 3,400 tokens x 0.33 MB ≈ 1.1 GB per in-flight request. 500 in-flight ≈ 550 GB of
   KV cache. That is your GPU fleet size, and it is the number that decides the budget.
4. **Now the levers.** Shorter context via better retrieval. Prefix caching for the shared system
   prompt. A smaller model for the 80% of tickets that are FAQ lookups. Quantized KV.
5. **Then the SLOs.** TTFT via streaming and prefill priority; total latency via output caps.

**Follow-ups you should expect:**
- *"p95 TTFT just doubled and nothing was deployed."* → Someone's prompts got longer (a new
  retrieval config, a bigger system prompt), so prefill got heavier. Or a long-prefill request is
  head-of-line blocking decode — the case for disaggregation.
- *"How do you handle a traffic spike?"* → Queue with admission control and shed load explicitly.
  You cannot autoscale GPUs in seconds; cold start is minutes. Degrade to a smaller model rather
  than failing.
- *"Cut cost by half."* → Smaller model for the easy majority (needs an eval set), shorter
  context (needs better retrieval), cache aggressively (needs a semantic cache), cap output length.

**How this shows up in an SDET loop:** you will be asked to *test* this. Capacity tests that
target KV-cache exhaustion rather than CPU. Latency tests that separate TTFT from total. Load
tests whose "requests per second" is meaningless without a token distribution — a load test with
uniform 100-token prompts tells you nothing about production with 4,000-token RAG prompts. See
[6.6](06-evaluating-and-observing-ai-systems.md) and the
[`aiqa/`](../../sdet/src/main/java/ra/hul/sdet/aiqa/) problems.

---

## Self-check

1. Why is TTFT a prefill problem and total latency a decode problem?
2. A model has 40 layers, 8 KV heads, head_dim 128, fp16. How much KV cache does a 4,000-token
   conversation use? How many fit in 20 GB of spare GPU memory?
3. Why does static batching waste so much capacity, and what exactly does continuous batching
   change?
4. What breaks if you admit a request whenever a batch slot is free?
5. What does paged attention borrow from operating systems, and what does it buy you beyond
   reduced fragmentation?
6. Your p95 TTFT is fine but p99 is terrible. What is the most likely cause?
7. Why might a 7B model be the *right* answer for a system with a 70B budget?

---

## Practice → the coding assignment

Build the scheduler: [`01-inference-scheduler-assignment/`](01-inference-scheduler-assignment/assignment/)

```bash
cd sd/06-ai-system-design/01-inference-scheduler-assignment/assignment
go test ./...
```

You will implement KV-cache accounting, a continuous-batching scheduler that admits and evicts
against a memory budget, and the capacity math above as executable code.

**Next:** [6.2 — Embeddings & Vector Search »](02-embeddings-and-vector-search.md)
