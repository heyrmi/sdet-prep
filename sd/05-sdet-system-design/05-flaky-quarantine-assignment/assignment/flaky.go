// Package flakyquarantine is the Module 5.5 assignment: build the detection and quarantine
// engine that sits behind a flaky-test dashboard.
//
// Read 05-sdet-system-design/05-flaky-test-detection-and-quarantine.md first.
//
// Fill in every function marked `// TODO`. Run the tests until green:
//
//	go test ./...
//	go test -race ./...   // the registry is written from many CI workers at once
//
// The interview framing: "10,000 tests, 40 merges a day, and the suite is red half the time.
// Nobody trusts it. Design the system that fixes that." Detection is the easy half — the hard
// half is the policy: when to quarantine, when to let something back in, and how to stop
// quarantine from becoming a graveyard where broken tests go to be forgotten.
package flakyquarantine

import (
	"sort"
	"sync"
	"time"
)

// Outcome is the result of one execution of one test.
type Outcome struct {
	TestID  string
	Passed  bool
	Commit  string    // the SHA under test — same commit + different result == flake
	At      time.Time
	Runtime time.Duration
}

// State is where a test sits in its quarantine lifecycle.
type State int

const (
	// Healthy: running in the merge gate, failures block merges.
	Healthy State = iota
	// Quarantined: still executed, but failures no longer block merges. The signal is kept
	// (it runs, we record it) while the blocking power is removed.
	Quarantined
	// Probation: candidate for release from quarantine — it has looked stable for a while,
	// so we watch it for a probation window before trusting it in the gate again.
	Probation
)

func (s State) String() string {
	switch s {
	case Healthy:
		return "healthy"
	case Quarantined:
		return "quarantined"
	case Probation:
		return "probation"
	}
	return "unknown"
}

// Policy holds the thresholds that decide quarantine transitions.
type Policy struct {
	// Window is how many recent outcomes per test to keep and score over.
	Window int
	// FlakeRateThreshold quarantines a test whose flake rate meets or exceeds it (0.0–1.0).
	FlakeRateThreshold float64
	// MinRuns is the minimum number of outcomes before a test can be quarantined at all.
	// Without this a single same-commit disagreement on a brand-new test quarantines it.
	MinRuns int
	// ConsecutivePassesToRelease is how many clean runs move Quarantined -> Probation.
	ConsecutivePassesToRelease int
	// ProbationRuns is how many further clean runs move Probation -> Healthy.
	// A failure during probation sends it straight back to Quarantined.
	ProbationRuns int
	// MaxQuarantineDuration caps how long a test may sit quarantined before it is reported
	// as expired. This is the anti-graveyard rule: quarantine is a loan, not a write-off.
	MaxQuarantineDuration time.Duration
}

// DefaultPolicy is a reasonable starting point for a large suite.
func DefaultPolicy() Policy {
	return Policy{
		Window:                     50,
		FlakeRateThreshold:         0.05, // 5% — one flake in 20 runs is already expensive
		MinRuns:                    10,
		ConsecutivePassesToRelease: 20,
		ProbationRuns:              10,
		MaxQuarantineDuration:      14 * 24 * time.Hour,
	}
}

// TestRecord is the tracked history and current state of a single test.
type TestRecord struct {
	TestID        string
	State         State
	Outcomes      []Outcome // most recent last, capped at Policy.Window
	QuarantinedAt time.Time
	// consecutivePasses counts clean runs since the last failure. Reset to 0 on any failure.
	consecutivePasses int
	// probationPasses counts clean runs accumulated while in Probation.
	probationPasses int
}

// Registry is the concurrent store of every test's history. CI workers call Record
// from many goroutines; the dashboard calls the read methods concurrently.
type Registry struct {
	mu      sync.RWMutex
	policy  Policy
	records map[string]*TestRecord
	now     func() time.Time
}

// NewRegistry creates an empty registry governed by the given policy.
func NewRegistry(p Policy) *Registry {
	return &Registry{
		policy:  p,
		records: make(map[string]*TestRecord),
		now:     time.Now,
	}
}

// ----------------------------------------------------------------------------
// 1) Flake rate
//
// The definition that matters: a test is flaky when it produces DIFFERENT results for the
// SAME commit. A test that fails consistently on a commit is not flaky — it is broken, and
// quarantining it would hide a real bug.
//
// Score = (number of commits with both a pass and a fail) / (number of commits with >1 run).
// Commits that ran only once carry no flake signal and must not dilute the denominator.
// ----------------------------------------------------------------------------

