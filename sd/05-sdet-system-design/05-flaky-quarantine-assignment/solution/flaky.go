// Package flakyquarantine is the reference solution for Module 5.5.
//
// Design notes worth being able to defend in an interview:
//
//   - Flakiness is defined as same-commit disagreement, not "sometimes fails". A test that
//     always fails on a commit is broken; quarantining it would hide a real regression. This
//     single definition choice is the difference between a system that builds trust and one
//     that silently suppresses bugs.
//
//   - Quarantined tests still RUN. Removing them from the suite destroys the very signal you
//     need to know when they are fixed. What quarantine removes is the power to block a merge.
//
//   - Release is two-phase (Quarantined -> Probation -> Healthy). A single lucky streak is not
//     evidence of a fix, and going straight back to blocking re-breaks the gate.
//
//   - Quarantine expires. Without MaxQuarantineDuration, quarantine becomes the graveyard where
//     tests go to be forgotten, and coverage silently rots.
//
//   - Ranking is by COST, not flake rate. A 5%-flaky test on the critical path burns far more
//     engineer-hours than a 50%-flaky test that runs weekly.
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
	Commit  string
	At      time.Time
	Runtime time.Duration
}

// State is where a test sits in its quarantine lifecycle.
type State int

const (
	Healthy State = iota
	Quarantined
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
	Window                     int
	FlakeRateThreshold         float64
	MinRuns                    int
	ConsecutivePassesToRelease int
	ProbationRuns              int
	MaxQuarantineDuration      time.Duration
}

// DefaultPolicy is a reasonable starting point for a large suite.
func DefaultPolicy() Policy {
	return Policy{
		Window:                     50,
		FlakeRateThreshold:         0.05,
		MinRuns:                    10,
		ConsecutivePassesToRelease: 20,
		ProbationRuns:              10,
		MaxQuarantineDuration:      14 * 24 * time.Hour,
	}
}

// TestRecord is the tracked history and current state of a single test.
type TestRecord struct {
	TestID            string
	State             State
	Outcomes          []Outcome
	QuarantinedAt     time.Time
	consecutivePasses int
	probationPasses   int
}

// Registry is the concurrent store of every test's history.
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

// FlakeRate returns the share of multi-run commits that disagreed with themselves.
func FlakeRate(outcomes []Outcome) float64 {
	type tally struct{ passes, fails int }
	byCommit := make(map[string]*tally)

	for _, o := range outcomes {
		t, ok := byCommit[o.Commit]
		if !ok {
			t = &tally{}
			byCommit[o.Commit] = t
		}
		if o.Passed {
			t.passes++
		} else {
			t.fails++
		}
	}

	multiRun, disagreeing := 0, 0
	for _, t := range byCommit {
		if t.passes+t.fails < 2 {
			continue // a single run cannot disagree with itself — no signal, so no denominator
		}
		multiRun++
		if t.passes > 0 && t.fails > 0 {
			disagreeing++
		}
	}

	if multiRun == 0 {
		return 0
	}
	return float64(disagreeing) / float64(multiRun)
}

// Record appends an outcome and applies the policy, returning the record's state afterwards.
func (r *Registry) Record(o Outcome) State {
	r.mu.Lock()
	defer r.mu.Unlock()

	rec, ok := r.records[o.TestID]
	if !ok {
		rec = &TestRecord{TestID: o.TestID, State: Healthy}
		r.records[o.TestID] = rec
	}

	rec.Outcomes = append(rec.Outcomes, o)
	if len(rec.Outcomes) > r.policy.Window {
		// Keep the newest Window entries. Unbounded history is a memory leak at
		// 10k tests x 40 merges/day, and stale outcomes distort the current picture anyway.
		rec.Outcomes = rec.Outcomes[len(rec.Outcomes)-r.policy.Window:]
	}

	if o.Passed {
		rec.consecutivePasses++
	} else {
		rec.consecutivePasses = 0
	}

	r.applyPolicy(rec, o)
	return rec.State
}

