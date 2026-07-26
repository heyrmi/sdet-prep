package linearizability

import (
	"testing"
)

func w(id int, client string, value, invoked, completed int) Operation {
	return Operation{ID: id, ClientID: client, Type: Write, Value: value,
		Invoked: invoked, Completed: completed, Status: OK}
}

func r(id int, client string, value, invoked, completed int) Operation {
	return Operation{ID: id, ClientID: client, Type: Read, Value: value,
		Invoked: invoked, Completed: completed, Status: OK}
}

func indeterminateWrite(id int, client string, value, invoked, completed int) Operation {
	return Operation{ID: id, ClientID: client, Type: Write, Value: value,
		Invoked: invoked, Completed: completed, Status: Indeterminate}
}

// ---------- 1) Real-time relationships ----------

func TestConcurrentOverlapping(t *testing.T) {
	a := w(1, "A", 1, 0, 10)
	b := w(2, "B", 2, 5, 15)
	if !Concurrent(a, b) {
		t.Fatal("[0,10] and [5,15] overlap and must be concurrent")
	}
	if !Concurrent(b, a) {
		t.Fatal("concurrency must be symmetric")
	}
}

func TestNotConcurrentWhenSeparated(t *testing.T) {
	a := w(1, "A", 1, 0, 10)
	b := r(2, "B", 1, 11, 20)
	if Concurrent(a, b) {
		t.Fatal("[0,10] finishes before [11,20] starts — not concurrent")
	}
}

func TestMustPrecede(t *testing.T) {
	a := w(1, "A", 1, 0, 10)
	b := r(2, "B", 1, 11, 20)

	if !MustPrecede(a, b) {
		t.Fatal("a completed at 10, b invoked at 11 — a must precede b")
	}
	if MustPrecede(b, a) {
		t.Fatal("the relation is not symmetric")
	}
}

func TestMustPrecedeIsFalseForConcurrent(t *testing.T) {
	a := w(1, "A", 1, 0, 10)
	b := w(2, "B", 2, 5, 15)
	if MustPrecede(a, b) || MustPrecede(b, a) {
		t.Fatal("overlapping operations may be ordered either way — neither must precede")
	}
}

// ---------- 2) Basic linearizability ----------

func TestEmptyHistoryIsLinearizable(t *testing.T) {
	if got := Check(nil, 0); !got.Linearizable {
		t.Fatal("an empty history is trivially linearizable")
	}
}

func TestSequentialWriteThenRead(t *testing.T) {
	history := []Operation{
		w(1, "A", 5, 0, 10),
		r(2, "B", 5, 11, 20),
	}
	if got := Check(history, 0); !got.Linearizable {
		t.Fatalf("write(5) then read()->5 is obviously legal: %s", got.Explanation)
	}
}

func TestReadOfInitialValue(t *testing.T) {
	history := []Operation{r(1, "A", 0, 0, 10)}
	if got := Check(history, 0); !got.Linearizable {
		t.Fatal("reading the initial value before any write is legal")
	}
}

func TestStaleReadIsNotLinearizable(t *testing.T) {
	// The canonical violation from the lesson: the read begins AFTER both writes complete, so
	// every valid order puts write(2) last. Returning 1 is impossible.
	history := []Operation{
		w(1, "A", 1, 0, 10),
		w(2, "B", 2, 11, 20),
		r(3, "C", 1, 21, 30),
	}
	got := Check(history, 0)
	if got.Linearizable {
		t.Fatal("read()->1 after write(2) completed is a linearizability violation")
	}
}

func TestConcurrentWritesAllowEitherOrder(t *testing.T) {
	// A and B overlap, so the system may order them either way. Both reads are legal.
	base := []Operation{
		w(1, "A", 1, 0, 10),
		w(2, "B", 2, 5, 15),
	}
	for _, readValue := range []int{1, 2} {
		history := append(append([]Operation{}, base...), r(3, "C", readValue, 16, 20))
		if got := Check(history, 0); !got.Linearizable {
			t.Fatalf("with concurrent write(1) and write(2), read()->%d must be legal: %s",
				readValue, got.Explanation)
		}
	}
}

