package chat

import (
	"fmt"
	"sync"
	"testing"
	"time"
)

// recv does a short, bounded read: it returns the next message on c.Out, or fails
// the test if nothing arrives quickly. This keeps tests deterministic WITHOUT real
// sleeps — we block only until the message is delivered (microseconds in practice),
// and the timeout is just a safety net against a hung Hub.
func recv(t *testing.T, c *Client) Message {
	t.Helper()
	select {
	case m := <-c.Out:
		return m
	case <-time.After(time.Second):
		t.Fatalf("client %s: timed out waiting for a message", c.ID)
		return Message{}
	}
}

// expectNothing asserts no message arrives on c.Out within a small window.
func expectNothing(t *testing.T, c *Client) {
	t.Helper()
	select {
	case m := <-c.Out:
		t.Fatalf("client %s: expected no message, got %+v", c.ID, m)
	case <-time.After(50 * time.Millisecond):
	}
}

func startHub(t *testing.T) *Hub {
	t.Helper()
	h := NewHub()
	go h.Run()
	t.Cleanup(h.Stop)
	return h
}

func TestBroadcastReachesAllMembers(t *testing.T) {
	h := startHub(t)
	a := NewClient("a", 4)
	b := NewClient("b", 4)
	h.Join("room1", a)
	h.Join("room1", b)

	h.Broadcast("room1", Message{From: "a", Body: "hello"})

	for _, c := range []*Client{a, b} {
		m := recv(t, c)
		if m.Body != "hello" || m.From != "a" || m.Room != "room1" {
			t.Fatalf("client %s got unexpected message %+v", c.ID, m)
		}
	}
}

func TestLeftClientGetsNothing(t *testing.T) {
	h := startHub(t)
	a := NewClient("a", 4)
	b := NewClient("b", 4)
	h.Join("room1", a)
	h.Join("room1", b)

	h.Leave("room1", b)

	h.Broadcast("room1", Message{From: "a", Body: "after-leave"})

	// a still in the room receives it...
	if m := recv(t, a); m.Body != "after-leave" {
		t.Fatalf("remaining member should still receive, got %+v", m)
	}
	// ...but b, who left, must get nothing further.
	expectNothing(t, b)
}

func TestSeqMonotonicAndConsistentAcrossReceivers(t *testing.T) {
	h := startHub(t)
	a := NewClient("a", 8)
	b := NewClient("b", 8)
	h.Join("room1", a)
	h.Join("room1", b)

	const n = 5
	for i := 0; i < n; i++ {
		h.Broadcast("room1", Message{From: "a", Body: fmt.Sprintf("m%d", i)})
	}

	var seqsA, seqsB []uint64
	for i := 0; i < n; i++ {
		seqsA = append(seqsA, recv(t, a).Seq)
		seqsB = append(seqsB, recv(t, b).Seq)
	}

	// Per-room Seq must be strictly increasing, starting at 1.
	for i, s := range seqsA {
		want := uint64(i + 1)
		if s != want {
			t.Fatalf("a: expected Seq %d at position %d, got %d (full: %v)", want, i, s, seqsA)
		}
	}
	// Both receivers must see the SAME sequence numbers in the SAME order.
	for i := range seqsA {
		if seqsA[i] != seqsB[i] {
			t.Fatalf("receivers disagree on ordering at %d: a=%d b=%d", i, seqsA[i], seqsB[i])
		}
	}
}

func TestRoomsAreIsolated(t *testing.T) {
	h := startHub(t)
	a := NewClient("a", 4) // room1
	b := NewClient("b", 4) // room2
	h.Join("room1", a)
	h.Join("room2", b)

	h.Broadcast("room1", Message{From: "a", Body: "only-room1"})

	if m := recv(t, a); m.Body != "only-room1" {
		t.Fatalf("room1 member should receive, got %+v", m)
	}
	expectNothing(t, b) // room2 must not see room1 traffic

	// Per-room Seq counters are independent: room2's first message is Seq 1.
	h.Broadcast("room2", Message{From: "b", Body: "only-room2"})
	if m := recv(t, b); m.Seq != 1 {
		t.Fatalf("room2's first message should have Seq 1, got %d", m.Seq)
	}
}

// TestConcurrentChaos runs many clients joining, broadcasting, and leaving at once.
// Its job is to be run under -race; we drain Out channels concurrently so the Hub's
// non-blocking sends always have a reader and nothing deadlocks.
func TestConcurrentChaos(t *testing.T) {
	h := startHub(t)

	const clients = 50
	const msgsEach = 20

	var wg sync.WaitGroup
	for i := 0; i < clients; i++ {
		wg.Add(1)
		go func(i int) {
			defer wg.Done()
			room := fmt.Sprintf("room%d", i%5) // 5 shared rooms
			c := NewClient(fmt.Sprintf("c%d", i), 8)

			// Drain whatever we receive so the Hub never blocks on us.
			stop := make(chan struct{})
			var dwg sync.WaitGroup
			dwg.Add(1)
			go func() {
				defer dwg.Done()
				for {
					select {
					case <-c.Out:
					case <-stop:
						return
					}
				}
			}()

			h.Join(room, c)
			for j := 0; j < msgsEach; j++ {
				h.Broadcast(room, Message{From: c.ID, Body: "x"})
			}
			h.Leave(room, c)

			close(stop)
			dwg.Wait()
		}(i)
	}
	wg.Wait()
	// If we got here under -race with no panic/deadlock, the Hub is concurrency-safe.
}
