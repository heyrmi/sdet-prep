# 4.16 — Design Object Storage (S3-like)

> **Module 4 · Case Studies** · ~35 min read + coding assignment
> *Concepts exercised:* buckets & objects, immutability, durability, replication vs
> erasure coding, versioning, eventual vs strong consistency, multipart uploads, metadata
> at scale, the "store-once, address-by-name" model.

---

## The problem

**Object storage** is a service that stores arbitrary blobs of bytes — photos, videos, backups,
log files, ML model weights — and hands each one back later by name. You `PUT` an object into a
**bucket** under a **key**, and later `GET` it. That's the whole contract from the outside. The
interesting part is everything hidden behind it: storing exabytes, never losing a byte, and
serving it cheaply to millions of clients.

This is the model behind **Amazon S3, Google Cloud Storage, and Azure Blob Storage**. It is *not*
a filesystem and *not* a database — it's a third thing, and understanding why is half the lesson.

> **Analogy.** A giant coat-check at an enormous event. You hand over your coat (the object) and
> get a ticket (the key). You don't get to say *which hook* it hangs on, you can't open someone
> else's coat, and you can't edit your coat once it's checked — you can only hand in a new one and
> get a new ticket. The coat-check's whole job is: never lose a coat, and return the exact coat for
> the exact ticket, even with millions of coats. That's object storage. A *filesystem*, by
> contrast, is like your own closet — nested folders, you reorganize freely, you edit garments in
> place. Different tool, different promises.

Why a separate model exists at all:

- **Flat namespace, not a tree.** There are no real directories — just a key like
  `2026/06/photos/cat.jpg`. The slashes are *part of the name*, a convention, not nested folders.
  A flat namespace is far easier to shard across thousands of machines than a deep mutable tree.
- **Objects are immutable.** You don't append to or edit an object in place; you replace it with a
  new version. Immutability makes replication, caching, and consistency dramatically simpler.
- **Massive scale, cheap, durable.** The goal is "store anything, basically never lose it, pay
  pennies." That pushes the design toward commodity disks + clever redundancy.

---

## Step 1: Requirements (always start here)

**Functional**
- **Buckets**: create/list/delete named containers. Bucket names are globally unique.
- **Objects**: `PUT` (upload), `GET` (download), `DELETE`, and `LIST` keys (often by prefix).
- **Versioning**: keep older copies when a key is overwritten; fetch a specific version by ID.
- **Large objects**: support multi-gigabyte uploads via **multipart upload**.
- **Metadata**: content type, size, an **ETag** (a content hash), timestamps, custom tags.

**Non-functional** (this is where object storage earns its reputation)
- **Durability — the headline number: "11 nines" (99.999999999%).** That means if you store 10
  million objects, you'd expect to lose roughly *one* object every 10,000 years. Durability is the
  *whole point*; everything below serves it.
- **Availability**: highly available reads/writes (think "four nines", 99.99%) — but note
  durability ≠ availability. A region outage might make data briefly unreachable (availability hit)
  without losing it (durability intact).
- **Scale**: exabytes of data, trillions of objects, single objects up to terabytes.
- **Throughput over latency**: object storage optimizes for *bandwidth* (stream a big file fast),
  not single-digit-millisecond random reads. It is not a database or a cache.
- **Cost**: must run on cheap commodity disks. Redundancy strategy is a direct cost lever.

---

## Step 2: Estimation (back-of-the-envelope)

Suppose a photo service: **500 M objects, average 1 MB each.**

```
Raw data       = 500e6 × 1 MB        = 500 TB logical
With 3× replication  = 1,500 TB      (200% storage overhead — expensive!)
With erasure coding (say 1.5× )      = 750 TB (only 50% overhead — half the disks)
```

That single line — **1,500 TB vs 750 TB for the same durability** — is why large systems lean on
erasure coding. We'll unpack it in Step 4.

Metadata sizing — small per object, huge in aggregate:

