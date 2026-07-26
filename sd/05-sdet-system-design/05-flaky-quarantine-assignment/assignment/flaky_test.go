package flakyquarantine

import (
	"fmt"
	"sync"
	"testing"
	"time"
)

func base() time.Time { return time.Date(2026, 1, 1, 0, 0, 0, 0, time.UTC) }

// out builds an outcome with a fixed runtime; helpers keep the tests readable.
func out(testID, commit string, passed bool) Outcome {
	return Outcome{TestID: testID, Commit: commit, Passed: passed, At: base(), Runtime: time.Second}
}

// ---------- 1) Flake rate ----------

func TestFlakeRateIgnoresSingleRunCommits(t *testing.T) {
	// Every commit ran once. There is no disagreement to observe, so there is no flake signal.
	outcomes := []Outcome{
		out("t", "c1", true),
		out("t", "c2", false),
		out("t", "c3", true),
	}
	if got := FlakeRate(outcomes); got != 0 {
		t.Fatalf("single-run commits carry no flake signal, want 0.0, got %v", got)
	}
}

func TestFlakeRateConsistentFailureIsNotFlaky(t *testing.T) {
	// Failing the same way on every run of a commit means the test is BROKEN, not flaky.
	// Quarantining this would hide a real bug.
	outcomes := []Outcome{
		out("t", "c1", false),
		out("t", "c1", false),
		out("t", "c1", false),
	}
	if got := FlakeRate(outcomes); got != 0 {
		t.Fatalf("consistent failure is broken, not flaky, want 0.0, got %v", got)
	}
}

func TestFlakeRateDetectsSameCommitDisagreement(t *testing.T) {
	// c1 disagreed with itself, c2 did not. One of two multi-run commits => 0.5.
	outcomes := []Outcome{
		out("t", "c1", true),
		out("t", "c1", false),
		out("t", "c2", true),
		out("t", "c2", true),
	}
	if got := FlakeRate(outcomes); got != 0.5 {
		t.Fatalf("want 0.5 (1 of 2 multi-run commits disagreed), got %v", got)
	}
}

func TestFlakeRateEmpty(t *testing.T) {
	if got := FlakeRate(nil); got != 0 {
		t.Fatalf("want 0.0 for no outcomes, got %v", got)
	}
}

// ---------- 2) State machine ----------

func TestHealthyTestBlocksMerges(t *testing.T) {
	r := NewRegistry(DefaultPolicy())
	if !r.IsBlocking("never-seen") {
		t.Fatal("an unknown test must block by default — we have no evidence it is flaky")
	}
	r.Record(out("stable", "c1", true))
	if !r.IsBlocking("stable") {
		t.Fatal("a healthy test must block merges when it fails")
	}
}

func TestQuarantineRequiresMinRuns(t *testing.T) {
	p := DefaultPolicy()
	p.MinRuns = 10
	r := NewRegistry(p)

	// Two runs of one commit that disagree => flake rate 1.0, but only 2 runs of history.
	r.Record(out("new", "c1", true))
	st := r.Record(out("new", "c1", false))
	if st == Quarantined {
		t.Fatalf("must not quarantine on %d runs when MinRuns is %d", 2, p.MinRuns)
	}
}

func TestQuarantineOnSustainedFlaking(t *testing.T) {
	p := DefaultPolicy()
	p.MinRuns = 4
	p.FlakeRateThreshold = 0.5
	r := NewRegistry(p)

	// Three commits, two of which disagree with themselves => 2/3 = 0.67 >= 0.5.
	r.Record(out("f", "c1", true))
	r.Record(out("f", "c1", false))
	r.Record(out("f", "c2", true))
	r.Record(out("f", "c2", true))
	r.Record(out("f", "c3", true))
	st := r.Record(out("f", "c3", false))

	if st != Quarantined {
		t.Fatalf("want Quarantined after sustained flaking, got %v", st)
	}
	if r.IsBlocking("f") {
		t.Fatal("a quarantined test must not block merges")
	}
}

