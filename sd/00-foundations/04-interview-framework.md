# 0.4 — A Framework for System Design Interviews

> **Module 0 · Foundations** · ~26 min read
> *The system design interview isn't a quiz with right answers — it's a 45-minute conversation
> where you show how you think. This lesson gives you a repeatable 4-step structure so you never
> freeze, never ramble, and always end up somewhere sensible.*

---

## The problem

You're handed a deliberately vague prompt: **"Design Twitter."** That's it. There's no spec, the
scope is enormous, and you have ~45 minutes. Most people do one of two bad things: they **freeze**
(stare at the whiteboard, unsure where to start), or they **dive** (immediately start drawing
databases before they know what they're even building). Both fail.

> **Analogy.** It's like being told "build a house" with no blueprint. A bad builder starts laying
> bricks at a random corner. A good builder *asks questions first* ("how many bedrooms? what's the
> budget? what's the climate?"), sketches the floor plan, then goes deep on the tricky parts (the
> foundation, the plumbing), and finally walks the client through trade-offs ("we used a flat roof
> to save cost — here's the maintenance implication"). The interview rewards the second builder.

The vagueness is **intentional.** The interviewer wants to see whether you can **impose
structure** on an open-ended problem, drive the conversation, and reason about trade-offs out loud.
Remember the course's golden rule: **everything is a trade-off** — there's no single right design,
only choices justified by constraints.

The fix is a framework you run *every single time*, so the structure is automatic and your brain
is free to think about the actual problem.

---

## Core idea: the 4-step framework

```
  ┌──────────────────────────────────────────────────────────────────────┐
  │  1. UNDERSTAND   → requirements & scope (functional + non-functional)  │
  │  2. HIGH-LEVEL   → APIs, data model, box diagram                       │
  │  3. DEEP DIVE    → pick interesting components; bottlenecks & scaling   │
  │  4. WRAP UP      → failure modes, monitoring, trade-offs recap          │
  └──────────────────────────────────────────────────────────────────────┘
```

### Time budget for a 45-minute interview

Discipline here is what separates a complete interview from a half-finished one. **Watch the
clock.** A rough allocation:

| Step | Time | What you're producing |
|------|------|-----------------------|
| 1. Understand requirements & scope | ~5–10 min | Agreed functional + non-functional reqs; scale numbers |
| 2. High-level design | ~10–15 min | APIs, data model, a box-and-arrow diagram |
| 3. Deep dive | ~10–15 min | 1–2 components explored deeply; scaling & bottlenecks |
| 4. Wrap up | ~5 min | Failure modes, monitoring, trade-off recap, "what's next" |

The most common timing failure is **spending 25 minutes on requirements and high-level**, then
never getting to a deep dive — which is where senior signal lives. Budget, and move on.

---

## Step 1 — Understand requirements & scope (~5–10 min)

**Never start designing until you know what you're designing.** The interviewer is *testing*
whether you'll scope the problem down to something buildable in 45 minutes. They will happily watch
you build the wrong thing if you don't ask.

Split requirements into two kinds:

- **Functional requirements** — *what the system does.* Features. "Users can post a tweet." "Users
  see a feed of people they follow." "Users can search."
- **Non-functional requirements (NFRs)** — *how well it does it.* The qualities: scale, latency,
  availability, consistency, durability. These are what actually shape the architecture.

> **Analogy.** Functional = "the car must drive and brake." Non-functional = "it must do 0–60 in 4
> seconds, seat 5, and survive a crash." The non-functional requirements decide whether you build a
> sports car or a minivan — same "drives and brakes," totally different design.

### The clarifying-questions checklist

Ask a focused handful (not all of them — pick what matters for *this* system):

**Scope / features**
- What are the **core features** we must support? (Get the interviewer to pick the top 2–3.)
- What's explicitly **out of scope**? (Just as important — shrinks the problem.)
- Who are the **users / clients**? (Mobile, web, other services?)

**Scale** (drives everything — see [Lesson 0.3](03-back-of-envelope-estimation.md))
- How many **DAU / total users**?
- What's the **read:write ratio**? (The single most clarifying question.)
- Expected **QPS**, peak vs. average?
- **Data size** per item and **growth/retention**?

**Non-functional priorities**
- **Latency** target? (Is this a real-time system or is a few seconds fine?)
- **Availability** target? (How many nines — what's the cost of downtime?)
- **Consistency** needs? **Strong** (everyone sees the same value immediately, e.g. bank balance)
  or is **eventual** consistency (replicas converge "soon," e.g. a like count) acceptable? This
  is the **CAP trade-off** (Module 2.9) and it shapes your whole data layer.
- **Durability** — can we ever lose data? (Payments: never. A "typing…" indicator: who cares.)

Then **do the back-of-the-envelope math** (Lesson 0.3) out loud: turn DAU into peak QPS and a
storage estimate. This bridges Step 1 into Step 2 and proves the scale you're designing for.

> **Senior signal:** ending Step 1 by restating the agreed scope — *"So: post tweets, follow users,
> read a home feed. ~300M DAU, ~450K peak read QPS, read-heavy, eventual consistency on the feed is
> fine, high availability over strict consistency. Sound right?"* Now you both know the target.

---

## Step 2 — High-level design (~10–15 min)

Now sketch the architecture at the **box-and-arrow** level. Three deliverables: the **API**, the
**data model**, and the **component diagram**. Keep it high-level — resist diving into any one box
yet.

### 2a. Define the API

A few endpoints make the system concrete and reveal the data flow. Keep them simple:

```
  POST /tweets        { text, mediaIds }        → 201 { tweetId }
  GET  /feed?cursor=  → 200 { tweets[], nextCursor }
  POST /follow        { targetUserId }          → 200
```

Mentioning **pagination via cursor** (not offset), **auth**, and **idempotency keys** for writes
(Module 2.14) here is cheap and scores well.

### 2b. Sketch the data model

What are the core entities and how are they stored? A couple of tables/collections:

```
  User   (id, handle, name, created_at)
  Tweet  (id, author_id, text, media_ids, created_at)
  Follow (follower_id, followee_id, created_at)
```

This is also the moment to make the **SQL vs. NoSQL** call (Module 2.4) and justify it with your
NFRs: "relational for users/follows where we want joins and integrity; the feed itself we'll
precompute and cache."

### 2c. Draw the box diagram

Walk the request path. Reuse the architecture from
[Lesson 0.1](01-scale-zero-to-millions.md) — clients → gateway/LB → services → cache → DB, plus a
queue for async work:

```
                       ┌──────────► [ CDN ] (media, static)
   Clients ──► [ API Gateway / LB ] ──► [ App / Service tier ] (stateless)
                                              │          │
                                          [ Cache ]   [ Queue ] ──► [ Workers ]
                                              │
                                       ┌──────┴───────┐
                                  [ Primary DB ]  [ Read replicas ]
                                          │
                                    [ Shards... ]   [ Blob store ] (media)
```

Talk through one read and one write as you draw. Don't optimize yet — get a working skeleton on the
board that satisfies the functional requirements. **Then** Step 3 makes it scale.

---

## Step 3 — Deep dive (~10–15 min)

This is where the interview is won or lost. Pick **one or two interesting components** and go deep:
discuss bottlenecks, scaling, and the relevant **building blocks**. Let the interviewer steer ("can
you go deeper on the feed?"), or propose the most interesting part yourself.

The deep dive is essentially "**which building block solves this bottleneck?**" — and that's
exactly what [Module 2](../02-building-blocks/) is. Map the pain to the brick:

| Bottleneck you hit | Building block to reach for |
|--------------------|-----------------------------|
| Too many requests for one web box | [Load balancing](../02-building-blocks/01-load-balancing.md) + stateless horizontal scaling |
| Reads hammering the DB | [Caching](../02-building-blocks/03-caching.md) + [read replicas](../02-building-blocks/06-replication.md) |
| Global users, slow static/media | [CDN / reverse proxy](../02-building-blocks/02-reverse-proxy-cdn.md) |
| One DB can't hold the data / writes | [Sharding & partitioning](../02-building-blocks/07-sharding-partitioning.md) |
| Adding/removing shards reshuffles everything | [Consistent hashing](../02-building-blocks/08-consistent-hashing.md) |
| Slow work blocking the request | [Message queues](../02-building-blocks/10-message-queues-streaming.md) + async workers |
| Need to cap abusive traffic | [Rate limiting](../02-building-blocks/11-rate-limiting.md) |
| Unique IDs across many machines | [Distributed ID generation](../02-building-blocks/12-unique-id-generation.md) |
| Strong vs. eventual consistency, partitions | [CAP / PACELC](../02-building-blocks/09-cap-pacelc-consistency.md) |
| "Have I seen this item?" at huge scale | [Probabilistic structures](../02-building-blocks/13-probabilistic-structures.md) |
| Don't double-charge on a retry | [Idempotency](../02-building-blocks/14-idempotency.md) |

> **Worked deep dive (the Twitter feed).** "How does the home feed scale to 450K peak reads/sec?"
> → The choice is **fan-out on write vs. on read** (Module 4.7). Fan-out on write precomputes each
> user's feed when a tweet is posted (fast reads, expensive writes, painful for celebrities with
> millions of followers — the **hot-key problem**). Fan-out on read assembles the feed at read time
> (cheap writes, slow reads). The senior answer: a **hybrid** — fan-out on write for normal users,
> fan-out on read for celebrity accounts. *That's trade-off reasoning made visible.*

The pattern for any deep dive: **state the bottleneck → propose an approach → name its trade-off →
offer the refinement.** Always say *why*, and always acknowledge what your choice costs.

---

## Step 4 — Wrap up (~5 min)

Don't let the interview just run out of time. Land the plane with a deliberate wrap-up — it leaves
a strong final impression and shows operational maturity.

Cover, briefly:

- **Bottlenecks & next scaling steps** — "If writes grow 10×, the next move is sharding tweets by
  author_id." Show you know where it breaks next.
- **Failure modes** — "If the cache dies, we risk a stampede onto the DB — mitigate with request
  coalescing and TTL jitter. If the primary DB fails, a replica is promoted, with a small
  data-loss window." (Module 3.5.) Naming failure modes unprompted is a strong senior signal.
- **Monitoring / observability** (Module 3.6) — "We'd track **p99 latency**, error rate, QPS, cache
  hit rate, and queue depth, with alerts on SLO breaches." (Recall from
  [Lesson 0.2](02-numbers-every-engineer-should-know.md): alert on **p99**, not the average.)
- **Trade-off recap** — summarize the 2–3 big decisions and what each cost: "We chose eventual
  consistency on the feed for availability and speed; the cost is a user might see a tweet a second
  late. We chose hybrid fan-out to handle celebrities; the cost is added system complexity."

> Ending on an explicit trade-off recap is the single highest-value closing move. It directly
> demonstrates the one skill the whole interview is testing.

---

## Common mistakes (and the fix)

| Mistake | Why it hurts | Fix |
|---------|--------------|-----|
| Diving into design before clarifying | You build the wrong system | Always run Step 1 first |
| Skipping the scale estimate | Your design isn't grounded in load | Do the envelope math out loud (0.3) |
| Staying high-level the whole time | No depth → no senior signal | Force yourself into a deep dive by ~minute 20 |
| Going *too* deep too early | Run out of time, design incomplete | Get a working skeleton first, then deepen |
| Presenting one design as "correct" | Misses the whole point | Offer alternatives + trade-offs |
| Silence while thinking | Interviewer can't follow you | Narrate everything; think out loud |
| Ignoring the interviewer's hints | They're steering you to the good part | Treat hints as gifts; follow them |
| Forgetting non-functional reqs | Architecture has no constraints to satisfy | Always nail down latency/availability/consistency |

---

## How to drive the conversation

The interview is a **two-way conversation**, and *you* hold the wheel:

- **Think out loud, always.** An unspoken brilliant idea scores zero. Narrate your reasoning,
  including the options you reject and *why*.
- **Lead, but stay steerable.** Propose the next step ("let me design the write path now"), but
  pivot instantly when the interviewer nudges — their hints point at what they want to assess.
- **Manage time visibly.** "I'll keep the data model brief so we have time for the feed deep dive."
- **State assumptions and confirm them.** "I'll assume eventual consistency is fine for the feed —
  okay?" Lets them correct course early.
- **Frame everything as a trade-off.** "We *could* do X, which is simpler but doesn't scale writes;
  or Y, which scales but adds operational complexity. Given our write volume, I'd start with X and
  move to Y when ___." This sentence pattern is the entire game.

---

## Trade-offs & key takeaways

- **Run the same 4 steps every time:** Understand → High-level → Deep dive → Wrap up. Structure
  frees your mind to think about the actual problem.
- **Clarify before you design.** Functional *and* non-functional requirements; the NFRs (scale,
  latency, availability, consistency) shape the architecture.
- **Ground the design in numbers** with back-of-the-envelope math.
- **Budget your time** — the deep dive is where senior signal lives; protect it.
- **The deep dive = mapping bottlenecks to building blocks.** Module 2 is your toolbox.
- **Everything is a trade-off.** Never present a single "right" answer; justify choices by
  constraints and name what each costs.

---

## In the wild

- This framework mirrors how real **design reviews / RFCs / ADRs** work at companies: state the
  problem and requirements, propose a design, discuss alternatives and trade-offs, list failure
  modes and monitoring. The interview is a compressed version of the actual job.
- Senior engineers are valued precisely for the Step-3/Step-4 skills: spotting bottlenecks early
  and reasoning crisply about trade-offs and failure modes — not for memorizing architectures.

---

## Interview angle

The framework *is* the interview angle for this whole module. The meta-signal interviewers grade:
*can this person take an ambiguous problem, structure it, reason about trade-offs out loud, and
drive to a sensible, scoped design?* Hit the four steps, narrate constantly, and always say what
each decision costs.

**Common follow-ups baked into every interview:**
- "How does this scale to 10× the traffic?" → name the next bottleneck and the building block fixing it.
- "What happens when [component] fails?" → failover, redundancy, fail-open/closed, data-loss window.
- "Strong or eventual consistency here?" → tie it to the feature: balances strong, like-counts eventual.
- "How would you monitor this in production?" → p99 latency, error rate, saturation, alerts on SLOs.

---

## Self-check

1. Name the four steps and the rough time budget for each in a 45-minute interview.
2. What's the difference between functional and non-functional requirements? Give one example of
   each for a chat app, and say which kind shapes the architecture more.
3. You're 20 minutes in and still drawing boxes. What should you do, and why does it matter?
4. Give the sentence-pattern for presenting a design decision as a trade-off. Why is this the single
   most important phrasing in the whole interview?
5. The interviewer asks "what happens if your cache goes down?" Name the failure mode and two
   mitigations.

---

**Next:** [1.1 — How the Internet Works: IP, DNS, TCP & UDP »](../01-networking-and-communication/01-internet-ip-dns-tcp-udp.md)
