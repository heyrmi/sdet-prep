# 4.8 — Design a Chat System

> **Module 4 · Case Studies** · ~35 min read + coding assignment
> *Concepts exercised:* WebSockets & long-lived connections, stateful connection servers,
> service discovery / session store, message ordering with per-conversation sequence numbers,
> wide-column storage, presence, push for offline users, and the fan-out trade-off.

---

*You type "running late, sorry!", hit send, and a half-second later three friends see it pop
up — in the same order, on three different phones, two of which were asleep. That tiny moment
hides almost every hard problem in distributed systems. Let's pull it apart.*

---

## The problem

A **chat system** (WhatsApp, Messenger, Slack, Discord) lets users exchange messages in
near-real-time — one-to-one and in groups — with delivery and read receipts, online presence,
and messages that survive when you're offline and get pushed to you later.

Why it's interesting: the web was built on **request/response** (you ask, the server answers).
Chat is the opposite — the *server* needs to push to *you* the instant someone else types,
without you asking. That single inversion drives the whole design.

> **Analogy.** Email is the postal service: you drop a letter in a box, it gets sorted, and
> maybe tomorrow it arrives. Chat is a **telephone switchboard**. Everyone has a line plugged
> into a central board (a persistent connection). When Alice speaks, the operator instantly
> patches her voice to Bob's line. If Bob's phone is unplugged, the operator writes the message
> on a sticky note (the "offline store") and reads it to him the moment he plugs back in. The
> hard parts are all about that switchboard: who's plugged into which board, what to do when a
> line drops, and how to keep a thousand operators in sync.

---

## Step 1: Requirements (always start here)

**Functional**
- **1:1 messaging** and **group messaging**.
- **Online presence** — show who is online / last-seen.
- **Delivery & read receipts** — sent ✓, delivered ✓✓, read (blue).
- **Message ordering** — within a conversation, everyone sees messages in the same order.
- **Offline messages** — if a recipient is offline, store and deliver when they reconnect.
- **Push notifications** — wake the app / phone for offline users.

**Non-functional**
- **Low latency** delivery (feels instant — well under a second).
- **Highly available** — being unreachable is the worst failure for a chat app.
- **Consistency within a conversation** — ordering must be stable; you can be looser about the
  exact global ordering across unrelated conversations.
- **Scale** — hundreds of millions of users, many of them connected *at once*. Connections,
  not just requests, are the scaling unit.

**Clarifying questions to ask the interviewer**
- Group size cap? (10 vs 100,000 changes the fan-out strategy completely.)
- Do we need message history / search, or is recent history enough?
- End-to-end encryption? (Changes where you can do server-side work like search.)
- Media (images/video) or text only? (Media goes to blob storage + CDN, not the message path.)
- "Sent/delivered/read" required, or just delivery?

State the scope you're designing for out loud — it frames every later decision.

---

## Step 2: Estimation (back-of-envelope)

Let's size it so the design has numbers behind it.

- **Users:** 1 billion registered, **300 million daily active**, say **50 million connected
  concurrently** at peak.
- **Messages:** 50 messages/day/user × 300M = **15 billion messages/day** ≈ **170K messages/sec**
  average, call it **~500K/sec at peak**.
- **Storage:** average message ~200 bytes (text + metadata). 15B/day × 200B ≈ **3 TB/day** of
  message data, before replication. Over a year that's ~1 PB — clearly a sharded, append-friendly
  store, not one big SQL table.
- **Connections:** 50M concurrent persistent connections. One commodity box handles maybe
  50K–100K connections, so you need on the order of **~1000 connection servers** just to hold the
  sockets open. This is why connection servers are their own tier.

The two numbers that shape everything: **50M open connections** (a memory/network problem) and
**~500K msg/sec** (a throughput/storage problem).

---

## Step 3: High-level design

### The transport: why WebSockets

You need the server to push to the client unprompted. Options (covered in Lesson 1.5):