func TestReadConcurrentWithWriteMaySeeEitherValue(t *testing.T) {
	// The read overlaps the write, so it may be linearized before or after it.
	for _, readValue := range []int{0, 7} {
		history := []Operation{
			w(1, "A", 7, 0, 20),
			r(2, "B", readValue, 5, 15),
		}
		if got := Check(history, 0); !got.Linearizable {
			t.Fatalf("a read overlapping write(7) may return %d: %s", readValue, got.Explanation)
		}
	}
}

func TestReadOfNeverWrittenValue(t *testing.T) {
	history := []Operation{
		w(1, "A", 1, 0, 10),
		r(2, "B", 99, 11, 20),
	}
	got := Check(history, 0)
	if got.Linearizable {
		t.Fatal("99 was never written and is not the initial value — impossible")
	}
	if got.Explanation == "" {
		t.Fatal("a violation must come with an explanation a human can act on")
	}
}

// ---------- Witness ordering ----------

func TestWitnessOrderIsReturned(t *testing.T) {
	history := []Operation{
		w(1, "A", 1, 0, 10),
		w(2, "B", 2, 5, 15),
		r(3, "C", 2, 16, 20),
	}
	got := Check(history, 0)
	if !got.Linearizable {
		t.Fatalf("should be linearizable: %s", got.Explanation)
	}
	if len(got.Order) != len(history) {
		t.Fatalf("the witness must contain every operation, want %d got %d",
			len(history), len(got.Order))
	}
	// write(2) must be ordered after write(1), because read()->2 comes last.
	pos := map[int]int{}
	for i, op := range got.Order {
		pos[op.ID] = i
	}
	if pos[2] < pos[1] {
		t.Fatal("the witness order must actually explain the read: write(2) has to come after write(1)")
	}
	if pos[3] != len(history)-1 {
		t.Fatal("the read completed last in real time and must be last in the order")
	}
}

func TestWitnessRespectsRealTime(t *testing.T) {
	history := []Operation{
		w(1, "A", 1, 0, 10),
		r(2, "B", 1, 11, 20),
		w(3, "C", 3, 21, 30),
		r(4, "D", 3, 31, 40),
	}
	got := Check(history, 0)
	if !got.Linearizable {
		t.Fatalf("should be linearizable: %s", got.Explanation)
	}
	for i := 0; i < len(got.Order); i++ {
		for j := i + 1; j < len(got.Order); j++ {
			if MustPrecede(got.Order[j], got.Order[i]) {
				t.Fatalf("witness violates real time: %s placed before %s",
					got.Order[i], got.Order[j])
			}
		}
	}
}

// ---------- 3) Indeterminate operations ----------

func TestIndeterminateWriteMayHaveTakenEffect(t *testing.T) {
	// The client timed out, so it never learned whether the write landed. A read returning the
	// timed-out value is therefore PERFECTLY LEGAL. A harness that treats a timeout as
	// "did not happen" would report a false violation here.
	history := []Operation{
		indeterminateWrite(1, "A", 42, 0, 10),
		r(2, "B", 42, 11, 20),
	}
	if got := Check(history, 0); !got.Linearizable {
		t.Fatalf("a timed-out write may still have been applied — this is legal: %s", got.Explanation)
	}
}

func TestIndeterminateWriteMayNotHaveTakenEffect(t *testing.T) {
	// The mirror case: the same timed-out write, and a read that does NOT see it. Also legal.
	// A harness that treats a timeout as "definitely happened" would falsely reject this.
	history := []Operation{
		indeterminateWrite(1, "A", 42, 0, 10),
		r(2, "B", 0, 11, 20),
	}
	if got := Check(history, 0); !got.Linearizable {
		t.Fatalf("a timed-out write may have been lost — this is also legal: %s", got.Explanation)
	}
}

func TestIndeterminateWriteCannotExplainAnUnrelatedValue(t *testing.T) {
	history := []Operation{
		indeterminateWrite(1, "A", 42, 0, 10),
		r(2, "B", 99, 11, 20),
	}
	if got := Check(history, 0); got.Linearizable {
		t.Fatal("an indeterminate write of 42 cannot explain a read of 99")
	}
}

