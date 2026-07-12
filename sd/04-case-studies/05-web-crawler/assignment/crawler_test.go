package crawler

import (
	"errors"
	"sort"
	"testing"
)

// mockFetcher is a deterministic, in-memory web graph. Fetch returns the links for a
// URL; a URL listed in `errs` returns an error (and no links). Unknown URLs return no
// links and no error (a dead-end leaf). It is safe for concurrent use because it is
// read-only after construction.
type mockFetcher struct {
	graph map[string][]string
	errs  map[string]bool
}

func (m *mockFetcher) Fetch(url string) ([]string, error) {
	if m.errs[url] {
		return nil, errors.New("fetch failed: " + url)
	}
	return m.graph[url], nil
}

func sortedCopy(s []string) []string {
	out := append([]string(nil), s...)
	sort.Strings(out)
	return out
}

func assertSet(t *testing.T, got, want []string) {
	t.Helper()
	g, w := sortedCopy(got), sortedCopy(want)
	if len(g) != len(w) {
		t.Fatalf("visited set size mismatch:\n got  %v\n want %v", g, w)
	}
	for i := range g {
		if g[i] != w[i] {
			t.Fatalf("visited set mismatch:\n got  %v\n want %v", g, w)
		}
	}
}

// hasDuplicates reports whether any URL appears more than once.
func hasDuplicates(s []string) (string, bool) {
	seen := map[string]bool{}
	for _, u := range s {
		if seen[u] {
			return u, true
		}
		seen[u] = true
	}
	return "", false
}

// workerCounts exercises a range including 1 (serial-ish) and high concurrency.
var workerCounts = []int{1, 2, 4, 8, 50}

func TestCrawlReachesAllNoDuplicates(t *testing.T) {
	f := &mockFetcher{graph: map[string][]string{
		"a": {"b", "c"},
		"b": {"d"},
		"c": {"d", "e"},
		"d": {},
		"e": {},
	}}
	want := []string{"a", "b", "c", "d", "e"}

	for _, w := range workerCounts {
		got := Crawl("a", f, w)
		if dup, ok := hasDuplicates(got); ok {
			t.Fatalf("workers=%d: URL %q visited more than once", w, dup)
		}
		assertSet(t, got, want)
	}
}

func TestCrawlCyclesDoNotHang(t *testing.T) {
	// a -> b -> c -> a (cycle), plus c -> d. Must terminate.
	f := &mockFetcher{graph: map[string][]string{
		"a": {"b"},
		"b": {"c"},
		"c": {"a", "d"},
		"d": {"b"}, // back-edge into the cycle
	}}
	want := []string{"a", "b", "c", "d"}

	for _, w := range workerCounts {
		got := Crawl("a", f, w)
		if dup, ok := hasDuplicates(got); ok {
			t.Fatalf("workers=%d: URL %q visited more than once", w, dup)
		}
		assertSet(t, got, want)
	}
}

func TestCrawlSelfLoop(t *testing.T) {
	f := &mockFetcher{graph: map[string][]string{
		"a": {"a", "b"}, // self-loop must not re-fetch a
		"b": {"b"},
	}}
	for _, w := range workerCounts {
		got := Crawl("a", f, w)
		assertSet(t, got, []string{"a", "b"})
	}
}

func TestCrawlErrorNodeSkipped(t *testing.T) {
	// b errors: it is still "visited" (a fetch was attempted) but yields no links.
	// Its would-be children (reachable only through b) are therefore NOT visited.
	f := &mockFetcher{
		graph: map[string][]string{
			"a": {"b", "c"},
			"b": {"x", "y"}, // never returned because b errors
			"c": {},
		},
		errs: map[string]bool{"b": true},
	}
	want := []string{"a", "b", "c"} // x, y unreachable because b failed
	for _, w := range workerCounts {
		got := Crawl("a", f, w)
		if dup, ok := hasDuplicates(got); ok {
			t.Fatalf("workers=%d: duplicate %q", w, dup)
		}
		assertSet(t, got, want)
	}
}

func TestCrawlUnreachableNotVisited(t *testing.T) {
	f := &mockFetcher{graph: map[string][]string{
		"a":       {"b"},
		"b":       {},
		"island1": {"island2"}, // not reachable from a
		"island2": {},
	}}
	for _, w := range workerCounts {
		got := Crawl("a", f, w)
		assertSet(t, got, []string{"a", "b"})
	}
}

func TestCrawlSeedOnly(t *testing.T) {
	f := &mockFetcher{graph: map[string][]string{"only": {}}}
	got := Crawl("only", f, 4)
	assertSet(t, got, []string{"only"})
}

func TestCrawlSeedIsErrorNode(t *testing.T) {
	// Even if the seed errors, it counts as visited and the crawl terminates.
	f := &mockFetcher{
		graph: map[string][]string{},
		errs:  map[string]bool{"seed": true},
	}
	got := Crawl("seed", f, 4)
	assertSet(t, got, []string{"seed"})
}

// TestCrawlWideGraph stresses termination and the visited set under real concurrency.
// Run the suite with -race to catch unsynchronized access to the visited set.
func TestCrawlWideGraph(t *testing.T) {
	const n = 500
	graph := make(map[string][]string, n)
	want := make([]string, 0, n)
	id := func(i int) string { return "u" + itoa(i) }
	for i := 0; i < n; i++ {
		want = append(want, id(i))
		// Each node links to a few others (with wrap-around → cycles).
		graph[id(i)] = []string{id((i + 1) % n), id((i + 7) % n), id((i + 13) % n)}
	}
	f := &mockFetcher{graph: graph}

	for _, w := range []int{1, 8, 64} {
		got := Crawl(id(0), f, w)
		if dup, ok := hasDuplicates(got); ok {
			t.Fatalf("workers=%d: duplicate %q", w, dup)
		}
		assertSet(t, got, want)
	}
}

// itoa avoids importing strconv in the test just for this.
func itoa(i int) string {
	if i == 0 {
		return "0"
	}
	var b []byte
	for i > 0 {
		b = append([]byte{byte('0' + i%10)}, b...)
		i /= 10
	}
	return string(b)
}
