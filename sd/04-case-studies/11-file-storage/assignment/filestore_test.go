package filestore

import (
	"bytes"
	"sync"
	"testing"
)

// ---------- Chunk ----------

func TestChunkRoundTrip(t *testing.T) {
	data := []byte("abcdefghij") // 10 bytes
	chunks := Chunk(data, 4)     // 4,4,2
	want := [][]byte{[]byte("abcd"), []byte("efgh"), []byte("ij")}
	if len(chunks) != len(want) {
		t.Fatalf("got %d chunks, want %d", len(chunks), len(want))
	}
	for i := range want {
		if !bytes.Equal(chunks[i], want[i]) {
			t.Fatalf("chunk %d = %q, want %q", i, chunks[i], want[i])
		}
	}
	var joined []byte
	for _, c := range chunks {
		joined = append(joined, c...)
	}
	if !bytes.Equal(joined, data) {
		t.Fatalf("concatenated chunks %q != original %q", joined, data)
	}
}

func TestChunkEdgeCases(t *testing.T) {
	if got := Chunk(nil, 4); got != nil {
		t.Fatalf("Chunk(nil) = %v, want nil", got)
	}
	if got := Chunk([]byte("x"), 0); got != nil {
		t.Fatalf("Chunk with size 0 = %v, want nil", got)
	}
	// File shorter than chunkSize: one chunk holding the whole thing.
	c := Chunk([]byte("ab"), 8)
	if len(c) != 1 || !bytes.Equal(c[0], []byte("ab")) {
		t.Fatalf("short file: got %v, want single chunk 'ab'", c)
	}
}

// ---------- PutFile / GetFile round trip ----------

func TestPutGetRoundTrip(t *testing.T) {
	s := NewStore()
	data := []byte("the quick brown fox jumps over the lazy dog")
	s.PutFile("/a.txt", data, 8)

	got, err := s.GetFile("/a.txt")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if !bytes.Equal(got, data) {
		t.Fatalf("GetFile = %q, want %q", got, data)
	}
}

func TestGetFileUnknown(t *testing.T) {
	s := NewStore()
	if _, err := s.GetFile("/missing"); err == nil {
		t.Fatal("expected error for unknown path")
	}
}

func TestEmptyFile(t *testing.T) {
	s := NewStore()
	s.PutFile("/empty", []byte{}, 8)
	got, err := s.GetFile("/empty")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(got) != 0 {
		t.Fatalf("empty file should reconstruct to 0 bytes, got %d", len(got))
	}
	if s.ChunkCount() != 0 {
		t.Fatalf("empty file should store 0 chunks, got %d", s.ChunkCount())
	}
}

func TestFileShorterThanChunkSize(t *testing.T) {
	s := NewStore()
	s.PutFile("/tiny", []byte("hi"), 1024)
	got, err := s.GetFile("/tiny")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if !bytes.Equal(got, []byte("hi")) {
		t.Fatalf("GetFile = %q, want 'hi'", got)
	}
	if s.ChunkCount() != 1 {
		t.Fatalf("expected 1 stored chunk, got %d", s.ChunkCount())
	}
}

// ---------- Dedup ----------

func TestDedupSharedContent(t *testing.T) {
	s := NewStore()
	// Two files built from the SAME chunks (chunkSize divides the data evenly).
	// "AAAABBBB" with chunkSize 4 -> chunks "AAAA","BBBB".
	s.PutFile("/x", []byte("AAAABBBB"), 4)
	s.PutFile("/y", []byte("AAAABBBB"), 4)
	// Both files reference the same 2 unique chunks; nothing stored twice.
	if s.ChunkCount() != 2 {
		t.Fatalf("identical content must dedup to 2 unique chunks, got %d", s.ChunkCount())
	}
	// A third file that shares one chunk ("AAAA") and adds one new ("CCCC").
	s.PutFile("/z", []byte("AAAACCCC"), 4)
	if s.ChunkCount() != 3 {
		t.Fatalf("after adding one new chunk, expected 3 unique, got %d", s.ChunkCount())
	}
	// All three still reconstruct correctly.
	for path, want := range map[string]string{"/x": "AAAABBBB", "/y": "AAAABBBB", "/z": "AAAACCCC"} {
		got, err := s.GetFile(path)
		if err != nil || string(got) != want {
			t.Fatalf("GetFile(%s) = %q, %v; want %q", path, got, err, want)
		}
	}
}

