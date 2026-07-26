package testplatform

import (
	"fmt"
	"testing"
	"time"
)

func mk(id string, d time.Duration, tags ...string) Test {
	return Test{ID: id, Duration: d, Tags: tags}
}

func sec(n int) time.Duration { return time.Duration(n) * time.Second }

// ---------- 1) Balanced sharding ----------

func TestShardTestsReturnsExactlyWorkerCountShards(t *testing.T) {
	tests := []Test{mk("a", sec(1)), mk("b", sec(2))}
	shards := ShardTests(tests, 5)
	if len(shards) != 5 {
		t.Fatalf("want 5 shards even though only 2 tests exist, got %d", len(shards))
	}
	for i, s := range shards {
		if s.WorkerID != i {
			t.Fatalf("shard %d must carry WorkerID %d, got %d", i, i, s.WorkerID)
		}
	}
}

func TestShardTestsInvalidWorkerCount(t *testing.T) {
	if got := ShardTests([]Test{mk("a", sec(1))}, 0); got != nil {
		t.Fatalf("want nil for workerCount 0, got %v", got)
	}
	if got := ShardTests([]Test{mk("a", sec(1))}, -3); got != nil {
		t.Fatalf("want nil for negative workerCount, got %v", got)
	}
}

func TestShardTestsAssignsEveryTestExactlyOnce(t *testing.T) {
	var tests []Test
	for i := 0; i < 50; i++ {
		tests = append(tests, mk(fmt.Sprintf("t%02d", i), sec(i%7+1)))
	}
	shards := ShardTests(tests, 6)

	seen := map[string]int{}
	for _, s := range shards {
		for _, tc := range s.Tests {
			seen[tc.ID]++
		}
	}
	if len(seen) != len(tests) {
		t.Fatalf("want all %d tests assigned, got %d distinct", len(tests), len(seen))
	}
	for id, n := range seen {
		if n != 1 {
			t.Fatalf("test %s assigned %d times — a test must run exactly once per suite", id, n)
		}
	}
}

func TestShardTestsBalancesWell(t *testing.T) {
	// One 10s test and ten 1s tests across 2 workers. Perfect split is 10s vs 10s.
	tests := []Test{mk("big", sec(10))}
	for i := 0; i < 10; i++ {
		tests = append(tests, mk(fmt.Sprintf("s%d", i), sec(1)))
	}
	shards := ShardTests(tests, 2)

	if cp := CriticalPath(shards); cp > sec(10) {
		t.Fatalf("LPT should reach a 10s/10s split; critical path %v exceeds 10s "+
			"(are you sorting ascending instead of descending?)", cp)
	}
}

func TestShardTestsIsDeterministic(t *testing.T) {
	var tests []Test
	for i := 0; i < 30; i++ {
		tests = append(tests, mk(fmt.Sprintf("t%02d", i), sec(i%5+1)))
	}
	a := ShardTests(tests, 4)
	b := ShardTests(tests, 4)

	for i := range a {
		if len(a[i].Tests) != len(b[i].Tests) {
			t.Fatalf("shard %d differs in size between runs — sharding must be reproducible "+
				"so a developer can re-run a failed shard", i)
		}
		for j := range a[i].Tests {
			if a[i].Tests[j].ID != b[i].Tests[j].ID {
				t.Fatalf("shard %d position %d differs between runs (%s vs %s)",
					i, j, a[i].Tests[j].ID, b[i].Tests[j].ID)
			}
		}
	}
}

func TestShardTestsDoesNotMutateInput(t *testing.T) {
	tests := []Test{mk("a", sec(1)), mk("b", sec(9)), mk("c", sec(5))}
	before := []string{tests[0].ID, tests[1].ID, tests[2].ID}
	ShardTests(tests, 2)
	for i, id := range before {
		if tests[i].ID != id {
			t.Fatalf("input slice was reordered: position %d was %s, now %s", i, id, tests[i].ID)
		}
	}
}

func TestNewTestsUseDefaultDuration(t *testing.T) {
	// Ten brand-new tests (no history) across 2 workers must still be split evenly, not
	// dumped on worker 0 because they all "cost zero".
	var tests []Test
	for i := 0; i < 10; i++ {
		tests = append(tests, mk(fmt.Sprintf("new%d", i), 0))
	}
	shards := ShardTests(tests, 2)
	if len(shards[0].Tests) != 5 || len(shards[1].Tests) != 5 {
		t.Fatalf("unknown-duration tests must be costed at DefaultDuration and split evenly; "+
			"got %d and %d", len(shards[0].Tests), len(shards[1].Tests))
	}
}

// ---------- 2) Critical path ----------

func TestCriticalPathIsSlowestShard(t *testing.T) {
	shards := []Shard{
		{WorkerID: 0, Tests: []Test{mk("a", sec(3))}},
		{WorkerID: 1, Tests: []Test{mk("b", sec(7))}},
		{WorkerID: 2, Tests: []Test{mk("c", sec(5))}},
	}
	if got := CriticalPath(shards); got != sec(7) {
		t.Fatalf("wall-clock is set by the slowest shard; want 7s, got %v", got)
	}
}

