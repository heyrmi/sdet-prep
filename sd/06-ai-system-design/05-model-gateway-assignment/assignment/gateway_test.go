package modelgateway

import (
	"errors"
	"testing"
)

func models() []Model {
	return []Model{
		{ID: "small-1", Tier: Small, CostPerMillionInput: 25, CostPerMillionOutput: 125},
		{ID: "med-1", Tier: Medium, CostPerMillionInput: 100, CostPerMillionOutput: 500},
		{ID: "large-1", Tier: Large, CostPerMillionInput: 300, CostPerMillionOutput: 1500},
		{ID: "large-2", Tier: Large, CostPerMillionInput: 500, CostPerMillionOutput: 2000},
	}
}

func router() *Router {
	return NewRouter(map[string]Tier{
		"classify":    Small,
		"summarise":   Medium,
		"code-review": Large,
	}, Large, models())
}

// stubProvider fails for models listed in failFor, succeeds otherwise.
type stubProvider struct {
	failFor map[string]bool
	calls   []string
}

func (p *stubProvider) Call(m Model, r Request) (string, int, error) {
	p.calls = append(p.calls, m.ID)
	if p.failFor[m.ID] {
		return "", 0, errors.New("provider down")
	}
	return "answer from " + m.ID, 100, nil
}

// ---------- 1) Cost ----------

func TestCostCents(t *testing.T) {
	m := Model{CostPerMillionInput: 300, CostPerMillionOutput: 1500}
	// 3,000,000 input x 300/M = 900 ; 400,000 output x 1500/M = 600 ; total 1500
	if got := m.CostCents(3_000_000, 400_000); got != 1500 {
		t.Fatalf("want 1500 cents, got %d", got)
	}
}

func TestCostCentsSmallCallRoundsDown(t *testing.T) {
	m := Model{CostPerMillionInput: 300, CostPerMillionOutput: 1500}
	if got := m.CostCents(100, 10); got != 0 {
		t.Fatalf("a tiny call rounds down to 0 cents, got %d", got)
	}
}

// ---------- 2) Routing ----------

func TestTierForKnownTask(t *testing.T) {
	r := router()
	if got := r.TierFor("classify"); got != Small {
		t.Fatalf("want Small, got %v", got)
	}
	if got := r.TierFor("code-review"); got != Large {
		t.Fatalf("want Large, got %v", got)
	}
}

func TestUnknownTaskDefaultsUp(t *testing.T) {
	// Defaulting DOWN would silently degrade quality for any task nobody configured — a much
	// harder failure to notice than a slightly larger bill.
	if got := router().TierFor("something-new"); got != Large {
		t.Fatalf("an unknown task must use the configured default (Large), got %v", got)
	}
}

func TestChainStartsAtTargetTierCheapestFirst(t *testing.T) {
	chain := router().Chain("code-review")
	if len(chain) == 0 {
		t.Fatal("chain must not be empty")
	}
	if chain[0].ID != "large-1" {
		t.Fatalf("cheapest model in the target tier first, want large-1, got %s", chain[0].ID)
	}
	if chain[1].ID != "large-2" {
		t.Fatalf("then the more expensive one in the same tier, got %s", chain[1].ID)
	}
}

func TestChainDegradesDownward(t *testing.T) {
	chain := router().Chain("code-review")
	var ids []string
	for _, m := range chain {
		ids = append(ids, m.ID)
	}
	want := []string{"large-1", "large-2", "med-1", "small-1"}
	if len(ids) != len(want) {
		t.Fatalf("want %v, got %v", want, ids)
	}
	for i := range want {
		if ids[i] != want[i] {
			t.Fatalf("want %v, got %v", want, ids)
		}
	}
}

func TestChainFromSmallHasNoLowerTier(t *testing.T) {
	chain := router().Chain("classify")
	if len(chain) != 1 || chain[0].ID != "small-1" {
		t.Fatalf("Small is the bottom tier; want just small-1, got %v", chain)
	}
}

// ---------- 3) Circuit breaker ----------

func TestBreakerOpensAtThreshold(t *testing.T) {
	b := NewBreaker(3)
	b.RecordFailure("m")
	b.RecordFailure("m")
	if b.IsOpen("m") {
		t.Fatal("2 failures with threshold 3 must not open the circuit")
	}
	b.RecordFailure("m")
	if !b.IsOpen("m") {
		t.Fatal("3 failures must open the circuit")
	}
}

func TestBreakerSuccessResets(t *testing.T) {
	b := NewBreaker(2)
	b.RecordFailure("m")
	b.RecordSuccess("m")
	b.RecordFailure("m")
	if b.IsOpen("m") {
		t.Fatal("a success must reset the consecutive-failure count")
	}
}

