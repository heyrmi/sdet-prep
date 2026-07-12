package objstore

import (
	"bytes"
	"reflect"
	"testing"
)

func mustBucket(t *testing.T, s *Store, name string) {
	t.Helper()
	if err := s.CreateBucket(name); err != nil {
		t.Fatalf("CreateBucket(%q): unexpected error %v", name, err)
	}
}

func TestCreateBucketRejectsDuplicate(t *testing.T) {
	s := NewStore()
	mustBucket(t, s, "photos")
	if err := s.CreateBucket("photos"); err != ErrBucketExists {
		t.Fatalf("duplicate bucket: want ErrBucketExists, got %v", err)
	}
	// A different name is fine.
	if err := s.CreateBucket("videos"); err != nil {
		t.Fatalf("distinct bucket should succeed, got %v", err)
	}
}

func TestPutGetRoundTripAndEtagStable(t *testing.T) {
	s := NewStore()
	mustBucket(t, s, "b")

	payload := []byte("hello world")
	_, etag1, err := s.PutObject("b", "greeting", payload)
	if err != nil {
		t.Fatalf("PutObject: %v", err)
	}

	got, etag, ok := s.GetObject("b", "greeting")
	if !ok {
		t.Fatal("GetObject after Put should be ok")
	}
	if !bytes.Equal(got, payload) {
		t.Fatalf("round-trip mismatch: got %q want %q", got, payload)
	}
	if etag != etag1 {
		t.Fatalf("etag from Get %q != etag from Put %q", etag, etag1)
	}

	// Same bytes under a different key => identical ETag (content-addressed).
	_, etag2, _ := s.PutObject("b", "greeting-copy", []byte("hello world"))
	if etag2 != etag1 {
		t.Fatalf("identical bytes must yield identical ETag: %q vs %q", etag2, etag1)
	}

	// Different bytes => different ETag.
	_, etag3, _ := s.PutObject("b", "other", []byte("different"))
	if etag3 == etag1 {
		t.Fatal("different bytes must yield a different ETag")
	}
}

func TestPutObjectUnknownBucket(t *testing.T) {
	s := NewStore()
	if _, _, err := s.PutObject("nope", "k", []byte("x")); err != ErrNoBucket {
		t.Fatalf("Put into missing bucket: want ErrNoBucket, got %v", err)
	}
}

func TestGetMissingReturnsNotOk(t *testing.T) {
	s := NewStore()
	mustBucket(t, s, "b")
	if _, _, ok := s.GetObject("b", "absent"); ok {
		t.Fatal("GetObject on missing key should report ok=false")
	}
	if _, _, ok := s.GetObject("missingBucket", "k"); ok {
		t.Fatal("GetObject in missing bucket should report ok=false")
	}
}

func TestVersioningLatestAndOldRetrievable(t *testing.T) {
	s := NewStore()
	mustBucket(t, s, "b")

	v1, _, _ := s.PutObject("b", "k", []byte("one"))
	v2, _, _ := s.PutObject("b", "k", []byte("two"))
	if v1 == v2 {
		t.Fatalf("each write must get a distinct versionID, both were %q", v1)
	}

	// Latest wins.
	got, _, ok := s.GetObject("b", "k")
	if !ok || !bytes.Equal(got, []byte("two")) {
		t.Fatalf("GetObject should return latest 'two', got %q ok=%v", got, ok)
	}

	// Old version still retrievable by id.
	old, ok := s.GetObjectVersion("b", "k", v1)
	if !ok || !bytes.Equal(old, []byte("one")) {
		t.Fatalf("GetObjectVersion(v1) should return 'one', got %q ok=%v", old, ok)
	}
	cur, ok := s.GetObjectVersion("b", "k", v2)
	if !ok || !bytes.Equal(cur, []byte("two")) {
		t.Fatalf("GetObjectVersion(v2) should return 'two', got %q ok=%v", cur, ok)
	}

	// Unknown version id.
	if _, ok := s.GetObjectVersion("b", "k", "v999"); ok {
		t.Fatal("GetObjectVersion on unknown id should report ok=false")
	}
}

