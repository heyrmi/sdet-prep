// Package lrucache is the reference solution for Module 2.3.
// Try the assignment yourself before reading this!
package lrucache

import "sync"

type node struct {
	key   string
	value int
	prev  *node
	next  *node
}

type LRU struct {
	mu       sync.Mutex
	capacity int
	items    map[string]*node
	head     *node // sentinel; head.next is most-recently-used
	tail     *node // sentinel; tail.prev is least-recently-used
}

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

func (c *LRU) Get(key string) (int, bool) {
	c.mu.Lock()
	defer c.mu.Unlock()

	n, ok := c.items[key]
	if !ok {
		return 0, false
	}
	c.moveToFront(n)
	return n.value, true
}

func (c *LRU) Put(key string, value int) {
	c.mu.Lock()
	defer c.mu.Unlock()

	if c.capacity <= 0 {
		return
	}

	if n, ok := c.items[key]; ok {
		n.value = value
		c.moveToFront(n)
		return
	}

	if len(c.items) == c.capacity {
		lru := c.tail.prev // least-recently-used real node
		c.remove(lru)
		delete(c.items, lru.key)
	}

	n := &node{key: key, value: value}
	c.addToFront(n)
	c.items[key] = n
}

func (c *LRU) Len() int {
	c.mu.Lock()
	defer c.mu.Unlock()
	return len(c.items)
}

func (c *LRU) addToFront(n *node) {
	n.prev = c.head
	n.next = c.head.next
	c.head.next.prev = n
	c.head.next = n
}

func (c *LRU) remove(n *node) {
	n.prev.next = n.next
	n.next.prev = n.prev
}

func (c *LRU) moveToFront(n *node) {
	c.remove(n)
	c.addToFront(n)
}