// FlakeRate returns the share of multi-run commits that disagreed with themselves,
// in the range 0.0–1.0. Returns 0 when no commit has more than one run.
func FlakeRate(outcomes []Outcome) float64 {
	// TODO:
	//  1. Group outcomes by Commit.
	//  2. Ignore any commit with fewer than 2 runs (no signal).
	//  3. Count a commit as "disagreeing" if it has at least one pass AND at least one fail.
	//  4. Return disagreeing / multiRunCommits, or 0 if there are no multi-run commits.
	return 0
}

// ----------------------------------------------------------------------------
// 2) Recording outcomes and driving the state machine
//
//	Healthy     --flake rate >= threshold (and >= MinRuns)--> Quarantined
//	Quarantined --ConsecutivePassesToRelease clean runs-----> Probation
//	Probation   --ProbationRuns further clean runs---------> Healthy
//	Probation   --any failure------------------------------> Quarantined
//
// A test in Quarantined or Probation must NOT be re-quarantined by the flake-rate rule —
// it is already contained, and its stale history would keep it pinned forever.
// ----------------------------------------------------------------------------

// Record appends an outcome and applies the policy, returning the record's state afterwards.
func (r *Registry) Record(o Outcome) State {
	// TODO:
	//  1. Lock for write. Look up or create the TestRecord for o.TestID.
	//  2. Append the outcome; trim rec.Outcomes to the newest policy.Window entries.
	//  3. Update rec.consecutivePasses: increment on pass, reset to 0 on fail.
	//  4. Apply the transitions above (see applyPolicy — you may implement it there).
	//  5. Return the resulting state.
	return Healthy
}

// applyPolicy runs the state machine for one record. Called with the write lock held.
func (r *Registry) applyPolicy(rec *TestRecord, last Outcome) {
	// TODO: implement the four transitions described above.
	//  - Entering Quarantined must stamp rec.QuarantinedAt = r.now() and zero the counters.
	//  - Entering Probation must zero rec.probationPasses.
	//  - Returning to Healthy must zero both counters.
}

// ----------------------------------------------------------------------------
// 3) Reads the dashboard and the CI gate need
// ----------------------------------------------------------------------------

// IsBlocking reports whether a failure of this test should fail the build.
// Quarantined and Probation tests still run, but only Healthy ones can block a merge.
func (r *Registry) IsBlocking(testID string) bool {
	// TODO: unknown tests are treated as Healthy (a brand-new test blocks by default).
	return true
}

// Quarantined returns the IDs of all quarantined tests, sorted, for the dashboard.
func (r *Registry) QuarantinedTests() []string {
	// TODO: include Quarantined only — Probation tests are on their way out, report separately.
	return nil
}

// ExpiredQuarantines returns tests that have been quarantined longer than
// policy.MaxQuarantineDuration, sorted. These need a human: either fix them or delete them.
// This is what stops quarantine from silently becoming a dumping ground.
func (r *Registry) ExpiredQuarantines() []string {
	// TODO: compare r.now().Sub(rec.QuarantinedAt) against policy.MaxQuarantineDuration.
	return nil
}

// ----------------------------------------------------------------------------
// 4) Ranking — where should a human spend their next hour?
//
// Flake rate alone is the wrong ranking. A test that flakes 50% of the time but runs twice a
// week wastes less engineering time than one that flakes 5% but runs on all 40 daily merges.
// Rank by COST: expected failed runs per day x how long each one wastes.
// ----------------------------------------------------------------------------

// Impact is a test's estimated daily cost from flaking.
type Impact struct {
	TestID    string
	FlakeRate float64
	RunsPerDay float64
	// WastedPerDay is the estimated engineer-time lost per day:
	//   FlakeRate * RunsPerDay * (test runtime + TriageOverhead)
	WastedPerDay time.Duration
}

// TriageOverhead is the fixed human cost of one flaky failure: someone notices the red build,
// checks the log, decides it is unrelated, and hits retry.
const TriageOverhead = 12 * time.Minute

// RankByImpact returns every tracked test with a non-zero flake rate, most costly first.
// runsPerDay maps a test ID to its observed execution frequency.
func (r *Registry) RankByImpact(runsPerDay map[string]float64) []Impact {
	// TODO:
	//  1. For each record, compute FlakeRate(rec.Outcomes); skip zeros.
	//  2. Use the mean Runtime across its outcomes as the per-run cost.
	//  3. WastedPerDay = flakeRate * runsPerDay[id] * (meanRuntime + TriageOverhead).
	//  4. Sort descending by WastedPerDay, tie-break by TestID ascending for determinism.
	return nil
}

// helper kept so the skeleton compiles before you start.
var _ = sort.Strings
