# 1.4 — GraphQL vs REST

> **Module 1 · Networking & Communication** · ~26 min read
> *REST hands you fixed responses from many endpoints. GraphQL flips it: one endpoint, and the
> client asks for exactly the data it wants — no more, no less. Powerful, but it moves the
> hard problems somewhere new.*

---

## The problem

You're building a mobile profile screen. It needs a user's **name**, their **last 3 posts'
titles**, and their **follower count**. With REST you might do:

```
GET /users/42            → name, email, bio, settings, ... (lots you don't need)
GET /users/42/posts      → ALL posts with full bodies (you wanted 3 titles!)
GET /users/42/followers  → full follower objects (you wanted a count!)
```

Three round trips, and each returns *way more than you asked for*. Two classic pains:

- **Over-fetching** — the server sends fields you don't need (wasting bandwidth, painful on
  mobile networks).
- **Under-fetching** — one endpoint doesn't have everything, so you make *more* requests (the
  dreaded "N+1 round trips" to build one screen).

> **Analogy.** REST is a **fixed-menu restaurant**: each dish (endpoint) comes plated a certain
> way. Want the burger without onions, plus a side of fries from another dish? You order multiple
> dishes and leave food on the plate. GraphQL is a **build-your-own bowl**: you tell the kitchen
> exactly which ingredients you want, in one order, and get back precisely that.

---

## Core idea: one endpoint, client-specified queries

**GraphQL** is a query language for APIs. Instead of many endpoints with fixed shapes, there's
**one endpoint** (usually `POST /graphql`), and the **client sends a query describing exactly the
shape of data it wants.** The server returns a JSON response matching that shape — nothing extra,
nothing missing.

The profile screen above becomes **one request**:

```graphql
query {
  user(id: 42) {
    name
    posts(last: 3) { title }
    followerCount
  }
}
```

Response — same shape as the query, exactly the requested fields:

```json
{
  "data": {
    "user": {
      "name": "Ada",
      "posts": [{ "title": "Hello" }, { "title": "Notes" }, { "title": "More" }],
      "followerCount": 1280
    }
  }
}
```

One round trip. No over-fetch, no under-fetch. The client is in control of the response shape.

---

## How it works: schema + resolvers

GraphQL has two halves.

### The schema — a typed contract

You define a **schema**: the types, fields, and relationships your API exposes. It's strongly
typed and self-documenting — clients can introspect it to learn exactly what's available.

```graphql
type User {
  id: ID!
  name: String!
  posts(last: Int): [Post!]!
  followerCount: Int!
}

type Post {
  id: ID!
  title: String!
  body: String!
}

type Query {            # the entry points
  user(id: ID!): User
}
```

### Resolvers — functions that fetch each field

For every field, the server has a **resolver**: a function that knows how to fetch *that* piece
of data (from a DB, a cache, another service). GraphQL walks the query and calls the resolvers
needed to fill the requested shape.

```
   query { user(id:42) { name  posts { title } } }
              │             │      │
              ▼             ▼      ▼
        userResolver   (from User)  postsResolver  ── DB / service calls
```

This is the elegant part — *and* the source of GraphQL's signature performance trap, next.

---

## The N+1 problem (and DataLoader)

Consider asking for a list of users **and each user's posts**:

```graphql
query { users(first: 100) { name  posts { title } } }
```

Naively, the resolvers run:

```
1 query   → fetch 100 users
+100 queries → for each user, fetch their posts  (one DB hit per user!)
= 101 queries for ONE GraphQL request.   ← the "N+1 problem"
```

That's **N+1**: 1 query for the list, then N more (one per item). It quietly murders your
database under load.

### The fix: DataLoader (batching + caching)

A **DataLoader** sits between resolvers and your data source. Instead of firing a query per
item, it **collects all the IDs requested in one tick of the event loop and fetches them in a
single batched query.** It also **caches** within a request so the same ID isn't fetched twice.

```
  Without DataLoader:  postsForUser(1), postsForUser(2), ... → 100 queries
  With DataLoader:     batch → postsForUsers([1,2,...,100])  → 1 query
```

So 101 queries collapse to **2** (users + a single batched posts query). N+1 isn't unique to
GraphQL, but GraphQL's flexible nesting makes it *especially* easy to trip into — so batching is
essentially mandatory.

---

## Caching: GraphQL's real weak spot

Here's a genuine trade-off where **REST wins**. REST leans on the entire HTTP caching ecosystem:
a `GET /users/42` has a stable URL, so browsers, CDNs, and proxies can cache it for free with
`Cache-Control` and `ETag` (recall [HTTP caching headers](02-http-https-rest.md)).

GraphQL breaks that model:

- Everything is **`POST` to one URL** (`/graphql`), and `POST` isn't cached by HTTP infrastructure.
- Every query can be a **different shape**, so there's no stable cacheable response per URL.

