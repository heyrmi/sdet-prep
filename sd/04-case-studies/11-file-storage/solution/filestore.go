// Package filestore is the reference solution for Module 4.11.
// Try the assignment yourself before reading this!
package filestore

import (
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"sync"
)

// Chunk splits data into fixed-size chunks; the last may be shorter.
func Chunk(data []byte, chunkSize int) [][]byte {
	if chunkSize <= 0 || len(data) == 0 {
		return nil
	}
	var out [][]byte
	for i := 0; i < len(data); i += chunkSize {
		end := i + chunkSize
		if end > len(data) {
			end = len(data)
		}
		c := make([]byte, end-i)
		copy(c, data[i:end])
		out = append(out, c)
	}
	return out
}

// Store is a content-addressed chunk store with per-file recipes.
type Store struct {
	mu     sync.RWMutex
	chunks map[string][]byte   // hash -> bytes (deduped)
	files  map[string][]string // path -> ordered hash list
}

// NewStore creates an empty Store.
func NewStore() *Store {
	return &Store{
		chunks: make(map[string][]byte),
		files:  make(map[string][]string),
	}
}

func hashChunk(b []byte) string {
	sum := sha256.Sum256(b)
	return hex.EncodeToString(sum[:])
}

// PutFile chunks data, dedup-stores each chunk by SHA-256, and records the recipe.
func (s *Store) PutFile(path string, data []byte, chunkSize int) {
	s.mu.Lock()
	defer s.mu.Unlock()

	chunks := Chunk(data, chunkSize)
	hashes := make([]string, 0, len(chunks))
	for _, c := range chunks {
		h := hashChunk(c)
		if _, ok := s.chunks[h]; !ok {
			stored := make([]byte, len(c))
			copy(stored, c)
			s.chunks[h] = stored
		}
		hashes = append(hashes, h)
	}
	s.files[path] = hashes
}

// GetFile reconstructs the bytes stored at path.
func (s *Store) GetFile(path string) ([]byte, error) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	hashes, ok := s.files[path]
	if !ok {
		return nil, fmt.Errorf("filestore: unknown path %q", path)
	}
	out := make([]byte, 0)
	for _, h := range hashes {
		out = append(out, s.chunks[h]...)
	}
	return out, nil
}

// ChunkCount reports the number of unique stored chunks.
func (s *Store) ChunkCount() int {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return len(s.chunks)
}

// ChangedChunks returns the indices of newData whose chunk hash differs from the
// stored version at path (delta sync).
func (s *Store) ChangedChunks(path string, newData []byte, chunkSize int) []int {
	s.mu.RLock()
	defer s.mu.RUnlock()

	stored := s.files[path] // nil if unknown — every new chunk then counts as changed
	newChunks := Chunk(newData, chunkSize)

	var changed []int
	for i, c := range newChunks {
		if i >= len(stored) || hashChunk(c) != stored[i] {
			changed = append(changed, i)
		}
	}
	return changed
}
