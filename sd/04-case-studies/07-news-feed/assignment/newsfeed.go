// Package newsfeed is the Module 4.7 assignment: a fan-out-on-read ("pull") news
// feed. Each author keeps their own timeline; a user's feed is built on demand by
// merging the timelines of everyone they follow, newest-first.
//
// Read 04-case-studies/07-news-feed/README.md first.
//
// Fill in every method marked `// TODO`. Run the tests until green:
//
//	go test ./...
//	go test -race ./...   // if you add concurrency, protect shared state with the mutex
//
// The interesting part is GetFeed: rather than gather every post from every followee
// and sort the lot, do a k-way merge with container/heap over the followees'
// per-author timelines, stopping once you have `limit` posts. Each timeline is kept
// newest-first, so the merge only ever peeks at the head of each.
package newsfeed

import (
	"sync"
)

// Post is one item in a timeline/feed.
type Post struct {
	ID     string // unique id; used as the tie-breaker when timestamps are equal
	Author string // user id of the poster
	Text   string
	Ts     int64 // unix-ish timestamp; larger = newer
}

// Service is an in-memory pull-based feed.
type Service struct {
	mu        sync.Mutex
	following map[string]map[string]bool // user -> set of users they follow
	timelines map[string][]Post          // author -> their posts, kept newest-first
}

func NewService() *Service {
	return &Service{
		following: make(map[string]map[string]bool),
		timelines: make(map[string][]Post),
	}
}

// Follow makes `user` follow `followee`. Idempotent.
func (s *Service) Follow(user, followee string) {
	// TODO: under the mutex, add followee to s.following[user] (create the set if nil).
	panic("TODO: implement Follow")
}

// Unfollow makes `user` stop following `followee`. Idempotent.
func (s *Service) Unfollow(user, followee string) {
	// TODO: under the mutex, delete followee from s.following[user].
	panic("TODO: implement Unfollow")
}

// Post appends p to its author's timeline, keeping the timeline newest-first.
// Posts are assumed to arrive in roughly increasing Ts order, but you should insert
// so the timeline stays sorted newest-first (newest at index 0).
func (s *Service) Post(p Post) {
	// TODO: under the mutex, insert p into s.timelines[p.Author] so the slice stays
	// ordered newest-first by (Ts desc, ID desc). Prepending then it's-already-sorted
	// is fine for the common case; for correctness, insert at the right position.
	panic("TODO: implement Post")
}

// GetFeed returns up to `limit` posts authored by anyone `user` follows, newest-first,
// ordered by Ts descending with ties broken by ID descending.
//
// Implement this with a k-way merge over the followees' timelines using container/heap:
//   - seed a max-heap with the head (index 0) of each non-empty followee timeline,
//   - pop the newest, append it to the result, then push the next post from that same
//     timeline,
//   - stop once you have `limit` posts (or the heap empties).
// This is O(N log k) for N popped posts across k followees — no full sort needed.
func (s *Service) GetFeed(user string, limit int) []Post {
	// TODO: implement the k-way merge described above. Remember to hold s.mu while
	// reading shared maps/slices.
	panic("TODO: implement GetFeed")
}

// less reports whether post a should come AFTER post b in the feed (i.e. a is older).
// Newest-first means "newer" posts sort earlier. A post is newer if its Ts is larger,
// or, on a Ts tie, if its ID is larger (lexicographically).
//
// You'll likely use the inverse of this inside your heap's Less so the heap pops the
// newest post first. Provided as a hint; you may delete or reuse it.
func newer(a, b Post) bool {
	if a.Ts != b.Ts {
		return a.Ts > b.Ts
	}
	return a.ID > b.ID
}
