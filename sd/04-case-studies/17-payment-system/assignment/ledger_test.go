package ledger

import (
	"sync"
	"testing"
)

func newFunded(t *testing.T) *Ledger {
	t.Helper()
	l := NewLedger()
	if err := l.CreateAccount("alice", 100); err != nil {
		t.Fatalf("CreateAccount alice: %v", err)
	}
	if err := l.CreateAccount("bob", 50); err != nil {
		t.Fatalf("CreateAccount bob: %v", err)
	}
	return l
}

func mustBalance(t *testing.T, l *Ledger, id string, want int64) {
	t.Helper()
	got, err := l.Balance(id)
	if err != nil {
		t.Fatalf("Balance(%q): %v", id, err)
	}
	if got != want {
		t.Fatalf("Balance(%q) = %d, want %d", id, got, want)
	}
}

func TestCreateAccountDuplicate(t *testing.T) {
	l := NewLedger()
	if err := l.CreateAccount("x", 10); err != nil {
		t.Fatalf("first create: %v", err)
	}
	if err := l.CreateAccount("x", 999); err != ErrAccountExists {
		t.Fatalf("duplicate create: want ErrAccountExists, got %v", err)
	}
	// Opening balance must not be clobbered by the rejected second create.
	mustBalance(t, l, "x", 10)
}

func TestTransferMovesFundsAndConserves(t *testing.T) {
	l := newFunded(t)
	before := l.TotalMoney()

	txn, err := l.Transfer("k1", "alice", "bob", 30)
	if err != nil {
		t.Fatalf("Transfer: %v", err)
	}
	if txn == "" {
		t.Fatal("successful transfer must return a non-empty txnID")
	}
	mustBalance(t, l, "alice", 70)
	mustBalance(t, l, "bob", 80)

	if after := l.TotalMoney(); after != before {
		t.Fatalf("TotalMoney must be conserved: before=%d after=%d", before, after)
	}
}

func TestInsufficientFundsNoStateChange(t *testing.T) {
	l := newFunded(t)
	before := l.TotalMoney()

	txn, err := l.Transfer("k1", "bob", "alice", 1000) // bob only has 50
	if err != ErrInsufficient {
		t.Fatalf("want ErrInsufficient, got %v", err)
	}
	if txn != "" {
		t.Fatalf("failed transfer must return empty txnID, got %q", txn)
	}
	// Nothing moved.
	mustBalance(t, l, "alice", 100)
	mustBalance(t, l, "bob", 50)
	if after := l.TotalMoney(); after != before {
		t.Fatalf("TotalMoney changed on failed transfer: before=%d after=%d", before, after)
	}
}

func TestNonPositiveRejected(t *testing.T) {
	l := newFunded(t)
	for _, amt := range []int64{0, -5} {
		if _, err := l.Transfer("k", "alice", "bob", amt); err != ErrNonPositive {
			t.Fatalf("amount %d: want ErrNonPositive, got %v", amt, err)
		}
	}
	mustBalance(t, l, "alice", 100)
	mustBalance(t, l, "bob", 50)
}

func TestUnknownAccountRejected(t *testing.T) {
	l := newFunded(t)
	if _, err := l.Transfer("k", "alice", "ghost", 10); err != ErrUnknownAccount {
		t.Fatalf("unknown 'to': want ErrUnknownAccount, got %v", err)
	}
	if _, err := l.Transfer("k", "ghost", "bob", 10); err != ErrUnknownAccount {
		t.Fatalf("unknown 'from': want ErrUnknownAccount, got %v", err)
	}
	if _, err := l.Balance("ghost"); err != ErrUnknownAccount {
		t.Fatalf("Balance(unknown): want ErrUnknownAccount, got %v", err)
	}
	mustBalance(t, l, "alice", 100)
}

// Same idempotency key twice: applies exactly once and returns the same txnID.
func TestIdempotencyAppliesOnce(t *testing.T) {
	l := newFunded(t)

	txn1, err := l.Transfer("order-42", "alice", "bob", 25)
	if err != nil {
		t.Fatalf("first transfer: %v", err)
	}
	txn2, err := l.Transfer("order-42", "alice", "bob", 25) // retry, same key
	if err != nil {
		t.Fatalf("replayed transfer: %v", err)
	}
	if txn1 != txn2 {
		t.Fatalf("same idempotency key must return same txnID: %q vs %q", txn1, txn2)
	}
	// Balances reflect ONE transfer, not two.
	mustBalance(t, l, "alice", 75)
	mustBalance(t, l, "bob", 75)
}

func TestTxnIDDeterministicAndUnique(t *testing.T) {
	l := newFunded(t)
	a, _ := l.Transfer("k1", "alice", "bob", 1)
	b, _ := l.Transfer("k2", "alice", "bob", 1)
	if a == b {
		t.Fatalf("distinct transfers must get distinct txnIDs, both %q", a)
	}
}

// -race: many concurrent transfers among several accounts. The conservation invariant
// (TotalMoney constant) must hold and no balance may go negative.
func TestConcurrentTransfersConserveMoney(t *testing.T) {
	l := NewLedger()
	accounts := []string{"a", "b", "c", "d"}
	for _, id := range accounts {
		if err := l.CreateAccount(id, 1000); err != nil {
			t.Fatalf("create %s: %v", id, err)
		}
	}
	total := l.TotalMoney()
	if total != 4000 {
		t.Fatalf("setup total should be 4000, got %d", total)
	}

	var wg sync.WaitGroup
	// Each goroutine uses a distinct idempotency key so all are real transfers.
	for i := 0; i < 200; i++ {
		wg.Add(1)
		go func(i int) {
			defer wg.Done()
			from := accounts[i%len(accounts)]
			to := accounts[(i+1)%len(accounts)]
			// Small amount; insufficient-funds rejections are fine and must leave
			// state consistent. Either outcome keeps the invariant.
			_, _ = l.Transfer(keyf(i), from, to, 5)
		}(i)
	}
	wg.Wait()

	if got := l.TotalMoney(); got != total {
		t.Fatalf("TotalMoney not conserved under concurrency: got %d want %d", got, total)
	}
	for _, id := range accounts {
		b, err := l.Balance(id)
		if err != nil {
			t.Fatalf("Balance(%s): %v", id, err)
		}
		if b < 0 {
			t.Fatalf("balance of %s went negative: %d", id, b)
		}
	}
}

func keyf(i int) string {
	const digits = "0123456789"
	// tiny deterministic itoa to avoid importing strconv in the test
	if i == 0 {
		return "key0"
	}
	var buf []byte
	for i > 0 {
		buf = append([]byte{digits[i%10]}, buf...)
		i /= 10
	}
	return "key" + string(buf)
}
