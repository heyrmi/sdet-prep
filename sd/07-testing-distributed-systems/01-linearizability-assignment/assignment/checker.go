// Package linearizability is the Module 7.1 assignment: a real linearizability checker.
//
// Read 07-testing-distributed-systems/01-consistency-checking-and-linearizability.md first.
//
// Fill in every function marked `// TODO`. Run the tests until green:
//
//	go test ./...
//
// The object under test is a single register holding an int, initial value 0:
//
//	Write(v)  - sets the register to v
//	Read()    - returns the current value
//
// A history is LINEARIZABLE if there exists a total order of its operations such that
//
//  1. the order respects REAL TIME - if op A completed before op B was invoked, A comes
//     before B; operations that OVERLAP in time may be ordered either way, and
//  2. under that order, every Read returns the value of the most recent preceding Write.
//
// This is the inversion at the heart of distributed-systems testing: you are not asserting that
// an output equals an expected value (many are legal), you are asserting that the observed
// history is POSSIBLE.
package linearizability

import (
	"fmt"
	"sort"
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
	// OK: the operation completed and the client saw the result.
	OK Status = iota
	// Indeterminate: the operation timed out. It MAY have taken effect, or may not. This is the
	// distinction naive harnesses get wrong: treating a timeout as "did not happen" produces
	// false violation reports, and treating it as "happened" hides real ones.
	Indeterminate
)

// Operation is one invocation, with the interval during which it could have taken effect.
type Operation struct {
	ID       int
	ClientID string
	Type     OpType
	// Value is the written value for a Write, or the returned value for a Read.
	Value int
	// Invoked and Completed are logical timestamps. For an Indeterminate operation, Completed
	// is the point past which it certainly cannot still take effect.
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

// ----------------------------------------------------------------------------
// 1) Real-time relationships
// ----------------------------------------------------------------------------

// Concurrent reports whether two operations overlap in time, meaning the system was free to
// order them either way.
//
// They are NOT concurrent exactly when one completes before the other is invoked.
func Concurrent(a, b Operation) bool {
	// TODO
	return false
}

// MustPrecede reports whether a is forced to come before b in every valid linearization,
// i.e. a completed strictly before b was invoked.
func MustPrecede(a, b Operation) bool {
	// TODO
	return false
}

// ----------------------------------------------------------------------------
// 2) The checker
//
// The Wing & Gong backtracking search:
//
//	linearizable(remaining, state):
//	    if remaining is empty: return true
//	    for each op in remaining that is MINIMAL (nothing remaining must precede it):
//	        if applying op to state is consistent with its observed result:
//	            if linearizable(remaining - op, newState): return true
//	        # backtrack and try the next minimal op
//	    return false
//
// Two things make this tractable:
//   - real-time constraints prune the candidate set hard, and
//   - memoisation on (remaining set, state) collapses the repeated subproblems.
//
// It is still exponential in the worst case, which is why real checkers run MANY SHORT
// histories rather than one long one.
// ----------------------------------------------------------------------------

// Result carries the verdict and, on success, a witness ordering.
type Result struct {
	Linearizable bool
	// Order is a valid linearization when Linearizable is true. A witness beats a bare "true":
	// it is what lets a human confirm the checker is not itself buggy.
	Order []Operation
	// Explanation describes why a non-linearizable history is impossible.
	Explanation string
}

// Check decides whether the history is linearizable for a register starting at initialValue.
func Check(history []Operation, initialValue int) Result {
	// TODO:
	//  1. Sort a copy of the history by Invoked (then ID) so the search is deterministic.
	//  2. Run the backtracking search from the full set with state = initialValue.
	//  3. On success return the witness order; on failure return an Explanation naming the
	//     first read that could not be satisfied (see firstImpossibleRead).
	return Result{}
}

// minimalOps returns the operations in `remaining` that no other remaining operation must
// precede — the legal candidates for the next position in the linearization.
func minimalOps(remaining []Operation) []Operation {
	// TODO: op is minimal when no other op in remaining satisfies MustPrecede(other, op).
	return nil
}

// applyOp returns the register state after op, and whether op's observed result is consistent
// with the state it was applied to.
//
// Rules:
//   - Write(v) with status OK    -> always consistent; new state is v.
//   - Write(v) Indeterminate     -> handled by the caller, which must try BOTH applying and
//     skipping it. Here, treat it as applied.
//   - Read()->v                  -> consistent only if v equals the current state; state unchanged.
func applyOp(state int, op Operation) (int, bool) {
	// TODO
	return state, false
}

// ----------------------------------------------------------------------------
// 3) Diagnostics
// ----------------------------------------------------------------------------

// firstImpossibleRead returns the earliest read whose value was never written and is not the
// initial value — an unambiguous, human-readable explanation of an impossible history.
// Returns ok=false if no such read exists (the violation is subtler and needs the full search).
func firstImpossibleRead(history []Operation, initialValue int) (Operation, bool) {
	// TODO:
	//  1. Collect every value written by any Write (including Indeterminate ones — they may
	//     have taken effect).
	//  2. Return the earliest-invoked Read whose Value is neither in that set nor the initial
	//     value.
	return Operation{}, false
}

// Shrink returns the smallest PREFIX of the history (by invocation order) that is still
// non-linearizable. Handing a developer a 6-operation counterexample instead of a
// 10,000-operation log is most of the value a checker provides.
//
// Returns the full history if no shorter prefix reproduces the violation, and nil if the
// history is linearizable.
func Shrink(history []Operation, initialValue int) []Operation {
	// TODO:
	//  1. If the full history is linearizable, return nil.
	//  2. Sort by Invoked; try prefixes of length 1, 2, ... and return the first that is
	//     NOT linearizable.
	return nil
}

var _ = sort.Slice
