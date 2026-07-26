package qualitygates

import (
	"testing"
	"time"
)

func day(n int) time.Time {
	return time.Date(2026, 1, n, 12, 0, 0, 0, time.UTC)
}

// ---------- 1) Gate evaluation ----------

func TestHigherIsBetterGate(t *testing.T) {
	gates := []Gate{{Name: "coverage", Metric: "coverage", Threshold: 80, Direction: HigherIsBetter, Severity: Blocking}}

	if d := Evaluate(gates, []Metric{{Name: "coverage", Value: 85}}); !d.Results[0].Passed {
		t.Fatal("85 >= 80 should pass")
	}
	if d := Evaluate(gates, []Metric{{Name: "coverage", Value: 80}}); !d.Results[0].Passed {
		t.Fatal("exactly at threshold should pass")
	}
	if d := Evaluate(gates, []Metric{{Name: "coverage", Value: 79}}); d.Results[0].Passed {
		t.Fatal("79 < 80 should fail")
	}
}

func TestLowerIsBetterGate(t *testing.T) {
	gates := []Gate{{Name: "p95", Metric: "p95_ms", Threshold: 250, Direction: LowerIsBetter, Severity: Blocking}}

	if d := Evaluate(gates, []Metric{{Name: "p95_ms", Value: 200}}); !d.Results[0].Passed {
		t.Fatal("200 <= 250 should pass")
	}
	if d := Evaluate(gates, []Metric{{Name: "p95_ms", Value: 300}}); d.Results[0].Passed {
		t.Fatal("300 > 250 should fail")
	}
}

func TestToleranceAbsorbsNoise(t *testing.T) {
	// Shared CI hardware makes perf numbers noisy; a 2% tolerance stops the gate crying wolf.
	gates := []Gate{{
		Name: "p95", Metric: "p95_ms", Threshold: 100,
		Direction: LowerIsBetter, Severity: Blocking, Tolerance: 0.02,
	}}

	if d := Evaluate(gates, []Metric{{Name: "p95_ms", Value: 101}}); !d.Results[0].Passed {
		t.Fatal("101 is within the 2% tolerance of 100, should pass")
	}
	if d := Evaluate(gates, []Metric{{Name: "p95_ms", Value: 105}}); d.Results[0].Passed {
		t.Fatal("105 exceeds the 2% tolerance, should fail")
	}
}

func TestMissingMetricFailsTheGate(t *testing.T) {
	gates := []Gate{{Name: "coverage", Metric: "coverage", Threshold: 80, Direction: HigherIsBetter, Severity: Blocking}}
	d := Evaluate(gates, []Metric{{Name: "something_else", Value: 99}})

	if d.Results[0].Passed {
		t.Fatal("a missing metric must FAIL — a broken coverage upload must not read as 'coverage is fine'")
	}
	if !d.Results[0].Missing {
		t.Fatal("the result must be flagged Missing so the failure is diagnosable")
	}
	if !d.Blocked {
		t.Fatal("a failed blocking gate must block")
	}
}

func TestAdvisoryGateDoesNotBlock(t *testing.T) {
	gates := []Gate{{Name: "docs", Metric: "docs", Threshold: 50, Direction: HigherIsBetter, Severity: Advisory}}
	d := Evaluate(gates, []Metric{{Name: "docs", Value: 10}})

	if d.Results[0].Passed {
		t.Fatal("10 < 50, the gate itself failed")
	}
	if d.Blocked {
		t.Fatal("an advisory gate must never block the pipeline")
	}
}

