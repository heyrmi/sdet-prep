// Package notifier is the reference solution for Module 4.6.
// Try the assignment yourself before reading this!
package notifier

import (
	"sync"
	"time"
)

type Sender interface {
	Send(userID, msg string) error
}

type Notification struct {
	ID       string
	UserID   string
	Channels []string
	Body     string
}

type Notifier struct {
	maxAttempts int
	backoff     time.Duration
	sleep       func(time.Duration)
	channels    map[string]Sender
	jobs        chan Notification

	mu   sync.Mutex
	done map[string]bool

	wg sync.WaitGroup
}

func NewNotifier(workers, maxAttempts int, backoff time.Duration, sleep func(time.Duration)) *Notifier {
	n := &Notifier{
		maxAttempts: maxAttempts,
		backoff:     backoff,
		sleep:       sleep,
		channels:    make(map[string]Sender),
		jobs:        make(chan Notification, 1024),
		done:        make(map[string]bool),
	}
	for i := 0; i < workers; i++ {
		n.wg.Add(1)
		go func() {
			defer n.wg.Done()
			for job := range n.jobs {
				n.process(job)
			}
		}()
	}
	return n
}

func (n *Notifier) RegisterChannel(name string, s Sender) {
	n.channels[name] = s
}

func (n *Notifier) Notify(notif Notification) {
	n.jobs <- notif
}

func (n *Notifier) process(notif Notification) {
	// Idempotency: if this ID was already delivered, drop it.
	n.mu.Lock()
	if n.done[notif.ID] {
		n.mu.Unlock()
		return
	}
	n.mu.Unlock()

	allOK := true
	for _, name := range notif.Channels {
		s, ok := n.channels[name]
		if !ok {
			// Unknown channel: nothing to send to, treat as a no-op (don't block dedup).
			continue
		}
		if !n.deliver(s, notif.UserID, notif.Body) {
			allOK = false
		}
	}

	if allOK {
		n.mu.Lock()
		n.done[notif.ID] = true
		n.mu.Unlock()
	}
}

func (n *Notifier) deliver(s Sender, userID, msg string) bool {
	for attempt := 1; attempt <= n.maxAttempts; attempt++ {
		if err := s.Send(userID, msg); err == nil {
			return true
		}
		if attempt < n.maxAttempts {
			n.sleep(n.backoff)
		}
	}
	return false
}

func (n *Notifier) Wait() {
	close(n.jobs)
	n.wg.Wait()
}

func (n *Notifier) Shutdown() { n.Wait() }
