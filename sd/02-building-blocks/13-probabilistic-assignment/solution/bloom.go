// Package bloomfilter is the reference solution for Module 2.13.
// Try the assignment yourself before reading this!
package bloomfilter

import (
	"hash/fnv"
	"math"
)

// Bloom is a classic (non-counting) Bloom filter over m bits with k hash functions.
type Bloom struct {
	m    uint
	k    uint
	bits []uint64
}

// NewBloom creates a filter with exactly m bits and k hash functions.
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

// NewBloomEstimate sizes a filter for n expected elements at false-positive rate p.
func NewBloomEstimate(n uint, falsePositive float64) *Bloom {
	if n == 0 {
		n = 1
	}
	p := falsePositive
	if p <= 0 || p >= 1 {
		p = 0.01
	}
	ln2 := math.Ln2
	// m = -n*ln(p) / (ln2)^2
	m := math.Ceil(-float64(n) * math.Log(p) / (ln2 * ln2))
	if m < 1 {
		m = 1
	}
	// k = round( (m/n) * ln2 )
	k := math.Round((m / float64(n)) * ln2)
	if k < 1 {
		k = 1
	}
	return NewBloom(uint(m), uint(k))
}

// baseHashes returns two independent 64-bit base hashes of data using FNV-1a.
func baseHashes(data []byte) (uint64, uint64) {
	h := fnv.New64a()
	_, _ = h.Write(data)
	h1 := h.Sum64()

	h.Reset()
	var buf [8]byte
	for i := 0; i < 8; i++ {
		buf[i] = byte(h1 >> (8 * i))
	}
	_, _ = h.Write(buf[:])
	h2 := h.Sum64()
	if h2 == 0 {
		h2 = 1
	}
	return h1, h2
}

// index returns the bit position for the i-th hash via double hashing.
func (b *Bloom) index(h1, h2 uint64, i uint) uint {
	return uint((h1 + uint64(i)*h2) % uint64(b.m))
}

// Add inserts data into the filter by setting all k bits.
func (b *Bloom) Add(data []byte) {
	h1, h2 := baseHashes(data)
	for i := uint(0); i < b.k; i++ {
		idx := b.index(h1, h2, i)
		b.bits[idx/64] |= 1 << (idx % 64)
	}
}

// Test reports whether data is PROBABLY present (true) or DEFINITELY absent (false).
func (b *Bloom) Test(data []byte) bool {
	h1, h2 := baseHashes(data)
	for i := uint(0); i < b.k; i++ {
		idx := b.index(h1, h2, i)
		if b.bits[idx/64]&(1<<(idx%64)) == 0 {
			return false
		}
	}
	return true
}

// M returns the number of bits in the filter.
func (b *Bloom) M() uint { return b.m }

// K returns the number of hash functions.
func (b *Bloom) K() uint { return b.k }
