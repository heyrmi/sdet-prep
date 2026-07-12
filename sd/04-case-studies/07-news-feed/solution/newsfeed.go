// Package newsfeed is the reference solution for Module 4.7 (fan-out-on-read feed).
// Try the assignment yourself before reading this!
package newsfeed

import (
	"container/heap"
	"sort"
	"sync"
)

type Post struct {
	ID     string
	Author string
	Text   string
	Ts     int64
}

type Service struct {
	mu        sync.Mutex
	following map[string]map[string]bool
	timelines map[string][]Post
}

func NewService() *Service {
	return &Service{
		following: make(map[string]map[string]bool),
		timelines: make(map[string][]Post),
	}
}

func (s *Service) Follow(user, followee string) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if s.following[user] == nil {
		s.following[user] = make(map[string]bool)
	}
	s.following[user][followee] = true
}

func (s *Service) Unfollow(user, followee string) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if set := s.following[user]; set != nil {
		delete(set, followee)
	}
}

// newer reports whether a is newer than b (Ts desc, ID desc tie-break).
func newer(a, b Post) bool {
	if a.Ts != b.Ts {
		return a.Ts > b.Ts
	}
	return a.ID > b.ID
}

func (s *Service) Post(p Post) {
	s.mu.Lock()
	defer s.mu.Unlock()
	tl := s.timelines[p.Author]
	// Find the insertion index that keeps the timeline newest-first.
	i := sort.Search(len(tl), func(i int) bool { return newer(p, tl[i]) })
	tl = append(tl, Post{})
	copy(tl[i+1:], tl[i:])
	tl[i] = p
	s.timelines[p.Author] = tl
}

// heapItem is a post plus a cursor into its author's timeline, so after popping the
// head we can push the next post from the same timeline.
type heapItem struct {
	post   Post
	source []Post // the timeline this post came from
	idx    int    // index of `post` within source
}

// feedHeap is a max-heap on post recency: Pop yields the newest post.
type feedHeap []heapItem

func (h feedHeap) Len() int            { return len(h) }
func (h feedHeap) Less(i, j int) bool  { return newer(h[i].post, h[j].post) }
func (h feedHeap) Swap(i, j int)       { h[i], h[j] = h[j], h[i] }
func (h *feedHeap) Push(x interface{}) { *h = append(*h, x.(heapItem)) }
func (h *feedHeap) Pop() interface{} {
	old := *h
	n := len(old)
	it := old[n-1]
	*h = old[:n-1]
	return it
}

func (s *Service) GetFeed(user string, limit int) []Post {
	s.mu.Lock()
	defer s.mu.Unlock()

	if limit <= 0 {
		return nil
	}

	h := &feedHeap{}
	heap.Init(h)
	// Seed with the head of each followee timeline.
	for followee := range s.following[user] {
		tl := s.timelines[followee]
		if len(tl) > 0 {
			heap.Push(h, heapItem{post: tl[0], source: tl, idx: 0})
		}
	}

	feed := make([]Post, 0, limit)
	for h.Len() > 0 && len(feed) < limit {
		it := heap.Pop(h).(heapItem)
		feed = append(feed, it.post)
		if it.idx+1 < len(it.source) {
			heap.Push(h, heapItem{post: it.source[it.idx+1], source: it.source, idx: it.idx + 1})
		}
	}
	return feed
}
