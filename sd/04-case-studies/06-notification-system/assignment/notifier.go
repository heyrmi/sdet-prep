// Package notifier is the Module 4.6 assignment: a small notification system that
// fans a notification out to multiple channels (push / SMS / email), retries on
// transient failure, deduplicates by notification ID, and drains cleanly.
//
// Read 04-case-studies/06-notification-system/README.md first.
//
// Fill in every method marked `// TODO`. Run the tests until green:
//
//	go test ./...
//	go test -race ./...   // concurrent Notify calls must be safe
//
// Two seams keep the tests deterministic and fast:
//   - sleep func(time.Duration) is injected, so tests pass a no-op (NO real sleeping).
//   - the worker pool exposes Wait()/Shutdown() so a test can block until work drains.
package notifier

import (
	"sync"
	"time"
)

// Sender delivers one message to one user over a single channel (push, SMS, email).
// A real Sender talks to a provider (APNs, Twilio, SES). Returning a non-nil error
// means "transient failure, please retry".
type Sender interface {
	Send(userID, msg string) error
}

// Notification is one logical notification to deliver, possibly across many channels.
type Notification struct {
	ID       string   // unique; used for idempotency/dedup
	UserID   string   // who to deliver to
	Channels []string // which registered channels to fan out to, e.g. ["push","email"]
	Body     string   // the message text
}

// Notifier dispatches notifications to registered channel Senders using a worker
// pool. It retries transient failures up to maxAttempts and never delivers the same
// notification ID more than once successfully.
type Notifier struct {
	maxAttempts int
	backoff     time.Duration             // base wait between retry attempts
	sleep       func(time.Duration)       // injected so tests can pass a no-op
	channels    map[string]Sender         // name -> Sender
	jobs        chan Notification         // worker-pool queue

	mu   sync.Mutex
	done map[string]bool // notification IDs already delivered successfully

	wg sync.WaitGroup // tracks in-flight worker goroutines
}

// NewNotifier builds a Notifier with `workers` goroutines draining the job queue,
// retrying each delivery up to maxAttempts with the given backoff between attempts.
// `sleep` is injected (pass time.Sleep in production, a no-op in tests).
func NewNotifier(workers, maxAttempts int, backoff time.Duration, sleep func(time.Duration)) *Notifier {
	n := &Notifier{
		maxAttempts: maxAttempts,
		backoff:     backoff,
		sleep:       sleep,
		channels:    make(map[string]Sender),
		jobs:        make(chan Notification, 1024),
		done:        make(map[string]bool),
	}
	// TODO:
	//  - start `workers` goroutines, each ranging over n.jobs and calling n.process(job).
	//  - track them with n.wg so Wait() can block until they finish.
	//    e.g. for i := 0; i < workers; i++ { n.wg.Add(1); go func(){ defer n.wg.Done(); for j := range n.jobs { n.process(j) } }() }
	_ = workers
	return n
}

// RegisterChannel wires a named channel (e.g. "push") to its Sender. Not safe to
// call concurrently with Notify; register all channels at startup.
func (n *Notifier) RegisterChannel(name string, s Sender) {
	// TODO: store s under name in n.channels.
	panic("TODO: implement RegisterChannel")
}

// Notify enqueues a notification for asynchronous delivery. It returns immediately;
// use Wait (or Shutdown) to block until all enqueued work has been processed.
func (n *Notifier) Notify(notif Notification) {
	// TODO: send notif onto n.jobs.
	panic("TODO: implement Notify")
}

// process delivers one notification: it dedups by ID, then for each requested
// channel calls the Sender, retrying transient errors up to maxAttempts.
func (n *Notifier) process(notif Notification) {
	// TODO:
	//  1. Dedup: if notif.ID is already in n.done (under the mutex), return without sending.
	//  2. For each channel name in notif.Channels:
	//       - look up the Sender; skip unknown channels.
	//       - call deliver(...) which retries up to maxAttempts.
	//  3. If delivery to all channels succeeded, mark notif.ID done (under the mutex).
	panic("TODO: implement process")
}

// deliver attempts one channel's Send up to maxAttempts, sleeping `backoff` between
// tries. Returns true on the first success, false if every attempt failed.
func (n *Notifier) deliver(s Sender, userID, msg string) bool {
	// TODO:
	//  for attempt := 1; attempt <= n.maxAttempts; attempt++ {
	//      if s.Send(userID, msg) == nil { return true }
	//      if attempt < n.maxAttempts { n.sleep(n.backoff) }
	//  }
	//  return false
	panic("TODO: implement deliver")
}

// Wait blocks until every enqueued notification has been processed, then returns.
// After Wait, the Notifier must not be used again.
func (n *Notifier) Wait() {
	// TODO: close n.jobs so workers exit when the queue drains, then n.wg.Wait().
	panic("TODO: implement Wait")
}

// Shutdown is an alias for Wait, kept for readability at call sites.
func (n *Notifier) Shutdown() { n.Wait() }
