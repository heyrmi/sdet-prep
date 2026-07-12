# 1.1 — How the Internet Works: IP, DNS, TCP & UDP

> **Module 1 · Networking & Communication** · ~28 min read
> *Before you can design systems that talk to each other, you need a feel for how any two
> machines on Earth actually find and message each other. This is the plumbing under every box
> in the [zero-to-millions diagram](../00-foundations/01-scale-zero-to-millions.md).*

---

## The problem

You type `myapp.com` and a page appears in 200 milliseconds. Behind that flick of the wrist, a
device in your hand located *one specific machine* among the **billions** connected to the
internet, opened a reliable conversation with it across the planet, exchanged data in tiny
chunks that may have taken different physical routes, and reassembled them perfectly in order.

How? The internet pulls this off by **stacking simple layers**, each solving one job and trusting
the layer below to do its part. Understand the four jobs — *addressing, naming, transport,
ports* — and the rest of networking stops being magic.

> **Analogy.** Sending a postcard to a friend in another country. You need:
> - their **street address** (which exact building) — that's an **IP address**,
> - a **phone book** to look up the address from their name — that's **DNS**,
> - a **postal service** that either guarantees delivery with tracking and re-sends
>   (registered mail = **TCP**) or just drops it in the box fast and cheap with no guarantee
>   (regular post = **UDP**),
> - and an **apartment number** so the right person in the building gets it — that's a **port**.
>
> The whole lesson is just those four ideas in detail.

---

## Core idea: the internet is layers

No single program does everything from "find the machine" to "render the pixel." The work is
split into **layers**, where each layer talks only to the one directly above and below it. This
is the most important structural idea in all of networking: **separation of concerns.**

You'll hear two models. The textbook one is the **OSI model** (7 layers). The one that maps to
reality is the **TCP/IP model** (4 layers). For this course, the 4-layer view is plenty:

```
   ┌─────────────────────────────────────────────┐
   │ Application   HTTP, DNS, gRPC, SSH            │  what the app speaks
   ├─────────────────────────────────────────────┤
   │ Transport     TCP, UDP   (+ ports)           │  reliable? fast? to which app?
   ├─────────────────────────────────────────────┤
   │ Internet      IP                             │  which machine, what route?
   ├─────────────────────────────────────────────┤
   │ Link          Ethernet, Wi-Fi               │  the actual wire/radio next hop
   └─────────────────────────────────────────────┘
```

A message is **wrapped** as it goes down the stack (each layer adds its own header, like nesting
envelopes) and **unwrapped** on the way up at the other end. This wrapping is called
**encapsulation**. You almost never think about the Link layer in system design; you live at the
**Transport** and **Application** layers. So that's where we'll spend our time.

---

## Layer 1: IP — addressing a machine

An **IP address (Internet Protocol address)** is the unique number that identifies a machine on
a network, like a building's street address. Two versions exist:

- **IPv4** — four numbers, e.g. `142.250.72.206`. 32 bits → about **4.3 billion** addresses.
  We *ran out* of these, which is why...
- **IPv6** — eight groups of hex, e.g. `2607:f8b0:4005:80b::200e`. 128 bits → effectively
  unlimited (`3.4 × 10^38` addresses).

IP's job is **routing**: getting a packet from your machine, hop by hop through routers, to the
destination machine. Crucially, **IP alone is "best effort."** It does not promise the packet
arrives, arrives once, or arrives in order. Those guarantees — if you want them — come from the
*transport* layer above it. IP just says "here's an address; I'll try my best to get this there."

> **A packet** is one chunk of data with a header (source IP, destination IP, etc.) and a
> payload. Big messages get split into many packets that travel independently and may take
> different routes.

---

## Layer 2: DNS — turning names into addresses

Humans remember `google.com`, not `142.250.72.206`. The **Domain Name System (DNS)** is the
internet's phone book: it translates **human-readable names into IP addresses.**

> **Analogy.** You know your friend's *name* but not their *phone number*. You ask directory
> assistance. DNS is directory assistance for the internet — and it's heavily cached so you
> rarely have to make the full call.

### DNS resolution, step by step

When your browser needs the IP for `www.example.com` and nothing is cached, here's the journey.
The piece doing the legwork is the **recursive resolver** (usually run by your ISP, or a public
one like `8.8.8.8`). It asks a chain of name servers on your behalf:

```
  Browser
    │  "IP for www.example.com?"
    ▼
  [ Recursive Resolver ]  ── (1) ──►  [ Root server ]      "ask the .com servers → here"
    │      ◄──────────────────────────
    │      ── (2) ──►  [ TLD server (.com) ]                "ask example.com's servers → here"
    │      ◄──────────────────────────
    │      ── (3) ──►  [ Authoritative server (example.com) ]  "www = 93.184.216.34"
    │      ◄──────────────────────────
    ▼
  Browser  ◄── "93.184.216.34"
```