| Technique | How it works | Fit for chat |
|-----------|--------------|--------------|
| Short polling | Client asks "anything new?" every few seconds | Wasteful, laggy. Poor. |
| Long polling | Client asks; server holds the request until data arrives | Workable, but awkward for sends. |
| SSE (server-sent events) | One-way server→client stream over HTTP | Good for feeds; chat needs both directions. |
| **WebSocket** | One TCP connection upgraded to **full-duplex**; both sides push anytime | **The standard answer.** |

A WebSocket is a long-lived, bidirectional pipe. The client opens it once and keeps it open;
messages flow both ways with tiny overhead. The catch: it makes the server **stateful** — that
socket lives in the memory of *one specific server*. That fact ripples through the whole design.

### Components

```
                       ┌───────────────────────────────┐
   ┌────────┐  HTTPS   │   Stateless API services      │
   │ Client │────────▶ │  (login, contacts, groups,    │──▶ [ User / Group DB ]
   │ (app)  │          │   history fetch, push tokens) │
   └────┬───┘          └───────────────────────────────┘
        │ WebSocket (persistent)
        ▼
   ┌──────────────────┐        ┌──────────────────────────┐
   │ Connection       │  who   │  Session store /          │
   │ Server #1..#N    │◀──────▶│  service discovery        │
   │ (holds 50K       │ is on  │  (user_id -> conn server) │
   │  sockets each)   │ which? │  e.g. Redis / ZooKeeper   │
   └───────┬──────────┘        └──────────────────────────┘
           │ enqueue / route
           ▼
   ┌──────────────────┐        ┌──────────────────────────┐
   │ Message service  │───────▶│  Message store            │
   │ (assign seq,     │        │  (wide-column: Cassandra/ │
   │  persist, route) │        │   HBase, keyed by convo)  │
   └───────┬──────────┘        └──────────────────────────┘
           │ recipient offline?
           ▼
   ┌──────────────────┐
   │ Push service     │──▶ APNs (Apple) / FCM (Android)
   └──────────────────┘
```

Two tiers do the heavy lifting:
- **Connection servers** are *stateful* — each holds tens of thousands of live WebSockets. Their
  only job is to keep sockets open and ferry bytes.
- **Everything else is stateless** and horizontally scalable behind a normal load balancer.

The bridge between them is the **session store / service discovery** layer: a fast lookup of
`user_id → which connection server holds their socket`. Without it, when Alice's message arrives,
nobody knows which of the 1000 connection servers Bob is plugged into.

### Message flow (Alice → Bob, 1:1)

1. Alice's app sends the message up her **WebSocket** to **Connection Server A**.
2. A hands it to the **message service**, which assigns a **per-conversation sequence number**,
   **persists** it to the message store, and looks up Bob in the **session store**.
3. If Bob is online on **Connection Server B**, the message is routed to B, which pushes it down
   Bob's socket. B sends a "delivered" receipt back along the same path to Alice.
4. If Bob is **offline**, the message is already persisted; the **push service** fires a
   notification via APNs/FCM. When Bob reconnects, his client fetches everything with a
   sequence number greater than the last one it has ("sync since seq N").

### API sketch

Over the WebSocket, messages are small JSON/protobuf frames, not REST calls:

```
// client -> server
{ "type": "send",    "convo": "c123", "tempId": "t9", "body": "hi" }
{ "type": "read",    "convo": "c123", "upToSeq": 42 }

// server -> client
{ "type": "message", "convo": "c123", "seq": 43, "from": "alice", "body": "hi", "ts": ... }
{ "type": "ack",     "tempId": "t9", "seq": 43 }        // your send was stored as seq 43
{ "type": "receipt", "convo": "c123", "seq": 43, "state": "delivered" }
{ "type": "presence","user": "bob", "state": "online" }
```

Stateless REST endpoints handle the rest: `POST /login`, `GET /conversations/{id}/messages?since=N`,
`POST /groups`, `PUT /push-token`.

### Data model — why wide-column

Messages are an **append-heavy, read-by-conversation, time-ordered** workload. That's the exact
shape a **wide-column store** (Cassandra, HBase, ScyllaDB) is built for. Model it as:

```
Partition key:  conversation_id      ← all of one chat's messages live together, on one shard
Clustering key: seq (or timestamp)   ← stored physically sorted, so "give me the latest 50"
                                        and "everything since seq N" are cheap range reads
Columns:        message_id, sender_id, body, created_at, ...
```