func TestBreakerReclosesOnSuccess(t *testing.T) {
	b := NewBreaker(1)
	b.RecordFailure("m")
	if !b.IsOpen("m") {
		t.Fatal("setup: should be open")
	}
	b.RecordSuccess("m")
	if b.IsOpen("m") {
		t.Fatal("a recovered provider must be usable again")
	}
}

// ---------- 4) Semantic cache ----------

func TestCacheHitOnIdenticalEmbedding(t *testing.T) {
	c := NewSemanticCache(0.97, nil)
	req := Request{Embedding: []float64{1, 0}, AuthScope: "tenant-a"}
	c.Store(req, Response{Text: "cached"})

	got, ok := c.Lookup(req)
	if !ok || got.Text != "cached" {
		t.Fatalf("identical embedding must hit, got %+v ok=%v", got, ok)
	}
}

func TestCacheMissBelowThreshold(t *testing.T) {
	// The dangerous near-miss: "how do I cancel" vs "how do I NOT cancel" sit around 0.94.
	// A conservative threshold is what stops a confidently wrong answer being served.
	c := NewSemanticCache(0.97, nil)
	c.Store(Request{Embedding: []float64{1, 0}, AuthScope: "t"}, Response{Text: "cached"})

	// cos ≈ 0.9487
	if _, ok := c.Lookup(Request{Embedding: []float64{3, 1}, AuthScope: "t"}); ok {
		t.Fatal("similarity below the threshold must MISS — this is the negation-flip failure")
	}
}

func TestCacheIsolatesAuthScopes(t *testing.T) {
	c := NewSemanticCache(0.9, nil)
	c.Store(Request{Embedding: []float64{1, 0}, AuthScope: "tenant-a"}, Response{Text: "secret-a"})

	if got, ok := c.Lookup(Request{Embedding: []float64{1, 0}, AuthScope: "tenant-b"}); ok {
		t.Fatalf("a different auth scope must NOT hit — this is how one user sees another "+
			"user's answer; got %q", got.Text)
	}
}

func TestCacheSkipsExcludedIntents(t *testing.T) {
	c := NewSemanticCache(0.9, []string{"medical", "billing"})
	req := Request{Embedding: []float64{1, 0}, AuthScope: "t", Intent: "medical"}
	c.Store(req, Response{Text: "should not be stored"})

	if _, ok := c.Lookup(req); ok {
		t.Fatal("high-stakes intents must never be cached")
	}
}

func TestCachePicksBestMatch(t *testing.T) {
	c := NewSemanticCache(0.5, nil)
	c.Store(Request{Embedding: []float64{1, 1}, AuthScope: "t"}, Response{Text: "far"})
	c.Store(Request{Embedding: []float64{1, 0}, AuthScope: "t"}, Response{Text: "near"})

	got, ok := c.Lookup(Request{Embedding: []float64{1, 0}, AuthScope: "t"})
	if !ok || got.Text != "near" {
		t.Fatalf("the most similar eligible entry must win, got %+v", got)
	}
}

// ---------- 5) Budgets ----------

func TestBudgetUnlimitedWhenUnconfigured(t *testing.T) {
	b := NewBudgetTracker(map[string]int{"capped": 100})
	if !b.CanSpend("uncapped", 1_000_000) {
		t.Fatal("a tenant with no configured limit is unlimited")
	}
}

func TestBudgetBlocksOverspend(t *testing.T) {
	b := NewBudgetTracker(map[string]int{"t": 100})
	b.Record("t", 90)

	if !b.CanSpend("t", 10) {
		t.Fatal("90 + 10 = 100 is exactly at the limit and must be allowed")
	}
	if b.CanSpend("t", 11) {
		t.Fatal("90 + 11 exceeds the limit and must be refused")
	}
}

// ---------- 6) Gateway ----------

func newGateway(p Provider) *Gateway {
	return NewGateway(router(), NewBreaker(2), NewSemanticCache(0.97, nil),
		NewBudgetTracker(map[string]int{}), p)
}

func TestGatewayHappyPath(t *testing.T) {
	p := &stubProvider{failFor: map[string]bool{}}
	g := newGateway(p)

	resp, err := g.Handle(Request{Tenant: "t", Task: "code-review", InputTokens: 1000, MaxOutputTokens: 500})
	if err != nil {
		t.Fatalf("want success, got %v", err)
	}
	if resp.ModelID != "large-1" {
		t.Fatalf("want the cheapest Large model, got %s", resp.ModelID)
	}
	if resp.Fallbacks != 0 {
		t.Fatalf("no fallbacks needed, got %d", resp.Fallbacks)
	}
}

