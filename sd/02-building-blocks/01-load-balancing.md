# 2.1 — Load Balancing

> **Module 2 · Building Blocks** · ~22 min read
> *In [Module 0.2](../00-foundations/01-scale-zero-to-millions.md) we put a box called "load
> balancer" in front of our web servers and moved on. Time to open that box. It's the traffic
> cop that makes horizontal scaling — and high availability — actually work.*

---

## The problem

You went horizontal: instead of one web server you now run three identical ones. Great — but
your users only know **one** address, `myapp.com`. Something has to stand at the front door and
decide, for each incoming request, *which* of the three servers handles it. And it has to do
that while:

- spreading work **evenly** (so one server doesn't melt while two sit idle),
- **noticing** when a server dies and routing around it,
- and never becoming a single thing whose failure takes the whole site down.

That something is a **load balancer (LB)**.

> **Analogy.** Picture a bank with one queue and five tellers. A good lobby has a host directing
> "next person, to teller 3." The host (load balancer) keeps every teller busy, skips the teller
> who stepped out for coffee (health check), and if the host themselves calls in sick, there's a
> backup host ready (no single point of failure). Without the host, everyone piles onto whichever
> teller is nearest the door.

---

## Core idea

A load balancer is a server (or service) that accepts client connections and **forwards** them
to one of several backend servers (the "pool" or "backend set"), then relays the response back.
Clients see only the LB's public address; the backends live on private addresses behind it.

```
                       ┌─> [ Server 1 ]
   Clients ──> [ LB ] ─┼─> [ Server 2 ]
                       └─> [ Server 3 ]
```

Two questions define everything an LB does:
1. **How does it pick a backend?** → *the balancing algorithm.*
2. **How much of the request does it understand to pick well?** → *L4 vs L7.*

---

## L4 vs L7: how deep does the LB look?

These names come from the OSI network layers. You don't need OSI memorized — just the contrast.

**L4 (transport-layer) load balancing** works at the level of **TCP/UDP connections**. It sees
IP addresses and ports, *not* the actual content. It decides where to send a connection and then
just shovels bytes back and forth without reading them.

- **Fast and cheap** — almost no parsing; it's basically smart packet forwarding.
- **Protocol-agnostic** — works for anything over TCP/UDP (databases, custom protocols, gRPC).
- **Blind** — it can't route based on URL, cookies, or headers because it never reads them.

**L7 (application-layer) load balancing** works at the level of **HTTP requests**. It terminates
the connection, reads the request, and can route on **URL path, hostname, headers, cookies**.

- **Smart routing** — send `/api/*` to the API pool, `/images/*` to the image pool.
- **Can do more** — TLS termination, caching, compression, rewriting (this blends into the
  reverse-proxy duties in [Module 2.2](02-reverse-proxy-cdn.md)).
- **More work per request** — it parses HTTP, so it's heavier than L4.

```
   L4: "Connection from 1.2.3.4:55001 → pick a backend → forward bytes." (content unseen)

   L7: "GET /api/orders, Host: shop.com, Cookie: sid=abc
        → /api/* goes to the API pool → forward to api-server-2." (content understood)
```

| | **L4** | **L7** |
|---|--------|--------|
| Operates on | TCP/UDP connections | HTTP requests |
| Can route by | IP, port | URL, host, headers, cookies |
| Speed | Faster (less to parse) | Slower (parses HTTP) |
| TLS termination | No (passes encrypted bytes through) | Yes |
| Use when | Raw throughput, non-HTTP protocols | Smart HTTP routing, web apps |

In practice, large systems use **both**: an L4 LB at the very edge for raw throughput, feeding L7
LBs that do the clever HTTP routing.

---

## The balancing algorithms

How does the LB choose *which* healthy backend gets the next request? The classics:

### Round-robin
Hand requests out in rotation: 1, 2, 3, 1, 2, 3… Dead simple. Works great when servers are
identical and requests cost about the same. **Weakness:** it's blind to how busy each server
actually is — a server stuck on a slow request still gets its turn.

### Weighted round-robin
Give beefier servers a bigger share. A server with weight 3 gets 3 requests for every 1 the
weight-1 server gets. Useful with a **mixed fleet** (some big boxes, some small).

### Least-connections
Send the next request to the server with the **fewest active connections** right now. Adapts to
reality: a server bogged down by long requests naturally receives fewer new ones. Better than
round-robin when request durations vary a lot.

### IP hash / consistent hash
Compute a hash of the client's IP (or some key) and use it to pick a backend, so **the same
client keeps landing on the same server**. This gives "stickiness" without storing state on the
LB. Plain hashing breaks badly when you add/remove servers (almost every client gets reshuffled)
— the fix is **consistent hashing**, a whole topic in
[Module 2.8](08-consistent-hashing.md).

```
   Round-robin:        next = (next + 1) % N           ← simple, blind
   Weighted RR:        bigger servers appear more often ← mixed fleet
   Least-connections:  pick min(active_conns)           ← adapts to load
   IP / consistent hash: backend = hash(key) → server   ← stickiness, cache locality
```

| Algorithm | Picks based on | Best when | Watch out for |
|-----------|----------------|-----------|---------------|
| Round-robin | Rotation | Uniform servers & requests | Ignores actual load |
| Weighted RR | Configured weights | Mixed-capacity fleet | Weights drift from reality |
| Least-connections | Live connection count | Variable request durations | Needs accurate conn tracking |
| IP / consistent hash | Hash of a key | Stickiness, cache locality | Rebalancing on fleet changes |

---

## Health checks: routing around the dead

An LB is only as good as its picture of which backends are alive. It runs **health checks** —
periodic probes — and only routes to backends that pass.

- **Passive (L4-ish):** "can I open a TCP connection to port 8080?" Cheap, but a process can
  accept connections while being totally broken internally.
- **Active (L7):** "send `GET /healthz`, expect `200 OK` within 500 ms." Much more honest —
  your `/healthz` handler can check the DB connection, disk, etc.

```
   LB ──GET /healthz──> Server 2 ──> 200 OK   ✅ keep in pool
   LB ──GET /healthz──> Server 3 ──> timeout  ❌ remove from pool, recheck later
```

Tuning matters: probe too rarely and you send traffic to a dead server for too long; probe too
aggressively (or with a too-tight timeout) and a briefly-slow server gets yanked out, making the
overload *worse*. This is a real trade-off, not a "set and forget."

---

## Stateful vs stateless: the sticky-session question

Remember from [Module 0](../00-foundations/01-scale-zero-to-millions.md): if a server stores your
login session **in its own memory**, a later request routed to a *different* server thinks you're
logged out. Two ways to deal with this:

**Sticky sessions (session affinity).** The LB pins each client to one backend (via a cookie or
IP hash) so they always hit the server holding their state.
- ✅ Simple; no shared store needed.
- ❌ **Uneven load** (some servers get the "heavy" users), and if that server dies, those users
  lose their session entirely. Adding/removing servers disrupts affinity.

**Stateless servers + shared store.** Servers keep *no* per-user state; sessions live in a shared
cache/DB (e.g. Redis). Any server can handle any request.
- ✅ Even balancing, painless failover, trivial scaling.
- ❌ A network hop to fetch session state (cheap, and cacheable).

> **The course's recurring advice:** prefer **stateless** servers. Stickiness is an
> escape hatch, not a default. Stateless is what makes horizontal scaling and failover clean.

| | Sticky sessions | Stateless + shared store |
|---|-----------------|--------------------------|
| Where state lives | On one backend's memory | In a shared cache/DB |
| Load distribution | Can be uneven | Even |
| Server failure | Affected users lose session | Any server picks up |
| Scaling | Disrupts affinity | Add/remove freely |

---

## Redundancy: don't make the LB the single point of failure

We added the LB to remove the single point of failure (SPOF) on the web tier — but now the LB
*itself* is one. If it dies, the whole site is unreachable. So you run **more than one LB**.

**Active-passive.** One LB serves all traffic; a standby watches it. If the active one fails, the
passive one takes over its IP (via a "floating"/virtual IP and a heartbeat). Simple, but the
standby sits idle burning money.

```
   Active-passive:
      Clients ──> [ LB-A (active) ]   <─heartbeat─>   [ LB-B (passive, idle) ]
                       │                                  ↑ takes over the VIP if A dies
                  (backends)
```

**Active-active.** Both LBs serve traffic at the same time (clients are spread across them, often
via DNS). On failure, the survivor absorbs the load. Better utilization, but you must ensure
either LB can handle the *full* load when alone.

```
   Active-active:
      Clients ─┬─> [ LB-A (active) ] ─┐
               └─> [ LB-B (active) ] ─┴─> (shared backend pool)
```

| | Active-passive | Active-active |
|---|----------------|----------------|
| Standby utilization | Idle (wasted) | Both working |
| Failover | Promote standby | Survivor absorbs load |
| Capacity planning | 1× needed | Each must handle full load alone |
| Complexity | Lower | Higher |

---

## Going global: GSLB (DNS & anycast)

Everything so far balances within **one** data center. But your users are worldwide and you run
servers in several regions. **Global Server Load Balancing (GSLB)** routes a user to the *best
region* — usually the nearest healthy one — *before* a regional LB takes over.

Two common mechanisms:

- **DNS-based.** When a user resolves `myapp.com`, the DNS service returns the IP of the closest
  healthy region. Simple and widely used. **Catch:** DNS answers are *cached* (by resolvers and
  browsers) for the TTL, so failover isn't instant — a dead region can keep getting traffic until
  caches expire.
- **Anycast.** The *same* IP is announced from many locations; the internet's routing (BGP)
  naturally sends each user to the nearest one. Failover is fast (routing reconverges), but it's
  operationally heavier and typically the domain of CDNs and big providers.

```
   User (Tokyo)   ──DNS──> "use 1.2.3.4 (asia region)" ──> [ Asia LB ] ──> Asia servers
   User (Berlin)  ──DNS──> "use 5.6.7.8 (eu region)"   ──> [ EU LB ]   ──> EU servers
```

We'll meet anycast again with CDNs in [Module 2.2](02-reverse-proxy-cdn.md).

---

## Trade-offs & key takeaways

- **L4 = fast & blind; L7 = smart & heavier.** Most stacks use both, L4 at the edge feeding L7.
- **No algorithm is "best."** Round-robin for uniform fleets; least-connections when request
  durations vary; hashing when you need stickiness or cache locality.
- **Health checks are the safety net** — and they're a tuning trade-off, not a toggle.
- **Stateless beats sticky.** Push session state into a shared store; keep servers interchangeable.
- **The LB must not be a SPOF.** Run redundant LBs (active-passive or active-active).
- **Globally, DNS/anycast pick the region first**, then a regional LB picks the server.

---

## In the wild

- **HAProxy** and **NGINX** are the classic software LBs (NGINX is also a reverse proxy/CDN
  origin shield — see the next lesson). Both can do L4 and L7.
- **AWS** splits them: **Network Load Balancer (NLB)** = L4, **Application Load Balancer (ALB)**
  = L7. **Route 53** and **Cloudflare** provide DNS-based GSLB; **Google Cloud** and Cloudflare
  use **anycast** heavily.
- **Envoy** is a modern L7 proxy that powers service meshes.

---

## Interview angle

When asked "how do requests reach your servers," walk it top-down: **DNS/GSLB picks a region →
edge L4 LB → L7 LB routes by path → backend pool**, with **health checks** removing dead nodes
and **redundant LBs** so the LB isn't a SPOF. Then volunteer the **stateless-vs-sticky** trade-off
unprompted — that's a senior signal. If they push on algorithms, contrast round-robin with
least-connections and explain *why* you'd pick each.

**Common follow-ups:**
- "Your LB itself fails — then what?" → active-passive vs active-active, floating/virtual IP.
- "How does a user in Tokyo reach the closest region?" → DNS-based GSLB or anycast, and the DNS
  **TTL caching** delay on failover.
- "Sessions break when I add a server — why?" → in-memory session state + round-robin; fix with
  stateless servers + shared store (or sticky sessions as a stopgap).
- "Why does adding a server reshuffle all your hash-routed clients?" → modulo hashing; lead into
  **consistent hashing**.

---

## Self-check

1. You serve only gRPC traffic and need maximum throughput. L4 or L7, and why?
2. Two servers are identical, but some requests take 10 ms and some take 10 s. Which algorithm
   balances them better — round-robin or least-connections? Why?
3. Why does the course recommend stateless servers over sticky sessions? Name the one thing
   sticky sessions still make simpler.
4. After we add a redundant LB, what's the new failure mode we must plan for, and how does
   active-active differ from active-passive in handling it?
5. A region goes down but users keep hitting it for a minute. What caused the delay, and which
   GSLB mechanism reduces it?

---

**Next:** [2.2 — Reverse Proxies & CDNs »](02-reverse-proxy-cdn.md)
