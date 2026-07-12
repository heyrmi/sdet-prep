package vod

import (
	"bytes"
	"sync"
	"testing"
)

// ---------- Segment ----------

func TestSegmentRoundTrip(t *testing.T) {
	data := []byte("abcdefghij") // 10 bytes
	segs := Segment(data, 3)
	// Expect segments of 3,3,3,1.
	want := [][]byte{[]byte("abc"), []byte("def"), []byte("ghi"), []byte("j")}
	if len(segs) != len(want) {
		t.Fatalf("got %d segments, want %d", len(segs), len(want))
	}
	for i := range want {
		if !bytes.Equal(segs[i], want[i]) {
			t.Fatalf("segment %d = %q, want %q", i, segs[i], want[i])
		}
	}
	// Concatenation must reproduce the original exactly.
	var joined []byte
	for _, s := range segs {
		joined = append(joined, s...)
	}
	if !bytes.Equal(joined, data) {
		t.Fatalf("concatenated segments %q != original %q", joined, data)
	}
}

func TestSegmentExactMultiple(t *testing.T) {
	data := []byte("abcdef") // 6 bytes, segLen 3 -> 2 full segments, no short tail
	segs := Segment(data, 3)
	if len(segs) != 2 {
		t.Fatalf("got %d segments, want 2", len(segs))
	}
	if len(segs[1]) != 3 {
		t.Fatalf("last segment len = %d, want 3 (exact multiple)", len(segs[1]))
	}
}

func TestSegmentEdgeCases(t *testing.T) {
	if got := Segment(nil, 4); got != nil {
		t.Fatalf("Segment(nil) = %v, want nil", got)
	}
	if got := Segment([]byte("x"), 0); got != nil {
		t.Fatalf("Segment with segLen 0 = %v, want nil", got)
	}
	if got := Segment([]byte("x"), -1); got != nil {
		t.Fatalf("Segment with negative segLen = %v, want nil", got)
	}
}

func TestSegmentDoesNotAlias(t *testing.T) {
	data := []byte("abcdef")
	segs := Segment(data, 3)
	// Mutate the original; stored segments must not change.
	data[0] = 'Z'
	if segs[0][0] != 'a' {
		t.Fatal("segment aliases caller's slice; expected an independent copy")
	}
}

// ---------- Library: manifest ----------

func TestBuildManifestSortedByBitrate(t *testing.T) {
	lib := NewLibrary()
	// Add out of order on purpose.
	lib.AddRendition("v1", "720p", 2500, bytes.Repeat([]byte("a"), 25), 10) // 3 segs (10,10,5)
	lib.AddRendition("v1", "240p", 300, bytes.Repeat([]byte("b"), 10), 10)  // 1 seg
	lib.AddRendition("v1", "1080p", 4500, bytes.Repeat([]byte("c"), 20), 10) // 2 segs

	m := lib.BuildManifest("v1")
	if m.VideoID != "v1" {
		t.Fatalf("VideoID = %q, want v1", m.VideoID)
	}
	wantOrder := []string{"240p", "720p", "1080p"}
	if len(m.Renditions) != len(wantOrder) {
		t.Fatalf("got %d renditions, want %d", len(m.Renditions), len(wantOrder))
	}
	for i, name := range wantOrder {
		if m.Renditions[i].Name != name {
			t.Fatalf("rendition %d = %q, want %q (must be sorted by bitrate)", i, m.Renditions[i].Name, name)
		}
	}
	// Segment counts and sizes for 720p (25 bytes, segLen 10 -> 10,10,5).
	var r720 RenditionManifest
	for _, r := range m.Renditions {
		if r.Name == "720p" {
			r720 = r
		}
	}
	if len(r720.Segments) != 3 {
		t.Fatalf("720p segment count = %d, want 3", len(r720.Segments))
	}
	wantSizes := []int{10, 10, 5}
	for i, si := range r720.Segments {
		if si.Index != i {
			t.Fatalf("segment %d Index = %d, want %d", i, si.Index, i)
		}
		if si.Size != wantSizes[i] {
			t.Fatalf("segment %d Size = %d, want %d", i, si.Size, wantSizes[i])
		}
	}
}

func TestBuildManifestEmptyVideo(t *testing.T) {
	lib := NewLibrary()
	m := lib.BuildManifest("nope")
	if m.VideoID != "nope" {
		t.Fatalf("VideoID = %q, want nope", m.VideoID)
	}
	if len(m.Renditions) != 0 {
		t.Fatalf("got %d renditions for unknown video, want 0", len(m.Renditions))
	}
}

