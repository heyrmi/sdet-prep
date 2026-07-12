# 4.12 — Design a Proximity Service (Yelp / "Nearby")

> **Module 4 · Case Studies** · ~35 min read + coding assignment
> *Concepts exercised:* geospatial indexing, geohash, quadtree, Google S2, the
> "range scan vs spatial index" trade-off, read-heavy caching, the haversine formula.

---

## The problem

A **proximity service** answers one question very fast: *"What businesses (or drivers, or
friends) are near me right now?"* You open Yelp, it knows your location, and it returns the
20 closest coffee shops within 2 km — ranked, in well under a second.

Concretely you're given a **center point** (your latitude/longitude) and a **radius** (or a
result count), and you must return the matching places, usually ranked by distance, rating,
or some blend.

Why it's harder than it looks: the world has *hundreds of millions* of places, queries come
from *everywhere at once*, and "near" is a **2-dimensional** question. Databases are brilliant
at sorting by *one* dimension. "Near me" is fundamentally two dimensions at once, and that's
the whole difficulty.

> **Analogy.** Imagine finding every friend within a 5-minute walk in a giant city. The naive
> way: phone *every* person in the city and ask "how far are you?" — millions of calls. The
> smart way: the city is divided into **neighborhoods**. You only call people in *your*
> neighborhood and the handful touching it. You've turned a city-wide search into a
> few-block search. **That neighborhood grid is a spatial index** — the heart of this lesson.

---

## Step 1: Requirements (always start here)

**Functional**
- **Search nearby:** given `(lat, lng, radius)` return places inside that circle.
- Alternatively given `(lat, lng, k)` return the `k` closest places.
- **Rank** results (distance, rating, popularity, or a mix).
- Places have metadata (name, category, hours) and change *rarely*.

**Non-functional**
- **Low latency** — this is on the interactive path; aim for tens of milliseconds.
- **Very read-heavy** — people *search* constantly; businesses are added/edited rarely. A
  classic ratio is hundreds of reads per write. This shapes everything: we optimize hard for
  reads and can afford to precompute/index aggressively.
- **High availability** over strict consistency — if a brand-new restaurant takes a minute to
  appear in results, nobody is harmed. Stale-by-seconds is fine.
- **Scale** — say 100M places, hundreds of millions of users, peak tens of thousands of
  searches per second.

> **The first trade-off to name out loud:** because this is read-heavy and tolerant of slight
> staleness, we **pre-build an index and cache aggressively**. We would *not* make that choice
> for a write-heavy, must-be-fresh system. Constraints drive the design.

---

## Step 2: Estimation (back-of-envelope)

Rough numbers to size the system (round generously; the goal is the order of magnitude):

- **Places:** 100M. Each place row ~1 KB (id, name, category, lat/lng, rating) → ~100 GB of
  business data. Fits comfortably in a sharded relational DB; the *metadata* is not the hard part.
- **Search QPS:** 100M daily active users × ~5 searches/day ≈ 500M/day ≈ **~6k QPS average**,
  call it **~30k QPS peak**. Read-heavy, so a cache layer absorbs most of it.
- **The geo index** (place id + cell key) is tiny — a few GB — and fits in memory on the
  search nodes. *That's the point of separating the index from the metadata.*
- **Writes** (new/edited places): maybe a few hundred per second globally — negligible
  compared to reads. Index rebuilds can be incremental and lazy.

The takeaway sizing tells you: the metadata store is ordinary; the **spatial index and read
path** are where the design effort goes.

---

## Step 3: High-level design

Two stores, because they have opposite access patterns:

1. **Business/metadata store** — a normal (sharded) SQL DB keyed by `business_id`. Holds the
   slow-changing facts.
2. **Geo index** — maps a *spatial cell* → the list of `business_id`s inside it. Built from the
   lat/lng of each place. This is what makes "near me" fast.

### API

```
GET /v1/search/nearby?lat=37.788&lng=-122.407&radius_km=2&limit=20
  → 200 OK
    { "results": [ { "id": "...", "name": "Blue Bottle", "distance_km": 0.3 }, ... ] }

POST /v1/businesses           # add a place (rare, write path)
PUT  /v1/businesses/{id}      # edit a place (rare); may move it between cells
GET  /v1/businesses/{id}      # fetch full metadata (after the geo step narrows ids)
```

### Data model

```
businesses                       geo_index (one row per (cell, business))
┌────────────┬───────────────┐   ┌──────────────┬──────────────┐
│ business_id│ ... metadata  │   │ cell_key     │ business_id  │
│ name       │ rating        │   │ "9q8yy"      │ "biz_42"     │
│ lat, lng   │ category      │   │ "9q8yy"      │ "biz_77"     │
└────────────┴───────────────┘   │ "9q8yz"      │ "biz_91"     │
                                  └──────────────┴──────────────┘
```

### Request flow

