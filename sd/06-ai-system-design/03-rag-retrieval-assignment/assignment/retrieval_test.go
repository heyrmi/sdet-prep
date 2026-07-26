package ragretrieval

import (
	"math"
	"testing"
)

func corpus() []Chunk {
	return []Chunk{
		{ID: "c1", Title: "Refunds", Tokens: 100,
			Text: "Refunds are issued within 30 days of purchase for standard plans."},
		{ID: "c2", Title: "Enterprise", Tokens: 120,
			Text: "Enterprise customers may request a refund exception beyond the standard window."},
		{ID: "c3", Title: "Errors", Tokens: 80,
			Text: "Error ERR_4021 indicates the payment method was declined by the issuing bank."},
		{ID: "c4", Title: "Shipping", Tokens: 90,
			Text: "Shipping takes three to five business days for domestic orders."},
		{ID: "c5", Title: "Holidays", Tokens: 60,
			Text: "The office is closed on public holidays and the last week of December."},
	}
}

func approx(a, b float64) bool { return math.Abs(a-b) < 1e-9 }

// ---------- 1) BM25 ----------

func TestTokenize(t *testing.T) {
	got := Tokenize("Error ERR_4021, declined!")
	want := []string{"error", "err", "4021", "declined"}
	if len(got) != len(want) {
		t.Fatalf("want %v, got %v", want, got)
	}
	for i := range want {
		if got[i] != want[i] {
			t.Fatalf("want %v, got %v", want, got)
		}
	}
}

func TestBM25FindsExactRareToken(t *testing.T) {
	// The case dense retrieval is bad at: a rare exact code.
	idx := NewBM25Index(corpus())
	got := idx.Search("ERR_4021", 3)

	if len(got) == 0 {
		t.Fatal("BM25 must find an exact token match")
	}
	if got[0].Chunk.ID != "c3" {
		t.Fatalf("want c3 (the chunk containing ERR_4021) first, got %s", got[0].Chunk.ID)
	}
}

func TestBM25ExcludesZeroScores(t *testing.T) {
	idx := NewBM25Index(corpus())
	got := idx.Search("refund", 10)

	for _, s := range got {
		if s.Score <= 0 {
			t.Fatalf("chunk %s scored %v — zero-scoring chunks must be excluded",
				s.Chunk.ID, s.Score)
		}
	}
	for _, s := range got {
		if s.Chunk.ID == "c5" {
			t.Fatal("the holidays chunk shares no term with 'refund' and must not appear")
		}
	}
}

func TestBM25RespectsK(t *testing.T) {
	idx := NewBM25Index(corpus())
	if got := idx.Search("refund refunds standard", 2); len(got) > 2 {
		t.Fatalf("k=2 must cap results, got %d", len(got))
	}
}

func TestBM25IsDescending(t *testing.T) {
	idx := NewBM25Index(corpus())
	got := idx.Search("refund standard plans", 10)
	for i := 1; i < len(got); i++ {
		if got[i].Score > got[i-1].Score {
			t.Fatalf("results must be sorted by score descending, got %v then %v",
				got[i-1].Score, got[i].Score)
		}
	}
}

func TestBM25RareTermsOutweighCommonOnes(t *testing.T) {
	// IDF: a term appearing in one chunk should count for far more than one in most chunks.
	chunks := []Chunk{
		{ID: "a", Text: "the quick brown fox", Tokens: 10},
		{ID: "b", Text: "the lazy dog sleeps", Tokens: 10},
		{ID: "c", Text: "the cat naps quietly", Tokens: 10},
	}
	idx := NewBM25Index(chunks)

	rare := idx.Search("fox", 5)
	common := idx.Search("the", 5)

	if len(rare) == 0 {
		t.Fatal("'fox' should match chunk a")
	}
	if len(common) > 0 && common[0].Score >= rare[0].Score {
		t.Fatalf("a term in 1 of 3 docs must score above one in 3 of 3; rare=%v common=%v",
			rare[0].Score, common[0].Score)
	}
}

