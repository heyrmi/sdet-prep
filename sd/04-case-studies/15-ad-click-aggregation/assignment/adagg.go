// Package adagg is the Module 4.15 assignment: a streaming ad-click aggregator
// with exactly-once counting via event-ID dedup and tumbling-window top-K.
//
// Read 04-case-studies/15-ad-click-aggregation/README.md first.
//
// Fill in every function marked `// TODO`. Run the tests until green:
//
//	go test ./...
//	go test -race ./...   // ProcessEvent is called from many goroutines
//
// The core challenge mirrors a real pipeline: events may be delivered more than
// once (at-least-once queues), so we dedup on a unique event ID to get
// exactly-once counts.
package adagg

import "sync"

// ClickEvent is one ad click flowing through the stream.
type ClickEvent struct {
	ID    string // globally unique event id; used for dedup
	AdID  string // which ad was clicked
	TsSec int64  // event time, in seconds
}

// AdCount is one row of a top-K result.
type AdCount struct {
	AdID  string
	Count int64
}

// Aggregator counts clicks per ad per tumbling window, ignoring duplicate IDs.
// It is safe for concurrent use.
type Aggregator struct {
	mu   sync.Mutex
	seen map[string]struct{} // event IDs already processed (dedup set)
	// counts[windowStart][adID] = clicks
	counts map[int64]map[string]int64
}

// NewAggregator creates an empty aggregator.
func NewAggregator() *Aggregator {
	return &Aggregator{
		seen:   make(map[string]struct{}),
		counts: make(map[int64]map[string]int64),
	}
}

// windowStart returns the aligned start of the tumbling window of width windowSec
// that contains tsSec.
func windowStart(tsSec, windowSec int64) int64 {
	return (tsSec / windowSec) * windowSec
}

// ProcessEvent ingests one event. If its ID was already seen, it is ignored
// (exactly-once). Otherwise it increments the click count for e.AdID.
//
// The window width isn't known until query time, so ingest stores counts at a
// fixed 1-second granularity (keyed by e.TsSec). Query-time functions then sum
// the seconds that fall inside the requested tumbling window. This decouples
// ingestion from any single window size.
func (a *Aggregator) ProcessEvent(e ClickEvent) {
	// TODO:
	//  1. Lock the mutex (ProcessEvent is called concurrently).
	//  2. If e.ID is already in a.seen, return (duplicate -> ignore).
	//  3. Add e.ID to a.seen.
	//  4. Bucket by the event's SECOND: ws := e.TsSec (store at 1s granularity).
	//     i.e. use a.counts[e.TsSec][e.AdID]++. Query time will sum the seconds
	//     that fall inside the requested window.
	panic("TODO: implement Aggregator.ProcessEvent")
}

// CountForWindow returns the number of clicks for adID in the tumbling window
// [windowStartSec, windowStartSec+windowSec). windowStartSec is assumed aligned
// to windowSec (the tests pass aligned starts).
func (a *Aggregator) CountForWindow(adID string, windowStartSec, windowSec int64) int64 {
	// TODO:
	//  1. Lock.
	//  2. Sum a.counts[sec][adID] for every sec in
	//     [windowStartSec, windowStartSec+windowSec).
	//  3. Return the total.
	panic("TODO: implement Aggregator.CountForWindow")
}

// TopK returns the k ads with the most clicks in the tumbling window
// [windowStartSec, windowStartSec+windowSec), sorted by Count DESC and then by
// AdID ASC for ties. If fewer than k ads have clicks, the result is shorter.
func (a *Aggregator) TopK(windowStartSec, windowSec int64, k int) []AdCount {
	// TODO:
	//  1. Lock.
	//  2. Sum clicks per adID across all seconds in the window into a map.
	//  3. Flatten to []AdCount, sort by Count DESC, tie-break AdID ASC.
	//  4. Return the first min(k, len) entries.
	panic("TODO: implement Aggregator.TopK")
}
