// Package ragretrieval is the Module 6.3 assignment: the online query pipeline of a RAG system.
//
// Read 06-ai-system-design/03-rag-architecture.md first.
//
// Fill in every function marked `// TODO`. Run the tests until green:
//
//	go test ./...
//
// You will build the pipeline end to end:
//
//	query → BM25 (lexical) ─┐
//	                        ├─► RRF fusion → rerank → context assembly
//	query → vector (dense) ─┘
//
// Everything here is DETERMINISTIC — no model calls. That is the point: the retrieval half of
// RAG is ordinary, testable software, and it is where most RAG bugs actually live.
package ragretrieval

import (
	"math"
	"sort"
	"strings"
)

// Chunk is one indexed passage.
type Chunk struct {
	ID     string
	Text   string
	Title  string
	Tokens int
	// Embedding is the dense vector. Assumed normalised in the tests.
	Embedding []float64
}

// Scored is a chunk with a retrieval score.
type Scored struct {
	Chunk Chunk
	Score float64
}

// ----------------------------------------------------------------------------
// 1) Lexical retrieval — BM25
//
// BM25 is what finds exact tokens embeddings are bad at: error codes, SKUs, names.
//
//	score(D,Q) = Σ  IDF(q) · ( f(q,D)·(k1+1) ) / ( f(q,D) + k1·(1 - b + b·|D|/avgdl) )
//	            q∈Q
//
//	IDF(q) = ln( 1 + (N - n(q) + 0.5) / (n(q) + 0.5) )
//
// where f(q,D) is the term frequency in D, |D| the document length, avgdl the average document
// length, N the corpus size, and n(q) the number of documents containing q.
// Standard parameters: k1 = 1.2 (term-frequency saturation), b = 0.75 (length normalisation).
// ----------------------------------------------------------------------------

const (
	BM25K1 = 1.2
	BM25B  = 0.75
)

// Tokenize lowercases and splits on non-letter/digit runs. Deliberately simple and
// deterministic — a real system would stem and drop stopwords.
func Tokenize(s string) []string {
	return strings.FieldsFunc(strings.ToLower(s), func(r rune) bool {
		return !(r >= 'a' && r <= 'z') && !(r >= 'A' && r <= 'Z') && !(r >= '0' && r <= '9')
	})
}

// BM25Index is a prebuilt lexical index.
type BM25Index struct {
	chunks []Chunk
	// termFreq[chunkIdx][term] = count
	termFreq []map[string]int
	// docFreq[term] = number of chunks containing it
	docFreq map[string]int
	// docLen[chunkIdx] = token count
	docLen []int
	avgDocLen float64
}

// NewBM25Index builds the index.
func NewBM25Index(chunks []Chunk) *BM25Index {
	// TODO:
	//  1. Tokenize each chunk's Text.
	//  2. Fill termFreq (per chunk), docLen (per chunk), and docFreq (corpus-wide;
	//     count each term ONCE per chunk, not once per occurrence).
	//  3. Compute avgDocLen (0 for an empty corpus — guard the division).
	return &BM25Index{}
}

// Search returns the top-k chunks by BM25 score, highest first.
// Ties break on chunk ID ascending so results are deterministic.
// Chunks scoring 0 (no query term present) are excluded.
func (idx *BM25Index) Search(query string, k int) []Scored {
	// TODO:
	//  1. Tokenize the query.
	//  2. For each chunk, sum the BM25 contribution of each query term.
	//  3. Drop zero scores, sort by score DESC then ID ASC, return the top k.
	return nil
}

// ----------------------------------------------------------------------------
// 2) Dense retrieval — cosine similarity
// ----------------------------------------------------------------------------

// CosineSimilarity returns the cosine of the angle between two vectors.
// Returns 0 for mismatched lengths or a zero-magnitude vector (rather than NaN, which would
// poison every downstream sort).
func CosineSimilarity(a, b []float64) float64 {
	// TODO
	return 0
}

// VectorSearch returns the top-k chunks by cosine similarity to the query embedding.
// Ties break on chunk ID ascending.
func VectorSearch(chunks []Chunk, queryEmbedding []float64, k int) []Scored {
	// TODO
	return nil
}

