// Package devicepool is the Module 5.3 assignment: the allocator behind a Selenium Grid /
// device farm.
//
// Read 05-sdet-system-design/03-design-test-infrastructure-at-scale.md first.
//
// Fill in every function marked `// TODO`. Run the tests until green:
//
//	go test ./...
//	go test -race ./...   // many CI workers lease concurrently
//
// The framing: "2,000 concurrent browser sessions across Chrome/Firefox/Safari and 40 physical
// devices. Sessions leak, nodes wedge, and one team's 5,000-test suite starves everyone else."
// Anyone can write a free-list. What gets asked about is leases that survive a dead client,
// health checks that eject bad nodes, and fairness that stops one tenant eating the pool.
package devicepool

import (
	"errors"
	"sort"
	"sync"
	"time"
)

var (
	// ErrNoCapacity means no healthy node matches the request right now.
	ErrNoCapacity = errors.New("devicepool: no matching capacity available")
	// ErrUnknownLease means the lease ID was never issued or has already been released.
	ErrUnknownLease = errors.New("devicepool: unknown lease")
)

// Node is one machine or device that can host sessions.
type Node struct {
	ID string
	// Capability describes what it offers, e.g. "chrome", "safari", "pixel-8".
	Capability string
	// Slots is how many concurrent sessions it can host.
	Slots int
}

// Lease is one granted allocation.
type Lease struct {
	ID       string
	NodeID   string
	Tenant   string
	Acquired time.Time
	// ExpiresAt is when the lease is reclaimed if not renewed. This is the mechanism that
	// makes a crashed client survivable: no heartbeat, no lease, capacity returns.
	ExpiresAt time.Time
}

// nodeState is the pool's internal bookkeeping for one node.
type nodeState struct {
	node Node
	// inUse is the number of currently-held leases on this node.
	inUse int
	// consecutiveFailures drives health ejection.
	consecutiveFailures int
	// healthy is false once the node is ejected; it stops receiving new leases.
	healthy bool
}

// Pool allocates nodes to tenants under lease.
type Pool struct {
	mu     sync.Mutex
	nodes  map[string]*nodeState
	leases map[string]*Lease
	// perTenant counts active leases per tenant, for fairness.
	perTenant map[string]int

	leaseTTL time.Duration
	// maxSharePerTenant caps the fraction of TOTAL pool slots one tenant may hold (0.0–1.0).
	// Without this, one team's 5,000-test suite starves everyone else — and the starved teams
	// stop trusting CI, which is how a shared platform dies.
	maxSharePerTenant float64
	// failureThreshold is how many consecutive failures eject a node.
	failureThreshold int

	now func() time.Time
	seq int // monotonic counter for deterministic lease IDs
}

// NewPool builds a pool from a node list.
func NewPool(nodes []Node, leaseTTL time.Duration, maxSharePerTenant float64, failureThreshold int) *Pool {
	p := &Pool{
		nodes:             make(map[string]*nodeState, len(nodes)),
		leases:            make(map[string]*Lease),
		perTenant:         make(map[string]int),
		leaseTTL:          leaseTTL,
		maxSharePerTenant: maxSharePerTenant,
		failureThreshold:  failureThreshold,
		now:               time.Now,
	}
	for _, n := range nodes {
		p.nodes[n.ID] = &nodeState{node: n, healthy: true}
	}
	return p
}

// TotalSlots is the pool's full capacity across healthy AND unhealthy nodes.
func (p *Pool) TotalSlots() int {
	p.mu.Lock()
	defer p.mu.Unlock()
	return p.totalSlotsLocked()
}

func (p *Pool) totalSlotsLocked() int {
	total := 0
	for _, ns := range p.nodes {
		total += ns.node.Slots
	}
	return total
}

// ----------------------------------------------------------------------------
// 1) Acquire / Release
//
// Rules:
//   - Only HEALTHY nodes with a free slot and the requested capability are candidates.
//   - Choose the candidate with the FEWEST leases in use (spread load, don't stack one node);
//     tie-break on node ID ascending so the result is deterministic.
//   - Enforce the per-tenant share cap BEFORE allocating.
//   - Expired leases must be reclaimed before deciding there is no capacity — otherwise a
//     crashed client permanently shrinks the pool.
// ----------------------------------------------------------------------------

