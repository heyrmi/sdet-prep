// Package testplatform is the reference solution for Module 5.1.
//
// Points worth defending in an interview:
//
//   - Suite wall-clock is the SLOWEST shard, not the average. Optimising mean shard time is the
//     classic mistake; it improves a number nobody waits on.
//
//   - LPT (longest-processing-time-first) is the right greedy: sort descending, always feed the
//     least-loaded shard. Guaranteed within 4/3 of optimal. Sorting ascending is a real and
//     common bug — the big tests then arrive with no room left to balance against.
//
//   - Sharding must be deterministic. A developer re-running "shard 3" after a failure must get
//     the same 250 tests, or the re-run proves nothing.
//
//   - OptimalWorkerCount is the business answer: past that point, more CI machines buy nothing,
//     and the real fix is splitting the monolithic test.
package testplatform

import (
	"sort"
	"time"
)

// Test is one executable unit with its observed cost.
type Test struct {
	ID       string
	Duration time.Duration
	Tags     []string
}

// Shard is the work assigned to one worker.
type Shard struct {
	WorkerID int
	Tests    []Test
}

// Total is the shard's predicted wall-clock time.
func (s Shard) Total() time.Duration {
	var sum time.Duration
	for _, t := range s.Tests {
		sum += t.Duration
	}
	return sum
}

// DefaultDuration is assumed for a test with no history.
const DefaultDuration = 5 * time.Second

// EffectiveDuration is the duration used for planning.
func EffectiveDuration(t Test) time.Duration {
	if t.Duration <= 0 {
		return DefaultDuration
	}
	return t.Duration
}

// effectiveTotal sums a shard using planning durations, so unrun tests are not free.
func effectiveTotal(s Shard) time.Duration {
	var sum time.Duration
	for _, t := range s.Tests {
		sum += EffectiveDuration(t)
	}
	return sum
}

// sortedForLPT returns a copy sorted longest-first, with a deterministic tie-break.
func sortedForLPT(tests []Test) []Test {
	out := make([]Test, len(tests))
	copy(out, tests) // never reorder the caller's slice
	sort.Slice(out, func(i, j int) bool {
		di, dj := EffectiveDuration(out[i]), EffectiveDuration(out[j])
		if di != dj {
			return di > dj // DESCENDING is what makes LPT work
		}
		return out[i].ID < out[j].ID // deterministic across runs
	})
	return out
}

// ShardTests splits tests across workerCount workers, balancing predicted duration.
func ShardTests(tests []Test, workerCount int) []Shard {
	if workerCount < 1 {
		return nil
	}

	shards := make([]Shard, workerCount)
	for i := range shards {
		shards[i] = Shard{WorkerID: i}
	}

	loads := make([]time.Duration, workerCount)
	for _, t := range sortedForLPT(tests) {
		// Feed the currently least-loaded shard; lowest WorkerID wins ties.
		lightest := 0
		for i := 1; i < workerCount; i++ {
			if loads[i] < loads[lightest] {
				lightest = i
			}
		}
		shards[lightest].Tests = append(shards[lightest].Tests, t)
		loads[lightest] += EffectiveDuration(t)
	}
	return shards
}

// CriticalPath returns the duration of the slowest shard — the suite's true wall-clock.
func CriticalPath(shards []Shard) time.Duration {
	var max time.Duration
	for _, s := range shards {
		if total := effectiveTotal(s); total > max {
			max = total
		}
	}
	return max
}

// MinimumPossible returns the floor on wall-clock: the single longest test.
func MinimumPossible(tests []Test) time.Duration {
	var max time.Duration
	for _, t := range tests {
		if d := EffectiveDuration(t); d > max {
			max = d
		}
	}
	return max
}

// OptimalWorkerCount returns the smallest worker count that reaches MinimumPossible.
func OptimalWorkerCount(tests []Test) int {
	if len(tests) == 0 {
		return 0
	}
	floor := MinimumPossible(tests)
	for n := 1; n <= len(tests); n++ {
		if CriticalPath(ShardTests(tests, n)) <= floor {
			return n
		}
	}
	return len(tests)
}

// Worker describes a machine and what it can run.
type Worker struct {
	ID           int
	Capabilities []string
}

// CanRun reports whether the worker satisfies every tag the test requires.
func (w Worker) CanRun(t Test) bool {
	for _, need := range t.Tags {
		found := false
		for _, have := range w.Capabilities {
			if have == need {
				found = true
				break
			}
		}
		if !found {
			return false // EVERY tag must be satisfied, not just one
		}
	}
	return true
}

// ShardWithConstraints assigns tests only to workers that can run them.
func ShardWithConstraints(tests []Test, workers []Worker) ([]Shard, []Test) {
	shards := make([]Shard, len(workers))
	loads := make([]time.Duration, len(workers))
	for i, w := range workers {
		shards[i] = Shard{WorkerID: w.ID}
	}

	var unschedulable []Test
	for _, t := range sortedForLPT(tests) {
		lightest := -1
		for i, w := range workers {
			if !w.CanRun(t) {
				continue
			}
			if lightest == -1 || loads[i] < loads[lightest] {
				lightest = i
			}
		}
		if lightest == -1 {
			// Surfaced, never dropped: a silently unscheduled test is lost coverage that
			// nobody notices until it matters.
			unschedulable = append(unschedulable, t)
			continue
		}
		shards[lightest].Tests = append(shards[lightest].Tests, t)
		loads[lightest] += EffectiveDuration(t)
	}
	return shards, unschedulable
}
