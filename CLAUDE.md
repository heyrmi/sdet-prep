# CLAUDE.md

Guidance for AI assistants (Claude Code and others) working in this repository. Keep this file
updated when the structure, tooling, or conventions change.

## What this is

**SDET Interview Prep — a polyglot "mega-repo"** covering an entire FAANG-level SDET interview loop.
Seven pillars:

| Pillar | Path | Stack | What it is |
|--------|------|-------|-----------|
| DSA | `dsa/` | Java 25 / Maven | ~189 runnable problems, `Ques{N}_{Name}.java`, one per file |
| SDET practical | `sdet/` | Java 25 / Maven | ~53 practical problems + 932-question company bank |
| Framework (Java) | `framework/` | Java 21 / Maven | Selenium+RestAssured+Appium+Gatling automation framework |
| Framework (TS) | `playwright/` | TypeScript / Node 24 | Playwright UI+API framework (vendored from `heyrmi/hlpw` via git subtree) |
| System Design | `sd/` | Markdown + Go | 30+ lessons + 17 case studies with Go assignments |
| Tracker | `study-tracker/` | Go | `srs` spaced-repetition CLI indexing DSA+SDET+SD |
| Company bank | `sdet/company-questions/` | Markdown | 932 Qs × 32 companies + JioStar deep-dive |

## Build / run / test (by toolchain)

**Java (Maven reactor = `dsa`, `sdet`, `framework` only):**
```bash
mvn clean compile                      # build all Java modules
mvn -pl dsa exec:java -Dexec.mainClass="ra.hul.dsa.arrays.Ques1_TwoSum"   # run a DSA problem
mvn -pl sdet exec:java -Dexec.mainClass="ra.hul.sdet.fileops.Ques2_LogParser"
cd framework && mvn test -Pweb         # framework tests: -Pweb/-Papi/-Pmobile/-Psmoke
```
`sd/`, `study-tracker/`, and `playwright/` are intentionally **outside** the Maven reactor.

**Go:**
```bash
cd sd/04-case-studies/01-rate-limiter/assignment && go test ./...   # SD assignments
cd study-tracker && go build -o srs . && go test ./... && ./srs init && ./srs today
```

**Node / Playwright:**
```bash
cd playwright && npm ci && npx playwright install --with-deps && npm test
```

## Conventions (must follow)

- **DSA & SDET problems**: `package ra.hul.{dsa|sdet}.<topic>;`, class `Ques{N}_{PascalName}` matching
  the filename, solution logic in `static` methods, helper types as **nested `static` classes** (never
  top-level — they collide within a package). Every file has a **Java 25 `static void main()`** (no
  `String[] args`) that runs the examples and prints results/`PASSED` (self-verifying without `-ea`).
- **Do not duplicate live-browser Selenium** in `sdet/` — that's covered by `framework/` (Java) and
  `playwright/` (TS). Cross-reference instead.
- **Framework module** has its own detailed rules in [`framework/CLAUDE.md`](framework/CLAUDE.md)
  (config resolution, ThreadLocal drivers, POM enforcement, TestNG suites). Read it before editing there.
- **`playwright/` is a git subtree** from `github.com/heyrmi/hlpw`. Avoid editing its files directly so
  upstream sync stays clean: `git subtree pull --prefix=playwright https://github.com/heyrmi/hlpw main --squash`.
- **Company bank** is generated from the public ShapeMyInterview API
  (`api.shapemyinterview.com/api/problems?company=<slug>`); regenerate rather than hand-editing.
- After adding DSA/SDET/SD content, re-run `cd study-tracker && ./srs init` so the tracker indexes it.

## Commit policy

**Do NOT add AI attribution to commits** (no `Co-Authored-By` trailers, no "generated with" lines).
Commits are authored by the repo owner only. This is a deliberate, standing preference for this repo.
