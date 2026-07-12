# 3.5 — Failure, Redundancy & Fault Tolerance

> **Module 3 · Distributed Systems** · ~30 min read
> *In a single program, a crash is an emergency. In a distributed system with thousands of
> machines, something is* always *broken — a disk, a network link, a whole datacenter. The shift in
> mindset that separates senior engineers from juniors: stop treating failure as exceptional and
> start designing for it as the* normal, expected *case.*

---

## The problem

You design a service assuming the happy path: the request arrives, the database answers, the
response goes back. Then production happens:

- A disk fails. A power supply dies. A whole rack loses network.
- A dependency you call is slow — not *down*, just slow — and your threads pile up waiting for it
  until your *own* service falls over.
- A deploy ships a bug. A cable gets unplugged. A cloud region has a bad day.

With one server, the chance of failure on any given day is small. With **10,000 servers**, if each
has a tiny daily failure probability, **multiple are failing every single day** — guaranteed. At
scale, failure isn't an edge case; it's the steady state.

> **Analogy.** A commercial airliner. Engineers don't assume nothing will fail — they assume things
> *will* and design so the plane flies anyway: two engines (it flies on one), redundant hydraulics,
> backup instruments, checklists for every failure mode. **Fault tolerance is engineering the
> system to keep working despite its parts breaking.** That's the standard we want.

**Fault tolerance** = the system continues to function (perhaps degraded) when components fail.
The goal is never "no failures" (impossible) — it's **no single failure takes down the whole
system**, and failures stay **contained**.

---

## Core idea

You can't prevent failures, so you do two things:

1. **Add redundancy** so any single failure has a backup ready (no single point of failure).
2. **Contain and absorb** failures so one broken part doesn't cascade into total collapse (retries,
   timeouts, circuit breakers, bulkheads, graceful degradation).

Underneath it all is one repeated tactic: **isolate, detect, and recover.** Let's build up the
toolkit.

### Types of failure

Knowing the failure *shape* tells you the right defense:

- **Crash (fail-stop):** a process/machine just stops. Easiest — others take over.
- **Omission:** messages get dropped (network loss). Handle with retries + timeouts.
- **Timing / slow:** a component responds, but *too slowly*. **The most insidious** — a slow
  dependency is often worse than a dead one because callers pile up waiting.
- **Partition:** the network splits the cluster into groups that can't talk (see split-brain in
  [3.4](04-coordination-leader-election.md)).
- **Byzantine:** a component behaves arbitrarily/maliciously (lies, sends garbage). Rare outside
  adversarial settings (blockchains, aerospace); most systems assume non-Byzantine.

---

## How it works: the fault-tolerance toolkit

### Redundancy & replication

The bedrock. Run **multiple copies** of everything so a backup is ready:

- Stateless services: run **N instances** behind a load balancer ([Module 2.1](../02-building-blocks/01-load-balancing.md)).
- Stateful data: **replicate** it ([Module 2.6](../02-building-blocks/06-replication.md)) across
  machines, **availability zones**, even **regions** — so a whole-datacenter loss doesn't lose data.

> **Active-active vs active-passive.** *Active-passive*: a standby sits idle until the primary dies,
> then takes over (simpler, wasted capacity). *Active-active*: all copies serve traffic
> simultaneously (better utilization, harder to keep consistent). A core redundancy trade-off.

### Failover

**Failover** is the act of switching to a redundant component when one fails. It requires three
things, each a place it can go wrong:

```
   1. DETECT failure   (health checks / missed heartbeats)  ← too eager = false alarms
   2. PROMOTE backup   (replica → primary, reroute traffic) ← must avoid two primaries
   3. RECOVER          (rejoin the healed node as a backup)
```

The danger: failover *too fast* on a transient blip causes thrashing; *too slow* means downtime.
And promoting a backup while the old primary is merely *slow* (not dead) risks **split-brain** —
which is exactly why leader election and quorums ([3.1](01-consensus-raft-paxos.md),
[3.4](04-coordination-leader-election.md)) matter.

### Timeouts — the foundation of not-hanging

