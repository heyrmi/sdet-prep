# Glossary

Every term in the course, defined in one or two plain-English sentences. Skim it whenever a lesson uses a word you don't recognize.

---

## A

**ACID** — The four guarantees classic SQL databases give a transaction: Atomicity (all-or-nothing), Consistency (rules always hold), Isolation (concurrent transactions don't trip over each other), Durability (committed data survives a crash). The "safe by default" model, traded against scale (see [Module 2.4](02-building-blocks/04-sql-vs-nosql.md)).

**Adaptive bitrate streaming** — A video technique that chops a video into small chunks encoded at several quality levels, so the player switches up or down on the fly based on your current bandwidth — smooth playback instead of buffering (see [Module 4.10](04-case-studies/10-video-streaming/)).

**API gateway** — A single front door in front of many backend services that handles cross-cutting jobs (auth, rate limiting, routing, request shaping) so each service doesn't have to. The trade-off: convenience vs. one more hop and a potential bottleneck (see [Module 1.6](01-networking-and-communication/06-api-gateway-design.md)).

**At-least-once delivery** — A messaging guarantee that a message will be delivered, but possibly more than once on retries — so consumers must be idempotent. Contrast with at-most-once and exactly-once (see [Module 2.14](02-building-blocks/14-idempotency.md)).

**At-most-once delivery** — A messaging guarantee that a message is delivered zero or one times, never duplicated — fast, but you may lose messages. The opposite trade-off from at-least-once.

**Availability** — The fraction of time a system is up and serving requests, usually quoted as a percentage (see "the nines"). The "A" in CAP, often traded against consistency (see [Module 2.9](02-building-blocks/09-cap-pacelc-consistency.md)).

**Exactly-once delivery** — see *Exactly-once delivery* under E.

## B

**Back-of-the-envelope estimation** — Quick rough math (QPS, storage, bandwidth) to size a system before designing it, the way you'd sketch on a napkin. The point is the right order of magnitude, not precision (see [Module 0.3](00-foundations/03-back-of-envelope-estimation.md)).

**Backpressure** — When a downstream component is overwhelmed and signals upstream to slow down, instead of silently dropping work or crashing. Like a checkout line telling the door to stop letting people in (see [Module 2.10](02-building-blocks/10-message-queues-streaming.md)).

**BASE** — The NoSQL counterpart to ACID: Basically Available, Soft state, Eventual consistency. It trades strict guarantees for availability and scale — "loose now, correct soon" (see [Module 2.4](02-building-blocks/04-sql-vs-nosql.md)).

**BFF (Backend for Frontend)** — A dedicated backend tailored to one client type (e.g. a mobile app), shaping data exactly how that client needs instead of forcing one generic API to serve everyone (see [Module 1.6](01-networking-and-communication/06-api-gateway-design.md)).

**Blob / object storage** — Storage for large unstructured files ("blobs" / objects) like images and videos, addressed by a key rather than rows and columns; e.g. Amazon S3. Cheap, durable, and effectively infinite, but not for relational queries (see [Module 4.16](04-case-studies/16-object-storage/)).

**Bloom filter** — A compact probabilistic structure that answers "is this item in the set?" with "definitely no" or "probably yes" (false positives possible, false negatives never). Saves expensive lookups for things that aren't there (see [Module 2.13](02-building-blocks/13-probabilistic-structures.md)).

**Broker** — The server in a messaging system that receives messages from producers and hands them to consumers (e.g. Kafka, RabbitMQ). The middleman that decouples senders from receivers (see [Module 2.10](02-building-blocks/10-message-queues-streaming.md)).

**B-Tree** — The classic database index structure: a balanced tree kept sorted on disk, giving fast reads and in-place updates. Great for read-heavy, random-access workloads; contrast with the write-optimized LSM-Tree (see [Module 3.2](03-distributed-systems/02-storage-engines.md)).

**Bulkhead** — A resilience pattern that isolates resources (e.g. separate thread pools per dependency) so one failing part can't sink the whole ship — named after a ship's watertight compartments (see [Module 3.5](03-distributed-systems/05-fault-tolerance.md)).

## C

**Cache-aside (lazy loading)** — The most common caching pattern: the app checks the cache, and on a miss reads the DB, stores the result, and returns it; on a write it invalidates the cache key. Simple and outage-resilient (see [Module 2.3](02-building-blocks/03-caching.md)).

**Cache hit / miss ratio** — The share of reads served from cache (hit) versus those that fall through to the slower store (miss). A high hit ratio is the whole point of a cache (see [Module 2.3](02-building-blocks/03-caching.md)).

**Cache stampede (thundering herd)** — When a popular cache entry expires or the cache dies and a flood of requests all hit the database at once. Mitigated with request coalescing, TTL jitter, and warm standbys (see [Module 2.3](02-building-blocks/03-caching.md)).

**CAP theorem** — In a distributed system, when a network partition happens you can keep either Consistency or Availability, not both. It frames the core distributed-systems trade-off (see [Module 2.9](02-building-blocks/09-cap-pacelc-consistency.md)).

**Causal consistency** — A consistency model guaranteeing that operations with a cause-and-effect relationship are seen by everyone in the same order, while unrelated operations may differ. Stronger than eventual, weaker than strong (see [Module 2.9](02-building-blocks/09-cap-pacelc-consistency.md)).

**CDN (Content Delivery Network)** — A global network of edge servers that cache static assets close to users, so a request travels metres instead of continents. Slashes latency and offloads your origin (see [Module 2.2](02-building-blocks/02-reverse-proxy-cdn.md)).

**Celebrity / hotspot problem** — When one key (a celebrity's account, a viral post) draws so much traffic it overwhelms its single shard, defeating even hashing. Needs special handling like replication or splitting the key (see [Module 2.7](02-building-blocks/07-sharding-partitioning.md)).

**Chaos engineering** — Deliberately injecting failures (killing servers, adding latency) in production-like environments to prove the system survives them, rather than hoping. "Break it on purpose so it doesn't break by surprise" (see [Module 3.5](03-distributed-systems/05-fault-tolerance.md)).

**Chunking** — Splitting a large file into fixed-size pieces so they can be uploaded, stored, deduplicated, and synced independently. Underpins file-storage and object systems (see [Module 4.11](04-case-studies/11-file-storage/)).

**Circuit breaker** — A guard that stops calling a failing dependency after too many errors, "tripping open" to fail fast and give it time to recover, then testing tentatively before resuming. Prevents one sick service from cascading (see [Module 3.5](03-distributed-systems/05-fault-tolerance.md)).

**Compaction** — The background process in an LSM-Tree that merges and rewrites SSTables to discard stale/deleted data and keep reads fast. The cost LSM pays for fast writes (see [Module 3.2](03-distributed-systems/02-storage-engines.md)).

**Compensating transaction** — In a Saga, an action that undoes a previously completed step when a later step fails (e.g. refund a charge). Since you can't roll back across services, you semantically "un-do" instead (see [Module 3.3](03-distributed-systems/03-distributed-transactions.md)).

**Consensus** — Getting a group of unreliable machines to agree on a single value or order of events despite failures. The hard core of distributed systems, solved by algorithms like Raft and Paxos (see [Module 3.1](03-distributed-systems/01-consensus-raft-paxos.md)).

**Consistent hashing** — A scheme that places servers and keys on a hash ring so that adding or removing a server moves only a small fraction of keys, not nearly all of them like `hash % N` does. Powers sharded caches and databases (see [Module 2.8](02-building-blocks/08-consistent-hashing.md)).

**Consumer** — A process that reads and processes messages from a queue or topic. The receiving end, opposite of a producer (see [Module 2.10](02-building-blocks/10-message-queues-streaming.md)).

**Consumer group** — A set of consumers that share the work of reading a topic, with each partition handled by exactly one member, so you scale processing by adding members (see [Module 4.13](04-case-studies/13-message-queue/)).

**Content-addressed storage** — Storing data under a key derived from its content (a hash), so identical data lands at the same address automatically — the basis of deduplication (see [Module 4.11](04-case-studies/11-file-storage/)).

**Count-min sketch** — A probabilistic structure that estimates how many times each item has appeared using little memory, with counts that can overestimate but never underestimate. Used for frequency/heavy-hitter tracking (see [Module 2.13](02-building-blocks/13-probabilistic-structures.md)).

## D

**Dead-letter queue (DLQ)** — A side queue where messages that repeatedly fail processing are parked for later inspection, instead of blocking the main queue or being lost. A safety net for poison messages (see [Module 2.10](02-building-blocks/10-message-queues-streaming.md)).

**Dedup (deduplication)** — Storing only one copy of identical data and pointing duplicates at it, saving huge space when many files or chunks repeat (see [Module 4.11](04-case-studies/11-file-storage/)).

**Delta sync** — Transferring only the parts of a file that changed rather than the whole file, making sync fast over slow links. How file-sync tools avoid re-uploading gigabytes for a one-line edit (see [Module 4.11](04-case-studies/11-file-storage/)).

**Denormalization** — Deliberately duplicating data across tables/documents to avoid expensive joins and make reads faster, accepting redundancy and harder updates. The reverse of normalization (see [Module 2.4](02-building-blocks/04-sql-vs-nosql.md)).

**DNS (Domain Name System)** — The internet's phone book: it translates a human name like `myapp.com` into an IP address a machine can connect to (see [Module 1.1](01-networking-and-communication/01-internet-ip-dns-tcp-udp.md)).

**Document store** — A NoSQL database that stores semi-structured documents (typically JSON), each self-contained, with flexible schemas; e.g. MongoDB. Good when data is nested and read together (see [Module 2.4](02-building-blocks/04-sql-vs-nosql.md)).

**Double-entry ledger** — An accounting model where every transaction is recorded as two balanced entries (a debit and a credit), so the books always sum to zero and money can't silently appear or vanish. The backbone of correct payment systems (see [Module 4.17](04-case-studies/17-payment-system/)).

## E

**Edge server** — A CDN/cache machine located physically near users (at the network "edge") that serves cached content so requests don't travel to the distant origin (see [Module 2.2](02-building-blocks/02-reverse-proxy-cdn.md)).

**Erasure coding** — A storage technique that splits data into fragments plus computed parity fragments, so the original survives losing some fragments — durability like replication but using far less space (see [Module 4.16](04-case-studies/16-object-storage/)).

**Error budget** — The allowed amount of unreliability implied by an SLO (e.g. 99.9% uptime leaves ~43 min/month of downtime). Spend it on risky deploys; burn it and you freeze changes (see [Module 3.6](03-distributed-systems/06-observability.md)).

**Eventual consistency** — A guarantee that if writes stop, all replicas will *eventually* agree, but for a while reads may return stale data. The price of high availability under replication lag (see [Module 2.9](02-building-blocks/09-cap-pacelc-consistency.md)).

**Eviction** — Removing entries from a full cache to make room for new ones, governed by a policy like LRU, LFU, or FIFO (see [Module 2.3](02-building-blocks/03-caching.md)).

**Exactly-once delivery** — The strongest (and hardest) messaging guarantee: each message takes effect once and only once, no losses and no duplicates. Usually achieved as at-least-once delivery plus idempotency rather than true once-only delivery (see [Module 2.14](02-building-blocks/14-idempotency.md)).

**Exponential backoff** — A retry strategy that waits progressively longer between attempts (1s, 2s, 4s...) so a struggling service isn't hammered. Usually paired with jitter (see [Module 3.5](03-distributed-systems/05-fault-tolerance.md)).

## F

**Failover** — Automatically switching to a standby component (e.g. promoting a replica to primary) when the active one fails, so the system keeps running (see [Module 2.6](02-building-blocks/06-replication.md)).

**Fan-out** — Delivering one event to many destinations, such as pushing a post to all of a user's followers (see [Module 4.7](04-case-studies/07-news-feed/)).

**Fan-out on write** — Doing the fan-out work when content is created (push it into each follower's feed up front), making reads cheap but writes expensive — bad for celebrities with millions of followers (see [Module 4.7](04-case-studies/07-news-feed/)).

**Fan-out on read** — Doing the fan-out work when a feed is requested (gather posts from everyone you follow at read time), making writes cheap but reads expensive. The other half of the news-feed trade-off (see [Module 4.7](04-case-studies/07-news-feed/)).

**Fencing token** — A monotonically increasing number handed out with a lock, so a delayed/zombie holder can be rejected when it finally acts (its token is stale). Prevents split-brain damage during leader changes (see [Module 3.4](03-distributed-systems/04-coordination-leader-election.md)).

**FIFO (First In, First Out)** — A cache eviction policy that evicts the oldest-inserted entry regardless of how often it's used. Simple but ignores popularity, unlike LRU/LFU (see [Module 2.3](02-building-blocks/03-caching.md)).

**Forward proxy** — A server that sits in front of *clients* and makes outbound requests on their behalf (for filtering, caching, anonymity). The mirror image of a reverse proxy (see [Module 2.2](02-building-blocks/02-reverse-proxy-cdn.md)).

## G

**Geohash** — Encoding a latitude/longitude into a short string where shared prefixes mean physical nearness, enabling fast "what's near me?" lookups. A common spatial-indexing trick (see [Module 4.12](04-case-studies/12-proximity-service/)).

**Graceful degradation** — Designing a system to drop to reduced but useful functionality under stress or partial failure (e.g. serve stale data, hide non-essential features) instead of failing completely (see [Module 3.5](03-distributed-systems/05-fault-tolerance.md)).

**Graph database** — A database that stores entities as nodes and relationships as edges, optimized for traversing connections (friends-of-friends, recommendations); e.g. Neo4j (see [Module 2.4](02-building-blocks/04-sql-vs-nosql.md)).

**GraphQL** — A query language and API style where the client asks for exactly the fields it wants in one request, avoiding over- and under-fetching. The trade-off vs. REST: flexibility for the client, more server complexity and caching difficulty (see [Module 1.4](01-networking-and-communication/04-graphql.md)).

**gRPC** — A high-performance RPC framework using HTTP/2 and Protocol Buffers for fast, strongly-typed service-to-service calls. Great inside a backend, less friendly to browsers than REST (see [Module 1.3](01-networking-and-communication/03-rpc-grpc.md)).

## H

**Hinted handoff** — In a leaderless system, when a target replica is down a peer temporarily holds the write (a "hint") and forwards it once the replica returns. Keeps writes available during brief outages (see [Module 4.2](04-case-studies/02-key-value-store/)).

**Horizontal scaling (scale out)** — Adding more machines to share load. No hard ceiling and gains redundancy, but requires stateless services and coordination — how all large systems scale (see [Module 0.1](00-foundations/01-scale-zero-to-millions.md)).

**Hotspot key** — A single cache or shard key receiving disproportionate traffic, creating a bottleneck on one node. See also the celebrity/hotspot problem (see [Module 2.3](02-building-blocks/03-caching.md)).

**HTTP/1.1** — The long-standing web protocol where each request waits its turn on a connection (head-of-line blocking), often needing multiple connections for parallelism (see [Module 1.2](01-networking-and-communication/02-http-https-rest.md)).

**HTTP/2** — An HTTP version that multiplexes many requests over one connection and compresses headers, fixing much of HTTP/1.1's slowness (see [Module 1.2](01-networking-and-communication/02-http-https-rest.md)).

**HTTP/3** — The newest HTTP version, running over QUIC (on UDP) instead of TCP to eliminate connection-level head-of-line blocking and speed up setup, especially on lossy networks (see [Module 1.2](01-networking-and-communication/02-http-https-rest.md)).

**HyperLogLog** — A probabilistic structure that estimates the number of *distinct* items (cardinality) in a huge stream using tiny memory, accepting a small error. Used for "unique visitors" type counts (see [Module 2.13](02-building-blocks/13-probabilistic-structures.md)).

## I

**Idempotency** — A property where doing the same operation multiple times has the same effect as doing it once, so retries are safe. The key to surviving at-least-once delivery (see [Module 2.14](02-building-blocks/14-idempotency.md)).

**Idempotency key** — A unique client-supplied ID attached to a request so the server can detect and ignore duplicate retries, returning the original result. How payment APIs avoid double-charging (see [Module 4.17](04-case-studies/17-payment-system/)).

**Index** — A secondary data structure (often a B-Tree) that lets the database find rows by a column without scanning every row — like a book's index. Speeds reads, slows writes and uses space (see [Module 2.5](02-building-blocks/05-indexing.md)).

## J

**Jitter** — Adding randomness to retry/backoff timings so many clients don't retry in lockstep and re-stampede the server. Small noise that prevents synchronized thundering herds (see [Module 3.5](03-distributed-systems/05-fault-tolerance.md)).

## K

**Key-value store** — The simplest NoSQL model: store and fetch a value by its key, like a giant hash map; e.g. Redis, DynamoDB. Blazing fast for lookups, limited for complex queries (see [Module 2.4](02-building-blocks/04-sql-vs-nosql.md)).

## L

**L4 load balancer** — A load balancer working at the transport layer (TCP/UDP), routing by IP and port without seeing request contents. Fast and protocol-agnostic, but less smart than L7 (see [Module 2.1](02-building-blocks/01-load-balancing.md)).

**L7 load balancer** — A load balancer working at the application layer (HTTP), able to route by URL, headers, or cookies. More flexible than L4 at the cost of more processing (see [Module 2.1](02-building-blocks/01-load-balancing.md)).

**Last-write-wins (LWW)** — A conflict-resolution rule that keeps the write with the latest timestamp and discards the rest. Simple, but can silently lose concurrent updates (see [Module 4.2](04-case-studies/02-key-value-store/)).

**Latency** — How long a single operation takes from request to response, e.g. milliseconds per query. Distinct from throughput, which is how many you handle per second (see [Module 0.2](00-foundations/02-numbers-every-engineer-should-know.md)).

**Leader election** — The process by which a cluster picks one node to coordinate (the leader), and re-picks if it dies. A core use of consensus, often backed by ZooKeeper/etcd (see [Module 3.4](03-distributed-systems/04-coordination-leader-election.md)).

**Leader-follower replication** — A scheme with one primary (leader) handling writes and replicas (followers) copying its data to serve reads. Scales reads and enables failover, but introduces replication lag (see [Module 2.6](02-building-blocks/06-replication.md)).

**Leaderless replication** — Replication with no single primary; any replica accepts writes and consistency is reached via quorums (W+R>N). Highly available, but you manage conflicts (e.g. Dynamo-style) (see [Module 2.6](02-building-blocks/06-replication.md)).

**Leaky bucket** — A rate-limiting algorithm that processes requests at a steady fixed rate from a queue (the bucket "leaks" evenly), smoothing bursts. Contrast with the burst-friendly token bucket (see [Module 2.11](02-building-blocks/11-rate-limiting.md)).

**Least-connections** — A load-balancing strategy that sends each new request to the server currently handling the fewest connections, adapting to uneven request durations better than round-robin (see [Module 2.1](02-building-blocks/01-load-balancing.md)).

**LFU (Least Frequently Used)** — A cache eviction policy that evicts the entry accessed the fewest times, keeping long-term popular data. Contrast with LRU's focus on recency (see [Module 2.3](02-building-blocks/03-caching.md)).

**Linearizability** — The strongest single-object consistency guarantee: every read sees the most recent write, as if there were one copy and operations happened instantly in order. Easy to reason about, costly at scale (see [Module 2.9](02-building-blocks/09-cap-pacelc-consistency.md)).

**Load balancer (LB)** — A component that spreads incoming requests across many backend servers, removing them as a single point of failure and making horizontal scaling easy (see [Module 2.1](02-building-blocks/01-load-balancing.md)).

**Load shedding** — Deliberately dropping or rejecting low-priority requests when overloaded to keep the system alive for the rest — better to serve some than crash for all (see [Module 3.5](03-distributed-systems/05-fault-tolerance.md)).

**Logging** — Recording discrete events and errors as the system runs, the first pillar of observability — the "what happened" record (see [Module 3.6](03-distributed-systems/06-observability.md)).

**Long polling** — A near-real-time technique where the client makes a request that the server holds open until it has data, then immediately reconnects. A simpler-than-WebSocket way to push updates (see [Module 1.5](01-networking-and-communication/05-polling-sse-websockets.md)).

**LRU (Least Recently Used)** — The default cache eviction policy: throw out whatever hasn't been touched for the longest, betting recent use predicts future use (see [Module 2.3](02-building-blocks/03-caching.md)).

**LSM-Tree (Log-Structured Merge-Tree)** — A write-optimized storage engine that buffers writes in memory (a memtable) and flushes sorted files (SSTables) to disk, merging them later via compaction. Fast writes, the cost is read amplification and background work; contrast with B-Tree (see [Module 3.2](03-distributed-systems/02-storage-engines.md)).

## M

**Memtable** — The in-memory sorted buffer of an LSM-Tree where new writes land first, before being flushed to disk as an SSTable. Backed by a WAL so a crash doesn't lose it (see [Module 3.2](03-distributed-systems/02-storage-engines.md)).

**Message queue** — A buffer that lets a producer hand off work to be processed later by a consumer, decoupling them so slow work doesn't block the user and spikes are absorbed (see [Module 2.10](02-building-blocks/10-message-queues-streaming.md)).

**Metrics** — Numeric measurements over time (QPS, latency, error rate, CPU), the second pillar of observability — the "how much / how fast" signal that drives alerts (see [Module 3.6](03-distributed-systems/06-observability.md)).

**Microservices** — Architecting an application as many small independently deployable services that talk over the network, instead of one big monolith. Buys independent scaling and team autonomy at the cost of operational and distributed-systems complexity.

**Monolith** — An application built and deployed as one single unit. Simple to develop and operate early on; the trade-off is it gets harder to scale and change as it grows — contrast with microservices.

**Monotonic reads** — A consistency guarantee that once you've seen a value, you'll never later see an *older* one (no going back in time). Prevents the jarring effect of stale replicas (see [Module 2.9](02-building-blocks/09-cap-pacelc-consistency.md)).

**Multi-leader replication** — Replication with several primaries that each accept writes (e.g. one per region), improving write availability but requiring conflict resolution when they disagree (see [Module 2.6](02-building-blocks/06-replication.md)).

## N

**The nines** — Shorthand for availability levels by number of nines: 99.9% ("three nines") ≈ 43 min downtime/month, 99.99% ("four nines") ≈ 4 min, each nine ~10× harder and costlier (see [Module 3.6](03-distributed-systems/06-observability.md)).

**Normalization** — Structuring a relational schema so each fact lives in exactly one place (no duplication), keeping data consistent at the cost of needing joins to reassemble it. The opposite of denormalization (see [Module 2.4](02-building-blocks/04-sql-vs-nosql.md)).

**NoSQL** — A broad family of non-relational databases (key-value, document, wide-column, graph) that trade SQL's joins and strict ACID for scale, flexible schemas, or specific access patterns. Choose for your constraints, not for hype (see [Module 2.4](02-building-blocks/04-sql-vs-nosql.md)).

## O

**Object storage** — see *Blob / object storage* under B.

**Observability** — The ability to understand a running system from the outside via logs, metrics, and traces — "if you can't measure it, you can't scale it" (see [Module 3.6](03-distributed-systems/06-observability.md)).

**Offset** — A consumer's position (an index) in a partitioned log, marking how far it has read; committing the offset lets it resume after a restart (see [Module 4.13](04-case-studies/13-message-queue/)).

**Outbox pattern** — A reliability technique where a service writes its database change and an outgoing message into the *same* local transaction (an "outbox" table), then a separate process publishes them — so you never update the DB without sending the event, or vice versa (see [Module 3.3](03-distributed-systems/03-distributed-transactions.md)).

## P

**p50 / p95 / p99** — Percentile latencies: p50 (median) is typical, p99 is the slowest 1% of requests. Averages hide pain; the tail (p99) is what unlucky users actually feel (see [Module 0.2](00-foundations/02-numbers-every-engineer-should-know.md)).

**PACELC** — An extension of CAP: if there's a Partition, choose Availability or Consistency; Else (normal operation) choose Latency or Consistency. It captures the everyday trade-off CAP ignores (see [Module 2.9](02-building-blocks/09-cap-pacelc-consistency.md)).

**Partition (log)** — One ordered sub-stream of a topic in a messaging system; splitting a topic into partitions is how it scales and how consumer groups divide work. Order is guaranteed within a partition, not across them (see [Module 4.13](04-case-studies/13-message-queue/)).

**Partition tolerance** — A system's ability to keep operating when the network splits and nodes can't all talk to each other. The "P" in CAP — in practice non-negotiable, which forces the C-vs-A choice (see [Module 2.9](02-building-blocks/09-cap-pacelc-consistency.md)).

**Partitioning** — see *Sharding / partitioning* under S.

**Paxos** — A foundational (and famously tricky) consensus algorithm for agreeing on a value among unreliable nodes. Powerful but hard to implement; Raft was designed to be its understandable alternative (see [Module 3.1](03-distributed-systems/01-consensus-raft-paxos.md)).

**Polyglot persistence** — Using different database types for different jobs within one system (e.g. SQL for orders, Redis for sessions, a graph DB for the social graph) — the right tool per workload (see [Module 2.4](02-building-blocks/04-sql-vs-nosql.md)).

**Producer** — A process that creates and sends messages into a queue or topic. The sending end, opposite of a consumer (see [Module 2.10](02-building-blocks/10-message-queues-streaming.md)).

**Protocol Buffers (protobuf)** — A compact binary format with a typed schema for serializing structured data, used by gRPC. Smaller and faster than JSON, at the cost of being non-human-readable (see [Module 1.3](01-networking-and-communication/03-rpc-grpc.md)).

**Pub/sub (publish-subscribe)** — A messaging pattern where publishers send messages to a topic and any number of subscribers receive them, with neither side knowing the other. Decouples one-to-many event delivery (see [Module 2.10](02-building-blocks/10-message-queues-streaming.md)).

## Q

**QPS (Queries Per Second)** — A throughput measure of how many requests a system handles each second; the central number in back-of-the-envelope estimation (see [Module 0.3](00-foundations/03-back-of-envelope-estimation.md)).

**Quadtree** — A tree that recursively splits a 2-D area into four quadrants, denser where there are more points, for efficient spatial range queries. A common alternative to geohash for "nearby" search (see [Module 4.12](04-case-studies/12-proximity-service/)).

**Quorum (W+R>N)** — In leaderless replication, requiring writes to reach W replicas and reads to query R out of N, set so W+R>N guarantees a read overlaps the latest write. Tunes the consistency-vs-availability dial (see [Module 4.2](04-case-studies/02-key-value-store/)).

## R

**Raft** — A consensus algorithm designed to be understandable, using leader election and a replicated log so a cluster agrees on an ordered series of operations. The basis of many real coordination systems (see [Module 3.1](03-distributed-systems/01-consensus-raft-paxos.md)).

**Rate limiting** — Capping how many requests a client may make in a window to protect a service from abuse or overload, using algorithms like token bucket, leaky bucket, or sliding window (see [Module 2.11](02-building-blocks/11-rate-limiting.md)).

**Read-your-writes** — A consistency guarantee that after you write something, your own subsequent reads will see it (no "I just updated my profile but it shows the old value"). A common practical minimum (see [Module 2.9](02-building-blocks/09-cap-pacelc-consistency.md)).

**Rebalancing** — Redistributing data or load across nodes when the cluster changes size, ideally moving as little as possible (the goal consistent hashing serves) (see [Module 2.7](02-building-blocks/07-sharding-partitioning.md)).

**Refresh-ahead** — A caching strategy that proactively reloads popular entries before they expire so hot keys never go cold, at the risk of wasted refreshes if predictions miss (see [Module 2.3](02-building-blocks/03-caching.md)).

**Relational database** — see *SQL / relational database* under S.

**Replication** — Keeping multiple copies of data on different machines for read scaling and availability. The trade-off is keeping copies in sync — see replication lag (see [Module 2.6](02-building-blocks/06-replication.md)).

**Replication lag** — The delay between a write hitting the primary and showing up on a replica, during which a replica read can return stale data. The root cause of eventual consistency (see [Module 2.6](02-building-blocks/06-replication.md)).

**REST** — An API style over HTTP that models things as resources (URLs) acted on with standard verbs (GET/POST/PUT/DELETE). Simple, cacheable, and ubiquitous; can over/under-fetch compared to GraphQL (see [Module 1.2](01-networking-and-communication/02-http-https-rest.md)).

**Reverse proxy** — A server that sits in front of your backend, receiving client requests and forwarding them on, handling things like TLS, caching, and load balancing. Clients see the proxy, not your servers (see [Module 2.2](02-building-blocks/02-reverse-proxy-cdn.md)).

**Round-robin** — The simplest load-balancing strategy: hand requests to servers in rotation, one after another. Even when servers and requests are uniform, weaker when they aren't (see [Module 2.1](02-building-blocks/01-load-balancing.md)).

**RPC (Remote Procedure Call)** — Calling a function on another machine as if it were local, hiding the network behind a normal-looking method call. Convenient, but the network's failures and latency don't actually disappear (see [Module 1.3](01-networking-and-communication/03-rpc-grpc.md)).

## S

**S2** — Google's spatial library that maps the globe onto a space-filling curve of hierarchical cells, used for fast geospatial indexing — an alternative to geohash and quadtrees (see [Module 4.12](04-case-studies/12-proximity-service/)).

**Saga** — A pattern for long-running transactions across services done as a sequence of local steps, each with a compensating transaction to undo it if a later step fails. How you get atomicity-ish behaviour without distributed locks (see [Module 3.3](03-distributed-systems/03-distributed-transactions.md)).

**SSE (Server-Sent Events)** — A one-way push channel where the server streams updates to the browser over a single long-lived HTTP connection. Simpler than WebSockets when you only need server-to-client (see [Module 1.5](01-networking-and-communication/05-polling-sse-websockets.md)).

**Shard key** — The field used to decide which shard a row belongs to (e.g. `user_id`). A good choice spreads load evenly; a bad one creates hotspots (see [Module 2.7](02-building-blocks/07-sharding-partitioning.md)).

**Sharding / partitioning** — Splitting one dataset across multiple databases, each holding a subset, so writes and storage scale beyond a single machine. Strategies include range, hash, and directory-based; it complicates joins and transactions (see [Module 2.7](02-building-blocks/07-sharding-partitioning.md)).

**Service discovery** — How services find each other's network addresses in a dynamic environment where instances come and go, via a registry instead of hard-coded IPs (see [Module 3.4](03-distributed-systems/04-coordination-leader-election.md)).

**SLA / SLO / SLI** — An SLI is a measured indicator (e.g. % of fast requests); an SLO is your internal target for it; an SLA is the contractual promise to customers (with penalties). From measurement to goal to promise (see [Module 3.6](03-distributed-systems/06-observability.md)).

**Snowflake** — A scheme for generating unique 64-bit IDs without coordination by packing a timestamp, a machine ID, and a per-millisecond sequence number — sortable by time and collision-free. Originated at Twitter (see [Module 2.12](02-building-blocks/12-unique-id-generation.md)).

**Span** — A single timed unit of work within a distributed trace (e.g. one service call), with a start, duration, and parent. Spans nest to form the full trace (see [Module 3.6](03-distributed-systems/06-observability.md)).

**Split-brain** — A failure where a network partition leaves two nodes both believing they're the leader and accepting conflicting writes. Guarded against with quorums and fencing tokens (see [Module 3.4](03-distributed-systems/04-coordination-leader-election.md)).

**SPOF (Single Point of Failure)** — Any one component whose failure takes down the whole system; eliminating SPOFs (via redundancy) is a core goal of resilient design (see [Module 3.5](03-distributed-systems/05-fault-tolerance.md)).

**SQL / relational database** — A database that stores data in tables with rows and columns and a fixed schema, queried with SQL and supporting joins and ACID transactions; e.g. Postgres, MySQL. The sensible default for most apps (see [Module 2.4](02-building-blocks/04-sql-vs-nosql.md)).

**SSTable (Sorted String Table)** — An immutable, sorted-on-disk file produced when an LSM-Tree flushes its memtable; reads scan these and compaction merges them. The on-disk building block of LSM engines (see [Module 3.2](03-distributed-systems/02-storage-engines.md)).

**Stateless service** — A server that keeps no per-user state in its own memory between requests, pushing shared state to a cache or DB. This is what lets any request go to any instance — the foundation of horizontal scaling (see [Module 0.1](00-foundations/01-scale-zero-to-millions.md)).

**Sticky session (session affinity)** — A load-balancer setting that always routes a given user to the same backend, useful for in-memory session state but at odds with statelessness and even load (see [Module 2.1](02-building-blocks/01-load-balancing.md)).

**Strong consistency** — A guarantee that every read returns the latest committed write, so all clients see one up-to-date truth. Easiest to reason about, costliest in latency and availability — contrast with eventual consistency (see [Module 2.9](02-building-blocks/09-cap-pacelc-consistency.md)).

## T

**Tail latency** — The latency of the slowest requests (e.g. p99/p99.9), which dominates user experience at scale because a single page touches many services. "The tail wags the dog" (see [Module 0.2](00-foundations/02-numbers-every-engineer-should-know.md)).

**TCP** — A connection-oriented transport protocol that guarantees ordered, reliable delivery via handshakes and retransmissions. Reliable but with more overhead than UDP (see [Module 1.1](01-networking-and-communication/01-internet-ip-dns-tcp-udp.md)).

**Throughput** — How much work a system completes per unit time (e.g. requests/second), as opposed to latency, which is the time for one request. You can have high throughput and high latency at once (see [Module 0.2](00-foundations/02-numbers-every-engineer-should-know.md)).

**TLS** — The protocol that encrypts and authenticates a connection (the "S" in HTTPS), so data in transit can't be read or tampered with (see [Module 1.2](01-networking-and-communication/02-http-https-rest.md)).

**Token bucket** — A rate-limiting algorithm where tokens refill at a steady rate and each request spends one; it allows short bursts (saved-up tokens) while capping the average rate. Contrast with the strictly-smooth leaky bucket (see [Module 2.11](02-building-blocks/11-rate-limiting.md)).

**Trace / trace ID** — A trace is the end-to-end record of one request as it flows across services; the trace ID is the shared identifier that stitches all its spans together. The third pillar of observability (see [Module 3.6](03-distributed-systems/06-observability.md)).

**Tracing** — Following a single request across every service it touches to see where time went and what failed — the observability pillar made of traces and spans (see [Module 3.6](03-distributed-systems/06-observability.md)).

**TTL (Time To Live)** — An expiry stamped on cached data after which it's considered stale and discarded, bounding how long stale data can live. The simplest cache-invalidation tool (see [Module 2.3](02-building-blocks/03-caching.md)).

**2PC (Two-Phase Commit)** — A distributed-transaction protocol where a coordinator asks all participants to "prepare", then "commit" only if all agreed. Gives atomicity across nodes but blocks if the coordinator fails — the motivation for Sagas (see [Module 3.3](03-distributed-systems/03-distributed-transactions.md)).

**3PC (Three-Phase Commit)** — A variant of 2PC adding an extra phase to avoid blocking when the coordinator fails, at the cost of more messages and weaker guarantees under partitions. Rarely used in practice (see [Module 3.3](03-distributed-systems/03-distributed-transactions.md)).

## U

**UDP** — A connectionless transport protocol that fires packets without ordering, reliability, or handshakes — fast and lightweight, used for video, games, and DNS where speed beats perfection (see [Module 1.1](01-networking-and-communication/01-internet-ip-dns-tcp-udp.md)).

**Unique ID generation** — Producing globally unique identifiers across a distributed system without collisions, ideally without a central bottleneck; approaches include UUIDs and Snowflake (see [Module 2.12](02-building-blocks/12-unique-id-generation.md)).

**UUID** — A 128-bit identifier generated to be unique with no coordination, great for avoiding collisions but large and (commonly) not time-sortable — contrast with Snowflake (see [Module 2.12](02-building-blocks/12-unique-id-generation.md)).

## V

**Vector clock** — A set of per-node counters attached to data that lets a distributed system tell whether two versions are causally ordered or genuinely concurrent (a conflict). The basis for smarter conflict resolution than last-write-wins (see [Module 4.2](04-case-studies/02-key-value-store/)).

**Vertical scaling (scale up)** — Getting more power by upgrading to a bigger machine. Simple with no app changes, but hits a hardware ceiling and gives no redundancy — contrast with horizontal scaling (see [Module 0.1](00-foundations/01-scale-zero-to-millions.md)).

**Virtual node (vnode)** — In consistent hashing, mapping each physical server to many points on the ring so load spreads evenly and rebalancing is smoother. More vnodes, more uniform distribution (see [Module 2.8](02-building-blocks/08-consistent-hashing.md)).

## W

**WAL (Write-Ahead Log)** — An append-only log where a database records a change *before* applying it, so a crash mid-operation can be recovered by replaying the log. The durability mechanism behind both B-Tree and LSM engines (see [Module 3.2](03-distributed-systems/02-storage-engines.md)).

**WebSocket** — A protocol giving a persistent, full-duplex (two-way) connection between client and server over a single TCP connection, ideal for chat and live updates. More capable than SSE/long polling, but more to manage (see [Module 1.5](01-networking-and-communication/05-polling-sse-websockets.md)).

**Wide-column store** — A NoSQL database that stores data in rows with flexible, sparse columns grouped into families, tuned for massive write volume and huge tables; e.g. Cassandra, HBase (see [Module 2.4](02-building-blocks/04-sql-vs-nosql.md)).

**Write amplification** — When one logical write causes much more physical writing under the hood (e.g. LSM compaction rewriting data repeatedly), wearing disks and consuming I/O. A key cost to weigh in storage engines (see [Module 3.2](03-distributed-systems/02-storage-engines.md)).

**Write-around** — A caching strategy that writes straight to the DB and skips the cache, which fills only on later reads — avoids polluting the cache with write-once data, but a read right after a write misses (see [Module 2.3](02-building-blocks/03-caching.md)).

**Write-back (write-behind)** — A caching strategy that updates only the cache and acknowledges immediately, flushing to the DB asynchronously later. Very fast writes, but risks data loss if the cache dies before flushing (see [Module 2.3](02-building-blocks/03-caching.md)).

**Write-through** — A caching strategy that writes to the cache and the DB synchronously so they stay in sync, guaranteeing fresh reads at the cost of slower writes (see [Module 2.3](02-building-blocks/03-caching.md)).

## Z

**ZooKeeper / etcd** — Battle-tested coordination services that provide consensus-backed primitives (leader election, locks, config, service discovery) so you don't build them yourself. The reliable "brain" many distributed systems lean on (see [Module 3.4](03-distributed-systems/04-coordination-leader-election.md)).
