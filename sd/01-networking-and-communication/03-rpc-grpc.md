# 1.3 — RPC and gRPC

> **Module 1 · Networking & Communication** · ~24 min read
> *REST is great for public, resource-shaped APIs. But when your own services chat with each
> other thousands of times a second, you often want something that feels like calling a normal
> function — and is faster on the wire. That's RPC, and gRPC is its modern flagship.*

---

## The problem

You've split your app into services (auth, billing, search, ...). The billing service needs the
user's email, which lives in the user service. With REST you'd hand-write: build a URL, set
headers, serialize JSON, send an HTTP request, parse the JSON response, map it back into an
object, handle errors as status codes. Every. Single. Call. Across dozens of services.

That's a lot of boilerplate for what is, conceptually, just: **"call `getUser(42)` on that other
machine and give me back a `User`."** What if calling a function on a remote server looked almost
exactly like calling a local one?

> **Analogy.** Calling a local function is like asking a coworker at the next desk — instant,
> trivial. RPC is like having an **assistant who handles long-distance calls for you**: you say
> "ask the Tokyo office for the Q3 numbers," and the assistant dials, speaks the language,
> writes down the answer, and hands it to you as if it were local. You never touch the phone.
> The assistant is the **stub** that RPC generates.

---

## Core idea: call a remote function like a local one

**RPC (Remote Procedure Call)** is a style where invoking a function on another machine *looks
like* a normal local function call. The network plumbing — serializing arguments, sending them,
waiting, deserializing the result — is hidden behind a generated **stub**.

```
   Your code:                 Under the hood (generated stub does this):
   user := client.GetUser(42)
        │                     1. serialize {id: 42}        ("marshalling")
        │                     2. send over the network
        ▼                     3. server deserializes, runs the real GetUser
   ...looks local...          4. server serializes the User result
        ▲                     5. send back; client deserializes into a User struct
        │                     6. return user
```

- **Marshalling / serialization** — turning an in-memory object into bytes for the wire.
- **Stub (or client/server "stubs")** — the auto-generated glue that does steps 1–6 so *you*
  don't. You call a method; the stub makes the network happen.

> **The leaky-abstraction warning.** RPC *looks* local, but it isn't. Remote calls can be slow,
> can fail mid-flight, and can partially succeed. Treating them as free local calls is a classic
> mistake — always design for latency and failure (timeouts, retries, the idempotency lessons
> from [1.2](02-http-https-rest.md)). The convenience is real; the network is still the network.

---

## gRPC: the modern RPC framework

**gRPC** (originally "Google RPC") is the dominant RPC framework today. Three pillars make it
fast and ergonomic:

### 1) It runs on HTTP/2

Recall HTTP/2 from the [last lesson](02-http-https-rest.md): **multiplexing** (many calls share
one connection), header compression, and bidirectional streams. gRPC gets all of that for free —
many concurrent RPCs over a single warm connection, no per-call handshake.

### 2) It uses Protocol Buffers (a binary format)

Instead of JSON (text, verbose, parsed at runtime), gRPC uses **Protocol Buffers ("protobuf")**:
a **compact binary** serialization format with a **schema** you define up front in a `.proto`
file. Binary means smaller payloads and much faster encode/decode than JSON.

```protobuf
// user.proto — the contract, shared by client and server
syntax = "proto3";

service UserService {
  rpc GetUser(GetUserRequest) returns (User);
}

message GetUserRequest {
  int32 id = 1;          // the "1" is the field's wire number, not its value
}

message User {
  int32  id    = 1;
  string name  = 2;
  string email = 3;
}
```

### 3) It generates code (the stubs) for you

You run the protobuf compiler against `user.proto` and it **generates** the client stub and
server interface in your language (Go, Java, Python, ...). You implement the server method and
*call* the client method — the marshalling glue is generated, type-safe, and identical on both
ends because both sides compiled the *same* contract.

```
  user.proto  ──(protoc codegen)──►  client stub  +  server interface
       │                                  │                  │
   the contract                    you just call       you just implement
                                   GetUser(...)         GetUser(...)
```

### Streaming: four call shapes

Because HTTP/2 supports streams, gRPC offers more than one-shot request/response:

| Type | Shape | Example |
|------|-------|---------|
| **Unary** | 1 request → 1 response | `GetUser(id)` |
| **Server streaming** | 1 request → stream of responses | "subscribe to price updates" |
| **Client streaming** | stream of requests → 1 response | "upload many chunks, get one ack" |
| **Bidirectional** | stream ↔ stream | live chat, real-time sync |

This is a big edge over plain REST, where streaming is awkward (you'd reach for SSE/WebSockets,
the [next lesson](05-polling-sse-websockets.md)).