**Never wait forever.** Every network call gets a **timeout**. Without one, a single slow
dependency holds your thread/connection hostage; enough hung calls and you exhaust your resources
and go down too. A timeout converts "hangs forever" into "fails fast," which you can then handle.
Set it from real latency data (e.g. a bit above p99), not a guess.

### Retries with exponential backoff + jitter

Many failures are **transient** (a blip, a brief overload). Retrying often works — but naive
retrying makes things worse:

- Retry **immediately and repeatedly** → you hammer an already-struggling service.
- Everyone retries **on the same schedule** → synchronized waves of load (a *thundering herd*).

The standard recipe: **exponential backoff with jitter.**

```
   attempt 1 → wait ~1s   ┐
   attempt 2 → wait ~2s   │ exponential: double each time
   attempt 3 → wait ~4s   ┘
   + JITTER: randomize each wait (e.g. random between 0 and the cap)
     so clients don't all retry at the exact same instant
```

Backoff gives the dependency room to recover; jitter spreads the retries out so they don't all land
together. **Crucial caveat:** only retry operations that are **idempotent**, or you risk doing the
thing twice (double charge, double order).

### Idempotency for safe retries

Because retries can duplicate, every retried operation must be **idempotent** — running it twice
has the same effect as once. Attach an **idempotency key**; the server dedupes. This is the bridge
between "retry for reliability" and "don't corrupt data," and it's covered in depth in
[Module 2.14](../02-building-blocks/14-idempotency.md).

### Circuit breakers — stop beating a dead horse

If a dependency is clearly down, retrying *at all* just wastes resources and slows you down. A
**circuit breaker** wraps calls to a dependency and tracks failures, modeled like an electrical
breaker with three states:

```
   CLOSED ──(too many failures)──► OPEN ──(after cooldown)──► HALF-OPEN
     ▲  normal: calls pass         │ fail fast,              │ allow a few trial calls
     │                             │ don't even try          │
     └────────(trial succeeds)─────┴───────────(trial fails)─┘
```