```
~1 KB metadata/object × 500 M objects = ~500 GB of metadata
```

That comfortably fits in a sharded database/KV store, but it tells you metadata is its *own*
scaling problem, separate from the bytes. A single "list a bucket with a billion keys" call is a
metadata query, not a data read.

Traffic: if 5,000 reads/sec average each pull 1 MB, that's **~5 GB/s** of egress — so the read path
must be bandwidth-optimized and CDN-friendly, not chatty.

---

## Step 3: High-level design

Three logical services, kept deliberately separate because they scale and fail differently:

1. **API / front-end service** — terminates HTTP(S), authenticates, validates, and routes. Speaks
   the S3-style REST verbs (`PUT /bucket/key`, `GET /bucket/key`).
2. **Metadata service** — a sharded database mapping `(bucket, key, version)` → where the bytes
   live, plus size, ETag, content type, timestamps. The "card catalog."
3. **Data / storage service** — the actual disks (storage nodes) that hold object bytes, with the
   redundancy scheme (replication or erasure coding) applied here.

### API (S3-style REST)

| Verb & path | Meaning |
|-------------|---------|
| `PUT /{bucket}` | Create a bucket |
| `PUT /{bucket}/{key}` | Upload an object (body = bytes); returns `ETag`, version ID |
| `GET /{bucket}/{key}` | Download the latest version |
| `GET /{bucket}/{key}?versionId=…` | Download a specific version |
| `DELETE /{bucket}/{key}` | Delete (with versioning on, writes a *delete marker*) |
| `GET /{bucket}?prefix=photos/&list-type=2` | List keys under a prefix |
| `POST /{bucket}/{key}?uploads` | Begin a multipart upload |

The **ETag** in the response is typically the content hash (MD5/SHA-256 hex). Clients use it to
verify integrity and to do conditional requests ("only download if changed").

### Data model (metadata table)

```
Object metadata (keyed/sharded by bucket+key):
  bucket        string
  key           string
  version_id    string        ← unique per write; latest one is "current"
  size          int64
  etag          string        ← content hash, stable for identical bytes
  content_type  string
  created_at    timestamp
  is_delete_marker bool        ← a tombstone version (see versioning)
  locations     [..]           ← which storage nodes / which erasure shards
```

### How an object maps to storage

A `PUT` does roughly:

1. API service authenticates, computes/streams the bytes.
2. Compute the **ETag** (content hash).
3. Hand the bytes to the data service, which **replicates or erasure-codes** them across N storage
   nodes (ideally across racks/zones).
4. Once enough copies/shards are durably written, write a **metadata** row mapping
   `(bucket, key, new version_id)` → those locations, and mark it the current version.
5. Return `ETag` + `version_id` to the client.

A `GET` reverses it: metadata lookup → read bytes from storage nodes → stream to client.

```
                       ┌──────────────────────┐
   PUT /b/k  ─────────►│   API / front-end     │  auth, validate, hash (ETag)
   GET /b/k  ◄─────────│   (stateless, scaled)│
                       └─────────┬────────────┘
                                 │
                ┌────────────────┴───────────────────┐
                ▼                                     ▼
      ┌───────────────────┐                ┌─────────────────────┐
      │  Metadata service │                │   Data service       │
      │ (bucket,key,ver)→ │  locations     │  stores object bytes │
      │   locations,etag  │◄──────────────►│  + redundancy scheme │
      │  (sharded DB/KV)  │                └──────────┬──────────┘
      └───────────────────┘                           │
                                          ┌────────────┼────────────┐
                                          ▼            ▼            ▼
                                      [Node A]     [Node B]     [Node C]   ← commodity disks,
                                      replica/      replica/     replica/      spread across
                                      shard 1       shard 2      shard 3       racks/zones
```

The split matters: the API tier is **stateless** (scale horizontally), metadata is a
**consistency** problem (a sharded DB), and data is a **durability + bandwidth** problem (disks +
redundancy). Three different beasts.

---

## Step 4: Deep dives

