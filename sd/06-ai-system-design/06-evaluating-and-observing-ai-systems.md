# 6.6 — Evaluating & Observing AI Systems

> **Prerequisites:** all of Module 6, plus [3.6 Observability](../03-distributed-systems/06-observability.md).
> **This is the bridge lesson.** Everything here is implemented as runnable Java in
> [`sdet/aiqa/`](../../sdet/src/main/java/ra/hul/sdet/aiqa/).

---

## The problem

You changed one line of a system prompt. Is the product better or worse?

For any conventional system this question is trivial — run the tests. Here, every tool you rely on
breaks at once:

- **The same input produces different output.** `assertEquals` is meaningless.
- **There is no single correct answer.** Many good summaries exist; a diff against one of them
  tells you nothing.
- **Failures are fluent.** A wrong answer is grammatical, confident, and superficially excellent.
  It looks exactly like a right one.
- **Quality is multi-dimensional.** An answer can be accurate but rude, helpful but unsourced,
  correct but leaking PII.
- **Everything is a dependency.** Model version, prompt, retrieval corpus, embedding model, tool
  implementations, temperature. Any of them shifting changes behaviour, and providers update models
  under you.

This is why **eval-first development is replacing prompt-first development**: teams build the
evaluation before writing prompts, then iterate against a measurable target. Without that, you are
tuning on anecdotes — and anecdotes are what everyone's intuition mistakes for evidence.

---

## Core idea: separate the layers, then assert what is assertable

The single most useful move is refusing to treat "is the AI good?" as one question. Decompose it,
because most layers turn out to be *deterministically testable*:

| Layer | Deterministic? | How you test it |
|---|---|---|
| Retrieval | **Yes** | recall@k, precision@k, MRR, NDCG vs. labelled relevant docs |
| Tool-call construction | **Yes** | schema validation, argument assertions, contract tests |
| Citation validity | **Yes** | every cited ID must exist in the supplied context |
| Output format | **Yes** | JSON schema, required fields, enum membership |
| Guardrails / policy | **Yes** | fixed adversarial corpus, expected block/allow |
| Routing, caching, budgets | **Yes** | ordinary unit tests ([6.5](05-model-gateway-routing-and-cost.md)) |
| Answer correctness | No | golden dataset + graded metrics |
| Tone, helpfulness | No | LLM-as-judge, calibrated against humans |

**Most of the surface area is deterministic.** Candidates who say "you can't really test LLMs"
have not decomposed the problem. Candidates who lead with this table have.

---

## The golden dataset

The core asset. Not the prompts, not the model — the dataset. It outlives both.

**Build it from real production failures**, not synthetic examples. Synthetic sets test what you
already thought of, which is precisely the set of things that are not broken. A practical start:
take 50 real failures, have one domain expert grade them pass/fail with a written critique, and
grow from there. Mature sets run 200–500 examples.

Each case should carry:

```jsonc
{
  "id": "refund-policy-enterprise-001",
  "input": "Can an enterprise customer get a refund after 45 days?",
  "context_ids": ["policy-refunds-v3#chunk-12"],   // for retrieval scoring
  "expected": {
    "must_contain": ["30-day", "enterprise exception"],
    "must_not_contain": ["guaranteed", "always"],
    "must_cite": true
  },
  "rubric": "Correct if it states the 30-day standard window AND the enterprise exception.",
  "tags": ["policy", "high-stakes"],
  "source": "prod-incident-4821"
}
```

Note the shape: **assertions where possible, rubric where not.** `must_contain` / `must_not_contain`
are deterministic and cheap; the rubric is the fallback for the genuinely subjective part.

Version the dataset. Version *everything* it depends on — prompt, model ID, inference parameters,
retrieval corpus, embedding model, tool implementations, judge prompt, judge model, scorer code.
An eval score without that provenance is not reproducible, and an irreproducible score cannot
gate anything.

---

## Metrics that mean something

**Retrieval** (deterministic, no LLM needed, run these first):
- **recall@k** — of the truly relevant chunks, how many appeared in the top k? *The* RAG metric:
  if the right chunk was never retrieved, no prompt fixes it.
- **precision@k** — of the k returned, how many were relevant? Low precision wastes context budget
  and degrades answers.
- **MRR** — 1/rank of the first relevant result. Rewards putting the right thing first.
- **NDCG@k** — rank-weighted with graded relevance. The right metric when relevance is not binary.

**Generation:**
- **Faithfulness / groundedness** — is every claim supported by the supplied context? The direct
  measure of hallucination.
- **Answer relevance** — does it address the question asked?
- **Citation validity** — deterministic, free, and startlingly effective.
- **Exact/fuzzy match** — for extraction and classification, where there *is* one right answer.

**Operational:** latency (TTFT and total), cost per request, token counts, cache hit rate,
refusal rate, tool-selection accuracy, task success rate.

---

## LLM-as-judge, used honestly

For subjective dimensions, another model grades the output. It works, and it has well-documented
biases you must state if asked:

- **Position bias** — prefers whichever candidate came first. Mitigate by swapping order and
  averaging.
- **Verbosity bias** — prefers longer answers. Mitigate by controlling for length in the rubric.
- **Self-preference** — prefers text from its own model family. Mitigate by using a different
  model as judge.
- **Confidence bias** — prefers assertive, fluent text even when it is wrong. This is the dangerous
  one, because it is the same bias a human reviewer has.

The discipline that makes it credible:

1. **Calibrate against humans.** Have experts label a subset; measure judge–human agreement.
   **85–90% agreement is the working target.** Below that, fix the judge prompt before trusting a
   single number it produces.
