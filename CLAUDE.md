# CLAUDE.md

Guidance for AI assistants (Claude Code and others) working in this repository. Keep this file
updated when the structure, tooling, or conventions change.

## What this is

**SDET Interview Prep — a polyglot "mega-repo"** covering an entire FAANG-level SDET interview loop.
Seven pillars:

| Pillar | Path | Stack | What it is |
|--------|------|-------|-----------|
| DSA | `dsa/` | Java 25 / Maven | ~205 runnable problems, `Ques{N}_{Name}.java`, one per file |
| SDET practical | `sdet/` | Java 25 / Maven | ~76 practical problems (incl. `aiqa/` and `propertytesting/`) + 932-question company bank |
| Verifier | `verifier/` | Java 25 / Maven | Runs every problem's `main()` as the PR gate; mutation-testing profile |
| Framework (Java) | `framework/` | Java 21 / Maven | Selenium+RestAssured+Appium+Gatling framework (vendored from `heyrmi/framework` via git subtree) |
| Framework (TS) | `playwright/` | TypeScript / Node 24 | Playwright UI+API framework (vendored from `heyrmi/hlpw` via git subtree) |
| System Design | `sd/` | Markdown + Go | 45+ lessons across modules 00–07, with **28 Go assignments** |
| Tracker | `study-tracker/` | Go | `srs` spaced-repetition CLI indexing DSA+SDET+SD |
| Company bank | `sdet/company-questions/` | Markdown | 932 Qs × 32 companies + JioStar deep-dive |

**`sd/` modules:** 00-foundations · 01-networking · 02-building-blocks · 03-distributed-systems ·
04-case-studies (17) · **05-sdet-system-design** (5 lessons + 5 assignments) ·
**06-ai-system-design** (6 + 3) · **07-testing-distributed-systems** (3 + 1).

## Build / run / test (by toolchain)

**Java (Maven reactor = `dsa`, `sdet`, `verifier`):**
```bash
mvn clean install -DskipTests          # build the reactor (verifier depends on dsa + sdet artifacts)
mvn -pl verifier test                  # THE GATE: runs all 281 problems' main(), ~3s
mvn -pl verifier test -Pmutation       # mutation-test the verifier's oracle (pitest)
mvn -pl dsa exec:java -Dexec.mainClass="ra.hul.dsa.arrays.Ques1_TwoSum"   # run a DSA problem
mvn -pl sdet exec:java -Dexec.mainClass="ra.hul.sdet.fileops.Ques2_LogParser"
cd framework && mvn test -Pweb         # framework builds standalone: -Pweb/-Papi/-Pmobile/-Psmoke/-Pvisual/-Pa11y/-Pcontract
```

**Drift check:**
```bash
./scripts/check-coverage.sh            # SDET guide vs implemented problems
./scripts/check-coverage.sh --strict   # exit 1 on any UNEXPLAINED gap (this runs in CI)
```
`sd/`, `study-tracker/`, `playwright/`, and `framework/` are intentionally **outside** the Maven reactor
(`framework/` is a self-contained subtree with its own standalone pom — see below).

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
- **`framework/` is a git subtree** from `github.com/heyrmi/framework` (self-contained, standalone pom,
  builds outside the reactor). Avoid editing its files directly so upstream sync stays clean; make changes
  upstream in `heyrmi/framework` then: `git subtree pull --prefix=framework https://github.com/heyrmi/framework main --squash`
  (and `git subtree push` to send local fixes up).
- **`playwright/` is a git subtree** from `github.com/heyrmi/hlpw`. Avoid editing its files directly so
  upstream sync stays clean: `git subtree pull --prefix=playwright https://github.com/heyrmi/hlpw main --squash`.
- **Company bank** is generated from the public ShapeMyInterview API
  (`api.shapemyinterview.com/api/problems?company=<slug>`); regenerate rather than hand-editing.
- After adding DSA/SDET/SD content, re-run `cd study-tracker && ./srs init` so the tracker indexes it.

### Rules that CI enforces (so don't break them silently)

- **Every problem must print something and self-verify.** `verifier/` fails the build on an
  exception, a hang (60s), empty output, a line starting `FAIL:`/`FAILED:`/`ERROR:`, or a
  `=== N passed, M failed ===` summary with `M > 0`. New problems are discovered automatically.
- **Never use repo-relative paths** like `sdet/src/main/resources/...` — they only work when CWD is
  the repo root. Resolve test data through the classpath (see `fileops/Ques1_FileComparator`).
- **A problem that needs network or a browser** goes in
  `verifier/src/test/resources/network-dependent.txt`, one FQCN per line. It then runs only in the
  nightly job.
- **`sd/` assignments must start RED and their solutions must pass** the assignment's own test file.
  CI copies the test into the solution dir and runs it.
- **Adding an `sd/` module?** Add it to `lessonModules` in `study-tracker/catalog.go`, or its
  lessons silently never enter the review deck.
- **A documented-but-unimplemented SDET question** needs a reason in `EXPECTED_GAPS` in
  `scripts/check-coverage.sh`, or the `doc-drift` CI job fails.

## Commit policy

**Do NOT add AI attribution to commits** (no `Co-Authored-By` trailers, no "generated with" lines).
Commits are authored by the repo owner only. This is a deliberate, standing preference for this repo.
