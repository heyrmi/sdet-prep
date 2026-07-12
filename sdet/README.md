# SDET Practical — hands-on test-engineering problems

The **SDET automation round** in code: real-world problems (file/data/API/concurrency/regex),
machine-coding builds, design patterns, plus a company-specific question bank. Not DSA — this is
the "write a tool / test / framework" side of the interview.

```
sdet/
├── SDET_INTERVIEW_QUESTIONS.md   the guide — 100+ questions, 13 categories, company tags
├── company-questions/            932 questions × 32 companies + JioStar deep-dive
└── src/main/java/ra/hul/sdet/    runnable solutions (one Ques{N}_{Name}.java per problem)
```

## Implemented problems (53 runnable)

Every file is self-contained with a `static void main()` that runs the logic and prints `PASSED`.
Run any of them from the repo root:

```bash
mvn -pl sdet compile
mvn -pl sdet exec:java -Dexec.mainClass="ra.hul.sdet.fileops.Ques2_LogParser"
# …or just click ▶ on the main() in your IDE
```

| Category | Package | Count | Notes |
|----------|---------|-------|-------|
| File Operations | `fileops` | 8 | diff, log parser, grep clone, word-freq, k-way merge, dup finder, tail -f, config parser |
| Data Processing | `dataprocessing` | 7 | CSV/JSON/XML parse+validate+transform, merge/dedup, streaming aggregates |
| Regex & Text | `regex` | 7 | email, log, URL, card-masker, HTML-strip, CSV, semver |
| Multithreading | `multithreading` | 4 | producer-consumer, thread-safe collector, parallel executor, RW-lock |
| Linux in Java | `linux` | 3 | ProcessBuilder exec, NIO file ops, ProcessHandle monitor |
| Design Patterns | `designpatterns` | 7 | POM, Singleton, Factory, Builder, Strategy, Observer, Decorator |
| Machine-Coding Builds | `builds` | 7 | file comparator, log analyzer, data validator, HTML report gen, config mgr, retry framework, mock server |
| API Testing | `api` | 6 | GET/POST/CRUD-chain/schema/rate-limit/percentiles — **need network** (RestAssured → public APIs) |
| Performance | `performance` | 2 | load test + throughput ramp — **need network** |
| Web Scraping | `scraping` | 1 | Selenium page-title scraper — **needs browser** |
| Selenium Practical | `selenium` | 1 | broken-link checker — **needs browser/network** |

> **Network/browser problems** hit stable public endpoints (jsonplaceholder, httpbin) or a local
> browser. The self-contained ones (file/data/regex/concurrency/linux/patterns/builds — 44 of them)
> run offline and self-verify.

## Not re-implemented here (by design — see the guide for the mapping)

The guide's **Selenium practical tasks** (waits, alerts, frames, upload/download, windows, drag-drop,
dropdowns, shadow DOM, CDP, cross-browser…) and **Gatling performance** are covered *properly* by the
two automation frameworks in this repo rather than duplicated as snippets:

- **[`../framework/`](../framework/)** — Java: Selenium 4 + RestAssured + Appium (mobile) + Gatling, with Page/Screen Object Model, TestNG, Allure, CI.
- **[`../playwright/`](../playwright/)** — TypeScript: Playwright UI + API, Zod schema validation, fixtures, CI.

## Company question bank

**[`company-questions/`](company-questions/)** — 932 real interview questions across 32 companies
(Google, Apple, Meta, Microsoft, Amazon + 27 more), by domain and difficulty, plus a fully-answered
[JioStar/Hotstar framework round](company-questions/jiostar-hotstar-framework-round.md).
