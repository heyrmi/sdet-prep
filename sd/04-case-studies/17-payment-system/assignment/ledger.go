// Package ledger is the Module 4.17 assignment: an idempotent, double-entry ledger.
//
// Read 04-case-studies/17-payment-system/README.md first.
//
// You implement the correctness core of a payment system:
//   - double-entry: every transfer debits one account and credits another by the
//     same amount, so the total money in the system never changes;
//   - atomicity: a transfer fully applies or not at all (no partial debit);
//   - idempotency: the same idempotencyKey applies exactly once and replays the
//     original result;
//   - concurrency-safety: callers hit it from many goroutines (tests run -race).
//
// Fill in every method marked `// TODO`. Run the tests until green:
//
//	go test ./...
//	go test -race ./...   // the ledger is shared across goroutines
//
// Money is int64 in the smallest unit (e.g. cents). Never use floats for money.
package ledger

import (
	"errors"
	"sync"
)

var (
	ErrUnknownAccount = errors.New("ledger: unknown account")
	ErrNonPositive    = errors.New("ledger: amount must be positive")
	ErrInsufficient   = errors.New("ledger: insufficient funds")
	ErrAccountExists  = errors.New("ledger: account already exists")
)

// Ledger holds accounts and an idempotency record. Safe for concurrent use once
// implemented. Use NewLedger to construct one.
type Ledger struct {
	mu sync.Mutex

	balances map[string]int64  // account id -> current balance (derived/cached)
	seen     map[string]string // idempotencyKey -> txnID (the original result)
	seq      int               // backs deterministic txnIDs (txn1, txn2, ...)
}

// NewLedger returns an empty ledger.
func NewLedger() *Ledger {
	return &Ledger{
		balances: make(map[string]int64),
		seen:     make(map[string]string),
	}
}

// CreateAccount registers an account with the given opening balance. It returns
// ErrAccountExists if the id is already registered. (Opening balances are how money
// enters the system in this model; transfers only move it around.)
func (l *Ledger) CreateAccount(id string, openingBalance int64) error {
	// TODO:
	//  1. Lock the mutex.
	//  2. If the account already exists, return ErrAccountExists.
	//  3. Otherwise set its balance to openingBalance.
	panic("TODO: implement Ledger.CreateAccount")
}

// Transfer atomically moves `amount` from `from` to `to` (double-entry: debit + credit).
// It is idempotent on idempotencyKey: a repeated key returns the original txnID and
// makes no further change. On any error (non-positive amount, unknown account,
// insufficient funds) it returns "" and leaves all balances unchanged.
func (l *Ledger) Transfer(idempotencyKey, from, to string, amount int64) (txnID string, err error) {
	// TODO:
	//  1. Lock the mutex (the whole operation must be atomic & race-free).
	//  2. Idempotency: if idempotencyKey was seen before, return its stored txnID, nil.
	//  3. Validate, making NO state change until everything is checked:
	//       - amount <= 0            -> ErrNonPositive
	//       - from or to unknown     -> ErrUnknownAccount
	//       - balances[from] < amount -> ErrInsufficient
	//     (Note: a transfer to oneself is a no-op on balance but still gets a txnID;
	//      decide and keep it consistent — the tests don't require self-transfer.)
	//  4. Apply both sides: balances[from] -= amount; balances[to] += amount.
	//  5. Allocate a txnID (e.g. l.seq++ -> "txn<seq>"), record seen[key] = txnID,
	//     and return it. Recording the key MUST happen together with the apply.
	panic("TODO: implement Ledger.Transfer")
}

// Balance returns the current balance of an account, or ErrUnknownAccount.
func (l *Ledger) Balance(id string) (int64, error) {
	// TODO: lock; look up; return balance or ErrUnknownAccount.
	panic("TODO: implement Ledger.Balance")
}

// TotalMoney returns the sum of all account balances. This must be invariant across
// any number of transfers (conservation of money) and only change via CreateAccount.
func (l *Ledger) TotalMoney() int64 {
	// TODO: lock; sum all balances; return.
	panic("TODO: implement Ledger.TotalMoney")
}