func TestBM25EmptyCorpus(t *testing.T) {
	idx := NewBM25Index(nil)
	if got := idx.Search("anything", 5); len(got) != 0 {
		t.Fatalf("empty corpus must return nothing, got %v", got)
	}
}

// ---------- 2) Vector search ----------

func TestCosineSimilarity(t *testing.T) {
	if got := CosineSimilarity([]float64{1, 0}, []float64{1, 0}); !approx(got, 1) {
		t.Fatalf("identical vectors want 1.0, got %v", got)
	}
	if got := CosineSimilarity([]float64{1, 0}, []float64{0, 1}); !approx(got, 0) {
		t.Fatalf("orthogonal vectors want 0.0, got %v", got)
	}
	if got := CosineSimilarity([]float64{1, 0}, []float64{-1, 0}); !approx(got, -1) {
		t.Fatalf("opposite vectors want -1.0, got %v", got)
	}
}

func TestCosineSimilarityDegenerateInputs(t *testing.T) {
	if got := CosineSimilarity([]float64{1, 0}, []float64{1, 0, 0}); got != 0 {
		t.Fatalf("mismatched lengths must return 0, got %v", got)
	}
	if got := CosineSimilarity([]float64{0, 0}, []float64{1, 0}); got != 0 {
		t.Fatalf("a zero vector must return 0, not NaN — NaN poisons every downstream sort, got %v", got)
	}
	if math.IsNaN(CosineSimilarity(nil, nil)) {
		t.Fatal("nil inputs must not produce NaN")
	}
}

func TestVectorSearchRanksByProximity(t *testing.T) {
	chunks := []Chunk{
		{ID: "near", Embedding: []float64{1, 0}},
		{ID: "mid", Embedding: []float64{0.7, 0.7}},
		{ID: "far", Embedding: []float64{0, 1}},
	}
	got := VectorSearch(chunks, []float64{1, 0}, 3)

	if len(got) != 3 {
		t.Fatalf("want 3 results, got %d", len(got))
	}
	if got[0].Chunk.ID != "near" || got[2].Chunk.ID != "far" {
		t.Fatalf("want near..far ordering, got %s..%s", got[0].Chunk.ID, got[2].Chunk.ID)
	}
}

// ---------- 3) RRF ----------

func TestRRFCombinesRanksNotScores(t *testing.T) {
	// The scales are deliberately absurd: BM25-like scores in the tens, cosine in [0,1].
	// A score-weighted blend would let the first list dominate; RRF must not care.
	lexical := []Scored{{Chunk: Chunk{ID: "a"}, Score: 95.0}, {Chunk: Chunk{ID: "b"}, Score: 90.0}}
	dense := []Scored{{Chunk: Chunk{ID: "b"}, Score: 0.81}, {Chunk: Chunk{ID: "a"}, Score: 0.80}}

	got := ReciprocalRankFusion([][]Scored{lexical, dense}, DefaultRRFK)
	if len(got) != 2 {
		t.Fatalf("want 2 fused docs, got %d", len(got))
	}
	// a: 1/61 + 1/62 ; b: 1/62 + 1/61 — identical, so the ID tie-break decides.
	if !approx(got[0].Score, got[1].Score) {
		t.Fatalf("symmetric ranks must produce equal RRF scores, got %v and %v",
			got[0].Score, got[1].Score)
	}
}

func TestRRFRewardsAppearingInBothLists(t *testing.T) {
	lexical := []Scored{{Chunk: Chunk{ID: "both"}}, {Chunk: Chunk{ID: "lex-only"}}}
	dense := []Scored{{Chunk: Chunk{ID: "dense-only"}}, {Chunk: Chunk{ID: "both"}}}

	got := ReciprocalRankFusion([][]Scored{lexical, dense}, DefaultRRFK)
	if got[0].Chunk.ID != "both" {
		t.Fatalf("a document ranked by BOTH retrievers must win, got %s", got[0].Chunk.ID)
	}
}

func TestRRFExactScore(t *testing.T) {
	single := []Scored{{Chunk: Chunk{ID: "x"}}}
	got := ReciprocalRankFusion([][]Scored{single}, 60)

	want := 1.0 / 61.0 // rank 1 => 1/(60+1)
	if !approx(got[0].Score, want) {
		t.Fatalf("want %v, got %v", want, got[0].Score)
	}
}