You get caching back only with extra work: client-side **normalized caches** (Apollo, Relay) that
cache by object ID, **persisted queries** (hashing known queries so they can be `GET`-cached), or
field-level server caching. **REST gives you HTTP/CDN caching for free; GraphQL makes you build
it.** State this in interviews.

---

## GraphQL vs REST

| Dimension | REST | GraphQL |
|-----------|------|---------|
| **Endpoints** | many (one per resource) | **one** (`/graphql`) |
| **Response shape** | fixed by server | **chosen by client** |
| **Over/under-fetching** | common | solved by design |
| **Round trips for one screen** | often several | usually **one** |
| **HTTP/CDN caching** | **free & easy** | hard (POST, varying shapes) |
| **Learning curve / setup** | low | higher (schema, resolvers, loaders) |
| **N+1 risk** | exists | **easy to trip into** (use DataLoader) |
| **File uploads, simple CRUD** | natural | clunkier |
| **Versioning** | versioned URLs | evolve schema (deprecate fields) |
| **Best fit** | simple/public APIs, CDN-cacheable | **rich clients, many data shapes** |

---

## When NOT to use GraphQL

GraphQL is a tool, not an upgrade. Reach for **REST** when:

- Your API is **simple CRUD** — GraphQL's machinery is overkill.
- **HTTP/CDN caching matters a lot** (e.g. a public, read-heavy content API). REST's free caching
  is a huge advantage.
- **File uploads/downloads** or binary streaming dominate — REST handles these more naturally.
- Your team is **small and time-constrained** — REST has a far gentler setup.
- You need **simple, predictable rate limiting** — a single flexible GraphQL query can be cheap or
  catastrophically expensive (a deeply nested query can hammer your DB), so you must add **query
  cost analysis / depth limiting**, which is more work than "N requests per minute" on REST
  endpoints (see [Module 2.11 — Rate limiting](../02-building-blocks/11-rate-limiting.md)).

> **Trade-off in a line:** GraphQL trades free HTTP caching and operational simplicity for
> client-driven flexibility and elimination of over/under-fetching. Worth it when many different
> clients need many different data shapes; overkill for a simple, cacheable API.

---

## Trade-offs & key takeaways

- **GraphQL = one endpoint + client-specified queries**, solving over- and under-fetching by
  design. Great for rich UIs (mobile, dashboards) that assemble varied data.
- **Schema + resolvers** are the two halves: a typed contract and per-field fetch functions.
- **The N+1 problem is GraphQL's signature trap** — nested resolvers fan out into per-item
  queries. **DataLoader** (batch + per-request cache) is the standard fix.
- **Caching is the real cost.** REST gets HTTP/CDN caching for free; GraphQL makes you rebuild it.
- **Rate limiting is harder** because query cost varies wildly — add depth/cost limits.
- It is **not** strictly better than REST. Choose by client needs and caching requirements.

---

## In the wild

- **GitHub** offers a full **GraphQL API** alongside its REST one — a great real comparison.
- **Meta** created GraphQL to serve its mobile apps' many data shapes efficiently over slow
  networks (the original over/under-fetch motivation).
- **Shopify, Netflix, Airbnb** use GraphQL to let varied clients request tailored data; Netflix's
  federated graph stitches many services behind one schema.
- **Apollo** and **Relay** are the dominant client libraries, providing the normalized caches that
  partly close GraphQL's caching gap.

---

## Interview angle

Bring up GraphQL when the design has **multiple clients with different data needs** (web + iOS +
Android) and **over/under-fetching is hurting** — especially on mobile. Show depth by
**volunteering the trade-offs**: the **N+1 problem and DataLoader**, the **caching disadvantage
vs REST**, and **rate-limiting complexity** from variable query cost. The senior move is "GraphQL
fixes over/under-fetching, but I'd weigh it against losing free HTTP/CDN caching and the N+1 risk
— here's how I'd handle each." Knowing when *not* to use it is the differentiator.

**Common follow-ups:**
- "How does GraphQL solve over-fetching?" → client specifies exact fields; server returns only
  those.
- "What's the N+1 problem here, and how do you fix it?" → nested resolvers query per-item;
  DataLoader batches + caches within a request.
- "Why is caching harder with GraphQL than REST?" → single POST endpoint + varying query shapes
  break URL-based HTTP/CDN caching; need normalized client caches or persisted queries.

---

## Self-check

1. Explain over-fetching and under-fetching with a concrete screen, and show how one GraphQL
   query removes both.
2. What are resolvers, and how do they create the N+1 problem? How does DataLoader fix it
   (be specific about batching *and* caching)?
3. Why does REST get CDN caching "for free" while GraphQL doesn't? What can you do to get some
   of it back?
4. Give two situations where you'd pick REST over GraphQL and explain the reasoning.
5. Why is rate limiting trickier for a GraphQL API than for a set of REST endpoints?

---

**Next:** [1.5 — Real-time: polling, SSE & WebSockets »](05-polling-sse-websockets.md)
