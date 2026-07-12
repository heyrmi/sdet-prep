# 2.5 — Database Indexing & Storage

> **Module 2 · Building Blocks** · ~30 min read
> *The single highest-leverage database skill. A query that scans a million rows in 800ms can
> return in under a millisecond — with the right index. But every index has a hidden cost, and
> knowing when an index **hurts** is what separates someone who "added an index" from someone who
> understands their database.*

---

## The problem

You have a `users` table with 10 million rows. You run:

```sql
SELECT * FROM users WHERE email = 'ada@example.com';
```

Without help, the database does a **full table scan**: it reads **every single row**, checking each
one's email. Ten million comparisons for one answer. It works, but it's slow and it gets slower as
the table grows. Multiply that by thousands of queries per second and your database melts.

There has to be a way to *jump straight* to the row you want. There is: an **index**.

> **Analogy.** Imagine a 1,000-page textbook and you want every mention of "consistency." Option A:
> read all 1,000 pages cover to cover (full table scan). Option B: flip to the **index** at the
> back — "consistency ... pages 42, 207, 891" — and jump straight there. The index is a small,
> *sorted* helper structure that points to where things live, so you don't read everything. A
> database index is exactly this.

---

## Core idea

An **index** is a separate, **sorted** data structure that lets the database find rows by a column's
value **without scanning the whole table**. It stores the indexed value plus a pointer to the full
row.

The fundamental trade-off, stated once and true everywhere in this lesson:

> **An index makes reads on that column fast — and makes every write slower, because the index must
> be updated too. It also costs disk space.** Indexes are not free. You add them deliberately.

Because the back-of-book analogy only works if the index is **sorted**, the question becomes: what
sorted structure do databases actually use? Overwhelmingly, a **B-Tree**.

---

## B-Tree indexes

A **B-Tree** (balanced tree) is the workhorse index of relational databases (Postgres, MySQL/InnoDB,
etc.). It keeps keys **sorted** and **balanced** so that any lookup takes the same small number of
steps, even as the table grows huge.

```
                 ┌──────────────┐
                 │   [ M ]      │            ← root
                 └──┬────────┬──┘
            ≤ M     │        │   > M
            ┌───────▼─┐    ┌─▼────────┐
            │ [D] [H] │    │ [T] [W]  │      ← internal nodes
            └─┬──┬──┬─┘    └─┬──┬───┬─┘
              ▼  ▼  ▼        ▼  ▼   ▼
            (leaf nodes: sorted keys → row pointers)
```

To find `email = 'P...'`: start at the root, compare, follow the right pointer, compare again,
arrive at the leaf. Each step eliminates a huge fraction of the data.

The magic is the **shape**: B-Trees are wide and shallow. A B-Tree over a **billion** rows is only
~4–5 levels deep, so a lookup is ~4–5 disk reads instead of a billion. That's why lookups feel
instant regardless of table size. Formally, lookups are **O(log n)** vs **O(n)** for a scan.

B-Trees are also great for **range queries** because the leaves are sorted and linked:

```sql
SELECT * FROM events WHERE created_at BETWEEN '2026-01-01' AND '2026-01-31';
```

Find the start of the range, then walk the sorted leaves until the end. (This is why B-Trees beat
hash indexes for ranges — more on that below.)

---

## Primary vs secondary indexes

- A **primary index** is built on the table's **primary key** (e.g. `id`). It's created
  automatically and is unique. It usually defines how the table itself is organized.
- A **secondary index** is any *additional* index you create on other columns you query by (e.g.
  `email`, `created_at`). You add these yourself.

```sql
CREATE INDEX idx_users_email ON users (email);   -- a secondary index
```

A secondary index stores the indexed value plus a way to find the full row (either the primary key
or a direct pointer — see clustered vs non-clustered below).

---

## Composite indexes & the leftmost-prefix rule

A **composite index** covers **multiple columns**, in a specific order:

```sql
CREATE INDEX idx_orders ON orders (user_id, status, created_at);
```

Think of it like sorting a phone book by **(last name, then first name)**. It's organized by the
columns **left to right**. This gives rise to the **leftmost-prefix rule**: the index can be used
only for queries that filter on a **prefix** of the column list, starting from the left.

```
  Index on (user_id, status, created_at) can serve:
    ✅ WHERE user_id = ?
    ✅ WHERE user_id = ? AND status = ?
    ✅ WHERE user_id = ? AND status = ? AND created_at > ?
    ❌ WHERE status = ?                    (skips the leftmost column)
    ❌ WHERE created_at > ?                (skips the two leftmost)
```

