# 2.2 — Reverse Proxies & CDNs

> **Module 2 · Building Blocks** · ~22 min read
> *The load balancer from the last lesson was secretly doing more than balancing — it was acting
> as a* reverse proxy*. Here we name that role, give it more jobs, and then push it to the edge of
> the planet as a CDN.*

---

## The problem

Your web servers shouldn't have to do everything themselves. Decrypting TLS, compressing every
response, blocking malicious traffic, serving the same logo a million times — that's a lot of
repetitive, expensive work bolted onto your application code. And your users in Sydney shouldn't
wait for that logo to cross an ocean from your server in Virginia, *every single time*.

We need (1) a smart layer **in front of** the servers that handles the cross-cutting chores, and
(2) a way to keep popular content **physically close** to users. Those are the **reverse proxy**
and the **CDN**.

> **Analogy.** A reverse proxy is the **front desk** of an office building. Visitors never wander
> into individual offices — they talk to the desk, which checks IDs (security), takes coats (TLS),
> answers FAQs from a binder so it doesn't bother the staff (caching), and routes you to the right
> person. A CDN is like putting a **copy of that front desk and its FAQ binder in every city**, so
> visitors get answers locally instead of flying to headquarters.

---

## Forward proxy vs reverse proxy

Both are "proxies" (middlemen), but they sit on opposite ends and serve opposite parties.

**Forward proxy** sits in front of **clients** and represents *them* to the internet. Your
corporate network's web filter, or a VPN, is a forward proxy: it makes requests *on your behalf*,
hiding *you* from the server.

**Reverse proxy** sits in front of **servers** and represents *them* to the world. Clients think
they're talking to the real server, but they're talking to the proxy, which hides the servers
behind it.

```
   Forward proxy (hides the client):
      [ Client ] ──> [ Forward Proxy ] ──> [ any website ]
                       ^ acts for the client

   Reverse proxy (hides the servers):
      [ Client ] ──> [ Reverse Proxy ] ──> [ your server 1 ]
                       ^ acts for the servers └─> [ your server 2 ]
```

| | Forward proxy | Reverse proxy |
|---|---------------|----------------|
| Sits in front of | Clients | Servers |
| Hides | The client from the server | The servers from the client |
| Typical use | Filtering, anonymity, caching for users | LB, TLS, caching, security for your app |

The rest of this lesson is about **reverse** proxies — that's the building block.

---

## What a reverse proxy actually does

The load balancer was one job; a reverse proxy bundles several cross-cutting jobs into one tier so
your application servers stay lean.

```
                         ┌──────────── Reverse Proxy ───────────┐
   Client ──HTTPS──────► │  TLS termination                     │ ──HTTP──► [ App Server ]
                         │  caching · compression · security    │           [ App Server ]
                         │  load balancing · routing            │
                         └──────────────────────────────────────┘
```

- **TLS termination.** The proxy decrypts incoming HTTPS once, then talks plain (or re-encrypted)
  HTTP to the backends. Your app servers don't each manage certificates and crypto. Centralizing
  certs here is a huge operational win. (Trade-off: traffic *inside* the trust boundary may be
  unencrypted unless you re-encrypt — "TLS passthrough" or mTLS if you need end-to-end.)
- **Caching.** Store responses to common requests and serve them without bothering the backend at
  all. (This is the seed of the CDN idea below, and the whole of [Module 2.3](03-caching.md).)
- **Compression.** Gzip/Brotli responses so less data crosses the wire — done once at the proxy.
- **Security.** A natural chokepoint to add TLS, hide backend IPs, terminate sloppy connections,
  rate-limit ([Module 2.11](11-rate-limiting.md)), and run a **Web Application Firewall (WAF)**
  to block common attacks.
- **Load balancing & routing.** Everything from [Module 2.1](01-load-balancing.md) — an L7
  reverse proxy *is* the L7 load balancer.

The theme: **pull cross-cutting concerns out of the app and into a shared edge layer.** Your
application code shrinks to "business logic," and the boring-but-critical plumbing lives in one
well-understood place.

