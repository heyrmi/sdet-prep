// Package vod is the reference solution for Module 4.10.
// Try the assignment yourself before reading this!
package vod

import (
	"fmt"
	"sort"
	"sync"
)

// Segment splits data into segLen-byte segments; the last may be shorter.
func Segment(data []byte, segLen int) [][]byte {
	if segLen <= 0 || len(data) == 0 {
		return nil
	}
	var out [][]byte
	for i := 0; i < len(data); i += segLen {
		end := i + segLen
		if end > len(data) {
			end = len(data)
		}
		seg := make([]byte, end-i)
		copy(seg, data[i:end])
		out = append(out, seg)
	}
	return out
}

// SegmentInfo describes one segment in a manifest.
type SegmentInfo struct {
	Index int
	Size  int
}

// RenditionManifest is one quality level in a video's manifest.
type RenditionManifest struct {
	Name        string
	BitrateKbps int
	Segments    []SegmentInfo
}

// Manifest is the playlist: all renditions sorted ascending by bitrate.
type Manifest struct {
	VideoID    string
	Renditions []RenditionManifest
}

type rendition struct {
	name        string
	bitrateKbps int
	segments    [][]byte
}

// Library stores renditions per video. Safe for concurrent use.
type Library struct {
	mu     sync.RWMutex
	videos map[string][]*rendition
}

// NewLibrary creates an empty Library.
func NewLibrary() *Library {
	return &Library{videos: make(map[string][]*rendition)}
}

// AddRendition stores (or replaces) a rendition's segments for a video.
func (l *Library) AddRendition(videoID, name string, bitrateKbps int, data []byte, segLen int) {
	l.mu.Lock()
	defer l.mu.Unlock()

	r := &rendition{name: name, bitrateKbps: bitrateKbps, segments: Segment(data, segLen)}
	rends := l.videos[videoID]
	for i, existing := range rends {
		if existing.name == name {
			rends[i] = r
			l.videos[videoID] = rends
			return
		}
	}
	l.videos[videoID] = append(rends, r)
}

// BuildManifest returns the manifest for videoID, renditions sorted by bitrate.
func (l *Library) BuildManifest(videoID string) Manifest {
	l.mu.RLock()
	defer l.mu.RUnlock()

	rends := make([]*rendition, len(l.videos[videoID]))
	copy(rends, l.videos[videoID])
	sort.Slice(rends, func(i, j int) bool {
		return rends[i].bitrateKbps < rends[j].bitrateKbps
	})

	m := Manifest{VideoID: videoID, Renditions: make([]RenditionManifest, 0, len(rends))}
	for _, r := range rends {
		segs := make([]SegmentInfo, len(r.segments))
		for i, s := range r.segments {
			segs[i] = SegmentInfo{Index: i, Size: len(s)}
		}
		m.Renditions = append(m.Renditions, RenditionManifest{
			Name:        r.name,
			BitrateKbps: r.bitrateKbps,
			Segments:    segs,
		})
	}
	return m
}

// GetSegment returns the bytes of segment idx of a rendition.
func (l *Library) GetSegment(videoID, rendName string, idx int) ([]byte, error) {
	l.mu.RLock()
	defer l.mu.RUnlock()

	rends, ok := l.videos[videoID]
	if !ok {
		return nil, fmt.Errorf("vod: unknown video %q", videoID)
	}
	for _, r := range rends {
		if r.name == rendName {
			if idx < 0 || idx >= len(r.segments) {
				return nil, fmt.Errorf("vod: segment index %d out of range [0,%d)", idx, len(r.segments))
			}
			return r.segments[idx], nil
		}
	}
	return nil, fmt.Errorf("vod: unknown rendition %q for video %q", rendName, videoID)
}

// SelectRendition picks the highest rendition whose bitrate <= bandwidthKbps,
// falling back to the lowest if none fit.
func (l *Library) SelectRendition(videoID string, bandwidthKbps int) string {
	l.mu.RLock()
	defer l.mu.RUnlock()

	rends := l.videos[videoID]
	if len(rends) == 0 {
		return ""
	}

	var best, lowest *rendition
	for _, r := range rends {
		if lowest == nil || r.bitrateKbps < lowest.bitrateKbps {
			lowest = r
		}
		if r.bitrateKbps <= bandwidthKbps {
			if best == nil || r.bitrateKbps > best.bitrateKbps {
				best = r
			}
		}
	}
	if best != nil {
		return best.name
	}
	return lowest.name
}
