# `srs` — Spaced-Repetition Study Tracker

> One scheduler for the **whole repo** — [System Design](../sd/) + [DSA](../dsa/) +
> [SDET practical](../sdet/). It turns every lesson and problem into a daily review queue using
> the **SM-2 (Anki-style) algorithm**, so the right material resurfaces *right before you'd
> forget it*. Dependency-free Go — one binary.
>
> It auto-discovers: SD lessons + case studies, the Maven DSA problems
> (`dsa/src/.../Ques*.java`) + orientation notes, and the SDET practical problems + question
> guides. Re-run `srs init` whenever you add content.

---

## Why spaced repetition

You don't forget at a steady rate — you forget along a curve, fast at first then slowing. If you
**review an item just as it's about to fade**, the memory strengthens and the next safe gap gets
longer (1 day → 6 days → ~2 weeks → ~1 month → …). Reviewing too early wastes time; too late and
you've forgotten. SM-2 estimates that sweet spot per item from how hard each review felt.

This tracker applies that to *active recall*, the highest-yield way to study:

- **Lessons** → "re-explain it out loud as if teaching a friend, then check what you missed."
- **Problems** → "re-solve it from scratch, run the tests, then grade how it went."

Recalling/re-deriving (not re-reading) is what builds durable memory and interview fluency.

---

## Install / run

You need Go (you already have it). From this directory:

```bash
# easiest: build a binary once and put it on your PATH
go build -o srs .
./srs init          # or move ./srs somewhere on $PATH

# or run without building:
go run . init
```

> Optional alias so you can type `srs` from anywhere:
> ```bash
> alias srs="$(pwd)/srs"      # after `go build -o srs .`
> ```

State is stored in `~/.srs/state.json` (override with `$SRS_STATE`). It holds your whole
schedule and history, so it's safe to rebuild the binary anytime.

---

## The daily workflow

```bash
srs init        # once — scans ../sd, ../dsa and ../sdet and builds your deck (re-run after adding content)
srs status      # your dashboard: what's due, your streak, the 7-day forecast
srs today       # do your reviews — this is the daily habit
```

That's it. Do `srs today` once a day. It serves everything **due** plus a few **new** items
(default 8/day), one at a time. For each: go do the recall/re-solve, come back, and grade:

| key | grade | effect |
|----|-------|--------|
| `1` | **again** | you blanked — card resets, comes back tomorrow |
| `2` | **hard**  | recalled with real difficulty — small interval |
| `3` | **good**  | solid — normal interval growth (× your ease factor) |
| `4` | **easy**  | trivial — bigger jump, ease increases |
| `s` | skip | leave it for later |
| `q` | quit | progress is saved after every grade |

Be honest with grades — the schedule is only as good as your self-assessment.

---

## All commands

```
srs init [--sd PATH] [--dsa PATH] [--sdet PATH] [--new N]   build/refresh the deck; set new-items-per-day
srs today [--max N] [--new N] [--course sd|dsa]  interactive review session
srs status                                       dashboard (also the default with no args)
srs next                                         print just the next item + how to grade it
srs grade <item-id> <again|hard|good|easy>       grade one item non-interactively
srs list [--all|--due|--new] [--course sd|dsa]   list items and their schedule
srs help
```

Examples:

```bash
srs today --course dsa          # only review DSA today
srs today --new 0               # reviews only, don't introduce new items (busy day)
srs today --new 15              # cram more new material (interview soon)
srs list --due                  # what's due right now
srs grade dsa/02-patterns/05-binary-search/problems/07-koko-eating-bananas good
```

---

## How the schedule works (SM-2)

Each item carries an **ease factor** (starts 2.5, floor 1.3) and an **interval**.

- The first two successful reviews use fixed steps: **1 day**, then **6 days**.
- After that, each `good` multiplies the interval by the ease (`hard` uses ×1.2, `easy` uses
  ease ×1.3 and bumps ease up; `hard` nudges ease down).
- `again` resets the item to 1 day, records a *lapse*, and lowers ease so you see it more often.

So a card you keep nailing marches out to weeks, then months — while a shaky one stays close.
Items are classed as **new → learning (<6d) → young (<21d) → mature (≥21d)** on the dashboard.

---

## Tips

- **Consistency beats volume.** 20 minutes every day crushes 3 hours once a week — that's the
  whole point of the curve. The streak counter is there to nudge you.
- **Tune `--new`.** Ramp it up when you're fresh and have time; drop to `--new 0` when the review
  backlog is heavy (clear debt before taking on more).
- **Re-run `srs init`** anytime you (or future-you) add lessons/problems — your progress is kept,
  new items just appear as `new`.
- Pair it with the courses' own **Self-check** questions (lessons) and **test suites** (problems)
  as your grading rubric: tests pass on the first try from memory? that's an `easy`/`good`.

Verified: `go test ./...` covers the SM-2 scheduling math, the queue/daily-cap logic, and the
course scanner.
