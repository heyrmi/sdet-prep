package devicepool

import (
	"errors"
	"sync"
	"testing"
	"time"
)

func base() time.Time { return time.Date(2026, 1, 1, 0, 0, 0, 0, time.UTC) }

func chromePool(n, slots int) []Node {
	var nodes []Node
	for i := 0; i < n; i++ {
		nodes = append(nodes, Node{ID: "chrome-" + itoa(i), Capability: "chrome", Slots: slots})
	}
	return nodes
}

// ---------- 1) Acquire / Release ----------

func TestAcquireAndRelease(t *testing.T) {
	p := NewPool(chromePool(1, 2), time.Minute, 1.0, 3)

	l1, err := p.Acquire("teamA", "chrome")
	if err != nil {
		t.Fatalf("first acquire should succeed: %v", err)
	}
	if l1.NodeID != "chrome-0" || l1.Tenant != "teamA" {
		t.Fatalf("unexpected lease %+v", l1)
	}

	if _, err := p.Acquire("teamA", "chrome"); err != nil {
		t.Fatalf("second slot should be available: %v", err)
	}
	if _, err := p.Acquire("teamA", "chrome"); !errors.Is(err, ErrNoCapacity) {
		t.Fatalf("node has 2 slots and both are taken, want ErrNoCapacity, got %v", err)
	}

	if err := p.Release(l1.ID); err != nil {
		t.Fatalf("release should succeed: %v", err)
	}
	if _, err := p.Acquire("teamA", "chrome"); err != nil {
		t.Fatalf("a released slot must be reusable: %v", err)
	}
}

func TestAcquireRejectsUnknownCapability(t *testing.T) {
	p := NewPool(chromePool(2, 2), time.Minute, 1.0, 3)
	if _, err := p.Acquire("teamA", "safari"); !errors.Is(err, ErrNoCapacity) {
		t.Fatalf("no safari nodes exist, want ErrNoCapacity, got %v", err)
	}
}

func TestReleaseUnknownLease(t *testing.T) {
	p := NewPool(chromePool(1, 1), time.Minute, 1.0, 3)
	if err := p.Release("nope"); !errors.Is(err, ErrUnknownLease) {
		t.Fatalf("want ErrUnknownLease, got %v", err)
	}

	l, _ := p.Acquire("t", "chrome")
	if err := p.Release(l.ID); err != nil {
		t.Fatal(err)
	}
	if err := p.Release(l.ID); !errors.Is(err, ErrUnknownLease) {
		t.Fatal("double release must be rejected, or capacity accounting drifts")
	}
}

func TestAcquireSpreadsLoadAcrossNodes(t *testing.T) {
	// Three nodes, two slots each. The first three leases must land on three different nodes,
	// not stack onto node 0 — stacking makes one node's failure take out three sessions.
	p := NewPool(chromePool(3, 2), time.Minute, 1.0, 3)

	seen := map[string]bool{}
	for i := 0; i < 3; i++ {
		l, err := p.Acquire("t", "chrome")
		if err != nil {
			t.Fatalf("acquire %d: %v", i, err)
		}
		seen[l.NodeID] = true
	}
	if len(seen) != 3 {
		t.Fatalf("leases must spread least-loaded-first across 3 nodes, landed on %d (%v)", len(seen), seen)
	}
}

// ---------- Fairness ----------

func TestPerTenantShareCap(t *testing.T) {
	// 10 total slots, 30% cap => one tenant may hold 3.
	p := NewPool(chromePool(5, 2), time.Minute, 0.3, 3)

	for i := 0; i < 3; i++ {
		if _, err := p.Acquire("hog", "chrome"); err != nil {
			t.Fatalf("lease %d should be within the cap: %v", i+1, err)
		}
	}
	if _, err := p.Acquire("hog", "chrome"); !errors.Is(err, ErrNoCapacity) {
		t.Fatalf("4th lease exceeds the 30%% cap of 10 slots, want ErrNoCapacity, got %v", err)
	}
	// Another tenant is unaffected — that is the point of the cap.
	if _, err := p.Acquire("other", "chrome"); err != nil {
		t.Fatalf("a different tenant must still get capacity: %v", err)
	}
}

