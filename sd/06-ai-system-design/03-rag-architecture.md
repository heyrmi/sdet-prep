# 6.3 — RAG Architecture

> **Prerequisites:** [6.1 LLM Inference](01-llm-inference-and-serving.md),
> [6.2 Embeddings & Vector Search](02-embeddings-and-vector-search.md).
> **Assignment:** [`03-rag-retrieval-assignment/`](03-rag-retrieval-assignment/assignment/)

---

## The problem

Your model's training data has a cutoff, knows nothing about your company, and cannot cite a
source. Fine-tuning is slow, expensive, and has to be redone every time a document changes.

**Retrieval-Augmented Generation** sidesteps all three: fetch the relevant text at query time and
put it in the prompt. The model does not need to *know* your refund policy — it needs to be handed
it.

The demo takes an afternoon. The production system does not, and the reason is worth stating up
front:

> **In 2026 the bottleneck in a RAG system is retrieval, not generation.** Almost every "the LLM
> hallucinated" bug is really "we retrieved the wrong chunk and the model faithfully summarised
> it." Debug retrieval first, always.

---

## Core idea: two pipelines, not one

The commonest design mistake is treating RAG as a single request path. It is two pipelines with
completely different characteristics.

**Offline (indexing)** — batch, triggered by document-change events. Throughput matters, latency
does not.

```
source docs → parse → chunk → embed → index (vector + lexical)
              ↑
        the step everyone underestimates: PDFs, tables,
        scanned images, HTML cruft, code blocks
```

**Online (query)** — per request, on the user's clock. Every stage has an explicit latency and
cost budget.

```
query → rewrite → hybrid retrieve → rerank → assemble context → generate → cite
        ~50ms      ~30ms             ~80ms    ~5ms              ~2s        —
```

Separating them makes the design tractable: you can rebuild the index without touching the serving
path, and you can change reranking without re-embedding anything.

---

## Stage by stage

### 1. Parsing — the unglamorous majority of the work

Real corpora are PDFs with two-column layouts, tables that lose meaning when flattened, scanned
images needing OCR, and HTML full of navigation chrome. A table rendered as
`Q1 Q2 Q3 100 200 300` is worse than useless — it will retrieve and mislead.

Budget for this. In practice it is where most RAG quality is won or lost, and it is invisible in
every tutorial.

### 2. Chunking

Covered in [6.2](02-embeddings-and-vector-search.md); the summary: 200–500 tokens, 10–20% overlap,
split on structure not characters, and consider **small-to-big** (embed small, return the parent
section). Chunking sets a ceiling that nothing downstream can raise.

### 3. Query rewriting

The user's query is often a bad search query — pronouns, follow-ups, typos, or three questions at
once.

- **Contextualisation**: "what about for enterprise?" → "what is the refund policy for enterprise
  plans?" Essential in chat; without it, turn 2 onward retrieves garbage.
- **Decomposition**: split multi-part questions and retrieve for each.
- **HyDE** (Hypothetical Document Embeddings): have the model draft a hypothetical *answer* and
  embed that instead — answers look more like documents than questions do.

Each costs an extra LLM call, so each is a latency/quality trade. Contextualisation in a chat
product is usually worth it; the others are situational.

### 4. Hybrid retrieval — vector AND lexical

This is the most important architectural point in the lesson.

Vector search finds meaning but is weak on rare exact tokens: error codes (`ERR_4021`), SKUs,
person names, function names. BM25 (lexical) finds those exactly but misses paraphrase entirely.

**Run both in parallel and fuse the results.** Published comparisons put hybrid around **66% MRR
versus ~57% for semantic-only** — a 9-point improvement, which is enormous for a change this
mechanical. Empirically, near enough every production RAG system converges on hybrid within its
first few months.

Fusion is done with **Reciprocal Rank Fusion (RRF)**, which combines ranked lists using only
positions, never raw scores:

```
RRF(d) = Σ  1 / (k + rank_i(d))        k ≈ 60 by convention
        i∈retrievers
```

Why ranks and not scores? Because cosine similarity and BM25 scores are on incomparable scales
that shift with corpus and query. Any weighted-score blend needs constant retuning; RRF needs
none. That property — **no normalisation, no tuning** — is why it is the default, and it is a
great thing to be able to explain.

### 5. Reranking

Retrieval uses a **bi-encoder**: query and document are embedded *independently*, so document
vectors can be precomputed. Fast, scalable, and lossy — the model never sees the pair together.

A **cross-encoder** reranker takes `(query, document)` as one input and runs a full transformer
pass over the pair. Far more accurate, far too slow to run over the corpus.

So you stage it:

```
hybrid retrieve top-100  (fast, bi-encoder + BM25)
         ↓
cross-encoder rerank      (accurate, expensive — 100 pairs, ~50-100 ms)
         ↓
keep top-5                 → into the prompt
```

Retrieve broad, rerank narrow. This two-stage shape is the standard production RAG pattern, and it
also *saves you money*: fewer, better chunks in the prompt means fewer input tokens, which is
directly [6.1](01-llm-inference-and-serving.md)'s KV-cache budget.

### 6. Context assembly

You have a token budget. Decisions that matter:

- **How many chunks?** More is not better. Irrelevant context measurably degrades answers, and
  models attend unevenly across a long context — material in the middle gets the least attention
  ("lost in the middle"). Put the strongest chunks at the edges.
