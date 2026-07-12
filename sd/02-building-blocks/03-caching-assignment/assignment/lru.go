// Package lrucache is the Module 2.3 assignment: implement an LRU
// (Least Recently Used) cache with O(1) Get and Put.
//
// Read 02-building-blocks/03-caching.md first.
//
// The trick is two data structures working together:
//
//	hash map:           key -> *node          (O(1) lookup)
//	doubly linked list: most-recent ... least-recent   (O(1) reorder/evict)
//
// On every access (Get or Put) you move the touched node to the FRONT
// (most-recently-used end). When the cache is full and you insert a new key,
// you evict the node at the BACK (least-recently-used end).
//
// We keep the cache mutex-protected so it is safe under concurrency. The tests
// run with `-race`, so all access to shared fields must happen under the lock.
//
// Fill in every method marked `// TODO`. Run the tests until green:
//
//	go test ./...
//	go test -race ./...
package lrucache

import "sync"

// node is one entry in the doubly linked list. We store the key (not just the
// value) so that when we evict the back node we know which map entry to delete.
type node struct {
	key   string
	value int
	prev  *node
	next  *node
}

// LRU is a fixed-capacity cache. The list is ordered:
//
//	head <-> (most recent) <-> ... <-> (least recent) <-> tail
//
// head and tail are sentinel (dummy) nodes so we never deal with nil edge cases
// when inserting or removing — the real entries always sit strictly between them.
type LRU struct {
	mu       sync.Mutex
	capacity int
	items    map[string]*node
	head     *node // sentinel; head.next is the most-recently-used real node
	tail     *node // sentinel; tail.prev is the least-recently-used real node
}

// NewLRU creates an empty cache that holds at most `capacity` entries.
// A capacity <= 0 is treated as a cache that holds nothing.
func NewLRU(capacity int) *LRU {
	head := &node{}
	tail := &node{}
	head.next = tail
	tail.prev = head
	return &LRU{
		capacity: capacity,
		items:    make(map[string]*node),
		head:     head,
		tail:     tail,
	}
}

// Get returns the value for key and true if present, or (0, false) if missing.
// A hit makes the key the most-recently-used entry.
func (c *LRU) Get(key string) (int, bool) {
	// TODO:
	//  1. Lock the mutex (defer Unlock).
	//  2. Look the key up in c.items. If absent, return (0, false).
	//  3. If present, move its node to the front (most-recently-used) and
	//     return (node.value, true).
	panic("TODO: implement LRU.Get")
}

// Put inserts or updates key=value and makes it the most-recently-used entry.
// If inserting a NEW key would exceed capacity, evict the least-recently-used
// entry first.
func (c *LRU) Put(key string, value int) {
	// TODO:
	//  1. Lock the mutex (defer Unlock).
	//  2. If capacity <= 0, there is nowhere to store anything — just return.
	//  3. If key already exists: update its value, move it to the front, return.
	//  4. Otherwise it's a new key:
	//       - if len(items) == capacity, evict the node at the back
	//         (c.tail.prev) and delete it from the map,
	//       - create a node, insert it at the front, add it to the map.
	panic("TODO: implement LRU.Put")
}

// Len returns the current number of entries (handy for tests).
func (c *LRU) Len() int {
	c.mu.Lock()
	defer c.mu.Unlock()
	return len(c.items)
}

// ---- helper methods you'll want for the doubly linked list ----
//
// These are not strictly required, but implementing Get/Put is much cleaner if
// you build these tiny O(1) primitives first.

// addToFront inserts n right after the head sentinel.
func (c *LRU) addToFront(n *node) {
	// TODO: splice n in between c.head and c.head.next.
	panic("TODO: implement addToFront")
}

// remove unlinks n from the list.
func (c *LRU) remove(n *node) {
	// TODO: point n.prev and n.next at each other.
	panic("TODO: implement remove")
}

// moveToFront is remove followed by addToFront.
func (c *LRU) moveToFront(n *node) {
	// TODO: reuse remove + addToFront.
	panic("TODO: implement moveToFront")
}
