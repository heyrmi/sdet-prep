// Package qualitygates is the Module 5.2 assignment: the gate engine and delivery metrics
// behind a CI/CD pipeline.
//
// Read 05-sdet-system-design/02-design-a-ci-cd-pipeline.md first.
//
// Fill in every function marked `// TODO`. Run the tests until green:
//
//	go test ./...
//
// The framing: "Design the pipeline for 200 engineers merging 40 times a day." The interesting
// half is not the YAML — it is the POLICY. What blocks a merge, what only warns, who can
// override, and how you prove the pipeline is helping rather than just being expensive.
//
// The metric definitions here are DORA (DevOps Research and Assessment): deployment frequency,
// lead time for change, change failure rate, and time to restore. Being able to compute and
// interpret these is what separates "I run tests" from "I own delivery quality".
package qualitygates

import (
	"sort"
	"time"
)

// Severity decides what a failing gate does to the pipeline.
type Severity int

const (
	// Advisory: report it, never block. Where every new gate should start — a gate that blocks
	// on day one and turns out to be noisy trains everyone to use the override.
	Advisory Severity = iota
	// Blocking: fails the pipeline. Overridable by a human with a recorded reason.
	Blocking
	// Mandatory: fails the pipeline and CANNOT be overridden. Reserve for security and
	// compliance — if everything is mandatory, the pipeline becomes something to route around.
	Mandatory
)

func (s Severity) String() string {
	switch s {
	case Advisory:
		return "advisory"
	case Blocking:
		return "blocking"
	case Mandatory:
		return "mandatory"
	}
	return "unknown"
}

// Metric is one measurement produced by a pipeline stage.
type Metric struct {
	Name  string
	Value float64
}

// Direction says which way is "good" for a threshold.
type Direction int

const (
	// HigherIsBetter: coverage, pass rate.
	HigherIsBetter Direction = iota
	// LowerIsBetter: p95 latency, flake rate, bundle size.
	LowerIsBetter
)

// Gate is one rule evaluated against a metric.
type Gate struct {
	Name      string
	Metric    string
	Threshold float64
	Direction Direction
	Severity  Severity
	// Tolerance allows a small regression without failing, for metrics that are inherently
	// noisy (performance benchmarks on shared CI hardware). Expressed as a fraction, e.g.
	// 0.02 permits a 2% move in the bad direction before the gate trips.
	Tolerance float64
}

// GateResult is the outcome of evaluating one gate.
type GateResult struct {
	Gate     Gate
	Actual   float64
	Passed   bool
	// Missing is true when the metric was never reported. A gate whose metric is absent must
	// NOT silently pass — that is how a broken coverage upload turns into "coverage is fine".
	Missing bool
}

// Decision is the pipeline's overall verdict.
type Decision struct {
	Results []GateResult
	// Blocked is true if any Blocking or Mandatory gate failed.
	Blocked bool
	// Overridable is true if the block could be cleared by a human — i.e. at least one gate
	// failed, but no MANDATORY gate did.
	Overridable bool
}

// ----------------------------------------------------------------------------
// 1) Gate evaluation
//
// Rules:
//   - HigherIsBetter passes when actual >= threshold*(1-Tolerance).
//   - LowerIsBetter  passes when actual <= threshold*(1+Tolerance).
//   - A missing metric FAILS the gate and is marked Missing. Absence of evidence is not
//     evidence of quality.
//   - Results come back in the order the gates were declared, so pipeline output is stable.
// ----------------------------------------------------------------------------

// Evaluate runs every gate against the reported metrics.
func Evaluate(gates []Gate, metrics []Metric) Decision {
	// TODO:
	//  1. Index metrics by name.
	//  2. For each gate in order, build a GateResult:
	//     - metric absent  -> Passed=false, Missing=true
	//     - HigherIsBetter -> Passed = actual >= Threshold*(1-Tolerance)
	//     - LowerIsBetter  -> Passed = actual <= Threshold*(1+Tolerance)
	//  3. Blocked     = any failed result whose Severity is Blocking or Mandatory.
	//  4. Overridable = Blocked AND no failed result has Severity Mandatory.
	//  5. Return the Decision.
	return Decision{}
}