func TestAddRenditionReplaces(t *testing.T) {
	lib := NewLibrary()
	lib.AddRendition("v1", "360p", 700, bytes.Repeat([]byte("a"), 30), 10) // 3 segs
	lib.AddRendition("v1", "360p", 700, bytes.Repeat([]byte("b"), 10), 10) // replace: 1 seg
	m := lib.BuildManifest("v1")
	if len(m.Renditions) != 1 {
		t.Fatalf("got %d renditions, want 1 (replace, not duplicate)", len(m.Renditions))
	}
	if len(m.Renditions[0].Segments) != 1 {
		t.Fatalf("after replace, segment count = %d, want 1", len(m.Renditions[0].Segments))
	}
}

// ---------- Library: GetSegment ----------

func TestGetSegment(t *testing.T) {
	lib := NewLibrary()
	lib.AddRendition("v1", "480p", 1200, []byte("HELLOWORLD"), 4) // segs: HELL, OWOR, LD

	got, err := lib.GetSegment("v1", "480p", 1)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if !bytes.Equal(got, []byte("OWOR")) {
		t.Fatalf("segment 1 = %q, want OWOR", got)
	}
	got2, err := lib.GetSegment("v1", "480p", 2)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if !bytes.Equal(got2, []byte("LD")) {
		t.Fatalf("segment 2 = %q, want LD", got2)
	}
}

func TestGetSegmentErrors(t *testing.T) {
	lib := NewLibrary()
	lib.AddRendition("v1", "480p", 1200, []byte("HELLOWORLD"), 4) // 3 segments (0..2)

	if _, err := lib.GetSegment("v1", "480p", 3); err == nil {
		t.Fatal("expected error for out-of-range index 3")
	}
	if _, err := lib.GetSegment("v1", "480p", -1); err == nil {
		t.Fatal("expected error for negative index")
	}
	if _, err := lib.GetSegment("v1", "999p", 0); err == nil {
		t.Fatal("expected error for unknown rendition")
	}
	if _, err := lib.GetSegment("nope", "480p", 0); err == nil {
		t.Fatal("expected error for unknown video")
	}
}

// ---------- Library: SelectRendition (the adaptive choice) ----------

func TestSelectRendition(t *testing.T) {
	lib := NewLibrary()
	one := []byte("x")
	lib.AddRendition("v1", "240p", 300, one, 1)
	lib.AddRendition("v1", "480p", 1200, one, 1)
	lib.AddRendition("v1", "720p", 2500, one, 1)
	lib.AddRendition("v1", "1080p", 4500, one, 1)

	cases := []struct {
		bandwidth int
		want      string
	}{
		{5000, "1080p"}, // plenty: highest
		{4500, "1080p"}, // exactly 1080p bitrate
		{4499, "720p"},  // just under 1080p
		{2500, "720p"},  // exactly 720p
		{2000, "480p"},  // between 480p and 720p -> highest that fits
		{1200, "480p"},  // exactly 480p
		{300, "240p"},   // exactly lowest
		{100, "240p"},   // below lowest -> fall back to lowest
		{0, "240p"},     // zero bandwidth -> lowest
	}
	for _, c := range cases {
		if got := lib.SelectRendition("v1", c.bandwidth); got != c.want {
			t.Fatalf("SelectRendition(bw=%d) = %q, want %q", c.bandwidth, got, c.want)
		}
	}
}

func TestSelectRenditionUnknownVideo(t *testing.T) {
	lib := NewLibrary()
	if got := lib.SelectRendition("nope", 1000); got != "" {
		t.Fatalf("SelectRendition for unknown video = %q, want empty string", got)
	}
}

// ---------- Concurrency (run with -race) ----------

func TestConcurrentReads(t *testing.T) {
	lib := NewLibrary()
	lib.AddRendition("v1", "240p", 300, bytes.Repeat([]byte("a"), 100), 10)
	lib.AddRendition("v1", "720p", 2500, bytes.Repeat([]byte("b"), 100), 10)

	var wg sync.WaitGroup
	for i := 0; i < 200; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			_ = lib.BuildManifest("v1")
			_ = lib.SelectRendition("v1", 1000)
			_, _ = lib.GetSegment("v1", "720p", 0)
		}()
	}
	wg.Wait()
}
