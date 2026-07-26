# `aiqa/` — Testing AI & LLM Systems

> **"How would you test an AI feature?"** is the question that most separates candidates in a 2026
> SDET loop, and the one most people answer with a shrug. These eight problems are the answer,
> as runnable code.

Pairs with [`sd/06-ai-system-design/`](../../../../../../../sd/06-ai-system-design/) — that module
designs the systems, this one tests them. Read
[6.6 Evaluating & Observing AI Systems](../../../../../../../sd/06-ai-system-design/06-evaluating-and-observing-ai-systems.md)
alongside these.

## Run them

```bash
cd sdet && mvn exec:java -Dexec.mainClass="ra.hul.sdet.aiqa.Ques3_RetrievalQualityMetrics"
```

Every problem is **self-contained and deterministic** — no network, no API keys, no model calls.
Randomness is seeded, so the numbers are identical on every run. That is itself the lesson of
`Ques7`.

## The problems

| # | Problem | The idea it defends |
|---|---------|---------------------|
| 1 | [`Ques1_SemanticAssertions`](Ques1_SemanticAssertions.java) | The **assertion ladder**: exact → structural → contains/excludes → fuzzy → semantic. Pick the *strongest* rung the output can satisfy. Demonstrates the embedding blind spot on negation. |
| 2 | [`Ques2_GoldenDatasetRunner`](Ques2_GoldenDatasetRunner.java) | The **golden dataset** is the durable asset. Built from real production failures, scored with assertions, reported **per tag** — because 82% overall hides high-stakes at 40%. |
| 3 | [`Ques3_RetrievalQualityMetrics`](Ques3_RetrievalQualityMetrics.java) | **recall@k, precision@k, MRR, NDCG@k**. Most "hallucinations" are retrieval misses; measuring retrieval separately from generation is what makes a failure attributable. |
| 4 | [`Ques4_LlmAsJudgeCalibration`](Ques4_LlmAsJudgeCalibration.java) | **Judge-vs-human agreement and Cohen's kappa.** Shows a judge with 40% raw agreement and *negative* kappa, plus verbosity and position bias probes. |
| 5 | [`Ques5_PromptInjectionGuardrail`](Ques5_PromptInjectionGuardrail.java) | Guardrails measured as a **classifier** — recall *and* false-positive rate. Deliberately shows three obfuscated attacks getting through, because pattern matching is a mitigation, not a solution. |
| 6 | [`Ques6_EvalRegressionGate`](Ques6_EvalRegressionGate.java) | Gating merges on evals **without building a flaky test**: tolerance band from measured variance, minimum sample size, per-tag gating, warn vs block tiers. |
| 7 | [`Ques7_NonDeterminismControl`](Ques7_NonDeterminismControl.java) | **pass@k**, majority voting (self-consistency), stability measurement, and seeding. Matching the technique to the task is the judgment being tested. |
| 8 | [`Ques8_ToolCallContractValidation`](Ques8_ToolCallContractValidation.java) | The **agent security boundary**: schema → authorisation → policy → idempotency. A schema-perfect refund is still denied on session scope. That is why runtime authorisation beats prompt engineering. |

## The core argument

Candidates who say *"you can't really test LLMs"* have not decomposed the problem. Most of an AI
system's surface is **deterministic and testable with ordinary assertions**:

| Layer | Deterministic? | Covered by |
|---|---|---|
| Retrieval quality | **yes** | `Ques3` |
| Tool-call construction | **yes** | `Ques8` |
| Citation validity | **yes** | `sd/06` RAG assignment |
| Output structure | **yes** | `Ques1` |
| Guardrails / policy | **yes** | `Ques5` |
| Routing, caching, budgets | **yes** | `sd/06` gateway assignment |
| Answer correctness | no | `Ques2`, `Ques6` |
| Tone, helpfulness | no | `Ques4` |

Only the last two rows need graded evaluation. Lead with that table in an interview.

## Interview cheat sheet

- **"How do you test something non-deterministic?"** → the assertion ladder (`Ques1`), then
  pass@k / voting / seeding (`Ques7`).
- **"How do you know a prompt change helped?"** → golden dataset (`Ques2`) + regression gate with
  a noise-derived tolerance band (`Ques6`).
- **"The model hallucinated."** → check retrieval first (`Ques3`); most of these are retrieval
  misses, not generation failures.
- **"How do you know the judge is right?"** → human holdout, agreement rate, kappa (`Ques4`).
- **"How do you defend against prompt injection?"** → layered, and the layer that holds is runtime
  authorisation (`Ques8`), not detection (`Ques5`).
- **"What's your coverage?"** → not line coverage — **scenario** coverage across intents, input
  shapes, adversarial classes, and known failure modes.
