#!/usr/bin/env bash
# Run every assignment in the course.
#
#   ./run-tests.sh            # check that YOUR code in each assignment/ passes
#   ./run-tests.sh --solution # check the reference solutions (copies tests into solution/)
#
# Exit code is non-zero if anything fails.
set -uo pipefail
cd "$(dirname "$0")"

mode="${1:-assignment}"
pass=0; fail=0; failed=""

for adir in $(find . -type d -name assignment | sort); do
  base=$(dirname "$adir")
  if [ "$mode" = "--solution" ]; then
    sdir="$base/solution"
    [ -d "$sdir" ] || continue
    cp "$adir"/*_test.go "$sdir"/ 2>/dev/null
    if (cd "$sdir" && go test -race ./... >/tmp/sdtest.log 2>&1); then
      echo "PASS (solution): $base"; pass=$((pass+1))
    else
      echo "FAIL (solution): $base"; fail=$((fail+1)); failed="$failed\n  $base"
    fi
    rm -f "$sdir"/*_test.go
  else
    if (cd "$adir" && go test -race ./... >/tmp/atest.log 2>&1); then
      echo "PASS: $base"; pass=$((pass+1))
    else
      echo "FAIL: $base   (keep coding — see: cd $adir && go test ./...)"; fail=$((fail+1)); failed="$failed\n  $base"
    fi
  fi
done

echo "----------------------------------------"
echo "PASS=$pass  FAIL=$fail"
[ -n "$failed" ] && printf "Failing:%b\n" "$failed"
[ "$fail" -eq 0 ]
