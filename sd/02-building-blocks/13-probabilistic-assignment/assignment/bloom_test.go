package bloomfilter

import (
	"fmt"
	"testing"
)

// key builds a deterministic byte key for index i (no randomness anywhere).
func key(prefix string, i int) []byte {
	return []byte(fmt.Sprintf("%s-%d", prefix, i))
}

// ---------- Core guarantee: NO false negatives ----------

// Everything Added MUST Test true. This is the property the whole structure rests on.
func TestNoFalseNegatives(t *testing.T) {
	b := NewBloomEstimate(1000, 0.01)

	const n = 1000
	for i := 0; i < n; i++ {
		b.Add(key("present", i))
	}
	for i := 0; i < n; i++ {
		if !b.Test(key("present", i)) {
			t.Fatalf("false negative: item %d was Added but Test returned false", i)
		}
	}
}

// Same guarantee with a hand-built filter (exercises NewBloom directly).
func TestNoFalseNegativesManual(t *testing.T) {
	b := NewBloom(1024, 5)
	items := [][]byte{
		[]byte("apple"), []byte("mango"), []byte("grape"),
		[]byte("lemon"), []byte(""), []byte("a much longer string key here"),
	}
	for _, it := range items {
		b.Add(it)
	}
	for _, it := range items {
		if !b.Test(it) {
			t.Fatalf("false negative: %q was Added but Test returned false", it)
		}
	}
}

// ---------- A specific known-absent item must Test false ----------

// With a generously sized, empty filter, a never-added item must report absent.
// (On an empty filter, ALL bits are 0, so any Test must be false — deterministic.)
func TestEmptyFilterReportsAbsent(t *testing.T) {
	b := NewBloom(4096, 7)
	absent := [][]byte{
		[]byte("nope"), []byte("definitely-not-here"), []byte("ghost"),
	}
	for _, it := range absent {
		if b.Test(it) {
			t.Fatalf("empty filter must report %q absent, got present", it)
		}
	}
}

// After adding a known set, a disjoint known-absent set should be reported
// absent. With a roomy filter and few items, we expect ZERO false positives
// here, deterministically.
func TestDisjointAbsentSet(t *testing.T) {
	b := NewBloom(8192, 6)
	for i := 0; i < 20; i++ {
		b.Add(key("in", i))
	}
	for i := 0; i < 20; i++ {
		if b.Test(key("out", i)) {
			t.Fatalf("known-absent item out-%d unexpectedly reported present "+
				"(roomy filter, few items => expected no false positive)", i)
		}
	}
}

// ---------- False-positive RATE stays under a generous bound ----------

// Tune for 1% over 1000 items, then measure the FP rate against a disjoint set
// of 10k never-added keys. Deterministic inputs; assert well under 5%.
func TestFalsePositiveRateUnderBound(t *testing.T) {
	const n = 1000
	b := NewBloomEstimate(n, 0.01)

	for i := 0; i < n; i++ {
		b.Add(key("member", i))
	}

	const trials = 10000
	falsePositives := 0
	for i := 0; i < trials; i++ {
		if b.Test(key("stranger", i)) { // "stranger-*" keys were never added
			falsePositives++
		}
	}
	rate := float64(falsePositives) / float64(trials)
	if rate > 0.05 {
		t.Fatalf("false-positive rate %.4f exceeds generous bound 0.05 (fp=%d/%d)",
			rate, falsePositives, trials)
	}
}

// ---------- Optimal m/k computation sanity ----------

func TestEstimateSizingSane(t *testing.T) {
	cases := []struct {
		n uint
		p float64
	}{
		{1000, 0.01},
		{100, 0.001},
		{1, 0.5},
		{1_000_000, 0.01},
	}
	for _, c := range cases {
		b := NewBloomEstimate(c.n, c.p)
		if b.M() == 0 {
			t.Fatalf("NewBloomEstimate(%d, %v): m must be > 0", c.n, c.p)
		}
		if b.K() < 1 {
			t.Fatalf("NewBloomEstimate(%d, %v): k must be >= 1, got %d", c.n, c.p, b.K())
		}
		// A lower target false-positive rate needs MORE bits than the count.
		if b.M() < c.n {
			t.Fatalf("NewBloomEstimate(%d, %v): expected m (%d) >= n for a small p",
				c.n, c.p, b.M())
		}
	}
}

// Degenerate inputs must not panic and must clamp to valid values.
func TestEstimateDegenerateInputs(t *testing.T) {
	for _, c := range []struct {
		n uint
		p float64
	}{
		{0, 0.01},   // zero elements
		{100, 0},    // impossible p
		{100, 1},    // impossible p
		{100, -1},   // negative p
		{100, 2},    // p > 1
	} {
		b := NewBloomEstimate(c.n, c.p)
		if b.M() == 0 || b.K() < 1 {
			t.Fatalf("degenerate NewBloomEstimate(%d, %v) must clamp to m>0,k>=1; got m=%d k=%d",
				c.n, c.p, b.M(), b.K())
		}
	}
}

// NewBloom must clamp zero arguments rather than producing an unusable filter.
func TestNewBloomClampsZero(t *testing.T) {
	b := NewBloom(0, 0)
	if b.M() == 0 || b.K() < 1 {
		t.Fatalf("NewBloom(0,0) must clamp to m>0,k>=1; got m=%d k=%d", b.M(), b.K())
	}
	// And it must still function: add then test must be true.
	b.Add([]byte("x"))
	if !b.Test([]byte("x")) {
		t.Fatal("clamped filter failed the no-false-negative guarantee")
	}
}
