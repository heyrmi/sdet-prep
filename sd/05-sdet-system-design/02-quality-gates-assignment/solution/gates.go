// Package qualitygates is the reference solution for Module 5.2.
//
// Points worth defending in an interview:
//
//   - The three-tier severity model exists because a pipeline where everything blocks is a
//     pipeline everyone learns to route around. New gates start Advisory, earn Blocking once
//     their false-positive rate is known, and only security/compliance rules are Mandatory.
//
//   - A missing metric FAILS. The most dangerous gate is one that silently passes because the
//     coverage upload broke — you get the reassurance without the check.
//
//   - Tolerance exists for genuinely noisy metrics (perf on shared runners). Without it the
//     gate cries wolf, and a gate nobody believes is worse than no gate.
//
//   - Lead time and restore time use the MEDIAN. One six-month-old commit landing today would
//     destroy a mean, and the resulting number would describe nothing real.
package qualitygates

import (
	"sort"
	"time"
)

// Severity decides what a failing gate does to the pipeline.
type Severity int

const (
	Advisory Severity = iota
	Blocking
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
	HigherIsBetter Direction = iota
	LowerIsBetter
)

// Gate is one rule evaluated against a metric.
type Gate struct {
	Name      string
	Metric    string
	Threshold float64
	Direction Direction
	Severity  Severity
	Tolerance float64
}

// GateResult is the outcome of evaluating one gate.
type GateResult struct {
	Gate    Gate
	Actual  float64
	Passed  bool
	Missing bool
}

// Decision is the pipeline's overall verdict.
type Decision struct {
	Results     []GateResult
	Blocked     bool
	Overridable bool
}

// Evaluate runs every gate against the reported metrics.
func Evaluate(gates []Gate, metrics []Metric) Decision {
	byName := make(map[string]float64, len(metrics))
	for _, m := range metrics {
		byName[m.Name] = m.Value
	}

	d := Decision{Results: make([]GateResult, 0, len(gates))}
	anyFailed, mandatoryFailed := false, false

	for _, g := range gates {
		actual, ok := byName[g.Metric]
		res := GateResult{Gate: g, Actual: actual}

		if !ok {
			// Absence of evidence is not evidence of quality.
			res.Missing = true
			res.Passed = false
		} else if g.Direction == HigherIsBetter {
			res.Passed = actual >= g.Threshold*(1-g.Tolerance)
		} else {
			res.Passed = actual <= g.Threshold*(1+g.Tolerance)
		}

		if !res.Passed {
			switch g.Severity {
			case Blocking:
				anyFailed = true
				d.Blocked = true
			case Mandatory:
				anyFailed = true
				mandatoryFailed = true
				d.Blocked = true
			case Advisory:
				// Reported, never blocking.
			}
		}
		d.Results = append(d.Results, res)
	}

	// Overridable only when a human *could* legitimately clear it: something blocked, but
	// nothing mandatory was involved.
	d.Overridable = anyFailed && d.Blocked && !mandatoryFailed
	return d
}

// Deployment is one release to production.
type Deployment struct {
	ID          string
	At          time.Time
	CommittedAt time.Time
	Failed      bool
	RestoredAt  time.Time
}

// DORA holds the four key delivery metrics.
type DORA struct {
	DeploymentFrequency float64
	LeadTimeP50         time.Duration
	ChangeFailureRate   float64
	TimeToRestoreP50    time.Duration
}

// ComputeDORA derives the four metrics from a deployment history.
func ComputeDORA(deployments []Deployment) DORA {
	if len(deployments) == 0 {
		return DORA{}
	}

	earliest, latest := deployments[0].At, deployments[0].At
	leadTimes := make([]time.Duration, 0, len(deployments))
	var restores []time.Duration
	failed := 0

	for _, d := range deployments {
		if d.At.Before(earliest) {
			earliest = d.At
		}
		if d.At.After(latest) {
			latest = d.At
		}
		leadTimes = append(leadTimes, d.At.Sub(d.CommittedAt))
		if d.Failed {
			failed++
			restores = append(restores, d.RestoredAt.Sub(d.At))
		}
	}

	// Floor the window at one day: three deploys in an afternoon is 3/day, not 72/day.
	windowDays := latest.Sub(earliest).Hours() / 24
	if windowDays < 1 {
		windowDays = 1
	}

	return DORA{
		DeploymentFrequency: float64(len(deployments)) / windowDays,
		LeadTimeP50:         medianDuration(leadTimes),
		ChangeFailureRate:   float64(failed) / float64(len(deployments)),
		TimeToRestoreP50:    medianDuration(restores),
	}
}

// medianDuration returns the median of a duration slice, 0 for empty.
func medianDuration(ds []time.Duration) time.Duration {
	if len(ds) == 0 {
		return 0
	}
	sorted := make([]time.Duration, len(ds))
	copy(sorted, ds)
	sort.Slice(sorted, func(i, j int) bool { return sorted[i] < sorted[j] })

	// Lower-middle for even counts: the result stays an observed value, not a synthetic one.
	return sorted[(len(sorted)-1)/2]
}

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

// RankDeploymentFrequency bands deployments per day.
func RankDeploymentFrequency(perDay float64) Rank {
	switch {
	case perDay >= 1:
		return Elite
	case perDay >= 1.0/7:
		return High
	case perDay >= 1.0/30:
		return Medium
	default:
		return Low
	}
}

// RankLeadTime bands commit-to-production duration.
func RankLeadTime(d time.Duration) Rank {
	switch {
	case d < 24*time.Hour:
		return Elite
	case d < 7*24*time.Hour:
		return High
	case d < 30*24*time.Hour:
		return Medium
	default:
		return Low
	}
}

// RankChangeFailureRate bands the failure rate.
func RankChangeFailureRate(rate float64) Rank {
	switch {
	case rate <= 0.05:
		return Elite
	case rate <= 0.10:
		return High
	case rate <= 0.15:
		return Medium
	default:
		return Low
	}
}
