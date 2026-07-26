package inferencescheduler

import (
	"testing"
)

// llama70B mirrors the worked example in the lesson: 80 layers, 8 KV heads (GQA),
// head_dim 128, fp16 => 0.33 MB per token.
func llama70B() ModelConfig {
	return ModelConfig{Layers: 80, KVHeads: 8, HeadDim: 128, BytesPerElement: 2}
}

const mb = int64(1) << 20
const gb = int64(1) << 30

// ---------- KV math ----------

func TestKVBytesPerToken(t *testing.T) {
	// 2 x 80 x 8 x 128 x 2 = 327,680
	if got := llama70B().KVBytesPerToken(); got != 327680 {
		t.Fatalf("want 327680 bytes/token, got %d", got)
	}
}

func TestKVCacheQuantizationHalvesIt(t *testing.T) {
	fp16 := llama70B()
	fp8 := fp16
	fp8.BytesPerElement = 1

	if fp8.KVBytesPerToken()*2 != fp16.KVBytesPerToken() {
		t.Fatal("halving bytes-per-element must halve the KV cache — this is the whole point " +
			"of KV quantization")
	}
}

func TestGQAReducesKVCache(t *testing.T) {
	// Without grouped-query attention a 64-head model would use 8x the KV cache of an
	// 8-KV-head one. This is why GQA made large-context serving practical.
	gqa := llama70B()
	mha := gqa
	mha.KVHeads = 64

	if mha.KVBytesPerToken() != gqa.KVBytesPerToken()*8 {
		t.Fatalf("64 KV heads should cost 8x of 8 KV heads; got %d vs %d",
			mha.KVBytesPerToken(), gqa.KVBytesPerToken())
	}
}

func TestKVBytesForSequence(t *testing.T) {
	m := llama70B()
	// 8,000 tokens x 327,680 = 2,621,440,000 ≈ 2.6 GB, matching the lesson.
	want := int64(8000) * 327680
	if got := m.KVBytesForSequence(8000); got != want {
		t.Fatalf("want %d, got %d", want, got)
	}
}

func TestMaxConcurrentSequences(t *testing.T) {
	m := llama70B()
	// 10 GiB = 10,737,418,240 bytes; each 8,000-token sequence costs 2,621,440,000.
	// 10,737,418,240 / 2,621,440,000 = 4.09 => 4 fit.
	if got := m.MaxConcurrentSequences(10*gb, 8000); got != 4 {
		t.Fatalf("10GiB / 2.62GB per sequence = 4 concurrent, got %d", got)
	}
}

func TestMaxConcurrentSequencesTooSmall(t *testing.T) {
	m := llama70B()
	if got := m.MaxConcurrentSequences(100*mb, 8000); got != 0 {
		t.Fatalf("a single 2.6GB sequence does not fit in 100MB, want 0, got %d", got)
	}
}

// ---------- Admission control ----------

// tiny keeps the arithmetic readable: 1 layer, 1 head, head_dim 1, 1 byte
// => 2 bytes per token.
func tiny() ModelConfig {
	return ModelConfig{Layers: 1, KVHeads: 1, HeadDim: 1, BytesPerElement: 1}
}

func req(id string, prompt, out, prio int) Request {
	return Request{ID: id, PromptTokens: prompt, MaxOutputTokens: out, Priority: prio}
}

func TestAdmitsUpToMemoryBudget(t *testing.T) {
	// 2 bytes/token. Each request peaks at 100 tokens => 200 bytes worst case.
	// Budget 500 bytes => 2 fit, the third must wait.
	s := NewScheduler(tiny(), 500, 10)
	s.Submit(req("a", 50, 50, 0))
	s.Submit(req("b", 50, 50, 0))
	s.Submit(req("c", 50, 50, 0))
	s.Step()

	if got := len(s.RunningIDs()); got != 2 {
		t.Fatalf("500 bytes / 200 per request = 2 admitted, got %d (%v)", got, s.RunningIDs())
	}
	if got := s.ReservedBytes(); got != 400 {
		t.Fatalf("want 400 bytes reserved, got %d", got)
	}
}

func TestReservesWorstCaseNotCurrentSize(t *testing.T) {
	// A request that has generated nothing yet still reserves prompt+maxOutput. Reserving
	// only the current size would over-admit and OOM mid-generation.
	s := NewScheduler(tiny(), 1000, 10)
	s.Submit(req("a", 10, 90, 0)) // peak 100 tokens => 200 bytes
	s.Step()

	if got := s.ReservedBytes(); got != 200 {
		t.Fatalf("must reserve the worst case (200), not the current size (~22); got %d", got)
	}
}

func TestRespectsMaxBatchSize(t *testing.T) {
	s := NewScheduler(tiny(), 1_000_000, 3) // memory is plentiful; the cap is the batch size
	for _, id := range []string{"a", "b", "c", "d", "e"} {
		s.Submit(req(id, 10, 10, 0))
	}
	s.Step()

	if got := len(s.RunningIDs()); got != 3 {
		t.Fatalf("maxBatchSize 3 must cap concurrency regardless of memory, got %d", got)
	}
}

func TestPriorityOrdering(t *testing.T) {
	s := NewScheduler(tiny(), 400, 10) // room for 2 requests of 100 peak tokens
	s.Submit(req("low", 50, 50, 0))
	s.Submit(req("high", 50, 50, 9))
	s.Submit(req("mid", 50, 50, 5))
	s.Step()

	running := s.RunningIDs()
	if len(running) != 2 {
		t.Fatalf("want 2 admitted, got %d", len(running))
	}
	seen := map[string]bool{running[0]: true, running[1]: true}
	if !seen["high"] || !seen["mid"] {
		t.Fatalf("highest priority first: want high+mid admitted, got %v", running)
	}
}

