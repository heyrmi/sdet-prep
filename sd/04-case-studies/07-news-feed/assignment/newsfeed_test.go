package newsfeed

import (
	"fmt"
	"sync"
	"testing"
)

// ids extracts the post IDs from a feed, in order, for easy assertions.
func ids(feed []Post) []string {
	out := make([]string, len(feed))
	for i, p := range feed {
		out[i] = p.ID
	}
	return out
}

func eq(a, b []string) bool {
	if len(a) != len(b) {
		return false
	}
	for i := range a {
		if a[i] != b[i] {
			return false
		}
	}
	return true
}

func TestFeedOnlyFollowees(t *testing.T) {
	s := NewService()
	s.Follow("alice", "bob")
	// alice does NOT follow carol
	s.Post(Post{ID: "p1", Author: "bob", Text: "hi", Ts: 10})
	s.Post(Post{ID: "p2", Author: "carol", Text: "yo", Ts: 20})

	got := ids(s.GetFeed("alice", 10))
	if !eq(got, []string{"p1"}) {
		t.Fatalf("feed must contain only followees' posts; want [p1], got %v", got)
	}
}

func TestFeedNewestFirst(t *testing.T) {
	s := NewService()
	s.Follow("alice", "bob")
	s.Follow("alice", "carol")
	s.Post(Post{ID: "p1", Author: "bob", Ts: 10})
	s.Post(Post{ID: "p2", Author: "carol", Ts: 30})
	s.Post(Post{ID: "p3", Author: "bob", Ts: 20})

	got := ids(s.GetFeed("alice", 10))
	want := []string{"p2", "p3", "p1"} // Ts 30, 20, 10
	if !eq(got, want) {
		t.Fatalf("feed must be newest-first; want %v, got %v", want, got)
	}
}

func TestFeedRespectsLimit(t *testing.T) {
	s := NewService()
	s.Follow("alice", "bob")
	for i := 0; i < 10; i++ {
		s.Post(Post{ID: fmt.Sprintf("p%d", i), Author: "bob", Ts: int64(i)})
	}
	got := s.GetFeed("alice", 3)
	if len(got) != 3 {
		t.Fatalf("limit 3 must return 3 posts, got %d", len(got))
	}
	want := []string{"p9", "p8", "p7"} // the 3 newest
	if !eq(ids(got), want) {
		t.Fatalf("limit must keep the newest; want %v, got %v", want, ids(got))
	}
}

func TestUnfollowRemovesPosts(t *testing.T) {
	s := NewService()
	s.Follow("alice", "bob")
	s.Post(Post{ID: "p1", Author: "bob", Ts: 10})
	if got := ids(s.GetFeed("alice", 10)); !eq(got, []string{"p1"}) {
		t.Fatalf("before unfollow want [p1], got %v", got)
	}
	s.Unfollow("alice", "bob")
	if got := s.GetFeed("alice", 10); len(got) != 0 {
		t.Fatalf("after unfollow feed must be empty, got %v", ids(got))
	}
}

func TestEmptyFeed(t *testing.T) {
	s := NewService()
	if got := s.GetFeed("nobody", 10); len(got) != 0 {
		t.Fatalf("a user following no one has an empty feed, got %v", ids(got))
	}
	s.Follow("alice", "bob") // bob has never posted
	if got := s.GetFeed("alice", 10); len(got) != 0 {
		t.Fatalf("following a silent author yields an empty feed, got %v", ids(got))
	}
}

func TestTieBreakDeterministic(t *testing.T) {
	s := NewService()
	s.Follow("alice", "bob")
	s.Follow("alice", "carol")
	// Same Ts across authors: tie-break by ID descending.
	s.Post(Post{ID: "a", Author: "bob", Ts: 100})
	s.Post(Post{ID: "c", Author: "carol", Ts: 100})
	s.Post(Post{ID: "b", Author: "bob", Ts: 100})

	got := ids(s.GetFeed("alice", 10))
	want := []string{"c", "b", "a"} // equal Ts => ID desc
	if !eq(got, want) {
		t.Fatalf("ties must break by ID desc deterministically; want %v, got %v", want, got)
	}
}

func TestManyFolloweesMerge(t *testing.T) {
	s := NewService()
	// alice follows 50 authors, each posting 20 times with interleaved timestamps.
	const authors, perAuthor = 50, 20
	for a := 0; a < authors; a++ {
		author := fmt.Sprintf("u%d", a)
		s.Follow("alice", author)
		for p := 0; p < perAuthor; p++ {
			ts := int64(p*authors + a) // unique, interleaved across authors
			s.Post(Post{ID: fmt.Sprintf("%d-%d", a, p), Author: author, Ts: ts})
		}
	}

	limit := 100
	feed := s.GetFeed("alice", limit)
	if len(feed) != limit {
		t.Fatalf("expected %d posts, got %d", limit, len(feed))
	}
	// Verify strictly newest-first ordering.
	for i := 1; i < len(feed); i++ {
		prev, cur := feed[i-1], feed[i]
		if cur.Ts > prev.Ts || (cur.Ts == prev.Ts && cur.ID > prev.ID) {
			t.Fatalf("merge out of order at %d: %v then %v", i, prev, cur)
		}
	}
	// The newest possible Ts is (perAuthor-1)*authors + (authors-1).
	maxTs := int64((perAuthor-1)*authors + (authors - 1))
	if feed[0].Ts != maxTs {
		t.Fatalf("first post should be the global newest (Ts=%d), got Ts=%d", maxTs, feed[0].Ts)
	}
}

func TestConcurrentPostAndRead(t *testing.T) {
	// Run with -race: concurrent Post/Follow/GetFeed must be mutex-protected.
	s := NewService()
	s.Follow("alice", "bob")

	var wg sync.WaitGroup
	for i := 0; i < 200; i++ {
		wg.Add(1)
		go func(i int) {
			defer wg.Done()
			s.Post(Post{ID: fmt.Sprintf("p%d", i), Author: "bob", Ts: int64(i)})
		}(i)
	}
	for i := 0; i < 50; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			_ = s.GetFeed("alice", 10)
		}()
	}
	wg.Wait()

	feed := s.GetFeed("alice", 5)
	if len(feed) != 5 {
		t.Fatalf("after 200 posts, top-5 feed should have 5 posts, got %d", len(feed))
	}
}
