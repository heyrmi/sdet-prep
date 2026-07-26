// Package modelgateway is the reference solution for Module 6.5.
//
// Points worth defending in an interview:
//
//   - The budget check happens BEFORE any provider call. Checking afterwards means you have
//     already spent the money you were trying not to spend.
//
//   - A cache hit costs nothing and consumes no budget. If a hit still bills the tenant, the
//     cache is not actually saving anything.
//
//   - AuthScope is part of the cache key. Without it, a semantic cache is a data-leak machine —
//     and the leak is silent, because every response looks perfectly normal.
//
//   - Unknown tasks default UP a tier. Defaulting down silently degrades quality for anything
//     nobody remembered to configure, which is far harder to notice than a bigger bill.
//
//   - Open circuits are skipped, not called-and-failed. Calling a known-dead provider burns
//     latency budget on a guaranteed failure.
package modelgateway

import (
	"errors"
	"math"
	"sort"
)

var (
	ErrBudgetExceeded     = errors.New("modelgateway: token budget exceeded")
	ErrAllProvidersFailed = errors.New("modelgateway: all providers failed")
	ErrCircuitOpen        = errors.New("modelgateway: circuit open")
)

// Tier is a model capability/cost class.
type Tier int

const (
	Small Tier = iota
	Medium
	Large
)

func (t Tier) String() string {
	switch t {
	case Small:
		return "small"
	case Medium:
		return "medium"
	case Large:
		return "large"
	}
	return "unknown"
}

// Model describes one servable model.
type Model struct {
	ID                   string
	Tier                 Tier
	CostPerMillionInput  int64
	CostPerMillionOutput int64
}

// Request is one gateway call.
type Request struct {
	Tenant          string
	Task            string
	Prompt          string
	Embedding       []float64
	AuthScope       string
	Intent          string
	InputTokens     int
	MaxOutputTokens int
}

// Response is what the gateway returns.
type Response struct {
	Text         string
	ModelID      string
	InputTokens  int
	OutputTokens int
	CostCents    int64
	CacheHit     bool
	Fallbacks    int
}

// CostCents returns the cost of a call in cents.
func (m Model) CostCents(inputTokens, outputTokens int) int64 {
	total := int64(inputTokens)*m.CostPerMillionInput + int64(outputTokens)*m.CostPerMillionOutput
	return total / 1_000_000
}

// Router picks a model for a request.
type Router struct {
	taskTiers   map[string]Tier
	defaultTier Tier
	models      map[Tier][]Model
}

// NewRouter builds a router.
func NewRouter(taskTiers map[string]Tier, defaultTier Tier, models []Model) *Router {
	r := &Router{
		taskTiers:   taskTiers,
		defaultTier: defaultTier,
		models:      make(map[Tier][]Model),
	}
	for _, m := range models {
		r.models[m.Tier] = append(r.models[m.Tier], m)
	}
	for tier := range r.models {
		list := r.models[tier]
		sort.Slice(list, func(i, j int) bool {
			if list[i].CostPerMillionInput != list[j].CostPerMillionInput {
				return list[i].CostPerMillionInput < list[j].CostPerMillionInput
			}
			return list[i].ID < list[j].ID
		})
		r.models[tier] = list
	}
	return r
}

// TierFor returns the tier a task routes to.
func (r *Router) TierFor(task string) Tier {
	if tier, ok := r.taskTiers[task]; ok {
		return tier
	}
	return r.defaultTier
}

// Chain returns the fallback chain for a task.
func (r *Router) Chain(task string) []Model {
	target := r.TierFor(task)
	var chain []Model
	// Target tier first, then degrade downward — a smaller model's answer beats an error page.
	for tier := target; tier >= Small; tier-- {
		chain = append(chain, r.models[tier]...)
	}
	return chain
}

// Breaker tracks per-model failures.
type Breaker struct {
	threshold int
	failures  map[string]int
	open      map[string]bool
}

// NewBreaker creates a breaker that opens after `threshold` consecutive failures.
func NewBreaker(threshold int) *Breaker {
	return &Breaker{threshold: threshold, failures: make(map[string]int), open: make(map[string]bool)}
}

// RecordSuccess resets a model's failure count and closes its circuit.
func (b *Breaker) RecordSuccess(modelID string) {
	b.failures[modelID] = 0
	b.open[modelID] = false
}

// RecordFailure increments the count and opens the circuit at the threshold.
func (b *Breaker) RecordFailure(modelID string) {
	b.failures[modelID]++
	if b.failures[modelID] >= b.threshold {
		b.open[modelID] = true
	}
}

// IsOpen reports whether the model should be skipped.
func (b *Breaker) IsOpen(modelID string) bool { return b.open[modelID] }

// CacheEntry is a stored response.
type CacheEntry struct {
	Embedding []float64
	AuthScope string
	Response  Response
}

// SemanticCache stores responses keyed by embedding similarity AND authorisation scope.
type SemanticCache struct {
	threshold       float64
	excludedIntents map[string]bool
	entries         []CacheEntry
	Hits, Misses    int
}

