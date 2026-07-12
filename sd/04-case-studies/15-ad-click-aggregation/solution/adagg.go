// Package adagg is the reference solution for Module 4.15.
// Try the assignment yourself before reading this!
package adagg

import (
	"sort"
	"sync"
)

type ClickEvent struct {
	ID    string
	AdID  string
	TsSec int64
}

type AdCount struct {
	AdID  string
	Count int64
}

type Aggregator struct {
	mu     sync.Mutex
	seen   map[string]struct{}
	counts map[int64]map[string]int64 // counts[second][adID] = clicks
}

func NewAggregator() *Aggregator {
	return &Aggregator{
		seen:   make(map[string]struct{}),
		counts: make(map[int64]map[string]int64),
	}
}

func windowStart(tsSec, windowSec int64) int64 {
	return (tsSec / windowSec) * windowSec
}

func (a *Aggregator) ProcessEvent(e ClickEvent) {
	a.mu.Lock()
	defer a.mu.Unlock()

	if _, dup := a.seen[e.ID]; dup {
		return
	}
	a.seen[e.ID] = struct{}{}

	bySec := a.counts[e.TsSec]
	if bySec == nil {
		bySec = make(map[string]int64)
		a.counts[e.TsSec] = bySec
	}
	bySec[e.AdID]++
}

func (a *Aggregator) CountForWindow(adID string, windowStartSec, windowSec int64) int64 {
	a.mu.Lock()
	defer a.mu.Unlock()

	var total int64
	for sec := windowStartSec; sec < windowStartSec+windowSec; sec++ {
		if bySec, ok := a.counts[sec]; ok {
			total += bySec[adID]
		}
	}
	return total
}

func (a *Aggregator) TopK(windowStartSec, windowSec int64, k int) []AdCount {
	a.mu.Lock()
	totals := make(map[string]int64)
	for sec := windowStartSec; sec < windowStartSec+windowSec; sec++ {
		for adID, c := range a.counts[sec] {
			totals[adID] += c
		}
	}
	a.mu.Unlock()

	out := make([]AdCount, 0, len(totals))
	for adID, c := range totals {
		out = append(out, AdCount{AdID: adID, Count: c})
	}
	sort.Slice(out, func(i, j int) bool {
		if out[i].Count != out[j].Count {
			return out[i].Count > out[j].Count // count DESC
		}
		return out[i].AdID < out[j].AdID // tie-break AdID ASC
	})

	if k < len(out) {
		out = out[:k]
	}
	return out
}