2. **Binary or low-cardinality scales.** "Pass/fail" is far more reliable than "score 1–10"; a
   judge's 7 versus 8 is noise.
3. **Require a written critique before the verdict.** Forces the judgment to be grounded and gives
   you something to debug.
4. **Keep a human-labelled holdout** and re-check agreement when you change judge model or prompt.

Report judge-based numbers *with* their agreement rate. A number without it is decoration.

---

## Where evaluation runs

```
 development ──► pull request ──► staging ──► production
      │              │               │            │
   fast subset   full golden     canary vs    online monitoring
   ~20 cases     dataset,        baseline,    on real traffic:
   seconds       gate on         shadow       drift, refusals,
                 regression      traffic      thumbs-down, cost
```

**The PR gate is the piece that turns evaluation into engineering.** A prompt change runs the
golden dataset; a regression below baseline blocks the merge — exactly like a test suite, exactly
like the quality gates in
[5.2](../05-sdet-system-design/02-design-a-ci-cd-pipeline.md).

Two subtleties:

**Non-determinism means a single run is not a measurement.** Set temperature to 0 where the
product allows it, and where it does not, run *n* samples and gate on the aggregate with a
tolerance band. A gate that trips on normal sampling variance gets disabled within a fortnight —
the same dynamic as a flaky test ([5.5](../05-sdet-system-design/05-flaky-test-detection-and-quarantine.md)),
and the same remedy: measure the noise, then set the threshold outside it.

**Online monitoring catches what offline cannot** — real inputs drift away from your dataset.
Track refusal rate, thumbs-down rate, retrieval-score distributions, output-length distribution,
and cost per request. Sudden movement in any of them usually precedes a quality complaint. And the
production failures you find here become tomorrow's golden cases; that loop is the whole system.

---

## Observability specifics

Ordinary tracing, plus AI-specific spans. Every request should record: prompt version, model ID
and version, temperature/params, retrieved chunk IDs and scores, tool calls with arguments and
results, token counts in/out/cached, TTFT and total latency, cost, cache hit/miss, guardrail
verdicts, and final output.

Two reasons this matters more than in a conventional system: you cannot reproduce a failure by
re-running it, so the trace *is* the evidence; and the provider can change the model under you,
so `model_version` is a diagnostic dimension you will genuinely need.

---

## Trade-offs & key takeaways

- **Decompose before despairing.** Most layers are deterministic; test those normally.
- **The golden dataset is the durable asset**, built from real failures, and versioned with
  everything it depends on.
- **Retrieval metrics first.** Most "hallucinations" are retrieval misses.
- **Citation validity is free hallucination detection.**
- **LLM-as-judge needs calibration**, a stated agreement rate, and binary scales.
- **Gate PRs on the golden dataset**, with a tolerance band sized to sampling noise.
- **Online monitoring feeds the golden set.** The loop is the point.
- **Eval-first beats prompt-first**, because without a target you are optimising on anecdotes.

---

## Interview angle

**"How would you test an AI feature?"** — the question that most often separates candidates in
2026, and one many answer with a shrug.

The strong answer:

1. **Decompose.** Present the deterministic/non-deterministic table. Most of it is normal testing.
2. **Golden dataset** from production failures; versioned; assertions plus rubrics.
3. **Layered metrics** — retrieval separately from generation, so failures are attributable.
4. **LLM-as-judge** only for the subjective remainder, calibrated to 85–90% human agreement,
   biases named.
5. **CI gate** on regression against baseline, with a tolerance band for sampling noise.
6. **Online monitoring** on refusals, thumbs-down, retrieval-score drift, cost.
7. **Adversarial suite** — prompt injection, jailbreaks, PII leakage, tool misuse — run
   continuously, not once before launch. Attack patterns evolve and models drift.

**Follow-ups:**
- *"The same test passes and fails randomly."* → That is the system, not a bug. Temperature 0
  where possible; otherwise n-sample aggregate with a tolerance band derived from measured variance.
- *"How do you know the judge is right?"* → Human-labelled holdout, agreement rate, re-checked
  whenever the judge changes.
- *"Provider silently updated the model."* → Version-pin where the provider allows it; run the
  golden dataset on a schedule, not only on your own changes; alert on baseline drift.
- *"Coverage for a non-deterministic system?"* → Not line coverage — *scenario* coverage across
  intents, input shapes, languages, adversarial classes, and known failure modes.

---

## Self-check

1. Which layers of an AI system are deterministically testable? Name five.
2. Why build the golden dataset from production failures rather than synthetic examples?
3. Why measure retrieval quality separately from answer quality?
4. Name three LLM-as-judge biases and a mitigation for each.
5. What agreement rate should a judge hit before you trust it to gate merges?
6. How do you keep an eval gate from behaving like a flaky test?
7. What must be versioned alongside the dataset for a score to be reproducible?

---

## Practice → the AI testing pillar

Everything above is implemented as runnable, self-verifying Java:

```bash
cd sdet && mvn exec:java -Dexec.mainClass="ra.hul.sdet.aiqa.Ques4_RetrievalQualityMetrics"
```

See [`sdet/src/main/java/ra/hul/sdet/aiqa/`](../../sdet/src/main/java/ra/hul/sdet/aiqa/) —
eval harness, golden datasets, judge calibration, retrieval metrics, guardrails, regression
gating, non-determinism control, and tool-call contract validation.

**Module complete.** Back to [Module 6 index »](README.md)
