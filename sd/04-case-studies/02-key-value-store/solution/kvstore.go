// Package kvstore is the reference solution for Module 4.2: a consistent hash ring
// with virtual nodes. Try the assignment yourself before reading this!
package kvstore

import (
	"hash/crc32"
	"sort"
)

type Ring struct {
	replicas int
	points   []uint32
	owner    map[uint32]string
	nodes    map[string]bool
}

func NewRing(replicas int) *Ring {
	return &Ring{
		replicas: replicas,
		owner:    make(map[uint32]string),
		nodes:    make(map[string]bool),
	}
}

func (r *Ring) hashKey(s string) uint32 {
	return crc32.ChecksumIEEE([]byte(s))
}

func vnodeKey(node string, i int) string {
	return node + "#" + itoa(i)
}

func (r *Ring) AddNode(node string) {
	if r.nodes[node] {
		return
	}
	r.nodes[node] = true
	for i := 0; i < r.replicas; i++ {
		p := r.hashKey(vnodeKey(node, i))
		r.points = append(r.points, p)
		r.owner[p] = node
	}
	sort.Slice(r.points, func(a, b int) bool { return r.points[a] < r.points[b] })
}

func (r *Ring) RemoveNode(node string) {
	if !r.nodes[node] {
		return
	}
	delete(r.nodes, node)
	kept := r.points[:0:0] // fresh backing array, keeps sorted order
	for _, p := range r.points {
		if r.owner[p] == node {
			delete(r.owner, p)
			continue
		}
		kept = append(kept, p)
	}
	r.points = kept
}

func (r *Ring) GetNode(key string) string {
	if len(r.points) == 0 {
		return ""
	}
	h := r.hashKey(key)
	idx := sort.Search(len(r.points), func(i int) bool { return r.points[i] >= h })
	if idx == len(r.points) {
		idx = 0 // wrap around the circle
	}
	return r.owner[r.points[idx]]
}

func (r *Ring) GetNodes(key string, n int) []string {
	if len(r.points) == 0 || n <= 0 {
		return nil
	}
	h := r.hashKey(key)
	start := sort.Search(len(r.points), func(i int) bool { return r.points[i] >= h })
	if start == len(r.points) {
		start = 0
	}

	var result []string
	seen := make(map[string]bool)
	for i := 0; i < len(r.points) && len(result) < n; i++ {
		p := r.points[(start+i)%len(r.points)]
		owner := r.owner[p]
		if seen[owner] {
			continue
		}
		seen[owner] = true
		result = append(result, owner)
	}
	return result
}

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
