# 2.12 — Distributed Unique IDs

> **Module 2 · Building Blocks** · ~28 min read
> *Every tweet, order, message, and upload needs a unique name. On one database that's a solved
> problem — `AUTO_INCREMENT` and you're done. Spread across hundreds of machines, "give me a new
> unique number" becomes a surprisingly deep distributed-systems question.*

---

## The problem

You have a single database. Every new row gets a unique ID for free:

```sql
INSERT INTO orders (...) VALUES (...);   -- DB hands back id = 1, 2, 3, ...
```

The database is the single source of truth, so it can simply keep a counter and hand out the next
number. Easy. Now grow up, like in [Module 0.1](../00-foundations/01-scale-zero-to-millions.md):
you **shard** the database across many machines (Module 2.7) so no single box holds all the data
or all the writes. Suddenly there is **no single counter** anymore. If Shard A and Shard B both
start counting from 1, you get **two orders with ID = 1**. Collision.

You could route every "give me an ID" request to one central counter — but now that counter is a
**single point of failure** and a **bottleneck** every write must wait on. The whole point of
sharding was to *remove* the central bottleneck; re-introducing one for IDs defeats it.

> **Analogy.** A single bakery uses one ticket dispenser — "now serving #42" — and it works
> perfectly because there's one machine and one line. Now open 100 branches nationwide. If every
> branch has its own dispenser starting at #1, two customers in two cities both hold ticket #1 —
> useless if you ever combine the lines. You need a scheme where every branch can print tickets
> **locally and instantly**, yet **no two tickets anywhere ever collide.** That's distributed ID
> generation.

---

## Core idea: what makes a good ID?

Before choosing an approach, pin down what you actually need:

- **Unique** — never collides, across all machines, forever. Non-negotiable.
- **Compact** — fits in 64 bits ideally; IDs are stored on every row and sent over the wire
  billions of times. A bloated ID is a tax you pay everywhere.
- **Sortable / time-ordered** — *often* wanted. If newer IDs are numerically larger, you can sort
  feeds by ID, range-scan "recent" rows efficiently, and get good database index locality.
- **k-sorted** — a softer version: IDs are *roughly* time-ordered (within a few milliseconds),
  even if not perfectly. Usually good enough and much cheaper than perfect ordering.
- **No coordination on the hot path** — generating an ID shouldn't require a network call to a
  central service per ID. That's the bottleneck we're trying to avoid.

You rarely get all of these. The approaches below trade them off differently.

> **Why sortability matters.** Database indexes (B-trees, Module 2.5) love **monotonically
> increasing** keys: new rows append to the "right" of the tree, keeping it compact and writes
> fast. Random IDs scatter inserts all over the index, causing page splits and fragmentation.
> "Time-sortable" isn't a nice-to-have — it's a real performance lever.

---

## Approach 1: UUID (random, no coordination)

A **UUID** is a 128-bit value. Version 4 is essentially **122 random bits** — so large that the
chance of two ever colliding is negligible. Each machine generates its own, **no coordination at
all**.

```
   f47ac10b-58cc-4372-a567-0e02b2c3d479   (128 bits, mostly random)
```

| Pros | Cons |
|------|------|
| Zero coordination — generate anywhere, instantly | **128 bits** — double the size of a 64-bit int |
| Effectively no collision risk | **Not sortable** — random bits ⇒ random order |
| Dead simple, built into every language | Random inserts hurt DB index locality |

UUIDv4 is great when you just need uniqueness and don't care about order (e.g. an idempotency key,
a session token). It's a poor primary key for a high-write table you'll sort by time.

> **Note:** newer **UUIDv7** prepends a millisecond timestamp, making UUIDs *time-sortable* while
> staying coordination-free — a modern middle ground, at the cost of still being 128 bits.

---

## Approach 2: DB ticket server

Dedicate **one** small database table whose only job is to hand out IDs. Each request does an
atomic increment and returns the value.

```
   [ Service A ] ┐
   [ Service B ] ┼──► [ Ticket DB ]  REPLACE INTO tickets ...; SELECT LAST_INSERT_ID();
   [ Service C ] ┘                   → 1, 2, 3, 4, ...
```

