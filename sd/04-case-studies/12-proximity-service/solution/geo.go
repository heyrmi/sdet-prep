// Package geo is the reference solution for Module 4.12 (Proximity Service).
// Try the assignment yourself before reading this!
package geo

import (
	"math"
)

// ---------------- Haversine ----------------

const earthRadiusKm = 6371.0

// Haversine returns the great-circle distance in kilometers between two
// (lat, lng) points in decimal degrees.
func Haversine(lat1, lng1, lat2, lng2 float64) float64 {
	rlat1 := lat1 * math.Pi / 180
	rlat2 := lat2 * math.Pi / 180
	dlat := (lat2 - lat1) * math.Pi / 180
	dlng := (lng2 - lng1) * math.Pi / 180

	a := math.Sin(dlat/2)*math.Sin(dlat/2) +
		math.Cos(rlat1)*math.Cos(rlat2)*math.Sin(dlng/2)*math.Sin(dlng/2)
	c := 2 * math.Atan2(math.Sqrt(a), math.Sqrt(1-a))
	return earthRadiusKm * c
}

// ---------------- Geohash ----------------

const base32 = "0123456789bcdefghjkmnpqrstuvwxyz"

// GeohashEncode encodes (lat, lng) into a standard base32 geohash of the given
// precision (number of characters). It interleaves longitude and latitude bits,
// longitude first, into 5-bit base32 symbols.
func GeohashEncode(lat, lng float64, precision int) string {
	latRange := [2]float64{-90, 90}
	lngRange := [2]float64{-180, 180}

	var geohash []byte
	bit := 0
	ch := 0
	even := true // true => bisect longitude, false => bisect latitude

	for len(geohash) < precision {
		if even {
			mid := (lngRange[0] + lngRange[1]) / 2
			if lng >= mid {
				ch |= 1 << (4 - bit)
				lngRange[0] = mid
			} else {
				lngRange[1] = mid
			}
		} else {
			mid := (latRange[0] + latRange[1]) / 2
			if lat >= mid {
				ch |= 1 << (4 - bit)
				latRange[0] = mid
			} else {
				latRange[1] = mid
			}
		}
		even = !even

		if bit < 4 {
			bit++
		} else {
			geohash = append(geohash, base32[ch])
			bit = 0
			ch = 0
		}
	}
	return string(geohash)
}

// ---------------- Grid spatial index ----------------

// cellKey identifies a fixed-size grid cell. The grid is defined in degrees so
// that cellSize roughly matches the search radius; Nearby scans the target cell
// plus its 8 neighbors, then haversine-filters for correctness.
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
// A cell size of ~0.1 degrees is roughly 11 km of latitude per cell.
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
	k := si.cellOf(lat, lng)
	si.cells[k] = append(si.cells[k], point{id: id, lat: lat, lng: lng})
}

// Nearby returns the ids of all points within radiusKm of (lat, lng). It scans
// the target cell plus enough neighboring cells to cover the radius, then
// filters candidates by exact Haversine distance so the result is correct
// regardless of cell alignment.
func (si *SpatialIndex) Nearby(lat, lng, radiusKm float64) []string {
	center := si.cellOf(lat, lng)

	// How many cells (in each direction) the radius can reach. One degree of
	// latitude is ~111 km; use that as a conservative upper bound so we never
	// miss a candidate. span >= 1 always (the 8 immediate neighbors).
	const kmPerDegree = 111.0
	radiusDeg := radiusKm / kmPerDegree
	span := int(math.Ceil(radiusDeg/si.cellSizeDeg)) + 1

	var out []string
	for dr := -span; dr <= span; dr++ {
		for dc := -span; dc <= span; dc++ {
			k := cellKey{row: center.row + dr, col: center.col + dc}
			for _, p := range si.cells[k] {
				if Haversine(lat, lng, p.lat, p.lng) <= radiusKm {
					out = append(out, p.id)
				}
			}
		}
	}
	return out
}
