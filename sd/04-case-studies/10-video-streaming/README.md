# 4.10 — Design a Video Streaming Service (YouTube / Netflix)

> **Module 4 · Case Studies** · ~35 min read + coding assignment
> *Concepts exercised:* upload + transcode pipeline, work queues + workers, blob storage,
> adaptive bitrate streaming (HLS/DASH), segmentation + manifests, CDN delivery, metadata DB,
> the "store once, serve many ways" trade-off.

---

## The problem

A **video streaming service** lets one person **upload** a video and millions of people **watch**
it — on a phone over flaky 3G, on a laptop over fiber, on a TV across the world — each getting
the best quality their connection can handle, starting almost instantly, without buffering.

That sentence hides an enormous amount of engineering. The raw file a creator uploads is *not*
the file you watch. Between "upload finished" and "play" there is a whole pipeline.

Why this is hard:
- **Files are huge.** A 10-minute 4K clip can be several gigabytes. Multiply by billions of
  videos and you are storing exabytes.
- **Watchers are everywhere and unequal.** One viewer has 50 Mbps, another has 1.5 Mbps on a
  train. The same file can't serve both well.
- **Watching is bursty and global.** A viral video gets hammered from every continent at once.
- **It must feel instant.** Nobody waits 30 seconds for a video to "download." Playback starts
  after a second or two and keeps going.

> **Analogy.** Think of a restaurant kitchen, not a vending machine. A vending machine hands you
> exactly the item you put money in for. A great kitchen takes one raw ingredient (the uploaded
> master file) and **prepares many dishes from it** — a small plate for a kid, a full portion for
> an adult, a gluten-free version — and **plates each dish in bite-sized courses** so you can
> start eating immediately instead of waiting for the whole meal. Uploading is "dropping off
> ingredients"; transcoding is "the prep kitchen"; adaptive streaming is "serving the right-sized
> dish in courses"; the CDN is "a chain of local kitchens near every customer so the food is hot."

---

## Step 1: Requirements (always start here)

**Functional**
- **Upload** a video (potentially huge — resumable uploads matter).
- **Transcode** the master into multiple resolutions/bitrates and into a streamable format.
- **Stream** to many device types; playback starts fast and adapts to bandwidth.
- **Browse / search** videos by metadata (title, channel, views).
- (Stretch) likes, comments, recommendations — *out of scope*; we focus on the storage +
  delivery core.

**Non-functional**
- **Scale** — billions of videos, hundreds of millions of concurrent viewers.
- **Global low latency** — fast start anywhere in the world.
- **High availability for playback** — reads (watching) vastly outnumber writes (uploads).
- **Durable storage** — never lose an uploaded master.
- **Cost-aware** — storage and especially **egress bandwidth** dominate the bill. Every design
  choice is really a cost choice in disguise.

> **The defining ratio:** this is an extremely **read-heavy** system. One upload may be watched
> millions of times. Spend effort making *reads* (playback) cheap and fast; you can afford to
> make *writes* (upload + transcode) slow and heavy because they happen once.

---

## Step 2: Estimation (back-of-the-envelope)

Let's sanity-check the scale so the design choices have numbers behind them.