---

## gRPC vs REST — the trade-off

Neither wins universally. They're tuned for different jobs.

| Dimension | REST (JSON/HTTP) | gRPC (protobuf/HTTP2) |
|-----------|------------------|------------------------|
| **Payload** | JSON — text, human-readable, verbose | Protobuf — binary, compact, fast |
| **Contract** | informal (OpenAPI optional) | **strict `.proto` schema, enforced** |
| **Codegen** | optional | built-in, first-class |
| **Streaming** | awkward (SSE/WebSocket) | native, 4 modes |
| **Browser support** | universal | needs a proxy (gRPC-Web) |
| **Human debuggability** | easy (`curl`, readable) | harder (binary; needs tooling) |
| **Performance** | good | **better** (smaller + multiplexed) |
| **Best fit** | **public APIs, browser clients** | **internal service-to-service** |

The clean mental split most teams use:

```
   Browser / 3rd-party ──REST/JSON──►  [ API Gateway ]
                                            │
                                   ──gRPC──►├──► [ Auth service ]
                                            ├──► [ Billing service ]
                                            └──► [ Search service ]
                                       (internal, fast, typed)
```

REST at the **edge** (public, browser-friendly, easy to debug); gRPC **inside** (fast, typed,
high-volume service-to-service). You'll see exactly this pattern again in the
[API gateway lesson](06-api-gateway-design.md).

---

## When NOT to use gRPC

- **Public/browser-facing APIs.** Browsers can't speak raw gRPC; you'd need a gRPC-Web proxy.
  REST is friendlier for third-party developers and quick `curl` debugging.
- **Simple CRUD with low call volume.** The protobuf/codegen setup may not pay for itself.
- **When human-readable payloads matter** for debugging or auditing.

Trade-off in a line: **gRPC trades human-friendliness and ubiquity for performance and a strict,
typed contract.** That trade pays off most *inside* your system, where both ends are yours.

---

## Trade-offs & key takeaways

- **RPC = "call a remote function like a local one,"** with generated stubs hiding the
  marshalling. Powerful, but the network is still the network — design for latency and failure.
- **gRPC's three pillars:** HTTP/2 (multiplexing), Protocol Buffers (compact binary + schema),
  and codegen (type-safe stubs from one shared `.proto`).
- **Streaming is a first-class gRPC superpower** (4 modes), unlike plain REST.
- **The contract is enforced.** Both sides compile the same `.proto`, so mismatches are caught at
  build time, not 3 a.m. in production.
- **REST at the edge, gRPC inside** is the default architecture. Pick per boundary, not globally.

---

## In the wild

- **Google** runs gRPC pervasively between internal services (it grew from their internal "Stubby").
- **Netflix, Square, Dropbox, Uber, Cloudflare** use gRPC for high-volume internal microservice
  communication.
- **Kubernetes** components (and `etcd`) talk gRPC under the hood.
- **Public APIs** at those same companies stay **REST/JSON** for browser and third-party reach —
  the edge-vs-internal split in practice.

---

## Interview angle

When the design has **many internal services chatting at high volume**, propose **gRPC** and
justify it: HTTP/2 multiplexing, compact protobuf, a strict generated contract, native
streaming. Then show range by explaining when you'd *keep* REST — public/browser-facing edges —
and that real systems do **both** (REST at the gateway, gRPC behind it). Naming the
leaky-abstraction risk ("RPC looks local but I'd still set timeouts and make retries idempotent")
is a strong senior signal.

**Common follow-ups:**
- "Why is gRPC faster than REST?" → binary protobuf (smaller, faster than JSON parsing) +
  HTTP/2 multiplexing over one warm connection.
- "Why not use gRPC for your public API?" → browsers can't speak it natively; harder to debug;
  REST is friendlier for third parties.
- "A gRPC call times out and you retry — any concern?" → same as any RPC: ensure the operation
  is idempotent, or you risk duplicating work (link back to [1.2](02-http-https-rest.md)).

---

## Self-check

1. In your own words, what does the generated **stub** do, and why is "RPC looks like a local
   call" both its biggest strength and its most dangerous illusion?
2. Name gRPC's three pillars and the concrete benefit each provides.
3. Give one scenario where you'd choose gRPC over REST and one where you'd choose REST over gRPC
   — with the reason for each.
4. Which gRPC streaming mode fits "stream live stock prices to a client after one subscribe
   request," and why?
5. Why does compiling both client and server from the same `.proto` catch a whole class of bugs
   that JSON-over-REST would only surface at runtime?

---

**Next:** [1.4 — GraphQL vs REST »](04-graphql.md)