func TestCriticalPathEmpty(t *testing.T) {
	if got := CriticalPath(nil); got != 0 {
		t.Fatalf("want 0 for no shards, got %v", got)
	}
}

func TestMinimumPossibleIsLongestTest(t *testing.T) {
	tests := []Test{mk("a", sec(2)), mk("b", sec(30)), mk("c", sec(4))}
	if got := MinimumPossible(tests); got != sec(30) {
		t.Fatalf("no worker count can beat the single longest test; want 30s, got %v", got)
	}
}

func TestOptimalWorkerCount(t *testing.T) {
	// Four 5s tests: 4 workers reach the 5s floor, more would be waste.
	tests := []Test{mk("a", sec(5)), mk("b", sec(5)), mk("c", sec(5)), mk("d", sec(5))}
	if got := OptimalWorkerCount(tests); got != 4 {
		t.Fatalf("want 4 workers to reach the floor, got %d", got)
	}
}

func TestOptimalWorkerCountDominatedByOneSlowTest(t *testing.T) {
	// One 60s monolith plus three 1s tests. The floor is 60s, and a single worker can
	// already absorb the three small tests alongside... no: worker 1 runs the 60s test while
	// worker 2 runs 3s of small tests, so 2 workers reach the floor.
	tests := []Test{mk("mono", sec(60)), mk("a", sec(1)), mk("b", sec(1)), mk("c", sec(1))}
	got := OptimalWorkerCount(tests)
	if got != 2 {
		t.Fatalf("want 2 (one worker for the monolith, one for the rest), got %d — "+
			"this is the number that tells you to split the monolith instead of buying machines", got)
	}
}

func TestOptimalWorkerCountEmpty(t *testing.T) {
	if got := OptimalWorkerCount(nil); got != 0 {
		t.Fatalf("want 0 for an empty suite, got %d", got)
	}
}

// ---------- 3) Constraints ----------

func TestCanRun(t *testing.T) {
	w := Worker{ID: 0, Capabilities: []string{"browser", "linux"}}

	if !w.CanRun(mk("plain", sec(1))) {
		t.Fatal("a test with no tags can run anywhere")
	}
	if !w.CanRun(mk("ui", sec(1), "browser")) {
		t.Fatal("worker has 'browser', should be able to run it")
	}
	if w.CanRun(mk("gpu", sec(1), "gpu")) {
		t.Fatal("worker lacks 'gpu', must not be eligible")
	}
	if w.CanRun(mk("both", sec(1), "browser", "gpu")) {
		t.Fatal("EVERY required tag must be satisfied, not just one")
	}
}

func TestShardWithConstraintsRespectsCapabilities(t *testing.T) {
	workers := []Worker{
		{ID: 0, Capabilities: []string{"linux"}},
		{ID: 1, Capabilities: []string{"linux", "browser"}},
	}
	tests := []Test{
		mk("ui1", sec(2), "browser"),
		mk("ui2", sec(2), "browser"),
		mk("unit", sec(1)),
	}

	shards, unschedulable := ShardWithConstraints(tests, workers)
	if len(unschedulable) != 0 {
		t.Fatalf("everything is runnable somewhere, got unschedulable %v", unschedulable)
	}
	for _, tc := range shards[0].Tests {
		if len(tc.Tags) > 0 {
			t.Fatalf("worker 0 cannot run %s (needs %v)", tc.ID, tc.Tags)
		}
	}
}

func TestShardWithConstraintsSurfacesUnschedulable(t *testing.T) {
	workers := []Worker{{ID: 0, Capabilities: []string{"linux"}}}
	tests := []Test{mk("gpu-test", sec(5), "gpu"), mk("ok", sec(1))}

	shards, unschedulable := ShardWithConstraints(tests, workers)
	if len(unschedulable) != 1 || unschedulable[0].ID != "gpu-test" {
		t.Fatalf("a test no worker can run must be reported, not silently dropped; got %v", unschedulable)
	}
	total := 0
	for _, s := range shards {
		total += len(s.Tests)
	}
	if total != 1 {
		t.Fatalf("want 1 scheduled test, got %d", total)
	}
}

func TestShardWithConstraintsBalancesWithinEligiblePool(t *testing.T) {
	workers := []Worker{
		{ID: 0, Capabilities: []string{"browser"}},
		{ID: 1, Capabilities: []string{"browser"}},
	}
	var tests []Test
	for i := 0; i < 8; i++ {
		tests = append(tests, mk(fmt.Sprintf("ui%d", i), sec(1), "browser"))
	}

	shards, _ := ShardWithConstraints(tests, workers)
	if len(shards[0].Tests) != 4 || len(shards[1].Tests) != 4 {
		t.Fatalf("8 equal tests across 2 eligible workers should split 4/4; got %d/%d",
			len(shards[0].Tests), len(shards[1].Tests))
	}
}