Just like a phone book sorted by last-then-first name is useless for "find everyone named *John*"
— first names are scattered everywhere. **Column order in a composite index matters enormously.**
Put the columns you always filter on first.

---

## Covering indexes

Normally an index lookup finds the row's location, then the database does a second read to fetch the
full row from the table. That second hop is **not free**. A **covering index** includes *all the
columns a query needs*, so the database answers the query **entirely from the index** — it never
touches the table at all.

```sql
-- Query only needs email and name:
SELECT email, name FROM users WHERE email = ?;

-- This index "covers" it (contains everything the query reads):
CREATE INDEX idx_cover ON users (email, name);
```

The database reads the index leaf, sees both `email` and `name` right there, and returns —
**index-only scan**. Very fast for hot read paths, at the cost of a wider, larger index.

---

## Clustered vs non-clustered indexes

This is about **how the actual row data is physically stored**.

- **Clustered index:** the table's rows are physically stored **in the order of this index**. There
  can be only **one** (you can only sort the data one way). In MySQL/InnoDB, the primary key *is*
  the clustered index — the rows live inside the primary-key B-Tree's leaves.
- **Non-clustered (secondary) index:** a separate structure that holds the indexed value and a
  **pointer** to where the row actually lives.

```
  CLUSTERED (rows live in key order)        NON-CLUSTERED (separate, points back)
  ┌───────────────────────────────┐         ┌──────────────────────┐
  │ id=1 | Ada   | London  | ...   │         │ email → row pointer  │
  │ id=2 | Linus | Helsinki| ...   │         │  ada@…  → (id=1)      │──┐
  │ id=3 | Grace | NYC     | ...   │◄────────│  grace@…→ (id=3)     │  │
  └───────────────────────────────┘         └──────────────────────┘  │
        the data IS the index                  points back to data ────┘
```

Consequence: with a clustered primary key, a secondary-index lookup finds the **primary key**, then
must do a **second lookup** into the clustered index to get the full row. (A covering secondary
index avoids that second hop — now you see why covering indexes matter.) Postgres differs slightly:
its tables are heap-organized and all indexes are effectively non-clustered pointing at row
locations — the *concepts* still apply.

---

## Hash indexes

A **hash index** stores keys in a **hash table**: hash the value, jump straight to the bucket.

- **Strength:** O(1) **exact-match** lookups (`WHERE x = ?`) — even faster than a B-Tree for equals.
- **Weakness:** **no ordering.** A hash scrambles values, so hash indexes **cannot** serve range
  queries (`>`, `<`, `BETWEEN`) or `ORDER BY`. That's why B-Trees are the default despite hash being
  faster for pure equality.

```
  B-Tree:  sorted     → exact match ✅   range ✅   order by ✅
  Hash:    scrambled  → exact match ✅✅  range ❌   order by ❌
```

Use a hash index only when you do **exclusively** equality lookups and never need order.

---

## The write-amplification cost of indexes

Here's the catch everyone forgets. **Every index must be kept in sync with the table.** When you
`INSERT`, `UPDATE`, or `DELETE` a row, the database must update **every index** on that table too.

```
  Table with 5 indexes:
    1 INSERT into the table
  + 5 updates (one per index)   ← write amplification
  ──────────────────────────
    6 writes for one logical insert
```

This is **write amplification**. Indexes turn reads cheap and writes expensive. They also consume
disk and RAM (indexes are cached too). So:

> **Every index is a bet that the read speedup is worth the write tax.** On a write-heavy table,
> too many indexes can cripple throughput.

### A note on storage engines (LSM-trees)

B-Trees update data **in place**, which means random disk writes — fine, but not ideal for
extremely write-heavy workloads. An alternative storage engine, the **LSM-tree** (Log-Structured
Merge tree, used by Cassandra, RocksDB, LevelDB), batches writes in memory and flushes them
sequentially, trading read amplification for cheaper writes. The B-Tree vs LSM-tree choice is a
whole topic on its own — we go deep in
[Storage engines: B-Tree vs LSM-Tree](../03-distributed-systems/02-storage-engines.md).

---

## When indexes hurt

Indexes are not a free "make it fast" button. They hurt when:

- **The table is write-heavy.** Write amplification can outweigh read gains.
- **The column has low selectivity** (few distinct values, e.g. a `gender` or `is_active` boolean).
  An index on a column that's `true` for 90% of rows is nearly useless — the database may correctly
  ignore it and scan anyway, while you still pay the write cost.