func TestIndeterminateWriteOnceAppliedStaysApplied(t *testing.T) {
	// If the timed-out write DID land, a later read cannot un-see it without another write.
	history := []Operation{
		indeterminateWrite(1, "A", 42, 0, 10),
		r(2, "B", 42, 11, 20),
		r(3, "C", 0, 21, 30),
	}
	if got := Check(history, 0); got.Linearizable {
		t.Fatal("once 42 was observed, a later read of 0 with no intervening write is impossible")
	}
}

// ---------- Diagnostics ----------

func TestFirstImpossibleRead(t *testing.T) {
	history := []Operation{
		w(1, "A", 1, 0, 10),
		r(2, "B", 1, 11, 20),
		r(3, "C", 77, 21, 30),
	}
	op, ok := firstImpossibleRead(history, 0)
	if !ok {
		t.Fatal("read()->77 is unexplainable and should be reported")
	}
	if op.ID != 3 {
		t.Fatalf("want operation 3, got %d", op.ID)
	}
}

func TestFirstImpossibleReadIgnoresLegalValues(t *testing.T) {
	history := []Operation{
		w(1, "A", 1, 0, 10),
		r(2, "B", 0, 11, 20), // the initial value — legal in principle
		r(3, "C", 1, 21, 30),
	}
	if _, ok := firstImpossibleRead(history, 0); ok {
		t.Fatal("every value read was either written or the initial value")
	}
}

func TestFirstImpossibleReadCountsIndeterminateWrites(t *testing.T) {
	// A timed-out write still explains a value, so it must not be flagged as impossible.
	history := []Operation{
		indeterminateWrite(1, "A", 5, 0, 10),
		r(2, "B", 5, 11, 20),
	}
	if _, ok := firstImpossibleRead(history, 0); ok {
		t.Fatal("an indeterminate write may have landed and explains the read")
	}
}

// ---------- Shrinking ----------

func TestShrinkReturnsNilForLegalHistory(t *testing.T) {
	history := []Operation{
		w(1, "A", 1, 0, 10),
		r(2, "B", 1, 11, 20),
	}
	if got := Shrink(history, 0); got != nil {
		t.Fatalf("nothing to shrink in a legal history, got %v", got)
	}
}

func TestShrinkFindsMinimalPrefix(t *testing.T) {
	// The violation happens at operation 3; the trailing legal operations are noise.
	history := []Operation{
		w(1, "A", 1, 0, 10),
		w(2, "B", 2, 11, 20),
		r(3, "C", 1, 21, 30), // violation
		w(4, "D", 9, 31, 40),
		r(5, "E", 9, 41, 50),
		r(6, "F", 9, 51, 60),
	}
	got := Shrink(history, 0)
	if got == nil {
		t.Fatal("the history is not linearizable, so shrink must return a counterexample")
	}
	if len(got) != 3 {
		t.Fatalf("the minimal failing prefix is the first 3 operations, got %d: %v", len(got), got)
	}
	if Check(got, 0).Linearizable {
		t.Fatal("the shrunk counterexample must itself still be non-linearizable")
	}
}

// ---------- Scale ----------

func TestLongLegalHistory(t *testing.T) {
	// Non-overlapping write/read pairs. Real-time constraints make the candidate set tiny at
	// every step, so a correct implementation handles this easily. If this hangs, the
	// pruning is missing.
	var history []Operation
	id, clock := 1, 0
	for i := 1; i <= 40; i++ {
		history = append(history, w(id, "A", i, clock, clock+1))
		id++
		clock += 2
		history = append(history, r(id, "B", i, clock, clock+1))
		id++
		clock += 2
	}
	if got := Check(history, 0); !got.Linearizable {
		t.Fatalf("a strictly sequential history must be linearizable: %s", got.Explanation)
	}
}

func TestConcurrentBatchIsHandled(t *testing.T) {
	// Eight fully-overlapping writes plus a read of one of them. The search space is large,
	// but memoisation on (remaining, state) keeps it tractable.
	var history []Operation
	for i := 1; i <= 8; i++ {
		history = append(history, w(i, "A", i, 0, 100))
	}
	history = append(history, r(9, "B", 3, 10, 90))

	if got := Check(history, 0); !got.Linearizable {
		t.Fatalf("read()->3 is legal among 8 concurrent writes: %s", got.Explanation)
	}
}