func TestQuarantineToProbationToHealthy(t *testing.T) {
	p := DefaultPolicy()
	p.MinRuns = 2
	p.FlakeRateThreshold = 0.5
	p.ConsecutivePassesToRelease = 3
	p.ProbationRuns = 2
	r := NewRegistry(p)

	// Get it quarantined.
	r.Record(out("f", "c1", true))
	if st := r.Record(out("f", "c1", false)); st != Quarantined {
		t.Fatalf("setup: want Quarantined, got %v", st)
	}

	// 3 clean runs -> Probation.
	for i := 0; i < 2; i++ {
		if st := r.Record(out("f", fmt.Sprintf("p%d", i), true)); st != Quarantined {
			t.Fatalf("run %d: should still be Quarantined, got %v", i, st)
		}
	}
	if st := r.Record(out("f", "p2", true)); st != Probation {
		t.Fatalf("after %d consecutive passes want Probation, got %v", p.ConsecutivePassesToRelease, st)
	}
	if r.IsBlocking("f") {
		t.Fatal("a test on probation must not block merges yet")
	}

	// 2 more clean runs -> Healthy.
	if st := r.Record(out("f", "q0", true)); st != Probation {
		t.Fatalf("mid-probation should still be Probation, got %v", st)
	}
	if st := r.Record(out("f", "q1", true)); st != Healthy {
		t.Fatalf("after %d probation runs want Healthy, got %v", p.ProbationRuns, st)
	}
	if !r.IsBlocking("f") {
		t.Fatal("a released test must block merges again")
	}
}

func TestFailureDuringProbationReturnsToQuarantine(t *testing.T) {
	p := DefaultPolicy()
	p.MinRuns = 2
	p.FlakeRateThreshold = 0.5
	p.ConsecutivePassesToRelease = 2
	p.ProbationRuns = 5
	r := NewRegistry(p)

	r.Record(out("f", "c1", true))
	r.Record(out("f", "c1", false)) // quarantined
	r.Record(out("f", "p0", true))
	if st := r.Record(out("f", "p1", true)); st != Probation {
		t.Fatalf("setup: want Probation, got %v", st)
	}

	if st := r.Record(out("f", "p2", false)); st != Quarantined {
		t.Fatalf("a failure during probation must return the test to Quarantined, got %v", st)
	}
}

func TestQuarantinedTestIsNotRequarantined(t *testing.T) {
	// Regression guard: re-applying the flake-rate rule to an already-quarantined test would
	// reset its clock every run and pin it in quarantine forever.
	p := DefaultPolicy()
	p.MinRuns = 2
	p.FlakeRateThreshold = 0.1
	p.ConsecutivePassesToRelease = 2
	r := NewRegistry(p)

	r.Record(out("f", "c1", true))
	r.Record(out("f", "c1", false)) // quarantined; history still shows a high flake rate

	r.Record(out("f", "p0", true))
	if st := r.Record(out("f", "p1", true)); st != Probation {
		t.Fatalf("stale history must not keep re-quarantining; want Probation, got %v", st)
	}
}

// ---------- 3) Dashboard reads ----------

func TestQuarantinedTestsSorted(t *testing.T) {
	p := DefaultPolicy()
	p.MinRuns = 2
	p.FlakeRateThreshold = 0.5
	r := NewRegistry(p)

	for _, id := range []string{"zebra", "alpha", "mango"} {
		r.Record(out(id, "c1", true))
		r.Record(out(id, "c1", false))
	}
	got := r.QuarantinedTests()
	want := []string{"alpha", "mango", "zebra"}
	if len(got) != len(want) {
		t.Fatalf("want %d quarantined, got %d (%v)", len(want), len(got), got)
	}
	for i := range want {
		if got[i] != want[i] {
			t.Fatalf("want sorted %v, got %v", want, got)
		}
	}
}

