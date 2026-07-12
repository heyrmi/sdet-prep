# 4.3 — Design a Distributed Unique ID Generator

> **Module 4 · Case Studies** · ~30 min read + coding assignment
> *Concepts exercised:* Snowflake bit-packing, time-sortable (k-sorted) IDs, 64-bit
> design, machine coordination, clock skew / NTP-backwards handling, sequence
> rollover, the uniqueness-vs-coordination trade-off.

---

## The problem

Every record needs a unique identifier — an order ID, a tweet ID, a message ID. On a
single database this is trivial: an `AUTO_INCREMENT` column. But once you **shard your
data across many databases**, there's no single counter to ask, and two shards happily
hand out the same `123`. We need a way to generate **globally unique IDs** across many
machines, **fast**, **without a central bottleneck**.

We'd also love some bonus properties: IDs that **fit in 64 bits** (cheap to store and
index), and IDs that are **roughly time-sortable** so "newest first" feeds and range
scans are cheap.

> **Analogy.** Picture a chain of bakeries that all print numbered tickets, and a rule
> that *any* ticket number, from *any* shop, must be unique forever — with no shop
> phoning headquarters for each ticket (too slow). The trick the bakeries use: bake
> the **time**, the **shop number**, and a **per-shop counter** right into the ticket
> number. Same millisecond, same shop? The counter breaks the tie. Different shop?
> The shop number does. Different time? The clock does. No coordination needed, and
> tickets sort by time. That's **Snowflake**.

---

## Step 1: Requirements (always start here)

**Functional**
- Generate IDs that are **globally unique** (never a collision, ever).
- IDs are **64-bit integers** (fit a `BIGINT` / `int64`).

**Non-functional**
- **High throughput** — many thousands of IDs per second per machine, ideally with no
  network call on the hot path.
- **Time-sortable (k-sorted)** — IDs generated later should generally be numerically
  larger, so they sort approximately by creation time. ("k-sorted" = sorted to within
  a small window, which is all you can promise across loosely-synced machines.)
- **No single point of failure / no central bottleneck** on the generation path.

**Clarifying questions to ask the interviewer**
- *Strictly monotonic, or just roughly time-sortable?* Strict ordering across machines
  is very expensive; "k-sorted" is usually enough.
- *How many IDs per second, across how many machines/data centers?* Sets the bit
  budget for machine ID and sequence.
- *Do IDs need to be unguessable (security)?* Snowflake IDs are sequential and leak
  rate/timing info — if you need opacity, that's a different design.

---

## Step 2: The options (and their trade-offs)

| Approach | Unique? | Time-sortable? | 64-bit? | Coordination | Notes |
|----------|---------|----------------|---------|--------------|-------|
| **UUID v4** (random 128-bit) | Yes (prob.) | No | No (128-bit) | None | Trivial & decentralized, but big and unsortable → bad index locality |
| **DB ticket server** (one auto-increment table) | Yes | Yes | Yes | Central | Simple, but a **single bottleneck / SPOF**; replicate it and you risk dupes |
| **DB range allocation** (hand each machine a block of 1000 IDs) | Yes | ~ | Yes | Occasional | Few DB hits, but IDs not strictly time-ordered; lose a block on crash |
| **Snowflake** (timestamp + machine + sequence) ⭐ | Yes | Yes (k-sorted) | Yes | One-time machine-ID assignment | Fast, decentralized, sortable — the standard answer |

The takeaway: **UUID** removes coordination entirely but sacrifices size and sorting.
A **central counter** gives perfect ordering but reintroduces the bottleneck we sharded
to escape. **Snowflake** is the sweet spot — and it's what interviewers want to hear.

---

## Step 3: Snowflake in depth

Twitter's **Snowflake** packs three things into a single 64-bit integer. We leave the
top bit `0` so the number is always a positive signed `int64`:

```
 63                                                  0
┌─┬───────────────────────────────┬──────────┬────────────┐
│0│   timestamp (41 bits)          │ machine  │  sequence  │
│ │   ms since custom epoch        │ (10 bits)│  (12 bits) │
└─┴───────────────────────────────┴──────────┴────────────┘
 1            41                       10            12     = 64 bits
```

- **1 bit** unused (sign bit kept 0 → positive IDs).
- **41 bits — timestamp**, in **milliseconds since a custom epoch**. 2⁴¹ ms ≈
  **69 years** of range.
- **10 bits — machine ID** → **1024** distinct machines/workers.
- **12 bits — sequence** → **4096** IDs per machine **per millisecond**.

Multiply it out: 4096 IDs/ms × 1000 ms = **~4 million IDs per second per machine**, ×
1024 machines = **~4 billion IDs/sec** cluster-wide. Plenty.

### Why a *custom* epoch?

The 41-bit timestamp counts ms since an epoch *you* pick (say 2024-01-01), not the Unix
epoch (1970). Starting the clock recently means all 69 years of range are *ahead* of
you instead of half-burned. Pick it once and never change it.

### Why this layout makes IDs time-sortable

The **timestamp occupies the high bits**, so a later millisecond always produces a
numerically larger ID, regardless of machine or sequence. Within one millisecond, the
sequence increments, so order is preserved there too. Across machines in the *same*
millisecond, order is arbitrary — hence "**k**-sorted," not perfectly sorted. That's
the deliberate trade-off: we give up strict global ordering to avoid coordination.

### Generating an ID (the algorithm)

```
on NextID():
  ts = now_ms_since_epoch()
  if ts < lastMs:                # clock jumped BACKWARDS
      return error               # refuse — never risk a duplicate
  if ts == lastMs:               # same millisecond as the last ID
      seq = (seq + 1) & 4095     # increment, wrapping at 12 bits
      if seq == 0:               # we used all 4096 slots this ms
          ts = wait_until(> lastMs)   # spin to the next millisecond
  else:                          # a new, later millisecond
      seq = 0
  lastMs = ts
  return (ts << 22) | (machineID << 12) | seq
```

Three details that separate a real answer from a toy:

1. **Same-millisecond bursts** → increment the sequence (up to 4095).
2. **Sequence overflow** within a millisecond → **wait for the next millisecond**
   (busy-spin). You can briefly exhaust 4096 IDs in one ms under extreme load.
3. **Clock moving backwards** → the hard one, below.

### The clock-skew / NTP-backwards problem

Machine clocks drift, and **NTP** periodically corrects them — sometimes by **stepping
the clock backwards**. If `now()` suddenly returns an *earlier* millisecond than the
last ID we issued, naively continuing could **reissue timestamps we've already used →
duplicate IDs**. That's the one thing an ID generator must never do.

