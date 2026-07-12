// Package ledger is the reference solution for Module 4.17.
// Try the assignment yourself before reading this!
package ledger

import (
	"errors"
	"fmt"
	"sync"
)

var (
	ErrUnknownAccount = errors.New("ledger: unknown account")
	ErrNonPositive    = errors.New("ledger: amount must be positive")
	ErrInsufficient   = errors.New("ledger: insufficient funds")
	ErrAccountExists  = errors.New("ledger: account already exists")
)

type Ledger struct {
	mu sync.Mutex

	balances map[string]int64
	seen     map[string]string
	seq      int
}

func NewLedger() *Ledger {
	return &Ledger{
		balances: make(map[string]int64),
		seen:     make(map[string]string),
	}
}

func (l *Ledger) CreateAccount(id string, openingBalance int64) error {
	l.mu.Lock()
	defer l.mu.Unlock()
	if _, ok := l.balances[id]; ok {
		return ErrAccountExists
	}
	l.balances[id] = openingBalance
	return nil
}

func (l *Ledger) Transfer(idempotencyKey, from, to string, amount int64) (txnID string, err error) {
	l.mu.Lock()
	defer l.mu.Unlock()

	// Idempotency: replay the original result, change nothing.
	if existing, ok := l.seen[idempotencyKey]; ok {
		return existing, nil
	}

	// Validate fully before mutating any state.
	if amount <= 0 {
		return "", ErrNonPositive
	}
	fromBal, ok := l.balances[from]
	if !ok {
		return "", ErrUnknownAccount
	}
	if _, ok := l.balances[to]; !ok {
		return "", ErrUnknownAccount
	}
	if fromBal < amount {
		return "", ErrInsufficient
	}

	// Apply both sides (double-entry) and record the idempotency key together.
	l.balances[from] -= amount
	l.balances[to] += amount

	l.seq++
	txnID = fmt.Sprintf("txn%d", l.seq)
	l.seen[idempotencyKey] = txnID
	return txnID, nil
}

func (l *Ledger) Balance(id string) (int64, error) {
	l.mu.Lock()
	defer l.mu.Unlock()
	bal, ok := l.balances[id]
	if !ok {
		return 0, ErrUnknownAccount
	}
	return bal, nil
}

func (l *Ledger) TotalMoney() int64 {
	l.mu.Lock()
	defer l.mu.Unlock()
	var total int64
	for _, b := range l.balances {
		total += b
	}
	return total
}