Assume:
- **500 hours of video uploaded per minute** (YouTube's real ballpark).
- Average upload master ≈ **1 GB per 10 minutes** of video (varies wildly with resolution).

**Upload ingest volume**

```
500 hours/min = 500 * 60 = 30,000 minutes of video per minute
30,000 min ÷ 10 min/GB = 3,000 GB ≈ 3 TB of master uploaded per minute
3 TB/min * 60 * 24 ≈ 4.3 PB of new masters per day
```

That's *before* transcoding. Each master explodes into ~5 renditions plus segmentation overhead,
so stored bytes are several times the master. **Storage is the obvious cost center.**

**Read (watch) bandwidth — the bigger bill**

Say 1 billion watch-hours/day at an average 3 Mbps stream:

```
3 Mbps = 0.375 MB/s
1 hour of watching = 0.375 * 3600 ≈ 1.35 GB delivered per watch-hour
1e9 watch-hours/day * 1.35 GB ≈ 1.35 EB/day of egress
```

Exabytes per day leaving your network. **This is why CDNs exist** — you cannot serve that from a
handful of data centers, and you cannot afford to serve it from origin every time.

**Takeaways the design must respect:**
1. Storage is huge but cheap-ish → **blob storage**, not a database, for the bytes.
2. Egress is gigantic and expensive → **CDN caching at the edge** is mandatory, not optional.
3. Transcoding is CPU-heavy and bursty → **async queue + worker fleet**, never inline with upload.

---

## Step 3: High-level design

### The shape of it

Two flows that barely touch: the **write path** (upload → transcode → store) and the
**read path** (request manifest → fetch segments via CDN). Metadata ties them together.

```
        WRITE PATH (happens once per video, slow & heavy)
  ┌────────┐  upload   ┌──────────────┐  raw master   ┌───────────────┐
  │ Creator│ ────────► │ Upload Service│ ────────────► │  Blob Storage │
  └────────┘ (resumable)└──────┬───────┘   (origin)    │   (masters)   │
                               │ enqueue job           └───────┬───────┘
                               ▼                                │ read master
                     ┌───────────────────┐                     ▼
                     │   Job Queue        │           ┌──────────────────────┐
                     │ (transcode tasks)  │ ─────────►│  Transcode Workers    │
                     └───────────────────┘  pull job  │ (CPU fleet, autoscale)│
                                                       └──────────┬───────────┘
                                          write renditions+segments│
                                                                   ▼
   metadata (renditions, segment lists) ──► ┌──────────────┐  ┌───────────────┐
                                            │ Metadata DB  │  │  Blob Storage │
                                            │ (videos,     │  │ (segments +   │
                                            │  manifests)  │  │  manifests)   │
                                            └──────┬───────┘  └───────┬───────┘
─────────────────────────────────────────────────┼──────────────────┼─────────
        READ PATH (happens millions of times, fast & cheap)         │
  ┌────────┐  GET manifest  ┌───────────────┐    │     ┌────────────▼────────┐
  │ Viewer │ ──────────────►│  API / Origin │◄───┘     │        CDN          │
  │ player │ ◄──────────────┤   (metadata)  │          │ (edge cache of      │
  └───┬────┘   manifest      └───────────────┘          │  segments, global) │
      │  GET segment_0.ts, segment_1.ts, ...            └────────────┬────────┘
      └─────────────────────────────────────────────────────────────┘
                         player measures bandwidth, picks rendition, repeats
```

### API (sketch)

Uploads use a **resumable** protocol (chunked PUTs against an upload session) so a dropped
connection on a 2 GB file doesn't mean starting over. Playback is plain HTTP GETs — which is
exactly why CDNs can cache it.

```
POST /videos                      → create video, returns {videoID, uploadURL}
PUT  /upload/{session}            → upload bytes (resumable, ranged)
POST /videos/{id}/finalize        → mark upload complete, enqueue transcode

GET  /videos/{id}/manifest        → the playlist: list of renditions + segment URLs
GET  /cdn/{id}/{rendition}/{seg}  → one media segment (served from CDN edge)
GET  /videos/{id}                 → metadata (title, duration, status: processing|ready)
```

### Data model

Keep the **bytes** out of the database. The DB stores only *pointers and metadata*:

| Store | Holds | Why |
|-------|-------|-----|
| **Metadata DB** (SQL or wide-column) | video record, title, status, list of renditions, ordered segment list per rendition | small, queryable, transactional-ish |
| **Blob storage** (S3-like) | the raw master, every transcoded segment, every manifest file | cheap, durable, huge, dumb byte store |
| **CDN** | hot copies of segments + manifests near viewers | absorbs the exabytes of egress |

A `Video` row roughly: `{id, title, durationSec, status, renditions: [{name, bitrateKbps,
segmentCount}], createdAt}`. The actual segment bytes live in blob storage, addressed by a
predictable key like `{videoID}/{rendition}/seg_{index}`.

---

## Step 4: Deep dive — the upload + transcode pipeline

**Never transcode inline with the upload request.** Transcoding a video can take minutes and
pins a CPU. If you did it in the upload handler, the creator's HTTP request would hang and your
upload servers would melt under load. Instead:

1. Upload service streams the master into **blob storage** and writes a `status=processing` row.
2. It drops a **transcode job** onto a **queue** (e.g. one job per rendition, or one fan-out job).
3. A fleet of **stateless workers** pull jobs, read the master, run the encoder (e.g. FFmpeg),
   write the resulting segments + manifest to blob storage, and update the metadata DB.
4. When all renditions finish, flip `status=ready`.

This is the classic **producer → queue → worker** pattern (you met it in Module 2.10). It gives
you three superpowers:

| Property | How the queue gives it |
|----------|------------------------|
| **Elastic scaling** | Workers are stateless; add/remove them based on queue depth. Viral upload day? Scale out. |
| **Retry / fault tolerance** | A worker that crashes mid-job? The job goes back on the queue; another worker retries. |
| **Backpressure** | If uploads outpace transcoding, the queue grows but uploads still succeed; you process when you can. |

> **Trade-off — fan-out granularity.** One big job per video is simple but slow (one worker does
> all 5 renditions serially). Fanning out into one job *per rendition* (or even per segment)
> parallelizes across the fleet for faster "time to ready," at the cost of more coordination
> (you must detect when *all* sub-jobs finish). Most large systems split per rendition.

---

## Step 4 (cont.): Deep dive — adaptive bitrate streaming (the core idea)

Here's the trick that makes a single video play smoothly on every connection.

**1. Encode multiple renditions (the "bitrate ladder").** Transcode the master into several
versions at different resolutions and bitrates:

| Rendition | Resolution | Video bitrate | Good for |
|-----------|-----------|---------------|----------|
| 240p | 426×240 | ~300 kbps | bad mobile / data-saver |
| 360p | 640×360 | ~700 kbps | mobile |
| 480p | 854×480 | ~1,200 kbps | SD / slow Wi-Fi |
| 720p | 1280×720 | ~2,500 kbps | HD, typical broadband |
| 1080p | 1920×1080 | ~4,500 kbps | full HD |
| 4K | 3840×2160 | ~15,000 kbps | fast fiber / TVs |

**2. Cut each rendition into small segments.** Slice every rendition into short chunks —
typically **2–10 seconds** each (e.g. `seg_0`, `seg_1`, …). The same wall-clock moment exists in
every rendition, so the player can switch renditions *between* segments seamlessly.

**3. Write a manifest (a.k.a. playlist).** A small text file that lists the available renditions
and, for each, the ordered list of segment URLs. This is what `HLS` (`.m3u8`) and `DASH` (`.mpd`)
are: standardized manifest formats. The player downloads the manifest first, then the segments.

**4. The player adapts.** This is the magic:

```
player: download manifest → start at a safe low rendition
loop:
   measure how fast the last segment downloaded (estimated bandwidth)
   if bandwidth comfortably exceeds the next-higher rendition → step up
   if a segment arrives late / buffer draining → step down
   fetch next segment of the chosen rendition
```

Bandwidth drops on the train? Next segment comes from 360p instead of 1080p — picture softens but
**never stops**. Back on Wi-Fi? It climbs back to 1080p. The viewer just sees "it adjusts."

| Approach | Pros | Cons |
|----------|------|------|
| **Single file, one quality** | trivial to store/serve | buffers on slow links, wastes bandwidth on fast ones, no fast start |
| **Progressive download (whole file)** | simple HTTP | must download a lot before play; can't adapt; pay to send 4K to a phone |
| **Adaptive bitrate (HLS/DASH)** ⭐ | fast start, smooth, right-sized per viewer, cacheable segments | must store N renditions (more storage + transcode CPU); more moving parts |

> **Trade-off — segment length.** *Short* segments (2s) adapt faster and start quicker, but mean
> more files, more requests, and more manifest/HTTP overhead. *Long* segments (10s) are more
> efficient per byte but react slowly to bandwidth changes. ~4–6s is a common middle ground.

> **Trade-off — more renditions = more storage + CPU.** Each rung on the ladder multiplies
> transcode cost and stored bytes. You pick a ladder that covers your real audience's devices and
> networks, not "every resolution imaginable."

---

## Step 4 (cont.): Deep dive — CDN delivery

Recall the estimate: ~exabytes of egress per day. You **cannot** serve that from origin. The
**CDN** is a global mesh of edge caches near viewers.

- Because segments are **immutable, named files served over plain HTTP GET**, they cache
  beautifully. `seg_42` never changes, so an edge can keep it and serve thousands of viewers
  without ever bothering origin.
- The **first** viewer in a region causes a cache miss (edge fetches from origin); everyone after
  hits the warm edge. For a viral video, the **origin offload** is enormous — origin sees a
  trickle, edges serve the flood.
- Manifests can be cached briefly too, but they change (e.g. for live), so short TTLs.

> **Trade-off — cache freshness vs hit rate.** Long TTLs maximize cache hits (cheap, fast) but
> make changes slow to propagate. For immutable VOD segments this is easy (cache forever); for
> live or frequently updated manifests you trade some hit rate for freshness with short TTLs.

> **Why segments are content-friendly:** immutable + uniquely named = perfect cache keys. This is
> the same reason static assets get hashed filenames. Design for cacheability and the CDN does
> the heavy lifting for free.

---

## In the wild

- **YouTube / Netflix** both encode a bitrate ladder and serve adaptive streams over HTTP from
  CDNs (Netflix runs its own CDN, *Open Connect*, with appliances inside ISPs).
- **HLS** (Apple, `.m3u8` playlists + `.ts`/`fMP4` segments) and **MPEG-DASH** (`.mpd`) are the
  two dominant adaptive-streaming standards. Players: hls.js, ExoPlayer, AVPlayer.
- **FFmpeg** is the workhorse encoder behind most transcode pipelines.
- **Live streaming** reuses the same idea, but the manifest is continuously appended with new
  segments and TTLs are short — VOD is just "the recording is already complete."

---

## Interview angle

Lead with the **two paths**: a heavy, async **write path** (upload → queue → transcode workers →
blob storage) and a light, cacheable **read path** (manifest → segments → CDN). State early that
the system is **read-heavy**, so you optimize playback and let uploads be slow.

The senior signal is **adaptive bitrate streaming**: explain the bitrate ladder, segmentation,
the manifest, and how the *player* (not the server) chooses the rendition by measuring bandwidth.
Then justify the **CDN** with the egress estimate — "we're serving exabytes; origin physically
can't, and immutable named segments cache perfectly." Close with the **transcoding queue** for
elasticity and retries.

**Common follow-ups:**
- "Why not just store one high-quality file and let the client downscale?" → you'd ship 4K bytes
  to a phone on 3G (buffering + wasted egress); adaptive sends the *right-sized* bytes.
- "How does the player decide which quality to use?" → it measures download speed of recent
  segments and watches the buffer; steps up/down between segment boundaries.
- "Transcoding is slow — does the user wait?" → no; it's async via a queue, video shows
  `processing` until renditions are ready.
- "How do you serve a viral video without melting origin?" → CDN edge caching; immutable segments
  give near-100% hit rates after warm-up; origin offload.
- "Why segment at all instead of byte-range requests on one file?" → segments enable per-segment
  rendition switching and clean cache keys, and decouple from a single physical file.

---

## Practice → the Go assignment

Now build the **segmentation + manifest + adaptive-selection** core — the heart of the read path.
Go to [`assignment/`](assignment/) (module `vod`) and implement, in order:

1. `Segment(data, segLen)` — split a rendition's bytes into fixed-size segments (last may be
   shorter). Concatenating them must reproduce the original exactly.
2. `Library.AddRendition(...)` — store a rendition's segments for a video.
3. `Library.BuildManifest(videoID)` — produce a `Manifest` listing renditions **sorted by
   bitrate**, each with its ordered segment list (index + size).
4. `Library.GetSegment(...)` — return a segment's bytes; error on a bad index.
5. `Library.SelectRendition(videoID, bandwidthKbps)` — the adaptive choice: pick the highest
   rendition whose bitrate `<=` bandwidth, or the lowest if none fit.

```bash
cd assignment
go test ./...          # red → implement → green
go test -race ./...    # the library is safe to read concurrently
```

The interface is given; you fill in the `// TODO`s. A reference solution is in
[`solution/`](solution/) — try first, peek after.

**Next case study:** [4.11 — Google Drive / File Storage »](../11-file-storage/)
