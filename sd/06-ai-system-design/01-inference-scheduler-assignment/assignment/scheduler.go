// Package inferencescheduler is the Module 6.1 assignment: KV-cache accounting and a
// continuous-batching scheduler.
//
// Read 06-ai-system-design/01-llm-inference-and-serving.md first.
//
// Fill in every function marked `// TODO`. Run the tests until green:
//
//	go test ./...
//
// The framing: a GPU can serve N concurrent sequences, where N is set by KV-cache memory, not by
// CPU or by request count. The scheduler's real job is admission control — deciding who to let in
// given a memory budget, and what to do when memory runs out mid-generation.
package inferencescheduler

import (
	"sort"
)

// ModelConfig describes the shape of the model being served. These are the numbers that
// determine KV-cache size per token.
type ModelConfig struct {
	Layers int
	// KVHeads is the number of key/value heads. Grouped-query attention makes this much
	// smaller than the query-head count, which is why modern models are servable at all.
	KVHeads int
	HeadDim int
	// BytesPerElement is 2 for fp16, 1 for fp8/int8 KV-cache quantization.
	BytesPerElement int
}

// KVBytesPerToken returns the KV-cache bytes one token of one sequence consumes:
//
//	2 (K and V) x layers x kv_heads x head_dim x bytes_per_element
func (m ModelConfig) KVBytesPerToken() int64 {
	// TODO
	return 0
}

// KVBytesForSequence returns the KV bytes a sequence of the given token length consumes.
func (m ModelConfig) KVBytesForSequence(tokens int) int64 {
	// TODO
	return 0
}

// MaxConcurrentSequences returns how many sequences of avgTokens fit in the given GPU memory
// budget. This is the capacity number an interviewer is listening for.
// Returns 0 if a single sequence does not fit.
func (m ModelConfig) MaxConcurrentSequences(availableBytes int64, avgTokens int) int {
	// TODO
	return 0
}

// ----------------------------------------------------------------------------
// Requests
// ----------------------------------------------------------------------------

// Request is one generation request.
type Request struct {
	ID string
	// PromptTokens is the prefill length — known exactly up front.
	PromptTokens int
	// MaxOutputTokens is the caller's cap. Used for the WORST-CASE memory reservation,
	// because you cannot know the true output length until generation ends.
	MaxOutputTokens int
	// Priority: higher runs first among waiting requests.
	Priority int
	// arrival is the sequence number used to break priority ties in FIFO order.
	arrival int
}

// PeakTokens is the maximum sequence length this request can reach.
func (r Request) PeakTokens() int {
	return r.PromptTokens + r.MaxOutputTokens
}

// Running is an admitted request mid-generation.
type Running struct {
	Request Request
	// Generated is how many output tokens have been produced so far.
	Generated int
	// Done is true once the request finished (hit MaxOutputTokens or emitted a stop).
	Done bool
}

// CurrentTokens is the sequence's length right now.
func (r Running) CurrentTokens() int {
	return r.Request.PromptTokens + r.Generated
}

// ----------------------------------------------------------------------------
// The scheduler
//
// Continuous batching: a scheduling decision every decode step. A finished sequence frees its
// slot AND its memory immediately, and a waiting request takes them.
//
// Admission rule (this is the point of the exercise): admit only if the request's WORST-CASE
// KV footprint fits in the remaining budget. Admitting on current size instead would
// over-commit and cause an out-of-memory mid-generation, forcing you to throw away work.
// ----------------------------------------------------------------------------

// Scheduler runs continuous batching against a fixed KV-cache budget.
type Scheduler struct {
	model ModelConfig
	// kvBudgetBytes is the memory available for KV cache (total GPU memory minus weights).
	kvBudgetBytes int64
	// maxBatchSize caps concurrency independently of memory (kernels have a practical limit).
	maxBatchSize int

	waiting []Request
	running []*Running
	// reserved tracks the worst-case bytes committed to running requests.
	reserved int64
	arrivals int

	// Completed collects finished request IDs in completion order.
	Completed []string
}

// NewScheduler creates a scheduler.
func NewScheduler(model ModelConfig, kvBudgetBytes int64, maxBatchSize int) *Scheduler {
	return &Scheduler{
		model:         model,
		kvBudgetBytes: kvBudgetBytes,
		maxBatchSize:  maxBatchSize,
	}
}

// Submit enqueues a request.
func (s *Scheduler) Submit(r Request) {
	// TODO: stamp r.arrival from s.arrivals (then increment) and append to s.waiting.
}

// ReservedBytes is the worst-case KV memory currently committed.
func (s *Scheduler) ReservedBytes() int64 { return s.reserved }

// RunningIDs returns the IDs of currently running requests, in admission order.
func (s *Scheduler) RunningIDs() []string {
	// TODO
	return nil
}

// WaitingIDs returns queued request IDs in the order they would be admitted.
func (s *Scheduler) WaitingIDs() []string {
	// TODO: order by Priority DESC, then arrival ASC.
	return nil
}

// admit moves as many waiting requests into the batch as fit.
//
// Order: highest Priority first, then earliest arrival.
// Stop conditions: batch is full, or no remaining waiting request fits.
//
// IMPORTANT: do NOT stop at the first request that does not fit — a huge request must not
// head-of-line block a small one that would fit. Keep scanning. (This is the same
// head-of-line blocking idea as 1.2/2.11, in a new setting.)
func (s *Scheduler) admit() int {
	// TODO:
	//  1. Sort a copy of waiting by (Priority DESC, arrival ASC).
	//  2. Walk it; for each request, if len(running) < maxBatchSize AND
	//     reserved + worstCase(req) <= kvBudgetBytes, admit it:
	//     append to running, add to reserved, and remove it from waiting.
	//  3. Return how many were admitted.
	return 0
}

// Step runs one decode iteration. Returns the number of requests that completed.
//
// Order matters, and it is the definition of continuous batching:
//
//	1. admit()   - fill any idle capacity BEFORE doing work
//	2. advance   - every running sequence generates one token
//	3. complete  - sequences that hit MaxOutputTokens finish and RELEASE their reservation
//	4. admit()   - refill the just-freed capacity in this SAME step
//
// Step 4 is the whole point. Static batching would wait for the entire batch to drain.
func (s *Scheduler) Step() int {
	// TODO:
	//  1. Call admit().
	//  2. For each running request: Generated++; if Generated >= MaxOutputTokens, mark Done,
	//     append the ID to s.Completed, and subtract its worst-case bytes from reserved.
	//  3. Remove Done entries from running (preserve relative order of the rest).
	//  4. Call admit() again.
	//  5. Return the completion count.
	return 0
}

// RunToCompletion steps until everything finishes or maxSteps is reached.
// Returns the number of steps actually taken.
func (s *Scheduler) RunToCompletion(maxSteps int) int {
	// TODO: loop while there is work (waiting or running) and steps < maxSteps.
	return 0
}

// ----------------------------------------------------------------------------
// Comparing batching strategies
//
// Static batching runs a fixed batch to completion before starting the next one, so the batch
// costs as long as its SLOWEST member. This function quantifies what continuous batching saves.
// ----------------------------------------------------------------------------

// StaticBatchSteps returns the decode steps a static scheduler needs: requests are taken in
// arrival order in groups of batchSize, and each group costs max(MaxOutputTokens) in the group.
func StaticBatchSteps(reqs []Request, batchSize int) int {
	// TODO
	return 0
}

var _ = sort.Slice
