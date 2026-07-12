# 2.4 — Databases: SQL vs NoSQL

> **Module 2 · Building Blocks** · ~30 min read
> *The most over-argued, under-reasoned decision in backend engineering. By the end you'll choose
> a database from your **constraints** — structure, scale, query patterns, consistency — instead of
> from a blog post's headline.*

---

## The problem

Your app needs to store data and get it back later. Easy at first: throw it in a table, query it.
Then the questions pile up. *Does this data have a fixed shape, or does every record look a little
different? Do I need to ask "give me all orders for users in California who signed up last month"?
Can I tolerate seeing slightly stale data for a fraction of a second, or must every reader see the
exact latest value? Will this grow to a terabyte? A petabyte?*

The answers point at very different databases. Pick wrong and you either fight your database every
day or rebuild on a deadline. This lesson is about choosing well.

> **Analogy.** Choosing a database is like choosing a vehicle. A sedan (relational DB) is the
> sensible default — comfortable, predictable, handles almost everything. A cargo truck
> (wide-column store) hauls enormous loads but is overkill for groceries. A motorcycle
> (key-value store) is blazing fast for one rider and one bag, useless for a family of five. There
> is no "best vehicle" — only the best fit for the trip. People who buy a cargo truck to commute
> because "it's bigger" are making the classic NoSQL mistake.

---

## Core idea

There are two broad families.

- **Relational databases (SQL)** store data in **tables** of rows and columns with a fixed,
  predefined **schema**. You query them with **SQL** (Structured Query Language). They guarantee
  strong **transactions**. Examples: **PostgreSQL, MySQL, SQL Server, Oracle**.
- **NoSQL databases** is an umbrella term ("Not Only SQL") for everything that broke away from the
  rigid table model to win **scale**, **flexible shape**, or **specialized queries**. There are
  four main sub-families, and they are very different from each other.

The headline distinction people fixate on is "SQL has a schema, NoSQL doesn't." That's true but
shallow. The deeper distinction is about **what guarantees you get** and **how the system scales**.

---

## The relational model

A relational database stores **entities** as rows in tables, and represents **relationships**
between entities by referencing keys.

```
  users                          orders
  ┌────┬─────────┬──────────┐    ┌────┬─────────┬────────┐
  │ id │ name    │ city     │    │ id │ user_id │ total  │
  ├────┼─────────┼──────────┤    ├────┼─────────┼────────┤
  │ 1  │ Ada     │ London   │◄───┤ 99 │ 1       │ 42.00  │
  │ 2  │ Linus   │ Helsinki │    │ 98 │ 2       │ 13.50  │
  └────┴─────────┴──────────┘    └────┴─────────┴────────┘
                                       └─ foreign key points back to users.id
```

`orders.user_id` is a **foreign key** — it points at `users.id`. To answer "show each order with
its buyer's name," you **join** the two tables on that key:

```sql
SELECT users.name, orders.total
FROM orders
JOIN users ON orders.user_id = users.id;
```

The **schema** (which tables exist, which columns, which types) is defined up front and enforced.
Try to insert a row missing a required column, or a string into an integer column, and the database
**rejects it**. This is a feature: the database guarantees your data stays well-shaped.

### Normalization

**Normalization** is the practice of storing each fact **exactly once** and referencing it by key,
instead of duplicating it. In the example above, a user's city lives only in `users` — orders don't
copy it. If Ada moves to Paris, you update **one row** and every query sees the change.

The opposite — **denormalization** — duplicates data to avoid joins (faster reads, but now you must
keep copies in sync). Relational databases lean normalized; many NoSQL designs deliberately
denormalize. Hold that thought; it's a core trade-off.

### ACID — the transaction guarantee

The crown jewel of relational databases is the **transaction**: a group of operations that succeed
or fail **as one unit**. Transactions provide **ACID** guarantees:

- **A — Atomicity:** all steps happen, or none do. Transfer $100 from A to B: either both the debit
  and credit happen, or neither. You never lose money in the gap.
- **C — Consistency:** the database moves from one valid state to another; constraints (e.g.
  "balance ≥ 0") always hold.
- **I — Isolation:** concurrent transactions don't trample each other; the result is as if they ran
  one at a time.