- **Closed:** calls flow normally; count failures.
- **Open:** the failure threshold tripped — **fail instantly** without even attempting the call (so
  you don't pile up on a dead dependency). After a cooldown, move to half-open.
- **Half-open:** let a few test calls through; if they succeed, close; if not, open again.

This prevents **cascading failure** — where one slow service drags down everything that calls it,
which drags down everything that calls *them*.

### Bulkheads — isolate the blast radius

Named after a ship's watertight compartments: a hull breach floods one compartment, not the whole
ship. In software, **bulkheading** means partitioning resources so one overloaded part can't starve
the rest — e.g. separate connection/thread pools per dependency, or isolating a noisy tenant.

```
   ┌─ pool A ─┐ ┌─ pool B ─┐ ┌─ pool C ─┐   dependency B saturates pool B,
   │ ████░░░░ │ │ ████████ │ │ █░░░░░░░ │   but A and C keep serving.
   └──────────┘ └──────────┘ └──────────┘
```

Without bulkheads, one slow dependency consuming *all* your threads takes down requests that didn't
even need that dependency.

### Graceful degradation & load shedding

When you can't serve everything, serve *something* useful instead of failing entirely:

- **Graceful degradation:** drop to a reduced experience. Recommendations service down? Show a
  generic list instead of a blank page. Return a slightly stale cached value instead of erroring.
- **Load shedding:** under overload, **deliberately reject some requests** (e.g. low-priority ones)
  to protect the system's ability to serve the rest. A controlled `503` beats a total meltdown.
  This is the same instinct as the rate limiter ([4.1](../04-case-studies/01-rate-limiter/README.md)),
  applied for self-preservation.

### Eliminating single points of failure (SPOF)

A **SPOF** is any component whose failure alone takes down the system. The discipline: trace every
request path and ask "what if *this* dies?" If the answer is "everything stops," add redundancy.
The load balancer, the database primary, the DNS, the coordination service — each needs a backup or
quorum so no single box is fatal.

### Chaos engineering

How do you *know* your fault tolerance works? You **deliberately break things in production** (or a
prod-like environment) and verify the system survives. Pioneered by Netflix's **Chaos Monkey**,
which randomly kills instances during business hours — forcing engineers to build services that
tolerate it. The philosophy: **the best way to be confident you survive failure is to fail on
purpose, regularly, while you're watching.**

---

## Availability math (recap)

Fault tolerance exists to protect **availability** — the fraction of time the system is up,
counted in "nines":

| Availability | Downtime per year | Nickname |
|--------------|-------------------|----------|
| 99% | ~3.65 days | "two nines" |
| 99.9% | ~8.8 hours | "three nines" |
| 99.99% | ~52 minutes | "four nines" |
| 99.999% | ~5 minutes | "five nines" |

Two rules to internalize:

- **Components in series multiply** and make things *worse*. If your request needs services A, B, C
  in sequence, each 99.9%, total ≈ 0.999³ ≈ **99.7%**. Every dependency you add *drops* availability.
- **Redundant components in parallel** make things *better*. Two independent 99% instances where you
  only need one up: failure only if *both* fail = 0.01 × 0.01 → availability ≈ **99.99%**.

This is the quantitative argument for redundancy and for minimizing hard dependencies.

---

## Trade-offs & key takeaways

- **Assume failure; design for it.** It's the normal case at scale, not an exception.
- **Redundancy removes SPOFs** but costs money and adds consistency challenges (replication lag,
  failover coordination, active-active conflicts).
- **Timeouts + retries (with backoff & jitter) + idempotency** are the inseparable trio for
  surviving transient faults safely — retries without idempotency cause duplicates; without backoff
  cause herds.
- **Circuit breakers and bulkheads stop cascades** — the difference between one service degrading
  and your *whole* system collapsing.
- **Graceful degradation and load shedding** turn "total outage" into "reduced service."
- **Test it with chaos engineering** — untested fault tolerance is a hopeful guess.

---

## In the wild

- **Netflix** built **Chaos Monkey** (and the broader Simian Army) and popularized circuit breakers
  via the **Hystrix** library; resilience patterns are now standard in service meshes (Istio,
  Envoy) and libraries (resilience4j).
- **AWS** structures regions into independent **Availability Zones** specifically so you can run
  redundant infrastructure that survives a datacenter-level failure.
- **Every major cloud SDK** ships retries with exponential backoff and jitter by default — because
  the naive version causes outages.

---

## Interview angle

When asked "what happens when X fails?" — and you *will* be asked — don't say "it won't." Walk the
toolkit: **redundancy** (no SPOF) → **health checks + failover** → **timeouts** so you fail fast →
**retries with backoff + jitter** (only if idempotent) → **circuit breakers** to stop cascades →
**bulkheads** to contain blast radius → **graceful degradation / load shedding** under overload.
Then quantify with **availability math**: "each dependency in series lowers availability, so I'd
minimize hard dependencies and add parallel redundancy where it counts." Mentioning you'd **verify
with chaos testing** is a strong closer.

**Common follow-ups:**
- "A downstream service gets slow (not down) — what happens to you?" → threads pile up on the slow
  call; fix with **timeouts**, **circuit breakers**, **bulkheads**.
- "Why add jitter to retries?" → prevents synchronized retry waves (thundering herd).
- "Is it safe to retry a payment request?" → only if it's idempotent (idempotency key); otherwise
  you risk double-charging.
- "Your cache dies and all traffic hits the DB." → cascading failure; mitigate with load shedding,
  request coalescing, graceful degradation to stale data.

---

## Self-check

1. Why is a *slow* dependency often more dangerous than a *dead* one, and which tools address it?
2. Explain exponential backoff with jitter. What goes wrong if you drop the backoff? If you drop
   the jitter?
3. Walk through the three states of a circuit breaker and what each one prevents.
4. Three services in series, each 99.9% available — what's the combined availability, and what does
   that tell you about adding dependencies?
5. What is graceful degradation, and how does it differ from load shedding?

---

**Next:** [3.6 — Observability: Logs, Metrics & Traces »](06-observability.md)