// ----------------------------------------------------------------------------
// 3) Reciprocal Rank Fusion
//
//	RRF(d) = Σ  1 / (rrfK + rank_i(d))       ranks are 1-based
//	        i∈lists
//
// The key property: it fuses RANKS, never scores. BM25 scores and cosine similarities live on
// incomparable scales that shift with corpus and query, so any weighted-score blend needs
// constant retuning. RRF needs none.
//
// A document missing from a list simply contributes nothing from that list.
// ----------------------------------------------------------------------------

// DefaultRRFK is the conventional constant. It damps the influence of top ranks so a single
// list cannot dominate the fusion.
const DefaultRRFK = 60.0

// ReciprocalRankFusion merges ranked lists into one.
// Returns all fused documents sorted by RRF score DESC, ties broken by chunk ID ASC.
func ReciprocalRankFusion(lists [][]Scored, rrfK float64) []Scored {
	// TODO:
	//  1. For each list, walk it in order; rank is index+1.
	//  2. Accumulate 1/(rrfK+rank) per chunk ID.
	//  3. Sort by accumulated score DESC, ID ASC. The Score field holds the RRF score.
	return nil
}

// HybridSearch runs both retrievers and fuses them. This is the function the lesson argues
// every production RAG system converges on.
func HybridSearch(idx *BM25Index, chunks []Chunk, query string, queryEmbedding []float64, k int) []Scored {
	// TODO: run BM25 and vector search for k candidates each, fuse with DefaultRRFK,
	//       return the top k of the fused list.
	return nil
}

// ----------------------------------------------------------------------------
// 4) Reranking
//
// A cross-encoder sees (query, document) together and is far more accurate than the
// bi-encoder used for retrieval — but too slow to run over the corpus. So: retrieve broad,
// rerank narrow.
//
// The tests supply a deterministic stub in place of a real model.
// ----------------------------------------------------------------------------

// Reranker scores a (query, chunk) pair. Higher is more relevant.
type Reranker interface {
	Score(query string, c Chunk) float64
}

// Rerank rescores candidates with the reranker and returns the top n.
// Ties break on chunk ID ascending.
func Rerank(r Reranker, query string, candidates []Scored, n int) []Scored {
	// TODO
	return nil
}

// ----------------------------------------------------------------------------
// 5) Context assembly
//
// The last mile, and the one people skip. Decisions that matter:
//   - A relevance FLOOR: chunks below it are dropped. Returning nothing is a valid, useful
//     answer — it lets the model say "I don't know" instead of inventing something.
//   - A token BUDGET: stop before overflowing the prompt.
//   - DEDUPLICATION: overlapping chunks repeat text and waste budget.
// ----------------------------------------------------------------------------

// ContextConfig controls assembly.
type ContextConfig struct {
	MaxTokens    int
	MinScore     float64
	// MaxChunks caps the count even if the token budget allows more. More context is not
	// better — irrelevant chunks measurably degrade answers.
	MaxChunks int
}

// AssembledContext is the result of assembly.
type AssembledContext struct {
	Chunks []Chunk
	// TotalTokens is the summed Tokens of the included chunks.
	TotalTokens int
	// Dropped counts candidates excluded for any reason (floor, budget, cap, duplicate).
	Dropped int
}

// AssembleContext selects chunks to place in the prompt.
//
// Rules, in order:
//  1. Drop any candidate scoring below MinScore.
//  2. Drop duplicates by chunk ID (keep the first, highest-scoring occurrence).
//  3. Add in score order while TotalTokens+chunk.Tokens <= MaxTokens and len < MaxChunks.
//  4. A chunk that does not fit is skipped, but keep scanning — a later, smaller chunk may
//     still fit. (Same reasoning as the scheduler in 6.1: do not head-of-line block.)
func AssembleContext(candidates []Scored, cfg ContextConfig) AssembledContext {
	// TODO
	return AssembledContext{}
}

// ----------------------------------------------------------------------------
// 6) Citation verification
//
// The cheapest hallucination check there is: every ID the model cites must have actually been
// in the context you supplied. Fully deterministic, no LLM required.
// ----------------------------------------------------------------------------

// VerifyCitations returns the cited IDs that were NOT in the supplied context, sorted.
// An empty result means every citation is grounded.
func VerifyCitations(cited []string, context AssembledContext) []string {
	// TODO
	return nil
}

var (
	_ = math.Log
	_ = sort.Slice
)