Standard handling:
- **Refuse and error** (what we implement, and what Twitter's Snowflake does): if
  `ts < lastMs`, return an error rather than risk a dup. The caller retries; the brief
  unavailability is safer than a collision.
- **Wait it out**: if the backward jump is tiny, spin until the clock catches back up
  to `lastMs`, then resume.
- **Steal bits / sequence**: some variants tolerate small regressions by bumping the
  sequence, but this is fiddly.

> **Trade-off — availability vs correctness.** On a backward clock jump you choose:
> erroring (favor correctness, brief unavailability) or waiting (favor availability if
> the jump is small). Stating this choice out loud is the senior signal.

### Decoding an ID

Because each field lives in fixed bits, you can **read them back out** with shifts and
masks — useful for debugging ("when was this created? which machine?"):

```
timestamp = id >> 22
machineID = (id >> 12) & 1023
sequence  =  id        & 4095
```

(Add the custom epoch back to the timestamp to get a real wall-clock time.) Your
assignment implements these `Timestamp` / `MachineID` / `Sequence` helpers, and the
tests use them to verify the bits are packed correctly.

---

## Step 4: Assigning machine IDs

Each generator needs a **unique 10-bit machine ID**. Where does it come from?

- **Static config / deployment** — simplest; baked into each instance's env.
- **A coordination service** — **ZooKeeper** or **etcd** hands out a unique ID on
  startup (Twitter used ZooKeeper). See Module 3.4 on coordination.
- **Derive from infrastructure** — e.g. from the private IP or pod ordinal.

The one rule: **no two live generators may share a machine ID**, or they can collide in
the same millisecond. This is the *only* coordination Snowflake needs — and it happens
**once at startup**, not on every ID.

---

## In the wild

- **Twitter Snowflake** — the original; 41/10/12 layout, ZooKeeper for worker IDs.
- **Instagram** — a Postgres-based variant: time + shard ID + per-shard sequence,
  generated inside the database with a stored procedure.
- **Sony `sonyflake`, Baidu `uid-generator`** — Snowflake variants tuning the bit split
  (e.g. more machine bits, fewer sequence bits, or lower time resolution for longer
  range).
- **Discord, Mastodon** — use Snowflake-style IDs (Discord even documents how to pull
  the timestamp back out of an ID).
- **ULID / UUIDv7** — modern "time-prefixed" IDs that, like Snowflake, put time in the
  high bits to regain sortability while staying decentralized.

---

## Interview angle

Open by **rejecting `AUTO_INCREMENT`** ("single bottleneck once we shard") and by
weighing **UUID** ("decentralized but 128-bit and unsortable → poor index locality").
Then present **Snowflake**, *drawing the 41/10/12 bit layout* — the diagram is the
answer. Explain **why time is in the high bits** (sortability) and do the throughput
math (~4M/sec/machine). Then earn the senior bump by raising the **two hard cases
unprompted**: **sequence overflow within a millisecond** (wait for the next ms) and the
**clock moving backwards** (error vs wait — availability vs correctness). Close on
**machine-ID assignment** via ZooKeeper/etcd as the only coordination, done once.

**Common follow-ups:**
- "What if two requests hit the same machine in the same millisecond?" → 12-bit
  sequence, 4096 slots; overflow waits for the next ms.
- "What if the clock goes backwards?" → detect `ts < lastMs`; error (or wait). Never
  reissue a timestamp.
- "Why not UUID?" → 128-bit, random → not sortable, hurts B-tree index locality.
- "How do machines get unique IDs?" → ZooKeeper/etcd hands them out at startup.
- "Are IDs guessable?" → yes, sequential; if you need opacity, don't use raw Snowflake.

---

## Practice → the Go assignment

Now build it. Go to [`assignment/`](assignment/) and implement a **Snowflake
generator** (`module snowflake`):

1. `NewGenerator(machineID)` — validate the ID fits **10 bits** (0..1023).
2. `NextID()` — pack `timestamp | machine | sequence`; increment the sequence within a
   millisecond; **wait for the next ms on overflow**; **error if the clock goes
   backwards**.
3. `Timestamp` / `MachineID` / `Sequence` — decode helpers (shifts & masks).

Time is injected via a `now func() int64` field (ms since epoch) so the tests are
**fully deterministic — no real sleeping**. The tests check:
- IDs are **unique** and **monotonically increasing** as the clock advances;
- the **sequence increments** within the same millisecond;
- **machine ID decodes** back to what you put in;
- a **backwards clock returns an error**;
- **sequence overflow** within a ms rolls forward to the next ms;
- a **`-race`** test fires IDs from many goroutines and asserts all are unique (so
  `NextID` must be mutex-protected).

```bash
cd assignment
go test ./...          # red → implement → green
go test -race ./...    # NextID is shared across goroutines — must be safe
```

Std lib only. The interface is given; you fill in the `// TODO`s. A reference solution
is in [`solution/`](solution/) — try first, peek after.

**Next case study:** [4.4 — URL Shortener »](../04-url-shortener/)
