#!/usr/bin/env bash
#
# check-coverage.sh — diff the SDET guide against what is actually implemented.
#
# The guide documents 87 `### QuesN` entries across 13 categories; the source tree has fewer.
# Some of that gap is deliberate (live-browser Selenium is covered by framework/ and playwright/
# per CLAUDE.md), and some of it is just drift. Before this script, nothing recorded which was
# which — so the gap was invisible and grew silently.
#
# Usage:
#   scripts/check-coverage.sh              # report the gap
#   scripts/check-coverage.sh --strict     # exit 1 if any UNEXPLAINED gap exists
#
# A category is "explained" when EXPECTED_GAPS below says why it is short. Anything else is
# drift and should either be implemented or added to that list with a reason.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GUIDE="$REPO_ROOT/sdet/SDET_INTERVIEW_QUESTIONS.md"
SRC="$REPO_ROOT/sdet/src/main/java/ra/hul/sdet"

STRICT=0
[[ "${1:-}" == "--strict" ]] && STRICT=1

# Guide category number -> source package directory.
declare -a CATEGORY_DIRS=(
  [1]="fileops"
  [2]="scraping"
  [3]="api"
  [4]="dataprocessing"
  [5]="selenium"
  [6]="database"
  [7]="linux"
  [8]="multithreading"
  [9]="designpatterns"
  [10]="regex"
  [11]="performance"
  [12]="builds"
  [13]=""            # Framework Design & Architecture is discussion-only, no runnable problems
)

# Categories that are deliberately short, with the reason. Format: [n]="reason"
declare -a EXPECTED_GAPS=(
  [2]="live HTTP scraping is covered by playwright/ network-intercept tests; only one offline example is kept here"
  [3]="OAuth2, multipart upload and GraphQL need a live authenticated endpoint — covered by framework/ (RestAssured) and playwright/ (API suites); the 6 kept here are the ones worth reading as standalone code"
  [5]="live-browser Selenium is deliberately NOT duplicated here — see framework/ (Java) and playwright/ (TS), per CLAUDE.md"
  [7]="SSH remote execution needs a real remote host; the offline half (shell exec, filesystem, process monitor, multi-host log aggregation) is implemented"
  [11]="load testing against live endpoints is covered by framework/ Gatling; only offline harness examples are kept here"
  [13]="discussion/architecture questions — no runnable code by design"
)

printf '%-4s %-22s %8s %8s %8s  %s\n' "#" "category" "guide" "impl" "gap" "status"
printf '%s\n' "------------------------------------------------------------------------------"

total_guide=0
total_impl=0
unexplained=0

for n in $(seq 1 13); do
  dir="${CATEGORY_DIRS[$n]:-}"

  # Guide count: `### QuesN` entries between this `## n.` heading and the next `## ` heading.
  guide_count=$(awk -v cat="^## ${n}\\. " '
    $0 ~ cat        { inside = 1; next }
    /^## [0-9]+\. / { inside = 0 }
    inside && /^### Ques/ { count++ }
    END { print count + 0 }
  ' "$GUIDE")

  if [[ -n "$dir" && -d "$SRC/$dir" ]]; then
    impl_count=$(find "$SRC/$dir" -name 'Ques*.java' | wc -l | tr -d ' ')
  else
    impl_count=0
  fi

  gap=$(( guide_count - impl_count ))
  (( gap < 0 )) && gap=0

  label="${dir:-<discussion only>}"
  reason="${EXPECTED_GAPS[$n]:-}"

  if (( gap == 0 )); then
    status="ok"
  elif [[ -n "$reason" ]]; then
    status="expected"
  else
    status="DRIFT"
    unexplained=$(( unexplained + gap ))
  fi

  printf '%-4s %-22s %8s %8s %8s  %s\n' "$n" "$label" "$guide_count" "$impl_count" "$gap" "$status"

  total_guide=$(( total_guide + guide_count ))
  total_impl=$(( total_impl + impl_count ))
done

printf '%s\n' "------------------------------------------------------------------------------"
printf '%-4s %-22s %8s %8s %8s\n' "" "TOTAL" "$total_guide" "$total_impl" "$(( total_guide - total_impl ))"

echo
echo "Explained gaps:"
for n in $(seq 1 13); do
  reason="${EXPECTED_GAPS[$n]:-}"
  [[ -n "$reason" ]] && printf '  %2s. %s\n' "$n" "$reason"
done

# Packages with no guide category at all — new pillars added after the guide was written.
echo
echo "Implemented but not in the guide (newer pillars):"
for d in "$SRC"/*/; do
  name=$(basename "$d")
  found=0
  for n in $(seq 1 13); do
    [[ "${CATEGORY_DIRS[$n]:-}" == "$name" ]] && found=1
  done
  if (( found == 0 )); then
    count=$(find "$d" -name 'Ques*.java' | wc -l | tr -d ' ')
    printf '  %-20s %s problems\n' "$name" "$count"
  fi
done

echo
if (( unexplained > 0 )); then
  echo "RESULT: $unexplained unexplained gap(s) — implement them, or add a reason to EXPECTED_GAPS."
  (( STRICT == 1 )) && exit 1
else
  echo "RESULT: every gap between the guide and the source tree is accounted for."
fi
exit 0
