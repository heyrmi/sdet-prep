package ratelimiter

import (
	"sync"
	"sync/atomic"
	"testing"
	"time"
)

// mockClock lets tests drive time forward deterministically — no real sleeping.
type mockClock struct{ t time.Time }

func (m *mockClock) Now() time.Time            { return m.t }
func (m *mockClock) Advance(d time.Duration)   { m.t = m.t.Add(d) }

func base() time.Time { return time.Date(2026, 1, 1, 0, 0, 0, 0, time.UTC) }

// ---------- Token Bucket ----------

func TestTokenBucketBurstThenEmpty(t *testing.T) {
	clk := &mockClock{t: base()}
	tb := NewTokenBucket(5, 1) // capacity 5, 1 token/sec
	tb.now, tb.last, tb.tokens = clk.Now, clk.Now(), 5

	for i := 0; i < 5; i++ {
		if !tb.Allow() {
			t.Fatalf("request %d should be allowed from the initial burst of 5", i+1)
		}
	}
	if tb.Allow() {
		t.Fatal("6th request should be denied: bucket is empty and no time has passed")
	}
}

func TestTokenBucketRefill(t *testing.T) {
	clk := &mockClock{t: base()}
	tb := NewTokenBucket(5, 1)
	tb.now, tb.last, tb.tokens = clk.Now, clk.Now(), 0 // start empty

	if tb.Allow() {
		t.Fatal("empty bucket should deny")
	}
	clk.Advance(2 * time.Second) // refill 2 tokens
	if !tb.Allow() || !tb.Allow() {
		t.Fatal("after 2s at 1 token/sec, exactly 2 requests should be allowed")
	}
	if tb.Allow() {
		t.Fatal("only 2 tokens were refilled; 3rd should be denied")
	}
}

func TestTokenBucketRefillCapsAtCapacity(t *testing.T) {
	clk := &mockClock{t: base()}
	tb := NewTokenBucket(5, 1)
	tb.now, tb.last, tb.tokens = clk.Now, clk.Now(), 0

	clk.Advance(1 * time.Hour) // would be 3600 tokens, but cap is 5
	allowed := 0
	for i := 0; i < 100; i++ {
		if tb.Allow() {
			allowed++
		}
	}
	if allowed != 5 {
		t.Fatalf("refill must cap at capacity 5, got %d allowed", allowed)
	}
}

func TestTokenBucketConcurrent(t *testing.T) {
	// rate 0 => no refill, so exactly `capacity` requests may ever succeed,
	// regardless of scheduling. Run with -race to catch unsynchronized access.
	tb := NewTokenBucket(100, 0)

	var allowed int64
	var wg sync.WaitGroup
	for i := 0; i < 500; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			if tb.Allow() {
				atomic.AddInt64(&allowed, 1)
			}
		}()
	}
	wg.Wait()
	if allowed != 100 {
		t.Fatalf("with capacity 100 and no refill, exactly 100 must be allowed, got %d", allowed)
	}
}

// ---------- Fixed Window ----------

func TestFixedWindowBasic(t *testing.T) {
	clk := &mockClock{t: base()}
	fw := NewFixedWindow(3, time.Minute)
	fw.now, fw.windowStart = clk.Now, clk.Now().Truncate(time.Minute)

	for i := 0; i < 3; i++ {
		if !fw.Allow() {
			t.Fatalf("request %d should be allowed (limit 3)", i+1)
		}
	}
	if fw.Allow() {
		t.Fatal("4th request in the same window should be denied")
	}
	clk.Advance(time.Minute) // new window
	if !fw.Allow() {
		t.Fatal("after the window rolls over, requests should be allowed again")
	}
}

// Documents the well-known boundary bug: a client can send ~2x the limit across a
// window boundary in a very short span.
func TestFixedWindowBoundaryBug(t *testing.T) {
	clk := &mockClock{t: base().Add(59 * time.Second)} // near end of minute 0
	fw := NewFixedWindow(5, time.Minute)
	fw.now, fw.windowStart = clk.Now, clk.Now().Truncate(time.Minute)

	allowed := 0
	for i := 0; i < 5; i++ {
		if fw.Allow() {
			allowed++
		}
	}
	clk.Advance(2 * time.Second) // cross into minute 1
	for i := 0; i < 5; i++ {
		if fw.Allow() {
			allowed++
		}
	}
	if allowed != 10 {
		t.Fatalf("fixed window lets ~2x through at the boundary; expected 10 in ~3s, got %d", allowed)
	}
}

// ---------- Sliding Window Counter ----------

// The sliding window counter should NOT allow a full second burst right after the
// boundary, because the previous window still counts (weighted). This is the fix
// for the fixed-window boundary bug.
func TestSlidingWindowSmoothsBoundary(t *testing.T) {
	clk := &mockClock{t: base().Add(59 * time.Second)}
	s := NewSlidingWindowCounter(5, time.Minute)
	s.now, s.curWindow = clk.Now, clk.Now().Truncate(time.Minute)

	allowed := 0
	for i := 0; i < 5; i++ {
		if s.Allow() {
			allowed++
		}
	}
	// Cross 1 second into the next window. The previous window (5 used) is weighted
	// by ~(1 - 1/60) ≈ 0.983, so estimated ≈ 4.9 — essentially no new requests fit.
	clk.Advance(1 * time.Second)
	for i := 0; i < 5; i++ {
		if s.Allow() {
			allowed++
		}
	}
	if allowed >= 10 {
		t.Fatalf("sliding window must smooth the boundary; got %d (fixed window would give ~10)", allowed)
	}
	if allowed < 5 {
		t.Fatalf("the first 5 in the original window should have been allowed, got %d", allowed)
	}
}

func TestSlidingWindowRecoversAfterFullWindow(t *testing.T) {
	clk := &mockClock{t: base()}
	s := NewSlidingWindowCounter(5, time.Minute)
	s.now, s.curWindow = clk.Now, clk.Now().Truncate(time.Minute)

	for i := 0; i < 5; i++ {
		s.Allow()
	}
	clk.Advance(2 * time.Minute) // a full empty window passes; prev should reset to 0
	allowed := 0
	for i := 0; i < 5; i++ {
		if s.Allow() {
			allowed++
		}
	}
	if allowed != 5 {
		t.Fatalf("after a full idle window, all 5 should be allowed again, got %d", allowed)
	}
}
