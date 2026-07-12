# 0.1 — Scale From Zero to Millions of Users

> **Module 0 · Foundations** · ~25 min read
> *The single best mental model for the whole course: start with one server, then fix one
> bottleneck at a time. Every "building block" in Module 2 is just a fix for a bottleneck you'll
> meet right here.*

---

## The problem

You just shipped an app. On day one, **10 people** use it. Everything runs on one cheap server
and it's *fine*. Eighteen months later, **10 million people** use it. The exact same code now
falls over constantly. What changed? Not the code — the **load**.

System design is largely the story of **what breaks as load grows, and how you fix each break
without rewriting everything.** Let's walk that story. By the end you'll recognize every major
piece of infrastructure as the answer to a specific, concrete pain.

> **Analogy.** Think of a single food cart. One cook takes orders, cooks, and handles payment.
> Great for a quiet street. Now imagine a lunch rush of thousands. You don't make the one cook
> faster — you split the jobs (cashier, line cooks), add carts, put up a menu board so people
> don't ask the cook the same question, and hire a host to direct the crowd. Scaling a system
> is the same set of moves.

---

## Step 0: A single server

Everything on one box: the **web app**, the **database**, the **cache**, all of it.

```
   User ──HTTP──> [ Single Server: app + DB ]
```

How a request actually flows:

1. User types `myapp.com`. The browser asks **DNS** (the internet's phone book) "what's the IP
   for myapp.com?" DNS replies, say, `15.197.1.20`.
2. Browser opens a connection to that IP and sends an **HTTP request**.
3. Server runs your code, talks to the database, and sends back **HTML or JSON**.

This is genuinely fine for a real product with modest traffic. **Do not over-engineer day one.**
But let's grow.

---

## Step 1: Separate the web tier from the data tier

The first thing that hurts: your app server and database **compete for the same CPU and RAM.**
A heavy query starves the web server, and vice versa. They also scale differently.

**Fix:** put the database on its own machine.

```
   User ──> [ Web Server ] ──> [ Database ]
```

Now you can scale them independently — give the DB more RAM, give the web tier more CPU. This
"separate the tiers" instinct repeats forever.

### A quick word: SQL or NoSQL?

For most apps, **start with a relational database (SQL)** — Postgres, MySQL. They're battle-tested,
support transactions, and you can ask flexible questions with joins. Reach for **NoSQL** when you
have specific needs: massive write volume, very low-latency at scale, unstructured data, or you
truly don't need joins/transactions. We'll go deep in [Module 2.4](../02-building-blocks/04-sql-vs-nosql.md).
The trap to avoid: choosing NoSQL because it sounds "web-scale." Choose for your *constraints*.

---

## Step 2: Scale the web tier — vertical vs horizontal

More users → your single web server's CPU pegs at 100%. Two ways to get more power:

- **Vertical scaling (scale up):** buy a bigger machine. Simple, but there's a ceiling (the
  biggest box money can buy) and **no redundancy** — if it dies, you're fully down.
- **Horizontal scaling (scale out):** add *more* machines. No ceiling, and surviving a single
  machine failure becomes possible. This is how all large systems scale.

We'll go horizontal. But that immediately raises a question: **if there are many web servers,
which one does a user talk to?**

### Enter the load balancer

A **load balancer (LB)** sits in front of your web servers and spreads incoming requests across
them. Users only ever see the LB's public IP; the servers behind it have private IPs.

```
                       ┌─> [ Web Server 1 ]
   User ──> [ LB ] ────┼─> [ Web Server 2 ]
                       └─> [ Web Server 3 ]
```

Two huge wins:
- **No single point of failure (on the web tier):** if Server 1 dies, the LB routes around it.
- **Easy scaling:** traffic doubles? Add Server 4 behind the LB.

> **Critical consequence — make servers stateless.** If Server 1 stored your login session *in
> its own memory*, a later request routed to Server 2 would think you're logged out. So we move
> shared state (sessions, etc.) *out* of the web servers into a shared store (a cache or DB).
> **Stateless web servers** are a recurring theme — they're what makes horizontal scaling work.
> More in [Module 2.1](../02-building-blocks/01-load-balancing.md).