1. **Root name servers** — the top of the tree. They don't know `example.com`, but they know
   who handles `.com`. (There are 13 logical root server addresses worldwide.)
2. **TLD (Top-Level Domain) servers** — handle a suffix like `.com`, `.org`, `.io`. They don't
   know `www.example.com`'s IP, but they know which server is *authoritative* for `example.com`.
3. **Authoritative name server** — the source of truth for `example.com`. It returns the
   actual IP.

The resolver does steps 1–3 (that's the "recursive" part — it chases the full chain so the
browser doesn't have to), then hands the answer back.

### Caching and TTL — why this isn't slow every time

If every page load did that whole dance, the web would crawl. So **every layer caches** the
result: your browser, your OS, the recursive resolver. Each DNS record carries a **TTL
(Time To Live)** — how many seconds it may be cached before it must be looked up again.

- **Short TTL** (e.g. 60s): changes propagate fast, but more lookups (more load, slightly
  slower). Good before a planned IP change.
- **Long TTL** (e.g. 24h): fewer lookups (faster, cheaper), but a changed IP takes up to a day
  to take effect everywhere. **This is your first networking trade-off:** *freshness vs. load.*

### Common record types

| Record | Maps... | Example |
|--------|---------|---------|
| **A** | name → IPv4 address | `example.com → 93.184.216.34` |
| **AAAA** | name → IPv6 address | `example.com → 2606:2800:220:1:...` |
| **CNAME** | name → another name (alias) | `www.example.com → example.com` |
| **MX** | domain → mail server | `example.com → mail.example.com` |
| **NS** | domain → its authoritative name servers | `example.com → ns1.example.com` |
| **TXT** | arbitrary text (used for verification, SPF) | `"v=spf1 include:..."` |

> **In system design, DNS is also a load-balancing tool.** Returning *different* IPs for the
> same name (round-robin DNS, or geo-based DNS) sends users to different or nearby servers.
> More in [Module 2.1 — Load balancing](../02-building-blocks/01-load-balancing.md).

---

## Layer 3: TCP vs UDP — the two ways to send data

We have an address (IP) and a name lookup (DNS). Now: how do we actually *send the bytes*? Two
transport protocols dominate, and choosing between them is a core design decision.

### TCP — the reliable, ordered conversation

**TCP (Transmission Control Protocol)** gives you a **connection**: a reliable, ordered,
two-way stream of bytes. It's "registered mail with tracking." Before any data flows, the two
machines perform a **3-way handshake** to agree they're both ready:

```
   Client                         Server
     │ ───────── SYN ──────────►  │   "let's talk; my sequence # is x"
     │ ◄──────── SYN-ACK ───────  │   "ok; got x, my seq # is y"
     │ ───────── ACK ──────────►  │   "got y; we're connected"
     │                            │
     │ ══════ data flows ═══════► │   (now reliable, ordered bytes)
```

What TCP gives you, and what it costs:

- **Reliability.** Every packet is acknowledged (ACK'd). Lost packets are detected and
  **retransmitted**. Nothing silently vanishes.
- **Ordering.** Packets are numbered (sequence numbers), so even if they arrive out of order,
  TCP reassembles them in the right sequence before handing them to your app.
- **Flow control.** The receiver advertises how much it can handle (a "window"), so a fast
  sender doesn't overwhelm a slow receiver.
- **Congestion control.** TCP senses network congestion (via packet loss/delay) and *slows
  down* to avoid making a jam worse. This is why a download ramps up gradually.
- **The cost: connection setup latency.** That handshake is a full round trip *before any real
  data moves.* Over a long distance that's tens of milliseconds of pure overhead per new
  connection — which is exactly why we **reuse** connections (keep-alive) and why HTTP/2 and
  connection pooling matter (next lesson).

### UDP — the fast, fire-and-forget datagram

**UDP (User Datagram Protocol)** is "regular postcard mail." There's **no handshake, no
connection, no acknowledgments, no ordering, no retransmission.** You hand UDP a packet
(a **datagram**) and it tries to deliver it. Maybe it arrives, maybe not, maybe out of order.

Why would anyone want that? **Speed and simplicity.** No setup round trip, no waiting to
re-send lost packets, far less per-packet overhead. For some workloads, *a slightly late or lost
piece is worse than useless* — by the time TCP re-sent it, the moment has passed.

```
   TCP:  handshake → ordered, ACK'd, retransmitted stream   (correctness first)
   UDP:  just send the datagram, no promises                (speed first)
```

### When to use which

| Need | Use | Why |
|------|-----|-----|
| Web pages, APIs, file transfer, databases | **TCP** | Cannot lose or reorder bytes |
| Email, SSH, most app traffic | **TCP** | Reliability is non-negotiable |
| Live video/voice calls (VoIP) | **UDP** | A dropped frame is fine; a *late* one is worse |
| Online gaming (position updates) | **UDP** | Want the *newest* state, not a resent old one |
| DNS lookups (small, fast) | **UDP** | One tiny request/reply; retry in the app if needed |
| Live streaming, telemetry | **UDP** | Volume + tolerance for loss |

> **Trade-off in one line:** TCP trades latency for guarantees; UDP trades guarantees for
> latency. Notably, **HTTP/3 is built on UDP** (via a protocol called QUIC) and rebuilds the
> reliability it needs in user space to dodge TCP's setup cost — we'll see that next lesson.

---

## Layer 4 detail: ports — which app on the machine

An IP address finds the *machine*. But a machine runs many programs at once (a web server, a
database, SSH). A **port** is a number (0–65535) that identifies *which program* the packet is
for — the apartment number inside the building.

A connection is therefore identified by a **4-tuple**: `(source IP, source port,
destination IP, destination port)`. That's how your laptop keeps a dozen browser tabs' worth of
connections to the same server straight.

Some **well-known ports** worth memorizing:

| Port | Service |
|------|---------|
| 80 | HTTP |
| 443 | HTTPS |
| 53 | DNS |
| 22 | SSH |
| 5432 | PostgreSQL |
| 6379 | Redis |

---

## Putting it together: one URL, the full trip

Typing `https://www.example.com` and hitting Enter triggers, roughly:

```
1. DNS:   www.example.com ──► 93.184.216.34   (cached if seen recently)
2. TCP:   3-way handshake to 93.184.216.34 : port 443
3. TLS:   secure handshake (next lesson)
4. HTTP:  GET / over the encrypted, reliable connection
5. Server responds with HTML; browser renders; fetches more assets
```

Every system you design in this course rides on exactly this foundation.

---

## Trade-offs & key takeaways

- **Layers = separation of concerns.** IP addresses machines; DNS names them; TCP/UDP move the
  bytes; ports pick the app. Each trusts the layer below.
- **IP is best-effort.** Reliability is a *choice* you make at the transport layer.
- **DNS TTL is freshness vs. load.** Short TTL = fast changes, more lookups; long TTL = the
  opposite. There's no free lunch.
- **TCP vs UDP is the canonical latency-vs-guarantees trade-off.** Default to TCP; reach for
  UDP when "newest and fastest" beats "complete and ordered."
- **Connection setup isn't free.** The TCP (and TLS) handshake is real latency — reuse
  connections whenever you can.

---

## In the wild

- **Public DNS resolvers** like Google `8.8.8.8` and Cloudflare `1.1.1.1` are faster, well-cached
  recursive resolvers many people use instead of their ISP's.
- **DNS-based failover & geo-routing**: providers like Route 53 and Cloudflare return different
  IPs based on health checks and user location — DNS as a first layer of load balancing.
- **QUIC/HTTP3** (Google, then everyone) runs over **UDP** to eliminate TCP's handshake and
  head-of-line blocking — the clearest modern example of "UDP, but we rebuild reliability."
- **Video conferencing** (Zoom, WebRTC) leans on **UDP**: better to drop a frame than freeze.

---

## Interview angle

Networking rarely *is* the whole question, but it's the substrate. The high-value moves: explain
the **DNS resolution chain** (recursive resolver → root → TLD → authoritative) and **TTL
caching trade-offs**; nail the **TCP 3-way handshake** and *why connection setup latency
matters* (it motivates keep-alive, pooling, HTTP/2); and give a crisp **TCP-vs-UDP** decision
with a concrete example each way. Bonus depth: "HTTP/3 uses UDP via QUIC to avoid TCP's
head-of-line blocking and handshake cost."

**Common follow-ups:**
- "You changed your server's IP but old users still hit the old one. Why?" → DNS TTL caching;
  lower the TTL *before* the change.
- "Why is the first request to a new server slower than the rest?" → DNS lookup + TCP handshake
  + TLS handshake on the cold path; subsequent requests reuse the warm connection.
- "Would you use TCP or UDP for a multiplayer game's position updates? Why?" → UDP — you want
  the freshest position, and a retransmitted stale one is worthless.

---

## Self-check

1. Walk through what happens, layer by layer, between typing `myapp.com` and the first byte of
   the HTML arriving. Where does each of IP, DNS, TCP, and ports come into play?
2. Your DNS records have a 24-hour TTL and you need to migrate to a new IP tomorrow. What do you
   do today, and what's the trade-off of that change?
3. Give one workload where UDP is clearly the right call and explain *why TCP would be worse*,
   not just "UDP is faster."
4. What three guarantees does TCP add on top of IP, and what's the price you pay for them?
5. Two browser tabs both connect to `443` on the same server IP. How does the OS keep their
   data from getting mixed up?

---

**Next:** [1.2 — HTTP, HTTPS & REST APIs »](02-http-https-rest.md)
