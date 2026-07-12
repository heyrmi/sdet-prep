# 1.6 — API Gateways & API Design

> **Module 1 · Networking & Communication** · ~28 min read
> *You've split your app into many services, each speaking REST or gRPC. But clients shouldn't
> need to know about all of them — and you shouldn't re-implement auth and rate limiting in every
> one. An **API gateway** is the single, smart front door. This lesson also distills the API
> design principles running through this whole module.*

---

## The problem

Your system has grown into a dozen microservices: users, orders, payments, search, notifications.
A mobile app needs data from five of them. Without a front door, the client must:

- know the address of every service,
- authenticate against each one,
- handle each service's rate limits, TLS, retries,
- and stitch together five responses itself.

And **every service** ends up re-implementing the same cross-cutting concerns — auth, rate
limiting, logging, TLS. That's duplicated, error-prone, and brittle. We need **one place** to
handle all of it.

> **Analogy.** A big office building has **one staffed reception desk**. Visitors don't wander
> the halls hunting for the right office. Reception checks your ID (**auth**), enforces the
> visitor cap (**rate limiting**), directs you to the correct floor (**routing**), and can even
> gather paperwork from several departments so you get one packet (**aggregation**). The API
> gateway is that reception desk for your services.

---

## Core idea: one smart front door

An **API gateway** is a single entry point that sits between clients and your backend services.
Every external request hits the gateway first; it handles the cross-cutting work, then forwards
the request to the right internal service (often translating **REST at the edge to gRPC inside**,
the split from [Lesson 1.3](03-rpc-grpc.md)).

```
                              ┌──► [ User service ]   (gRPC)
   Clients ──► [ API Gateway ]├──► [ Order service ]  (gRPC)
   (REST/HTTPS)   │           ├──► [ Payment service ]
                  │           └──► [ Search service ]
                  └─ auth, rate limit, TLS, routing, logging done HERE, once
```

The win: services stay **focused on business logic** while the gateway owns the shared concerns —
implemented once, consistently.

---

## What an API gateway does

| Responsibility | What it means |
|----------------|---------------|
| **Routing** | Map an incoming path (`/orders/*`) to the right backend service |
| **Authentication / authZ** | Verify the token/API key once, before traffic reaches services |
| **Rate limiting** | Enforce per-client quotas at the edge ([Module 2.11](../02-building-blocks/11-rate-limiting.md)) |
| **TLS termination** | Decrypt HTTPS once at the gateway; talk plain/internal-TLS behind it |
| **Aggregation** | Fan out to several services and combine results into one response |
| **Protocol translation** | REST/JSON outside ↔ gRPC/protobuf inside |
| **Caching** | Cache common responses at the edge |
| **Observability** | One place to log, trace, and meter every request |

### A closer look: TLS termination

**TLS termination** means the gateway is where HTTPS is decrypted. Clients connect over HTTPS;
the gateway does the (costly) TLS handshake from [Lesson 1.2](02-http-https-rest.md) once, then
forwards requests to internal services over the (trusted) internal network. This centralizes
certificate management and offloads crypto from every service.

### A closer look: aggregation

Instead of the client making five calls, it makes **one** to the gateway, which fans out
internally and merges the results:

```
   Client ──► [ Gateway ] ─┬─► User service ──► {name}
   (1 request)             ├─► Order service ─► {orders}
                           └─► Stats service ─► {count}
                                  │
                           merge → {name, orders, count}  ──► back to client (1 response)
```

This reduces client round trips (echoing the over/under-fetching pain from
[GraphQL, 1.4](04-graphql.md) — a gateway is one way to address under-fetching without adopting
GraphQL).

---

## The BFF pattern (Backend for Frontend)

Different clients want **different shapes** of data. A mobile app on a slow network wants small,
trimmed responses; a desktop web app wants richer ones. A single one-size-fits-all gateway forces
compromises.

The **BFF (Backend for Frontend)** pattern gives **each frontend its own gateway**, tailored to
that client's needs:

```
   Mobile app  ──► [ Mobile BFF ]  ─┐
                                    ├──► shared backend services
   Web app     ──► [ Web BFF ]     ─┘
   (each BFF shapes/aggregates data for its own client)
```

- **Pros:** each client gets exactly the data and shape it needs; teams own their BFF and move
  independently; no over-fetching on constrained clients.
- **Cons:** more gateways to build and maintain; risk of duplicated logic across BFFs.

> **Trade-off:** one gateway is simpler to operate; BFFs give per-client optimization at the cost
> of more moving parts. Choose BFFs when clients diverge enough that a shared gateway becomes a
> compromise nobody likes.

---

## Good API design principles

Whether REST or gRPC, behind a gateway or not, well-designed APIs share these traits — a
distillation of this whole module.

### 1) Clear, consistent naming
- **Nouns for resources, plural collections:** `/users`, `/users/42/orders`.
- **Verbs are the HTTP methods,** not the path: `DELETE /users/42`, not `POST /deleteUser`.
- Consistency across the whole API beats local cleverness.

### 2) Meaningful errors
Return the right **status code** (the 4xx-vs-5xx contract from [1.2](02-http-https-rest.md)) plus
a structured, actionable body:

```json
{
  "error": {
    "code": "INSUFFICIENT_FUNDS",
    "message": "Balance 5.00 is below the 12.50 charge.",
    "request_id": "req_8f3a"   // so support can trace it
  }
}
```