// NewSemanticCache creates a cache.
func NewSemanticCache(threshold float64, excludedIntents []string) *SemanticCache {
	ex := make(map[string]bool, len(excludedIntents))
	for _, i := range excludedIntents {
		ex[i] = true
	}
	return &SemanticCache{threshold: threshold, excludedIntents: ex}
}

// CosineSimilarity returns the cosine of the angle between two vectors.
func CosineSimilarity(a, b []float64) float64 {
	if len(a) == 0 || len(a) != len(b) {
		return 0
	}
	var dot, magA, magB float64
	for i := range a {
		dot += a[i] * b[i]
		magA += a[i] * a[i]
		magB += b[i] * b[i]
	}
	if magA == 0 || magB == 0 {
		return 0
	}
	return dot / (math.Sqrt(magA) * math.Sqrt(magB))
}

// Lookup returns a cached response if one is similar enough AND in the same auth scope.
func (c *SemanticCache) Lookup(r Request) (Response, bool) {
	if c.excludedIntents[r.Intent] {
		return Response{}, false // never consulted the cache, so not a miss either
	}

	best, bestSim := -1, c.threshold
	for i, e := range c.entries {
		// Authorisation is part of the key. Omitting this is how one user sees another
		// user's answer, and nothing errors when it happens.
		if e.AuthScope != r.AuthScope {
			continue
		}
		if sim := CosineSimilarity(e.Embedding, r.Embedding); sim >= bestSim {
			best, bestSim = i, sim
		}
	}

	if best == -1 {
		c.Misses++
		return Response{}, false
	}
	c.Hits++
	resp := c.entries[best].Response
	resp.CacheHit = true
	return resp, true
}

// Store adds a response to the cache, unless the intent is excluded.
func (c *SemanticCache) Store(r Request, resp Response) {
	if c.excludedIntents[r.Intent] {
		return // high-stakes answers are always freshly generated
	}
	c.entries = append(c.entries, CacheEntry{
		Embedding: r.Embedding,
		AuthScope: r.AuthScope,
		Response:  resp,
	})
}

// BudgetTracker enforces per-tenant token allowances.
type BudgetTracker struct {
	limits map[string]int
	used   map[string]int
}

// NewBudgetTracker creates a tracker.
func NewBudgetTracker(limits map[string]int) *BudgetTracker {
	if limits == nil {
		limits = make(map[string]int)
	}
	return &BudgetTracker{limits: limits, used: make(map[string]int)}
}

// CanSpend reports whether the tenant can afford `tokens` more.
func (b *BudgetTracker) CanSpend(tenant string, tokens int) bool {
	limit, ok := b.limits[tenant]
	if !ok {
		return true // no configured limit means unlimited
	}
	return b.used[tenant]+tokens <= limit
}

// Record adds tokens to a tenant's usage.
func (b *BudgetTracker) Record(tenant string, tokens int) { b.used[tenant] += tokens }

// Used returns a tenant's consumption.
func (b *BudgetTracker) Used(tenant string) int { return b.used[tenant] }

// Provider executes a call against a model.
type Provider interface {
	Call(model Model, r Request) (string, int, error)
}

// Gateway ties everything together.
type Gateway struct {
	router   *Router
	breaker  *Breaker
	cache    *SemanticCache
	budget   *BudgetTracker
	provider Provider
}

// NewGateway builds a gateway.
func NewGateway(r *Router, b *Breaker, c *SemanticCache, bt *BudgetTracker, p Provider) *Gateway {
	return &Gateway{router: r, breaker: b, cache: c, budget: bt, provider: p}
}

// Handle processes one request.
func (g *Gateway) Handle(r Request) (Response, error) {
	// 1. Cache first — a hit costs nothing and consumes no budget.
	if cached, ok := g.cache.Lookup(r); ok {
		cached.CostCents = 0
		return cached, nil
	}

	// 2. Budget against the WORST case, before spending anything.
	worstCase := r.InputTokens + r.MaxOutputTokens
	if !g.budget.CanSpend(r.Tenant, worstCase) {
		return Response{}, ErrBudgetExceeded
	}

	// 3. Walk the fallback chain.
	attempts := 0
	for _, m := range g.router.Chain(r.Task) {
		if g.breaker.IsOpen(m.ID) {
			attempts++ // skipped, but it still cost the caller a position in the chain
			continue
		}

		text, outputTokens, err := g.provider.Call(m, r)
		if err != nil {
			g.breaker.RecordFailure(m.ID)
			attempts++
			continue
		}

		g.breaker.RecordSuccess(m.ID)
		g.budget.Record(r.Tenant, r.InputTokens+outputTokens)

		resp := Response{
			Text:         text,
			ModelID:      m.ID,
			InputTokens:  r.InputTokens,
			OutputTokens: outputTokens,
			CostCents:    m.CostCents(r.InputTokens, outputTokens),
			Fallbacks:    attempts,
		}
		g.cache.Store(r, resp)
		return resp, nil
	}

	return Response{}, ErrAllProvidersFailed
}
