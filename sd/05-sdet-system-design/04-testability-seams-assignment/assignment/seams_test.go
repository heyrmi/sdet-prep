package testability

import (
	"errors"
	"sync"
	"testing"
	"time"
)

func base() time.Time { return time.Date(2026, 1, 1, 0, 0, 0, 0, time.UTC) }

// ---------- 1) Clock ----------

func TestFakeClockDoesNotMoveOnItsOwn(t *testing.T) {
	c := NewFakeClock(base())
	first := c.Now()
	time.Sleep(5 * time.Millisecond) // real time passes
	if !c.Now().Equal(first) {
		t.Fatal("a fake clock must only move when a test moves it")
	}
}

func TestFakeClockAdvance(t *testing.T) {
	c := NewFakeClock(base())
	c.Advance(90 * time.Minute)
	if want := base().Add(90 * time.Minute); !c.Now().Equal(want) {
		t.Fatalf("want %v, got %v", want, c.Now())
	}
}

func TestFakeClockAfterFiresOnAdvance(t *testing.T) {
	c := NewFakeClock(base())
	ch := c.After(30 * time.Second)

	select {
	case <-ch:
		t.Fatal("must not fire before the clock advances")
	default:
	}

	c.Advance(30 * time.Second)
	select {
	case got := <-ch:
		if !got.Equal(base().Add(30 * time.Second)) {
			t.Fatalf("want the deadline instant, got %v", got)
		}
	default:
		t.Fatal("must fire once the clock reaches the deadline — this is what replaces a real sleep")
	}
}

func TestFakeClockAfterNonPositiveFiresImmediately(t *testing.T) {
	c := NewFakeClock(base())
	select {
	case <-c.After(0):
	default:
		t.Fatal("a zero duration must fire immediately")
	}
}

func TestFakeClockPartialAdvanceDoesNotFire(t *testing.T) {
	c := NewFakeClock(base())
	ch := c.After(time.Minute)
	c.Advance(59 * time.Second)
	select {
	case <-ch:
		t.Fatal("59s < 60s, must not fire yet")
	default:
	}
	c.Advance(time.Second)
	select {
	case <-ch:
	default:
		t.Fatal("should fire at exactly the deadline")
	}
}

func TestFakeClockFiresInDeadlineOrder(t *testing.T) {
	c := NewFakeClock(base())
	// Registered out of order on purpose.
	late := c.After(30 * time.Second)
	early := c.After(10 * time.Second)
	mid := c.After(20 * time.Second)

	c.Advance(time.Minute)

	var order []time.Duration
	for _, ch := range []<-chan time.Time{early, mid, late} {
		select {
		case got := <-ch:
			order = append(order, got.Sub(base()))
		default:
			t.Fatal("all three should have fired")
		}
	}
	for i := 1; i < len(order); i++ {
		if order[i] < order[i-1] {
			t.Fatalf("waiters must fire in deadline order, got %v", order)
		}
	}
}

func TestFakeClockAdvanceDoesNotDeadlock(t *testing.T) {
	// Nobody is reading the channel. Advance must not block — hence buffered channels.
	c := NewFakeClock(base())
	for i := 0; i < 100; i++ {
		c.After(time.Second)
	}
	done := make(chan struct{})
	go func() { c.Advance(2 * time.Second); close(done) }()
	select {
	case <-done:
	case <-time.After(2 * time.Second):
		t.Fatal("Advance blocked with no readers — the waiter channels must be buffered")
	}
}

// ---------- 2) Deterministic randomness ----------

func TestSeededRandIsReproducible(t *testing.T) {
	a, b := NewSeededRand(42), NewSeededRand(42)
	for i := 0; i < 100; i++ {
		if a.Float64() != b.Float64() {
			t.Fatalf("same seed must produce the same sequence (diverged at %d) — "+
				"otherwise a failure is not reproducible", i)
		}
	}
}

func TestSeededRandDiffersBySeed(t *testing.T) {
	a, b := NewSeededRand(1), NewSeededRand(2)
	same := 0
	for i := 0; i < 50; i++ {
		if a.Float64() == b.Float64() {
			same++
		}
	}
	if same > 2 {
		t.Fatalf("different seeds should produce different sequences, %d/50 matched", same)
	}
}

