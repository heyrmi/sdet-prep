// Package linearizability is the reference solution for Module 7.1.
//
// Points worth defending in an interview:
//
//   - The assertion is POSSIBILITY, not equality. Under concurrency several results are legal, so
//     you check whether any valid ordering explains the observed history.
//
//   - Real-time constraints are what make the search tractable. Operations that overlap may be
//     ordered freely; ones that do not may not. For a mostly-sequential history the candidate set
//     at each step is a single operation, so the exponential worst case never materialises.
//
//   - Memoise on (remaining set, state). Only NEGATIVE results are cached — a positive result
//     needs its witness ordering, which the recursion is already carrying on the stack.
//
//   - An indeterminate (timed-out) operation must be tried BOTH ways. Treating a timeout as
//     "did not happen" produces false violations; treating it as "happened" hides real ones.
//     This one distinction is where naive harnesses are wrong.
//
//   - Return a witness on success and a shrunk counterexample on failure. "false" is not a bug
//     report; six operations that cannot be ordered is.
package linearizability

import (
	"fmt"
	"sort"
	"strconv"
	"strings"
)

// OpType is the kind of operation.
type OpType int

const (
	Write OpType = iota
	Read
)

func (t OpType) String() string {
	if t == Write {
		return "write"
	}
	return "read"
}

// Status records whether the client learned the outcome.
type Status int

const (
	OK Status = iota
	Indeterminate
)

// Operation is one invocation, with the interval during which it could have taken effect.
type Operation struct {
	ID        int
	ClientID  string
	Type      OpType
	Value     int
	Invoked   int
	Completed int
	Status    Status
}

func (o Operation) String() string {
	s := fmt.Sprintf("%s:%s", o.ClientID, o.Type)
	if o.Type == Write {
		s += fmt.Sprintf("(%d)", o.Value)
	} else {
		s += fmt.Sprintf("->%d", o.Value)
	}
	if o.Status == Indeterminate {
		s += "?"
	}
	return s + fmt.Sprintf("[%d,%d]", o.Invoked, o.Completed)
}

// Concurrent reports whether two operations overlap in time.
func Concurrent(a, b Operation) bool {
	// They fail to overlap exactly when one finishes before the other starts.
	return !(a.Completed < b.Invoked || b.Completed < a.Invoked)
}

// MustPrecede reports whether a is forced before b in every valid linearization.
func MustPrecede(a, b Operation) bool {
	return a.Completed < b.Invoked
}

// Result carries the verdict and, on success, a witness ordering.
type Result struct {
	Linearizable bool
	Order        []Operation
	Explanation  string
}

// Check decides whether the history is linearizable for a register starting at initialValue.
func Check(history []Operation, initialValue int) Result {
	if len(history) == 0 {
		return Result{Linearizable: true}
	}

	ops := make([]Operation, len(history))
	copy(ops, history)
	sort.Slice(ops, func(i, j int) bool {
		if ops[i].Invoked != ops[j].Invoked {
			return ops[i].Invoked < ops[j].Invoked
		}
		return ops[i].ID < ops[j].ID // deterministic across runs
	})

	remaining := make([]bool, len(ops))
	for i := range remaining {
		remaining[i] = true
	}

	memo := make(map[string]bool)
	order := make([]Operation, 0, len(ops))

	if search(ops, remaining, len(ops), initialValue, memo, &order) {
		witness := make([]Operation, len(order))
		copy(witness, order)
		return Result{Linearizable: true, Order: witness}
	}

	// Prefer the specific explanation when one exists — it is far more actionable.
	if op, ok := firstImpossibleRead(history, initialValue); ok {
		return Result{
			Linearizable: false,
			Explanation: fmt.Sprintf(
				"%s read %d, which was never written and is not the initial value %d",
				op.ClientID, op.Value, initialValue),
		}
	}
	return Result{
		Linearizable: false,
		Explanation: "no ordering of the operations satisfies both the real-time constraints " +
			"and the register semantics",
	}
}

