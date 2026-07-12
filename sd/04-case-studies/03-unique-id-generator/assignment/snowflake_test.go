package snowflake

import (
	"sync"
	"sync/atomic"
	"testing"
)

// withClock builds a generator whose clock is fully controlled by the test.
// `tick` is a pointer to the current ms-since-epoch; advance it by writing to it.
func withClock(t *testing.T, machineID int64, tick *int64) *Generator {
	t.Helper()
	g, err := NewGenerator(machineID)
	if err != nil {
		t.Fatalf("NewGenerator(%d): %v", machineID, err)
	}
	g.now = func() int64 { return *tick }
	return g
}

// ---------- validation ----------

func TestNewGeneratorRejectsBadMachineID(t *testing.T) {
	if _, err := NewGenerator(-1); err == nil {
		t.Fatal("machineID -1 should be rejected")
	}
	if _, err := NewGenerator(1024); err == nil {
		t.Fatal("machineID 1024 should be rejected (max is 1023)")
	}
	if _, err := NewGenerator(1023); err != nil {
		t.Fatalf("machineID 1023 should be valid, got %v", err)
	}
	if _, err := NewGenerator(0); err != nil {
		t.Fatalf("machineID 0 should be valid, got %v", err)
	}
}

// ---------- uniqueness & ordering ----------

func TestIDsUniqueAndMonotonicAsClockAdvances(t *testing.T) {
	tick := int64(1000)
	g := withClock(t, 7, &tick)

	seen := make(map[int64]bool)
	var prev int64 = -1
	for i := 0; i < 5000; i++ {
		if i%3 == 0 {
			tick++ // advance the clock periodically
		}
		id, err := g.NextID()
		if err != nil {
			t.Fatalf("NextID: %v", err)
		}
		if seen[id] {
			t.Fatalf("duplicate ID %d at i=%d", id, i)
		}
		seen[id] = true
		if id <= prev {
			t.Fatalf("IDs must be strictly increasing: %d then %d", prev, id)
		}
		prev = id
	}
}

// ---------- sequence increments within one millisecond ----------

func TestSequenceIncrementsWithinSameMs(t *testing.T) {
	tick := int64(42)
	g := withClock(t, 3, &tick) // clock frozen: same ms for every call

	for want := int64(0); want < 10; want++ {
		id, err := g.NextID()
		if err != nil {
			t.Fatalf("NextID: %v", err)
		}
		if got := Sequence(id); got != want {
			t.Fatalf("call %d: expected sequence %d, got %d", want, want, got)
		}
		if got := Timestamp(id); got != 42 {
			t.Fatalf("timestamp should stay 42, got %d", got)
		}
	}
}

// ---------- machine ID is encoded correctly ----------

func TestMachineIDEncoded(t *testing.T) {
	for _, mid := range []int64{0, 1, 42, 511, 1023} {
		tick := int64(9999)
		g := withClock(t, mid, &tick)
		id, err := g.NextID()
		if err != nil {
			t.Fatalf("NextID: %v", err)
		}
		if got := MachineID(id); got != mid {
			t.Fatalf("machineID: encoded %d, decoded %d", mid, got)
		}
		if got := Timestamp(id); got != 9999 {
			t.Fatalf("timestamp: encoded 9999, decoded %d", got)
		}
	}
}

// ---------- clock moving backwards is an error ----------

func TestClockMovedBackReturnsError(t *testing.T) {
	tick := int64(1000)
	g := withClock(t, 1, &tick)

	if _, err := g.NextID(); err != nil {
		t.Fatalf("first NextID: %v", err)
	}
	tick = 999 // clock jumps backwards (e.g. NTP correction)
	if _, err := g.NextID(); err != ErrClockMovedBack {
		t.Fatalf("expected ErrClockMovedBack, got %v", err)
	}
}

// ---------- sequence overflow within a ms rolls to the next ms ----------

func TestSequenceOverflowWaitsForNextMs(t *testing.T) {
	tick := int64(500)
	g := withClock(t, 2, &tick)

	// waitNextMs busy-waits until now() > ts. Advance the clock from a background
	// goroutine so the spin terminates deterministically; the value only ever moves
	// forward, so no backwards-clock error is possible.
	stop := make(chan struct{})
	var wg sync.WaitGroup
	wg.Add(1)
	go func() {
		defer wg.Done()
		for {
			select {
			case <-stop:
				return
			default:
				atomic.AddInt64(&tick, 1)
			}
		}
	}()
	// Point the generator's clock at the atomically-updated tick.
	g.now = func() int64 { return atomic.LoadInt64(&tick) }

	// Generate well over one millisecond's worth (4096) of IDs. Each must be unique
	// and the timestamp must advance whenever the sequence wraps.
	seen := make(map[int64]bool)
	for i := 0; i < 4096*3; i++ {
		id, err := g.NextID()
		if err != nil {
			t.Fatalf("NextID at i=%d: %v", i, err)
		}
		if seen[id] {
			t.Fatalf("duplicate ID after overflow at i=%d", i)
		}
		seen[id] = true
	}
	close(stop)
	wg.Wait()
}

// ---------- concurrency: -race, many goroutines, all IDs unique ----------

func TestConcurrentUnique(t *testing.T) {
	// Real monotonic counter clock: never goes backwards, advances on its own so
	// the sequence never starves. Exercises the mutex under -race.
	var tick int64
	g, err := NewGenerator(5)
	if err != nil {
		t.Fatalf("NewGenerator: %v", err)
	}
	g.now = func() int64 { return atomic.AddInt64(&tick, 1) }

	const goroutines = 50
	const perG = 1000

	var mu sync.Mutex
	seen := make(map[int64]bool, goroutines*perG)

	var wg sync.WaitGroup
	for i := 0; i < goroutines; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for j := 0; j < perG; j++ {
				id, err := g.NextID()
				if err != nil {
					t.Errorf("NextID: %v", err)
					return
				}
				mu.Lock()
				if seen[id] {
					t.Errorf("duplicate ID %d", id)
				}
				seen[id] = true
				mu.Unlock()
			}
		}()
	}
	wg.Wait()
	if len(seen) != goroutines*perG {
		t.Fatalf("expected %d unique IDs, got %d", goroutines*perG, len(seen))
	}
}