### 4a. Durability — replication vs erasure coding

This is *the* object-storage trade-off. Both protect against disk/node failure; they cost very
differently.

**Replication**: keep N full copies (commonly 3) on different nodes. Lose one, read another.

**Erasure coding (EC)**: split an object into **k data shards**, compute **m parity shards**
(Reed-Solomon math), and store all `k+m` on different nodes. You can lose **any m** shards and still
reconstruct the object from any `k` of the `k+m`. Example **EC(6,3)**: 6 data + 3 parity = 9 shards;
survive any 3 failures; storage overhead = 9/6 = **1.5×**.

| Property | 3× Replication | Erasure coding (e.g. 6+3) |
|----------|----------------|---------------------------|
| Storage overhead | 200% (3× the data) | ~50% (1.5×) — **much cheaper** |
| Failures tolerated | 2 node losses | any 3 shard losses (tunable) |
| Read path | Read 1 copy — **simple, fast** | Gather k shards, maybe reconstruct — more CPU/IO |
| Write path | Write N copies | Compute parity (CPU), write k+m shards |
| Repair after failure | Copy one whole replica | Read k shards, recompute — heavier |
| Small-object friendliness | Great | Poor (shard/parity overhead dominates tiny objects) |
| Best for | Hot data, small objects, low latency | Cold/large data, cost-sensitive bulk |

> **Trade-off in one sentence.** Replication buys *simplicity and speed* with *disks*; erasure
> coding buys *cheap durability* with *CPU and complexity*. Real systems use **both**: replicate
> hot/small objects, erasure-code cold/large ones, and often migrate objects from replicated →
> erasure-coded as they age (a storage "tier").

Two more durability tools worth naming in an interview:
- **Spread copies/shards across failure domains** (different racks, power, availability zones) so
  one event can't take out enough copies to lose data.
- **Background scrubbing**: continuously re-read stored data, verify checksums, and re-create any
  corrupted/lost shard *before* a second failure can cause loss. This is how 11 nines is actually
  sustained — not by one big write, but by relentless repair.

### 4b. Versioning

With versioning **on**, an overwrite doesn't destroy the old bytes — it creates a *new version* and
marks it current; old versions remain fetchable by `version_id`. A `DELETE` doesn't erase anything
either: it writes a **delete marker** — a special version that becomes "current" and makes plain
`GET`s return *not found*, while older versions are still retrievable by ID (and the delete is
reversible by removing the marker).

| Behavior | Versioning OFF | Versioning ON |
|----------|----------------|---------------|
| Overwrite same key | old bytes gone | old kept as a prior version |
| `DELETE` | object removed | delete marker added; history preserved |
| Recover a mistake | impossible | fetch/restore an old version |
| Storage cost | minimal | grows with history (mitigate via lifecycle rules) |

Versioning is your safety net against accidental overwrite/delete and against ransomware, at the
cost of storing history. **Lifecycle rules** ("expire non-current versions after 30 days") bound
that cost.

### 4c. Consistency

Historically S3 offered *eventual consistency* for some operations — after a write, a read might
briefly see stale data because metadata/replicas hadn't all caught up. As of 2020, **S3 provides
strong read-after-write consistency**: once a `PUT` returns success, every subsequent `GET` sees
the new data.

| Model | What you observe | Cost |
|-------|------------------|------|
| Eventual | A read just after a write may see old data for a short window | Cheaper, simpler, lower write latency |
| Strong read-after-write | After a successful `PUT`, all reads see it | Requires coordination on metadata; slightly costlier |

The key insight: **the consistency story lives in the metadata service**, not the bytes. The bytes
are immutable; the only thing that "changes" is which version a key points to. Make the metadata
pointer update atomic and visible, and you get strong consistency on top of an eventually-replicated
data layer. This is why splitting metadata from data pays off twice.

### 4d. Large objects — multipart upload

Uploading a 5 GB file in one HTTP request is fragile: one dropped connection at 99% and you start
over. **Multipart upload** fixes this:

1. **Initiate** — client asks to start a multipart upload, gets an `uploadId`.
2. **Upload parts** — client splits the file into parts (e.g. 100 MB each) and uploads them
   independently, *in parallel*, each returning an ETag. Failed parts are simply retried.
3. **Complete** — client sends the list of part numbers + ETags; the service stitches them into one
   logical object and computes the final ETag.

```
file ──► [part 1][part 2][part 3]...[part N]   uploaded in parallel, retried individually
                       │
                  complete(uploadId, [etags])
                       ▼
              one logical object, one final ETag
```

Benefits: parallelism (faster), resumability (retry only the failed part), and the ability to
upload a file whose total size you don't know up front (streaming). The trade-off is bookkeeping:
incomplete uploads leave orphaned parts that consume storage until cleaned up — hence "abort
incomplete multipart uploads after 7 days" lifecycle rules.

---

## In the wild

- **Amazon S3** — the canonical object store: buckets, 11-nines durability, versioning,
  multipart upload, strong read-after-write consistency since 2020, and storage *classes*
  (Standard → Infrequent Access → Glacier) that trade retrieval latency/cost for storage cost,
  largely by changing the redundancy and where the bytes physically sit.
- **Google Cloud Storage / Azure Blob Storage** — same object model, comparable durability.
- **MinIO / Ceph (RADOS)** — open-source, S3-compatible; great for seeing erasure coding and
  scrubbing in code you can actually read.
- **Backblaze** publishes real-world drive-failure data and uses Reed-Solomon erasure coding;
  their engineering blog is a goldmine for the durability math.

---

## Interview angle

Lead by **separating the three services** (API / metadata / data) and say *why*: they scale and
fail differently. Establish that **objects are immutable** and the namespace is **flat** — that
single design choice simplifies everything downstream. Then go straight at **durability**: state the
"11 nines" goal and immediately reach for the **replication vs erasure-coding trade-off** with the
storage-overhead numbers (3× vs ~1.5×) — that's the senior signal. Add **scrubbing + spreading
across failure domains** to show you understand durability is *maintained*, not just achieved once.
Close with **versioning** (delete markers), **strong-vs-eventual consistency living in the metadata
layer**, and **multipart upload** for large objects.

**Common follow-ups:**
- "How do you get 11 nines — surely disks fail?" → redundancy (replication/EC) across failure
  domains *plus* continuous background scrubbing/repair before a second failure compounds.
- "Replication is simpler — why bother with erasure coding?" → cost: ~50% overhead vs 200% for the
  same durability; the trade is CPU + slower repair + bad fit for tiny objects.
- "A user overwrote a file by mistake — can they get it back?" → versioning keeps prior versions;
  delete writes a marker, not a wipe.
- "Read right after write returns stale data — why?" → eventual consistency in the metadata/replica
  layer; modern S3 closes this with strong read-after-write by making the metadata pointer update
  atomic.
- "Upload of a 10 GB file keeps failing near the end." → multipart upload: parallel parts, retry
  only the failed part, resumable.

---

## Practice → the Go assignment

Now build a small in-memory object store that captures the *semantics* (not the disks). Go to
[`assignment/`](assignment/) and implement, in order:

1. `CreateBucket` — reject duplicates.
2. `PutObject` — compute a content **ETag** (SHA-256 hex), keep **versions** on overwrite, return
   a deterministic `versionID`.
3. `GetObject` / `GetObjectVersion` — fetch the latest, or a specific version by ID.
4. `ListObjects` — keys under a prefix, **sorted**.
5. `DeleteObject` — and document its behavior (delete marker vs remove) in a test.

```bash
cd assignment
go test ./...          # red → implement → green
```

The interface is given; you fill in the `// TODO`s. The deterministic version counter means tests
don't depend on wall-clock time. A reference solution is in [`solution/`](solution/) — try first,
peek after.

**Next case study:** [4.17 — Payment System »](../17-payment-system/)
