// Package ragretrieval is the reference solution for Module 6.3.
//
// Points worth defending in an interview:
//
//   - Hybrid retrieval is not an optimisation, it is the baseline. Dense retrieval cannot find
//     ERR_4021; BM25 cannot find "my payment bounced" -> "transaction declined". You need both.
//
//   - RRF fuses RANKS. BM25 scores and cosine similarities are on incomparable scales that move
//     with corpus and query, so a weighted-score blend needs perpetual retuning. Rank fusion
//     needs none — that tuning-free property is the entire reason it is the default.
//
//   - Retrieve broad, rerank narrow. A cross-encoder sees the (query, doc) pair together and is
//     much more accurate, but cannot run over a corpus. Two stages get you both.
//
//   - A relevance floor that yields an EMPTY context is a feature. It is what lets the system
//     say "I don't know" instead of fluently summarising an irrelevant chunk.
//
//   - Citation verification is deterministic hallucination detection, and it is free.
package ragretrieval

import (
	"math"
	"sort"
	"strings"
)

// Chunk is one indexed passage.
type Chunk struct {
	ID        string
	Text      string
	Title     string
	Tokens    int
	Embedding []float64
}

// Scored is a chunk with a retrieval score.
type Scored struct {
	Chunk Chunk
	Score float64
}

const (
	BM25K1 = 1.2
	BM25B  = 0.75
)

// Tokenize lowercases and splits on non-alphanumeric runs.
func Tokenize(s string) []string {
	return strings.FieldsFunc(strings.ToLower(s), func(r rune) bool {
		return !(r >= 'a' && r <= 'z') && !(r >= 'A' && r <= 'Z') && !(r >= '0' && r <= '9')
	})
}

// BM25Index is a prebuilt lexical index.
type BM25Index struct {
	chunks    []Chunk
	termFreq  []map[string]int
	docFreq   map[string]int
	docLen    []int
	avgDocLen float64
}

// NewBM25Index builds the index.
func NewBM25Index(chunks []Chunk) *BM25Index {
	idx := &BM25Index{
		chunks:   chunks,
		termFreq: make([]map[string]int, len(chunks)),
		docFreq:  make(map[string]int),
		docLen:   make([]int, len(chunks)),
	}

	totalLen := 0
	for i, c := range chunks {
		tokens := Tokenize(c.Text)
		tf := make(map[string]int, len(tokens))
		for _, t := range tokens {
			tf[t]++
		}
		idx.termFreq[i] = tf
		idx.docLen[i] = len(tokens)
		totalLen += len(tokens)

		// Document frequency counts each term ONCE per chunk, however often it occurs.
		for term := range tf {
			idx.docFreq[term]++
		}
	}

	if len(chunks) > 0 {
		idx.avgDocLen = float64(totalLen) / float64(len(chunks))
	}
	return idx
}

// Search returns the top-k chunks by BM25 score.
func (idx *BM25Index) Search(query string, k int) []Scored {
	if len(idx.chunks) == 0 || k <= 0 {
		return nil
	}

	terms := Tokenize(query)
	n := float64(len(idx.chunks))
	var results []Scored

	for i, c := range idx.chunks {
		score := 0.0
		for _, term := range terms {
			f := float64(idx.termFreq[i][term])
			if f == 0 {
				continue
			}
			df := float64(idx.docFreq[term])
			// Rare terms carry far more signal than common ones.
			idf := math.Log(1 + (n-df+0.5)/(df+0.5))

			// Length normalisation: a term in a short document counts for more.
			norm := 1 - BM25B + BM25B*float64(idx.docLen[i])/idx.avgDocLen
			score += idf * (f * (BM25K1 + 1)) / (f + BM25K1*norm)
		}
		if score > 0 { // a chunk sharing no term with the query is not a result
			results = append(results, Scored{Chunk: c, Score: score})
		}
	}

	sortScored(results)
	return truncate(results, k)
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
		return 0 // never NaN — a NaN would poison every downstream comparison
	}
	return dot / (math.Sqrt(magA) * math.Sqrt(magB))
}

// VectorSearch returns the top-k chunks by cosine similarity.
func VectorSearch(chunks []Chunk, queryEmbedding []float64, k int) []Scored {
	if k <= 0 {
		return nil
	}
	results := make([]Scored, 0, len(chunks))
	for _, c := range chunks {
		results = append(results, Scored{Chunk: c, Score: CosineSimilarity(c.Embedding, queryEmbedding)})
	}
	sortScored(results)
	return truncate(results, k)
}

