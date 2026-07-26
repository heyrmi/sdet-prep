// Package testplatform is the Module 5.1 assignment: the scheduling core of a test
// automation platform.
//
// Read 05-sdet-system-design/01-design-a-test-automation-platform.md first.
//
// Fill in every function marked `// TODO`. Run the tests until green:
//
//	go test ./...
//	go test -race ./...
//
// The framing: 10,000 tests, a 90-minute serial suite, and a promise of "PR feedback in under
// 10 minutes". You have 40 workers. Naive round-robin sharding gives you a 30-minute shard and
// 39 idle workers — the wall-clock is set by the SLOWEST shard, not the average.
package testplatform

import (
	"sort"
	"time"
)

// Test is one executable unit with its observed cost.
type Test struct {
	ID string
	// Duration is the p50 from recent history. Zero means "never run before".
	Duration time.Duration
	// Tags carry scheduling constraints, e.g. "browser", "gpu", "serial".
	Tags []string
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

// DefaultDuration is assumed for a test with no history. Guessing too low starves the shard
// that receives a batch of new tests; too high wastes capacity. The p75 of the suite is a
// defensible middle, but a fixed constant keeps this assignment deterministic.
const DefaultDuration = 5 * time.Second

// ----------------------------------------------------------------------------
// 1) Balanced sharding
//
// This is multiway number partitioning — NP-hard, so you want the standard greedy
// approximation: Longest Processing Time first (LPT).
//
//	Sort tests by duration DESCENDING, then repeatedly assign the next test to the
//	shard that is currently least loaded.
//
// LPT is guaranteed within 4/3 of optimal, and in practice much closer. Sorting ascending
// instead (a common bug) produces markedly worse balance, because the big tests arrive last
// when there is no room left to compensate.
//
// Determinism matters: the same input must always produce the same shards, or a developer
// re-running a failed shard gets a different set of tests.
// ----------------------------------------------------------------------------

// Shard splits tests across workerCount workers, balancing predicted duration.
// Tests with a zero Duration are costed at DefaultDuration for planning purposes
// (their reported Duration in the returned Shard stays zero — do not mutate the input).
// Returns exactly workerCount shards, some possibly empty. Returns nil if workerCount < 1.
func ShardTests(tests []Test, workerCount int) []Shard {
	// TODO:
	//  1. Guard workerCount < 1 -> nil.
	//  2. Copy the input so sorting does not mutate the caller's slice.
	//  3. Sort by effective duration DESCENDING; tie-break on ID ASCENDING for determinism.
	//  4. Initialise workerCount shards with WorkerID 0..n-1.
	//  5. For each test, append it to the shard with the smallest current effective load.
	//     Tie-break on the lowest WorkerID so the result is reproducible.
	//  6. Return the shards.
	return nil
}

// EffectiveDuration is the duration used for planning: the observed one, or DefaultDuration
// when the test has never run.
func EffectiveDuration(t Test) time.Duration {
	if t.Duration <= 0 {
		return DefaultDuration
	}
	return t.Duration
}

// ----------------------------------------------------------------------------
// 2) Critical path
//
// The suite's wall-clock is the slowest shard, so that is the number to report and optimise.
// It also tells you when adding workers has stopped helping: once the longest single TEST
// exceeds the average shard load, more workers cannot reduce wall-clock at all.
// ----------------------------------------------------------------------------

// CriticalPath returns the duration of the slowest shard — the suite's true wall-clock.
func CriticalPath(shards []Shard) time.Duration {
	// TODO: return the maximum effective total across shards (0 for no shards).
	return 0
}

// MinimumPossible returns the theoretical floor on wall-clock: no matter how many workers you
// add, the suite cannot finish faster than its single longest test.
func MinimumPossible(tests []Test) time.Duration {
	// TODO: return the maximum EffectiveDuration across tests.
	return 0
}

// OptimalWorkerCount returns the smallest worker count at which adding another worker would
// not reduce the critical path — i.e. where the critical path has reached MinimumPossible.
// Never returns more than len(tests), and returns 0 for an empty suite.
//
// This is the answer to "should we buy more CI machines?", which is the question a test
// platform actually exists to answer.
func OptimalWorkerCount(tests []Test) int {
	// TODO:
	//  1. Empty suite -> 0.
	//  2. floor := MinimumPossible(tests).
	//  3. Try n = 1..len(tests); return the first n where CriticalPath(ShardTests(tests, n)) <= floor.
	//  4. Fall back to len(tests).
	return 0
}

// ----------------------------------------------------------------------------
// 3) Constraint-aware sharding
//
// Real suites are not homogeneous. Some tests need a browser, some need a GPU, and some must
// not run in parallel with each other at all. A shard must be runnable by the worker it lands
// on, so tests are grouped by their required capability BEFORE balancing within each group.
// ----------------------------------------------------------------------------

// Worker describes a machine and what it can run.
type Worker struct {
	ID           int
	Capabilities []string
}

// CanRun reports whether the worker satisfies every tag the test requires.
func (w Worker) CanRun(t Test) bool {
	// TODO: every tag in t.Tags must appear in w.Capabilities. No tags => any worker.
	return false
}

// ShardWithConstraints assigns tests only to workers that can run them, balancing load within
// each eligible pool. Returns one shard per worker, plus the tests no worker can run at all —
// those must be surfaced loudly, not silently dropped, or coverage vanishes without a trace.
func ShardWithConstraints(tests []Test, workers []Worker) (shards []Shard, unschedulable []Test) {
	// TODO:
	//  1. Initialise one shard per worker, preserving worker IDs.
	//  2. Sort tests by effective duration DESCENDING, tie-break ID ASCENDING.
	//  3. For each test, find eligible workers (CanRun). If none, add to unschedulable.
	//  4. Otherwise assign to the eligible shard with the smallest load (tie-break lowest WorkerID).
	//  5. Return both slices.
	return nil, nil
}

var _ = sort.Slice