func TestRRFEmpty(t *testing.T) {
	if got := ReciprocalRankFusion(nil, DefaultRRFK); len(got) != 0 {
		t.Fatalf("want nothing, got %v", got)
	}
}

// ---------- Hybrid ----------

func TestHybridBeatsEitherAloneOnMixedQuery(t *testing.T) {
	c := corpus()
	idx := NewBM25Index(c)

	// Give every chunk an embedding; make the "enterprise refund" chunk (c2) the dense winner
	// while the exact code query term favours c3 lexically.
	embeddings := map[string][]float64{
		"c1": {0.5, 0.5}, "c2": {1, 0}, "c3": {0, 1}, "c4": {-1, 0}, "c5": {0, -1},
	}
	for i := range c {
		c[i].Embedding = embeddings[c[i].ID]
	}

	got := HybridSearch(idx, c, "refund exception", []float64{1, 0}, 3)
	if len(got) == 0 {
		t.Fatal("hybrid search returned nothing")
	}
	found := false
	for _, s := range got {
		if s.Chunk.ID == "c2" {
			found = true
		}
	}
	if !found {
		t.Fatalf("c2 wins on both retrievers and must be in the fused top-3, got %v", ids(got))
	}
}

func ids(s []Scored) []string {
	out := make([]string, 0, len(s))
	for _, x := range s {
		out = append(out, x.Chunk.ID)
	}
	return out
}

// ---------- 4) Reranking ----------

// titleMatchReranker is a deterministic stand-in for a cross-encoder: it scores by how many
// query terms appear in the chunk TITLE, something the retrievers above never look at.
type titleMatchReranker struct{}

func (titleMatchReranker) Score(query string, c Chunk) float64 {
	score := 0.0
	title := Tokenize(c.Title)
	for _, q := range Tokenize(query) {
		for _, t := range title {
			if q == t {
				score++
			}
		}
	}
	return score
}

func TestRerankReordersCandidates(t *testing.T) {
	candidates := []Scored{
		{Chunk: Chunk{ID: "c4", Title: "Shipping"}, Score: 9.0},
		{Chunk: Chunk{ID: "c1", Title: "Refunds"}, Score: 8.0},
	}
	got := Rerank(titleMatchReranker{}, "refunds policy", candidates, 2)

	if got[0].Chunk.ID != "c1" {
		t.Fatalf("the reranker sees the title and must promote c1 above the higher-retrieval-scored "+
			"c4; got %v", ids(got))
	}
}

func TestRerankRespectsN(t *testing.T) {
	candidates := []Scored{
		{Chunk: Chunk{ID: "a", Title: "Refunds"}},
		{Chunk: Chunk{ID: "b", Title: "Refunds"}},
		{Chunk: Chunk{ID: "c", Title: "Refunds"}},
	}
	if got := Rerank(titleMatchReranker{}, "refunds", candidates, 2); len(got) != 2 {
		t.Fatalf("retrieve broad, rerank narrow: want 2, got %d", len(got))
	}
}

func TestRerankEmpty(t *testing.T) {
	if got := Rerank(titleMatchReranker{}, "q", nil, 5); len(got) != 0 {
		t.Fatalf("want nothing, got %v", got)
	}
}

// ---------- 5) Context assembly ----------

func TestAssembleRespectsTokenBudget(t *testing.T) {
	candidates := []Scored{
		{Chunk: Chunk{ID: "a", Tokens: 100}, Score: 0.9},
		{Chunk: Chunk{ID: "b", Tokens: 100}, Score: 0.8},
		{Chunk: Chunk{ID: "c", Tokens: 100}, Score: 0.7},
	}
	got := AssembleContext(candidates, ContextConfig{MaxTokens: 250, MaxChunks: 10})

	if got.TotalTokens > 250 {
		t.Fatalf("must not exceed the token budget, got %d", got.TotalTokens)
	}
	if len(got.Chunks) != 2 {
		t.Fatalf("250 tokens fits 2 x 100-token chunks, got %d", len(got.Chunks))
	}
	if got.Dropped != 1 {
		t.Fatalf("want 1 dropped, got %d", got.Dropped)
	}
}