func TestShareCapRoundsUpToAtLeastOne(t *testing.T) {
	// 2 slots, 10% cap => floor gives 0, which would make the pool unusable.
	p := NewPool(chromePool(1, 2), time.Minute, 0.1, 3)
	if _, err := p.Acquire("t", "chrome"); err != nil {
		t.Fatalf("a cap that computes to 0 must be treated as 1: %v", err)
	}
}

func TestReleasingFreesTenantQuota(t *testing.T) {
	p := NewPool(chromePool(5, 2), time.Minute, 0.3, 3)
	var ids []string
	for i := 0; i < 3; i++ {
		l, _ := p.Acquire("hog", "chrome")
		ids = append(ids, l.ID)
	}
	if err := p.Release(ids[0]); err != nil {
		t.Fatal(err)
	}
	if _, err := p.Acquire("hog", "chrome"); err != nil {
		t.Fatalf("releasing should free quota back to the tenant: %v", err)
	}
}

// ---------- Lease expiry ----------

func TestExpiredLeasesAreReclaimed(t *testing.T) {
	p := NewPool(chromePool(1, 1), 30*time.Second, 1.0, 3)
	clock := base()
	p.now = func() time.Time { return clock }

	if _, err := p.Acquire("crashed", "chrome"); err != nil {
		t.Fatal(err)
	}
	if _, err := p.Acquire("other", "chrome"); !errors.Is(err, ErrNoCapacity) {
		t.Fatal("the single slot is held")
	}

	// The client crashes and never renews.
	clock = base().Add(31 * time.Second)
	if _, err := p.Acquire("other", "chrome"); err != nil {
		t.Fatalf("an expired lease must be reclaimed, or a crashed client shrinks the pool forever: %v", err)
	}
}

func TestRenewExtendsLease(t *testing.T) {
	p := NewPool(chromePool(1, 1), 30*time.Second, 1.0, 3)
	clock := base()
	p.now = func() time.Time { return clock }

	l, _ := p.Acquire("alive", "chrome")

	clock = base().Add(20 * time.Second)
	if err := p.Renew(l.ID); err != nil {
		t.Fatalf("renew before expiry should succeed: %v", err)
	}

	clock = base().Add(45 * time.Second) // past the ORIGINAL expiry, within the renewed one
	if _, err := p.Acquire("other", "chrome"); !errors.Is(err, ErrNoCapacity) {
		t.Fatal("a renewed lease must still hold its slot")
	}
}

func TestRenewExpiredLeaseFails(t *testing.T) {
	p := NewPool(chromePool(1, 1), 30*time.Second, 1.0, 3)
	clock := base()
	p.now = func() time.Time { return clock }

	l, _ := p.Acquire("zombie", "chrome")
	clock = base().Add(60 * time.Second)

	if err := p.Renew(l.ID); !errors.Is(err, ErrUnknownLease) {
		t.Fatalf("an expired lease must not be renewable — its slot may already belong to "+
			"someone else; got %v", err)
	}
}

// ---------- 2) Health ----------

func TestNodeEjectedAfterConsecutiveFailures(t *testing.T) {
	p := NewPool(chromePool(2, 1), time.Minute, 1.0, 3)

	p.ReportResult("chrome-0", false)
	p.ReportResult("chrome-0", false)
	if healthy := p.ReportResult("chrome-0", false); healthy {
		t.Fatal("3 consecutive failures at threshold 3 must eject the node")
	}

	if got := p.HealthyNodes(); len(got) != 1 || got[0] != "chrome-1" {
		t.Fatalf("want only chrome-1 healthy, got %v", got)
	}
}

func TestSuccessResetsFailureCount(t *testing.T) {
	p := NewPool(chromePool(1, 1), time.Minute, 1.0, 3)

	p.ReportResult("chrome-0", false)
	p.ReportResult("chrome-0", false)
	p.ReportResult("chrome-0", true) // reset
	p.ReportResult("chrome-0", false)
	p.ReportResult("chrome-0", false)

	if got := p.HealthyNodes(); len(got) != 1 {
		t.Fatalf("only 2 consecutive failures since the reset, node should still be healthy; got %v", got)
	}
}

