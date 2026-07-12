// Package urlshortener is the reference solution for Module 4.4.
// Try the assignment yourself before reading this!
package urlshortener

import (
	"errors"
	"strings"
	"sync"
)

const alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

const base = uint64(len(alphabet))

var ErrAliasTaken = errors.New("alias already taken")

var ErrInvalidCode = errors.New("invalid base62 code")

// ---------------- Base62 ----------------

func Encode(n uint64) string {
	if n == 0 {
		return "0"
	}
	var b []byte
	for n > 0 {
		b = append(b, alphabet[n%base])
		n /= base
	}
	// b holds digits low→high; reverse in place.
	for i, j := 0, len(b)-1; i < j; i, j = i+1, j-1 {
		b[i], b[j] = b[j], b[i]
	}
	return string(b)
}

func Decode(s string) (uint64, error) {
	if s == "" {
		return 0, ErrInvalidCode
	}
	var n uint64
	for i := 0; i < len(s); i++ {
		idx := strings.IndexByte(alphabet, s[i])
		if idx < 0 {
			return 0, ErrInvalidCode
		}
		n = n*base + uint64(idx)
	}
	return n, nil
}

// ---------------- Shortener ----------------

type Shortener struct {
	mu        sync.Mutex
	byCode    map[string]string
	codeByURL map[string]string
	idgen     func() uint64
}

func NewShortener(idgen func() uint64) *Shortener {
	return &Shortener{
		byCode:    make(map[string]string),
		codeByURL: make(map[string]string),
		idgen:     idgen,
	}
}

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

func (s *Shortener) Shorten(longURL string) (code string, err error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	if existing, ok := s.codeByURL[longURL]; ok {
		return existing, nil // dedup: same URL → same code
	}
	code = Encode(s.idgen())
	s.byCode[code] = longURL
	s.codeByURL[longURL] = code
	return code, nil
}

func (s *Shortener) ShortenCustom(longURL, alias string) error {
	s.mu.Lock()
	defer s.mu.Unlock()

	if _, taken := s.byCode[alias]; taken {
		return ErrAliasTaken
	}
	s.byCode[alias] = longURL
	s.codeByURL[longURL] = alias
	return nil
}

func (s *Shortener) Resolve(code string) (longURL string, ok bool) {
	s.mu.Lock()
	defer s.mu.Unlock()

	longURL, ok = s.byCode[code]
	return longURL, ok
}
