// Package modelgateway is the Module 6.5 assignment: routing, caching, fallback, and budget
// enforcement for a model gateway.
//
// Read 06-ai-system-design/05-model-gateway-routing-and-cost.md first.
//
// Fill in every function marked `// TODO`. Run the tests until green:
//
//	go test ./...
//
// Everything here is DETERMINISTIC. That is the lesson's closing point: the gateway is the most
// testable component in an AI stack, because routing rules, cache keys, fallback ordering and
// budget arithmetic are all ordinary logic.
package modelgateway

import (
	"errors"
	"sort"
)

var (
	// ErrBudgetExceeded means the tenant has spent its token allowance.
	ErrBudgetExceeded = errors.New("modelgateway: token budget exceeded")
	// ErrAllProvidersFailed means every model in the fallback chain failed.
	ErrAllProvidersFailed = errors.New("modelgateway: all providers failed")
	// ErrCircuitOpen means a provider is being skipped because its circuit is open.
	ErrCircuitOpen = errors.New("modelgateway: circuit open")
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
	ID   string
	Tier Tier
	// CostPerMillionInput / Output in cents, so the arithmetic stays in integers.
	CostPerMillionInput  int64
	CostPerMillionOutput int64
}

// Request is one gateway call.
type Request struct {
	Tenant string
	// Task drives static routing, e.g. "classify", "summarise", "code-review".
	Task string
	// Prompt is the full prompt text; used for cache keys.
	Prompt string
	// Embedding is the prompt's vector, for semantic cache lookup.
	Embedding []float64
	// AuthScope is who is allowed to see the answer. It MUST be part of the cache key —
	// omitting it is how a cache becomes a data-leak machine.
	AuthScope string
	// Intent lets high-stakes calls opt out of caching entirely.
	Intent string
	InputTokens int
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
	// Fallbacks is how many models were tried before this one succeeded.
	Fallbacks int
}

// ----------------------------------------------------------------------------
// 1) Cost
// ----------------------------------------------------------------------------

// CostCents returns the cost of a call in cents (integer arithmetic, rounded down).
//
//	(inputTokens * costPerMillionInput + outputTokens * costPerMillionOutput) / 1_000_000
func (m Model) CostCents(inputTokens, outputTokens int) int64 {
	// TODO
	return 0
}

// ----------------------------------------------------------------------------
// 2) Routing
//
// Static routing: map a task to a tier, fall back to a default for unknown tasks.
// Crude, transparent, effective — and the right place to start, because you can explain
// exactly why any given request went where it did.
// ----------------------------------------------------------------------------

// Router picks a model for a request.
type Router struct {
	// taskTiers maps task name -> tier.
	taskTiers map[string]Tier
	// defaultTier is used for unknown tasks. Default UP, not down: an unknown task on an
	// under-powered model fails silently and produces a worse product, which is harder to
	// notice than a slightly larger bill.
	defaultTier Tier
	// models by tier, cheapest first within a tier.
	models map[Tier][]Model
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
	// TODO: look up task; fall back to defaultTier.
	return Small
}

// Chain returns the fallback chain for a task: every model in the target tier (cheapest first),
// followed by every model in each LOWER tier (also cheapest first).
//
// Degrading downward is deliberate. When the large models are unavailable, a smaller model's
// answer beats an error page — but you must label it, which the caller does via Response.Fallbacks.
func (r *Router) Chain(task string) []Model {
	// TODO:
	//  1. target := TierFor(task).
	//  2. Append r.models[target], then each tier below it in descending order.
	return nil
}

// ----------------------------------------------------------------------------
// 3) Circuit breaker
//
// When a provider is failing, stop sending it traffic. Retrying into a dead provider burns
// latency budget and money for nothing.
// ----------------------------------------------------------------------------

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
	// TODO
}

// RecordFailure increments the count and opens the circuit at the threshold.
func (b *Breaker) RecordFailure(modelID string) {
	// TODO
}

// IsOpen reports whether the model should be skipped.
func (b *Breaker) IsOpen(modelID string) bool {
	// TODO
	return false
}

// ----------------------------------------------------------------------------
// 4) Semantic cache
//
// Dangerous by nature: a near-miss returns a confidently wrong answer, and nothing errors.
// The controls that make it survivable are all implemented here.
// ----------------------------------------------------------------------------

// CacheEntry is a stored response.
type CacheEntry struct {
	Embedding []float64
	AuthScope string
	Response  Response
}

// SemanticCache stores responses keyed by embedding similarity AND authorisation scope.
type SemanticCache struct {
	// threshold is the minimum cosine similarity for a hit. Conservative (0.97+) beats
	// permissive: "how do I cancel" and "how do I NOT cancel" sit around 0.94.
	threshold float64
	// excludedIntents never cache — high-stakes answers must always be freshly generated.
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

// CosineSimilarity returns the cosine of the angle between two vectors, 0 for degenerate input.
func CosineSimilarity(a, b []float64) float64 {
	// TODO (same as the 6.3 assignment — return 0 rather than NaN)
	return 0
}

// Lookup returns a cached response if one is similar enough AND in the same auth scope.
//
// Rules:
//  1. An excluded intent never hits (and does not count as a miss — it never consulted the cache).
//  2. Only entries with a MATCHING AuthScope are eligible. Skipping this is how one user
//     sees another user's answer.
//  3. Among eligible entries, the highest similarity above the threshold wins.
func (c *SemanticCache) Lookup(r Request) (Response, bool) {
	// TODO
	return Response{}, false
}

// Store adds a response to the cache, unless the intent is excluded.
func (c *SemanticCache) Store(r Request, resp Response) {
	// TODO
}

// ----------------------------------------------------------------------------
// 5) Budgets
// ----------------------------------------------------------------------------

// BudgetTracker enforces per-tenant token allowances.
type BudgetTracker struct {
	limits map[string]int
	used   map[string]int
}

// NewBudgetTracker creates a tracker. A tenant with no limit is unlimited.
func NewBudgetTracker(limits map[string]int) *BudgetTracker {
	return &BudgetTracker{limits: limits, used: make(map[string]int)}
}

// CanSpend reports whether the tenant can afford `tokens` more.
func (b *BudgetTracker) CanSpend(tenant string, tokens int) bool {
	// TODO: no limit configured => unlimited.
	return true
}

// Record adds tokens to a tenant's usage.
func (b *BudgetTracker) Record(tenant string, tokens int) {
	// TODO
}

// Used returns a tenant's consumption.
func (b *BudgetTracker) Used(tenant string) int {
	// TODO
	return 0
}

// ----------------------------------------------------------------------------
// 6) The gateway
// ----------------------------------------------------------------------------

// Provider executes a call against a model. Returns an error to simulate a provider failure.
type Provider interface {
	Call(model Model, r Request) (string, int, error) // text, outputTokens, err
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
//
// Order (each step exists for a reason stated in the lesson):
//  1. Cache lookup. A hit returns immediately with CacheHit=true and zero cost — a cached
//     response must not consume budget.
//  2. Budget check against worst case (InputTokens + MaxOutputTokens). Over budget ->
//     ErrBudgetExceeded, before spending anything.
//  3. Walk the fallback chain. Skip models whose circuit is open.
//     - success: record success, record budget usage, store in cache, return with the
//       fallback count (how many models were SKIPPED OR FAILED before this one).
//     - failure: record the failure and continue to the next model.
//  4. Chain exhausted -> ErrAllProvidersFailed.
func (g *Gateway) Handle(r Request) (Response, error) {
	// TODO
	return Response{}, ErrAllProvidersFailed
}
