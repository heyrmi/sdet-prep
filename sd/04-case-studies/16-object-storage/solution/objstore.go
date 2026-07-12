// Package objstore is the reference solution for Module 4.16.
// Try the assignment yourself before reading this!
package objstore

import (
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"sort"
	"strings"
)

var (
	ErrBucketExists = errors.New("objstore: bucket already exists")
	ErrNoBucket     = errors.New("objstore: bucket does not exist")
)

type version struct {
	versionID    string
	data         []byte
	etag         string
	isDeleteMark bool
}

type Store struct {
	buckets map[string]map[string][]version
	seq     int
}

func NewStore() *Store {
	return &Store{buckets: make(map[string]map[string][]version)}
}

func etagOf(data []byte) string {
	sum := sha256.Sum256(data)
	return hex.EncodeToString(sum[:])
}

func (s *Store) nextVersionID() string {
	s.seq++
	return fmt.Sprintf("v%d", s.seq)
}

func (s *Store) CreateBucket(name string) error {
	if _, ok := s.buckets[name]; ok {
		return ErrBucketExists
	}
	s.buckets[name] = make(map[string][]version)
	return nil
}

func (s *Store) PutObject(bucket, key string, data []byte) (versionID string, etag string, err error) {
	b, ok := s.buckets[bucket]
	if !ok {
		return "", "", ErrNoBucket
	}
	versionID = s.nextVersionID()
	etag = etagOf(data)
	cp := append([]byte(nil), data...)
	b[key] = append(b[key], version{versionID: versionID, data: cp, etag: etag})
	return versionID, etag, nil
}

func (s *Store) GetObject(bucket, key string) (data []byte, etag string, ok bool) {
	b, ok := s.buckets[bucket]
	if !ok {
		return nil, "", false
	}
	versions := b[key]
	if len(versions) == 0 {
		return nil, "", false
	}
	cur := versions[len(versions)-1]
	if cur.isDeleteMark {
		return nil, "", false
	}
	return append([]byte(nil), cur.data...), cur.etag, true
}

func (s *Store) GetObjectVersion(bucket, key, versionID string) (data []byte, ok bool) {
	b, ok := s.buckets[bucket]
	if !ok {
		return nil, false
	}
	for _, v := range b[key] {
		if v.versionID == versionID {
			if v.isDeleteMark {
				return nil, false
			}
			return append([]byte(nil), v.data...), true
		}
	}
	return nil, false
}

func (s *Store) ListObjects(bucket, prefix string) []string {
	b, ok := s.buckets[bucket]
	if !ok {
		return nil
	}
	var keys []string
	for key, versions := range b {
		if len(versions) == 0 {
			continue
		}
		if versions[len(versions)-1].isDeleteMark {
			continue // currently deleted
		}
		if strings.HasPrefix(key, prefix) {
			keys = append(keys, key)
		}
	}
	sort.Strings(keys)
	return keys
}

func (s *Store) DeleteObject(bucket, key string) (versionID string, err error) {
	b, ok := s.buckets[bucket]
	if !ok {
		return "", ErrNoBucket
	}
	versionID = s.nextVersionID()
	b[key] = append(b[key], version{versionID: versionID, isDeleteMark: true})
	return versionID, nil
}
