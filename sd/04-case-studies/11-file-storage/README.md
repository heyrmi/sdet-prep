# 4.11 — Design a File Storage & Sync Service (Google Drive / Dropbox)

> **Module 4 · Case Studies** · ~35 min read + coding assignment
> *Concepts exercised:* chunking, content-addressed storage, SHA-256 dedup, file metadata
> (chunk lists), delta sync, versioning, conflict handling, change notification, the
> "store the bytes once, describe the file as a recipe" trade-off.

---

## The problem

A **file storage & sync service** lets you drop a file in a folder on one device and have it
appear, correct and up to date, on every other device you own and on everyone you've shared it
with. Edit it on your laptop on a plane; the moment you land, the changes flow everywhere.

The naive version — "upload the whole file to a server, download the whole file on every other
device" — collapses quickly:
- You edit one paragraph of a 2 GB video project. Re-uploading 2 GB to change a few kilobytes is
  absurd, especially on a phone tether.
- The same 50 MB attachment lives in a thousand users' folders. Storing it a thousand times is
  pure waste.
- Two devices edit the same file while offline. Who wins?

> **Analogy.** Think of how a recipe book references ingredients, not how a photocopier copies
> pages. A photocopier duplicates the *whole page* every time — that's the naive "re-upload
> everything" approach. A recipe book instead says "this dish uses eggs, flour, sugar." The
> ingredients (chunks of file content) live **once** in the pantry; each recipe (file) is just an
> **ordered list of which ingredients to use**. Two recipes that both call for eggs don't each
> own a separate carton — they reference the same one. To change a dish, you swap *one*
> ingredient, not rewrite the whole recipe.

That "pantry of unique ingredients + recipes that reference them" is the whole design.

---

## Step 1: Requirements (always start here)

**Functional**
- **Upload / download** files of arbitrary size.
- **Sync** changes across a user's devices automatically and efficiently (don't resend unchanged
  data).
- **Share** files/folders with other users.
- **Versioning** — keep history; allow restoring an older version.
- **Conflict handling** — two devices editing offline must not silently lose data.
- **Notify** other devices when something changes so they pull promptly.

**Non-functional**
- **Storage-efficient** — deduplicate identical content; never store the same bytes twice.
- **Bandwidth-efficient** — sync only what changed (**delta sync**), critical on mobile.
- **Durable & available** — files must never be lost; reads should rarely fail.
- **Consistent enough** — eventual consistency across devices is fine; *data loss* is not.

> **The core trade-off this whole system is built around:** spend a little CPU (hashing chunks)
> to save a lot of storage (dedup) and a lot of bandwidth (delta sync). For a service where
> storage and egress are the dominant costs, that's a trade you take every time.

---

## Step 2: Estimation (back-of-the-envelope)

Assume 100M users, average 50 GB stored each, average file 1 MB.

**Raw stored bytes (before dedup)**

```
100e6 users * 50 GB = 5e9 GB = 5 EB of logical data
```

But a huge fraction is *duplicated* across users (the same OS installers, shared docs, popular
media). Real systems report **dedup ratios of 1.5x–3x or more** on mixed corpora.

```
At a modest 2x dedup ratio: physical storage ≈ 2.5 EB (half saved, "for free")
```

**Why delta sync dominates the bandwidth story**

Suppose a user edits a 100 MB file, changing 1 MB of it.

```
Full re-upload:  100 MB over the wire
Delta sync (1 MB chunks): re-upload only the changed chunk(s) ≈ 1–2 MB
                 → ~50–100x less bandwidth for that save
```

Multiply by every save by every user every day and delta sync is the difference between a viable
service and a bandwidth bankruptcy.