func TestGatewayFallsBackOnFailure(t *testing.T) {
	p := &stubProvider{failFor: map[string]bool{"large-1": true, "large-2": true}}
	g := newGateway(p)

	resp, err := g.Handle(Request{Tenant: "t", Task: "code-review", InputTokens: 100, MaxOutputTokens: 100})
	if err != nil {
		t.Fatalf("the chain should degrade to a working model, got %v", err)
	}
	if resp.ModelID != "med-1" {
		t.Fatalf("want med-1 after both Large models failed, got %s", resp.ModelID)
	}
	if resp.Fallbacks != 2 {
		t.Fatalf("want 2 fallbacks recorded, got %d", resp.Fallbacks)
	}
}

func TestGatewayAllProvidersFail(t *testing.T) {
	p := &stubProvider{failFor: map[string]bool{
		"large-1": true, "large-2": true, "med-1": true, "small-1": true,
	}}
	g := newGateway(p)

	if _, err := g.Handle(Request{Tenant: "t", Task: "code-review", InputTokens: 10, MaxOutputTokens: 10}); !errors.Is(err, ErrAllProvidersFailed) {
		t.Fatalf("want ErrAllProvidersFailed, got %v", err)
	}
}

func TestGatewaySkipsOpenCircuits(t *testing.T) {
	p := &stubProvider{failFor: map[string]bool{}}
	b := NewBreaker(2)
	b.RecordFailure("large-1")
	b.RecordFailure("large-1") // open

	g := NewGateway(router(), b, NewSemanticCache(0.97, nil), NewBudgetTracker(nil), p)
	resp, err := g.Handle(Request{Tenant: "t", Task: "code-review", InputTokens: 10, MaxOutputTokens: 10})
	if err != nil {
		t.Fatal(err)
	}
	if resp.ModelID != "large-2" {
		t.Fatalf("large-1's circuit is open and must be skipped, got %s", resp.ModelID)
	}
	for _, called := range p.calls {
		if called == "large-1" {
			t.Fatal("an open circuit must not be called at all — that is the point of the breaker")
		}
	}
}

func TestGatewayCacheHitCostsNothing(t *testing.T) {
	p := &stubProvider{failFor: map[string]bool{}}
	g := NewGateway(router(), NewBreaker(2), NewSemanticCache(0.9, nil),
		NewBudgetTracker(map[string]int{"t": 10_000}), p)

	req := Request{Tenant: "t", Task: "classify", Prompt: "hello",
		Embedding: []float64{1, 0}, AuthScope: "t", InputTokens: 500, MaxOutputTokens: 100}

	first, err := g.Handle(req)
	if err != nil {
		t.Fatal(err)
	}
	if first.CacheHit {
		t.Fatal("the first call cannot be a cache hit")
	}
	usedAfterFirst := g.budget.Used("t")

	second, err := g.Handle(req)
	if err != nil {
		t.Fatal(err)
	}
	if !second.CacheHit {
		t.Fatal("the identical second call must hit the cache")
	}
	if second.CostCents != 0 {
		t.Fatalf("a cache hit must cost nothing, got %d cents", second.CostCents)
	}
	if g.budget.Used("t") != usedAfterFirst {
		t.Fatal("a cache hit must not consume budget — otherwise caching does not actually save money")
	}
	if len(p.calls) != 1 {
		t.Fatalf("the provider must be called once, not twice; got %v", p.calls)
	}
}

func TestGatewayRefusesOverBudget(t *testing.T) {
	p := &stubProvider{failFor: map[string]bool{}}
	g := NewGateway(router(), NewBreaker(2), NewSemanticCache(0.97, nil),
		NewBudgetTracker(map[string]int{"t": 100}), p)

	_, err := g.Handle(Request{Tenant: "t", Task: "classify", InputTokens: 500, MaxOutputTokens: 100})
	if !errors.Is(err, ErrBudgetExceeded) {
		t.Fatalf("want ErrBudgetExceeded, got %v", err)
	}
	if len(p.calls) != 0 {
		t.Fatal("the budget check must happen BEFORE any provider call — otherwise you have " +
			"already spent the money you were trying not to spend")
	}
}

func TestGatewayBudgetChecksWorstCase(t *testing.T) {
	// 900 used of 1000; a request whose worst case is 200 must be refused even though its
	// input alone (50) would fit.
	p := &stubProvider{failFor: map[string]bool{}}
	bt := NewBudgetTracker(map[string]int{"t": 1000})
	bt.Record("t", 900)
	g := NewGateway(router(), NewBreaker(2), NewSemanticCache(0.97, nil), bt, p)

	_, err := g.Handle(Request{Tenant: "t", Task: "classify", InputTokens: 50, MaxOutputTokens: 150})
	if !errors.Is(err, ErrBudgetExceeded) {
		t.Fatalf("must budget against prompt + max output, got %v", err)
	}
}
