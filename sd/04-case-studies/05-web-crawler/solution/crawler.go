// Package crawler is the reference solution for Module 4.5.
// Try the assignment yourself before reading this!
package crawler

import "sync"

type Fetcher interface {
	Fetch(url string) (links []string, err error)
}

func Crawl(seed string, fetcher Fetcher, workers int) (visited []string) {
	if workers < 1 {
		workers = 1
	}

	var (
		mu       sync.Mutex                  // guards seen
		seen     = make(map[string]struct{}) // every URL we've accepted (fetched or about to)
		work     sync.WaitGroup              // counts outstanding (queued + in-flight) URLs
		workers2 sync.WaitGroup              // counts live worker goroutines
		// A buffered frontier reduces (but doesn't eliminate) blocking on send; we always
		// enqueue from a goroutine, so a full buffer never deadlocks the workers.
		frontier = make(chan string, 1024)
	)

	// markSeen returns true the first time it sees url, false thereafter.
	markSeen := func(url string) bool {
		mu.Lock()
		defer mu.Unlock()
		if _, ok := seen[url]; ok {
			return false
		}
		seen[url] = struct{}{}
		return true
	}

	// enqueue accepts a URL exactly once and schedules it onto the frontier. The send
	// happens in its own goroutine so a worker never blocks here while it is also the
	// only thing that could drain the channel.
	var enqueue func(url string)
	enqueue = func(url string) {
		if !markSeen(url) {
			return
		}
		work.Add(1)
		go func() { frontier <- url }()
	}

	// Workers: pull a URL, fetch, enqueue its children, then mark the unit of work done.
	for i := 0; i < workers; i++ {
		workers2.Add(1)
		go func() {
			defer workers2.Done()
			for url := range frontier {
				links, err := fetcher.Fetch(url)
				if err == nil {
					for _, link := range links {
						enqueue(link)
					}
				}
				// Whether or not the fetch succeeded, this URL's work is complete.
				work.Done()
			}
		}()
	}

	enqueue(seed)

	// When all outstanding work is done, close the frontier so workers exit their range.
	go func() {
		work.Wait()
		close(frontier)
	}()

	workers2.Wait()

	// Collect the visited set. No other goroutine is running now, but lock anyway.
	mu.Lock()
	defer mu.Unlock()
	visited = make([]string, 0, len(seen))
	for u := range seen {
		visited = append(visited, u)
	}
	return visited
}
