// Package testability is the Module 5.4 assignment: build the seams that make a system
// testable without sleeping, without flaking, and without a full environment.
//
// Read 05-sdet-system-design/04-design-for-testability.md first.
//
// Fill in every function marked `// TODO`. Run the tests until green:
//
//	go test ./...
//	go test -race ./...
//
// The framing: "Our tests are slow and flaky." Almost always the real answer is not "write
// better tests" — it is that the system under test has no seams. It calls time.Now directly,
// it seeds randomness from the clock, it has no way to simulate a downstream failure, and it
// leaks state between runs. Every one of those is a DESIGN defect that shows up as a TEST
// problem, which is exactly the argument a test architect gets paid to make.
//
// The four seams here are the ones that pay for themselves fastest:
//  1. Clock       - kills Thread.sleep and time-dependent flakes
//  2. Rand        - makes "random" reproducible from a seed
//  3. FaultInjector - lets you test the unhappy path without breaking a real dependency
//  4. Resettable  - makes state leakage between tests impossible rather than merely unlikely
package testability

import (
	"errors"
	"sort"
	"sync"
	"time"
)

// ErrInjected is returned by a FaultInjector when it decides to fail a call.
var ErrInjected = errors.New("testability: injected fault")

// ----------------------------------------------------------------------------
// 1) Clock
//
// Production passes a real clock; tests pass a fake one they can drive. The point is not
// "avoid time.Now" for its own sake — it is that a test which sleeps 2 seconds to wait for a
// timeout is both slow AND flaky, because 2 seconds is never quite enough on a loaded CI box.
// ----------------------------------------------------------------------------

// Clock is the time seam.
type Clock interface {
	Now() time.Time
	// After returns a channel that fires once the duration has elapsed on THIS clock.
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
	// TODO: lock and return c.now.
	return time.Time{}
}

// After registers a waiter that fires when the clock advances past now+d.
// A non-positive duration must fire immediately.
func (c *FakeClock) After(d time.Duration) <-chan time.Time {
	// TODO:
	//  1. Lock. Compute deadline = c.now.Add(d).
	//  2. Make a BUFFERED channel (capacity 1) so firing never blocks Advance.
	//  3. If d <= 0, send c.now immediately and return the channel.
	//  4. Otherwise append a fakeWaiter and return the channel.
	return nil
}

// Advance moves the clock forward, firing every waiter whose deadline has passed.
// Waiters must fire in deadline order — a test that asserts "the retry fired before the
// timeout" depends on it.
func (c *FakeClock) Advance(d time.Duration) {
	// TODO:
	//  1. Lock. Advance c.now by d.
	//  2. Collect waiters whose deadline is <= the new now; keep the rest.
	//  3. Sort the fired waiters by deadline ascending.
	//  4. Send the deadline time on each channel, then release the lock.
	//     (Send while holding the lock is safe here only because the channels are buffered.)
}

// ----------------------------------------------------------------------------
// 2) Deterministic randomness
//
// "It only fails sometimes" is usually unseeded randomness. Seeding it does not make the test
// weaker — it makes a failure REPRODUCIBLE, which is the whole difference between a bug you
// can fix and a bug you retry.
// ----------------------------------------------------------------------------

// Rand is the randomness seam.
type Rand interface {
	// Float64 returns a value in [0.0, 1.0).
	Float64() float64
	// Intn returns a value in [0, n).
	Intn(n int) int
}

// SeededRand is a small deterministic PRNG (xorshift64*). Deliberately not crypto — the point
// is reproducibility, and depending on math/rand's global state would defeat that.
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

// next advances the xorshift64* state and returns the raw value.
func (r *SeededRand) next() uint64 {
	// TODO: implement xorshift64*, under the lock:
	//   x := r.state
	//   x ^= x >> 12
	//   x ^= x << 25
	//   x ^= x >> 27
	//   r.state = x
	//   return x * 2685821657736338717
	return 0
}

// Float64 returns a value in [0.0, 1.0).
func (r *SeededRand) Float64() float64 {
	// TODO: use the top 53 bits: float64(next()>>11) / (1 << 53).
	return 0
}

// Intn returns a value in [0, n). Panics if n <= 0, matching math/rand.
func (r *SeededRand) Intn(n int) int {
	// TODO
	return 0
}

// ----------------------------------------------------------------------------
// 3) Fault injection
//
// You cannot test the retry path if you cannot make the dependency fail. Injecting the failure
// at a seam beats unplugging a real database, and it beats a mock that drifts from reality.
// ----------------------------------------------------------------------------

// FaultPolicy describes how a named operation should misbehave.
type FaultPolicy struct {
	// FailureRate is the probability of returning ErrInjected, 0.0–1.0.
	FailureRate float64
	// Latency is added delay before returning, applied against the injector's Clock.
	Latency time.Duration
	// FailFirstN fails the first N calls unconditionally, then behaves per FailureRate.
	// This is what lets you test "succeeds on the 3rd retry" deterministically.
	FailFirstN int
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
// Nothing is injected until Enable is called.
func NewFaultInjector(r Rand, c Clock) *FaultInjector {
	return &FaultInjector{
		policies: make(map[string]*FaultPolicy),
		calls:    make(map[string]int),
		rand:     r,
		clock:    c,
	}
}

// Enable turns injection on. Disable turns it off without discarding policies.
func (f *FaultInjector) Enable()  { f.mu.Lock(); f.enabled = true; f.mu.Unlock() }
func (f *FaultInjector) Disable() { f.mu.Lock(); f.enabled = false; f.mu.Unlock() }

// SetPolicy registers (or replaces) the policy for an operation and resets its call count.
func (f *FaultInjector) SetPolicy(op string, p FaultPolicy) {
	// TODO
}

// Maybe applies the policy for op. Returns ErrInjected when the call should fail, nil otherwise.
// Ordering rules:
//   - Disabled injector, or no policy for op -> always nil, and the call is NOT counted.
//   - Otherwise increment the call count FIRST, then decide.
//   - FailFirstN takes precedence: while callCount <= FailFirstN, fail.
//   - Then FailureRate: fail when rand.Float64() < FailureRate.
//   - Latency applies on the paths that are reached (see the tests for exact expectations):
//     it is applied before returning, whether failing or succeeding.
func (f *FaultInjector) Maybe(op string) error {
	// TODO
	return nil
}

// CallCount reports how many times an operation passed through a policy.
func (f *FaultInjector) CallCount(op string) int {
	// TODO
	return 0
}

// ----------------------------------------------------------------------------
// 4) Reset registry
//
// Test isolation by construction. Every stateful component registers a reset hook; the harness
// calls ResetAll between tests. Order matters — components are reset in registration order so
// a cache is cleared before the store it fronts.
// ----------------------------------------------------------------------------

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

// Register adds a resettable under a name. Re-registering the same name REPLACES the target
// but keeps its original position, so reset order stays stable across test runs.
func (r *Registry) Register(name string, target Resettable) {
	// TODO
}

// ResetAll resets every registered target in registration order.
// It must attempt EVERY target even if one fails — a harness that stops at the first error
// leaves the rest of the system dirty, and the next test fails for an unrelated reason.
// Returns the joined errors, or nil if all succeeded.
func (r *Registry) ResetAll() error {
	// TODO: collect errors from every target; return errors.Join(errs...) (nil when empty).
	return nil
}

// Names returns registered names in registration order.
func (r *Registry) Names() []string {
	// TODO
	return nil
}

var _ = sort.Slice
