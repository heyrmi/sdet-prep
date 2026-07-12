package geo

import (
	"math"
	"sort"
	"strings"
	"testing"
)

func sortedCopy(s []string) []string {
	out := append([]string(nil), s...)
	sort.Strings(out)
	return out
}

func equalSets(a, b []string) bool {
	if len(a) != len(b) {
		return false
	}
	a, b = sortedCopy(a), sortedCopy(b)
	for i := range a {
		if a[i] != b[i] {
			return false
		}
	}
	return true
}

// ---------------- Haversine ----------------

func TestHaversineNYCtoLA(t *testing.T) {
	// New York (JFK area) to Los Angeles (LAX area).
	nycLat, nycLng := 40.7128, -74.0060
	laLat, laLng := 34.0522, -118.2437

	got := Haversine(nycLat, nycLng, laLat, laLng)
	const want = 3935.0 // km, well-known great-circle distance
	if math.Abs(got-want) > 25 {
		t.Fatalf("NYC->LA distance: got %.1f km, want ~%.0f km", got, want)
	}
}

func TestHaversineZero(t *testing.T) {
	if d := Haversine(51.5074, -0.1278, 51.5074, -0.1278); d > 1e-6 {
		t.Fatalf("distance from a point to itself should be 0, got %v", d)
	}
}

func TestHaversineSymmetric(t *testing.T) {
	a := Haversine(48.8566, 2.3522, 52.5200, 13.4050) // Paris -> Berlin
	b := Haversine(52.5200, 13.4050, 48.8566, 2.3522) // Berlin -> Paris
	if math.Abs(a-b) > 1e-6 {
		t.Fatalf("haversine must be symmetric: %v vs %v", a, b)
	}
}

// ---------------- Geohash ----------------

func TestGeohashKnownPrefix(t *testing.T) {
	// San Francisco (37.7749, -122.4194) geohashes to "9q8yyk8ytpxr".
	got := GeohashEncode(37.7749, -122.4194, 6)
	const wantPrefix = "9q8yy"
	if !strings.HasPrefix(got, wantPrefix) {
		t.Fatalf("geohash of SF: got %q, want prefix %q", got, wantPrefix)
	}
}

func TestGeohashLength(t *testing.T) {
	for _, p := range []int{1, 5, 8, 12} {
		if g := GeohashEncode(37.7749, -122.4194, p); len(g) != p {
			t.Fatalf("precision %d: got length %d (%q)", p, len(g), g)
		}
	}
}

func TestGeohashNearbyPointsSharePrefix(t *testing.T) {
	// Two points ~200 m apart in San Francisco should share a long prefix.
	a := GeohashEncode(37.7749, -122.4194, 7)
	b := GeohashEncode(37.7760, -122.4180, 7)
	shared := 0
	for shared < len(a) && shared < len(b) && a[shared] == b[shared] {
		shared++
	}
	if shared < 5 {
		t.Fatalf("nearby points should share >=5 geohash chars, got %d (%q vs %q)", shared, a, b)
	}
}

func TestGeohashFarPointsDifferentPrefix(t *testing.T) {
	sf := GeohashEncode(37.7749, -122.4194, 5)
	ny := GeohashEncode(40.7128, -74.0060, 5)
	if sf[0] == ny[0] {
		t.Fatalf("SF (%q) and NY (%q) are far apart and should differ in the first char", sf, ny)
	}
}

// ---------------- SpatialIndex ----------------

func newSFIndex() *SpatialIndex {
	si := NewSpatialIndex(0.1) // ~11 km cells
	// All in/around San Francisco. Distances from the query point below.
	si.Add("ferry-building", 37.7955, -122.3937)
	si.Add("union-square", 37.7880, -122.4075)
	si.Add("golden-gate-park", 37.7694, -122.4862)
	si.Add("oakland", 37.8044, -122.2712)    // ~13 km east
	si.Add("san-jose", 37.3382, -121.8863)   // ~67 km south-east
	si.Add("los-angeles", 34.0522, -118.2437) // ~560 km away
	return si
}

func TestNearbyReturnsInsideRadius(t *testing.T) {
	si := newSFIndex()
	// Query from downtown SF, 8 km radius. Ferry Building (~1.5 km),
	// Union Square (0 km) and Golden Gate Park (~7.2 km) are inside.
	got := si.Nearby(37.7880, -122.4075, 8)
	want := []string{"ferry-building", "union-square", "golden-gate-park"}
	if !equalSets(got, want) {
		t.Fatalf("8km query: got %v, want %v", sortedCopy(got), sortedCopy(want))
	}
}

func TestNearbyExcludesOutsideRadius(t *testing.T) {
	si := newSFIndex()
	got := si.Nearby(37.7880, -122.4075, 8)
	for _, id := range got {
		if id == "oakland" || id == "san-jose" || id == "los-angeles" {
			t.Fatalf("8km query should not include %q, got %v", id, got)
		}
	}
}

func TestNearbyWiderRadiusIncludesMore(t *testing.T) {
	si := newSFIndex()
	// 20 km should pick up Oakland but still exclude San Jose and LA.
	got := si.Nearby(37.7880, -122.4075, 20)
	if !contains(got, "oakland") {
		t.Fatalf("20km query should include oakland, got %v", got)
	}
	if contains(got, "san-jose") || contains(got, "los-angeles") {
		t.Fatalf("20km query should still exclude san-jose and los-angeles, got %v", got)
	}
}

func TestNearbyBoundaryHandled(t *testing.T) {
	si := NewSpatialIndex(0.1)
	// Place a point exactly ~5 km north of the query. One degree latitude is
	// ~111 km, so 5 km is ~0.045045 degrees.
	const km = 5.0
	const degPerKm = 1.0 / 111.0
	qLat, qLng := 37.0, -122.0
	si.Add("on-boundary", qLat+km*degPerKm, qLng)
	si.Add("just-inside", qLat+(km-0.5)*degPerKm, qLng)
	si.Add("just-outside", qLat+(km+0.5)*degPerKm, qLng)

	got := si.Nearby(qLat, qLng, km)
	if !contains(got, "just-inside") {
		t.Fatalf("point just inside the radius must be returned, got %v", got)
	}
	if contains(got, "just-outside") {
		t.Fatalf("point just outside the radius must be excluded, got %v", got)
	}
}

func TestNearbyEmptyIndex(t *testing.T) {
	si := NewSpatialIndex(0.1)
	if got := si.Nearby(37.0, -122.0, 50); len(got) != 0 {
		t.Fatalf("empty index should return no results, got %v", got)
	}
}

func TestNearbyCrossesCellBoundary(t *testing.T) {
	// A point and query sitting in different grid cells but physically close
	// must still be found (this is why we scan neighbor cells).
	si := NewSpatialIndex(0.1)
	// 0.0999 and 0.1001 fall on opposite sides of a cell edge at 0.1.
	si.Add("a", 37.0999, -122.0)
	got := si.Nearby(37.1001, -122.0, 5)
	if !contains(got, "a") {
		t.Fatalf("point across a cell boundary but within radius must be found, got %v", got)
	}
}

func contains(s []string, v string) bool {
	for _, x := range s {
		if x == v {
			return true
		}
	}
	return false
}
