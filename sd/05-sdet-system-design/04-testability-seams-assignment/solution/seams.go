// Package testability is the reference solution for Module 5.4.
//
// Points worth defending in an interview:
//
//   - "Slow and flaky tests" is usually a DESIGN defect, not a test-writing defect. A system
//     that calls time.Now directly cannot be tested without sleeping, and a test that sleeps is
//     both slow and flaky on loaded CI. The fix belongs in the system, not the test.
//
//   - Seeding randomness does not weaken a test. It converts "fails sometimes" into "fails with
//     seed 42", which is the difference between a bug you can fix and a bug you retry.
//
//   - The fault injector is DISABLED by default. A testing hook that can fire in production is
//     an outage waiting to happen; safe-by-default is non-negotiable.
//
//   - Latency is applied against the injected Clock. Simulating a 5s timeout must not cost 5
//     real seconds — that is the entire reason the Clock seam exists.
//
//   - ResetAll keeps going after a failure. Stopping at the first error leaves the rest of the
//     system dirty, and the NEXT test fails for an unrelated reason — the most expensive kind
//     of debugging there is.
package testability

import (
	"errors"
	"sort"
	"sync"
	"time"
)

// ErrInjected is returned by a FaultInjector when it decides to fail a call.
var ErrInjected = errors.New("testability: injected fault")

// Clock is the time seam.
type Clock interface {
	Now() time.Time
	After(d time.Duration) <-chan time.Time
}

// RealClock is the production implementation.
type RealClock struct{}

func (RealClock) Now() time.Time                         { return time.Now() }
func (RealClock) After(d time.Duration) <-chan time.Time { return time.After(d) }

// FakeClock is the test implementation: time only moves when a test moves it.
type FakeClock struct {
	mu      sync.Mutex
	now     time.Time
	waiters []fakeWaiter
}

type fakeWaiter struct {
	at time.Time
	ch chan time.Time
}

// NewFakeClock starts a fake clock at the given instant.
func NewFakeClock(start time.Time) *FakeClock {
	return &FakeClock{now: start}
}

// Now returns the fake clock's current time.
func (c *FakeClock) Now() time.Time {
	c.mu.Lock()
	defer c.mu.Unlock()
	return c.now
}

// After registers a waiter that fires when the clock advances past now+d.
func (c *FakeClock) After(d time.Duration) <-chan time.Time {
	c.mu.Lock()
	defer c.mu.Unlock()

	// Buffered: Advance must never block on a waiter nobody is reading.
	ch := make(chan time.Time, 1)
	if d <= 0 {
		ch <- c.now
		return ch
	}
	c.waiters = append(c.waiters, fakeWaiter{at: c.now.Add(d), ch: ch})
	return ch
}

// Advance moves the clock forward, firing every waiter whose deadline has passed.
func (c *FakeClock) Advance(d time.Duration) {
	c.mu.Lock()
	defer c.mu.Unlock()

	c.now = c.now.Add(d)

	var fired, pending []fakeWaiter
	for _, w := range c.waiters {
		if !w.at.After(c.now) {
			fired = append(fired, w)
		} else {
			pending = append(pending, w)
		}
	}
	c.waiters = pending

	// Deadline order: a test asserting "the retry fired before the timeout" depends on it.
	sort.Slice(fired, func(i, j int) bool { return fired[i].at.Before(fired[j].at) })
	for _, w := range fired {
		w.ch <- w.at // safe under the lock only because ch is buffered
	}
}

// Rand is the randomness seam.
type Rand interface {
	Float64() float64
	Intn(n int) int
}

// SeededRand is a small deterministic PRNG (xorshift64*).
type SeededRand struct {
	mu    sync.Mutex
	state uint64
}

// NewSeededRand creates a PRNG. A zero seed is replaced, since xorshift is stuck at 0.
func NewSeededRand(seed uint64) *SeededRand {
	if seed == 0 {
		seed = 0x9E3779B97F4A7C15
	}
	return &SeededRand{state: seed}
}

func (r *SeededRand) next() uint64 {
	r.mu.Lock()
	defer r.mu.Unlock()

	x := r.state
	x ^= x >> 12
	x ^= x << 25
	x ^= x >> 27
	r.state = x
	return x * 2685821657736338717
}

// Float64 returns a value in [0.0, 1.0).
func (r *SeededRand) Float64() float64 {
	// Top 53 bits: exactly the mantissa width of a float64, so the result is uniform.
	return float64(r.next()>>11) / float64(uint64(1)<<53)
}

