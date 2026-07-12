package metrics

import (
	"sync"
	"testing"
)

// Helper: assert two []Point slices are equal (order matters).
func wantPoints(t *testing.T, got, want []Point) {
	t.Helper()
	if len(got) != len(want) {
		t.Fatalf("point count: got %d %v, want %d %v", len(got), got, len(want), want)
	}
	for i := range want {
		if got[i].WindowStart != want[i].WindowStart || got[i].Value != want[i].Value {
			t.Fatalf("point %d: got %+v, want %+v", i, got[i], want[i])
		}
	}
}

// ---------- tumbling-window bucketing ----------

func TestQueryBucketsIntoWindows(t *testing.T) {
	s := NewStore()
	// window = 10s. Samples land in windows [0,10) and [10,20).
	s.Record("cpu", 1, 0)
	s.Record("cpu", 2, 5)  // window 0
	s.Record("cpu", 3, 9)  // window 0
	s.Record("cpu", 4, 10) // window 10
	s.Record("cpu", 5, 19) // window 10

	got := s.Query("cpu", 0, 20, 10, Count)
	wantPoints(t, got, []Point{
		{WindowStart: 0, Value: 3},
		{WindowStart: 10, Value: 2},
	})
}

// ---------- each aggregation ----------

func TestAggregations(t *testing.T) {
	s := NewStore()
	// All five samples fall in window [0,60).
	for _, v := range []float64{2, 4, 6, 8, 10} {
		s.Record("m", v, 0)
	}

	cases := []struct {
		agg  Aggregation
		want float64
	}{
		{Sum, 30},
		{Avg, 6},
		{Count, 5},
		{Max, 10},
		{Min, 2},
	}
	for _, c := range cases {
		got := s.Query("m", 0, 60, 60, c.agg)
		wantPoints(t, got, []Point{{WindowStart: 0, Value: c.want}})
	}
}

// ---------- empty windows produce NO point ----------

func TestQueryEmptyWindow(t *testing.T) {
	s := NewStore()
	// Samples only in window [0,10) and [20,30); window [10,20) is empty.
	s.Record("x", 1, 1)
	s.Record("x", 1, 25)

	got := s.Query("x", 0, 30, 10, Count)
	// Documented behaviour: the empty middle window [10,20) yields no Point.
	wantPoints(t, got, []Point{
		{WindowStart: 0, Value: 1},
		{WindowStart: 20, Value: 1},
	})
}

// ---------- range filtering ----------

func TestQueryRangeFiltering(t *testing.T) {
	s := NewStore()
	s.Record("r", 1, 5)  // below from? no, included
	s.Record("r", 1, 0)  // ts == from -> included ([from, to) is inclusive of from)
	s.Record("r", 1, 30) // ts == to   -> EXCLUDED (half-open)
	s.Record("r", 1, 99) // far out    -> excluded

	got := s.Query("r", 0, 30, 10, Count)
	wantPoints(t, got, []Point{
		{WindowStart: 0, Value: 2}, // ts 0 and 5
	})
}

// ---------- Rate (counter per second) ----------

func TestRate(t *testing.T) {
	s := NewStore()
	// A counter increasing by 10 every 5 seconds -> 2 per second.
	s.Record("requests_total", 100, 0)
	s.Record("requests_total", 110, 5)
	s.Record("requests_total", 120, 10)

	got := s.Rate("requests_total", 0, 100)
	want := 2.0 // (120 - 100) / (10 - 0)
	if got != want {
		t.Fatalf("Rate: got %v, want %v", got, want)
	}
}

func TestRateInsufficientSamples(t *testing.T) {
	s := NewStore()
	s.Record("c", 5, 3)
	if got := s.Rate("c", 0, 100); got != 0 {
		t.Fatalf("Rate with one sample should be 0, got %v", got)
	}
	if got := s.Rate("missing", 0, 100); got != 0 {
		t.Fatalf("Rate with no samples should be 0, got %v", got)
	}
}

func TestRateRespectsRange(t *testing.T) {
	s := NewStore()
	s.Record("c", 0, 0)
	s.Record("c", 100, 50) // outside the queried range below
	s.Record("c", 10, 10)
	// Only ts 0 and 10 are in [0, 20): (10 - 0) / (10 - 0) = 1/sec.
	if got := s.Rate("c", 0, 20); got != 1 {
		t.Fatalf("Rate should ignore out-of-range samples; got %v, want 1", got)
	}
}

// ---------- concurrency ----------

func TestRecordConcurrent(t *testing.T) {
	s := NewStore()
	var wg sync.WaitGroup
	// 50 goroutines each record 100 samples of value 1 at ts 0 -> Count must be 5000.
	for i := 0; i < 50; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for j := 0; j < 100; j++ {
				s.Record("hits", 1, 0)
			}
		}()
	}
	wg.Wait()

	got := s.Query("hits", 0, 60, 60, Count)
	wantPoints(t, got, []Point{{WindowStart: 0, Value: 5000}})
}