- **D — Durability:** once committed, the data survives a crash (it's written to disk / a log).

```
  BEGIN;
    UPDATE accounts SET balance = balance - 100 WHERE id = 'A';
    UPDATE accounts SET balance = balance + 100 WHERE id = 'B';
  COMMIT;                       -- both, or (on error) ROLLBACK → neither
```

If your domain has money, inventory, or anything where a half-finished operation is a disaster,
**ACID transactions are why relational databases dominate.**

---

## The NoSQL families

NoSQL is not one thing. Four families, each solving a different problem.

### 1) Key-Value stores

The simplest model: a giant dictionary. You `PUT(key, value)` and `GET(key)`. The value is opaque
to the database — it doesn't look inside it. Blazing fast, trivially scalable, but you can only look
up **by key** (no "find all values where...").

```
  "user:1:session"  →  {token: "abc", exp: 1700000000}
  "cart:42"         →  ["sku-1", "sku-9"]
```

- **Examples:** Redis, Memcached, Amazon DynamoDB (in its simplest mode), etcd.
- **Use cases:** caching, session storage, feature flags, rate-limiter counters, leaderboards.

### 2) Document stores

Store self-contained **documents** (JSON/BSON). Each document can have a **different shape**, and
you can query on **fields inside** the document — not just the key.

```
  {
    "_id": "order-99",
    "user": { "name": "Ada", "city": "London" },   ← nested / denormalized
    "items": [ {"sku": "sku-1", "qty": 2} ],
    "total": 42.00
  }
```

- **Examples:** MongoDB, Couchbase, Amazon DocumentDB, Firestore.
- **Use cases:** content management, product catalogs, user profiles — anywhere records are
  semi-structured and naturally "object-shaped." Great when each record is fetched as a whole.

### 3) Wide-column stores

Data is stored in tables, but each row can have **billions of columns**, and rows need not share
the same columns. Optimized for **massive write throughput** and queries over huge datasets,
typically by a row key + column range. Think "a sparse, distributed, sorted map."

```
  row key        │ columns →
  ───────────────┼──────────────────────────────────────────
  user:1         │ name:Ada   city:London   login:2026-06-15
  sensor:7       │ temp@09:00:21.4  temp@09:01:21.6  ...   (time-series, sparse)
```

- **Examples:** Apache Cassandra, HBase, Google Bigtable, ScyllaDB.
- **Use cases:** time-series, event logging, messaging, IoT — write-heavy data at enormous scale.

### 4) Graph databases

Data is **nodes** and **edges** (relationships), with relationships as first-class citizens.
Optimized for queries that traverse connections ("friends of friends of friends").

```
   (Ada) ──FRIEND──► (Linus) ──FRIEND──► (Grace)
     │
   LIKES
     ▼
   (Go)
```

- **Examples:** Neo4j, Amazon Neptune, JanusGraph.
- **Use cases:** social networks, recommendation engines, fraud detection, knowledge graphs —
  anywhere the *relationships* are the point. (A deeply-nested join in SQL is what a graph DB does
  natively and fast.)

### NoSQL family comparison

| Family | Data model | Query by | Killer use case | Example |
|--------|-----------|----------|-----------------|---------|
| Key-value | dictionary | key only | cache, sessions | Redis, DynamoDB |
| Document | JSON docs | key or fields | catalogs, profiles | MongoDB |
| Wide-column | sparse rows × many cols | row key + range | time-series, write-heavy | Cassandra |
| Graph | nodes + edges | traversals | social, recommendations | Neo4j |

---

## BASE vs ACID

Many (not all) NoSQL systems trade ACID for **BASE** to win availability and scale:

- **BA — Basically Available:** the system stays responsive even during partial failures.
- **S — Soft state:** the data may be in flux (replicas not yet agreeing).
- **E — Eventually consistent:** if writes stop, all replicas *eventually* converge to the same
  value — but for a window, different readers may see different values.

```
  ACID  → "always correct, even if that means saying no / waiting"
  BASE  → "always available, correct soon"
```

This isn't NoSQL being sloppy — it's a deliberate choice rooted in the **CAP theorem**: when the
network partitions, you must choose between consistency and availability. We devote a whole lesson
to it in [CAP, PACELC & consistency models](09-cap-pacelc-consistency.md). For now: **ACID favors
correctness; BASE favors availability and scale.** Neither is "better."

> **Important nuance:** the SQL=ACID, NoSQL=BASE split is a generalization, not a law. Modern
> distributed SQL databases (CockroachDB, Google Spanner) give ACID at scale. Some NoSQL stores
> offer tunable or full ACID (MongoDB multi-document transactions, DynamoDB transactions). Always
> check the **specific product**, not the category.

---

## How it works: scaling, the real divider

The deepest practical difference is **how each scales when one machine isn't enough.**

- **Relational databases scale up (vertically) most naturally** — a bigger machine. Scaling *out*
  (across machines) is possible but hard, because **joins and transactions across machines are
  expensive**. You eventually shard manually (see [Sharding & partitioning](07-sharding-partitioning.md)),
  and that's where relational gets painful.
- **Many NoSQL databases were born to scale out (horizontally).** They drop cross-machine joins and
  multi-key transactions *precisely so* the data can be partitioned across hundreds of nodes with
  no coordination. The flexibility you give up (joins, ACID across keys) is the price of that scale.

```
  SQL default growth:     [ bigger DB box ]      ← ceiling + single point of failure
  NoSQL default growth:   [db][db][db][db]...    ← add nodes, near-linear scale
```

This is the actual reason "web-scale" companies reach for NoSQL — not because relational is slow,
but because horizontal scale and rigid cross-machine guarantees are fundamentally in tension.

---

## When to pick which

Decide from these factors, not from hype:

| Factor | Lean **SQL (relational)** | Lean **NoSQL** |
|--------|---------------------------|----------------|
| **Data structure** | well-defined, stable schema | flexible / evolving / varied shape |
| **Relationships & joins** | many, central to queries | few; data fits in self-contained docs |
| **Transactions** | need multi-row ACID (money, inventory) | rarely need cross-key transactions |
| **Query patterns** | ad-hoc, flexible, "ask anything" | known, simple access patterns (by key) |
| **Consistency** | need strong / read-your-writes always | eventual consistency acceptable |
| **Scale** | up to large; manual sharding past that | built to scale out to huge write/data volume |
| **Write volume** | moderate to high | extreme (Cassandra-class) |

A useful rule of thumb:

> **Start with PostgreSQL.** It's the boring, correct default for ~90% of applications. It does
> JSON columns, full-text search, and handles serious load on one box. Reach for NoSQL when you
> hit a *specific, measured* constraint that relational can't meet — not before.

---

## The "don't pick NoSQL because it sounds web-scale" trap

This is the single most common database mistake junior teams make. The reasoning goes: "Google and
Netflix use NoSQL, we want to be big like them, so we'll use NoSQL." Then reality hits:

- They needed a join → had to do it in **application code** (slow, buggy, reinvented poorly).
- They needed a transaction → discovered the store doesn't guarantee one across keys.
- They had an ad-hoc reporting question → realized they can only query by the keys they planned for.
- They had **gigabytes**, not petabytes → they never needed horizontal scale at all.

You inherited all of NoSQL's costs and used **none** of its benefits. Most apps that "want to be
web-scale" are nowhere near the scale where relational breaks. **Choose for the constraints you
actually have, with a margin — not for the company you dream of becoming.**

---

## Polyglot persistence

It's not either/or. Mature systems use **polyglot persistence** — the right store for each job:

```
   [ Orders, payments ]   → PostgreSQL   (ACID, joins)
   [ Sessions, cache ]    → Redis        (key-value, speed)
   [ Product catalog ]    → MongoDB      (flexible documents)
   [ Activity feed/logs ] → Cassandra    (write-heavy, scale-out)
   [ "People you know" ]  → Neo4j        (graph traversal)
```

The cost is **operational complexity** (more systems to run, monitor, back up, and keep in sync).
So don't sprawl prematurely — but know that "one database for everything" stops being optimal as a
system grows and its workloads diversify.

---

## Trade-offs & key takeaways

- **There is no "best" database.** SQL vs NoSQL is a trade-off between flexibility/scale and
  guarantees/queryability. Reason from constraints.
- **SQL's superpowers:** schema enforcement, flexible joins, and ACID transactions. Its limit:
  scaling writes/data beyond one machine is hard.
- **NoSQL's superpowers:** horizontal scale, flexible shape, specialized models. Its cost: usually
  weaker consistency, no cross-machine joins, limited transactions, query patterns fixed up front.
- **ACID vs BASE = correctness vs availability.** Modern products blur the line; check the specific
  one.
- **Default to relational (Postgres).** Switch when a *measured* need forces it.
- **Polyglot persistence** is normal at scale — at the cost of operational complexity.

---

## In the wild

- **Amazon** built **DynamoDB** (key-value/document, wide-column lineage) for shopping-cart scale,
  but still runs relational databases for many internal systems.
- **Facebook** stores the social graph in systems backed by sharded MySQL plus a graph layer
  (TAO) — relational *underneath*, graph-shaped on top.
- **Netflix** uses **Cassandra** for high-volume viewing data and **EVCache** (Memcached) for
  caching — and relational stores where transactions matter.
- **Most startups** that succeed ran on a single **PostgreSQL** instance far longer than they
  expected to.

---

## Interview angle

When the interviewer asks "SQL or NoSQL for this system?", **never** answer with a one-word
preference. Instead, walk the **decision factors**: "What's the data shape? Do we need joins or
multi-row transactions? What are the query patterns? What's the consistency requirement? What's the
write volume and scale?" Then justify. Saying "I'd start with Postgres because the data is
relational and we need transactions for payments, but I'd put the session store in Redis and move
the activity feed to Cassandra if write volume demands it" is exactly the trade-off reasoning that
signals seniority.

**Common follow-ups:**
- "Why not just use NoSQL for everything to scale?" → you lose joins/transactions/ad-hoc queries;
  most systems aren't at the scale that justifies it; you pay the cost without the benefit.
- "How do relational databases scale then?" → vertical first, then read replicas
  ([Replication](06-replication.md)), then sharding ([Sharding](07-sharding-partitioning.md)).
- "What does ACID give you that eventual consistency doesn't?" → atomic multi-step operations and
  the guarantee that a committed read reflects the latest write.
- "When is a graph database clearly the right call?" → when queries are deep relationship
  traversals (friends-of-friends, fraud rings) that explode into many joins in SQL.

---

## Self-check

1. Explain ACID to a friend using a bank transfer. Which letter prevents losing money mid-transfer?
2. Name the four NoSQL families and one use case for each.
3. What does "eventually consistent" mean, and when is it acceptable in a real app?
4. A teammate wants to use MongoDB for a payments ledger "to be future-proof." What questions do you
   ask, and what's your likely recommendation?
5. What is polyglot persistence, and what does it cost?

---

**Next:** [2.5 — Database indexing & storage »](05-indexing.md)
