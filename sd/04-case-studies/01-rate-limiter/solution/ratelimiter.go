// Package ratelimiter is the reference solution for Module 4.1.
// Try the assignment yourself before reading this!
package ratelimiter

import (
	"sync"
	"time"
)

type Limiter interface {
	Allow() bool
}

// ---------------- Token Bucket ----------------

type TokenBucket struct {
	capacity   float64
	refillRate float64

	mu     sync.Mutex
	tokens float64
	last   time.Time
	now    func() time.Time
}

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
	tb.mu.Lock()
	defer tb.mu.Unlock()

	now := tb.now()
	elapsed := now.Sub(tb.last).Seconds()
	// Lazily refill based on elapsed wall-clock time, capped at capacity.
	tb.tokens = min(tb.capacity, tb.tokens+elapsed*tb.refillRate)
	tb.last = now

	if tb.tokens >= 1 {
		tb.tokens -= 1
		return true
	}
	return false
}

// ---------------- Fixed Window ----------------

type FixedWindow struct {
	limit  int
	window time.Duration

	mu          sync.Mutex
	count       int
	windowStart time.Time
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
	fw.mu.Lock()
	defer fw.mu.Unlock()

	cur := fw.now().Truncate(fw.window)
	if cur != fw.windowStart {
		fw.windowStart = cur
		fw.count = 0
	}
	if fw.count < fw.limit {
		fw.count++
		return true
	}
	return false
}

// ---------------- Sliding Window Counter ----------------

type SlidingWindowCounter struct {
	limit  int
	window time.Duration

	mu        sync.Mutex
	curWindow time.Time
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
	s.mu.Lock()
	defer s.mu.Unlock()

	now := s.now()
	cur := now.Truncate(s.window)
	if cur != s.curWindow {
		if cur.Sub(s.curWindow) == s.window {
			// We advanced exactly one window: the old current becomes previous.
			s.prevCount = s.curCount
		} else {
			// A gap of 2+ windows: nothing carries over.
			s.prevCount = 0
		}
		s.curWindow = cur
		s.curCount = 0
	}

	// Fraction of the current window already elapsed, in [0, 1).
	elapsed := now.Sub(cur).Seconds() / s.window.Seconds()
	estimated := float64(s.prevCount)*(1-elapsed) + float64(s.curCount)

	if estimated < float64(s.limit) {
		s.curCount++
		return true
	}
	return false
}