- **The table is tiny.** Scanning 100 rows is already instant; the index just adds overhead.
- **You have redundant/unused indexes.** Each one taxes writes and storage for no benefit. An index
  on `(a, b)` already serves queries on `(a)` thanks to leftmost-prefix — a separate index on `(a)`
  is often redundant.

The discipline: **index the columns you actually filter, join, and sort by — and audit for unused
indexes.** More is not better.

---

## EXPLAIN basics

How do you know if a query uses an index? **`EXPLAIN`** (and `EXPLAIN ANALYZE`) asks the database to
show its **query plan** — the strategy it will use — without you guessing.

```sql
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'ada@example.com';
```

What you're looking for, in plain terms:

| You see... | Meaning | Good or bad? |
|------------|---------|--------------|
| **Index Scan** / **Index Seek** | used an index to jump to rows | good for selective queries |
| **Index-Only Scan** | answered from the index alone (covering) | best |
| **Seq Scan** / **Full Table Scan** | read the whole table | bad on a large table |
| high **rows** estimate vs actual | planner mis-estimated | may need stats/`ANALYZE` |

The workflow: run a slow query through `EXPLAIN`, see a **Seq Scan** on a big table, add the right
index, re-run, confirm it became an **Index Scan**. That loop is 80% of practical database
performance tuning.

---

## Trade-offs & key takeaways

- **An index trades write speed and disk for read speed.** Always a trade-off, never free.
- **B-Tree** is the default: O(log n) lookups *and* range/sort support. **Hash** is faster for pure
  equality but can't do ranges or ordering.
- **Composite indexes obey the leftmost-prefix rule** — column order is a design decision.
- **Covering indexes** answer a query from the index alone (no table hop), at the cost of size.
- **Clustered** index = rows stored in key order (one per table); **secondary** indexes point back.
- **Write amplification:** N indexes = N extra writes per row change. Don't over-index.
- **Index where you filter/join/sort**, prefer high-selectivity columns, and **use `EXPLAIN`** to
  verify — don't guess.

---

## In the wild

- **PostgreSQL** defaults to B-Tree indexes and offers GIN/GiST/Hash/BRIN for special cases; its
  `EXPLAIN ANALYZE` is a gold-standard tuning tool.
- **MySQL/InnoDB** stores tables as a **clustered** B-Tree on the primary key — choosing a good
  primary key directly affects physical layout and performance.
- **Cassandra** uses **LSM-tree** storage and is tuned for writes, with a very different indexing
  story (partition + clustering keys) — see the storage-engines lesson.
- **MongoDB** uses B-Tree indexes too, including compound and covered queries — the same principles
  carry over to the document world.

---

## Interview angle

When asked "this query is slow, how do you fix it?", reach for **`EXPLAIN` first** ("I'd check
whether it's doing a sequential scan"), then propose an index on the **filtered/joined columns**,
mention **composite index column order** and **leftmost-prefix** if multiple columns are involved,
and — crucially — name the **cost**: "but this slows writes and uses space, so on a write-heavy
table I'd weigh that." Volunteering the write-amplification trade-off unprompted is the senior
signal. Bonus depth: B-Tree vs LSM-tree for write-heavy workloads.

**Common follow-ups:**
- "You have an index on `(a, b, c)`. Does `WHERE b = ?` use it?" → No — leftmost-prefix; `b` isn't
  the left column.
- "Why not just index every column?" → write amplification, disk/RAM cost, redundant/unused indexes
  that only add overhead.
- "When would you choose a hash index over a B-Tree?" → exclusively exact-match lookups, never
  ranges or ordering.
- "What's a covering index and why is it fast?" → it contains all columns the query needs, so the
  database answers from the index alone (index-only scan), skipping the table read.

---

## Self-check

1. Why is a B-Tree lookup ~O(log n) instead of O(n)? Why does that make table size barely matter?
2. Given an index on `(country, city, name)`, which of these can use it: `WHERE city='Paris'`;
   `WHERE country='FR'`; `WHERE country='FR' AND city='Paris'`? Explain.
3. What is write amplification, and why does adding a 6th index to a hot table risk hurting?
4. A query does `WHERE status = ? ORDER BY created_at`. Would a hash index help? Why or why not?
5. You run `EXPLAIN` and see "Seq Scan" on a 50M-row table for a selective filter. What do you do?

---

**Next:** [2.6 — Replication »](06-replication.md)
