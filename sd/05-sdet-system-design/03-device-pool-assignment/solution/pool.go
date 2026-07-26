// Package devicepool is the reference solution for Module 5.3.
//
// Points worth defending in an interview:
//
//   - Leases expire; they are not held until released. A client that crashes mid-session would
//     otherwise shrink the pool permanently, and at 2,000 sessions that bleeds capacity daily.
//     Renewal is the heartbeat: alive clients keep their slot, dead ones give it back.
//
//   - An expired lease cannot be renewed. Its slot may already be serving someone else, so
//     resurrecting it would double-book the node.
//
//   - Allocation is least-loaded-first, not first-fit. Stacking sessions on one node means one
//     wedged machine takes out every session on it.
//
//   - Health ejection is on CONSECUTIVE failures, reset by any success. A wedged node that
//     still accepts work is worse than a dead one — its failures masquerade as product bugs.
//
//   - The per-tenant share cap is what makes a shared pool survivable. Without it, one team's
//     5,000-test suite starves everyone, and starved teams stop trusting CI.
package devicepool

import (
	"errors"
	"sort"
	"sync"
	"time"
)

var (
	ErrNoCapacity   = errors.New("devicepool: no matching capacity available")
	ErrUnknownLease = errors.New("devicepool: unknown lease")
)

// Node is one machine or device that can host sessions.
type Node struct {
	ID         string
	Capability string
	Slots      int
}

// Lease is one granted allocation.
type Lease struct {
	ID        string
	NodeID    string
	Tenant    string
	Acquired  time.Time
	ExpiresAt time.Time
}

type nodeState struct {
	node                Node
	inUse               int
	consecutiveFailures int
	healthy             bool
}

// Pool allocates nodes to tenants under lease.
type Pool struct {
	mu        sync.Mutex
	nodes     map[string]*nodeState
	leases    map[string]*Lease
	perTenant map[string]int

	leaseTTL          time.Duration
	maxSharePerTenant float64
	failureThreshold  int

	now func() time.Time
	seq int
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

// TotalSlots is the pool's full capacity.
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

// Acquire grants a lease on a node matching capability, or ErrNoCapacity.
func (p *Pool) Acquire(tenant, capability string) (*Lease, error) {
	p.mu.Lock()
	defer p.mu.Unlock()

	// Always reclaim first: otherwise a crashed client's slot is invisible capacity.
	p.reclaimExpiredLocked()

	// Fairness cap. A cap that floors to 0 would make a small pool unusable, so treat it as 1.
	cap := int(float64(p.totalSlotsLocked()) * p.maxSharePerTenant)
	if cap < 1 {
		cap = 1
	}
	if p.perTenant[tenant] >= cap {
		return nil, ErrNoCapacity
	}

	var chosen *nodeState
	for _, ns := range p.nodes {
		if !ns.healthy || ns.node.Capability != capability || ns.inUse >= ns.node.Slots {
			continue
		}
		// Least-loaded first; lowest ID breaks ties so allocation is reproducible.
		if chosen == nil || ns.inUse < chosen.inUse ||
			(ns.inUse == chosen.inUse && ns.node.ID < chosen.node.ID) {
			chosen = ns
		}
	}
	if chosen == nil {
		return nil, ErrNoCapacity
	}

	now := p.now()
	l := &Lease{
		ID:        p.nextLeaseIDLocked(),
		NodeID:    chosen.node.ID,
		Tenant:    tenant,
		Acquired:  now,
		ExpiresAt: now.Add(p.leaseTTL),
	}
	chosen.inUse++
	p.perTenant[tenant]++
	p.leases[l.ID] = l
	return l, nil
}

// Release returns a lease's capacity to the pool.
func (p *Pool) Release(leaseID string) error {
	p.mu.Lock()
	defer p.mu.Unlock()

	l, ok := p.leases[leaseID]
	if !ok {
		return ErrUnknownLease
	}
	p.freeLeaseLocked(l)
	return nil
}

// freeLeaseLocked removes a lease and returns its capacity. Called with the lock held.
func (p *Pool) freeLeaseLocked(l *Lease) {
	if ns, ok := p.nodes[l.NodeID]; ok && ns.inUse > 0 {
		ns.inUse--
	}
	if p.perTenant[l.Tenant] > 0 {
		p.perTenant[l.Tenant]--
	}
	delete(p.leases, l.ID)
}

// Renew extends a lease by the pool's TTL. This is the client heartbeat.
func (p *Pool) Renew(leaseID string) error {
	p.mu.Lock()
	defer p.mu.Unlock()

	l, ok := p.leases[leaseID]
	if !ok {
		return ErrUnknownLease
	}
	// An already-expired lease is not renewable: its slot may already be serving someone else.
	if !p.now().Before(l.ExpiresAt) {
		p.freeLeaseLocked(l)
		return ErrUnknownLease
	}
	l.ExpiresAt = p.now().Add(p.leaseTTL)
	return nil
}

// reclaimExpiredLocked frees every lease past its expiry.
func (p *Pool) reclaimExpiredLocked() int {
	now := p.now()
	var expired []*Lease
	for _, l := range p.leases {
		if !now.Before(l.ExpiresAt) {
			expired = append(expired, l)
		}
	}
	for _, l := range expired {
		p.freeLeaseLocked(l)
	}
	return len(expired)
}

func (p *Pool) nextLeaseIDLocked() string {
	p.seq++
	return "lease-" + itoa(p.seq)
}

// ReportResult records a session outcome, ejecting a node after repeated failures.
func (p *Pool) ReportResult(nodeID string, ok bool) bool {
	p.mu.Lock()
	defer p.mu.Unlock()

	ns, exists := p.nodes[nodeID]
	if !exists {
		return false
	}
	if ok {
		ns.consecutiveFailures = 0
		ns.healthy = true // a node that starts working again rejoins the pool
		return true
	}
	ns.consecutiveFailures++
	if ns.consecutiveFailures >= p.failureThreshold {
		ns.healthy = false
	}
	return ns.healthy
}

// HealthyNodes returns the IDs of healthy nodes, sorted.
func (p *Pool) HealthyNodes() []string {
	p.mu.Lock()
	defer p.mu.Unlock()

	var ids []string
	for id, ns := range p.nodes {
		if ns.healthy {
			ids = append(ids, id)
		}
	}
	sort.Strings(ids)
	return ids
}

// ScaleDecision is a recommended capacity change.
type ScaleDecision struct {
	Delta  int
	Reason string
}

// RecommendScale suggests a capacity change for one capability.
func (p *Pool) RecommendScale(capability string, queueDepth, slotsPerNode int) ScaleDecision {
	p.mu.Lock()
	defer p.mu.Unlock()

	if queueDepth > 0 {
		if slotsPerNode < 1 {
			slotsPerNode = 1
		}
		need := (queueDepth + slotsPerNode - 1) / slotsPerNode // ceil
		return ScaleDecision{Delta: need, Reason: "queue depth"}
	}

	healthy, slots, inUse := 0, 0, 0
	for _, ns := range p.nodes {
		if !ns.healthy || ns.node.Capability != capability {
			continue
		}
		healthy++
		slots += ns.node.Slots
		inUse += ns.inUse
	}

	if slots == 0 || healthy < 2 {
		return ScaleDecision{Delta: 0, Reason: "at minimum capacity"}
	}

	if float64(inUse)/float64(slots) < 0.25 {
		// Exactly one node at a time. Thrashing capacity costs more than briefly over-providing.
		return ScaleDecision{Delta: -1, Reason: "utilisation below 25%"}
	}
	return ScaleDecision{Delta: 0, Reason: "utilisation healthy"}
}

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
