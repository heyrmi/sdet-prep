package adagg

import (
	"fmt"
	"sync"
	"sync/atomic"
	"testing"
)

func wantTopK(t *testing.T, got, want []AdCount) {
	t.Helper()
	if len(got) != len(want) {
		t.Fatalf("topK length: got %d %v, want %d %v", len(got), got, len(want), want)
	}
	for i := range want {
		if got[i] != want[i] {
			t.Fatalf("topK[%d]: got %+v, want %+v", i, got[i], want[i])
		}
	}
}

// ---------- dedup / exactly-once ----------

func TestDuplicateIDsCountedOnce(t *testing.T) {
	a := NewAggregator()
	a.ProcessEvent(ClickEvent{ID: "e1", AdID: "ad", TsSec: 5})
	a.ProcessEvent(ClickEvent{ID: "e1", AdID: "ad", TsSec: 5}) // exact duplicate
	a.ProcessEvent(ClickEvent{ID: "e1", AdID: "ad", TsSec: 5}) // again

	if got := a.CountForWindow("ad", 0, 60); got != 1 {
		t.Fatalf("duplicate IDs must count once; got %d, want 1", got)
	}
}

// ---------- window bucketing ----------

func TestBucketsIntoWindows(t *testing.T) {
	a := NewAggregator()
	// window width 60s. Window [0,60) and [60,120).
	a.ProcessEvent(ClickEvent{ID: "1", AdID: "ad", TsSec: 0})
	a.ProcessEvent(ClickEvent{ID: "2", AdID: "ad", TsSec: 59})
	a.ProcessEvent(ClickEvent{ID: "3", AdID: "ad", TsSec: 60})
	a.ProcessEvent(ClickEvent{ID: "4", AdID: "ad", TsSec: 119})

	if got := a.CountForWindow("ad", 0, 60); got != 2 {
		t.Fatalf("window [0,60): got %d, want 2", got)
	}
	if got := a.CountForWindow("ad", 60, 60); got != 2 {
		t.Fatalf("window [60,120): got %d, want 2", got)
	}
}

func TestCountExcludesOutOfWindow(t *testing.T) {
	a := NewAggregator()
	a.ProcessEvent(ClickEvent{ID: "1", AdID: "ad", TsSec: 10}) // in [0,60)
	a.ProcessEvent(ClickEvent{ID: "2", AdID: "ad", TsSec: 60}) // start of NEXT window, excluded
	a.ProcessEvent(ClickEvent{ID: "3", AdID: "ad", TsSec: 5})  // in [0,60)

	if got := a.CountForWindow("ad", 0, 60); got != 2 {
		t.Fatalf("got %d, want 2 (ts 60 is the next window)", got)
	}
}

// ---------- TopK ordering, tie-break, limit ----------

func TestTopKOrderingAndLimit(t *testing.T) {
	a := NewAggregator()
	add := func(id, ad string, n int) {
		for i := 0; i < n; i++ {
			a.ProcessEvent(ClickEvent{ID: fmt.Sprintf("%s-%d", id, i), AdID: ad, TsSec: 1})
		}
	}
	add("a", "adA", 5)
	add("b", "adB", 3)
	add("c", "adC", 3) // tie with adB on count -> adB before adC (AdID ASC)
	add("d", "adD", 1)

	got := a.TopK(0, 60, 3)
	wantTopK(t, got, []AdCount{
		{AdID: "adA", Count: 5},
		{AdID: "adB", Count: 3}, // tie-break: adB < adC
		{AdID: "adC", Count: 3},
	})
}

func TestTopKFewerThanK(t *testing.T) {
	a := NewAggregator()
	a.ProcessEvent(ClickEvent{ID: "1", AdID: "only", TsSec: 1})
	got := a.TopK(0, 60, 10)
	wantTopK(t, got, []AdCount{{AdID: "only", Count: 1}})
}

func TestTopKRespectsWindow(t *testing.T) {
	a := NewAggregator()
	a.ProcessEvent(ClickEvent{ID: "1", AdID: "adA", TsSec: 5})  // window [0,60)
	a.ProcessEvent(ClickEvent{ID: "2", AdID: "adB", TsSec: 65}) // window [60,120)
	a.ProcessEvent(ClickEvent{ID: "3", AdID: "adB", TsSec: 70}) // window [60,120)

	got := a.TopK(60, 60, 5)
	wantTopK(t, got, []AdCount{{AdID: "adB", Count: 2}})
}

// ---------- concurrency: -race ----------

func TestProcessEventConcurrent(t *testing.T) {
	a := NewAggregator()
	var wg sync.WaitGroup

	// 100 goroutines each try to process the SAME 50 unique IDs. With dedup, each
	// ID is counted exactly once: total must be 50, all for "ad" in window [0,60).
	var attempts int64
	for g := 0; g < 100; g++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for i := 0; i < 50; i++ {
				a.ProcessEvent(ClickEvent{ID: fmt.Sprintf("evt-%d", i), AdID: "ad", TsSec: 1})
				atomic.AddInt64(&attempts, 1)
			}
		}()
	}
	wg.Wait()

	if attempts != 5000 {
		t.Fatalf("sanity: expected 5000 attempts, got %d", attempts)
	}
	if got := a.CountForWindow("ad", 0, 60); got != 50 {
		t.Fatalf("dedup under concurrency failed: got %d, want 50", got)
	}
}