// DefaultRRFK is the conventional constant.
const DefaultRRFK = 60.0

// ReciprocalRankFusion merges ranked lists into one.
func ReciprocalRankFusion(lists [][]Scored, rrfK float64) []Scored {
	scores := make(map[string]float64)
	chunks := make(map[string]Chunk)

	for _, list := range lists {
		for i, s := range list {
			rank := float64(i + 1) // 1-based
			// Only the position is used — raw scores never enter the fusion, which is
			// precisely why this needs no normalisation or tuning.
			scores[s.Chunk.ID] += 1.0 / (rrfK + rank)
			chunks[s.Chunk.ID] = s.Chunk
		}
	}

	fused := make([]Scored, 0, len(scores))
	for id, score := range scores {
		fused = append(fused, Scored{Chunk: chunks[id], Score: score})
	}
	sortScored(fused)
	return fused
}

// HybridSearch runs both retrievers and fuses them.
func HybridSearch(idx *BM25Index, chunks []Chunk, query string, queryEmbedding []float64, k int) []Scored {
	lexical := idx.Search(query, k)
	dense := VectorSearch(chunks, queryEmbedding, k)
	return truncate(ReciprocalRankFusion([][]Scored{lexical, dense}, DefaultRRFK), k)
}

// Reranker scores a (query, chunk) pair.
type Reranker interface {
	Score(query string, c Chunk) float64
}

// Rerank rescores candidates and returns the top n.
func Rerank(r Reranker, query string, candidates []Scored, n int) []Scored {
	if n <= 0 || len(candidates) == 0 {
		return nil
	}
	rescored := make([]Scored, 0, len(candidates))
	for _, c := range candidates {
		rescored = append(rescored, Scored{Chunk: c.Chunk, Score: r.Score(query, c.Chunk)})
	}
	sortScored(rescored)
	return truncate(rescored, n)
}

// ContextConfig controls assembly.
type ContextConfig struct {
	MaxTokens int
	MinScore  float64
	MaxChunks int
}

// AssembledContext is the result of assembly.
type AssembledContext struct {
	Chunks      []Chunk
	TotalTokens int
	Dropped     int
}

// AssembleContext selects chunks to place in the prompt.
func AssembleContext(candidates []Scored, cfg ContextConfig) AssembledContext {
	out := AssembledContext{}
	seen := make(map[string]bool)

	for _, c := range candidates {
		if c.Score < cfg.MinScore {
			out.Dropped++
			continue
		}
		if seen[c.Chunk.ID] {
			out.Dropped++ // the same chunk from both retrievers; including it twice wastes budget
			continue
		}
		if cfg.MaxChunks > 0 && len(out.Chunks) >= cfg.MaxChunks {
			out.Dropped++
			continue
		}
		if out.TotalTokens+c.Chunk.Tokens > cfg.MaxTokens {
			// Skip but keep scanning: a later, smaller chunk may still fit.
			out.Dropped++
			continue
		}

		seen[c.Chunk.ID] = true
		out.Chunks = append(out.Chunks, c.Chunk)
		out.TotalTokens += c.Chunk.Tokens
	}
	return out
}

// VerifyCitations returns the cited IDs that were NOT in the supplied context.
func VerifyCitations(cited []string, context AssembledContext) []string {
	inContext := make(map[string]bool, len(context.Chunks))
	for _, c := range context.Chunks {
		inContext[c.ID] = true
	}

	var ungrounded []string
	for _, id := range cited {
		if !inContext[id] {
			ungrounded = append(ungrounded, id)
		}
	}
	sort.Strings(ungrounded)
	return ungrounded
}

// sortScored orders by score descending, breaking ties on chunk ID so results are stable.
func sortScored(s []Scored) {
	sort.Slice(s, func(i, j int) bool {
		if s[i].Score != s[j].Score {
			return s[i].Score > s[j].Score
		}
		return s[i].Chunk.ID < s[j].Chunk.ID
	})
}

func truncate(s []Scored, k int) []Scored {
	if k > 0 && len(s) > k {
		return s[:k]
	}
	return s
}
