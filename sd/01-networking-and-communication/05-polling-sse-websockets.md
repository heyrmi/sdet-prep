# 1.5 — Real-Time: Polling, SSE & WebSockets

> **Module 1 · Networking & Communication** · ~28 min read
> *Everything so far has been client-asks, server-answers. But chat messages, live scores, and
> notifications need the server to push data *to* the client the moment it happens. HTTP wasn't
> built for that — here are the four ways we make it work, and what each costs.*

---

## The problem

HTTP is **client-initiated**: the client asks, the server answers, the exchange ends. But tons of
features need the *server* to tell the client about something **as it happens**:

- A new chat message arrives.
- A stock price ticks.
- A friend comes online.
- A long background job finishes.

The server can't just "call" the client — there's no open channel by default, and the client
might be behind a firewall with no public address. So how do we get **server-to-client, real-time
delivery** out of a protocol designed for "ask and answer"? Four techniques, from a crude hack to
a purpose-built channel.

> **Analogy.** You're waiting on important news from a friend.
> - **Short polling:** you text "any news?" every 30 seconds. Annoying, wasteful, but simple.
> - **Long polling:** you call and *stay on the line* until they have news, then hang up and call
>   right back. Less chatter, but you're tying up a line.
> - **SSE:** they promise to text *you* the moment anything happens — a one-way subscription.
> - **WebSockets:** you both keep a phone line open and talk freely in *both* directions anytime.

---

## Option 1: Short polling

The client repeatedly asks "anything new?" on a fixed timer (say every 5s). Dead simple — just a
loop of normal HTTP requests.

```
  Client                         Server
    │ ── GET /messages ──────►    │
    │ ◄── [] (nothing) ──────────  │
    │   ...wait 5s...             │
    │ ── GET /messages ──────►    │
    │ ◄── [] (nothing) ──────────  │
    │   ...wait 5s...             │
    │ ── GET /messages ──────►    │
    │ ◄── [msg!] ───────────────  │   finally something
```

- **Pros:** trivial to build, works everywhere, stateless on the server.
- **Cons:** **wasteful** (most requests return nothing) and **laggy** (a message can sit up to one
  interval before delivery). Tighten the interval for less lag → even more wasted requests. Pure
  trade-off, and a bad one at scale.

---

## Option 2: Long polling

The client asks, and the server **holds the request open** until it actually has something to
send (or a timeout hits). On reply, the client *immediately* reconnects. This trades constant
empty requests for a held-open connection.

```
  Client                         Server
    │ ── GET /messages ──────►    │   (server holds it open... waiting...)
    │                             │
    │ ◄── [msg!] ───────────────  │   data arrives → respond now
    │ ── GET /messages ──────►    │   client reconnects instantly
    │                             │   (held open again...)
```

- **Pros:** near real-time, works over plain HTTP (great compatibility, no special protocol).
- **Cons:** each message still costs a full request/response cycle (headers and all); holding many
  open connections strains the server; awkward edge cases around reconnect timing. It's the
  "good-enough" fallback when you can't use the next two.

---

## Option 3: Server-Sent Events (SSE)

**SSE** opens **one long-lived HTTP connection** over which the **server streams events to the
client** continuously. It's **one-directional**: server → client only. Built into browsers via
the `EventSource` API; it's just HTTP with `Content-Type: text/event-stream`.

```
  Client                         Server
    │ ── GET /stream ────────►    │   open once
    │ ◄══ event: price 101 ═════  │
    │ ◄══ event: price 102 ═════  │   server keeps pushing
    │ ◄══ event: price 103 ═════  │   over the SAME connection
    │            ...              │
```

- **Pros:** simple (plain HTTP, no new protocol), **auto-reconnect built in**, plays nicely with
  proxies/firewalls, efficient for one-way streams.
- **Cons:** **server → client only** (the client still uses normal requests to send data up); text
  only; subject to the browser's per-host connection limits on HTTP/1.1.
- **Perfect for:** live feeds, notifications, dashboards, log/price streams, AI token streaming —
  anywhere data flows *down* and the client rarely needs to push *up*.

---

## Option 4: WebSockets

A **WebSocket** is a **full-duplex** (both directions, simultaneously), persistent connection
between client and server. It starts as an HTTP request that **"upgrades"** to the WebSocket
protocol, after which either side can send messages anytime, with very low overhead per message.

```
  Client                              Server
    │ ── HTTP GET (Upgrade: websocket) ──►  │   handshake
    │ ◄── 101 Switching Protocols ───────   │   upgraded!
    │ ◄════════════ open channel ═════════► │
    │ ── "hi" ─────────────────────────►    │   client → server, anytime
    │ ◄───────────────── "hey!" ─────────   │   server → client, anytime
    │ ── "typing..." ──────────────────►    │   both directions, full-duplex
```

- **Pros:** true two-way, low latency, low per-message overhead — the gold standard for
  interactive real-time.
- **Cons:** it's a **stateful, long-lived connection**, which is operationally heavier (see
  scaling, below); not plain HTTP, so some proxies/load balancers need configuring; you handle
  reconnection and heartbeats yourself.
- **Perfect for:** chat, multiplayer games, collaborative editing (Google Docs), live trading —
  anything genuinely **bidirectional**.

---

## Comparison

| | Short polling | Long polling | SSE | WebSockets |
|---|---|---|---|---|
| **Direction** | client pulls | client pulls | **server → client** | **both (full-duplex)** |
| **Connection** | new each time | held, then reopened | one long-lived | one long-lived |
| **Latency** | poor (interval) | good | good | **best** |
| **Server load** | high (wasted reqs) | medium | low | low (but stateful) |
| **Protocol** | plain HTTP | plain HTTP | plain HTTP | WS (HTTP upgrade) |
| **Complexity** | trivial | low | low | higher |
| **Best for** | tiny apps, prototypes | legacy fallback | feeds, notifications | chat, games, collab |