| Choice | Why | Trade-off |
|--------|-----|-----------|
| Partition by `conversation_id` | All of a chat's history is co-located → one fast read | A viral group is a hot partition |
| Cluster (sort) by `seq` | Range scans for "latest N" / "since N" are sequential disk reads | Must assign a stable seq (see deep dive) |
| Wide-column vs SQL | Handles PB-scale writes, linear scale-out, time-series shape | Weaker joins/transactions — fine here |

A relational DB *can* do this at small scale, but at 500K writes/sec across petabytes, the
write throughput and horizontal scaling of a wide-column store win. State that you'd use SQL for
the **user/group metadata** (small, relational, needs transactions) and wide-column for the
**message firehose**. Right tool, right job.

---

## Step 4: Deep dive — message ordering

"Everyone sees messages in the same order" sounds trivial and is not. Clocks on different phones
and servers disagree by seconds; the network reorders things. If you order by client timestamp,
two people sending at once can see different orders, and a phone with a wrong clock can stick a
message "in the past."

**The fix: a single authority assigns a per-conversation sequence number.** When the message
service accepts a message for conversation C, it does an atomic "give me the next number for C"
and stamps the message. Every client orders by that number. Because one component owns the
counter per conversation, the order is **total and identical for all recipients**.

| Ordering strategy | Consistent across clients? | Notes |
|-------------------|----------------------------|-------|
| Client timestamp | No | Clock skew & lies break it |
| Server receive time | Mostly | Two servers' clocks still differ |
| **Per-conversation seq** | **Yes** | One counter per conversation; total order ⭐ |
| Global seq for whole system | Yes, but overkill | A single global counter is a bottleneck; you don't need cross-conversation order |

**This is exactly what the Go assignment makes you build:** a Hub that stamps every message in a
room with a monotonically increasing `Seq`, so all members agree on order. The key insight you'll
feel in the code: ordering is easy *if a single owner assigns the numbers*. Scatter that
responsibility and you're back to chaos.

> **Trade-off — scope of the counter.** Per-conversation seq is cheap and sufficient. A global
> total order across *all* conversations would need one global counter (a bottleneck) or
> consensus, and chat simply doesn't need it: nobody cares whether your message to Mom came
> "before" my message to my boss. Match the consistency to what users can actually observe.

---

## Step 4b: Deep dive — group fan-out

A 1:1 message has one recipient. A group has many — and "send to a group" means looking up every
member and routing to each one's connection server.

| Group size | Strategy | Why |
|------------|----------|-----|
| Small (≤ ~500) | **Fan-out on write**: when a message arrives, push to every online member immediately | Simple, instant; member count is small |
| Large / "channels" (10K–1M) | **Fan-out on read** or hybrid: store once; members pull on open, only push to the actively-watching ones | Pushing to a million sockets per message is a write storm |

This is the same **fan-out-on-write vs fan-out-on-read** trade-off from the News Feed lesson (4.7).
Small groups behave like the feed of a normal user; huge broadcast groups behave like a celebrity's
followers — you can't afford to fan out every message to everyone, so you let most clients pull.

> **Trade-off.** Fan-out on write gives the lowest delivery latency but its cost grows with group
> size. Fan-out on read keeps writes cheap but adds latency and read load. Real systems pick a
> threshold and switch strategies above it.

---

## Step 4c: Deep dive — presence

