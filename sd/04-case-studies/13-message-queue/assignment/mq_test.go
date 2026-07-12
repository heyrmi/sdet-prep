package mq

import (
	"fmt"
	"sort"
	"sync"
	"testing"
)

func mustTopic(t *testing.T, b *Broker, name string, parts int) {
	t.Helper()
	if err := b.CreateTopic(name, parts); err != nil {
		t.Fatalf("CreateTopic(%q): %v", name, err)
	}
}

// ---------------- Produce / partitioning ----------------

func TestSameKeySamePartitionAndOrder(t *testing.T) {
	b := NewBroker()
	mustTopic(t, b, "orders", 4)

	key := []byte("user-42")
	var firstPart int
	for i := 0; i < 10; i++ {
		p, off, err := b.Produce("orders", key, []byte(fmt.Sprintf("v%d", i)))
		if err != nil {
			t.Fatalf("produce: %v", err)
		}
		if i == 0 {
			firstPart = p
		} else if p != firstPart {
			t.Fatalf("same key must map to same partition: got %d then %d", firstPart, p)
		}
		if off != int64(i) {
			t.Fatalf("offset must increase 0,1,2,...; record %d got offset %d", i, off)
		}
	}

	// Read them back and confirm value order matches produce order.
	c, _ := b.Join("g-order", "orders")
	recs := c.Poll(100)
	var got []string
	for _, r := range recs {
		if r.Partition == firstPart {
			got = append(got, string(r.Value))
		}
	}
	for i, v := range got {
		if v != fmt.Sprintf("v%d", i) {
			t.Fatalf("order not preserved within partition: got %v", got)
		}
	}
}

func TestOffsetsIncreasePerPartition(t *testing.T) {
	b := NewBroker()
	mustTopic(t, b, "t", 3)
	next := map[int]int64{}
	for i := 0; i < 30; i++ {
		key := []byte(fmt.Sprintf("k%d", i))
		p, off, err := b.Produce("t", key, []byte("x"))
		if err != nil {
			t.Fatalf("produce: %v", err)
		}
		want := next[p]
		if off != want {
			t.Fatalf("partition %d expected next offset %d, got %d", p, want, off)
		}
		next[p]++
	}
}

// ---------------- Consumer groups ----------------

func sortedInts(s []int) []int {
	out := append([]int(nil), s...)
	sort.Ints(out)
	return out
}

func TestTwoConsumersSplitPartitions(t *testing.T) {
	b := NewBroker()
	mustTopic(t, b, "t", 4)

	c1, _ := b.Join("g", "t")
	c2, _ := b.Join("g", "t")

	a1 := c1.Assignment()
	a2 := c2.Assignment()

	// No overlap.
	seen := map[int]bool{}
	for _, p := range append(append([]int{}, a1...), a2...) {
		if seen[p] {
			t.Fatalf("partition %d assigned to more than one consumer", p)
		}
		seen[p] = true
	}
	// Full coverage of all 4 partitions.
	if len(seen) != 4 {
		t.Fatalf("partitions 0..3 must all be covered, got %v + %v", sortedInts(a1), sortedInts(a2))
	}
}

func TestCommitAndResume(t *testing.T) {
	b := NewBroker()
	mustTopic(t, b, "t", 1) // single partition keeps it simple

	for i := 0; i < 5; i++ {
		if _, _, err := b.Produce("t", []byte("same-key"), []byte(fmt.Sprintf("v%d", i))); err != nil {
			t.Fatalf("produce: %v", err)
		}
	}

	c1, _ := b.Join("g", "t")
	first := c1.Poll(3) // read v0,v1,v2
	if len(first) != 3 {
		t.Fatalf("expected 3 records, got %d", len(first))
	}
	c1.Commit()

	// A new consumer in the SAME group resumes after the committed offset.
	c2, _ := b.Join("g", "t")
	rest := c2.Poll(100)
	if len(rest) != 2 {
		t.Fatalf("after committing 3, a new consumer should see the remaining 2, got %d", len(rest))
	}
	if string(rest[0].Value) != "v3" || string(rest[1].Value) != "v4" {
		t.Fatalf("resume should start at v3,v4; got %s,%s", rest[0].Value, rest[1].Value)
	}
}

func TestUncommittedRedelivered(t *testing.T) {
	b := NewBroker()
	mustTopic(t, b, "t", 1)
	for i := 0; i < 3; i++ {
		b.Produce("t", []byte("k"), []byte(fmt.Sprintf("v%d", i)))
	}

	c1, _ := b.Join("g", "t")
	got1 := c1.Poll(100)
	if len(got1) != 3 {
		t.Fatalf("first consumer should read all 3, got %d", len(got1))
	}
	// c1 never commits (simulating a crash). A new consumer in the same group
	// must re-read from the last committed offset (0) — at-least-once delivery.
	c2, _ := b.Join("g", "t")
	got2 := c2.Poll(100)
	if len(got2) != 3 {
		t.Fatalf("uncommitted records must be redelivered to a new consumer, got %d", len(got2))
	}
}

func TestFreshGroupReplaysFromZero(t *testing.T) {
	b := NewBroker()
	mustTopic(t, b, "t", 2)
	for i := 0; i < 10; i++ {
		b.Produce("t", []byte(fmt.Sprintf("k%d", i)), []byte("x"))
	}

	// Group A consumes and commits everything.
	ca, _ := b.Join("ga", "t")
	for len(ca.Poll(100)) > 0 {
		ca.Commit()
	}

	// A brand-new group replays from offset 0 — it sees all 10 records.
	cb, _ := b.Join("gb", "t")
	total := 0
	for {
		recs := cb.Poll(100)
		if len(recs) == 0 {
			break
		}
		total += len(recs)
		cb.Commit()
	}
	if total != 10 {
		t.Fatalf("a fresh group must replay all 10 records from 0, got %d", total)
	}
}

// ---------------- Concurrency ----------------

func TestConcurrentProduce(t *testing.T) {
	b := NewBroker()
	mustTopic(t, b, "t", 8)

	const producers = 8
	const perProducer = 200
	var wg sync.WaitGroup
	for g := 0; g < producers; g++ {
		wg.Add(1)
		go func(g int) {
			defer wg.Done()
			for i := 0; i < perProducer; i++ {
				key := []byte(fmt.Sprintf("p%d-%d", g, i))
				if _, _, err := b.Produce("t", key, []byte("v")); err != nil {
					t.Errorf("produce: %v", err)
					return
				}
			}
		}(g)
	}
	wg.Wait()

	// Every produced record must be retrievable exactly once across the group.
	c, _ := b.Join("readers", "t")
	total := 0
	for {
		recs := c.Poll(500)
		if len(recs) == 0 {
			break
		}
		total += len(recs)
		c.Commit()
	}
	if total != producers*perProducer {
		t.Fatalf("expected %d records retrievable, got %d", producers*perProducer, total)
	}
}
