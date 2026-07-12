# 1.2 — HTTP, HTTPS & REST APIs

> **Module 1 · Networking & Communication** · ~30 min read
> *TCP gets bytes between two machines. HTTP gives those bytes meaning — it's the language almost
> every web API speaks. This lesson is the one you'll reach for in nearly every design.*

---

## The problem

In the [last lesson](01-internet-ip-dns-tcp-udp.md) we got a reliable byte stream between two
machines with TCP. But a raw byte stream is just... bytes. How does the client say "give me the
profile for user 42"? How does the server reply "here it is" vs "that user doesn't exist" vs
"you're not allowed"? They need a **shared format and vocabulary** layered on top of TCP.

That vocabulary is **HTTP (HyperText Transfer Protocol)** — the request/response language of the
web — plus **TLS** to make it private (**HTTPS**), and **REST**, a popular *style* for designing
HTTP APIs that are predictable and easy to use.

> **Analogy.** TCP is an open phone line between two people. HTTP is the **etiquette of the
> conversation**: you ask a clearly phrased question ("a request"), they give a structured answer
> with a status ("a response"), and both of you agree on the language. HTTPS is having that same
> conversation in a private room nobody can eavesdrop on. REST is a *consistent way of phrasing
> your questions* so the other person always knows what you mean.

---

## Core idea: request → response

HTTP is a **request/response** protocol. The client sends a **request**; the server sends back
exactly one **response**. That's the whole shape.

```
   CLIENT                                  SERVER
     │  ── HTTP Request ───────────────►   │
     │     GET /users/42 HTTP/1.1          │
     │     Host: api.example.com           │
     │     Accept: application/json        │
     │                                     │
     │  ◄── HTTP Response ───────────────  │
     │     200 OK                          │
     │     Content-Type: application/json  │
     │                                     │
     │     {"id":42,"name":"Ada"}          │
```

### Anatomy of a request

- **Method** — the *verb*: what you want to do (`GET`, `POST`, ...).
- **Path** — *what* you want: `/users/42`.
- **Headers** — metadata: who you are, what formats you accept, auth tokens.
- **Body** — the payload (for `POST`/`PUT`); often JSON.

### Anatomy of a response

- **Status code** — a 3-digit result (`200`, `404`, `500`).
- **Headers** — metadata about the response (content type, caching, cookies).
- **Body** — the actual data (HTML, JSON, an image, ...).

---

## HTTP methods (the verbs)

| Method | Means | Has body? | Safe? | Idempotent? |
|--------|-------|-----------|-------|-------------|
| **GET** | Read a resource | no | yes | yes |
| **POST** | Create / trigger an action | yes | no | **no** |
| **PUT** | Replace a resource entirely | yes | no | yes |
| **PATCH** | Partially update a resource | yes | no | usually no |
| **DELETE** | Remove a resource | no | no | yes |

Two words there carry enormous design weight:

- **Safe** — doesn't change server state. `GET` should never modify data. (If your `GET`
  deletes something, you've built a footgun — a crawler will trigger it.)
- **Idempotent** — *doing it N times has the same effect as doing it once.* `PUT user 42 = {...}`
  ten times leaves user 42 in the same final state. `POST /orders` ten times might create **ten
  orders.**

> **Why idempotency matters so much:** networks are unreliable. A client sends a request, gets
> no response (timeout), and **retries**. If the operation is idempotent, retrying is safe. If
> it's not (like `POST`), the retry might double-charge a customer. This is why payment and
> ordering APIs use **idempotency keys** — we'll design those in
> [1.6](06-api-gateway-design.md) and go deep in
> [Module 2.14](../02-building-blocks/14-idempotency.md).

---

## Status codes (the answers)

Grouped by leading digit — memorize the *families*, not every code:

| Range | Meaning | Common examples |
|-------|---------|-----------------|
| **1xx** | Informational | `100 Continue` |
| **2xx** | Success | `200 OK`, `201 Created`, `204 No Content` |
| **3xx** | Redirection | `301 Moved Permanently`, `304 Not Modified` |
| **4xx** | **Client** error (your request was wrong) | `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `429 Too Many Requests` |
| **5xx** | **Server** error (the server messed up) | `500 Internal Server Error`, `502 Bad Gateway`, `503 Service Unavailable` |

The 4xx-vs-5xx split is a *contract*: **4xx means "you (client) did something wrong, don't just
blindly retry"; 5xx means "I (server) failed, retrying might work."** Honoring this lets clients
and load balancers behave intelligently (e.g. retry 5xx with backoff, surface 4xx to the user).

---

## Headers, statelessness & cookies

**Headers** are key-value metadata on both requests and responses. A few you'll see constantly:

- `Content-Type: application/json` — the format of the body.
- `Authorization: Bearer <token>` — who's making the request.
- `Cache-Control: max-age=3600` — how this response may be cached.
- `Retry-After: 5` — when to try again (paired with `429`/`503`).

### HTTP is stateless

**Each HTTP request is independent** — the server remembers nothing about previous requests by
default. This is the same **statelessness** that lets web servers scale horizontally (recall the
[zero-to-millions lesson](../00-foundations/01-scale-zero-to-millions.md): any server can handle
any request because no server holds your session in its private memory).

But "the server remembers nothing" raises a problem: **how do you stay logged in?** Each request
must *carry its own proof of identity.*

### Cookies & tokens

- A **cookie** is a small piece of data the server sends (`Set-Cookie`) and the browser stores
  and **sends back on every subsequent request** to that site. Classic use: a **session ID**
  that the server looks up in a shared store (cache/DB) to know who you are.
- A **token** (e.g. a JWT in an `Authorization` header) carries identity claims *inside it*, so
  the server can verify it without a lookup.

Either way, the *request* carries the state — the server stays stateless. That's the trick.

---

## HTTP versions: 1.1 vs 2 vs 3

HTTP's meaning hasn't changed much, but *how the bytes move* has improved dramatically to cut
latency. This is a favorite interview topic.

### HTTP/1.1 — one request at a time per connection

A connection handles **one request/response at a time**. Need 10 images? Either wait for each
in turn, or open multiple connections (browsers cap this, ~6 per host). The killer flaw is
**head-of-line (HOL) blocking**: a slow response stalls everything queued behind it on that
connection.

```
HTTP/1.1 (one connection):
  req1 ──► ◄── resp1   req2 ──► ◄── resp2   req3 ──► ◄── resp3
  (serialized; resp1 must finish before resp2 starts)
```

### HTTP/2 — multiplexing over one connection

HTTP/2 introduces **multiplexing**: many requests and responses share **one** TCP connection
*simultaneously*, interleaved as independent "streams." It also adds header compression and
server push.

```
HTTP/2 (one connection, interleaved streams):
  ═══ req1 req2 req3 ════════►
  ◄═══ resp2 resp1 resp3 ═════   (all in flight at once)
```

The catch: it still rides on **TCP**, so if a single packet is lost, **TCP** holds back *all*
streams until it's retransmitted — **HOL blocking moved from the HTTP layer down to TCP.**

### HTTP/3 — QUIC over UDP

HTTP/3 swaps TCP for **QUIC**, a protocol built on **UDP** (remember UDP from the last lesson).
QUIC rebuilds reliability and ordering *per-stream* in user space, so a lost packet only stalls
*its own* stream — finally killing HOL blocking. It also folds the connection and TLS handshakes
into fewer round trips, so connections start faster (great on mobile/lossy networks).

| | HTTP/1.1 | HTTP/2 | HTTP/3 |
|---|----------|--------|--------|
| Transport | TCP | TCP | **QUIC (UDP)** |
| Concurrency | 1 per connection | multiplexed streams | multiplexed streams |
| HOL blocking | yes (HTTP level) | yes (TCP level) | **no** |
| Handshake cost | TCP + TLS | TCP + TLS | combined, fewer RTTs |
| Best for | simple/legacy | most modern sites | mobile, lossy networks |

---

## HTTPS & the TLS handshake — why and how

Plain HTTP is sent in the clear: anyone between you and the server (your ISP, café Wi-Fi, a
router) can **read and modify** it. **HTTPS = HTTP over TLS (Transport Layer Security)**, which
adds three guarantees:

- **Encryption** — eavesdroppers see gibberish.
- **Integrity** — tampering is detected.
- **Authentication** — you're really talking to `example.com`, proven by a **certificate** signed
  by a trusted **Certificate Authority (CA)**.

> **Analogy.** Before the private conversation, you and the server (a) check each other's ID card
> (the certificate, vouched for by a notary = the CA), then (b) agree on a secret only you two
> share, used to scramble everything after. An eavesdropper hears only noise.

```
   Client                                Server
     │ ── ClientHello ───────────────►   │   "let's do TLS; here are my ciphers"
     │ ◄── ServerHello + certificate ──  │   "ok; here's my certificate (proves identity)"
     │  (client verifies cert via CA)    │
     │ ── key exchange ─────────────►    │   both derive a shared session key
     │ ══════ encrypted data ════════►   │   (now everything is encrypted)