A `request_id` that also appears in your logs/traces is a small touch that saves hours of
debugging.

### 3) Idempotency keys for writes
Recall: networks retry, and `POST` isn't idempotent ([1.2](02-http-https-rest.md)). For unsafe
operations like payments, let the client send an **idempotency key**:

```
POST /payments
Idempotency-Key: a1b2c3-unique-per-attempt
```

The server records the key with the result; a retried request with the **same key** returns the
**original result instead of charging again.** This is *the* pattern for safe retries — we go deep
in [Module 2.14 — Idempotency](../02-building-blocks/14-idempotency.md).

### 4) Pagination
Never dump unbounded lists. Default to **cursor-based** pagination for large/live data; offset is
fine for small, stable sets (the trade-off table is in [1.2](02-http-https-rest.md)).

### 5) Versioning
Evolve without breaking existing clients: `/v1/...` → `/v2/...` (path) or via headers. Pick one
strategy and apply it consistently.

---

## Forward vs reverse proxy (brief)

An API gateway is a specialized **reverse proxy**, so it's worth pinning down the two kinds of
proxy — they sit on **opposite ends** of the conversation.

- **Forward proxy** — sits in front of **clients**, acting on their behalf to reach the internet.
  The *server* doesn't know the real client. (Think: a corporate web filter, a VPN.)
- **Reverse proxy** — sits in front of **servers**, acting on their behalf toward clients. The
  *client* doesn't know which backend actually served it. (Think: NGINX, a load balancer, an API
  gateway.)

```
   Forward proxy:   [ Clients ] ──► (proxy) ──► Internet/servers      (hides the client)

   Reverse proxy:   Clients ──► (proxy) ──► [ Your servers ]          (hides the servers)
```

An API gateway is a reverse proxy *with brains* — it adds auth, rate limiting, aggregation, and
routing on top of plain proxying. Load balancers, CDNs, and TLS terminators are all reverse-proxy
relatives. We dig into reverse proxies, CDNs, and load balancing next, in Module 2.

---

## Trade-offs & key takeaways

- An **API gateway is the single smart front door**: it centralizes routing, auth, rate limiting,
  TLS termination, aggregation, and observability so services don't each reinvent them.
- **Centralization is the win and the risk:** consistent cross-cutting logic in one place, but the
  gateway becomes a critical component — make it highly available (it's a potential single point
  of failure and bottleneck).
- **BFF** tailors a gateway per client type — great when clients diverge, at the cost of more
  components.
- **Good API design** = clear naming, correct status codes + structured errors, **idempotency keys**
  for writes, pagination, and versioning. These habits run through every lesson in this module.
- A gateway is a **reverse proxy with brains**; forward proxies front clients, reverse proxies
  front servers.

---

## In the wild

- **NGINX, Envoy, Kong, AWS API Gateway, Apigee** are common gateway implementations.
- **Netflix** popularized the **BFF** pattern and runs a sophisticated edge/gateway tier (Zuul).
- **Stripe** showcases the API-design principles: idempotency keys, versioning, cursor pagination,
  structured errors with codes.
- **Cloud edges** (Cloudflare, AWS) combine gateway duties with CDN and TLS termination at the
  network edge.

---

## Interview angle

In any microservices design, propose an **API gateway** and list what it centralizes — routing,
auth, rate limiting, TLS termination, aggregation — so individual services stay focused on
business logic. Mention **REST-at-the-edge, gRPC-inside** and the **BFF** pattern when clients
diverge. Then flag that the gateway is **a critical, must-be-HA component** (potential SPOF and
bottleneck). Sprinkle in the API-design fundamentals — **idempotency keys**, correct status codes,
pagination, versioning — to show end-to-end maturity.

**Common follow-ups:**
- "Doesn't the gateway become a single point of failure?" → yes; run it as a horizontally scaled,
  load-balanced, HA cluster — never a single instance.
- "When would you use a BFF instead of one gateway?" → when client types (mobile vs web) need
  genuinely different data shapes/optimizations.
- "Where would you enforce rate limiting — gateway or service?" → often both; gateway for coarse
  per-client edge limits, services for fine-grained internal limits
  ([Module 2.11](../02-building-blocks/11-rate-limiting.md)).

---

## Self-check

1. Name five responsibilities an API gateway centralizes, and explain why putting them in the
   gateway beats implementing them in each service.
2. What problem does the BFF pattern solve that a single shared gateway struggles with, and what's
   the cost of adopting it?
3. A client's `POST /payments` times out and it retries. Walk through how an idempotency key
   prevents a double charge.
4. Distinguish a forward proxy from a reverse proxy by *whom each one represents*. Which one is an
   API gateway?
5. Your gateway is a single instance. What's the danger, and how do you fix it?

---

You've finished **Module 1 — Networking & Communication**: how machines find each other (IP/DNS),
move bytes reliably or fast (TCP/UDP), speak HTTP/REST, call each other efficiently (gRPC),
fetch tailored data (GraphQL), push in real time (SSE/WebSockets), and sit behind a smart front
door (the gateway). Next we assemble these into the **building blocks** of scalable systems,
starting with the workhorse the gateway leans on: the load balancer.

**Next:** [2.1 — Load Balancing »](../02-building-blocks/01-load-balancing.md)