func TestMandatoryGateIsNotOverridable(t *testing.T) {
	gates := []Gate{
		{Name: "coverage", Metric: "coverage", Threshold: 80, Direction: HigherIsBetter, Severity: Blocking},
		{Name: "critical-cves", Metric: "cves", Threshold: 0, Direction: LowerIsBetter, Severity: Mandatory},
	}

	// Only the blocking gate fails -> a human can override.
	d := Evaluate(gates, []Metric{{Name: "coverage", Value: 10}, {Name: "cves", Value: 0}})
	if !d.Blocked || !d.Overridable {
		t.Fatalf("a failed Blocking gate should block but stay overridable; blocked=%v overridable=%v",
			d.Blocked, d.Overridable)
	}

	// The mandatory gate fails -> no override.
	d = Evaluate(gates, []Metric{{Name: "coverage", Value: 99}, {Name: "cves", Value: 3}})
	if !d.Blocked {
		t.Fatal("a failed Mandatory gate must block")
	}
	if d.Overridable {
		t.Fatal("a Mandatory gate must NOT be overridable — that is the whole point of the tier")
	}
}

func TestAllPassingIsNotBlocked(t *testing.T) {
	gates := []Gate{
		{Name: "coverage", Metric: "coverage", Threshold: 80, Direction: HigherIsBetter, Severity: Blocking},
		{Name: "cves", Metric: "cves", Threshold: 0, Direction: LowerIsBetter, Severity: Mandatory},
	}
	d := Evaluate(gates, []Metric{{Name: "coverage", Value: 91}, {Name: "cves", Value: 0}})

	if d.Blocked {
		t.Fatal("everything passed, nothing should block")
	}
	if d.Overridable {
		t.Fatal("nothing failed, so there is nothing to override")
	}
}

func TestResultsPreserveGateOrder(t *testing.T) {
	gates := []Gate{
		{Name: "a", Metric: "a", Threshold: 1, Direction: HigherIsBetter},
		{Name: "b", Metric: "b", Threshold: 1, Direction: HigherIsBetter},
		{Name: "c", Metric: "c", Threshold: 1, Direction: HigherIsBetter},
	}
	d := Evaluate(gates, nil)
	for i, want := range []string{"a", "b", "c"} {
		if d.Results[i].Gate.Name != want {
			t.Fatalf("results must follow declaration order; position %d want %s got %s",
				i, want, d.Results[i].Gate.Name)
		}
	}
}

// ---------- 2) DORA ----------

func TestComputeDORAEmpty(t *testing.T) {
	if got := ComputeDORA(nil); got.DeploymentFrequency != 0 || got.ChangeFailureRate != 0 {
		t.Fatalf("want zero value for no deployments, got %+v", got)
	}
}

func TestDeploymentFrequency(t *testing.T) {
	// 10 deployments spanning 10 days => 1/day.
	var ds []Deployment
	for i := 1; i <= 10; i++ {
		ds = append(ds, Deployment{ID: string(rune('a' + i)), At: day(i), CommittedAt: day(i).Add(-time.Hour)})
	}
	got := ComputeDORA(ds)
	if got.DeploymentFrequency < 0.9 || got.DeploymentFrequency > 1.2 {
		t.Fatalf("10 deploys over ~10 days should be ~1/day, got %v", got.DeploymentFrequency)
	}
}

func TestDeploymentFrequencySubDayWindow(t *testing.T) {
	// Three deploys in one afternoon. Without a 1-day floor this reports an absurd rate.
	ds := []Deployment{
		{ID: "a", At: day(1), CommittedAt: day(1)},
		{ID: "b", At: day(1).Add(time.Hour), CommittedAt: day(1)},
		{ID: "c", At: day(1).Add(2 * time.Hour), CommittedAt: day(1)},
	}
	if got := ComputeDORA(ds).DeploymentFrequency; got != 3 {
		t.Fatalf("a sub-day window must be floored at 1 day, want 3/day, got %v", got)
	}
}

func TestLeadTimeUsesMedianNotMean(t *testing.T) {
	// Four fast commits and one ancient one. The mean would be dragged into uselessness.
	ds := []Deployment{
		{ID: "a", At: day(10), CommittedAt: day(10).Add(-1 * time.Hour)},
		{ID: "b", At: day(10), CommittedAt: day(10).Add(-2 * time.Hour)},
		{ID: "c", At: day(10), CommittedAt: day(10).Add(-3 * time.Hour)},
		{ID: "d", At: day(10), CommittedAt: day(10).Add(-4 * time.Hour)},
		{ID: "old", At: day(10), CommittedAt: day(10).Add(-2000 * time.Hour)},
	}
	got := ComputeDORA(ds).LeadTimeP50
	if got > 5*time.Hour {
		t.Fatalf("median must resist the 2000h outlier; want <= 5h, got %v", got)
	}
}

