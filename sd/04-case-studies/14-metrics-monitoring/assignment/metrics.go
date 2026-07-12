// Package metrics is the Module 4.14 assignment: an in-memory time-series store
// with tumbling-window aggregation, the core of a Prometheus/Datadog-like system.
//
// Read 04-case-studies/14-metrics-monitoring/README.md first.
//
// Fill in every function marked `// TODO`. Run the tests until green:
//
//	go test ./...
//	go test -race ./...   // Record is called from many goroutines
//
// A "time series" is identified by a metric name. Each sample is a (timestamp,
// value) pair. Query rolls samples up into fixed, non-overlapping "tumbling"
// windows and applies an aggregation function to each window.
package metrics

import "sync"

// Aggregation selects how the samples inside one window are reduced to a value.
type Aggregation int

const (
	Sum Aggregation = iota
	Avg
	Count
	Max
	Min
)

// Point is one aggregated output: the start timestamp of a tumbling window and
// the aggregated value for the samples that fell into it.
type Point struct {
	WindowStart int64   // seconds; aligned to windowSec
	Value       float64
}

// sample is a single recorded data point inside a series.
type sample struct {
	ts  int64 // seconds
	val float64
}

// Store holds one slice of samples per metric name. It is safe for concurrent use.
type Store struct {
	mu     sync.Mutex
	series map[string][]sample
}

// NewStore creates an empty store.
func NewStore() *Store {
	return &Store{series: make(map[string][]sample)}
}

// Record appends one sample (value at tsSec) to the named series.
func (s *Store) Record(name string, value float64, tsSec int64) {
	// TODO:
	//  1. Lock the mutex (Record is called concurrently).
	//  2. Append a sample{ts: tsSec, val: value} to s.series[name].
	panic("TODO: implement Store.Record")
}

// Query rolls the named series up into tumbling windows of width windowSec over
// the half-open range [fromSec, toSec) and applies agg to each window.
//
// A tumbling window is aligned to a multiple of windowSec: a sample at ts belongs
// to the window starting at (ts / windowSec) * windowSec. Windows do not overlap.
//
// Return one Point per NON-EMPTY window, ordered by WindowStart ascending. Empty
// windows produce NO point (documented behaviour — tested in TestQueryEmptyWindow).
// Samples with ts < fromSec or ts >= toSec are excluded.
func (s *Store) Query(name string, fromSec, toSec, windowSec int64, agg Aggregation) []Point {
	// TODO:
	//  1. Lock. Read s.series[name].
	//  2. Group in-range samples by window start = (ts/windowSec)*windowSec.
	//  3. For each window with >=1 sample, reduce its values with `agg`:
	//       Sum   -> total
	//       Avg   -> total / count
	//       Count -> number of samples
	//       Max   -> largest value
	//       Min   -> smallest value
	//  4. Return the points sorted by WindowStart ascending.
	panic("TODO: implement Store.Query")
}

// Rate treats the named series as a monotonically increasing counter and returns
// its average increase per second over [fromSec, toSec): it uses the first and
// last in-range samples as (lastValue - firstValue) / (lastTs - firstTs).
//
// Return 0 if there are fewer than 2 in-range samples or if the timestamps are
// equal (no time elapsed).
func (s *Store) Rate(name string, fromSec, toSec int64) float64 {
	// TODO:
	//  1. Lock. Collect in-range samples ordered by ts (samples are recorded in
	//     time order in the tests, but be safe: find the earliest and latest ts).
	//  2. If fewer than 2 in-range samples, or first.ts == last.ts, return 0.
	//  3. Return (last.val - first.val) / float64(last.ts - first.ts).
	panic("TODO: implement Store.Rate")
}