- **Deduplicate.** Overlapping chunks repeat text and waste budget.
- **Include metadata** — title, section, date, URL. It grounds the model and enables citation.
- **Say what to do when nothing is relevant.** If retrieval returns weak matches, instruct the
  model to say it doesn't know. A relevance floor that returns zero chunks is a feature.

### 7. Generation and citation

Require the answer to cite chunk IDs, then **verify the citations programmatically** — check that
each cited ID was actually in the context. This is a cheap, deterministic hallucination check, and
it is the kind of assertion that survives contact with a non-deterministic system.

---

## Advanced patterns (know they exist, know when they don't apply)

| Pattern | Idea | When |
|---|---|---|
| **Agentic RAG** | The model decides *whether* and *what* to retrieve, possibly iterating | Complex multi-hop questions; costs multiple round trips |
| **GraphRAG** | Build an entity/relationship graph; traverse it for retrieval | Questions requiring connections across many documents |
| **Self-correction (CRAG)** | Grade retrieved chunks; re-query or fall back to web if weak | High-stakes answers where a wrong retrieval is expensive |
| **Contextual retrieval** | Prepend an LLM-generated summary of the parent doc to each chunk before embedding | Strong recall gains; adds an indexing-time LLM cost per chunk |
| **Multi-vector** | Several vectors per chunk (summary, questions-it-answers, raw) | When queries and documents are stylistically very different |

The trap is reaching for these before doing hybrid + reranking + decent chunking well. In an
interview, proposing GraphRAG for a FAQ bot reads as pattern-matching rather than judgment.

---

## Trade-offs & key takeaways

- **Two pipelines**, offline and online, with separate budgets.
- **Retrieval is the bottleneck.** Debug it before blaming the model.
- **Hybrid retrieval is table stakes** — vector for meaning, BM25 for exact tokens.
- **RRF fuses by rank**, which is why it needs no score normalisation or tuning.
- **Retrieve broad, rerank narrow.** Cheap recall then expensive precision.
- **Fewer, better chunks** beat more chunks — for quality *and* for cost.
- **Citations are verifiable** — the one deterministic assertion you get for free.
- **Eval-first, not prompt-first.** Build the eval set before you tune anything, or you are
  optimising on anecdotes.

---

## Interview angle

**"Design a RAG system over 10 years of internal engineering docs for 5,000 employees."**

1. **Clarify** — freshness SLO? permissions? does it need to say "I don't know"? latency target?
2. **Offline pipeline** — event-driven on doc change; parse (call out PDFs/tables explicitly);
   chunk 300 tokens with structural boundaries; embed versioned; index into both a vector store and
   BM25.
3. **Online pipeline** — contextualise the query; hybrid retrieve top-100; RRF; cross-encoder
   rerank to top-5; assemble with metadata and a relevance floor; generate with citations; verify
   citations.
4. **Numbers** — corpus size → chunk count → index memory; per-query cost breakdown by stage.
5. **Evaluation** — golden set of ~200 real questions with labelled relevant chunks. Track
   recall@k and MRR for retrieval *separately* from answer quality. **Separating those two is the
   thing that makes the system debuggable**, and it is the highest-signal thing you can say.
6. **Failure handling** — no good chunks → say so; retrieval down → degrade gracefully, do not
   answer unsourced.

**Follow-ups:**
- *"It hallucinated a policy that doesn't exist."* → Check retrieval first. Was the right chunk
  retrieved? If yes, it is a prompting/grounding problem. If no, it is a retrieval problem. Most
  are the latter.
- *"Exact error codes return nothing."* → No lexical retriever. Add BM25, fuse with RRF.
- *"Answers got worse after we added more documents."* → More near-duplicates crowding the top-k;
  reranking and dedup matter more as the corpus grows.
- *"Cut latency in half."* → Drop query rewriting, shrink the rerank candidate set, cache
  aggressively, run retrieval and prompt assembly concurrently.

**How this shows up in an SDET loop:** RAG is unusually testable if you split the layers.
Retrieval gets classic IR metrics against a labelled set (deterministic, no LLM needed).
Generation gets faithfulness and citation-validity checks. Both are implemented in
[`sdet/aiqa/`](../../sdet/src/main/java/ra/hul/sdet/aiqa/).

---

## Self-check

1. Why is retrieval, not generation, the usual cause of a hallucinated answer?
2. Why does RRF fuse ranks instead of scores?
3. What can a cross-encoder do that a bi-encoder cannot, and why can't you use it for retrieval?
4. Why can adding *more* retrieved chunks make answers worse?
5. How do you verify a citation without an LLM?
6. What breaks on turn 2 of a chat if you skip query contextualisation?
7. You must separately measure retrieval quality and answer quality. Why?

---

## Practice → the coding assignment

[`03-rag-retrieval-assignment/`](03-rag-retrieval-assignment/assignment/)

```bash
cd sd/06-ai-system-design/03-rag-retrieval-assignment/assignment
go test ./...
```

You will implement BM25 scoring, cosine similarity retrieval, Reciprocal Rank Fusion, a reranking
stage, and token-budgeted context assembly with a relevance floor.

**Next:** [6.4 — Agents, Tool Calling & MCP »](04-agents-tool-calling-and-mcp.md)