- ✅ Simple; IDs are compact and perfectly sortable.
- ❌ A **single point of failure** and a **bottleneck** — every ID needs a round-trip. You can run
  two ticket servers (one for even IDs, one for odd) for redundancy, but it's a band-aid.

Fine at modest scale; doesn't survive serious write volume.

---

## Approach 3: Range (segment) allocation

Soften the ticket server: instead of handing out **one** ID per request, hand out a **block** of,
say, 1,000 IDs. Each app server grabs a range, then serves IDs from it **locally** with zero
network calls until the range runs out.

```
   Server A asks ticket DB → gets range [1000–1999]   serves 1000,1001,... locally
   Server B asks ticket DB → gets range [2000–2999]   serves 2000,2001,... locally
                                          └─ one round-trip per 1000 IDs, not per ID
```

- ✅ Cuts coordination by 1000× (or whatever your block size is); IDs stay compact and increasing.
- ❌ IDs can be handed out **out of strict time order** across servers (A may still be on 1005 while
  B issues 2000); a crashed server **wastes** its remaining range (gaps — usually harmless). Still
  leans on a central allocator, just far less often.

A solid, pragmatic choice — this is roughly what Flickr's ticket servers and many "ID service"
designs do.

---

## Approach 4: Snowflake (timestamp + machine + sequence)

Twitter's **Snowflake** is the classic answer for "compact, time-sortable, no per-ID
coordination." Pack a **64-bit** integer from three parts:

```
   64-bit Snowflake ID
   ┌─┬──────────────────────────────┬────────────┬──────────────┐
   │0│        timestamp (41 bits)    │ machine ID │  sequence    │
   │ │       ms since custom epoch   │  (10 bits) │  (12 bits)   │
   └─┴──────────────────────────────┴────────────┴──────────────┘
    1            41                       10            12         = 64 bits
```

- **1 sign bit** — unused (kept 0 so the number stays positive).
- **41 bits timestamp** — milliseconds since a *custom epoch*. 41 bits ≈ **69 years** of ms.
- **10 bits machine ID** — identifies the generating node. 2¹⁰ = **1024** distinct machines.
- **12 bits sequence** — a per-millisecond counter on each machine. 2¹² = **4096** IDs per
  machine *per millisecond*.

How a machine generates one:

```
1. Read current time in ms.
2. Same ms as last ID?  → increment the 12-bit sequence.
   New ms?              → reset sequence to 0.
3. Sequence overflowed (4096 in one ms)? → busy-wait until the next ms.
4. Shift the parts into place and OR them together into one 64-bit int.
```

Why it's clever:

- **Time-sortable** — the timestamp is in the **high bits**, so larger ID ⇒ later time (k-sorted;
  exact ordering only within a machine).
- **No coordination per ID** — each machine generates locally; the only setup is assigning unique
  machine IDs (often via ZooKeeper/etcd at boot — Module 3.4).
- **Compact** — fits a 64-bit `BIGINT`.
- **High throughput** — 4096 × 1000 = **~4M IDs/sec per machine**.

### The clock skew problem

Snowflake's correctness rests on **time only moving forward**. But servers run **NTP**, which can
**step the clock backward** to correct drift. If the clock jumps back, a machine could generate an
ID with a *smaller* timestamp than one it already issued — risking a **duplicate** or breaking the
sort order.

```
   issued ID at t = 1000ms ──► NTP corrects clock back to 998ms ──► next ID at 998ms
   → smaller timestamp than an already-issued ID → ordering breaks, collision risk
```

Mitigations: **refuse to generate** (wait or error) while `now < last_timestamp`; use a
**monotonic clock**; or tolerate a tiny backward step. This "what if the clock goes backward?" is
the favorite Snowflake interview follow-up — naming it is the senior signal.

---

## Comparison table

| Approach | Bits | Sortable? | Coordination | Throughput | Notes |
|----------|------|-----------|--------------|------------|-------|
| **UUIDv4** | 128 | ❌ random | None | Unlimited | Simplest; bad index locality |
| **UUIDv7** | 128 | ✅ (ms) | None | Unlimited | Time-ordered UUID; still 128 bits |
| **DB ticket** | 64 | ✅ exact | Per ID (central) | Low | SPOF + bottleneck |
| **Range alloc** | 64 | ~roughly | Per *block* | High | Gaps on crash; pragmatic |
| **Snowflake** | 64 | ✅ k-sorted | Per *machine* (boot) | ~4M/machine/s | Clock skew caveat ⭐ |