func TestAssembleRelevanceFloor(t *testing.T) {
	// Returning NOTHING is a valid outcome — it lets the model say "I don't know" instead of
	// summarising an irrelevant chunk with total confidence.
	candidates := []Scored{
		{Chunk: Chunk{ID: "weak", Tokens: 10}, Score: 0.1},
		{Chunk: Chunk{ID: "weaker", Tokens: 10}, Score: 0.05},
	}
	got := AssembleContext(candidates, ContextConfig{MaxTokens: 1000, MinScore: 0.5, MaxChunks: 10})

	if len(got.Chunks) != 0 {
		t.Fatalf("everything is below the floor; want an empty context, got %v", got.Chunks)
	}
	if got.Dropped != 2 {
		t.Fatalf("want 2 dropped, got %d", got.Dropped)
	}
}

func TestAssembleMaxChunksCap(t *testing.T) {
	var candidates []Scored
	for i := 0; i < 20; i++ {
		candidates = append(candidates, Scored{
			Chunk: Chunk{ID: string(rune('a' + i)), Tokens: 1}, Score: 1.0,
		})
	}
	got := AssembleContext(candidates, ContextConfig{MaxTokens: 10000, MaxChunks: 5})

	if len(got.Chunks) != 5 {
		t.Fatalf("more context is not better — MaxChunks must cap at 5, got %d", len(got.Chunks))
	}
}

func TestAssembleDeduplicates(t *testing.T) {
	candidates := []Scored{
		{Chunk: Chunk{ID: "a", Tokens: 10}, Score: 0.9},
		{Chunk: Chunk{ID: "a", Tokens: 10}, Score: 0.8}, // same chunk from the other retriever
		{Chunk: Chunk{ID: "b", Tokens: 10}, Score: 0.7},
	}
	got := AssembleContext(candidates, ContextConfig{MaxTokens: 1000, MaxChunks: 10})

	if len(got.Chunks) != 2 {
		t.Fatalf("duplicate chunk IDs waste budget and must be collapsed; got %v", got.Chunks)
	}
}

func TestAssembleSkipsOversizedButKeepsScanning(t *testing.T) {
	candidates := []Scored{
		{Chunk: Chunk{ID: "huge", Tokens: 5000}, Score: 0.9},
		{Chunk: Chunk{ID: "small", Tokens: 50}, Score: 0.8},
	}
	got := AssembleContext(candidates, ContextConfig{MaxTokens: 100, MaxChunks: 10})

	if len(got.Chunks) != 1 || got.Chunks[0].ID != "small" {
		t.Fatalf("an oversized chunk must be skipped without blocking a smaller one behind it; got %v",
			got.Chunks)
	}
}

// ---------- 6) Citations ----------

func TestVerifyCitationsAllGrounded(t *testing.T) {
	ctx := AssembledContext{Chunks: []Chunk{{ID: "c1"}, {ID: "c2"}}}
	if got := VerifyCitations([]string{"c1", "c2"}, ctx); len(got) != 0 {
		t.Fatalf("all citations are in context; want none flagged, got %v", got)
	}
}

func TestVerifyCitationsCatchesFabrication(t *testing.T) {
	// The model cited a chunk it was never given. Deterministic hallucination detection,
	// for free.
	ctx := AssembledContext{Chunks: []Chunk{{ID: "c1"}}}
	got := VerifyCitations([]string{"c1", "c99"}, ctx)

	if len(got) != 1 || got[0] != "c99" {
		t.Fatalf("want [c99] flagged as ungrounded, got %v", got)
	}
}

func TestVerifyCitationsSorted(t *testing.T) {
	ctx := AssembledContext{}
	got := VerifyCitations([]string{"z", "a", "m"}, ctx)
	want := []string{"a", "m", "z"}
	for i := range want {
		if i >= len(got) || got[i] != want[i] {
			t.Fatalf("want sorted %v, got %v", want, got)
		}
	}
}