```
          ┌─────────┐   lat/lng/radius   ┌───────────────┐
  client ─┤  API GW ├───────────────────►│ Search service │
          └─────────┘                    └──────┬────────┘
                                                │ 1. compute the candidate cells
                                                │    for (lat,lng,radius)
                                                ▼
                                         ┌───────────────┐
                                         │   Geo index   │ → candidate business_ids
                                         │ (in memory /  │
                                         │   Redis)      │
                                         └──────┬────────┘
                                                │ 2. exact-distance filter (haversine)
                                                │ 3. fetch metadata + rank
                                                ▼
                                         ┌───────────────┐
                                         │ Metadata store│ + cache
                                         └───────────────┘
```

The two-step shape — **index narrows to candidates, then an exact check filters and ranks** —
is the pattern behind almost every spatial system. The index is allowed to be *approximate*
(it returns a slightly-too-big candidate set); the haversine pass makes the answer *exact*.

---

## Step 4: Deep dive — why the naive approach fails

The tempting first idea: store `lat` and `lng` as two columns and query a bounding box.

```sql
SELECT id FROM businesses
WHERE lat BETWEEN 37.77 AND 37.81
  AND lng BETWEEN -122.43 AND -122.39;
```

Why this falls apart at scale:

- A B-tree index on `lat` finds the right *latitude band* fast — but that band wraps the
  **entire planet** east-to-west. The index on `lng` does the same vertically. The database
  must **intersect two huge 1-D ranges**, and the intersection is the only part you wanted.
- You can index `lat` *or* `lng` efficiently, but a standard B-tree can't index *both at once*
  in a way that prunes both dimensions. One dimension gets scanned.
- It returns a **rectangle**, not a circle; you still need a distance filter afterward — fine,
  but the candidate set was already far too large.

The fix is a **spatial index**: a single key that encodes *both* dimensions so that points
close on Earth are close in the index. Then "near me" becomes "look up a few adjacent keys."

### Computing distance: the haversine formula

Earth is a sphere(ish), so straight-line lat/lng math is wrong over distance. The **haversine
formula** gives the great-circle distance between two points:

```
a = sin²(Δlat/2) + cos(lat1)·cos(lat2)·sin²(Δlng/2)
c = 2 · atan2(√a, √(1−a))
distance = R · c          (R ≈ 6371 km)
```

You use it in the *refinement* step: the index hands you candidates, haversine tells you which
truly fall inside the radius. (You implement exactly this in the assignment.)

---

## Step 4b: Deep dive — geospatial indexing options

This is the heart of the topic. Four approaches, all solving "encode 2-D so neighbors cluster."

### 1) Even (fixed) grid

Slice the world into equal squares (e.g. each cell 1 km × 1 km). A place's cell is just
`(floor(lat/size), floor(lng/size))`. To search, look at the center cell plus the ring of
neighbors the radius can reach, then haversine-filter.

```
        col-1     col      col+1
       ┌───────┬───────┬───────┐
 row+1 │       │   ▲   │       │   search a center cell + its 8 neighbors,
       ├───────┼───────┼───────┤   then exact-filter the candidates
 row   │       │ ● you │       │
       ├───────┼───────┼───────┤
 row-1 │       │       │       │
       └───────┴───────┴───────┘
```

