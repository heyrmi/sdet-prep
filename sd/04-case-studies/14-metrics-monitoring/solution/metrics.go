// Package metrics is the reference solution for Module 4.14.
// Try the assignment yourself before reading this!
package metrics

import (
	"sort"
	"sync"
)

type Aggregation int

const (
	Sum Aggregation = iota
	Avg
	Count
	Max
	Min
)

type Point struct {
	WindowStart int64
	Value       float64
}

type sample struct {
	ts  int64
	val float64
}

type Store struct {
	mu     sync.Mutex
	series map[string][]sample
}

func NewStore() *Store {
	return &Store{series: make(map[string][]sample)}
}

func (s *Store) Record(name string, value float64, tsSec int64) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.series[name] = append(s.series[name], sample{ts: tsSec, val: value})
}

func (s *Store) Query(name string, fromSec, toSec, windowSec int64, agg Aggregation) []Point {
	s.mu.Lock()
	samples := s.series[name]
	// Copy out the in-range samples while holding the lock, then release it.
	in := make([]sample, 0, len(samples))
	for _, sm := range samples {
		if sm.ts >= fromSec && sm.ts < toSec {
			in = append(in, sm)
		}
	}
	s.mu.Unlock()

	// Group by aligned window start.
	buckets := make(map[int64][]float64)
	for _, sm := range in {
		ws := (sm.ts / windowSec) * windowSec
		buckets[ws] = append(buckets[ws], sm.val)
	}

	points := make([]Point, 0, len(buckets))
	for ws, vals := range buckets {
		points = append(points, Point{WindowStart: ws, Value: reduce(vals, agg)})
	}
	sort.Slice(points, func(i, j int) bool { return points[i].WindowStart < points[j].WindowStart })
	return points
}

func reduce(vals []float64, agg Aggregation) float64 {
	switch agg {
	case Count:
		return float64(len(vals))
	case Sum:
		total := 0.0
		for _, v := range vals {
			total += v
		}
		return total
	case Avg:
		total := 0.0
		for _, v := range vals {
			total += v
		}
		return total / float64(len(vals))
	case Max:
		m := vals[0]
		for _, v := range vals[1:] {
			if v > m {
				m = v
			}
		}
		return m
	case Min:
		m := vals[0]
		for _, v := range vals[1:] {
			if v < m {
				m = v
			}
		}
		return m
	}
	return 0
}

func (s *Store) Rate(name string, fromSec, toSec int64) float64 {
	s.mu.Lock()
	samples := s.series[name]
	var first, last sample
	found := false
	for _, sm := range samples {
		if sm.ts < fromSec || sm.ts >= toSec {
			continue
		}
		if !found {
			first, last = sm, sm
			found = true
			continue
		}
		if sm.ts < first.ts {
			first = sm
		}
		if sm.ts > last.ts {
			last = sm
		}
	}
	s.mu.Unlock()

	if !found || first.ts == last.ts {
		return 0
	}
	return (last.val - first.val) / float64(last.ts-first.ts)
}
