// Package ratelimiter is the Module 4.1 assignment: implement three rate-limiting
// algorithms behind a common Limiter interface.
//
// Read 04-case-studies/01-rate-limiter/README.md first.
//
// Fill in every function marked `// TODO`. Run the tests until green:
//
//	go test ./...
//	go test -race ./...   // the token bucket must be safe under concurrency
//
// All limiters take their current time from a `now func() time.Time` field so the
// tests can control time without sleeping. In production this is just time.Now.
package ratelimiter

import (
	"sync"
	"time"
)

// Limiter decides whether one request is allowed right now.
// Allow returns true and "consumes" capacity, or returns false (the caller should 429).
type Limiter interface {
	Allow() bool
}

// ----------------------------------------------------------------------------
// 1) Token Bucket
//
// A bucket holds up to `capacity` tokens and refills at `refillRate` tokens/second.
// Each allowed request consumes one token. Refill is computed lazily from the time
// elapsed since the last call (no background goroutine needed).
// ----------------------------------------------------------------------------

type TokenBucket struct {
	capacity   float64
	refillRate float64 // tokens added per second

	mu     sync.Mutex
	tokens float64
	last   time.Time
	now    func() time.Time
}

// NewTokenBucket creates a bucket that starts full.
func NewTokenBucket(capacity, refillPerSec float64) *TokenBucket {
	now := time.Now
	return &TokenBucket{
		capacity:   capacity,
		refillRate: refillPerSec,
		tokens:     capacity,
		last:       now(),
		now:        now,
	}
}

func (tb *TokenBucket) Allow() bool {
	// TODO:
	//  1. Lock the mutex (this is called from many goroutines).
	//  2. Compute elapsed seconds since tb.last using tb.now().
	//  3. Add elapsed*refillRate tokens, capped at capacity. Update tb.last.
	//  4. If tokens >= 1, subtract 1 and return true; else return false.
	panic("TODO: implement TokenBucket.Allow")
}

// ----------------------------------------------------------------------------
// 2) Fixed Window Counter
//
// Count requests in the current aligned window (e.g. each calendar minute).
// Reset the count when the window rolls over. Simple, but suffers the boundary
// burst bug (see the test TestFixedWindowBoundaryBug).
// ----------------------------------------------------------------------------

type FixedWindow struct {
	limit  int
	window time.Duration

	mu          sync.Mutex
	count       int
	windowStart time.Time // aligned start of the current window
	now         func() time.Time
}

func NewFixedWindow(limit int, window time.Duration) *FixedWindow {
	now := time.Now
	return &FixedWindow{
		limit:       limit,
		window:      window,
		windowStart: now().Truncate(window),
		now:         now,
	}
}

func (fw *FixedWindow) Allow() bool {
	// TODO:
	//  1. Lock. Compute the current aligned window: fw.now().Truncate(fw.window).
	//  2. If it differs from fw.windowStart, roll over: set windowStart, reset count to 0.
	//  3. If count < limit, increment and return true; else return false.
	panic("TODO: implement FixedWindow.Allow")
}

// ----------------------------------------------------------------------------
// 3) Sliding Window Counter
//
// Keep the current and previous aligned-window counts. Estimate the rolling count
// by weighting the previous window by the fraction of it still "in view":
//
//	estimated = prevCount * (1 - elapsedFractionIntoCurrentWindow) + curCount
//
// Allow if estimated < limit. O(1) memory, smooths the boundary bug.
// ----------------------------------------------------------------------------

type SlidingWindowCounter struct {
	limit  int
	window time.Duration

	mu        sync.Mutex
	curWindow time.Time // aligned start of the current window
	curCount  int
	prevCount int
	now       func() time.Time
}

func NewSlidingWindowCounter(limit int, window time.Duration) *SlidingWindowCounter {
	now := time.Now
	return &SlidingWindowCounter{
		limit:     limit,
		window:    window,
		curWindow: now().Truncate(window),
		now:       now,
	}
}

func (s *SlidingWindowCounter) Allow() bool {
	// TODO:
	//  1. Lock. Compute the current aligned window `cur`.
	//  2. If cur != s.curWindow:
	//       - if cur is exactly one window after s.curWindow, prevCount = curCount
	//       - otherwise (a gap), prevCount = 0
	//       - set curWindow = cur, curCount = 0
	//  3. elapsed = fraction into the current window in [0,1):
	//       now.Sub(cur).Seconds() / window.Seconds()
	//  4. estimated = float64(prevCount)*(1-elapsed) + float64(curCount)
	//  5. If estimated < float64(limit): curCount++, return true; else return false.
	panic("TODO: implement SlidingWindowCounter.Allow")
}