// ----------------------------------------------------------------------------
// 2) DORA metrics
//
// Four numbers that describe delivery performance. Know them cold — they are the vocabulary
// for arguing that test investment is worth funding.
// ----------------------------------------------------------------------------

// Deployment is one release to production.
type Deployment struct {
	ID       string
	At       time.Time
	// CommittedAt is when the code was committed. Lead time is At - CommittedAt.
	CommittedAt time.Time
	// Failed marks a deployment that caused an incident, rollback, or hotfix.
	Failed bool
	// RestoredAt is when service was restored, for failed deployments only.
	RestoredAt time.Time
}

// DORA holds the four key delivery metrics.
type DORA struct {
	// DeploymentFrequency is deployments per day over the observed window.
	DeploymentFrequency float64
	// LeadTimeP50 is the median commit-to-production duration. Median, not mean — one
	// six-month-old commit landing today would wreck a mean and tell you nothing.
	LeadTimeP50 time.Duration
	// ChangeFailureRate is failed deployments / total deployments, 0.0–1.0.
	ChangeFailureRate float64
	// TimeToRestoreP50 is the median RestoredAt - At across failed deployments.
	TimeToRestoreP50 time.Duration
}

// ComputeDORA derives the four metrics from a deployment history.
// The window is the observed span from earliest to latest deployment; if that span is under
// one day, treat it as one day so frequency stays meaningful rather than exploding.
// Returns a zero DORA for no deployments.
func ComputeDORA(deployments []Deployment) DORA {
	// TODO:
	//  1. Empty -> zero value.
	//  2. Find earliest and latest At. windowDays = max(1, span in days).
	//  3. DeploymentFrequency = len(deployments) / windowDays.
	//  4. LeadTimeP50 = median of (At - CommittedAt).
	//  5. ChangeFailureRate = failed / total.
	//  6. TimeToRestoreP50 = median of (RestoredAt - At) over failed deployments only
	//     (zero if none failed).
	return DORA{}
}

// medianDuration returns the median of a duration slice, 0 for empty.
// For an even count, return the LOWER of the two middle values — keeps the result an
// observed value rather than a synthetic average.
func medianDuration(ds []time.Duration) time.Duration {
	// TODO: sort a copy, return the middle element (lower-middle for even lengths).
	return 0
}

// ----------------------------------------------------------------------------
// 3) Performance rank
//
// DORA's published bands. Interviewers ask "is that good?" — this is the answer.
// ----------------------------------------------------------------------------

// Rank is a DORA performance band.
type Rank int

const (
	Low Rank = iota
	Medium
	High
	Elite
)

func (r Rank) String() string {
	switch r {
	case Low:
		return "low"
	case Medium:
		return "medium"
	case High:
		return "high"
	case Elite:
		return "elite"
	}
	return "unknown"
}

// RankDeploymentFrequency bands deployments per day:
//
//	Elite  >= 1/day
//	High   >= 1/week   (>= 1/7 per day)
//	Medium >= 1/month  (>= 1/30 per day)
//	Low    below that
func RankDeploymentFrequency(perDay float64) Rank {
	// TODO
	return Low
}

// RankLeadTime bands commit-to-production:
//
//	Elite  < 1 day
//	High   < 1 week
//	Medium < 1 month (30 days)
//	Low    beyond that
func RankLeadTime(d time.Duration) Rank {
	// TODO
	return Low
}

// RankChangeFailureRate bands the failure rate:
//
//	Elite  <= 0.05
//	High   <= 0.10
//	Medium <= 0.15
//	Low    above that
func RankChangeFailureRate(rate float64) Rank {
	// TODO
	return Low
}

var _ = sort.Slice
