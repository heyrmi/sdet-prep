// Package mq is the reference solution for Module 4.13 (Distributed Message Queue).
// Try the assignment yourself before reading this!
package mq

import (
	"errors"
	"hash/fnv"
	"sort"
	"sync"
)

// Record is a single message read back from a partition.
type Record struct {
	Partition int
	Offset    int64
	Key       []byte
	Value     []byte
}

type message struct {
	key   []byte
	value []byte
}

// partition is an append-only log.
type partition struct {
	log []message
}

type topic struct {
	partitions []*partition
}

// Broker holds topics and consumer-group state. All methods are safe for
// concurrent use.
type Broker struct {
	mu     sync.Mutex
	topics map[string]*topic
	// committed[group][topic][partition] = next offset to read (count consumed).
	committed map[string]map[string][]int64
	// groups tracks the live consumers per (group, topic) for assignment.
	groups map[string]*group
}

type group struct {
	topic       string
	members     []*Consumer // ordered by join time
	partitionN  int
}

// NewBroker creates an empty broker.
func NewBroker() *Broker {
	return &Broker{
		topics:    make(map[string]*topic),
		committed: make(map[string]map[string][]int64),
		groups:    make(map[string]*group),
	}
}

// CreateTopic registers a topic with the given number of partitions.
func (b *Broker) CreateTopic(name string, partitions int) error {
	if partitions < 1 {
		return errors.New("partitions must be >= 1")
	}
	b.mu.Lock()
	defer b.mu.Unlock()
	if _, ok := b.topics[name]; ok {
		return errors.New("topic already exists")
	}
	t := &topic{partitions: make([]*partition, partitions)}
	for i := range t.partitions {
		t.partitions[i] = &partition{}
	}
	b.topics[name] = t
	return nil
}

// partitionFor maps a key to a partition via FNV-1a hash. A nil/empty key maps
// to partition 0 deterministically.
func partitionFor(key []byte, n int) int {
	h := fnv.New32a()
	_, _ = h.Write(key)
	return int(h.Sum32() % uint32(n))
}

// Produce appends (key, value) to the partition chosen by hash(key) and returns
// the partition index and the assigned offset.
func (b *Broker) Produce(topicName string, key, value []byte) (int, int64, error) {
	b.mu.Lock()
	defer b.mu.Unlock()

	t, ok := b.topics[topicName]
	if !ok {
		return 0, 0, errors.New("no such topic")
	}
	p := partitionFor(key, len(t.partitions))
	off := int64(len(t.partitions[p].log))
	// Copy the byte slices so later caller mutation can't corrupt the log.
	t.partitions[p].log = append(t.partitions[p].log, message{
		key:   append([]byte(nil), key...),
		value: append([]byte(nil), value...),
	})
	return p, off, nil
}

// Consumer reads from the partitions assigned to it within its group.
type Consumer struct {
	broker *Broker
	group  string
	topic  string
	// cursor[partition] = next offset this consumer will read (its private view
	// until Commit makes it visible to the group).
	cursor map[int]int64
}

// Join adds a consumer to group `groupName` on `topicName`. Partitions are
// re-assigned round-robin across all current members of the group.
func (b *Broker) Join(groupName, topicName string) (*Consumer, error) {
	b.mu.Lock()
	defer b.mu.Unlock()

	t, ok := b.topics[topicName]
	if !ok {
		return nil, errors.New("no such topic")
	}
	if _, ok := b.committed[groupName]; !ok {
		b.committed[groupName] = make(map[string][]int64)
	}
	if _, ok := b.committed[groupName][topicName]; !ok {
		b.committed[groupName][topicName] = make([]int64, len(t.partitions))
	}

	g, ok := b.groups[groupName]
	if !ok {
		g = &group{topic: topicName, partitionN: len(t.partitions)}
		b.groups[groupName] = g
	}
	c := &Consumer{
		broker: b,
		group:  groupName,
		topic:  topicName,
		cursor: make(map[int]int64),
	}
	g.members = append(g.members, c)
	return c, nil
}

// assignedLocked returns the sorted partition indexes assigned to consumer c
// under the current round-robin layout. Caller must hold b.mu.
func (b *Broker) assignedLocked(c *Consumer) []int {
	g := b.groups[c.group]
	idx := -1
	for i, m := range g.members {
		if m == c {
			idx = i
			break
		}
	}
	if idx < 0 {
		return nil
	}
	n := len(g.members)
	// Reverse round-robin: the most recently joined member takes the lowest
	// partitions. This way a fresh consumer that joins after another "left"
	// (crashed without committing) takes over its partitions and redelivers
	// the uncommitted records — at-least-once. Member i owns partitions p
	// where p mod n == (n-1-i).
	want := (n - 1 - idx) % n
	var parts []int
	for p := 0; p < g.partitionN; p++ {
		if p%n == want {
			parts = append(parts, p)
		}
	}
	sort.Ints(parts)
	return parts
}

// Assignment returns the partition indexes currently assigned to this consumer.
func (c *Consumer) Assignment() []int {
	c.broker.mu.Lock()
	defer c.broker.mu.Unlock()
	return c.broker.assignedLocked(c)
}

// Poll returns up to `max` of the next un-consumed records across this
// consumer's assigned partitions. Records are not committed until Commit is
// called; the private cursor advances so repeated Polls don't re-read the same
// records within this consumer's lifetime.
func (c *Consumer) Poll(max int) []Record {
	b := c.broker
	b.mu.Lock()
	defer b.mu.Unlock()

	t := b.topics[c.topic]
	committed := b.committed[c.group][c.topic]
	parts := b.assignedLocked(c)

	var out []Record
	for _, p := range parts {
		// Private cursor starts at the group's committed offset.
		start, ok := c.cursor[p]
		if !ok {
			start = committed[p]
		}
		log := t.partitions[p].log
		for off := start; off < int64(len(log)); off++ {
			if len(out) >= max {
				c.cursor[p] = off
				return out
			}
			m := log[off]
			out = append(out, Record{
				Partition: p,
				Offset:    off,
				Key:       append([]byte(nil), m.key...),
				Value:     append([]byte(nil), m.value...),
			})
			c.cursor[p] = off + 1
		}
	}
	return out
}

// Commit persists this consumer's read progress to the group. After Commit, a
// new consumer in the same group resumes after the committed offsets; without
// Commit, those records are redelivered (at-least-once).
func (c *Consumer) Commit() {
	b := c.broker
	b.mu.Lock()
	defer b.mu.Unlock()

	committed := b.committed[c.group][c.topic]
	for p, next := range c.cursor {
		if next > committed[p] {
			committed[p] = next
		}
	}
}