func TestSeededRandFloat64Range(t *testing.T) {
	r := NewSeededRand(7)
	for i := 0; i < 1000; i++ {
		v := r.Float64()
		if v < 0 || v >= 1 {
			t.Fatalf("Float64 must be in [0,1), got %v", v)
		}
	}
}

func TestSeededRandIntnRange(t *testing.T) {
	r := NewSeededRand(7)
	for i := 0; i < 1000; i++ {
		v := r.Intn(10)
		if v < 0 || v >= 10 {
			t.Fatalf("Intn(10) must be in [0,10), got %v", v)
		}
	}
}

func TestSeededRandZeroSeedStillWorks(t *testing.T) {
	r := NewSeededRand(0)
	if a, b := r.Float64(), r.Float64(); a == 0 && b == 0 {
		t.Fatal("a zero seed must be replaced — xorshift is stuck at zero")
	}
}

// ---------- 3) Fault injection ----------

func TestInjectorDisabledByDefault(t *testing.T) {
	f := NewFaultInjector(NewSeededRand(1), NewFakeClock(base()))
	f.SetPolicy("db.read", FaultPolicy{FailureRate: 1.0})

	if err := f.Maybe("db.read"); err != nil {
		t.Fatalf("a disabled injector must never inject — production must be safe by default; got %v", err)
	}
}

func TestInjectorAlwaysFailAtRateOne(t *testing.T) {
	f := NewFaultInjector(NewSeededRand(1), NewFakeClock(base()))
	f.SetPolicy("db.read", FaultPolicy{FailureRate: 1.0})
	f.Enable()

	for i := 0; i < 20; i++ {
		if err := f.Maybe("db.read"); !errors.Is(err, ErrInjected) {
			t.Fatalf("rate 1.0 must always fail, call %d got %v", i, err)
		}
	}
}

func TestInjectorNeverFailsAtRateZero(t *testing.T) {
	f := NewFaultInjector(NewSeededRand(1), NewFakeClock(base()))
	f.SetPolicy("db.read", FaultPolicy{FailureRate: 0})
	f.Enable()

	for i := 0; i < 20; i++ {
		if err := f.Maybe("db.read"); err != nil {
			t.Fatalf("rate 0 must never fail, call %d got %v", i, err)
		}
	}
}

func TestInjectorUnregisteredOpIsUntouched(t *testing.T) {
	f := NewFaultInjector(NewSeededRand(1), NewFakeClock(base()))
	f.SetPolicy("db.read", FaultPolicy{FailureRate: 1.0})
	f.Enable()

	if err := f.Maybe("cache.get"); err != nil {
		t.Fatalf("an op with no policy must pass through, got %v", err)
	}
	if n := f.CallCount("cache.get"); n != 0 {
		t.Fatalf("a pass-through call must not be counted, got %d", n)
	}
}

func TestFailFirstNIsDeterministic(t *testing.T) {
	// This is what makes "succeeds on the 3rd attempt" a real test instead of a hope.
	f := NewFaultInjector(NewSeededRand(1), NewFakeClock(base()))
	f.SetPolicy("api.call", FaultPolicy{FailFirstN: 2})
	f.Enable()

	if err := f.Maybe("api.call"); !errors.Is(err, ErrInjected) {
		t.Fatalf("call 1 must fail, got %v", err)
	}
	if err := f.Maybe("api.call"); !errors.Is(err, ErrInjected) {
		t.Fatalf("call 2 must fail, got %v", err)
	}
	if err := f.Maybe("api.call"); err != nil {
		t.Fatalf("call 3 must succeed, got %v", err)
	}
}

func TestSetPolicyResetsCallCount(t *testing.T) {
	f := NewFaultInjector(NewSeededRand(1), NewFakeClock(base()))
	f.Enable()
	f.SetPolicy("api.call", FaultPolicy{FailFirstN: 1})

	f.Maybe("api.call")
	f.Maybe("api.call")
	if n := f.CallCount("api.call"); n != 2 {
		t.Fatalf("want 2 calls, got %d", n)
	}

	f.SetPolicy("api.call", FaultPolicy{FailFirstN: 1})
	if n := f.CallCount("api.call"); n != 0 {
		t.Fatalf("re-setting a policy must reset the counter so tests start clean, got %d", n)
	}
	if err := f.Maybe("api.call"); !errors.Is(err, ErrInjected) {
		t.Fatal("after the reset, call 1 must fail again")
	}
}