"Online / last seen" looks easy and quietly generates enormous load if done naively. The signal
is simple: a user is online while their WebSocket is connected; offline when it drops (or after a
missed heartbeat, since dead TCP connections aren't always noticed instantly).

The trap is **broadcasting presence changes**. If a user with 1,000 contacts flips online/offline,
that's 1,000 notifications — and a flaky connection flapping does it repeatedly. Mitigations:

- **Heartbeats**, not instant flips: mark offline only after a missed heartbeat window, which also
  debounces flapping.
- **Fetch on demand**: only compute presence for contacts the user is *currently looking at*,
  rather than push every change to everyone.
- Keep presence in a **fast, expiring store** (Redis with a TTL refreshed by heartbeats) — if the
  heartbeat stops, the key expires and the user is implicitly offline.

> **Trade-off — freshness vs cost.** Real-time presence for every contact is expensive and rarely
> worth it. A few seconds of staleness is invisible to users and saves enormous fan-out.

---

## Step 4d: Deep dive — the stateful-connection problem

Because a WebSocket lives in one server's memory, two awkward things follow:

1. **Routing.** To reach Bob you must know *which* connection server holds his socket — hence the
   **session store** (`user_id → server`), updated on connect/disconnect. Stale entries (server
   crashed) must expire, so it's TTL-based.
2. **Failure.** If a connection server dies, all its sockets drop. Clients must **auto-reconnect**
   (to any server, via the load balancer), re-register in the session store, and **sync since
   their last seq**. This is why persistence + sequence numbers aren't optional: they're how a
   reconnecting client catches up without losing or duplicating messages.

> **Trade-off — statefulness.** Stateless services are trivial to scale and recover. Chat
> deliberately keeps a stateful tier because long-lived connections are the whole point; you pay
> for it with a discovery layer and careful reconnect logic. You isolate the statefulness to one
> tier and keep everything else stateless.

---

## In the wild

- **WhatsApp** famously ran ~50M+ connections per server tuned on Erlang/BEAM, with offline store
  + push to APNs/FCM, and tight, encrypted, text-first message frames.
- **Slack / Discord** use persistent WebSocket "gateways" (connection servers) plus a discovery
  layer; Discord stores messages in Cassandra/ScyllaDB keyed by channel, ordered by Snowflake IDs
  (which double as a sortable sequence — see Lesson 4.3).
- **Messenger / Signal** use a similar connection-server + offline-queue + push design, with
  end-to-end encryption pushing search and some features to the client.

---

## Interview angle

Lead with the **transport choice (WebSocket)** and immediately name the consequence:
**connection servers are stateful**, so you need a **session-store / discovery** layer to know who
is connected where. Walk the **message flow** for 1:1, then handle the offline case with
**persist + push + sync-on-reconnect**. The senior signal is **message ordering via a
per-conversation sequence number** — explain *why* client timestamps fail. Then show range by
covering **group fan-out (write vs read)**, **presence load**, and **reconnect after a server
dies**. Close with the storage choice: **wide-column keyed by conversation, sorted by seq.**

**Common follow-ups:**
- "Two people send at the exact same time — what order do they see?" → a single owner assigns the
  per-conversation seq; that owner's order is the truth for everyone.
- "A connection server crashes. What happens?" → sockets drop, clients reconnect through the LB,
  re-register in the session store, and sync messages since their last seq (nothing lost,
  duplicates de-duped by message id).
- "A group has 500,000 members. Still fan out on write?" → no; switch to fan-out on read / pull
  for huge groups, only pushing to active viewers.
- "How do you keep presence cheap?" → heartbeats + TTL store + fetch-on-demand instead of
  broadcasting every flip.

---

## Practice → the Go assignment

Now build the heart of it: an in-memory **chat Hub** using goroutines and channels — the same
shape as a real connection server, minus the network. Go to [`assignment/`](assignment/) and
implement, in order:

1. A `Hub.Run()` event loop — **one goroutine owns all room state**; the public methods only send
   it commands over channels. (This is "share memory by communicating," the Go way.)
2. `Join`, `Leave`, and `Broadcast` — Broadcast delivers a `Message` to every current member's
   buffered `Out` channel.
3. **Per-room sequence numbers** — every broadcast gets a monotonically increasing `Seq` so all
   receivers agree on order (the ordering deep dive, in code).
4. A clean `Stop()` that shuts the loop down and waits for it to exit.

```bash
cd assignment
go test ./...          # red → implement → green
go test -race ./...    # must pass: many goroutines join/broadcast concurrently
```

The tests use buffered channels and a short bounded `select`+timeout read — **deterministic, no
real sleeps**. The interface is given; you fill in the `// TODO`s. A reference solution is in
[`solution/`](solution/) — try first, peek after.

**Next case study:** [4.9 — Search Autocomplete (Typeahead) »](../09-autocomplete/)