### How to choose

```
  Do you need the client to send data continuously too?
        │
        ├── No  → Is one-way server push enough?
        │            ├── Yes → SSE        (simple, HTTP-native)
        │            └── rare updates / simplest possible → polling
        │
        └── Yes → WebSockets  (true full-duplex)
```

> **Default advice:** use **SSE** for one-way streams (it's underrated and dead simple), **WebSockets**
> for true two-way interaction, and **long polling** only as a compatibility fallback. Reach for
> **short polling** only for prototypes or genuinely infrequent updates.

---

## Scaling stateful WebSocket connections

This is where real-time gets hard — and where interviews go deep. Normal HTTP servers are
**stateless** (recall [zero-to-millions](../00-foundations/01-scale-zero-to-millions.md)): any
server can handle any request, so load balancing is trivial. WebSockets break that assumption:
each connection is **long-lived and pinned to one specific server**.

### Problem 1: connections are sticky and limited

A load balancer must route a client's traffic to the **same** server holding its socket (a.k.a.
**sticky sessions**). And each server can hold only so many open sockets (file descriptors,
memory) — often tens of thousands, not millions. To serve millions of users you need **many**
WebSocket servers.

```
                         ┌─ [ WS Server A ]  ← Alice's socket lives here
   Clients ─► [ LB ] ────┼─ [ WS Server B ]  ← Bob's socket lives here
              (sticky)   └─ [ WS Server C ]
```

### Problem 2: cross-server delivery (the fan-out problem)

Alice (on Server A) sends Bob a message, but **Bob's socket is on Server B.** Server A has no
direct way to reach Bob. The standard fix is a **shared pub/sub backbone** (e.g. Redis Pub/Sub or
a message queue): each server **subscribes** to the channels for its connected users; to send to
Bob, you **publish** to Bob's channel and whichever server holds Bob's socket delivers it.

```
   Alice ─► [ Server A ] ──publish "to:Bob"──► [ Redis Pub/Sub ]
                                                    │ subscribe
                                          [ Server B ] ──► Bob's socket
```

You also need a **presence/registry** ("which server holds Bob right now?") and **heartbeats**
(ping/pong) to detect dead connections and clean them up. We build exactly this in the
[Chat System case study](../04-case-studies/08-chat-system/), and the pub/sub backbone shows up
again in [Module 2.10 — Message queues](../02-building-blocks/10-message-queues-streaming.md).

> **Trade-off in a line:** WebSockets give the best real-time experience but reintroduce
> **server statefulness** — sticky routing, connection limits, and cross-server fan-out — that
> stateless HTTP let you avoid. That operational cost is the price of full-duplex.

---

## Trade-offs & key takeaways

- The four options form a ladder: **short polling → long polling → SSE → WebSockets**, trading
  more setup/complexity for lower latency and less waste.
- **Polling is simple but wasteful;** most requests return nothing and latency is bounded by the
  interval.
- **SSE is the underrated default for one-way pushes** — plain HTTP, auto-reconnect, low overhead.
- **WebSockets are the gold standard for two-way real-time** but make servers **stateful**.
- **Scaling WebSockets** means sticky routing, per-server connection limits, and a **pub/sub
  backbone** for cross-server delivery — the hard part lives here, not in the protocol.
- Match the tool to the **direction** of data flow first, then to latency needs.

---

## In the wild

- **Slack, WhatsApp, Discord** use **WebSockets** for chat and presence, backed by pub/sub
  fan-out across many gateway servers.
- **ChatGPT and other LLM UIs** stream tokens with **SSE** — a textbook one-way server push.
- **Stock tickers and live dashboards** commonly use **SSE** (one-way) or WebSockets when they
  also accept client actions.
- **Figma and Google Docs** use **WebSockets** for low-latency collaborative editing.
- Many older systems still use **long polling** as a fallback when WebSockets are blocked.

---

## Interview angle

When a design needs real-time updates, **state the data direction first** — one-way vs two-way —
because it picks your tool. Propose **SSE for one-way** (notifications, feeds, token streaming)
and **WebSockets for bidirectional** (chat, games, collaboration), mentioning **polling as a
fallback.** Then earn senior points by diving into **scaling WebSockets**: sticky sessions,
per-server connection limits, and a **Redis/queue pub/sub backbone** for cross-server message
fan-out, plus heartbeats for dead-connection cleanup. Calling out that WebSockets make servers
*stateful* (and why that's costly) is the key insight.

**Common follow-ups:**
- "Why not just poll every second?" → wasteful (mostly empty responses) and still up to 1s of
  latency; doesn't scale.
- "Two chat users are connected to different servers — how does a message get across?" → pub/sub
  backbone (Redis/queue) + a presence registry of who's on which server.
- "When would SSE be a better choice than WebSockets?" → when data flows only server → client
  (feeds, notifications); SSE is simpler, HTTP-native, and auto-reconnects.

---

## Self-check

1. Explain why short polling is both the simplest and the most wasteful option, and what tightening
   the poll interval trades away.
2. SSE and WebSockets both keep a long-lived connection. What's the fundamental difference, and
   how does it drive when you'd pick each?
3. Why are stateless HTTP servers easy to load-balance while WebSocket servers need sticky
   sessions?
4. Alice and Bob are on different WebSocket servers. Walk through how a message from Alice reaches
   Bob, naming the components involved.
5. You're building live notifications that the client never replies to. Which option do you pick
   and why?

---

**Next:** [1.6 — API Gateways & API Design »](06-api-gateway-design.md)
