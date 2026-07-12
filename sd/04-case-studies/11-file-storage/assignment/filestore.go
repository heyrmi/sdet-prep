// Package filestore is the Module 4.11 assignment: a content-addressed chunk
// store with deduplication and delta sync — the core of a Drive/Dropbox-style
// file storage & sync service.
//
// Read 04-case-studies/11-file-storage/README.md first.
//
// The design in one sentence: store the BYTES once (chunks keyed by their
// SHA-256 hash, so identical content is stored a single time) and describe each
// FILE as a recipe (an ordered list of chunk hashes).
//
// Fill in every function marked `// TODO`. Run the tests until green:
//
//	go test ./...
//	go test -race ./...   // the store is used concurrently
//
// Standard library only. Use crypto/sha256 for hashing.
package filestore

import (
	"sync"
)

// Chunk splits data into fixed-size chunks of chunkSize bytes; the final chunk
// may be shorter (the leftover). Concatenating the chunks in order must
// reproduce data exactly.
//
// Edge cases:
//   - chunkSize <= 0: return nil.
//   - len(data) == 0: return nil (an empty file has no chunks).
//
// Returned chunks should be independent copies, not aliases of data.
func Chunk(data []byte, chunkSize int) [][]byte {
	// TODO:
	//  1. If chunkSize <= 0 or len(data) == 0, return nil.
	//  2. Walk data in steps of chunkSize, copying each [i:end] slice into a
	//     fresh []byte appended to the result.
	panic("TODO: implement Chunk")
}

// Store is a content-addressed chunk store with per-file recipes (the ordered
// list of chunk hashes that make up each file). Safe for concurrent use.
type Store struct {
	mu     sync.RWMutex
	chunks map[string][]byte   // sha-256 hex hash -> chunk bytes (deduped: one entry per unique content)
	files  map[string][]string // path -> ordered list of chunk hashes
}

// NewStore creates an empty Store.
func NewStore() *Store {
	return &Store{
		chunks: make(map[string][]byte),
		files:  make(map[string][]string),
	}
}

// hashChunk returns the lowercase hex SHA-256 of b. Helper for the methods below.
func hashChunk(b []byte) string {
	// TODO:
	//  Compute sha256.Sum256(b) and return it as a hex string
	//  (e.g. hex.EncodeToString(sum[:])).
	panic("TODO: implement hashChunk")
}

// PutFile chunks data, stores each unique chunk keyed by its SHA-256 hash
// (identical chunks are stored ONCE — dedup), and records the file's ordered
// list of chunk hashes (its recipe). Re-putting an existing path replaces its
// recipe.
func (s *Store) PutFile(path string, data []byte, chunkSize int) {
	// TODO:
	//  1. Lock for writing.
	//  2. Chunk(data, chunkSize).
	//  3. For each chunk: h := hashChunk(chunk); if h not already in s.chunks,
	//     store a copy under s.chunks[h] (dedup: skip if present). Append h to
	//     the file's hash list.
	//  4. s.files[path] = the ordered hash list.
	panic("TODO: implement Store.PutFile")
}

// GetFile reconstructs and returns the exact bytes previously stored at path.
// It returns an error if the path does not exist.
func (s *Store) GetFile(path string) ([]byte, error) {
	// TODO:
	//  1. RLock.
	//  2. Look up the file's hash list; if the path is unknown, return an error.
	//  3. Concatenate s.chunks[h] for each hash h in order; return the result.
	//     (An empty file has an empty, non-nil recipe -> return an empty slice.)
	panic("TODO: implement Store.GetFile")
}

// ChunkCount reports the number of UNIQUE chunks physically stored. With dedup,
// identical content across files counts once.
func (s *Store) ChunkCount() int {
	// TODO: RLock; return len(s.chunks).
	panic("TODO: implement Store.ChunkCount")
}

// ChangedChunks compares newData (re-chunked at chunkSize) against the version
// currently stored at path and returns the indices whose chunk hash differs —
// the delta that a sync would need to upload.
//
// Rules:
//   - Compare index by index over the overlapping range.
//   - Any index present in newData but beyond the stored file's length counts as
//     changed (newly added chunks).
//   - If path is unknown, every chunk of newData is "changed" (indices 0..n-1).
//   - The returned indices are in ascending order.
func (s *Store) ChangedChunks(path string, newData []byte, chunkSize int) []int {
	// TODO:
	//  1. RLock. Look up the stored hash list (may be empty/absent).
	//  2. Chunk(newData, chunkSize) and hash each new chunk.
	//  3. For each new index i: it is changed if i >= len(stored) OR
	//     newHash[i] != stored[i]. Collect changed indices in order.
	panic("TODO: implement Store.ChangedChunks")
}