- **Pro:** dead simple, O(1) cell math, easy to reason about. (This is what you'll build.)
- **Con:** **uneven density.** A cell in downtown Manhattan holds thousands of places; a cell
  in the desert holds zero. Fixed cells waste memory on empty areas and create hotspots on
  dense ones.

### 2) Geohash

Recursively bisect the world (longitude, then latitude, then longitude…) and record each
"which half?" decision as a bit. Pack the bits into a **base32 string**. The magic: **a shared
prefix means physical nearness** — `9q8yy` and `9q8yz` are adjacent; the longer the shared
prefix, the closer the points.

```
precision 1 → ~5000 km cell      "9"
precision 4 → ~20 km cell        "9q8y"
precision 5 → ~5 km cell         "9q8yy"
precision 6 → ~1 km cell         "9q8yyk"
precision 7 → ~150 m cell        "9q8yyk8"

  San Francisco ≈ 9q8yyk8ytpxr
  a block away  ≈ 9q8yyk8yt... (shares the first ~8 chars)
```

- **Pro:** turns "near" into a cheap **string prefix** query — works in any plain key-value or
  SQL store with a B-tree, no special DB needed. Tunable precision.
- **Con:** the **edge problem** — two points can be meters apart but sit either side of a
  bisection boundary, giving totally different prefixes. You must also query the **8 neighbor
  geohashes**, not just your own. Also still fixed-resolution (same density problem as a grid).

### 3) Quadtree

A *tree*, not a fixed grid. Start with one cell covering the world. Whenever a cell exceeds a
capacity (say 100 places), **split it into 4 quadrants**, recursively. Dense areas (cities) end
up finely subdivided; empty areas (oceans) stay one big cell.

```
          ┌───────────────┐
          │       │       │   dense quadrant splits again:
          │       │  ┌─┬─┐ │   ┌─┬─┐
          ├───────┼──┼─┼─┤─┤   │·│·│   each leaf holds ≤ capacity points
          │       │  └─┴─┘ │   └─┴─┘
          │       │       │
          └───────────────┘
```

- **Pro:** **adapts to density** — no wasted cells, no city hotspots. Great memory behavior.
- **Con:** it's an in-memory tree that must be **built and rebalanced**; updates are trickier
  than a flat grid; splitting/merging needs care under concurrency.

### 4) Google S2

Project the sphere onto a cube, then use a space-filling **Hilbert curve** to map the 2-D
surface to **1-D 64-bit cell ids** with excellent locality (better than geohash at edges).
Cells come in many levels; you cover a region with a handful of variable-size cells.

- **Pro:** strong locality, fewer edge surprises, hierarchical, battle-tested.
- **Con:** **conceptual and implementation complexity** — you'll use a library, not roll it.

### Comparison

| Approach | Adapts to density? | Edge handling | Storage | Complexity | Used by |
|----------|--------------------|---------------|---------|------------|---------|
| Even grid | No (hotspots) | Check 8 neighbors | Wastes empty cells | Trivial | Internal/simple cases |
| Geohash | No | Edge problem; query neighbors | Compact strings, any KV store | Low | Many "nearby" features, older Yelp/Uber |
| Quadtree | **Yes** | Natural (tree bounds) | Efficient | Medium | Yelp's described design |
| Google S2 | Via cell levels | **Best** locality | 64-bit ids | High (use a lib) | Uber, Google Maps |

> **The trade-off in one breath:** grid and geohash are *simple and fixed-resolution* — easy to
> build, but they hotspot in dense areas. Quadtree and S2 *adapt to density* — better behavior
> at scale, at the cost of more complex code. There's no universal winner; pick by how skewed
> your data is and how much engineering you can spend.

---

## Step 4c: Deep dive — the read path & caching

Because the workload is read-heavy and stale-tolerant, lean on caching:

- **Cache the geo index in memory** (or Redis). It's small (ids + cell keys). Each search node
  can hold the whole index for its region — no DB round trip to find candidates.
- **Cache popular result sets.** "Coffee near downtown SF" is queried constantly; cache the
  computed result for a short TTL (seconds to a minute). Slight staleness is acceptable.
- **Cache metadata** for hot businesses behind the index lookup.
- **Shard the index geographically.** Route a query to the node owning that region. This keeps
  each node's working set small and the data local to the users querying it.

> **Trade-off — index granularity.** Fine cells (geohash precision 7) mean tiny candidate sets
> but you must scan *many* neighbor cells for a large radius. Coarse cells mean few cells to
> read but huge candidate lists to haversine-filter. You tune cell size to your typical radius.

---

## In the wild

- **Yelp** has publicly described a **quadtree**-based approach for its nearby search.
- **Uber** built and open-sourced **H3** (a hexagonal hierarchical grid) and earlier used
  Google **S2** for matching riders to drivers.
- **Redis** ships geospatial commands (`GEOADD`, `GEOSEARCH`) backed by geohash-encoded sorted
  sets — a ready-made geohash index.
- **PostGIS** (PostgreSQL) and Elasticsearch offer geo indexes (R-trees / BKD-trees) so you
  often don't hand-roll one.

---

## Interview angle

Open by **naming the read-heavy, stale-tolerant** nature of the workload — it justifies
indexing + caching. Then explain **why naive lat/lng range scans fail** (intersecting two
planet-spanning 1-D ranges; a B-tree prunes one dimension only). That single insight is the
crux of the question. Introduce the **spatial index** and walk **grid → geohash → quadtree →
S2**, framing it as **simple/fixed vs adaptive/complex**, and call out **density skew** as the
deciding factor. Mention the **two-step pattern** (index narrows, haversine refines) and the
**neighbor-cell / edge problem**. Close with caching and geographic sharding.

**Common follow-ups:**
- "Why not just index lat and lng columns?" → a B-tree prunes one dimension; the other is a
  full planet-wide range. You need a single key encoding both.
- "How do you handle wildly uneven density (Manhattan vs the desert)?" → quadtree / S2 cell
  levels that subdivide dense areas; fixed grids hotspot.
- "A point sits right on a cell boundary — how do you not miss it?" → always query neighboring
  cells too, then haversine-filter; the index is a *candidate* generator, not the final answer.
- "How fresh do results need to be?" → seconds-stale is fine, which is *why* you can cache.

---

## Practice → the Go assignment

Now build the core primitives. Go to [`assignment/`](assignment/) and implement, in order:

1. **`Haversine`** — great-circle distance in km between two lat/lng points.
2. **`GeohashEncode`** — standard base32 geohash at a given precision (bit-interleaving).
3. **`SpatialIndex`** — a fixed grid: `Add` buckets a point into a cell; `Nearby` scans the
   center cell *plus neighbor cells* covering the radius, then **haversine-filters** so the
   answer is exact, not merely cell-based.

```bash
cd assignment
go test ./...          # red → implement → green
```

The interface is given; you fill in the `// TODO`s. A reference solution is in
[`solution/`](solution/) — try first, peek after.

**Next case study:** [4.13 — Distributed Message Queue »](../13-message-queue/)