func TestEjectedNodeReceivesNoLeases(t *testing.T) {
	p := NewPool(chromePool(2, 1), time.Minute, 1.0, 2)
	p.ReportResult("chrome-0", false)
	p.ReportResult("chrome-0", false) // ejected

	for i := 0; i < 1; i++ {
		l, err := p.Acquire("t", "chrome")
		if err != nil {
			t.Fatalf("chrome-1 is still healthy: %v", err)
		}
		if l.NodeID == "chrome-0" {
			t.Fatal("an ejected node must not receive new leases — a wedged node fails every "+
				"test routed to it and the failures look like product bugs")
		}
	}
	if _, err := p.Acquire("t", "chrome"); !errors.Is(err, ErrNoCapacity) {
		t.Fatal("only chrome-1's single slot was available")
	}
}

func TestRecoveredNodeRejoins(t *testing.T) {
	p := NewPool(chromePool(1, 1), time.Minute, 1.0, 2)
	p.ReportResult("chrome-0", false)
	p.ReportResult("chrome-0", false)
	if len(p.HealthyNodes()) != 0 {
		t.Fatal("setup: node should be ejected")
	}

	if healthy := p.ReportResult("chrome-0", true); !healthy {
		t.Fatal("a node that starts succeeding again must rejoin the pool")
	}
}

func TestReportResultUnknownNode(t *testing.T) {
	p := NewPool(chromePool(1, 1), time.Minute, 1.0, 2)
	if p.ReportResult("ghost", true) {
		t.Fatal("unknown node must report unhealthy")
	}
}

// ---------- 3) Autoscaling ----------

func TestScaleUpOnQueueDepth(t *testing.T) {
	p := NewPool(chromePool(2, 4), time.Minute, 1.0, 3)
	d := p.RecommendScale("chrome", 10, 4)
	if d.Delta != 3 {
		t.Fatalf("10 waiting / 4 slots per node = ceil(2.5) = 3 nodes, got %d", d.Delta)
	}
}

func TestScaleDownWhenIdle(t *testing.T) {
	p := NewPool(chromePool(4, 4), time.Minute, 1.0, 3)
	p.Acquire("t", "chrome") // 1 of 16 slots => 6% utilisation

	d := p.RecommendScale("chrome", 0, 4)
	if d.Delta != -1 {
		t.Fatalf("deep idle should scale down by exactly 1 (never thrash), got %d", d.Delta)
	}
}

func TestNoScaleDownBelowOneNode(t *testing.T) {
	p := NewPool(chromePool(1, 4), time.Minute, 1.0, 3)
	if d := p.RecommendScale("chrome", 0, 4); d.Delta != 0 {
		t.Fatalf("must never scale below 1 node, got %d", d.Delta)
	}
}

func TestNoScaleWhenBusyAndNoQueue(t *testing.T) {
	p := NewPool(chromePool(2, 2), time.Minute, 1.0, 3)
	for i := 0; i < 3; i++ {
		p.Acquire("t", "chrome") // 3 of 4 slots = 75%
	}
	if d := p.RecommendScale("chrome", 0, 2); d.Delta != 0 {
		t.Fatalf("healthy utilisation with no queue needs no change, got %d", d.Delta)
	}
}

// ---------- Concurrency ----------

func TestPoolIsRaceFree(t *testing.T) {
	p := NewPool(chromePool(10, 10), time.Minute, 1.0, 3)
	var wg sync.WaitGroup

	for w := 0; w < 8; w++ {
		wg.Add(1)
		go func(w int) {
			defer wg.Done()
			for i := 0; i < 50; i++ {
				if l, err := p.Acquire("t"+itoa(w), "chrome"); err == nil {
					p.Renew(l.ID)
					p.Release(l.ID)
				}
				p.ReportResult("chrome-"+itoa(i%10), i%7 != 0)
				_ = p.HealthyNodes()
				_ = p.RecommendScale("chrome", i%3, 10)
			}
		}(w)
	}
	wg.Wait()
}

func TestNoSlotLeakUnderChurn(t *testing.T) {
	// Acquire and release the whole pool repeatedly; capacity must return to full each time.
	p := NewPool(chromePool(3, 2), time.Minute, 1.0, 3)

	for round := 0; round < 5; round++ {
		var ids []string
		for {
			l, err := p.Acquire("t", "chrome")
			if err != nil {
				break
			}
			ids = append(ids, l.ID)
		}
		if len(ids) != 6 {
			t.Fatalf("round %d: want to fill all 6 slots, got %d — capacity is leaking", round, len(ids))
		}
		for _, id := range ids {
			if err := p.Release(id); err != nil {
				t.Fatal(err)
			}
		}
	}
}