func TestExpiredQuarantines(t *testing.T) {
	p := DefaultPolicy()
	p.MinRuns = 2
	p.FlakeRateThreshold = 0.5
	p.MaxQuarantineDuration = 7 * 24 * time.Hour
	r := NewRegistry(p)

	clock := base()
	r.now = func() time.Time { return clock }

	r.Record(out("old", "c1", true))
	r.Record(out("old", "c1", false)) // quarantined at base()

	if len(r.ExpiredQuarantines()) != 0 {
		t.Fatal("nothing should be expired immediately after quarantine")
	}

	clock = base().Add(8 * 24 * time.Hour)
	got := r.ExpiredQuarantines()
	if len(got) != 1 || got[0] != "old" {
		t.Fatalf("want [old] expired after 8 days with a 7-day cap, got %v", got)
	}
}

// ---------- 4) Impact ranking ----------

func TestRankByImpactPrefersFrequentlyRunTests(t *testing.T) {
	p := DefaultPolicy()
	p.MinRuns = 1000 // keep everything Healthy; we are testing ranking, not quarantine
	r := NewRegistry(p)

	// "rare" flakes far more often, but "hot" runs 100x more per day.
	for i := 0; i < 2; i++ {
		c := fmt.Sprintf("c%d", i)
		r.Record(out("rare", c, true))
		r.Record(out("rare", c, false)) // flake rate 1.0
	}
	r.Record(out("hot", "h0", true))
	r.Record(out("hot", "h0", false)) // 1 of 2 multi-run commits
	r.Record(out("hot", "h1", true))
	r.Record(out("hot", "h1", true)) // => 0.5

	ranked := r.RankByImpact(map[string]float64{"rare": 1, "hot": 200})
	if len(ranked) != 2 {
		t.Fatalf("want 2 ranked tests, got %d (%v)", len(ranked), ranked)
	}
	if ranked[0].TestID != "hot" {
		t.Fatalf("a 0.5-flake test running 200x/day costs more than a 1.0-flake test running 1x/day; "+
			"want hot first, got %v", ranked[0].TestID)
	}
	if ranked[0].WastedPerDay <= ranked[1].WastedPerDay {
		t.Fatal("ranking must be descending by WastedPerDay")
	}
}

func TestRankByImpactSkipsNonFlakyTests(t *testing.T) {
	r := NewRegistry(DefaultPolicy())
	r.Record(out("clean", "c1", true))
	r.Record(out("clean", "c1", true))

	if got := r.RankByImpact(map[string]float64{"clean": 100}); len(got) != 0 {
		t.Fatalf("a test with a 0 flake rate has no impact to rank, got %v", got)
	}
}

// ---------- Concurrency ----------

func TestRegistryIsRaceFree(t *testing.T) {
	// Run with -race. Many CI workers report outcomes while the dashboard reads.
	r := NewRegistry(DefaultPolicy())
	var wg sync.WaitGroup

	for w := 0; w < 8; w++ {
		wg.Add(1)
		go func(w int) {
			defer wg.Done()
			for i := 0; i < 100; i++ {
				r.Record(out(fmt.Sprintf("t%d", i%10), fmt.Sprintf("c%d", i), i%3 != 0))
			}
		}(w)
	}
	for reader := 0; reader < 4; reader++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for i := 0; i < 100; i++ {
				_ = r.QuarantinedTests()
				_ = r.IsBlocking("t1")
				_ = r.ExpiredQuarantines()
			}
		}()
	}
	wg.Wait()
}

func TestWindowIsBounded(t *testing.T) {
	p := DefaultPolicy()
	p.Window = 10
	p.MinRuns = 1000
	r := NewRegistry(p)

	for i := 0; i < 50; i++ {
		r.Record(out("t", fmt.Sprintf("c%d", i), true))
	}
	r.mu.RLock()
	n := len(r.records["t"].Outcomes)
	r.mu.RUnlock()

	if n != p.Window {
		t.Fatalf("history must be capped at Window=%d, got %d — an unbounded slice is a memory leak "+
			"at 10k tests x 40 merges/day", p.Window, n)
	}
}
