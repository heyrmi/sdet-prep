# 6.2 — Embeddings & Vector Search

> **Prerequisites:** [2.5 Indexing](../02-building-blocks/05-indexing.md),
> [2.7 Sharding](../02-building-blocks/07-sharding-partitioning.md),
> [6.1 LLM Inference](01-llm-inference-and-serving.md).

---

## The problem

A user searches your 50-million-document knowledge base for *"why did my payment bounce"*. The
document that answers it says *"declined transactions: insufficient funds"* and shares **not one
word** with the query. Keyword search returns nothing useful.

You need retrieval by **meaning**, not by string match. That is what embeddings buy you — and the
bill comes as a new set of failure modes: an index that answers in 200 ms but misses the right
document 30% of the time, and does so silently.

---

## Core idea

An **embedding model** maps text to a fixed-length vector (typically 384–3,072 dimensions) such
that semantically similar text lands nearby in that space. "Payment bounced" and "transaction
declined" end up close; "payment bounced" and "office holiday schedule" end up far apart.

Retrieval is then a nearest-neighbour problem: embed the query, find the k closest document
vectors.

Distance is almost always **cosine similarity** (the angle between vectors, ignoring magnitude).
Most modern embedding models emit normalised vectors, in which case cosine similarity and dot
product are equivalent — and dot product is cheaper. Worth knowing, because it is a real
optimisation and a common interview probe.

---

## Exact search doesn't scale, and that is the whole subject

Brute-force comparison against every vector is exact and trivially correct:

```
50,000,000 docs x 1,536 dims x 4 bytes = 307 GB   ← and every query touches all of it
```

Even in memory, that is hundreds of milliseconds per query. So production uses **Approximate
Nearest Neighbour (ANN)** search, and the word that matters is *approximate*:

> **You are trading recall for latency.** An ANN index will miss some true neighbours. How many is
> a tuning parameter, and — critically — it is invisible unless you measure it.

This is the single most important thing to say in an interview about vector search, and it is the
thing that makes vector search a *testing* problem: a keyword index that breaks returns nothing
and you notice; a vector index tuned too aggressively returns *plausible but wrong* results and
nobody notices for months.

### The index families

**HNSW (Hierarchical Navigable Small World)** — a layered proximity graph. Search enters at a
sparse top layer, greedily walks toward the query, and descends. The default choice: excellent
recall/latency, supports incremental inserts.
- `M` — edges per node. Higher = better recall, more memory.
- `ef_construction` — build-time search width. Higher = better graph, slower build.
- `ef_search` — query-time search width. **The runtime recall/latency dial.**
- Memory: roughly `vectors + M x 8 bytes x n`. It is RAM-hungry — budget for it.

**IVF (Inverted File)** — cluster vectors into `nlist` buckets via k-means; search only the
`nprobe` nearest buckets. Lower memory than HNSW, but recall suffers when the true neighbour sits
just across a cluster boundary. Needs a training step on representative data.

**PQ (Product Quantization)** — split each vector into sub-vectors and replace each with a
codebook centroid ID. Compresses 10–50x (that 307 GB becomes ~10 GB) at a real accuracy cost.
Usually combined as **IVF-PQ** for billion-scale corpora.

**Flat (brute force)** — exact, no tuning, no surprises. Genuinely correct up to a few hundred
thousand vectors. Do not skip past this in an interview: *"at 200k documents I'd use exact search
and spend the complexity budget elsewhere"* is a strong answer, not a lazy one.

| | Flat | HNSW | IVF-PQ |
|---|---|---|---|
| Recall | 100% | 95–99% | 80–95% |
| Latency @1M | ~100 ms | ~1–5 ms | ~2–10 ms |
| Memory @1M x 768d | 3 GB | 4–5 GB | 0.3 GB |
| Incremental inserts | trivial | good | needs retraining |
| Best at | <500k | most cases | >100M |

---

## Choosing a store

**pgvector** — vectors as a Postgres column type. For most teams under roughly 5–10 million
vectors this is the right answer, and the reasoning is not about vectors at all: your metadata,
your permissions, and your transactional guarantees are already in Postgres. Keeping one system
beats keeping two in sync.

**Dedicated stores** (Qdrant, Weaviate, Milvus, Pinecone) — earn their place when you need
billion-scale, sophisticated pre-filtering, distributed sharding, or managed operations.

**Search engines** (Elasticsearch, OpenSearch) — already run BM25, and now do vectors too. If you
need hybrid search (you do — see [6.3](03-rag-architecture.md)) and already operate one, this is
often the pragmatic choice.

**The build-vs-buy answer interviewers want:** start with what you already run. A second database
has an operational cost that is easy to underestimate and hard to unwind.

---

## The three problems that actually bite

