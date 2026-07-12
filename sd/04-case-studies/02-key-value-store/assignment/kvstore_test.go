package kvstore

import (
	"fmt"
	"testing"
)

// keys generates n deterministic test keys.
func keys(n int) []string {
	ks := make([]string, n)
	for i := 0; i < n; i++ {
		ks[i] = fmt.Sprintf("key-%d", i)
	}
	return ks
}

// ---------- consistency ----------

// The same key must always map to the same node, on a fixed ring.
func TestGetNodeIsDeterministic(t *testing.T) {
	r := NewRing(50)
	for _, n := range []string{"a", "b", "c"} {
		r.AddNode(n)
	}
	for _, k := range keys(1000) {
		first := r.GetNode(k)
		if first == "" {
			t.Fatalf("key %q mapped to empty node", k)
		}
		for i := 0; i < 5; i++ {
			if got := r.GetNode(k); got != first {
				t.Fatalf("key %q mapped inconsistently: %q then %q", k, first, got)
			}
		}
	}
}

func TestEmptyRing(t *testing.T) {
	r := NewRing(50)
	if got := r.GetNode("anything"); got != "" {
		t.Fatalf("empty ring should return \"\", got %q", got)
	}
	if got := r.GetNodes("anything", 3); got != nil {
		t.Fatalf("empty ring GetNodes should return nil, got %v", got)
	}
}

// ---------- the headline property: adding a node remaps only a minority ----------

func TestAddNodeRemapsMinority(t *testing.T) {
	const numKeys = 10000
	r := NewRing(100)
	for _, n := range []string{"n1", "n2", "n3", "n4"} {
		r.AddNode(n)
	}

	ks := keys(numKeys)
	before := make(map[string]string, numKeys)
	for _, k := range ks {
		before[k] = r.GetNode(k)
	}

	r.AddNode("n5") // 4 -> 5 nodes

	moved := 0
	for _, k := range ks {
		if r.GetNode(k) != before[k] {
			moved++
		}
	}
	frac := float64(moved) / float64(numKeys)
	// With consistent hashing, going 4->5 nodes should move ~1/5 (20%) of keys.
	// `hash % N` would move ~80%. Assert we are comfortably under half.
	if frac >= 0.5 {
		t.Fatalf("adding a node remapped %.1f%% of keys; consistent hashing should move well under 50%%", frac*100)
	}
	if frac == 0 {
		t.Fatalf("adding a node moved 0 keys — that cannot be right for 10k keys")
	}
	t.Logf("4->5 nodes remapped %.1f%% of keys", frac*100)
}

// Removing a node should likewise only move that node's keys.
func TestRemoveNodeRemapsMinority(t *testing.T) {
	const numKeys = 10000
	r := NewRing(100)
	for _, n := range []string{"n1", "n2", "n3", "n4", "n5"} {
		r.AddNode(n)
	}
	ks := keys(numKeys)
	before := make(map[string]string, numKeys)
	for _, k := range ks {
		before[k] = r.GetNode(k)
	}

	r.RemoveNode("n3")

	moved := 0
	for _, k := range ks {
		now := r.GetNode(k)
		if now == "n3" {
			t.Fatalf("key %q still maps to removed node n3", k)
		}
		if now != before[k] {
			moved++
		}
	}
	frac := float64(moved) / float64(numKeys)
	// Only keys that were on n3 (~1/5) should move.
	if frac >= 0.5 {
		t.Fatalf("removing a node remapped %.1f%% of keys; expected well under 50%%", frac*100)
	}
	t.Logf("removing 1 of 5 nodes remapped %.1f%% of keys", frac*100)
}

// ---------- replication: GetNodes returns n distinct physical nodes ----------

func TestGetNodesDistinct(t *testing.T) {
	r := NewRing(100)
	for _, n := range []string{"a", "b", "c", "d", "e"} {
		r.AddNode(n)
	}
	for _, k := range keys(1000) {
		got := r.GetNodes(k, 3)
		if len(got) != 3 {
			t.Fatalf("key %q: expected 3 nodes, got %d (%v)", k, len(got), got)
		}
		seen := map[string]bool{}
		for _, n := range got {
			if seen[n] {
				t.Fatalf("key %q: GetNodes returned duplicate node %q in %v", k, n, got)
			}
			seen[n] = true
		}
		// The first replica must equal GetNode (the primary).
		if got[0] != r.GetNode(k) {
			t.Fatalf("key %q: GetNodes[0]=%q but GetNode=%q", k, got[0], r.GetNode(k))
		}
	}
}

// Asking for more replicas than there are nodes returns all distinct nodes.
func TestGetNodesMoreThanNodes(t *testing.T) {
	r := NewRing(100)
	for _, n := range []string{"a", "b", "c"} {
		r.AddNode(n)
	}
	got := r.GetNodes("some-key", 10)
	if len(got) != 3 {
		t.Fatalf("only 3 nodes exist; expected 3, got %d (%v)", len(got), got)
	}
}

// ---------- balance: with enough vnodes, load is roughly even ----------

func TestDistributionRoughlyBalanced(t *testing.T) {
	const numKeys = 10000
	nodes := []string{"a", "b", "c", "d", "e"}
	r := NewRing(150)
	for _, n := range nodes {
		r.AddNode(n)
	}
	counts := map[string]int{}
	for _, k := range keys(numKeys) {
		counts[r.GetNode(k)]++
	}
	// Perfectly even would be 20% each. With 150 vnodes per node, every node should
	// get a healthy share — assert each gets at least 5% (a generous lower bound).
	for _, n := range nodes {
		frac := float64(counts[n]) / float64(numKeys)
		if frac < 0.05 {
			t.Fatalf("node %q got only %.1f%% of keys; distribution too skewed (counts=%v)", n, frac*100, counts)
		}
	}
	t.Logf("distribution over 5 nodes: %v", counts)
}
