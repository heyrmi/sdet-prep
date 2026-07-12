// Package crawler is the Module 4.5 assignment: a concurrent BFS web crawler.
//
// Read 04-case-studies/05-web-crawler/README.md first.
//
// Fill in every function marked `// TODO`. Run the tests until green:
//
//	go test ./...
//	go test -race ./...   // the visited set is shared across worker goroutines
//
// The crawler fetches through an injected Fetcher (no real network), so the tests
// are fully deterministic. The crux of this assignment is TERMINATION: shutting the
// worker pool down cleanly when — and only when — the frontier has fully drained
// (no deadlock, no premature exit).
package crawler

import "sync"

// Fetcher fetches one URL and returns the links found on that page.
// A non-nil error means the page could not be fetched; the crawler must skip it
// gracefully (it is NOT fatal).
type Fetcher interface {
	Fetch(url string) (links []string, err error)
}

// Crawl performs a breadth-first crawl starting from seed, using a pool of `workers`
// goroutines that fetch through fetcher. Each reachable URL is fetched at most once.
// It returns the set of visited URLs (those a fetch was attempted for). Order is
// unspecified.
//
// If workers < 1, treat it as 1.
func Crawl(seed string, fetcher Fetcher, workers int) (visited []string) {
	if workers < 1 {
		workers = 1
	}

	// Suggested design (one correct approach):
	//
	//   - A `frontier` channel of URLs for workers to pull from.
	//   - A visited set (map + sync.Mutex, or sync.Map) recording every URL we have
	//     already accepted, so each URL is enqueued/fetched at most once. Mark the
	//     seed as visited BEFORE enqueuing it.
	//   - An "outstanding work" counter (sync.WaitGroup) that you Add(1) for every URL
	//     you put on the frontier and Done() once that URL has been fully processed
	//     (fetched + its new children enqueued). This is what lets you know when the
	//     crawl is finished.
	//   - A separate goroutine that calls wg.Wait() and then closes the frontier; the
	//     worker `for range frontier` loops then exit, and a second WaitGroup lets the
	//     main goroutine wait for the workers to finish.
	//
	// Watch out for deadlock: if you both send to `frontier` from workers AND read from
	// it in those same workers, an unbuffered/full channel can block forever. Enqueue
	// new URLs from a goroutine (or use a buffered channel + helper) so a worker never
	// blocks on a send while holding up the only reader.

	// TODO: implement the concurrent BFS described above.
	//
	//  1. Set up: frontier channel, visited set, an outstanding-work WaitGroup (wg),
	//     and a worker WaitGroup (workerWG).
	//  2. markVisited(url) bool: lock, return false if already present, else add and
	//     return true. Use it to decide whether to enqueue a URL.
	//  3. enqueue(url): if markVisited(url), wg.Add(1) then send url to the frontier
	//     (from a goroutine to avoid blocking). Call enqueue(seed) to start.
	//  4. Start `workers` goroutines. Each ranges over the frontier; for every url it
	//     fetches via fetcher.Fetch, records the url as visited (already done at enqueue
	//     time), enqueues each returned link, then wg.Done() for the url it just handled.
	//     On fetch error: skip the links but STILL wg.Done().
	//  5. Closer goroutine: wg.Wait(); close(frontier).
	//  6. workerWG.Wait(); then collect and return the visited set as a slice.
	panic("TODO: implement Crawl")
}

// visitedSet is an optional helper you may use for the visited set. You are free to
// use sync.Map or your own struct instead — delete this if unused.
type visitedSet struct {
	mu sync.Mutex
	m  map[string]struct{}
}
