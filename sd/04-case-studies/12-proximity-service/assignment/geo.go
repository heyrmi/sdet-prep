// Package geo is the Module 4.12 assignment (Proximity Service):
// implement great-circle distance, geohash encoding, and a grid-based
// spatial index for "find businesses near me" queries.
//
// Read 04-case-studies/12-proximity-service/README.md first.
//
// Fill in every function marked `// TODO`. Run the tests until green:
//
//	go test ./...
//
// Standard library only.
package geo

import (
	"math"
)

// ---------------- Haversine ----------------

const earthRadiusKm = 6371.0

// Haversine returns the great-circle distance in kilometers between two
// (lat, lng) points given in decimal degrees.
func Haversine(lat1, lng1, lat2, lng2 float64) float64 {
	// TODO:
	//  1. Convert lat1, lat2 and the lat/lng deltas from degrees to radians
	//     (radians = degrees * pi / 180).
	//  2. a = sin(dlat/2)^2 + cos(rlat1)*cos(rlat2)*sin(dlng/2)^2
	//  3. c = 2 * atan2(sqrt(a), sqrt(1-a))
	//  4. return earthRadiusKm * c
	panic("TODO: implement Haversine")
}

// ---------------- Geohash ----------------

const base32 = "0123456789bcdefghjkmnpqrstuvwxyz"

// GeohashEncode encodes (lat, lng) into a standard base32 geohash of the given
// precision (number of characters). Longitude and latitude bits are
// interleaved, longitude first, packed 5 bits per base32 symbol.
func GeohashEncode(lat, lng float64, precision int) string {
	// TODO:
	//  Maintain a bounding box: lng in [-180,180], lat in [-90,90].
	//  Repeatedly bisect — alternating longitude, latitude, longitude, ...:
	//    - find the midpoint of the active range
	//    - if the coordinate >= midpoint: emit bit 1, move the low edge up
	//      else:                          emit bit 0, move the high edge down
	//  Every 5 bits, append base32[accumulated 5-bit value] and reset.
	//  Stop once you have `precision` characters.
	panic("TODO: implement GeohashEncode")
}

// ---------------- Grid spatial index ----------------

// cellKey identifies a fixed-size grid cell.
type cellKey struct {
	row int // latitude band
	col int // longitude band
}

type point struct {
	id  string
	lat float64
	lng float64
}

// SpatialIndex buckets points into a degree-based grid for fast candidate
// lookup, then refines with an exact Haversine distance check.
type SpatialIndex struct {
	cellSizeDeg float64 // grid cell edge length in degrees
	cells       map[cellKey][]point
}

// NewSpatialIndex creates an index with the given cell size in degrees.
func NewSpatialIndex(cellSizeDeg float64) *SpatialIndex {
	return &SpatialIndex{
		cellSizeDeg: cellSizeDeg,
		cells:       make(map[cellKey][]point),
	}
}

func (si *SpatialIndex) cellOf(lat, lng float64) cellKey {
	return cellKey{
		row: int(math.Floor(lat / si.cellSizeDeg)),
		col: int(math.Floor(lng / si.cellSizeDeg)),
	}
}

// Add inserts a point into the index.
func (si *SpatialIndex) Add(id string, lat, lng float64) {
	// TODO: compute the cell with si.cellOf and append a point{} to si.cells[k].
	panic("TODO: implement SpatialIndex.Add")
}

// Nearby returns the ids of all points within radiusKm of (lat, lng).
//
// The grid only narrows the search; it does not give the answer by itself.
// Scan the target cell PLUS enough neighbor cells to cover the radius, then
// filter each candidate with an exact Haversine check so the result is correct
// regardless of how points fall across cell edges.
func (si *SpatialIndex) Nearby(lat, lng, radiusKm float64) []string {
	// TODO:
	//  1. center := si.cellOf(lat, lng).
	//  2. Convert radiusKm to a cell span. One degree of latitude is ~111 km,
	//     so radiusDeg = radiusKm/111; span = ceil(radiusDeg/cellSizeDeg)+1
	//     (the +1 guards against points sitting near a cell edge).
	//  3. For dr,dc in [-span,span], look at cell {center.row+dr, center.col+dc};
	//     for each stored point, keep its id if Haversine(...) <= radiusKm.
	//  4. Return the collected ids (an empty/nil slice if none match).
	panic("TODO: implement SpatialIndex.Nearby")
}