---

## Step 3: Scale the database — replication

The web tier scales nicely now, but **all** of it hammers **one** database. Reads especially
pile up (most apps read far more than they write).

**Fix: database replication.** Run multiple copies of the DB:

- One **primary (leader)** handles **writes**.
- Several **replicas (followers)** receive a copy of the data and serve **reads**.

```
                        writes
   [ Web Servers ] ───────────────> [ Primary DB ]
          │                               │ replicates
          │ reads                         ▼
          └──────────────────────> [ Replica ] [ Replica ]
```

Wins:
- **Read scaling:** add replicas to absorb more read traffic.
- **Availability:** if a replica dies, others serve reads. If the primary dies, a replica gets
  **promoted** to primary.

The catch (and your first real trade-off): **replication lag.** A write hits the primary, but
it takes a moment to copy to replicas. A user who writes then immediately reads from a replica
might not see their own change yet. This is **eventual consistency** — we'll unpack it in
[Module 2.9](../02-building-blocks/09-cap-pacelc-consistency.md).

---

## Step 4: Add a cache

Even with replicas, hitting the database for *every* read is wasteful. Many reads are for the
**same popular data** (the homepage, a trending post). Databases are relatively slow because
they often hit disk and parse queries.

**Fix: a cache** — an in-memory key-value store (e.g., **Redis**, **Memcached**) that sits
between your app and the DB. Memory is ~100,000× faster than disk.

The most common pattern is **cache-aside**:

```
1. App needs data → check cache.
2. HIT  → return it. (fast!)
3. MISS → read from DB, write it into the cache, return it.
```

```
   [ Web Server ] ──> [ Cache ] ──(miss)──> [ Database ]
                          ▲                      │
                          └──────────────────────┘
                              (store result)
```

Caching introduces its own famous problems — **stale data**, **expiration (TTL)**, and **cache
invalidation** ("there are only two hard things in computer science..."). Full treatment in
[Module 2.3](../02-building-blocks/03-caching.md).

---

## Step 5: Use a CDN for static content

Your users are global, but your servers are in one region. Someone in Tokyo loading images from
a server in Virginia waits for data to cross the planet — every image, every time.

**Fix: a Content Delivery Network (CDN).** A CDN is a global network of edge servers that cache
your **static assets** (images, CSS, JS, video) physically close to users. The first user in
Tokyo triggers a fetch from your origin; everyone after gets it from a nearby Tokyo edge.

```
   User (Tokyo) ──> [ CDN edge in Tokyo ] ──(miss, first time)──> [ Origin (Virginia) ]
```

This slashes latency *and* offloads massive traffic from your servers. More in
[Module 2.2](../02-building-blocks/02-reverse-proxy-cdn.md).

---

## Step 6: Decouple with message queues

Some work is **slow** and shouldn't block the user. Example: a user uploads a video. Transcoding
it into five resolutions takes minutes. If the web request waits for that, the user stares at a
spinner and the request times out.

**Fix: a message queue.** The web server drops a "transcode this video" **message** onto a queue
and *immediately* responds "got it, processing!". Separate **worker** servers pull messages off
the queue and do the slow work in the background.

```
   [ Web Server ] ──(produce)──> [ Queue ] ──(consume)──> [ Workers ]
```

This is **asynchronous processing**, and it gives you:
- **Responsiveness:** users aren't blocked on slow work.
- **Decoupling:** producers and consumers scale independently. Backlog growing? Add workers.
- **Buffering:** a traffic spike fills the queue instead of crashing the workers.

Deep dive in [Module 2.10](../02-building-blocks/10-message-queues-streaming.md).

---

## Step 7: Scale the database further — sharding

Replication scales *reads*. But eventually **writes** (or sheer data size) overwhelm a single
primary — one machine can only hold and write so much.

**Fix: sharding (horizontal partitioning).** Split the data across multiple databases, each
holding a *subset*. For example, shard users by `user_id % 4` across 4 DBs:

