// Package vod is the Module 4.10 assignment: the segmentation + manifest +
// adaptive-rendition-selection core of a video streaming service (YouTube/Netflix).
//
// Read 04-case-studies/10-video-streaming/README.md first.
//
// You are building the read-path heart of adaptive bitrate streaming:
//   - Segment: cut a rendition's bytes into fixed-size segments.
//   - Library: store renditions per video, build a manifest, fetch segments,
//     and pick the right rendition for a viewer's bandwidth.
//
// Fill in every function marked `// TODO`. Run the tests until green:
//
//	go test ./...
//	go test -race ./...   // the library is read concurrently
//
// Standard library only.
package vod

import (
	"sync"
)

// Segment splits data into consecutive segments of segLen bytes each. The final
// segment may be shorter (the leftover). Concatenating the returned segments in
// order must reproduce data exactly.
//
// Edge cases:
//   - segLen <= 0: return nil (invalid segment length).
//   - len(data) == 0: return nil (nothing to segment).
//
// The returned segments should reference fresh backing memory (copy), not alias
// the caller's slice, so callers can't mutate stored data.
func Segment(data []byte, segLen int) [][]byte {
	// TODO:
	//  1. If segLen <= 0 or len(data) == 0, return nil.
	//  2. Walk data in steps of segLen; for each step, copy data[i:end] into a
	//     new []byte and append it to the result.
	panic("TODO: implement Segment")
}

// SegmentInfo describes one segment in a manifest: its index and byte size.
type SegmentInfo struct {
	Index int
	Size  int
}

// RenditionManifest is one quality level in a video's manifest.
type RenditionManifest struct {
	Name        string
	BitrateKbps int
	Segments    []SegmentInfo // ordered by index
}

// Manifest is the playlist a player downloads first: all renditions for a video,
// sorted ascending by bitrate.
type Manifest struct {
	VideoID    string
	Renditions []RenditionManifest
}

// rendition is the internal stored form of one quality level.
type rendition struct {
	name        string
	bitrateKbps int
	segments    [][]byte
}

// Library stores renditions for many videos and serves the read path. It is safe
// for concurrent use.
type Library struct {
	mu     sync.RWMutex
	videos map[string][]*rendition // videoID -> renditions
}

// NewLibrary creates an empty Library.
func NewLibrary() *Library {
	return &Library{videos: make(map[string][]*rendition)}
}

// AddRendition segments data into segLen-byte segments and stores them under
// (videoID, name) with the given bitrate. Adding a rendition name that already
// exists for the video replaces it.
func (l *Library) AddRendition(videoID, name string, bitrateKbps int, data []byte, segLen int) {
	// TODO:
	//  1. Lock for writing.
	//  2. Segment(data, segLen) into segments.
	//  3. If a rendition with the same name exists for videoID, replace it;
	//     otherwise append a new rendition. Store name, bitrateKbps, segments.
	panic("TODO: implement Library.AddRendition")
}

// BuildManifest returns the manifest for videoID: every rendition sorted ascending
// by bitrate, each with its ordered segment list (index + size). If the video has
// no renditions, Renditions is empty (non-nil) and VideoID is still set.
func (l *Library) BuildManifest(videoID string) Manifest {
	// TODO:
	//  1. RLock.
	//  2. Copy the video's renditions into a slice and sort ascending by bitrate.
	//  3. For each rendition, build []SegmentInfo{Index: i, Size: len(seg)}.
	//  4. Return Manifest{VideoID, Renditions}.
	panic("TODO: implement Library.BuildManifest")
}

// GetSegment returns the bytes of segment idx of the named rendition for videoID.
// It returns an error if the video, rendition, or index does not exist.
func (l *Library) GetSegment(videoID, rendition string, idx int) ([]byte, error) {
	// TODO:
	//  1. RLock.
	//  2. Find the video and the named rendition; if missing, return an error.
	//  3. If idx < 0 or idx >= len(segments), return an error.
	//  4. Return the segment bytes.
	panic("TODO: implement Library.GetSegment")
}

// SelectRendition implements the adaptive choice: return the Name of the highest
// rendition whose bitrate is <= bandwidthKbps. If none fit (bandwidth below the
// lowest), return the lowest rendition's name. If the video has no renditions,
// return "".
func (l *Library) SelectRendition(videoID string, bandwidthKbps int) string {
	// TODO:
	//  1. RLock.
	//  2. If the video has no renditions, return "".
	//  3. Scan renditions: track the highest bitrate that is <= bandwidthKbps,
	//     and also the overall lowest-bitrate rendition.
	//  4. Return the best fit if any; otherwise the lowest.
	panic("TODO: implement Library.SelectRendition")
}