// Acquire grants a lease on a node matching capability, or ErrNoCapacity.
func (p *Pool) Acquire(tenant, capability string) (*Lease, error) {
	// TODO:
	//  1. Lock. Reclaim expired leases first (see reclaimExpiredLocked).
	//  2. Enforce fairness: if perTenant[tenant] >= floor(totalSlots * maxSharePerTenant),
	//     return ErrNoCapacity. Treat a cap that computes to 0 as 1, so a tiny pool is usable.
	//  3. Find healthy nodes with matching Capability and inUse < Slots.
	//  4. Pick the one with the lowest inUse; tie-break lowest ID.
	//  5. Build a Lease (use p.nextLeaseIDLocked()), bump counters, store it, return it.
	return nil, ErrNoCapacity
}

// Release returns a lease's capacity to the pool.
func (p *Pool) Release(leaseID string) error {
	// TODO: look up the lease, decrement node inUse and perTenant, delete the lease.
	//       Unknown or already-released -> ErrUnknownLease.
	return ErrUnknownLease
}

// Renew extends a lease by the pool's TTL. This is the client heartbeat.
func (p *Pool) Renew(leaseID string) error {
	// TODO: extend ExpiresAt to now+leaseTTL. Unknown -> ErrUnknownLease.
	//       An ALREADY-EXPIRED lease must not be renewable — its capacity may already be
	//       reclaimed and handed to someone else.
	return ErrUnknownLease
}

// reclaimExpiredLocked frees every lease past its expiry. Called with the lock held.
func (p *Pool) reclaimExpiredLocked() int {
	// TODO: delete expired leases, decrement their node's inUse and the tenant count.
	//       Return how many were reclaimed.
	return 0
}

// nextLeaseIDLocked produces a deterministic lease ID. Called with the lock held.
func (p *Pool) nextLeaseIDLocked() string {
	p.seq++
	return "lease-" + itoa(p.seq)
}

// ----------------------------------------------------------------------------
// 2) Health
//
// A wedged node that still accepts sessions is worse than a dead one: every test routed to it
// fails, and the failures look like product bugs. Eject after N consecutive failures; one
// success resets the counter.
// ----------------------------------------------------------------------------

// ReportResult records a session outcome on a node, ejecting it after failureThreshold
// consecutive failures. Returns true if the node is healthy afterwards.
func (p *Pool) ReportResult(nodeID string, ok bool) bool {
	// TODO:
	//  - ok:  reset consecutiveFailures to 0, mark healthy (a recovered node rejoins).
	//  - !ok: increment; if >= failureThreshold, set healthy = false.
	//  - Unknown node -> return false.
	return false
}

// HealthyNodes returns the IDs of healthy nodes, sorted.
func (p *Pool) HealthyNodes() []string {
	// TODO
	return nil
}

// ----------------------------------------------------------------------------
// 3) Autoscaling
//
// The question a platform owner is actually asked: "how many nodes do we need?" Answer from
// queue depth and utilisation, not vibes.
// ----------------------------------------------------------------------------

// ScaleDecision is a recommended capacity change.
type ScaleDecision struct {
	// Delta is nodes to add (positive) or remove (negative).
	Delta int
	Reason string
}

// RecommendScale suggests a capacity change for one capability.
//
//	queueDepth  - sessions currently waiting for that capability
//	slotsPerNode- slots a new node of this type provides
//
// Rules, in order:
//  1. queueDepth > 0        -> scale UP by ceil(queueDepth / slotsPerNode).
//  2. utilisation < 0.25 and there are at least 2 healthy nodes of this capability
//     -> scale DOWN by 1 (never below 1 node; never remove more than one at a time,
//     because thrashing capacity is worse than briefly over-providing).
//  3. otherwise            -> Delta 0.
//
// Utilisation is inUse/Slots across healthy nodes of that capability.
func (p *Pool) RecommendScale(capability string, queueDepth, slotsPerNode int) ScaleDecision {
	// TODO
	return ScaleDecision{}
}

// itoa is a tiny helper so the package has no fmt dependency in the hot path.
func itoa(n int) string {
	if n == 0 {
		return "0"
	}
	var buf [20]byte
	i := len(buf)
	for n > 0 {
		i--
		buf[i] = byte('0' + n%10)
		n /= 10
	}
	return string(buf[i:])
}

var _ = sort.Strings
