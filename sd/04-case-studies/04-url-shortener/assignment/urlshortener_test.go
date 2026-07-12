package urlshortener

import (
	"sync"
	"sync/atomic"
	"testing"
)

// ---------- Base62 ----------

func TestBase62RoundTrip(t *testing.T) {
	cases := []uint64{
		0, 1, 9, 10, 61, 62, 63, 124, 125, 3843, 3844,
		1000, 1_000_000, 1_000_000_000, 1<<32 - 1, 1 << 40,
		1<<63 + 12345, ^uint64(0), // max uint64
	}
	for _, n := range cases {
		s := Encode(n)
		got, err := Decode(s)
		if err != nil {
			t.Fatalf("Decode(Encode(%d)=%q) returned error: %v", n, s, err)
		}
		if got != n {
			t.Fatalf("round-trip failed: %d -> %q -> %d", n, s, got)
		}
	}
}

func TestEncodeZero(t *testing.T) {
	if got := Encode(0); got != "0" {
		t.Fatalf("Encode(0) = %q, want \"0\"", got)
	}
}

func TestEncodeKnownValues(t *testing.T) {
	// 62 is "10" (1*62 + 0); 125 is "21" (2*62 + 1).
	if got := Encode(62); got != "10" {
		t.Fatalf("Encode(62) = %q, want \"10\"", got)
	}
	if got := Encode(125); got != "21" {
		t.Fatalf("Encode(125) = %q, want \"21\"", got)
	}
	// First lowercase letter 'a' has value 36.
	if got := Encode(36); got != "a" {
		t.Fatalf("Encode(36) = %q, want \"a\"", got)
	}
}

func TestDecodeInvalid(t *testing.T) {
	for _, s := range []string{"", "abc!", "with space", "_", "-"} {
		if _, err := Decode(s); err == nil {
			t.Fatalf("Decode(%q) should have returned an error", s)
		}
	}
}

// ---------- Shortener: a deterministic id source ----------

// seqGen returns an idgen that yields start, start+1, start+2, ... deterministically.
func seqGen(start uint64) func() uint64 {
	var mu sync.Mutex
	next := start
	return func() uint64 {
		mu.Lock()
		defer mu.Unlock()
		id := next
		next++
		return id
	}
}

func TestShortenResolvable(t *testing.T) {
	s := NewShortener(seqGen(1000)) // first code = Encode(1000)
	code, err := s.Shorten("https://example.com/a")
	if err != nil {
		t.Fatalf("Shorten error: %v", err)
	}
	if code != Encode(1000) {
		t.Fatalf("first code = %q, want Encode(1000) = %q", code, Encode(1000))
	}
	got, ok := s.Resolve(code)
	if !ok || got != "https://example.com/a" {
		t.Fatalf("Resolve(%q) = (%q, %v), want the original URL", code, got, ok)
	}
}

func TestResolveUnknown(t *testing.T) {
	s := NewShortener(seqGen(1))
	if got, ok := s.Resolve("nope"); ok {
		t.Fatalf("Resolve of unknown code returned ok=true (got %q)", got)
	}
}

func TestShortenDedup(t *testing.T) {
	s := NewShortener(seqGen(1))
	c1, _ := s.Shorten("https://example.com/same")
	c2, _ := s.Shorten("https://example.com/same")
	if c1 != c2 {
		t.Fatalf("dedup failed: same URL produced %q and %q", c1, c2)
	}
	// A different URL must get a different code.
	c3, _ := s.Shorten("https://example.com/other")
	if c3 == c1 {
		t.Fatalf("different URLs must get different codes; both got %q", c1)
	}
}

func TestShortenCustom(t *testing.T) {
	s := NewShortener(seqGen(1))
	if err := s.ShortenCustom("https://example.com/talk", "my-talk"); err != nil {
		t.Fatalf("ShortenCustom error: %v", err)
	}
	got, ok := s.Resolve("my-talk")
	if !ok || got != "https://example.com/talk" {
		t.Fatalf("Resolve(\"my-talk\") = (%q, %v), want the custom URL", got, ok)
	}
}

func TestShortenCustomCollision(t *testing.T) {
	s := NewShortener(seqGen(1))
	if err := s.ShortenCustom("https://example.com/a", "dup"); err != nil {
		t.Fatalf("first ShortenCustom error: %v", err)
	}
	if err := s.ShortenCustom("https://example.com/b", "dup"); err != ErrAliasTaken {
		t.Fatalf("second ShortenCustom = %v, want ErrAliasTaken", err)
	}
}

// ---------- Concurrency (run with -race) ----------

func TestShortenConcurrentUnique(t *testing.T) {
	const n = 1000
	// Shared atomic counter as the id source: thread-safe and gives n distinct ids.
	var ctr uint64
	s := NewShortener(func() uint64 { return atomic.AddUint64(&ctr, 1) })

	var wg sync.WaitGroup
	codes := make([]string, n)
	for i := 0; i < n; i++ {
		wg.Add(1)
		go func(i int) {
			defer wg.Done()
			// Each goroutine shortens a unique URL so dedup doesn't merge them.
			code, err := s.Shorten("https://example.com/" + Encode(uint64(i)))
			if err != nil {
				t.Errorf("Shorten error: %v", err)
				return
			}
			codes[i] = code
		}(i)
	}
	wg.Wait()

	// Every code must be unique and resolvable.
	seen := make(map[string]bool, n)
	for i, code := range codes {
		if code == "" {
			t.Fatalf("goroutine %d produced empty code", i)
		}
		if seen[code] {
			t.Fatalf("duplicate code produced under concurrency: %q", code)
		}
		seen[code] = true
		if _, ok := s.Resolve(code); !ok {
			t.Fatalf("code %q not resolvable after concurrent Shorten", code)
		}
	}
	if len(seen) != n {
		t.Fatalf("expected %d unique codes, got %d", n, len(seen))
	}
}