---

## Trade-offs & key takeaways

- **The enemy is the central counter.** A single allocator is simple but a bottleneck and SPOF;
  every distributed scheme is a way to generate IDs **locally** without colliding.
- **Compactness matters more than it looks.** 64 bits vs 128 bits is paid on every row, index, and
  network hop, at billions of scale.
- **Sortability is a performance lever**, not just a nicety — time-ordered IDs keep DB indexes
  happy and make "recent items" queries cheap.
- **Snowflake is the canonical 64-bit answer:** `timestamp | machine | sequence`. Know the bit
  layout and the **clock-skew** failure mode cold.
- **UUIDs trade size and order for total simplicity** — perfect when you only need uniqueness
  (idempotency keys, tokens) and don't sort by ID.
- **Range allocation** is the pragmatic middle: central allocator, but only once per *block*.

---

## In the wild

- **Twitter Snowflake** — the original; 64-bit `timestamp | datacenter | worker | sequence`.
- **Instagram** — a Snowflake-like scheme implemented in Postgres stored procedures, packing
  timestamp + shard ID + per-shard sequence into 64 bits.
- **Discord** — uses Snowflake IDs (their custom epoch is in 2015); message IDs encode creation
  time, which clients decode to render timestamps.
- **Flickr** — classic **ticket servers** (two MySQL boxes, one even / one odd, for redundancy).
- **MongoDB ObjectId** — 12 bytes: timestamp + machine + process + counter (a Snowflake cousin).
- **Sonyflake, Boundary's flake** — community Snowflake variants with different bit splits.

---

## Interview angle

When a design needs IDs at scale, **start by stating the requirements** (unique, ~64-bit,
time-sortable, no per-ID coordination). Reject the **single DB counter** as a bottleneck/SPOF,
mention **UUID** (simple but 128-bit and unsorted), then land on **Snowflake** and **draw the bit
layout**. The senior move is volunteering the **clock-skew** problem and how you'd handle it
(refuse to go backward / monotonic clock), plus how machines get unique IDs at boot (coordination
service). If the interviewer doesn't need ordering, note that **UUIDv4** is the simplest correct
answer — choosing the *simplest thing that meets the constraints* is itself good signal.

**Common follow-ups:**

- *"What if a server's clock moves backward (NTP)?"* → refuse to generate until time catches up,
  or use a monotonic clock; never emit an ID with a past timestamp.
- *"How do machines get unique machine IDs?"* → assigned at boot via ZooKeeper/etcd or config; the
  10-bit field caps you at 1024 nodes.
- *"Why not just use auto-increment?"* → single counter ⇒ bottleneck + SPOF; doesn't survive
  sharding.
- *"Why is UUIDv4 a bad primary key for a write-heavy table?"* → random ⇒ poor B-tree index
  locality (page splits, fragmentation); 128 bits is also bulky.

---

## Self-check

1. Why does a single `AUTO_INCREMENT` counter stop working once you shard the database?
2. List the bit layout of a 64-bit Snowflake ID and what each field is for. Roughly how many IDs
   can one machine generate per millisecond, and why that number?
3. Why are UUIDv4 IDs a poor choice when you frequently sort or range-scan by ID?
4. Explain the clock-skew problem in Snowflake and one way to handle it.
5. How does range (segment) allocation reduce coordination compared to a per-ID ticket server,
   and what does it give up in exchange?

---

## Practice → the coding assignment

The Go assignment for this topic is the full **Unique ID Generator** case study, where you'll
build a **Snowflake-style generator**: pack timestamp, machine ID, and sequence into a 64-bit int,
handle sequence rollover within a millisecond, and defend against the clock moving backward.

```bash
cd ../04-case-studies/03-unique-id-generator/assignment
go test ./...
go test -race ./...    # the generator is shared across goroutines
```

Go to **[4.3 — Unique ID Generator](../04-case-studies/03-unique-id-generator/)**. Bring this
lesson's bit-layout diagram with you.

**Next:** [2.13 — Probabilistic Data Structures »](13-probabilistic-structures.md)