---

## From reverse proxy to CDN

A reverse proxy that caches is great — but it's still in **one** data center. A user across the
world still pays the round-trip latency to reach it. The fix is to put caching reverse proxies in
**many** locations worldwide. That's a **Content Delivery Network (CDN)**.

A CDN is a globally distributed fleet of **edge servers** (also called PoPs — Points of Presence)
that cache your content close to users. The user's request is routed (via DNS/anycast, from
[Module 2.1](01-load-balancing.md)) to the **nearest edge**. The edge either serves a cached copy
instantly or fetches it from your **origin** server once and caches it for everyone after.

```
   User (Sydney) ──> [ Edge: Sydney ] ──HIT──> instant ✅
                                     └──MISS──> [ Origin: Virginia ] (first time only)
   User (Berlin) ──> [ Edge: Berlin ] ──HIT──> instant ✅
```

Two big wins, same as in [Module 0.5](../00-foundations/01-scale-zero-to-millions.md):
**lower latency** (content is nearby) and **origin offload** (your servers serve the *first*
request per edge, not the millionth).

### Push vs pull CDNs

How does content *get* to the edges?

- **Pull CDN (lazy).** You do nothing special. The first user to request a file at an edge
  triggers a **cache miss**; the edge pulls it from your origin and caches it. Subsequent users
  hit the cache. Easiest to operate (the default for most sites); downside is the first user per
  edge pays the slow path, and a sudden spike of *different* files can hammer the origin.
- **Push CDN (eager).** You proactively upload content to the CDN ahead of time. Best for large,
  predictable assets (a video release, a software download) where you don't want *any* user to
  pay the miss penalty. More work: you manage what's pushed and when it expires.

```
   Pull: origin is the source of truth; edges fetch on first miss. ("cache on demand")
   Push: you upload to the CDN up front; edges already hold it.    ("pre-warm")
```

| | Pull CDN | Push CDN |
|---|----------|----------|
| Who populates the cache | The first request (lazy) | You, in advance (eager) |
| Operational effort | Low (set and forget) | Higher (manage uploads/TTLs) |
| First-request latency | Slow (miss) | Fast (already there) |
| Best for | General websites, lots of files | Large, known, infrequently-changing assets |

---

## Controlling the cache: Cache-Control & TTL

The edge needs to know **how long** it may serve a cached copy. That's the **TTL (time to live)**,
driven mostly by the HTTP `Cache-Control` header your origin sends:

```
   Cache-Control: public, max-age=86400      ← cache this for 1 day at the edge
   Cache-Control: no-store                    ← never cache (e.g. personal/private data)
   Cache-Control: private, max-age=0          ← only the user's browser may cache, not shared edges
```

- A **longer TTL** → higher cache-hit ratio, less origin load, but staler content.
- A **shorter TTL** → fresher content, but more origin traffic.
- That tension — *freshness vs hit rate* — is the central caching trade-off, explored fully in
  [Module 2.3](03-caching.md).

A common pattern for assets that change: **content-hashed filenames** like `app.9f3a1c.js`. The
file gets a near-infinite TTL (it never changes), and when you deploy a new version you ship a new
*filename*. No invalidation needed — the HTML simply points to the new name.

---

## Cache invalidation: making the edge forget

Sometimes you must update content *before* its TTL expires (you fixed a typo on the homepage,
pulled a bad image). Options, from blunt to surgical:

- **Wait for TTL** — do nothing; it expires on its own. Fine if TTLs are short.
- **Purge / invalidate** — tell the CDN "drop this URL (or this tag) now." Most CDNs offer an API.
  Surgical but can be slow to propagate across all edges, and aggressive purging spikes origin
  load (every edge re-fetches at once — a mini stampede; see [Module 2.3](03-caching.md)).
- **Versioned URLs** — change the filename (the content-hash trick above) so old caches are
  simply never asked for again. The cleanest approach when you control the asset references.

