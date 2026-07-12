[![Run Framework Tests](https://github.com/heyrmi/sdet-prep/actions/workflows/run-tests.yml/badge.svg)](https://github.com/heyrmi/sdet-prep/actions/workflows/run-tests.yml)

# SDET Interview Prep — the mega-repo

> One repo to train for a **FAANG-level SDET** loop end-to-end: the coding round, the
> system-design round, the practical automation/test-engineering round, the company-specific
> screens, and the behavioral round — plus a **spaced-repetition engine** so it all sticks.

Seven pillars, one workflow:

| # | Pillar | What it is | Entry point |
|---|--------|-----------|-------------|
| 🧩 | **DSA** | **190+ runnable Java problems** across 26 topic/pattern packages, each a single file with a `main()` you can run in your IDE. Orientation lessons (Big-O, Java gotchas, UMPIRE) + a frequency-ranked strategy guide. | [`dsa/`](dsa/) · [guide](dsa/DSA_INTERVIEW_QUESTIONS.md) |
| 🛠️ | **SDET practical** | **53 runnable solutions** across file ops, data processing, API, concurrency, regex, Linux, design patterns, and machine-coding builds — plus the 100+ question, 13-category guide. | [`sdet/`](sdet/) · [guide](sdet/SDET_INTERVIEW_QUESTIONS.md) |
| 🏢 | **Company question bank** | **932 real interview questions across 32 companies** (verbal/scenario style), by domain + difficulty. Plus a fully-answered JioStar/Hotstar framework round. | [`sdet/company-questions/`](sdet/company-questions/) |
| ⚙️ | **Automation framework (Java)** | Production-grade **Web + API + Mobile + Performance** framework (Selenium 4, RestAssured, Appium, Gatling, TestNG, Allure, CI). The centerpiece of the SDET automation round. | [`framework/`](framework/) |
| 🎭 | **Automation framework (TypeScript)** | A **Playwright** framework — UI + API in TypeScript with Zod runtime schema validation, fixtures, page objects, Biome, and CI. The modern-stack counterpart to the Java framework. | [`playwright/`](playwright/) |
| 📐 | **System Design** | A ByteByteGo-style course: 30+ first-principles lessons (networking → building blocks → distributed systems) + **17 case studies**, each with a Go coding assignment + tests. | [`sd/`](sd/) |
| 🔁 | **Spaced-repetition tracker** | `srs` — a dependency-free Go CLI that auto-discovers every lesson & problem across DSA + SDET + System Design and schedules **active-recall reviews** (SM-2 / Anki algorithm). | [`study-tracker/`](study-tracker/) |

---

## Repository map

```
sdet-prep/                        (parent pom — Maven reactor: dsa, sdet, framework)
│
├── dsa/                          🧩 Java, no external deps — runnable Ques{N}_{Name}.java
│   ├── DSA_INTERVIEW_QUESTIONS.md   frequency-ranked strategy guide (MUST-DO vs GOOD-TO-KNOW)
│   ├── lessons/                     orientation: Big-O, Java-for-interviews, UMPIRE, glossary
│   └── src/main/java/ra/hul/dsa/    arrays, strings, hashmap, linkedlist, fastslowpointers,
│                                    stack, queue, tree, trie, unionfind, advancedtrees, heap,
│                                    graph, twopointers, slidingwindow, prefixsum, binarysearch,
│                                    intervals, backtracking, dp, dpadvanced, greedy, matrix,
│                                    bitmanipulation, sorting, recursion
│
├── sdet/                         🛠️ Selenium, RestAssured, TestNG practical problems
│   ├── SDET_INTERVIEW_QUESTIONS.md  100+ questions, 13 categories, 10 machine-coding builds
│   ├── company-questions/           🏢 932 Qs × 32 companies + JioStar deep-dive
│   └── src/main/java/ra/hul/sdet/   fileops, scraping, api, dataprocessing, selenium, database…
│
├── framework/                    ⚙️ Java automation framework (Web+API+Mobile+Perf)
│   ├── README.md · MOBILE_SETUP.md · CLAUDE.md
│   └── src/…                        core, web, api, mobile, performance + test suites
│
├── playwright/                   🎭 TypeScript Playwright framework (UI + API, Zod, Biome, CI)
│   ├── pageFactory/ui/              Page Object Models over the-internet.herokuapp.com
│   ├── lib/ · types/ · tests/       fixtures, API clients, UI + httpbin + FreeAPI test suites
│   └── (vendored from github.com/heyrmi/hlpw via git subtree)
│
├── sd/                           📐 System Design course (Markdown lessons + Go assignments)
│   ├── 00-foundations · 01-networking · 02-building-blocks · 03-distributed-systems
│   └── 04-case-studies/             17 systems (rate limiter → payment system), each with Go
│
└── study-tracker/                🔁 `srs` — spaced-repetition CLI (Go) over DSA + SDET + System Design
```

---

## Quick start

### Java modules (DSA, SDET, framework) — needs JDK 25 + Maven
```bash
mvn clean compile                                              # build all Java modules

# run any DSA problem (IDE: just click ▶ on its main())
cd dsa && mvn exec:java -Dexec.mainClass="ra.hul.dsa.arrays.Ques1_TwoSum"

# run any SDET practical problem
cd sdet && mvn exec:java -Dexec.mainClass="ra.hul.sdet.api.Ques1_GetRequestValidation"

# framework tests (per module via Maven profiles)
cd framework && mvn test -Pweb     # or -Papi / -Pmobile / -Psmoke; bare `mvn test` runs all
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
| 8–12 | Company-specific drilling (answer out loud, time-boxed) + behavioral stories | `sdet/company-questions/` |
| every day | 20 min of `srs today` — consistency on the forgetting curve is the whole game | tracker |

---

## Is this enough for a FAANG SDET loop?

That was the design goal. Coverage maps to the actual round types:

- **Coding round** → `dsa/` (pattern-based, 190+ problems approaching NeetCode-150 breadth).
- **Automation / test-engineering round** → `framework/` (Java) + `playwright/` (TypeScript) + 53
  runnable `sdet/` solutions (the differentiator — real, production-grade code, not toy snippets).
- **System-design round** → `sd/` (17 end-to-end case studies with runnable primitives).
- **Company screen** → `sdet/company-questions/` (932 questions, FAANG + 26 more).
- **Behavioral** → the *Situational* sections of the company bank + the JioStar deep-dive.
- **Retention** → `study-tracker/` turns all of it into durable, interview-ready recall.

Keep pushing volume in the hard-tier DSA and rehearse behavioral stories out loud — those are the
two areas where more reps always help.

---

## Tech stack

- **Java 25** — DSA/SDET modules (framework compiles at release 21)
- **Selenium 4.41 · REST Assured 6.0 · Appium 10.1 · Gatling 3.15 · TestNG 7.12 · Allure 2.33**
- **TypeScript · Playwright 1.58 · Zod 4 · Biome** — the `playwright/` framework (Node 24+)
- **Go 1.21+** — System Design assignments + the `srs` tracker
- No cloud accounts, no paid services — just the JDK, Maven, Node, and Go toolchains.

## Attribution

The System Design course was synthesized with reference to the open-source
[System Design Primer](https://github.com/donnemartin/system-design-primer) and
[System Design 101](https://github.com/ByteByteGoHq/system-design-101); the DSA set follows the
Blind-75 / NeetCode canon. Company questions are sourced from the public
[ShapeMyInterview](https://www.shapemyinterview.com) question banks (prompts + metadata). All
lesson prose, solution code, the framework, and the tracker are original to this repo.
