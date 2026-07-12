package notifier

import (
	"errors"
	"fmt"
	"sync"
	"sync/atomic"
	"testing"
	"time"
)

// noSleep is the injected sleep seam: tests never actually sleep.
func noSleep(_ time.Duration) {}

// mockSender records every Send call and can be told to fail the first failFirst
// attempts (returning an error) before succeeding, or to fail forever.
type mockSender struct {
	mu        sync.Mutex
	failFirst int  // fail this many initial attempts, then succeed
	failAll   bool // fail every attempt
	calls     int
	delivered []string
}

func (m *mockSender) Send(userID, msg string) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.calls++
	if m.failAll {
		return errors.New("permanent failure")
	}
	if m.calls <= m.failFirst {
		return fmt.Errorf("transient failure on attempt %d", m.calls)
	}
	m.delivered = append(m.delivered, userID+":"+msg)
	return nil
}

func (m *mockSender) numCalls() int {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.calls
}

func (m *mockSender) numDelivered() int {
	m.mu.Lock()
	defer m.mu.Unlock()
	return len(m.delivered)
}

func TestRetriesThenSucceeds(t *testing.T) {
	n := NewNotifier(1, 5, 0, noSleep)
	sender := &mockSender{failFirst: 2} // fail twice, succeed on the 3rd
	n.RegisterChannel("push", sender)

	n.Notify(Notification{ID: "a1", UserID: "u1", Channels: []string{"push"}, Body: "hi"})
	n.Wait()

	if got := sender.numCalls(); got != 3 {
		t.Fatalf("expected exactly 3 attempts (2 failures + 1 success), got %d", got)
	}
	if got := sender.numDelivered(); got != 1 {
		t.Fatalf("expected 1 successful delivery, got %d", got)
	}
}

func TestStopsAfterMaxAttempts(t *testing.T) {
	n := NewNotifier(1, 3, 0, noSleep)
	sender := &mockSender{failAll: true}
	n.RegisterChannel("push", sender)

	n.Notify(Notification{ID: "a2", UserID: "u1", Channels: []string{"push"}, Body: "hi"})
	n.Wait()

	if got := sender.numCalls(); got != 3 {
		t.Fatalf("permanently failing sender should be tried exactly maxAttempts=3 times, got %d", got)
	}
	if got := sender.numDelivered(); got != 0 {
		t.Fatalf("nothing should have been delivered, got %d", got)
	}
}

func TestIdempotencyDeliversOnce(t *testing.T) {
	// One worker => the first job fully finishes (and marks the ID done) before the
	// duplicate is processed. This makes dedup deterministic.
	n := NewNotifier(1, 3, 0, noSleep)
	sender := &mockSender{}
	n.RegisterChannel("push", sender)

	notif := Notification{ID: "dup", UserID: "u1", Channels: []string{"push"}, Body: "once"}
	n.Notify(notif)
	n.Notify(notif) // same ID submitted again
	n.Wait()

	if got := sender.numCalls(); got != 1 {
		t.Fatalf("same notification ID must be sent only once, got %d sends", got)
	}
}

func TestMultiChannelFanOut(t *testing.T) {
	n := NewNotifier(1, 3, 0, noSleep)
	push := &mockSender{}
	email := &mockSender{}
	sms := &mockSender{}
	n.RegisterChannel("push", push)
	n.RegisterChannel("email", email)
	n.RegisterChannel("sms", sms)

	n.Notify(Notification{
		ID:       "fan1",
		UserID:   "u1",
		Channels: []string{"push", "email", "sms"},
		Body:     "hello",
	})
	n.Wait()

	for name, s := range map[string]*mockSender{"push": push, "email": email, "sms": sms} {
		if got := s.numDelivered(); got != 1 {
			t.Fatalf("channel %s should have delivered once, got %d", name, got)
		}
	}
}

func TestConcurrentNotify(t *testing.T) {
	// Run with -race: many goroutines call Notify while a pool of workers drains.
	// Each notification has a distinct ID, so all should be delivered exactly once.
	n := NewNotifier(8, 3, 0, noSleep)
	var delivered int64
	sender := &countingSender{onSend: func() { atomic.AddInt64(&delivered, 1) }}
	n.RegisterChannel("push", sender)

	const total = 1000
	var wg sync.WaitGroup
	for i := 0; i < total; i++ {
		wg.Add(1)
		go func(i int) {
			defer wg.Done()
			n.Notify(Notification{
				ID:       fmt.Sprintf("c-%d", i),
				UserID:   "u1",
				Channels: []string{"push"},
				Body:     "x",
			})
		}(i)
	}
	wg.Wait()
	n.Wait()

	if got := atomic.LoadInt64(&delivered); got != total {
		t.Fatalf("expected %d deliveries, got %d", total, got)
	}
}

// countingSender always succeeds and invokes onSend per call; safe for -race.
type countingSender struct{ onSend func() }

func (c *countingSender) Send(_, _ string) error {
	c.onSend()
	return nil
}
