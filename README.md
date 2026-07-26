[![Run Framework Tests](https://github.com/heyrmi/sdet-prep/actions/workflows/run-tests.yml/badge.svg)](https://github.com/heyrmi/sdet-prep/actions/workflows/run-tests.yml)

# SDET Interview Prep — the mega-repo

> One repo to train for a **FAANG-level SDET** loop end-to-end: the coding round, the
> system-design round, the practical automation/test-engineering round, the company-specific
> screens, and the behavioral round — plus a **spaced-repetition engine** so it all sticks.

Seven pillars, one workflow:

| # | Pillar | What it is | Entry point |
|---|--------|-----------|-------------|
| 🧩 | **DSA** | **205 runnable Java problems** across 28 topic/pattern packages, each a single file with a `main()` you can run in your IDE. Orientation lessons (Big-O, Java gotchas, UMPIRE) + a frequency-ranked strategy guide. | [`dsa/`](dsa/) · [guide](dsa/DSA_INTERVIEW_QUESTIONS.md) |
| 🛠️ | **SDET practical** | **61 runnable solutions** across file ops, data processing, API, concurrency, regex, Linux, design patterns, SQL/JDBC (in-memory H2), and machine-coding builds — plus the 100+ question, 13-category guide. | [`sdet/`](sdet/) · [guide](sdet/SDET_INTERVIEW_QUESTIONS.md) |
| 🏢 | **Company question bank** | **932 real interview questions across 32 companies** (verbal/scenario style), by domain + difficulty. Plus full **model answers for Google/Amazon/Meta/Microsoft/Apple** and a fully-answered JioStar/Hotstar framework round. | [`sdet/company-questions/`](sdet/company-questions/) |
| ⚙️ | **Automation framework (Java)** | Production-grade **Web + API + Mobile + Performance** framework (Selenium 4, RestAssured, Appium, Gatling, TestNG, Allure, CI) with **visual regression, accessibility (axe-core), Pact contract testing, and data factories**. Vendored from `heyrmi/framework` via git subtree. | [`framework/`](framework/) |
| 🎭 | **Automation framework (TypeScript)** | A **Playwright** framework — UI + API in TypeScript with Zod schema validation, fixtures, page objects, Biome, CI, plus **cross-browser, visual regression, accessibility (axe-core), Pact contract testing, and faker data factories**. Vendored from `heyrmi/hlpw` via git subtree. | [`playwright/`](playwright/) |
| 📐 | **System Design** | A ByteByteGo-style course: 30+ first-principles lessons (networking → building blocks → distributed systems) + **17 case studies**, a **5-lesson SDET module**, a **6-lesson AI system design module**, and a **3-lesson distributed-systems testing module** — **28 Go assignments** in total, every solution verified in CI. | [`sd/`](sd/) |
| 🤖 | **AI/LLM testing** | **8 runnable problems** on the question that most separates candidates in 2026: eval harnesses, golden datasets, LLM-as-judge calibration, retrieval metrics, guardrails, regression gating, non-determinism control, and agent tool-call validation. | [`sdet/…/aiqa/`](sdet/src/main/java/ra/hul/sdet/aiqa/) |
| 🔁 | **Spaced-repetition tracker** | `srs` — a dependency-free Go CLI that auto-discovers every lesson & problem across DSA + SDET + System Design and schedules **active-recall reviews** (SM-2 / Anki algorithm). | [`study-tracker/`](study-tracker/) |
| ✅ | **Regression gate** | `verifier` — executes **every one of the 281 problems' `main()`** on each PR and fails the build on any exception, hang, or failed self-check. Plus a mutation-testing profile and a guide-vs-code drift check. | [`verifier/`](verifier/) |

---

## Repository map

```
sdet-prep/                        (parent pom — Maven reactor: dsa, sdet; framework & playwright are git subtrees, outside the reactor)
│
├── dsa/                          🧩 Java, no external deps — runnable Ques{N}_{Name}.java
│   ├── DSA_INTERVIEW_QUESTIONS.md   frequency-ranked strategy guide (MUST-DO vs GOOD-TO-KNOW)
│   ├── lessons/                     orientation: Big-O, Java-for-interviews, UMPIRE, glossary
│   └── src/main/java/ra/hul/dsa/    arrays, strings, hashmap, linkedlist, fastslowpointers,
│                                    stack, queue, tree, trie, unionfind, advancedtrees, design,
│                                    heap, graph, twopointers, slidingwindow, prefixsum,
│                                    binarysearch, intervals, backtracking, dp, dpadvanced,
│                                    greedy, matrix, monotonicstack, bitmanipulation, sorting, recursion
│
├── sdet/                         🛠️ Selenium, RestAssured, TestNG practical problems
│   ├── SDET_INTERVIEW_QUESTIONS.md  100+ questions, 13 categories, 10 machine-coding builds
│   ├── company-questions/           🏢 932 Qs × 32 companies + 5 model-answer sets + JioStar deep-dive
│   └── src/main/java/ra/hul/sdet/   fileops, scraping, api, dataprocessing, selenium, database (JDBC/SQL),
│                                    aiqa/ (AI & LLM testing), propertytesting/ (PBT, shrinking,
│                                    metamorphic, mutation testing)…
│
├── verifier/                     ✅ runs every problem's main() as the PR gate; mutation-testing profile
│
├── framework/                    ⚙️ Java automation framework (Web+API+Mobile+Perf + visual/a11y/contract/data)
│   ├── README.md · MOBILE_SETUP.md · CLAUDE.md
│   ├── src/…                        core, web, api, mobile, performance + test suites
│   └── (git subtree from github.com/heyrmi/framework — self-contained standalone pom)
│
├── playwright/                   🎭 TypeScript Playwright framework (UI + API, Zod, Biome, CI)
│   ├── pageFactory/ui/              Page Object Models over the-internet.herokuapp.com
│   ├── lib/ · types/ · tests/       fixtures, API clients, UI + httpbin + FreeAPI + visual/a11y suites
│   └── (git subtree from github.com/heyrmi/hlpw)
│
├── sd/                           📐 System Design course (Markdown lessons + Go assignments)
│   ├── 00-foundations · 01-networking · 02-building-blocks · 03-distributed-systems
│   ├── 04-case-studies/             17 systems (rate limiter → payment system), each with Go
│   ├── 05-sdet-system-design/       5 SDET lessons + 5 assignments (test platform, CI/CD + DORA,
│   │                                device pool, testability seams, flaky quarantine)
│   ├── 06-ai-system-design/         6 lessons + 3 assignments (LLM serving & KV-cache math, vector
│   │                                search, RAG + RRF, agents & MCP, model gateway, evals)
│   └── 07-testing-distributed-systems/  3 lessons + a real linearizability checker
│                                    (consistency checking, deterministic simulation, chaos)
│
└── study-tracker/                🔁 `srs` — spaced-repetition CLI (Go) over DSA + SDET + System Design
```

---

## Quick start

### Java reactor (DSA + SDET + verifier) — needs JDK 25 + Maven
```bash
mvn clean install -DskipTests    # build the reactor (dsa + sdet + verifier)
mvn -pl verifier test            # run ALL 281 problems; ~3s, 10 networked ones skipped
./scripts/check-coverage.sh      # diff the SDET guide against what is implemented

# run any DSA problem (IDE: just click ▶ on its main())
cd dsa && mvn exec:java -Dexec.mainClass="ra.hul.dsa.arrays.Ques1_TwoSum"

# run any SDET practical problem
cd sdet && mvn exec:java -Dexec.mainClass="ra.hul.sdet.api.Ques1_GetRequestValidation"
```

### Java automation framework — builds standalone (JDK 21)
```bash
# framework is a self-contained git subtree, outside the reactor — build/run it on its own
cd framework && mvn test -Pweb     # or -Papi / -Pmobile / -Psmoke / -Pvisual / -Pa11y / -Pcontract; bare `mvn test` runs all
```
> Each DSA/SDET problem is a self-contained file whose `main()` prints results with the expected
> value in a trailing comment — run it and eyeball green/red. No test runner required.

### Playwright framework — needs Node 24+
```bash
cd playwright
npm ci
npx playwright install --with-deps
npm test              # UI + API + FreeAPI; or npm run test:ui / test:api / test:smoke
```

### System Design assignments — needs Go 1.21+
```bash
cd sd/04-case-studies/01-rate-limiter/assignment
go test ./...        # red → implement the TODOs → green; reference in ../solution/
```

### The daily habit — the spaced-repetition tracker
```bash
cd study-tracker && go build -o srs .
./srs init      # scans ../dsa, ../sdet and ../sd; builds your review deck
./srs status    # dashboard: due count, streak, 7-day forecast
./srs today     # review what's due — re-explain lessons, re-solve problems
```
Re-run `./srs init` whenever you add content — progress is preserved.

---

## A suggested 12-week plan

| Weeks | Focus | Repo areas |
|-------|-------|-----------|
| 1 | Orientation + Big-O + Java toolkit; start `srs today` daily | `dsa/lessons/`, tracker |
| 2–5 | DSA by pattern, easy→hard; re-solve from blank the next day | `dsa/` + strategy guide |
| 3–6 | SDET practical: file ops → API → Selenium → concurrency → machine-coding builds | `sdet/` |
| 4–7 | Build/extend the automation frameworks (Java + Playwright); solve the `sdet/` practical problems; defend every design choice | `framework/` + `playwright/` + `sdet/` + JioStar deep-dive |
| 6–9 | System Design: foundations → building blocks → case studies (design *then* code) | `sd/` |
| 8–10 | Test-architect modules: SDET system design (5), AI system design (6), distributed-systems testing (7) — do the assignments | `sd/05` · `sd/06` · `sd/07` |
| 9–11 | AI & advanced testing: eval harnesses, judge calibration, guardrails; property-based, metamorphic, mutation testing | `sdet/…/aiqa/` · `sdet/…/propertytesting/` |
| 8–12 | Company-specific drilling (answer out loud, time-boxed) + behavioral stories | `sdet/company-questions/` |
| every day | 20 min of `srs today` — consistency on the forgetting curve is the whole game | tracker |

---

## Is this enough for a FAANG SDET loop?

That was the design goal. Coverage maps to the actual round types:

- **Coding round** → `dsa/` (pattern-based, 205 problems approaching NeetCode-150 breadth).
- **Automation / test-engineering round** → `framework/` (Java) + `playwright/` (TypeScript) — both with
  visual regression, accessibility, and Pact contract testing — + 76 runnable `sdet/` solutions (the
  differentiator — real, production-grade code, not toy snippets).
- **System-design round** → `sd/` (17 end-to-end case studies + SDET, AI, and distributed-systems-testing modules).
- **AI round** → `sd/06-ai-system-design/` for the design side, `sdet/…/aiqa/` for *"how would you
  test an AI feature?"* — the question that most separates candidates in 2026.
- **Test-architect signal** → `sd/05` (test platform, DORA, device pools, testability seams),
  `sd/07` (linearizability, deterministic simulation, chaos), and
  `sdet/…/propertytesting/` (PBT, shrinking, metamorphic, mutation testing).
- **Company screen** → `sdet/company-questions/` (932 questions, FAANG + 26 more).
- **Behavioral** → the *Situational* sections of the company bank + the JioStar deep-dive.
- **Retention** → `study-tracker/` turns all of it into durable, interview-ready recall.

Everything here is **verified, not asserted**: `verifier/` runs all 281 problems on every PR, every
Go solution is tested against its own assignment's test suite in CI, and
`scripts/check-coverage.sh` fails the build if the guide and the source tree drift apart.

Keep pushing volume in the hard-tier DSA and rehearse behavioral stories out loud — those are the
two areas where more reps always help.

---

## Tech stack

- **Java 25** — DSA/SDET modules (framework compiles at release 21)
- **Selenium 4.41 · REST Assured 6.0 · Appium 10.1 · Gatling 3.15 · TestNG 7.12 · Allure 2.33 · axe-core · Pact JVM · datafaker** — the Java `framework/`
- **TypeScript · Playwright 1.58 · Zod 4 · Biome · @axe-core/playwright · @pact-foundation/pact · faker** — the `playwright/` framework (Node 24+)
- **Go 1.21+** — System Design assignments + the `srs` tracker
- No cloud accounts, no paid services — just the JDK, Maven, Node, and Go toolchains.

## Attribution

The System Design course was synthesized with reference to the open-source
[System Design Primer](https://github.com/donnemartin/system-design-primer) and
[System Design 101](https://github.com/ByteByteGoHq/system-design-101); the DSA set follows the
Blind-75 / NeetCode canon. Company questions are sourced from the public
[ShapeMyInterview](https://www.shapemyinterview.com) question banks (prompts + metadata). All
lesson prose, solution code, the framework, and the tracker are original to this repo.