// Intn returns a value in [0, n).
func (r *SeededRand) Intn(n int) int {
	if n <= 0 {
		panic("testability: Intn requires n > 0")
	}
	return int(r.next() % uint64(n))
}

// FaultPolicy describes how a named operation should misbehave.
type FaultPolicy struct {
	FailureRate float64
	Latency     time.Duration
	FailFirstN  int
}

// FaultInjector applies policies to named operations.
type FaultInjector struct {
	mu       sync.Mutex
	policies map[string]*FaultPolicy
	calls    map[string]int
	rand     Rand
	clock    Clock
	enabled  bool
}

// NewFaultInjector creates a DISABLED injector — production must be safe by default.
func NewFaultInjector(r Rand, c Clock) *FaultInjector {
	return &FaultInjector{
		policies: make(map[string]*FaultPolicy),
		calls:    make(map[string]int),
		rand:     r,
		clock:    c,
	}
}

func (f *FaultInjector) Enable()  { f.mu.Lock(); f.enabled = true; f.mu.Unlock() }
func (f *FaultInjector) Disable() { f.mu.Lock(); f.enabled = false; f.mu.Unlock() }

// SetPolicy registers (or replaces) the policy for an operation and resets its call count.
func (f *FaultInjector) SetPolicy(op string, p FaultPolicy) {
	f.mu.Lock()
	defer f.mu.Unlock()

	copied := p
	f.policies[op] = &copied
	f.calls[op] = 0 // a fresh policy means a fresh test; stale counts would leak between them
}

// Maybe applies the policy for op.
func (f *FaultInjector) Maybe(op string) error {
	f.mu.Lock()

	if !f.enabled {
		f.mu.Unlock()
		return nil
	}
	p, ok := f.policies[op]
	if !ok {
		f.mu.Unlock()
		return nil // no policy: pass through untouched and uncounted
	}

	f.calls[op]++
	count := f.calls[op]

	failFirst := count <= p.FailFirstN
	var roll float64
	if !failFirst {
		roll = f.rand.Float64()
	}
	rate, latency := p.FailureRate, p.Latency

	// Release BEFORE waiting: holding the lock across the delay would serialise every
	// other caller behind this one and deadlock the test that drives the clock.
	f.mu.Unlock()

	if latency > 0 {
		<-f.clock.After(latency)
	}
	if failFirst || roll < rate {
		return ErrInjected
	}
	return nil
}

// CallCount reports how many times an operation passed through a policy.
func (f *FaultInjector) CallCount(op string) int {
	f.mu.Lock()
	defer f.mu.Unlock()
	return f.calls[op]
}

// Resettable is anything that can return itself to a known-clean state.
type Resettable interface {
	Reset() error
}

// ResetFunc adapts a plain function to Resettable.
type ResetFunc func() error

func (f ResetFunc) Reset() error { return f() }

// Registry holds reset hooks in registration order.
type Registry struct {
	mu      sync.Mutex
	names   []string
	targets map[string]Resettable
}

// NewRegistry creates an empty registry.
func NewRegistry() *Registry {
	return &Registry{targets: make(map[string]Resettable)}
}

// Register adds a resettable under a name, replacing in place if it already exists.
func (r *Registry) Register(name string, target Resettable) {
	r.mu.Lock()
	defer r.mu.Unlock()

	if _, exists := r.targets[name]; !exists {
		r.names = append(r.names, name)
	}
	r.targets[name] = target
}

// ResetAll resets every registered target in registration order.
func (r *Registry) ResetAll() error {
	r.mu.Lock()
	names := make([]string, len(r.names))
	copy(names, r.names)
	targets := make(map[string]Resettable, len(r.targets))
	for k, v := range r.targets {
		targets[k] = v
	}
	r.mu.Unlock()

	var errs []error
	for _, name := range names {
		// Keep going on failure: a half-reset system makes the NEXT test fail for an
		// unrelated reason, which is the most expensive kind of debugging.
		if err := targets[name].Reset(); err != nil {
			errs = append(errs, err)
		}
	}
	return errors.Join(errs...)
}

// Names returns registered names in registration order.
func (r *Registry) Names() []string {
	r.mu.Lock()
	defer r.mu.Unlock()

	out := make([]string, len(r.names))
	copy(out, r.names)
	return out
}