```

The cost is extra round trips on connection setup — **another reason to reuse connections.**
Modern TLS (1.3) trims this to a single round trip, and QUIC trims it further. The takeaway: HTTPS
is non-negotiable today, and its handshake cost is yet another argument for connection reuse and
HTTP/2 / HTTP/3.

---

## REST — a style for HTTP APIs

**REST (REpresentational State Transfer)** isn't a protocol; it's a set of **conventions** for
designing HTTP APIs so they're predictable. The core idea: model your system as **resources**
(nouns) and act on them with HTTP **methods** (verbs).

### Resource modeling

Resources are **nouns**, identified by URLs; methods are the verbs:

```
GET    /users           → list users
POST   /users           → create a user
GET    /users/42        → get user 42
PUT    /users/42        → replace user 42
PATCH  /users/42        → partially update user 42
DELETE /users/42        → delete user 42
GET    /users/42/orders → list user 42's orders   (nesting shows relationship)
```

Good RESTful design, in practice:

- **Nouns in paths, verbs as methods.** `POST /users`, *not* `POST /createUser`.
- **Plural collections.** `/users`, not `/user`.
- **Use the status codes correctly.** `201 Created` after a `POST`, `404` for a missing
  resource, `400` for bad input.
- **Lean on method idempotency** (see the table earlier) so clients can retry safely.

### Versioning

APIs evolve, but existing clients must not break. **Version** the API:

- **URL path** — `/v1/users` → `/v2/users`. Simple, visible, most common.
- **Header** — `Accept: application/vnd.example.v2+json`. Cleaner URLs, less discoverable.

Trade-off: path versioning is obvious and easy to route; header versioning keeps URLs stable but
is harder to test in a browser and to cache.

### Pagination

Never return a million rows in one response — it's slow and can crash the client. **Paginate:**

- **Offset/limit** — `GET /users?limit=20&offset=40`. Easy, but slow on deep pages and
  *unstable* if rows are inserted/deleted while paging (you can skip or repeat items).
- **Cursor-based** — `GET /users?limit=20&after=<cursor>`. The cursor points to the last item
  seen. Stable and fast at scale; the standard for large or live datasets.

| | Offset | Cursor |
|---|--------|--------|
| Simplicity | high | medium |
| Deep-page performance | poor | good |
| Stable under inserts/deletes | no | yes |
| "Jump to page 500" | easy | hard |

---

## Trade-offs & key takeaways

- **HTTP = a shared request/response vocabulary on top of TCP.** Method + path + status are the
  contract.
- **Idempotency is a reliability tool.** It's what makes retrying safe. Know which methods are
  idempotent and why `POST` isn't.
- **4xx vs 5xx is a contract** about *whose fault it is* and *whether retrying helps.*
- **Statelessness scales.** Identity travels with each request (cookie/token); the server holds
  no per-client memory.
- **HTTP versions are about latency, not meaning:** 1.1 (serial) → 2 (multiplexed, TCP HOL) →
  3 (QUIC/UDP, no HOL). Handshake cost recurs as a theme — reuse connections.
- **HTTPS is mandatory**, and its handshake is one more reason to keep connections warm.
- **REST is convention, not law** — but conventions make APIs predictable and a joy to use.

---

## In the wild

- **Stripe, GitHub, Twilio** publish exemplary REST APIs: clean resource URLs, correct status
  codes, **idempotency keys** for writes, and cursor pagination.
- **Cloudflare, Google, Meta** serve the bulk of their traffic over **HTTP/2 and HTTP/3** to cut
  latency at planet scale.
- **JWTs** (stateless tokens) are ubiquitous for auth in microservice architectures because they
  avoid a session-store lookup on every request.

---

## Interview angle

This lesson is dense with interview gold. Be ready to: explain **idempotency** and why it makes
retries safe (and why `POST` needs idempotency keys); justify a **status code** choice precisely;
contrast **HTTP/1.1 vs 2 vs 3** with the HOL-blocking story (and that HTTP/3 rides UDP/QUIC);
and design a clean **REST resource model** with **cursor pagination** and **versioning**. Saying
"I'd use cursor pagination because offset is unstable under concurrent inserts" is a strong
senior signal.

**Common follow-ups:**
- "A client's `POST /payments` times out and it retries — how do you avoid double-charging?" →
  idempotency key; server dedupes by key.
- "Why might HTTP/2 not fully solve head-of-line blocking?" → it's still on TCP; one lost packet
  stalls all streams. HTTP/3 (QUIC/UDP) fixes it per-stream.
- "How would you paginate a feed that's constantly getting new posts?" → cursor-based; offset
  would skip/repeat items as rows shift.

---

## Self-check

1. Why is `PUT` idempotent but `POST` is not? Give a concrete example where the difference causes
   a real-world bug.
2. A client gets a `503`. Should it retry? What about a `400`? Explain using the 4xx-vs-5xx
   contract.
3. HTTP is stateless, yet you stay logged in across requests. How? Where does the "state"
   actually live?
4. Explain head-of-line blocking and how each of HTTP/2 and HTTP/3 does (or doesn't) address it.
5. When would you choose cursor pagination over offset pagination, and what does cursor give up
   in exchange?

---

**Next:** [1.3 — RPC and gRPC »](03-rpc-grpc.md)
