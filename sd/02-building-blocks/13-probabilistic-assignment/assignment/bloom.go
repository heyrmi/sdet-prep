// Package bloomfilter is the Module 2.13 assignment: implement a Bloom filter.
//
// Read 02-building-blocks/13-probabilistic-structures.md first.
//
// A Bloom filter answers "have I seen this?" with a one-sided error:
//   - Test returns true  => the item is PROBABLY present (could be a false positive)
//   - Test returns false => the item is DEFINITELY absent (never a false negative)
//
// Fill in every method marked `// TODO`. Run the tests until green:
//
//	go test ./...
//
// Implementation notes:
//   - Store the m bits packed into a []uint64 (64 bits per word).
//   - Derive the k indices via DOUBLE HASHING from two base FNV hashes:
//       h1, h2 := baseHashes(data)
//       g_i = (h1 + i*h2) mod m,   for i = 0, 1, ..., k-1
//   - Use std lib only (hash/fnv, math).
package bloomfilter

import (
	"hash/fnv"
	"math"
)

// Bloom is a classic (non-counting) Bloom filter over m bits with k hash functions.
type Bloom struct {
	m    uint     // number of bits
	k    uint     // number of hash functions
	bits []uint64 // packed bit array, len == ceil(m/64)
}

// NewBloom creates a filter with exactly m bits and k hash functions.
// Both m and k are clamped to a minimum of 1.
func NewBloom(m uint, k uint) *Bloom {
	if m == 0 {
		m = 1
	}
	if k == 0 {
		k = 1
	}
	words := (m + 63) / 64
	return &Bloom{
		m:    m,
		k:    k,
		bits: make([]uint64, words),
	}
}

// NewBloomEstimate sizes a filter for n expected elements at the target
// falsePositive rate p (e.g. 0.01 for 1%). It computes the optimal m and k:
//
//	m = ceil( -n * ln(p) / (ln 2)^2 )
//	k = round( (m/n) * ln 2 )
//
// then delegates to NewBloom.
func NewBloomEstimate(n uint, falsePositive float64) *Bloom {
	// TODO:
	//  1. Guard inputs: if n == 0 treat as 1; clamp p into (0, 1) (e.g. if it's
	//     <= 0 or >= 1, fall back to a sane default like 0.01).
	//  2. Compute m = ceil( -n * ln(p) / (ln2)^2 ). Use math.Log, math.Ceil.
	//  3. Compute k = round( (m/n) * ln2 ). Use math.Round. Ensure k >= 1.
	//  4. return NewBloom(uint(m), uint(k)).
	panic("TODO: implement NewBloomEstimate")
}

// baseHashes returns two independent 64-bit base hashes of data using FNV-1a.
// We hash once with FNV-1a (h1), then hash the hash again (h2) to get a cheap
// second independent value for double hashing.
func baseHashes(data []byte) (uint64, uint64) {
	h := fnv.New64a()
	_, _ = h.Write(data)
	h1 := h.Sum64()

	// Second base hash: feed h1's bytes back through a fresh FNV-1a.
	h.Reset()
	var buf [8]byte
	for i := 0; i < 8; i++ {
		buf[i] = byte(h1 >> (8 * i))
	}
	_, _ = h.Write(buf[:])
	h2 := h.Sum64()
	if h2 == 0 {
		// h2 must be non-zero, else every g_i collapses to h1.
		h2 = 1
	}
	return h1, h2
}

// index returns the bit position for the i-th hash function via double hashing:
//
//	g_i = (h1 + i*h2) mod m
func (b *Bloom) index(h1, h2 uint64, i uint) uint {
	// TODO:
	//  return uint((h1 + uint64(i)*h2) % uint64(b.m))
	panic("TODO: implement Bloom.index")
}

// Add inserts data into the filter by setting all k bits.
func (b *Bloom) Add(data []byte) {
	// TODO:
	//  1. h1, h2 := baseHashes(data)
	//  2. for i in [0, b.k): idx := b.index(h1, h2, i); set that bit.
	//     Setting bit idx: b.bits[idx/64] |= 1 << (idx % 64)
	panic("TODO: implement Bloom.Add")
}

// Test reports whether data is PROBABLY present (true) or DEFINITELY absent (false).
func (b *Bloom) Test(data []byte) bool {
	// TODO:
	//  1. h1, h2 := baseHashes(data)
	//  2. for i in [0, b.k): idx := b.index(h1, h2, i);
	//     if that bit is NOT set, return false (definitely absent).
	//     Checking bit idx: b.bits[idx/64] & (1 << (idx % 64)) != 0
	//  3. all bits set => return true (probably present).
	panic("TODO: implement Bloom.Test")
}

// M returns the number of bits in the filter (handy for tests/sizing checks).
func (b *Bloom) M() uint { return b.m }

// K returns the number of hash functions.
func (b *Bloom) K() uint { return b.k }

// ensure math is referenced even before NewBloomEstimate is implemented.
var _ = math.Ln2