func TestFIFOWithinSamePriority(t *testing.T) {
	s := NewScheduler(tiny(), 200, 10) // room for exactly 1
	s.Submit(req("first", 50, 50, 5))
	s.Submit(req("second", 50, 50, 5))
	s.Step()

	if got := s.RunningIDs(); len(got) != 1 || got[0] != "first" {
		t.Fatalf("equal priority must break FIFO by arrival, got %v", got)
	}
}

func TestLargeRequestDoesNotBlockSmallOne(t *testing.T) {
	// The head-of-line blocking case. "huge" cannot fit; the scheduler must keep scanning
	// and admit "small" rather than stalling the queue behind it.
	s := NewScheduler(tiny(), 300, 10)
	s.Submit(req("huge", 500, 500, 9)) // 2000 bytes — never fits
	s.Submit(req("small", 10, 10, 0))  // 40 bytes
	s.Step()

	running := s.RunningIDs()
	if len(running) != 1 || running[0] != "small" {
		t.Fatalf("a request that does not fit must not head-of-line block one that does; got %v", running)
	}
}

// ---------- Continuous batching ----------

func TestCompletionFreesMemoryForTheNextRequest(t *testing.T) {
	// This is the definition of continuous batching: the freed slot is refilled immediately,
	// not after the whole batch drains.
	s := NewScheduler(tiny(), 200, 10) // exactly one 100-peak-token request at a time
	s.Submit(req("short", 50, 2, 0))   // peak 52 => 104 bytes
	s.Submit(req("next", 50, 50, 0))   // peak 100 => 200 bytes

	s.Step() // short admitted (104 <= 200); next does not fit alongside it
	if got := s.RunningIDs(); len(got) != 1 || got[0] != "short" {
		t.Fatalf("setup: want only 'short' running, got %v", got)
	}

	s.Step() // short completes here (2 output tokens) and frees its reservation
	if len(s.Completed) != 1 || s.Completed[0] != "short" {
		t.Fatalf("'short' should have completed, got %v", s.Completed)
	}
	if got := s.RunningIDs(); len(got) != 1 || got[0] != "next" {
		t.Fatalf("the freed memory must admit 'next' in the SAME step; got %v", got)
	}
}

func TestReservationReleasedOnCompletion(t *testing.T) {
	s := NewScheduler(tiny(), 10_000, 10)
	s.Submit(req("a", 10, 3, 0))
	s.Step()
	if s.ReservedBytes() == 0 {
		t.Fatal("setup: memory should be reserved while running")
	}
	s.RunToCompletion(100)
	if got := s.ReservedBytes(); got != 0 {
		t.Fatalf("all memory must be released once everything finishes, got %d — this is a leak", got)
	}
}

func TestRunToCompletionFinishesEverything(t *testing.T) {
	s := NewScheduler(tiny(), 400, 2)
	for _, id := range []string{"a", "b", "c", "d", "e"} {
		s.Submit(req(id, 10, 5, 0))
	}
	s.RunToCompletion(1000)

	if len(s.Completed) != 5 {
		t.Fatalf("want all 5 completed, got %d (%v)", len(s.Completed), s.Completed)
	}
	if len(s.RunningIDs()) != 0 || len(s.WaitingIDs()) != 0 {
		t.Fatal("nothing should remain running or waiting")
	}
}

func TestRunToCompletionRespectsMaxSteps(t *testing.T) {
	s := NewScheduler(tiny(), 10_000, 10)
	s.Submit(req("long", 10, 1000, 0))
	if got := s.RunToCompletion(5); got != 5 {
		t.Fatalf("want exactly 5 steps, got %d", got)
	}
	if len(s.Completed) != 0 {
		t.Fatal("nothing should have completed in 5 steps")
	}
}

// ---------- Continuous vs static ----------

func TestContinuousBatchingBeatsStatic(t *testing.T) {
	// The lesson's scenario: wildly uneven output lengths. Static batching runs each group at
	// the speed of its slowest member; continuous batching refills slots as they free.
	reqs := []Request{
		req("a", 10, 300, 0),
		req("b", 10, 10, 0),
		req("c", 10, 10, 0),
		req("d", 10, 10, 0),
		req("e", 10, 300, 0),
		req("f", 10, 10, 0),
	}

	static := StaticBatchSteps(reqs, 3)

	s := NewScheduler(tiny(), 10_000_000, 3)
	for _, r := range reqs {
		s.Submit(r)
	}
	continuous := s.RunToCompletion(10_000)

	if len(s.Completed) != len(reqs) {
		t.Fatalf("all requests must finish, got %d", len(s.Completed))
	}
	if continuous >= static {
		t.Fatalf("continuous batching should need fewer steps than static (%d) with uneven "+
			"output lengths, got %d", static, continuous)
	}
}

func TestStaticBatchStepsCostsTheSlowestInEachGroup(t *testing.T) {
	reqs := []Request{
		req("a", 10, 100, 0),
		req("b", 10, 10, 0), // group 1 costs 100
		req("c", 10, 50, 0),
		req("d", 10, 5, 0), // group 2 costs 50
	}
	if got := StaticBatchSteps(reqs, 2); got != 150 {
		t.Fatalf("want 100 + 50 = 150, got %d", got)
	}
}

func TestStaticBatchStepsEmpty(t *testing.T) {
	if got := StaticBatchSteps(nil, 4); got != 0 {
		t.Fatalf("want 0, got %d", got)
	}
}