**Read/write mix:** sync is chatty but most operations are *metadata* checks ("has anything
changed?"). Make metadata lookups cheap and fast; the bulk byte transfers are the rare, large
events.

---

## Step 3: High-level design

### The shape of it

Separate the **bytes** (content-addressed chunks in blob storage, deduped) from the **file**
(metadata: an ordered list of chunk hashes). A metadata service coordinates; a notification
service tells devices to pull.

```
  ┌──────────┐         changed chunks only        ┌────────────────────┐
  │ Device A │ ─────────────────────────────────► │   Block / Chunk     │
  │ (client) │   1. chunk file locally             │   Store (blob)      │
  │          │   2. hash each chunk (SHA-256)      │  key = SHA-256      │
  │          │   3. ask: which hashes are new?     │  identical chunk    │
  │          │   4. upload only the new ones       │  stored ONCE (dedup)│
  └────┬─────┘                                     └─────────┬───────────┘
       │ commit file = ordered [hashes]                      │
       ▼                                                      │ reconstruct
  ┌──────────────────┐   stores file recipe:                 │ (fetch chunks
  │ Metadata Service │   {path, version, [chunk hashes]}     │  in order)
  │  + Metadata DB   │                                        │
  └────────┬─────────┘                                        ▼
           │ "file X changed → v7"               ┌────────────────────┐
           ▼                                     │ Device B (client)  │
  ┌──────────────────┐    notify                 │  pulls new metadata │
  │  Notification    │ ───────────────────────►  │  diffs vs local,    │
  │    Service       │  (long-poll / push)       │  fetches only the   │
  └──────────────────┘                           │  chunks it lacks    │
                                                  └────────────────────┘
```

### API (sketch)

The split between "do I already have this content?" and "transfer bytes" is what makes dedup and
delta sync possible — the client asks before it uploads.

```
POST /chunks/check     {hashes: [...]}   → which hashes the server is missing
PUT  /chunks/{hash}     <bytes>          → upload one chunk (idempotent: hash is the key)
GET  /chunks/{hash}                      → download one chunk

PUT  /files/{path}     {chunks:[hashes], baseVersion} → commit a file version (recipe)
GET  /files/{path}                       → file metadata (version + chunk list)
GET  /changes?since={cursor}             → list of files changed since a cursor (for sync)
```

### Data model

| Store | Holds | Notes |
|-------|-------|-------|
| **Chunk store** (blob) | the actual chunk bytes, **keyed by content hash** | content-addressed; identical chunk → identical key → stored once |
| **Metadata DB** | per file: `{path, ownerID, version, chunkHashes:[...], size, mtime}`; version history | small rows; the "recipe" + history |
| **Notification index** | per device: a cursor / change feed | drives "pull now" |

A file is **just an ordered list of chunk hashes** plus metadata. The bytes are nowhere in the
DB — they're in the chunk store, addressed by hash. This is **content-addressed storage**: the
*name* of a chunk *is* the hash of its content.

---

## Step 4: Deep dive — chunking + content-addressed dedup

**Chunking** splits a file into pieces (e.g. fixed 4 MB blocks). For each chunk, compute a
cryptographic hash (we'll use **SHA-256**). Use that hash as the chunk's storage key.

Two powerful consequences fall out for free:

1. **Deduplication.** Two chunks with identical bytes produce the identical hash → the same key →
   the second "upload" is a no-op. The same attachment in a thousand folders is stored **once**.
2. **Cheap existence checks.** "Do you have this chunk?" is just "does key `<hash>` exist?" — no
   need to send the bytes to find out.

```
file:  [ chunk0 ][ chunk1 ][ chunk2 ][ chunk3 ]
hash:    a1b2      c3d4      a1b2      e5f6
                              ▲ same content as chunk0 → same hash → stored once
store keys present: {a1b2, c3d4, e5f6}   (3 unique, not 4)
file recipe:        [a1b2, c3d4, a1b2, e5f6]   (ordered list of hashes)
```

| Chunking strategy | Pros | Cons |
|-------------------|------|------|
| **Fixed-size blocks** (what we build) | dead simple; trivial indexing | a single byte *inserted* near the start shifts every later block → all hashes change ("boundary shift" problem) |
| **Content-defined chunking** (rolling hash, e.g. Rabin) | boundaries follow content, so an insert only changes nearby chunks → far better dedup on edits | more complex; variable chunk sizes |

> **Trade-off — chunk size.** *Small* chunks dedup better and make deltas tighter, but blow up
> metadata (more hashes per file) and per-chunk overhead. *Large* chunks mean tiny metadata but
> coarse dedup and bigger deltas. A few MB is a common sweet spot.

> **Why a cryptographic hash?** Collisions (two different chunks, same hash) would corrupt data
> by aliasing. SHA-256's collision resistance makes that astronomically unlikely, so we can
> safely treat "same hash" as "same content."

---

## Step 4 (cont.): Deep dive — delta sync

When a file changes, **don't re-upload it.** Re-chunk the new version, hash each chunk, and
compare against the stored version's chunk list. Only the chunks whose hashes **differ** need to
travel.

```
old file chunks:  [ A ][ B ][ C ][ D ]
new file chunks:  [ A ][ B'][ C ][ D ]     (user edited the region in chunk 1)
diff:                   ▲ only index 1 changed
upload:           just chunk B'   →  new recipe [A, B', C, D]
```

For a fixed-size scheme, "what changed" is a per-index hash comparison. (Insertions mid-file are
the boundary-shift weakness above; content-defined chunking is the fix in real systems.)

Combine delta sync with dedup and the win compounds: the changed chunk might *already exist* in
the store (someone else has identical content), so even the "new" chunk may upload zero bytes.

> **Trade-off — delta computation cost.** Computing deltas costs CPU and I/O (re-hash the file).
> For tiny files it can be cheaper to just re-send. Systems often skip delta sync below a size
> threshold.

---

## Step 4 (cont.): Deep dive — versioning, conflicts, notifications

**Versioning.** Because files are recipes (lists of hashes) and chunks are immutable and shared,
keeping history is cheap: a new version is a new recipe that **reuses** unchanged chunks and adds
only the new ones. "Restore version 3" = point the file at recipe v3. You pay for history only in
the chunks that actually differ between versions.

**Conflict handling.** Two devices edit the same file offline, then both sync. You commit a new
version with a `baseVersion` (the version you started from). If the server's current version no
longer matches your `baseVersion`, you have a conflict. Options:

| Strategy | What happens | Trade-off |
|----------|--------------|-----------|
| **Last-write-wins** | newer timestamp overwrites | simple, but silently loses the other edit |
| **Keep both (conflicted copy)** ⭐ | server keeps both, names one `file (conflicted copy)` | never loses data; user resolves manually (what Dropbox does) |
| **Operational merge** | auto-merge edits (like text CRDTs) | best UX, hard in general for binary files |

> **The non-negotiable:** *never silently lose a user's data.* When in doubt, keep both and let
> the human decide. Availability of edits beats a clever-but-lossy auto-merge.

**Change notification.** Devices shouldn't poll constantly. A **notification service** (long-poll
or push) tells a device "something under your account changed; pull `/changes?since=cursor`." The
device then fetches the new metadata, diffs against local, and pulls only the chunks it lacks —
delta sync in reverse.

---

## In the wild

- **Dropbox** chunks files into ~4 MB blocks, content-addresses them, and pioneered this design at
  scale; it keeps **conflicted copies** rather than losing edits.
- **Git** is content-addressed storage you already use: blobs/trees/commits are all keyed by
  SHA hashes, and unchanged files are shared across commits — the same dedup idea.
- **rsync** popularized rolling-hash delta transfer (send only the differing parts of a file).
- **Backup systems** (Borg, restic, Time Machine) lean hard on chunk dedup so daily backups store
  only what changed.

---

## Interview angle

Open with the **two-part split**: content-addressed **chunks** (the bytes, deduped, keyed by
hash) versus **file metadata** (a recipe = ordered list of chunk hashes). That split *is* the
design — say it first. Then derive **dedup** ("same content → same hash → stored once") and
**delta sync** ("re-hash, compare, send only changed chunks") as natural consequences, and back
them with the bandwidth/storage estimates.

The senior signals: name the **fixed vs content-defined chunking** trade-off and the
boundary-shift problem; insist on **never losing data** in conflicts (keep-both over
last-write-wins); and note that **versioning is cheap** because chunks are immutable and shared.

**Common follow-ups:**
- "User changes one byte in a 1 GB file — what travels?" → re-chunk + hash; only the affected
  chunk(s) upload. (And note fixed-size chunking's insert weakness → content-defined chunking.)
- "Two offline edits collide — what do you do?" → detect via `baseVersion` mismatch; keep both as
  a conflicted copy; never silently overwrite.
- "How is storing 5 versions not 5x the storage?" → versions share unchanged chunks; you store
  only the differing chunks.
- "Why hash the content instead of using a random ID?" → the hash *is* the dedup key and the
  integrity check; same content anywhere maps to the same key automatically.
- "How do other devices find out about a change?" → notification service (long-poll/push) +
  a `since` cursor; device pulls metadata then only the missing chunks.

---

## Practice → the Go assignment

Now build the **content-addressed chunk store with dedup + delta sync** — the core of the design.
Go to [`assignment/`](assignment/) (module `filestore`) and implement, in order:

1. `Chunk(data, chunkSize)` — return the chunk boundaries (fixed-size; last may be shorter).
2. `PutFile(path, data, chunkSize)` — chunk the data, store each chunk keyed by **SHA-256**
   (`crypto/sha256`), storing identical chunks **once** (dedup); record the file's ordered chunk
   hash list.
3. `GetFile(path)` — reconstruct the exact original bytes from the recipe.
4. `ChunkCount()` — number of **unique** chunks physically stored (proves dedup).
5. `ChangedChunks(path, newData, chunkSize)` — indices whose chunk hash differs from the stored
   version (delta sync).

```bash
cd assignment
go test ./...          # red → implement → green
go test -race ./...    # the store is used concurrently
```

The interface is given; you fill in the `// TODO`s. A reference solution is in
[`solution/`](solution/) — try first, peek after.

**Next case study:** [4.12 — Proximity Service (Yelp) »](../12-proximity-service/)
