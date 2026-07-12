package lrucache

import (
	"sync"
	"testing"
)

// mustGet is a small helper: fail the test if the key isn't present.
func mustGet(t *testing.T, c *LRU, key string, want int) {
	t.Helper()
	got, ok := c.Get(key)
	if !ok {
		t.Fatalf("Get(%q): expected present, got miss", key)
	}
	if got != want {
		t.Fatalf("Get(%q): got %d, want %d", key, got, want)
	}
}

func mustMiss(t *testing.T, c *LRU, key string) {
	t.Helper()
	if v, ok := c.Get(key); ok {
		t.Fatalf("Get(%q): expected miss, got %d", key, v)
	}
}

func TestGetMissingKey(t *testing.T) {
	c := NewLRU(2)
	mustMiss(t, c, "nope")
}

func TestPutGet(t *testing.T) {
	c := NewLRU(2)
	c.Put("a", 1)
	c.Put("b", 2)
	mustGet(t, c, "a", 1)
	mustGet(t, c, "b", 2)
	if c.Len() != 2 {
		t.Fatalf("Len: got %d, want 2", c.Len())
	}
}

func TestUpdateExistingMovesToFront(t *testing.T) {
	c := NewLRU(2)
	c.Put("a", 1)
	c.Put("b", 2)
	// Re-Put "a": it should both update the value AND become most-recently-used.
	c.Put("a", 10)
	// Inserting "c" must now evict "b" (the LRU), not "a".
	c.Put("c", 3)

	mustGet(t, c, "a", 10) // updated value, survived
	mustMiss(t, c, "b")    // evicted
	mustGet(t, c, "c", 3)
}

func TestCapacityBound(t *testing.T) {
	c := NewLRU(3)
	for _, k := range []string{"a", "b", "c", "d", "e"} {
		c.Put(k, 1)
	}
	if c.Len() != 3 {
		t.Fatalf("Len: cache must never exceed capacity 3, got %d", c.Len())
	}
}

func TestEvictionOrderLRU(t *testing.T) {
	c := NewLRU(2)
	c.Put("a", 1)
	c.Put("b", 2)
	// "a" is now LRU. Inserting "c" should evict "a".
	c.Put("c", 3)
	mustMiss(t, c, "a")
	mustGet(t, c, "b", 2)
	mustGet(t, c, "c", 3)
}

// A Get must count as a use: it should rescue a key from being evicted next.
func TestGetRefreshesRecency(t *testing.T) {
	c := NewLRU(2)
	c.Put("a", 1)
	c.Put("b", 2)
	// Touch "a" so "b" becomes the least-recently-used.
	mustGet(t, c, "a", 1)
	// Inserting "c" should now evict "b", not "a".
	c.Put("c", 3)
	mustGet(t, c, "a", 1)
	mustMiss(t, c, "b")
	mustGet(t, c, "c", 3)
}

func TestZeroCapacityStoresNothing(t *testing.T) {
	c := NewLRU(0)
	c.Put("a", 1)
	mustMiss(t, c, "a")
	if c.Len() != 0 {
		t.Fatalf("zero-capacity cache should hold nothing, Len=%d", c.Len())
	}
}

// A long access pattern exercising repeated reordering + eviction.
func TestAccessPatternEviction(t *testing.T) {
	c := NewLRU(3)
	c.Put("a", 1)
	c.Put("b", 2)
	c.Put("c", 3) // order (MRU->LRU): c, b, a
	mustGet(t, c, "a", 1) // order: a, c, b
	c.Put("d", 4)         // evicts b -> order: d, a, c
	mustMiss(t, c, "b")
	mustGet(t, c, "c", 3) // order: c, d, a
	c.Put("e", 5)         // evicts a -> order: e, c, d
	mustMiss(t, c, "a")
	mustGet(t, c, "c", 3)
	mustGet(t, c, "d", 4)
	mustGet(t, c, "e", 5)
}

// Run with `go test -race` to prove the mutex actually protects shared state.
// We don't assert exact contents (scheduling is nondeterministic) — the point is
// that the race detector finds no unsynchronized access and nothing panics, and
// the cache never exceeds capacity.
func TestConcurrentAccess(t *testing.T) {
	c := NewLRU(64)
	var wg sync.WaitGroup
	for g := 0; g < 16; g++ {
		wg.Add(1)
		go func(g int) {
			defer wg.Done()
			for i := 0; i < 1000; i++ {
				key := string(rune('a' + (i+g)%26))
				c.Put(key, i)
				c.Get(key)
			}
		}(g)
	}
	wg.Wait()

	if c.Len() > 64 {
		t.Fatalf("cache exceeded capacity under concurrency: Len=%d", c.Len())
	}
}