func TestLatencyUsesTheClockSeam(t *testing.T) {
	// Latency must be applied against the injected clock, not a real sleep — otherwise
	// "simulate a 5s timeout" costs 5 real seconds in CI.
	c := NewFakeClock(base())
	f := NewFaultInjector(NewSeededRand(1), c)
	f.SetPolicy("slow.op", FaultPolicy{Latency: 5 * time.Second})
	f.Enable()

	done := make(chan error, 1)
	go func() { done <- f.Maybe("slow.op") }()

	// Give the goroutine a moment to register its waiter, then release it.
	deadline := time.After(2 * time.Second)
	for {
		select {
		case err := <-done:
			if err != nil {
				t.Fatalf("no failure configured, got %v", err)
			}
			return
		case <-deadline:
			t.Fatal("Maybe did not return — latency must wait on the injected Clock, not time.Sleep")
		default:
			c.Advance(5 * time.Second)
			time.Sleep(time.Millisecond)
		}
	}
}

func TestInjectorIsRaceFree(t *testing.T) {
	f := NewFaultInjector(NewSeededRand(9), NewFakeClock(base()))
	f.SetPolicy("op", FaultPolicy{FailureRate: 0.5})
	f.Enable()

	var wg sync.WaitGroup
	for w := 0; w < 8; w++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for i := 0; i < 200; i++ {
				_ = f.Maybe("op")
				_ = f.CallCount("op")
			}
		}()
	}
	wg.Wait()
}

// ---------- 4) Reset registry ----------

type counter struct{ resets int }

func (c *counter) Reset() error { c.resets++; return nil }

func TestResetAllCallsEveryTarget(t *testing.T) {
	r := NewRegistry()
	a, b := &counter{}, &counter{}
	r.Register("cache", a)
	r.Register("store", b)

	if err := r.ResetAll(); err != nil {
		t.Fatalf("want no error, got %v", err)
	}
	if a.resets != 1 || b.resets != 1 {
		t.Fatalf("every target must be reset once, got %d and %d", a.resets, b.resets)
	}
}

func TestResetOrderIsRegistrationOrder(t *testing.T) {
	r := NewRegistry()
	var order []string
	for _, name := range []string{"cache", "store", "queue"} {
		n := name
		r.Register(n, ResetFunc(func() error { order = append(order, n); return nil }))
	}
	r.ResetAll()

	want := []string{"cache", "store", "queue"}
	for i := range want {
		if i >= len(order) || order[i] != want[i] {
			t.Fatalf("reset order must be registration order (a cache is cleared before the "+
				"store it fronts); want %v got %v", want, order)
		}
	}
}

func TestResetAllContinuesAfterFailure(t *testing.T) {
	r := NewRegistry()
	boom := errors.New("boom")
	after := &counter{}

	r.Register("bad", ResetFunc(func() error { return boom }))
	r.Register("good", after)

	err := r.ResetAll()
	if err == nil {
		t.Fatal("the failure must be reported")
	}
	if !errors.Is(err, boom) {
		t.Fatalf("the original error must be retrievable, got %v", err)
	}
	if after.resets != 1 {
		t.Fatal("a failing hook must not stop the rest — the next test would fail for an " +
			"unrelated reason and cost an hour of debugging")
	}
}

func TestReRegisterKeepsPosition(t *testing.T) {
	r := NewRegistry()
	r.Register("a", &counter{})
	r.Register("b", &counter{})
	replacement := &counter{}
	r.Register("a", replacement) // replace, do not append

	names := r.Names()
	if len(names) != 2 || names[0] != "a" || names[1] != "b" {
		t.Fatalf("re-registering must replace in place, not duplicate or reorder; got %v", names)
	}
	r.ResetAll()
	if replacement.resets != 1 {
		t.Fatal("the replacement target must be the one that gets reset")
	}
}

func TestResetAllEmpty(t *testing.T) {
	if err := NewRegistry().ResetAll(); err != nil {
		t.Fatalf("an empty registry must succeed, got %v", err)
	}
}