```
   user_id % 4 == 0 → [ Shard 0 ]
   user_id % 4 == 1 → [ Shard 1 ]
   user_id % 4 == 2 → [ Shard 2 ]
   user_id % 4 == 3 → [ Shard 3 ]
```

Now each shard handles 1/4 of the writes and stores 1/4 of the data. Sharding is powerful but
**hard** — it complicates joins, transactions, and "what happens when you add a 5th shard?"
(that's exactly what **consistent hashing** in [Module 2.8](../02-building-blocks/08-consistent-hashing.md)
solves). We dedicate [Module 2.7](../02-building-blocks/07-sharding-partitioning.md) to it.

---

## Step 8: Observe everything

Once you have dozens of servers, queues, caches, and shards, **you cannot debug by SSH-ing into
boxes.** You need:

- **Logging** — what happened (events, errors).
- **Metrics** — numbers over time (QPS, latency p99, error rate, CPU).
- **Tracing** — follow one request across all the services it touched.
- **Alerting** — get paged *before* users notice.

This is **observability**, covered in [Module 3.6](../03-distributed-systems/06-observability.md).
Rule of thumb: *if you can't measure it, you can't scale it.*

---

## The full picture

Putting every step together, the architecture that serves millions looks like:

```
                         ┌──────────── [ CDN ] (static assets)
   Users ──> [ DNS ] ──> │
                         └─> [ Load Balancer ]
                                   │
                     ┌─────────────┼─────────────┐
              [ Web Server ]  [ Web Server ]  [ Web Server ]   (stateless)
                     │                                  │
                 [ Cache ]                          [ Queue ] ──> [ Workers ]
                     │
              ┌──────┴───────┐
        [ Primary DB ]   read replicas
              │
        ┌─────┴─────┐
   [ Shard 0 ] [ Shard 1 ] ...
```

None of this was designed up front. **Each piece is the fix for a specific bottleneck** we hit
as load grew. That's the mindset: *don't memorize this diagram — understand which pain each box
removes.*

---

## Trade-offs & key takeaways

- **Don't over-engineer.** A single server is the right answer until it isn't. Premature
  scaling wastes time and money and adds failure modes.
- **Add complexity only to fix a measured bottleneck.** Every box above costs operational
  overhead and introduces new failure modes (replication lag, cache staleness, queue backlogs).
- **Stateless web tier** is the foundation of horizontal scaling.
- **Reads ≠ writes.** Replication and caching scale reads; sharding scales writes and storage.
- **Async (queues) is how you keep slow work from hurting users.**

---

## Interview angle

This chapter *is* the skeleton of most system design interviews. When asked "design X for
millions of users," you walk this exact evolution: single server → split tiers → LB + horizontal
web tier → replicas → cache → CDN → queues → shards → observability — **justifying each step
with the bottleneck it removes.** Interviewers love hearing "we don't need this yet, but if write
volume grows we'd shard by ___." That's trade-off reasoning.

**Common follow-ups:**
- "What happens when the primary DB fails?" → failover, replica promotion, data loss window.
- "How do you keep sessions working with multiple web servers?" → stateless servers + shared store.
- "Your cache just went down and all traffic hit the DB — what happens?" → *cache stampede /
  thundering herd*; mitigate with request coalescing, TTL jitter, and a warm standby.

---

## Self-check

1. Why must web servers be stateless to scale horizontally? Where does session state go instead?
2. Replication scales reads. What scales writes, and why can't replication do it?
3. A user updates their profile, then immediately reloads and sees the *old* data. Which design
   choice caused this, and is it a bug or a trade-off?
4. Give one concrete example of work that belongs on a queue instead of in the request path.

---

## Practice

There's no standalone coding assignment for this overview lesson — it's the map for everything
that follows. Your assignment is the **first building block** it points to: head to
**[Module 2.3 — Caching](../02-building-blocks/03-caching.md)** and you'll build an **LRU cache**
in Go, the exact structure that powers Step 4 above.

**Next:** [0.2 — Numbers Every Engineer Should Know »](02-numbers-every-engineer-should-know.md)