> "There are only two hard things in computer science: cache invalidation and naming things."
> The joke is famous because invalidation genuinely *is* hard — you can't cheaply prove every
> copy everywhere is fresh.

---

## Cache hit ratio: the number that matters

The headline metric for any cache, edge or otherwise:

```
   cache hit ratio = cache hits / (cache hits + cache misses)
```

A 95% hit ratio means only 5% of requests ever reach your origin. Pushing it from 90% → 95%
**halves** your origin traffic. You raise it with longer TTLs, smart key design, and not caching
things that are inherently personal (which can never be shared). It's the first thing to look at
when judging whether a CDN/cache is "working."

---

## When does a CDN actually help?

CDNs are not free or universally beneficial — match the tool to the workload.

- ✅ **Static assets** (images, CSS, JS, fonts, video) — the canonical win.
- ✅ **Cacheable, read-heavy, geographically spread** traffic where the same content serves many.
- ✅ **Large media / downloads** — offload bandwidth and serve nearby.
- ⚠️ **Highly personalized, per-user, or rapidly-changing** responses — low hit ratio, little
  benefit; you may even add a hop for nothing.
- ⚠️ **Write-heavy APIs** — caching a POST result makes no sense.

The deciding question is always **"is this content shared and cacheable?"** If many users want the
same bytes and those bytes don't change every second, a CDN shines.

---

## Trade-offs & key takeaways

- **Forward proxy hides the client; reverse proxy hides the servers.** The building block is the
  reverse proxy.
- **A reverse proxy centralizes cross-cutting concerns** — TLS, caching, compression, security,
  load balancing — so app servers stay lean.
- **A CDN is a planet-scale caching reverse proxy.** Pull (lazy, easy) vs push (eager, for big
  known assets).
- **TTL is the master dial:** longer = higher hit ratio + staler; shorter = fresher + more origin
  load.
- **Invalidation is hard;** prefer versioned URLs over purging when you can.
- **Watch the cache hit ratio** — it directly measures origin offload.
- **CDNs help shared, cacheable content;** they do little for personalized, write-heavy traffic.

---

## In the wild

- **Cloudflare, Akamai, Fastly, AWS CloudFront, Google Cloud CDN** are major CDNs, all
  pull-capable with optional push/pre-warm and global anycast.
- **NGINX, HAProxy, Envoy, Apache Traffic Server** are common self-hosted reverse proxies.
- **Content-hashed asset filenames** (`main.[hash].js`) are standard output of every modern
  frontend build tool, precisely to make CDN caching safe and invalidation-free.

---

## Interview angle

When the design has global users and static/media content, **reach for a CDN** and justify it
with *latency + origin offload*. Be ready to explain **pull vs push**, set a sensible **TTL**, and
describe **invalidation via versioned URLs**. Mention **TLS termination at the reverse proxy** as
the place certificates live. The senior move: state the **freshness vs hit-ratio** trade-off and
note that personalized content is a poor CDN fit.

**Common follow-ups:**
- "User in another continent — how do you cut latency?" → CDN edge + DNS/anycast routing.
- "You shipped a bad homepage; how do you fix it everywhere fast?" → purge/invalidate vs short
  TTL vs versioned URL, and the propagation/stampede caveat.
- "What does your reverse proxy do besides balance load?" → TLS, caching, compression, WAF.
- "Forward vs reverse proxy?" → who's hidden: client vs servers.

---

## Self-check

1. In one sentence each, distinguish a forward proxy from a reverse proxy by *who they hide*.
2. Your blog's images are served by a pull CDN. Why is the *very first* visitor in a new region
   slower than the rest, and how would a push CDN change that?
3. You set `Cache-Control: max-age=86400` and then discover a typo on the page. Name two ways to
   correct it everywhere, and the downside of each.
4. Your cache hit ratio drops from 95% to 90%. Roughly what happens to origin traffic, and why is
   that worse than it sounds?
5. Give one type of content where adding a CDN would help almost nothing. Why?

---

**Next:** [2.3 — Caching (strategies, eviction, pitfalls) »](03-caching.md)
