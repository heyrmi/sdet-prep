// Package urlshortener is the Module 4.4 assignment: implement the core of a
// URL shortener — base62 encoding and a small Shortener service.
//
// Read 04-case-studies/04-url-shortener/README.md first.
//
// Fill in every function marked `// TODO`. Run the tests until green:
//
//	go test ./...
//	go test -race ./...   // Shorten must be safe under concurrency
//
// The Shortener takes its IDs from an injectable `idgen func() uint64` so tests
// can drive ID generation deterministically. In production this is just a counter.
package urlshortener

import (
	"errors"
	"sync"
)

// alphabet is the base62 symbol set: digits, then uppercase, then lowercase.
// Index i maps to value i (so '0'=0, 'A'=10, 'a'=36).
const alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

// base is the radix (62).
const base = uint64(len(alphabet))

// ErrAliasTaken is returned by ShortenCustom when the requested alias already exists.
var ErrAliasTaken = errors.New("alias already taken")

// ErrInvalidCode is returned by Decode when the string contains a non-base62 character.
var ErrInvalidCode = errors.New("invalid base62 code")

// ----------------------------------------------------------------------------
// Base62 encode / decode
//
// Encode is plain base-conversion: repeatedly take n % base for the next digit
// (low to high), then divide. Decode is the inverse Horner evaluation.
// Encode(0) must return "0" (not the empty string).
// ----------------------------------------------------------------------------

// Encode returns the base62 representation of n over the alphabet above.
func Encode(n uint64) string {
	// TODO:
	//  1. If n == 0, return "0".
	//  2. Otherwise, while n > 0: prepend alphabet[n%base], then n /= base.
	//     (Collect digits low→high then reverse, or prepend each time.)
	panic("TODO: implement Encode")
}

// Decode parses a base62 string back into its uint64 value.
// It returns ErrInvalidCode if s is empty or contains a character not in the alphabet.
func Decode(s string) (uint64, error) {
	// TODO:
	//  1. If s == "", return ErrInvalidCode.
	//  2. For each byte c: find its index in the alphabet (0..61). If not found,
	//     return ErrInvalidCode.
	//  3. Accumulate: n = n*base + index. Return n.
	panic("TODO: implement Decode")
}

// ----------------------------------------------------------------------------
// Shortener service
//
// Backed by two in-memory maps:
//   - byCode:    code     -> longURL   (the forward / resolve map)
//   - codeByURL: longURL  -> code      (the reverse / dedup map)
//
// IDs come from idgen (injected for determinism). Shorten base62-encodes the next ID.
// All map access is guarded by mu so Shorten is safe under concurrency.
// ----------------------------------------------------------------------------

type Shortener struct {
	mu        sync.Mutex
	byCode    map[string]string
	codeByURL map[string]string
	idgen     func() uint64
}

// NewShortener returns a Shortener whose generated codes are the base62 encodings
// of the integers produced by idgen. A typical idgen is a counter starting at some
// offset; tests inject a deterministic one.
func NewShortener(idgen func() uint64) *Shortener {
	return &Shortener{
		byCode:    make(map[string]string),
		codeByURL: make(map[string]string),
		idgen:     idgen,
	}
}

// NewCounterShortener is a convenience constructor: IDs start at `start` and
// increase by 1 on each Shorten. (Useful for quick manual experiments.)
func NewCounterShortener(start uint64) *Shortener {
	var mu sync.Mutex
	next := start
	return NewShortener(func() uint64 {
		mu.Lock()
		defer mu.Unlock()
		id := next
		next++
		return id
	})
}

// Shorten returns a short code for longURL. If longURL was shortened before, the
// SAME code is returned (dedup). Otherwise it mints the next ID, base62-encodes it,
// stores both map directions, and returns the new code.
func (s *Shortener) Shorten(longURL string) (code string, err error) {
	// TODO:
	//  1. Lock the mutex (called from many goroutines).
	//  2. If longURL is already in codeByURL, return the existing code (dedup).
	//  3. Otherwise: id := s.idgen(); code := Encode(id).
	//  4. Store byCode[code] = longURL and codeByURL[longURL] = code. Return code.
	panic("TODO: implement Shorten")
}

// ShortenCustom registers a caller-chosen alias for longURL.
// It returns ErrAliasTaken if the alias is already mapped to some URL.
func (s *Shortener) ShortenCustom(longURL, alias string) error {
	// TODO:
	//  1. Lock.
	//  2. If alias already exists in byCode, return ErrAliasTaken.
	//  3. Otherwise store byCode[alias] = longURL and codeByURL[longURL] = alias. Return nil.
	panic("TODO: implement ShortenCustom")
}

// Resolve returns the long URL for a code. ok is false if the code is unknown.
func (s *Shortener) Resolve(code string) (longURL string, ok bool) {
	// TODO:
	//  1. Lock.
	//  2. Look up code in byCode; return the value and whether it was present.
	panic("TODO: implement Resolve")
}