func TestVersionIDIsDeterministicCounter(t *testing.T) {
	s := NewStore()
	mustBucket(t, s, "b")
	v1, _, _ := s.PutObject("b", "a", []byte("1"))
	v2, _, _ := s.PutObject("b", "b", []byte("2"))
	v3, _, _ := s.PutObject("b", "a", []byte("3"))
	if v1 != "v1" || v2 != "v2" || v3 != "v3" {
		t.Fatalf("versionIDs should be a deterministic counter v1,v2,v3; got %q,%q,%q", v1, v2, v3)
	}
}

func TestListObjectsPrefixSorted(t *testing.T) {
	s := NewStore()
	mustBucket(t, s, "b")
	for _, k := range []string{"photos/cat.jpg", "photos/dog.jpg", "videos/a.mp4", "photos/ant.jpg"} {
		s.PutObject("b", k, []byte("x"))
	}

	got := s.ListObjects("b", "photos/")
	want := []string{"photos/ant.jpg", "photos/cat.jpg", "photos/dog.jpg"}
	if !reflect.DeepEqual(got, want) {
		t.Fatalf("ListObjects prefix+sort: got %v want %v", got, want)
	}

	// Empty prefix => all current keys, sorted.
	all := s.ListObjects("b", "")
	wantAll := []string{"photos/ant.jpg", "photos/cat.jpg", "photos/dog.jpg", "videos/a.mp4"}
	if !reflect.DeepEqual(all, wantAll) {
		t.Fatalf("ListObjects empty prefix: got %v want %v", all, wantAll)
	}

	// Unknown bucket => nil/empty.
	if got := s.ListObjects("nope", ""); len(got) != 0 {
		t.Fatalf("ListObjects on missing bucket should be empty, got %v", got)
	}
}

// DeleteObject writes a delete marker: GetObject reports absent, the key drops out of
// ListObjects, but prior versions stay retrievable by id. (Behavior documented in the
// lesson and in objstore.go.)
func TestDeleteObjectWritesMarker(t *testing.T) {
	s := NewStore()
	mustBucket(t, s, "b")
	v1, _, _ := s.PutObject("b", "k", []byte("payload"))

	delVer, err := s.DeleteObject("b", "k")
	if err != nil {
		t.Fatalf("DeleteObject: %v", err)
	}
	if delVer == v1 || delVer == "" {
		t.Fatalf("delete marker should get its own versionID, got %q", delVer)
	}

	// Current object now appears absent.
	if _, _, ok := s.GetObject("b", "k"); ok {
		t.Fatal("after DeleteObject, GetObject should report ok=false (delete marker)")
	}
	// Key disappears from listing.
	if got := s.ListObjects("b", ""); len(got) != 0 {
		t.Fatalf("deleted key should not be listed, got %v", got)
	}
	// Old version is still retrievable by id.
	old, ok := s.GetObjectVersion("b", "k", v1)
	if !ok || !bytes.Equal(old, []byte("payload")) {
		t.Fatalf("prior version must survive delete; got %q ok=%v", old, ok)
	}
	// Re-putting brings the key back.
	s.PutObject("b", "k", []byte("revived"))
	got, _, ok := s.GetObject("b", "k")
	if !ok || !bytes.Equal(got, []byte("revived")) {
		t.Fatalf("Put after delete should revive the key, got %q ok=%v", got, ok)
	}
}

func TestDeleteUnknownBucket(t *testing.T) {
	s := NewStore()
	if _, err := s.DeleteObject("nope", "k"); err != ErrNoBucket {
		t.Fatalf("DeleteObject in missing bucket: want ErrNoBucket, got %v", err)
	}
}

// Stored bytes must be insulated from later mutation of the caller's slice.
func TestPutMakesDefensiveCopy(t *testing.T) {
	s := NewStore()
	mustBucket(t, s, "b")
	buf := []byte("mutable")
	s.PutObject("b", "k", buf)
	buf[0] = 'X' // mutate caller's slice after the put

	got, _, ok := s.GetObject("b", "k")
	if !ok || !bytes.Equal(got, []byte("mutable")) {
		t.Fatalf("stored bytes must not change when caller mutates input; got %q", got)
	}
}
