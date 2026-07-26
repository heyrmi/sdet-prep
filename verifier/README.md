# `verifier/` — the regression gate for every problem

Every problem in `dsa/` and `sdet/` is self-verifying: its `static void main()` runs the examples and
prints results. That convention is only worth anything if something actually *runs* them — otherwise
a refactor breaks a problem and nobody notices for months.

This module is that something. It reflectively invokes `main()` on all **266** `Ques*` classes and
fails the build on any exception, hang, or failed self-check.

## Run it

```bash
mvn clean install -DskipTests     # the verifier depends on the dsa + sdet artifacts
mvn -pl verifier test             # offline mode (default): 256 run, 10 skipped, ~3s
mvn -pl verifier test -Dverifier.offline=false    # include the networked problems
```

| Property | Default | Meaning |
|---|---|---|
| `verifier.offline` | `true` | Skip problems listed in `src/test/resources/network-dependent.txt` |
| `verifier.timeoutSeconds` | `60` | Per-problem timeout; a hang fails rather than blocking forever |

## What counts as a failure

1. `main()` throws.
2. `main()` doesn't finish inside the timeout (infinite loop, blocking call).
3. Output contains a line starting with `FAIL:` / `FAILED:` / `ERROR:` — the repo's failure
   convention, the counterpart to `PASSED:`.
4. A `=== N passed, M failed ===` summary reports `M > 0`.
5. `main()` prints nothing — a self-verifying problem that says nothing verified nothing.

Detection is **line-anchored on purpose**. A bare substring search for `FAILED` produces false
positives on narrative output like `attempt 1: FAILED -> retry after 1ms` in
`builds/Ques6_RetryFramework`, which is a passing problem describing a retry.

## The skip list

`src/test/resources/network-dependent.txt` lists the 10 problems that need a live service or a real
browser. They are skipped in the PR gate (hermetic, no third-party flakiness) and run by
`.github/workflows/nightly.yml` with `-Dverifier.offline=false`.

Adding a networked problem = adding one line to that file. If you add a problem that *doesn't* need
network, do nothing — it is discovered and gated automatically.

## Where it runs

- **`.github/workflows/ci.yml`** — on every push and PR. Offline mode. This is the merge gate.
- **`.github/workflows/nightly.yml`** — 03:00 UTC daily. Full set, including networked problems.
