// Package inferencescheduler is the reference solution for Module 6.1.
//
// Points worth defending in an interview:
//
//   - Capacity is KV-cache memory, not request count. "How many concurrent users per GPU?" is
//     answered by arithmetic, and MaxConcurrentSequences is that arithmetic.
//
//   - Admission reserves the WORST CASE (prompt + max output). You cannot know the true output
//     length in advance, and admitting against current size over-commits — the sequence grows a
//     token per step and eventually there is no memory left, forcing you to evict live work.
//
//   - A request that does not fit must not head-of-line block one that does. Scanning past it is
//     the difference between a queue that drains and a queue that stalls behind one big job.
//
//   - Completion releases memory immediately and the freed budget is re-admitted in the SAME
//     step. That is the entire difference between continuous and static batching, and it is
//     worth 2-4x throughput on realistic (uneven) output lengths.
package inferencescheduler

import (
	"sort"
)

// ModelConfig describes the shape of the model being served.
type ModelConfig struct {
	Layers          int
	KVHeads         int
	HeadDim         int
	BytesPerElement int
}

// KVBytesPerToken returns the KV-cache bytes one token of one sequence consumes.
func (m ModelConfig) KVBytesPerToken() int64 {
	// 2 covers K and V; everything else is model geometry.
	return 2 * int64(m.Layers) * int64(m.KVHeads) * int64(m.HeadDim) * int64(m.BytesPerElement)
}

// KVBytesForSequence returns the KV bytes a sequence of the given token length consumes.
func (m ModelConfig) KVBytesForSequence(tokens int) int64 {
	if tokens <= 0 {
		return 0
	}
	return m.KVBytesPerToken() * int64(tokens)
}

// MaxConcurrentSequences returns how many sequences of avgTokens fit in a memory budget.
func (m ModelConfig) MaxConcurrentSequences(availableBytes int64, avgTokens int) int {
	per := m.KVBytesForSequence(avgTokens)
	if per <= 0 {
		return 0
	}
	return int(availableBytes / per)
}

// Request is one generation request.
type Request struct {
	ID              string
	PromptTokens    int
	MaxOutputTokens int
	Priority        int
	arrival         int
}

// PeakTokens is the maximum sequence length this request can reach.
func (r Request) PeakTokens() int {
	return r.PromptTokens + r.MaxOutputTokens
}

// Running is an admitted request mid-generation.
type Running struct {
	Request   Request
	Generated int
	Done      bool
}

// CurrentTokens is the sequence's length right now.
func (r Running) CurrentTokens() int {
	return r.Request.PromptTokens + r.Generated
}

// Scheduler runs continuous batching against a fixed KV-cache budget.
type Scheduler struct {
	model         ModelConfig
	kvBudgetBytes int64
	maxBatchSize  int

	waiting  []Request
	running  []*Running
	reserved int64
	arrivals int

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
	r.arrival = s.arrivals
	s.arrivals++
	s.waiting = append(s.waiting, r)
}

// ReservedBytes is the worst-case KV memory currently committed.
func (s *Scheduler) ReservedBytes() int64 { return s.reserved }

// worstCase is the memory a request must have reserved for its entire lifetime.
func (s *Scheduler) worstCase(r Request) int64 {
	return s.model.KVBytesForSequence(r.PeakTokens())
}

// RunningIDs returns the IDs of currently running requests, in admission order.
func (s *Scheduler) RunningIDs() []string {
	ids := make([]string, 0, len(s.running))
	for _, r := range s.running {
		ids = append(ids, r.Request.ID)
	}
	return ids
}

// admissionOrder returns waiting requests sorted by priority then arrival.
func (s *Scheduler) admissionOrder() []Request {
	order := make([]Request, len(s.waiting))
	copy(order, s.waiting)
	sort.Slice(order, func(i, j int) bool {
		if order[i].Priority != order[j].Priority {
			return order[i].Priority > order[j].Priority
		}
		return order[i].arrival < order[j].arrival // FIFO within a priority
	})
	return order
}

// WaitingIDs returns queued request IDs in the order they would be admitted.
func (s *Scheduler) WaitingIDs() []string {
	order := s.admissionOrder()
	ids := make([]string, 0, len(order))
	for _, r := range order {
		ids = append(ids, r.ID)
	}
	return ids
}

// admit moves as many waiting requests into the batch as fit.
func (s *Scheduler) admit() int {
	admitted := 0
	admittedIDs := make(map[string]bool)

	for _, r := range s.admissionOrder() {
		if len(s.running) >= s.maxBatchSize {
			break // no more slots, regardless of memory
		}
		need := s.worstCase(r)
		if s.reserved+need > s.kvBudgetBytes {
			// Keep scanning: a request that does not fit must not head-of-line block a
			// smaller one behind it.
			continue
		}
		s.running = append(s.running, &Running{Request: r})
		s.reserved += need
		admittedIDs[r.ID] = true
		admitted++
	}

	if admitted > 0 {
		remaining := s.waiting[:0]
		for _, r := range s.waiting {
			if !admittedIDs[r.ID] {
				remaining = append(remaining, r)
			}
		}
		s.waiting = remaining
	}
	return admitted
}

// Step runs one decode iteration: admit, advance, complete, admit again.
func (s *Scheduler) Step() int {
	completed := 0

	s.admit() // fill idle capacity before doing work

	for _, r := range s.running {
		r.Generated++
		if r.Generated >= r.Request.MaxOutputTokens {
			r.Done = true
			s.Completed = append(s.Completed, r.Request.ID)
			// Release the full reservation now — this freed budget is what lets admit()
			// refill the slot in this same step.
			s.reserved -= s.worstCase(r.Request)
			completed++
		}
	}

	if completed > 0 {
		alive := s.running[:0]
		for _, r := range s.running {
			if !r.Done {
				alive = append(alive, r)
			}
		}
		s.running = alive
	}

	s.admit()
	return completed
}

// RunToCompletion steps until everything finishes or maxSteps is reached.
func (s *Scheduler) RunToCompletion(maxSteps int) int {
	steps := 0
	for steps < maxSteps && (len(s.running) > 0 || len(s.waiting) > 0) {
		s.Step()
		steps++
	}
	return steps
}

// StaticBatchSteps returns the decode steps a static scheduler needs.
func StaticBatchSteps(reqs []Request, batchSize int) int {
	if batchSize < 1 {
		batchSize = 1
	}
	total := 0
	for i := 0; i < len(reqs); i += batchSize {
		end := i + batchSize
		if end > len(reqs) {
			end = len(reqs)
		}
		// The group costs its slowest member; every other slot idles for the remainder.
		longest := 0
		for _, r := range reqs[i:end] {
			if r.MaxOutputTokens > longest {
				longest = r.MaxOutputTokens
			}
		}
		total += longest
	}
	return total
}