func TestRepeatedChunkWithinFile(t *testing.T) {
	s := NewStore()
	// "XXXXXXXX" with chunkSize 4 -> two identical chunks "XXXX","XXXX".
	s.PutFile("/r", []byte("XXXXXXXX"), 4)
	if s.ChunkCount() != 1 {
		t.Fatalf("repeated identical chunk should be stored once, got %d", s.ChunkCount())
	}
	got, err := s.GetFile("/r")
	if err != nil || string(got) != "XXXXXXXX" {
		t.Fatalf("GetFile = %q, %v; want XXXXXXXX", got, err)
	}
}

// ---------- Delta sync (ChangedChunks) ----------

func TestChangedChunksDetectsModifiedRegion(t *testing.T) {
	s := NewStore()
	// chunkSize 4. "AAAABBBBCCCC" -> [AAAA, BBBB, CCCC]
	s.PutFile("/f", []byte("AAAABBBBCCCC"), 4)

	// Modify only the middle chunk: BBBB -> BBBX.
	newData := []byte("AAAABBBXCCCC")
	changed := s.ChangedChunks("/f", newData, 4)
	if len(changed) != 1 || changed[0] != 1 {
		t.Fatalf("only chunk index 1 changed, got %v", changed)
	}

	// No change at all: empty delta.
	if got := s.ChangedChunks("/f", []byte("AAAABBBBCCCC"), 4); len(got) != 0 {
		t.Fatalf("identical data should yield no changed chunks, got %v", got)
	}
}

func TestChangedChunksAppendedData(t *testing.T) {
	s := NewStore()
	s.PutFile("/f", []byte("AAAABBBB"), 4) // [AAAA, BBBB]
	// Append a new chunk: indices beyond the stored length count as changed.
	changed := s.ChangedChunks("/f", []byte("AAAABBBBCCCC"), 4) // [AAAA, BBBB, CCCC]
	if len(changed) != 1 || changed[0] != 2 {
		t.Fatalf("appended chunk at index 2 should be changed, got %v", changed)
	}
}

func TestChangedChunksUnknownPath(t *testing.T) {
	s := NewStore()
	// Unknown path: every chunk of newData is "changed".
	changed := s.ChangedChunks("/new", []byte("AAAABBBBCCCC"), 4)
	want := []int{0, 1, 2}
	if len(changed) != len(want) {
		t.Fatalf("unknown path: got %v, want %v", changed, want)
	}
	for i := range want {
		if changed[i] != want[i] {
			t.Fatalf("unknown path: got %v, want %v", changed, want)
		}
	}
}

func TestDeltaSyncOnlyUploadsChanged(t *testing.T) {
	s := NewStore()
	s.PutFile("/f", []byte("AAAABBBBCCCC"), 4) // 3 unique chunks
	if s.ChunkCount() != 3 {
		t.Fatalf("expected 3 chunks initially, got %d", s.ChunkCount())
	}
	// Re-put with one chunk changed; only the new chunk content is added to the store.
	s.PutFile("/f", []byte("AAAABBBXCCCC"), 4) // BBBB -> BBBX
	// Now unique chunks: AAAA, BBBB, CCCC, BBBX = 4 (BBBB stays; old versions retained as chunks).
	if s.ChunkCount() != 4 {
		t.Fatalf("delta save should add exactly 1 new chunk (total 4), got %d", s.ChunkCount())
	}
	got, _ := s.GetFile("/f")
	if string(got) != "AAAABBBXCCCC" {
		t.Fatalf("GetFile after re-put = %q, want AAAABBBXCCCC", got)
	}
}

// ---------- Concurrency (run with -race) ----------

func TestConcurrentAccess(t *testing.T) {
	s := NewStore()
	var wg sync.WaitGroup
	for i := 0; i < 100; i++ {
		wg.Add(1)
		go func(n int) {
			defer wg.Done()
			data := bytes.Repeat([]byte{byte('A' + n%5)}, 16)
			s.PutFile("/shared", data, 4)
			_, _ = s.GetFile("/shared")
			_ = s.ChunkCount()
			_ = s.ChangedChunks("/shared", data, 4)
		}(i)
	}
	wg.Wait()
}