func TestChangeFailureRate(t *testing.T) {
	ds := []Deployment{
		{ID: "a", At: day(1), CommittedAt: day(1)},
		{ID: "b", At: day(2), CommittedAt: day(2), Failed: true, RestoredAt: day(2).Add(time.Hour)},
		{ID: "c", At: day(3), CommittedAt: day(3)},
		{ID: "d", At: day(4), CommittedAt: day(4)},
	}
	if got := ComputeDORA(ds).ChangeFailureRate; got != 0.25 {
		t.Fatalf("1 failure in 4 deploys = 0.25, got %v", got)
	}
}

func TestTimeToRestore(t *testing.T) {
	ds := []Deployment{
		{ID: "ok", At: day(1), CommittedAt: day(1)},
		{ID: "f1", At: day(2), CommittedAt: day(2), Failed: true, RestoredAt: day(2).Add(30 * time.Minute)},
		{ID: "f2", At: day(3), CommittedAt: day(3), Failed: true, RestoredAt: day(3).Add(90 * time.Minute)},
		{ID: "f3", At: day(4), CommittedAt: day(4), Failed: true, RestoredAt: day(4).Add(60 * time.Minute)},
	}
	if got := ComputeDORA(ds).TimeToRestoreP50; got != 60*time.Minute {
		t.Fatalf("median of 30/60/90 minutes is 60m, got %v", got)
	}
}

func TestTimeToRestoreNoFailures(t *testing.T) {
	ds := []Deployment{{ID: "a", At: day(1), CommittedAt: day(1)}}
	if got := ComputeDORA(ds).TimeToRestoreP50; got != 0 {
		t.Fatalf("no failures means no restore time, want 0, got %v", got)
	}
}

func TestMedianDurationEvenCountTakesLowerMiddle(t *testing.T) {
	got := medianDuration([]time.Duration{time.Hour, 2 * time.Hour, 3 * time.Hour, 4 * time.Hour})
	if got != 2*time.Hour {
		t.Fatalf("even count should return the lower middle (2h), got %v", got)
	}
}

func TestMedianDurationEmpty(t *testing.T) {
	if got := medianDuration(nil); got != 0 {
		t.Fatalf("want 0 for empty, got %v", got)
	}
}

// ---------- 3) Ranking ----------

func TestRankDeploymentFrequency(t *testing.T) {
	cases := []struct {
		perDay float64
		want   Rank
	}{
		{5, Elite}, {1, Elite}, {0.5, High}, {1.0 / 7, High}, {0.1, Medium}, {1.0 / 30, Medium}, {0.01, Low},
	}
	for _, c := range cases {
		if got := RankDeploymentFrequency(c.perDay); got != c.want {
			t.Fatalf("%v/day: want %v, got %v", c.perDay, c.want, got)
		}
	}
}

func TestRankLeadTime(t *testing.T) {
	cases := []struct {
		d    time.Duration
		want Rank
	}{
		{2 * time.Hour, Elite},
		{3 * 24 * time.Hour, High},
		{20 * 24 * time.Hour, Medium},
		{90 * 24 * time.Hour, Low},
	}
	for _, c := range cases {
		if got := RankLeadTime(c.d); got != c.want {
			t.Fatalf("%v: want %v, got %v", c.d, c.want, got)
		}
	}
}

func TestRankChangeFailureRate(t *testing.T) {
	cases := []struct {
		rate float64
		want Rank
	}{
		{0.02, Elite}, {0.05, Elite}, {0.08, High}, {0.13, Medium}, {0.40, Low},
	}
	for _, c := range cases {
		if got := RankChangeFailureRate(c.rate); got != c.want {
			t.Fatalf("%v: want %v, got %v", c.rate, c.want, got)
		}
	}
}