### 1. Filtering interacts badly with ANN

"Find documents about payments **that this user is allowed to see**." Two wrong ways:

- **Post-filter**: retrieve top-100, then drop unauthorised ones. If the user can see 1% of the
  corpus, you return ~1 result. Recall collapses silently.
- **Pre-filter then brute force**: correct, but throws away the index.

Real stores implement **filtered ANN** — traversing the graph while respecting the predicate.
Support and quality vary a lot between engines, and this is a genuine selection criterion. For
high-selectivity filters (tenant ID in a multi-tenant system), **partition the index per tenant**
instead — it sidesteps the problem and helps isolation and deletion.

If you take one thing from this lesson into an interview: **"how does your filter interact with
your index?"** is the question that separates people who have run vector search from people who
have read about it.

### 2. Chunking is a retrieval decision, not a preprocessing detail

Embeddings have a fixed context; documents do not. So you chunk. And chunking determines what can
ever be retrieved:

- **Too large** — one vector averages several topics, matching everything weakly and nothing well.
- **Too small** — a chunk loses the context that made it meaningful ("it costs $40" — what does?).
- **Split mid-sentence or mid-table** — the chunk is unusable no matter how good the model.

Practical defaults: 200–500 tokens with 10–20% overlap, split on structural boundaries (headings,
paragraphs) rather than character counts. **Small-to-big** is a strong pattern: embed small precise
chunks for matching, but return the larger parent section as context.

### 3. Re-embedding is a migration, and nobody plans for it

Embeddings from different models are **not comparable**. Upgrading your embedding model means
re-embedding the entire corpus. At 50M documents that is a real batch job with real cost, and
during the transition you have two incompatible index versions.

Design for this on day one: **version your embeddings**, store the model ID alongside every
vector, and build the index pipeline so it can backfill a new version alongside the old, then cut
over. It is the schema-migration problem wearing a hat, and treating it as one is the mature
answer.

---

## Trade-offs & key takeaways

- **ANN trades recall for latency, silently.** Measure recall@k against an exact baseline, or you
  do not know what your retrieval is doing.
- **HNSW by default; Flat under ~500k; IVF-PQ at 100M+.**
- **`ef_search` is your runtime dial** — it lets you trade latency for recall per query, without
  reindexing.
- **Filtering is where vector stores differ most.** Ask about it early.
- **Chunking sets the ceiling on retrieval quality.** No reranker recovers from bad chunks.
- **Embeddings are versioned data.** Plan the re-embedding migration before you need it.
- **Vector search alone is not enough** — exact terms (error codes, SKUs, names) need lexical
  matching, which is [6.3](03-rag-architecture.md).

---

## Interview angle

**"Design semantic search over 50M internal documents with per-user permissions."**

Expected shape:
1. **Chunk** (300 tokens, structural boundaries, small-to-big), yielding perhaps 250M chunks.
2. **Embed** in batch, versioned by model ID. Back-of-envelope the one-time cost and the ongoing
   delta cost — interviewers want to see you price it.
3. **Index**: at 250M vectors, IVF-PQ or a sharded HNSW. State the memory estimate.
4. **Permissions**: partition per tenant/org, and use filtered ANN within it. Explicitly reject
   post-filtering and explain why.
5. **Freshness**: an event-driven pipeline on document change; state the staleness SLO.
6. **Measure**: recall@10 versus an exact baseline on a golden query set, tracked over time —
   otherwise a tuning change silently degrades quality.

**Follow-ups:**
- *"Results feel worse since last month, nothing changed."* → Corpus drifted; your ANN parameters
  were tuned for the old distribution. Or an index rebuild used different settings. This is why
  you keep a golden set.
- *"Someone searched an exact error code and got nothing."* → Embeddings are bad at rare exact
  tokens. You need hybrid retrieval.
- *"Cut memory by 80%."* → PQ compression, smaller dimensions (many models support Matryoshka
  truncation), or offload cold shards.

**How this shows up in an SDET loop:** retrieval quality is measurable and therefore testable —
recall@k, precision@k, MRR, NDCG against a labelled set. That is exactly what
[`Ques4_RetrievalQualityMetrics`](../../sdet/src/main/java/ra/hul/sdet/aiqa/) implements.

---

## Self-check

1. Why is cosine similarity usually equivalent to dot product in practice?
2. You have 300k documents. Why might Flat be the correct choice?
3. What does `ef_search` control, and why is it the useful dial?
4. Why does post-filtering destroy recall for a highly selective filter?
5. Why can't you compare embeddings from two different models?
6. Give two failure modes caused purely by chunking.
7. How would you detect that your ANN index has quietly lost recall?

---

**Next:** [6.3 — RAG Architecture »](03-rag-architecture.md)
