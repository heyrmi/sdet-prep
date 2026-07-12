// Package mq is the Module 4.13 assignment (Distributed Message Queue):
// build an in-memory, Kafka-lite broker with partitioned append-only logs,
// offsets, consumer groups, commits, replay, and at-least-once delivery.
//
// Read 04-case-studies/13-message-queue/README.md first.
//
// Fill in every function marked `// TODO`. Run the tests until green:
//
//	go test ./...
//	go test -race ./...   // Produce must be safe under concurrency
//
// Standard library only.
package mq

import (
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

// Broker holds topics and consumer-group state. All methods must be safe for
// concurrent use (guard shared state with b.mu).
type Broker struct {
	mu     sync.Mutex
	topics map[string]*topic
	// committed[group][topic][partition] = next offset to read for that group.
	committed map[string]map[string][]int64
	// groups tracks the live consumers per group for partition assignment.
	groups map[string]*group
}

type group struct {
	topic      string
	members    []*Consumer // ordered by join time
	partitionN int
}

// NewBroker creates an empty broker.
func NewBroker() *Broker {
	return &Broker{
		topics:    make(map[string]*topic),
		committed: make(map[string]map[string][]int64),
		groups:    make(map[string]*group),
	}
}

// CreateTopic registers a topic with the given number of partitions (>= 1).
func (b *Broker) CreateTopic(name string, partitions int) error {
	// TODO:
	//  - reject partitions < 1.
	//  - lock; reject if the topic already exists.
	//  - create a *topic with `partitions` empty *partition logs; store it.
	panic("TODO: implement CreateTopic")
}

// partitionFor maps a key to a partition index in [0, n) via FNV-1a hash.
func partitionFor(key []byte, n int) int {
	h := fnv.New32a()
	_, _ = h.Write(key)
	return int(h.Sum32() % uint32(n))
}

// Produce appends (key, value) to the partition chosen by hash(key) and returns
// the partition index and the offset the record landed at.
func (b *Broker) Produce(topicName string, key, value []byte) (int, int64, error) {
	// TODO:
	//  - lock; look up the topic (error if missing).
	//  - p := partitionFor(key, numPartitions).
	//  - offset := current length of partition p's log.
	//  - append a message (copy the key/value byte slices) and return p, offset.
	panic("TODO: implement Produce")
}

// Consumer reads from the partitions assigned to it within its group.
type Consumer struct {
	broker *Broker
	group  string
	topic  string
	// cursor[partition] = next offset this consumer will read (its private view
	// until Commit publishes progress to the group).
	cursor map[int]int64
}

// Join adds a consumer to `groupName` on `topicName`. Joining re-assigns
// partitions across the group's current members (see assignedLocked).
func (b *Broker) Join(groupName, topicName string) (*Consumer, error) {
	// TODO:
	//  - lock; look up the topic (error if missing).
	//  - ensure committed[groupName][topicName] exists as a []int64 of length
	//    numPartitions (zero-valued => a fresh group reads from offset 0).
	//  - ensure groups[groupName] exists; append a new *Consumer to its members.
	//  - return the consumer (with an initialized cursor map).
	panic("TODO: implement Join")
}

// assignedLocked returns the sorted partitions assigned to consumer c.
// Caller must hold b.mu.
//
// Use a deterministic round-robin so that, with n members, member i owns
// partitions p where p mod n == (n-1-i). The (n-1-i) twist means the most
// recently joined member takes the lowest partitions — so when a new consumer
// joins after another effectively "left" (crashed without committing), it
// takes over those partitions and redelivers the uncommitted records.
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

// Assignment returns the partitions currently assigned to this consumer.
func (c *Consumer) Assignment() []int {
	c.broker.mu.Lock()
	defer c.broker.mu.Unlock()
	return c.broker.assignedLocked(c)
}

// Poll returns up to `max` of the next un-consumed records across this
// consumer's assigned partitions. The private cursor advances so repeated
// Polls don't re-read, but records are NOT visible to the group until Commit.
func (c *Consumer) Poll(max int) []Record {
	// TODO:
	//  - lock the broker.
	//  - for each assigned partition (b.assignedLocked(c)):
	//      * start at c.cursor[p] if set, else the group's committed[p].
	//      * append records (copy key/value) until you hit the end of the log
	//        or len(out) == max; advance c.cursor[p] as you go.
	//  - return the collected records.
	panic("TODO: implement Poll")
}

// Commit publishes this consumer's read progress to the group. Afterwards a new
// consumer in the same group resumes after the committed offsets; without a
// Commit, those records are redelivered (at-least-once).
func (c *Consumer) Commit() {
	// TODO:
	//  - lock; for each partition in c.cursor, advance the group's committed
	//    offset to max(committed[p], cursor[p]).
	panic("TODO: implement Commit")
}
