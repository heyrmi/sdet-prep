// Package kvstore is the Module 4.2 assignment: implement a CONSISTENT HASH RING
// with virtual nodes — the partitioning primitive behind a Dynamo-style key-value
// store.
//
// Read 04-case-studies/02-key-value-store/README.md first.
//
// Fill in every method marked `// TODO`. Run the tests until green:
//
//	go test ./...
//
// The whole point of consistent hashing: when you add or remove a node, only a
// SMALL fraction of keys move (roughly 1/N), instead of nearly all of them as with
// `hash(key) % N`. Virtual nodes (a.k.a. replicas) spread each physical node across
// many points on the ring so the load is balanced.
package kvstore

import (
	"hash/crc32"
	"sort"
)

// Ring is a consistent hash ring. It maps keys to physical nodes.
//
// Internally it keeps a SORTED slice of hash points (one per virtual node) plus a
// lookup from each point back to the physical node that owns it. Lookups use binary
// search (sort.Search) to find the first point clockwise from the key's hash.
type Ring struct {
	replicas int               // virtual nodes per physical node
	points   []uint32          // sorted hash points on the ring
	owner    map[uint32]string // hash point -> physical node name
	nodes    map[string]bool   // set of physical nodes currently in the ring
}

// NewRing creates an empty ring. `replicas` is the number of virtual nodes created
// per physical node — more replicas means smoother key distribution (try 100–200 in
// production), at the cost of a little more memory and slower adds.
func NewRing(replicas int) *Ring {
	return &Ring{
		replicas: replicas,
		owner:    make(map[uint32]string),
		nodes:    make(map[string]bool),
	}
}

// hashKey maps an arbitrary string to a point on the ring (a uint32).
// Deterministic: the same string always hashes to the same point.
func (r *Ring) hashKey(s string) uint32 {
	return crc32.ChecksumIEEE([]byte(s))
}

// vnodeKey builds the string we hash for the i-th virtual node of a physical node.
// Using a distinct string per replica scatters a node's points around the ring.
func vnodeKey(node string, i int) string {
	// e.g. "node-a#0", "node-a#1", ...
	return node + "#" + itoa(i)
}

// AddNode inserts a physical node by placing `replicas` virtual nodes on the ring,
// then re-sorting the points slice so binary search stays valid.
func (r *Ring) AddNode(node string) {
	// TODO:
	//  1. If the node is already present (r.nodes[node]), return.
	//  2. Mark r.nodes[node] = true.
	//  3. For i in [0, r.replicas): compute p = r.hashKey(vnodeKey(node, i)),
	//     append p to r.points, and set r.owner[p] = node.
	//  4. Re-sort r.points ascending (sort.Slice) so binary search works.
	panic("TODO: implement Ring.AddNode")
}

// RemoveNode removes a physical node and all of its virtual nodes from the ring.
func (r *Ring) RemoveNode(node string) {
	// TODO:
	//  1. If the node is not present, return.
	//  2. delete(r.nodes, node).
	//  3. Rebuild r.points keeping only points whose owner != node, and delete those
	//     owner entries. (Simplest: build a fresh slice; it's already sorted because
	//     you filter the sorted slice in order.)
	panic("TODO: implement Ring.RemoveNode")
}

// GetNode returns the physical node that owns `key`: the owner of the first ring
// point at or clockwise after hash(key), wrapping around the ring.
// Returns "" if the ring is empty.
func (r *Ring) GetNode(key string) string {
	// TODO:
	//  1. If len(r.points) == 0, return "".
	//  2. h := r.hashKey(key).
	//  3. Use sort.Search to find the index of the first point >= h.
	//  4. If that index == len(r.points), wrap to 0 (the ring is circular).
	//  5. Return r.owner[r.points[idx]].
	panic("TODO: implement Ring.GetNode")
}

// GetNodes returns up to `n` DISTINCT physical nodes for `key`, walking clockwise
// from the key's hash. This is how replication picks the N nodes that store a key:
// the primary plus the next distinct nodes around the ring.
//
// If the ring has fewer than n physical nodes, it returns all of them.
func (r *Ring) GetNodes(key string, n int) []string {
	// TODO:
	//  1. If the ring is empty or n <= 0, return nil.
	//  2. Find the starting index with sort.Search (same as GetNode).
	//  3. Walk clockwise over r.points (wrapping with modulo). Collect owners,
	//     skipping nodes already collected, until you have n distinct nodes or you
	//     have visited every point.
	//  4. Return the collected slice.
	panic("TODO: implement Ring.GetNodes")
}

// itoa is a tiny dependency-free int->string (std lib strconv would be fine too;
// kept here so vnodeKey reads cleanly).
func itoa(i int) string {
	if i == 0 {
		return "0"
	}
	var buf [20]byte
	pos := len(buf)
	neg := i < 0
	if neg {
		i = -i
	}
	for i > 0 {
		pos--
		buf[pos] = byte('0' + i%10)
		i /= 10
	}
	if neg {
		pos--
		buf[pos] = '-'
	}
	return string(buf[pos:])
}

// Compile-time guard that sort is used (the solution uses sort.Search / sort.Slice).
var _ = sort.Search