// search is the Wing & Gong backtracking linearization search.
func search(ops []Operation, remaining []bool, count, state int, memo map[string]bool, order *[]Operation) bool {
	if count == 0 {
		return true
	}

	key := memoKey(remaining, state)
	if cached, ok := memo[key]; ok && !cached {
		return false // only negatives are cached; see the package comment
	}

	for i := range ops {
		if !remaining[i] || !isMinimal(ops, remaining, i) {
			continue
		}
		op := ops[i]

		// Candidate resulting states. An indeterminate write has TWO possible outcomes and
		// both must be explored.
		var nextStates []int
		if op.Type == Write {
			if op.Status == Indeterminate {
				nextStates = []int{op.Value, state} // applied, or lost
			} else {
				nextStates = []int{op.Value}
			}
		} else {
			if op.Value != state {
				continue // this read cannot be explained here; try another candidate
			}
			nextStates = []int{state}
		}

		remaining[i] = false
		*order = append(*order, op)
		for _, next := range nextStates {
			if search(ops, remaining, count-1, next, memo, order) {
				return true
			}
		}
		*order = (*order)[:len(*order)-1]
		remaining[i] = true
	}

	memo[key] = false
	return false
}

// isMinimal reports whether ops[i] may be linearized next: no remaining operation is forced
// to come before it. This pruning is what keeps the search out of the factorial worst case.
func isMinimal(ops []Operation, remaining []bool, i int) bool {
	for j := range ops {
		if j == i || !remaining[j] {
			continue
		}
		if MustPrecede(ops[j], ops[i]) {
			return false
		}
	}
	return true
}

// memoKey identifies a subproblem by its remaining operations and register state.
func memoKey(remaining []bool, state int) string {
	var b strings.Builder
	b.Grow(len(remaining) + 12)
	for _, r := range remaining {
		if r {
			b.WriteByte('1')
		} else {
			b.WriteByte('0')
		}
	}
	b.WriteByte('|')
	b.WriteString(strconv.Itoa(state))
	return b.String()
}

// applyOp returns the register state after op and whether op's result is consistent.
// Indeterminate writes are treated as applied here; search explores the "lost" branch itself.
func applyOp(state int, op Operation) (int, bool) {
	if op.Type == Write {
		return op.Value, true
	}
	if op.Value == state {
		return state, true
	}
	return state, false
}

// firstImpossibleRead returns the earliest read whose value was never written.
func firstImpossibleRead(history []Operation, initialValue int) (Operation, bool) {
	written := map[int]bool{initialValue: true}
	for _, op := range history {
		if op.Type == Write {
			// Indeterminate writes count: they may well have landed, so a read of their
			// value is explainable and must not be flagged.
			written[op.Value] = true
		}
	}

	sorted := make([]Operation, len(history))
	copy(sorted, history)
	sort.Slice(sorted, func(i, j int) bool {
		if sorted[i].Invoked != sorted[j].Invoked {
			return sorted[i].Invoked < sorted[j].Invoked
		}
		return sorted[i].ID < sorted[j].ID
	})

	for _, op := range sorted {
		if op.Type == Read && !written[op.Value] {
			return op, true
		}
	}
	return Operation{}, false
}

// Shrink returns the smallest non-linearizable prefix of the history.
func Shrink(history []Operation, initialValue int) []Operation {
	if Check(history, initialValue).Linearizable {
		return nil
	}

	sorted := make([]Operation, len(history))
	copy(sorted, history)
	sort.Slice(sorted, func(i, j int) bool {
		if sorted[i].Invoked != sorted[j].Invoked {
			return sorted[i].Invoked < sorted[j].Invoked
		}
		return sorted[i].ID < sorted[j].ID
	})

	// Trailing operations are usually noise; the earliest failing prefix is what a human wants.
	for n := 1; n <= len(sorted); n++ {
		if !Check(sorted[:n], initialValue).Linearizable {
			out := make([]Operation, n)
			copy(out, sorted[:n])
			return out
		}
	}
	return sorted
}