// applyPolicy runs the state machine for one record. Called with the write lock held.
func (r *Registry) applyPolicy(rec *TestRecord, last Outcome) {
	switch rec.State {
	case Healthy:
		// Only quarantine with enough evidence. MinRuns stops a brand-new test from being
		// quarantined by one unlucky same-commit disagreement.
		if len(rec.Outcomes) >= r.policy.MinRuns &&
			FlakeRate(rec.Outcomes) >= r.policy.FlakeRateThreshold {
			rec.State = Quarantined
			rec.QuarantinedAt = r.now()
			rec.consecutivePasses = 0
			rec.probationPasses = 0
		}

	case Quarantined:
		// Deliberately NOT re-checking the flake rate here. The history that got the test
		// quarantined is still in the window, so re-applying the rule would reset the clock on
		// every run and pin the test in quarantine forever.
		if rec.consecutivePasses >= r.policy.ConsecutivePassesToRelease {
			rec.State = Probation
			rec.probationPasses = 0
		}

	case Probation:
		if !last.Passed {
			// One failure is enough. Probation exists precisely to catch a premature release.
			rec.State = Quarantined
			rec.QuarantinedAt = r.now()
			rec.consecutivePasses = 0
			rec.probationPasses = 0
			return
		}
		rec.probationPasses++
		if rec.probationPasses >= r.policy.ProbationRuns {
			rec.State = Healthy
			rec.consecutivePasses = 0
			rec.probationPasses = 0
		}
	}
}

// IsBlocking reports whether a failure of this test should fail the build.
func (r *Registry) IsBlocking(testID string) bool {
	r.mu.RLock()
	defer r.mu.RUnlock()

	rec, ok := r.records[testID]
	if !ok {
		return true // unknown test: no evidence of flakiness, so it blocks by default
	}
	return rec.State == Healthy
}

// QuarantinedTests returns the IDs of all quarantined tests, sorted.
func (r *Registry) QuarantinedTests() []string {
	r.mu.RLock()
	defer r.mu.RUnlock()

	var ids []string
	for id, rec := range r.records {
		if rec.State == Quarantined {
			ids = append(ids, id)
		}
	}
	sort.Strings(ids)
	return ids
}

// ExpiredQuarantines returns tests quarantined longer than policy.MaxQuarantineDuration.
func (r *Registry) ExpiredQuarantines() []string {
	r.mu.RLock()
	defer r.mu.RUnlock()

	now := r.now()
	var ids []string
	for id, rec := range r.records {
		if rec.State != Quarantined {
			continue
		}
		if now.Sub(rec.QuarantinedAt) > r.policy.MaxQuarantineDuration {
			ids = append(ids, id)
		}
	}
	sort.Strings(ids)
	return ids
}

// Impact is a test's estimated daily cost from flaking.
type Impact struct {
	TestID       string
	FlakeRate    float64
	RunsPerDay   float64
	WastedPerDay time.Duration
}

// TriageOverhead is the fixed human cost of one flaky failure.
const TriageOverhead = 12 * time.Minute

// RankByImpact returns every tracked test with a non-zero flake rate, most costly first.
func (r *Registry) RankByImpact(runsPerDay map[string]float64) []Impact {
	r.mu.RLock()
	defer r.mu.RUnlock()

	var out []Impact
	for id, rec := range r.records {
		rate := FlakeRate(rec.Outcomes)
		if rate == 0 {
			continue // nothing to rank
		}

		var total time.Duration
		for _, o := range rec.Outcomes {
			total += o.Runtime
		}
		var mean time.Duration
		if len(rec.Outcomes) > 0 {
			mean = total / time.Duration(len(rec.Outcomes))
		}

		freq := runsPerDay[id]
		// The cost of a flake is not just the test runtime — it is the human who has to notice
		// the red build, read the log, decide it is unrelated, and retry.
		wasted := time.Duration(rate * freq * float64(mean+TriageOverhead))

		out = append(out, Impact{
			TestID:       id,
			FlakeRate:    rate,
			RunsPerDay:   freq,
			WastedPerDay: wasted,
		})
	}

	sort.Slice(out, func(i, j int) bool {
		if out[i].WastedPerDay != out[j].WastedPerDay {
			return out[i].WastedPerDay > out[j].WastedPerDay
		}
		return out[i].TestID < out[j].TestID // deterministic tie-break
	})
	return out
}
