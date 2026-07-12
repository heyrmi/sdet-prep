// Package objstore is the Module 4.16 assignment: an in-memory, S3-like object store.
//
// Read 04-case-studies/16-object-storage/README.md first.
//
// You implement the *semantics* of object storage — buckets, immutable objects,
// content ETags, versioning, prefix listing, and delete markers — not the disks.
//
// Fill in every method marked `// TODO`. Run the tests until green:
//
//	go test ./...
//
// Notes:
//   - The ETag is the content hash (SHA-256 hex of the bytes). Identical bytes must
//     always produce the same ETag.
//   - Versioning is always ON here: overwriting a key keeps the prior version, and
//     each write gets a new versionID. The versionID is a deterministic counter
//     (v1, v2, ...) so tests need no wall-clock time.
//   - DeleteObject writes a *delete marker*: a tombstone version that becomes the
//     current version. A plain GetObject then reports the object as absent (ok=false),
//     but older versions remain retrievable via GetObjectVersion.
package objstore

import (
	"crypto/sha256"
	"encoding/hex"
	"errors"
)

var (
	// ErrBucketExists is returned by CreateBucket when the bucket already exists.
	ErrBucketExists = errors.New("objstore: bucket already exists")
	// ErrNoBucket is returned when an operation targets a bucket that does not exist.
	ErrNoBucket = errors.New("objstore: bucket does not exist")
)

// version is one immutable stored revision of a key.
type version struct {
	versionID    string
	data         []byte
	etag         string
	isDeleteMark bool // true => this version is a tombstone (delete marker)
}

// Store is an in-memory object store. The zero value is not ready; use NewStore.
type Store struct {
	// buckets maps bucketName -> (key -> ordered list of versions, oldest first).
	buckets map[string]map[string][]version
	// seq backs the deterministic versionID counter (v1, v2, ...).
	seq int
}

// NewStore returns an empty object store.
func NewStore() *Store {
	return &Store{buckets: make(map[string]map[string][]version)}
}

// etagOf returns the SHA-256 hex digest of data — the content ETag.
func etagOf(data []byte) string {
	sum := sha256.Sum256(data)
	return hex.EncodeToString(sum[:])
}

// CreateBucket creates a new, empty bucket. It returns ErrBucketExists if a bucket
// with the same name already exists.
func (s *Store) CreateBucket(name string) error {
	// TODO:
	//  1. If s.buckets[name] already exists, return ErrBucketExists.
	//  2. Otherwise create an empty key->[]version map for it.
	panic("TODO: implement Store.CreateBucket")
}

// PutObject stores data under bucket/key as a new version and returns the new
// versionID and the content ETag. The bucket must exist (else ErrNoBucket).
// A defensive copy of data should be stored so later mutation of the caller's slice
// cannot corrupt stored bytes.
func (s *Store) PutObject(bucket, key string, data []byte) (versionID string, etag string, err error) {
	// TODO:
	//  1. Look up the bucket; if missing, return "", "", ErrNoBucket.
	//  2. Increment s.seq and form versionID := fmt.Sprintf("v%d", s.seq).
	//  3. etag := etagOf(data). Copy data into a fresh slice.
	//  4. Append a non-delete-marker version to the key's version list.
	//  5. Return versionID, etag, nil.
	panic("TODO: implement Store.PutObject")
}

// GetObject returns the latest (current) version's data and ETag. ok is false if the
// bucket/key does not exist, or if the current version is a delete marker.
func (s *Store) GetObject(bucket, key string) (data []byte, etag string, ok bool) {
	// TODO:
	//  1. Look up bucket then key's version list; if absent/empty, return nil, "", false.
	//  2. Take the last version (the current one).
	//  3. If it is a delete marker, return nil, "", false.
	//  4. Otherwise return a copy of its data, its etag, true.
	panic("TODO: implement Store.GetObject")
}

// GetObjectVersion returns the data for a specific versionID under bucket/key.
// ok is false if no such version exists or that version is a delete marker.
func (s *Store) GetObjectVersion(bucket, key, versionID string) (data []byte, ok bool) {
	// TODO:
	//  1. Look up the version list for bucket/key.
	//  2. Find the version whose versionID matches; skip delete markers.
	//  3. Return a copy of its data and true, else nil, false.
	panic("TODO: implement Store.GetObjectVersion")
}

// ListObjects returns the keys in bucket whose names start with prefix, sorted
// ascending. A key is included only if its current version is NOT a delete marker
// (i.e. the object currently "exists"). A nil/empty slice is returned for an unknown
// bucket or no matches.
func (s *Store) ListObjects(bucket, prefix string) []string {
	// TODO:
	//  1. Look up the bucket; if missing, return nil.
	//  2. Collect keys where the current version is not a delete marker AND the key
	//     has the given prefix.
	//  3. Sort the result ascending and return it.
	panic("TODO: implement Store.ListObjects")
}

// DeleteObject performs a versioned delete: it appends a delete marker as a new
// version, making the object appear absent to GetObject/ListObjects while keeping
// prior versions retrievable via GetObjectVersion. It returns the new delete marker's
// versionID. The bucket must exist (else ErrNoBucket). Deleting a key that was never
// written still records a delete marker and returns its versionID.
func (s *Store) DeleteObject(bucket, key string) (versionID string, err error) {
	// TODO:
	//  1. Look up the bucket; if missing, return "", ErrNoBucket.
	//  2. Increment s.seq and form versionID.
	//  3. Append a version with isDeleteMark = true (nil data) to the key's list.
	//  4. Return versionID, nil.
	panic("TODO: implement Store.DeleteObject")
}
