# 4.17 — Design a Payment System (Ledger)

> **Module 4 · Case Studies** · ~35 min read + coding assignment
> *Concepts exercised:* correctness over everything, double-entry bookkeeping, idempotency
> keys, exactly-once processing, the dual-write problem, reconciliation, PSP integration,
> the ledger as source of truth, auditability.

---

## The problem

A **payment system** moves money between parties: a customer pays a merchant, a wallet sends funds
to another wallet, a refund flows back. The bytes are easy — the hard part is that **money must be
correct, always.** Lose a photo and a user is annoyed; lose (or duplicate) a payment and you have a
legal and financial incident. So this design inverts the usual priorities: where most systems trade
correctness for availability or speed, a payment system treats **correctness and auditability as
non-negotiable** and trades *latency and convenience* to protect them.

> **Analogy.** Think of an old-fashioned accountant's ledger book — two columns, ink, never an
> eraser. Every time money moves, the accountant writes it in *two places at once*: the amount
> *leaves* one account (a debit) and the *same amount arrives* in another (a credit). At the end of
> the day the columns must balance to the penny. If they don't, you don't "fix the number" — you
> hunt down the missing entry, because the book is the *truth*. A payment system is that ledger,
> made of code: append-only, double-sided, and reconciled relentlessly. You never overwrite a
> balance; you record what happened and *derive* the balance from history.

Why this is its own discipline:

- **Money is conserved.** It can't be created or destroyed by a transfer — only moved. The system
  must *structurally* enforce that.
- **Exactly-once, not at-least-once.** Networks retry. A retried "charge $50" must charge $50
  *once*, not twice. That's the **idempotency** problem.
- **Everything is auditable.** Regulators, disputes, and your own debugging all demand: "show me
  every entry that produced this balance." So the system is append-only and immutable.

---

## Step 1: Requirements (always start here)

**Functional**
- Move money between accounts: `Transfer(from, to, amount)`.
- Hold balances and report them: `Balance(account)`.
- Integrate with **Payment Service Providers (PSPs)** — Stripe, Adyen, banks — for real
  card/bank rails (we model the *internal ledger*, not the card network).
- Refunds, reversals, holds/authorizations.

**Non-functional** (the whole point)
- **Correctness above all.** No lost or duplicated money, ever. A transfer is *all-or-nothing*
  (atomic): both sides happen, or neither does. No partial state.
- **Idempotency / exactly-once.** The same logical operation, retried any number of times, applies
  *once* and returns the original result.
- **Strong consistency.** A balance read must reflect all committed transfers. This is a case where
  we deliberately pick **consistency over availability** (the C in CAP) — better to reject a
  payment than to double-spend.
- **Durability & auditability.** Every committed entry survives crashes and is permanently
  retrievable.
- **Conservation invariant.** Sum of all balances is constant across internal transfers — and you
  should be able to *check* it at any moment.

> **The mindset shift.** In Module 2 we happily traded a little accuracy for speed (approximate
> rate limiting, eventual consistency). Here, *do the opposite.* When in doubt, choose the option
> that is easier to prove correct and easier to audit, even if it's slower. Say this out loud in an
> interview; it's the senior signal for payments.

---

## Step 2: Estimation (back-of-the-envelope)

Suppose a mid-size wallet: **10 M users, 5 M transfers/day.**

```
Avg write rate   = 5e6 / 86,400 s        ≈ 58 transfers/sec
Peak (say 10×)                            ≈ 580 transfers/sec
```

That is a *modest* write rate — payments are not a "millions of QPS" problem. The challenge isn't
throughput; it's **never being wrong** at that throughput. Now storage, the part that *does* grow:

```
Each transfer = 2 ledger entries (debit + credit), ~300 bytes each
Per day  = 5e6 × 2 × 300 B               ≈ 3 GB/day of immutable ledger entries
Per year                                  ≈ 1.1 TB/year, append-only, kept for years (compliance)
```

So the design is: **low-to-moderate write rate, strict correctness, ever-growing immutable
history.** That points at a transactional store (SQL is the classic, correct choice here) with the
ledger as an append-only table, not a "blow it up to a billion QPS" architecture.

---

## Step 3: High-level design

### The core idea: double-entry bookkeeping

Never store a balance as a single mutable number you overwrite. Instead, record **entries**. Every
money movement produces **two entries that sum to zero**: a **debit** on one account and an equal
**credit** on another.

```
Transfer $50 from Alice to Bob:
   entry 1:  Alice  -50   (debit)
   entry 2:  Bob    +50   (credit)
            ───────────
   sum        0           ← money is conserved, structurally
```

A balance is then *derived*: `balance(acct) = sum of all entries for that account`. Because every
transfer's two entries sum to zero, the **sum of all balances never changes** from an internal
transfer — the conservation invariant falls out for free. This is the single most important idea in
the lesson: **make the books impossible to unbalance by construction.**

### The money-movement flow

```
                          idempotencyKey = "order-4417"
   ┌────────┐  Transfer(key, from, to, amt)  ┌──────────────────┐
   │ Client │ ──────────────────────────────►│  Payment service │
   └────────┘                                 └────────┬─────────┘
        ▲                                              │
        │  same key again? return stored result        │ 1. check idempotency store
        └──────────────────────────────────────────────┤    (seen this key? → replay)
                                                         │ 2. validate (amount>0, funds OK)
                                                         │ 3. in ONE atomic txn:
                                                         ▼      - append debit + credit
                                              ┌──────────────────────┐  - record (key → result)
                                              │   Ledger (append-only │
                                              │   double-entry store) │  ← source of truth
                                              └──────────┬───────────┘
                                                         │  async, separately
                                                         ▼
                                              ┌──────────────────────┐
                                              │  PSP / bank rails     │  (Stripe, ACH…)
                                              └──────────────────────┘
                                                         │  daily statement
                                                         ▼
                                              ┌──────────────────────┐
                                              │   Reconciliation      │  ledger vs PSP must agree
                                              └──────────────────────┘
```

### API & data model

```
Transfer(idempotencyKey, from, to string, amount int64) (txnID string, err error)
Balance(account string) (int64, error)

accounts:
  id        string    (PK)
  -- balance is DERIVED from entries, not stored mutably (or cached & checked)

entries (append-only):
  txn_id    string    -- groups the two sides of one transfer
  account   string
  amount    int64     -- signed: negative = debit, positive = credit
  created_at timestamp

idempotency (the dedup table):
  key       string    (PK)
  txn_id    string    -- the result produced the first time
  status    string
```

Money is stored as **integers in the smallest unit** (cents), never floats — `0.1 + 0.2 != 0.3` in
floating point, which is unacceptable for money.

---

## Step 4: Deep dives

### 4a. Idempotency — exactly-once on a retrying network

The client (or a queue) may send the *same* `Transfer` twice: a timeout made it retry, but the
first one actually committed. Without protection you double-charge.

The fix: the client attaches a unique **idempotency key** per logical operation. The server:

1. Looks the key up in the idempotency table.
2. **Seen before?** Return the *stored original result* — do nothing else.
3. **New?** Perform the transfer *and* record `(key → result)` **in the same atomic transaction**.

| Approach | Behavior | Risk |
|----------|----------|------|
| No key (naive) | Each request applies | Double-charge on retry |
| Key checked, recorded *separately* | Usually dedups | Race/crash between apply and record → double-apply |
| Key recorded **in the same txn** as the transfer | Exactly-once | None for this op (the correct design) ⭐ |

> **Trade-off.** Idempotency keys cost a storage row and a lookup per request, and the client must
> generate stable keys. Cheap insurance against the worst bug in payments. The subtlety is *atomicity*:
> applying the transfer and recording the key must commit together, or a crash in between reintroduces
> double-apply.

### 4b. Atomicity — all-or-nothing

A transfer touches two accounts. If we debit Alice, crash, and never credit Bob, money vanished.
Both entries must commit together or not at all. Within one database this is a **single ACID
transaction** wrapping the validate-debit-credit-record steps. The validation (amount > 0,
sufficient funds) happens *inside* the transaction so a concurrent transfer can't sneak the balance
below zero between the check and the write — the classic check-then-act race.

### 4c. The dual-write problem

The ledger is your DB; the PSP/bank is an external system. You must update *both* — and you **cannot
make a database transaction span an external API call.** If you write the ledger then call the PSP
and the PSP call fails (or the reverse), the two disagree. This is the **dual-write problem**, and
"just do both" is the wrong answer.

| Strategy | How | Trade-off |
|----------|-----|-----------|
| Write DB, then call PSP inline | Simplest | If PSP/crash fails after commit, they diverge; no retry record |
| **Transactional outbox** | Commit ledger entry + an "outbox" row in one txn; a worker reads the outbox and calls the PSP, retrying | Reliable, at-least-once to PSP (needs PSP idempotency keys); adds a worker + small delay ⭐ |
| Two-phase commit across systems | Coordinate DB + PSP | Most PSPs don't support it; blocking, fragile — avoid |

The practical answer is the **outbox pattern**: make the only thing you do atomically a *local*
DB write (ledger entry + outbox message in one transaction), then let a separate process deliver to
the PSP with retries and *its own* idempotency key. You convert an impossible distributed-atomic
problem into a reliable local-atomic write plus retryable delivery.

### 4d. Reconciliation — trust, but verify

Even with all of the above, reality drifts: a PSP webhook is lost, a manual adjustment slips in, a
bug double-posts. **Reconciliation** is the periodic (often daily) batch job that compares your
internal ledger against the PSP's statement/bank file and flags every discrepancy.

It's the safety net *below* the code: idempotency and atomicity prevent most errors; reconciliation
*catches the ones that slip through* and produces an auditable report. A payment system without
reconciliation is one bug away from silently losing money. The invariant you constantly assert —
**sum of all balances is unchanged by internal transfers** — is the cheapest reconciliation check
of all, and it's exactly what your assignment will verify.

### 4e. Concurrency & consistency

Many transfers run at once; some touch the same account. Two concurrent withdrawals from one account
must not both pass an "enough funds?" check and overspend. Real systems use **row locking**
(`SELECT … FOR UPDATE`) or **optimistic concurrency** (version check on write); in our in-memory
assignment a **mutex** plays that role. The deliberate choice is **consistency over availability**:
under contention we *serialize and possibly reject*, rather than allow a fast-but-wrong answer.

---

## In the wild

- **Stripe** is built around **idempotency keys** (you pass `Idempotency-Key` on requests) and an
  internal double-entry ledger; their engineering writing on idempotency and reconciliation is a
  must-read.
- **Square, PayPal, Adyen** all run double-entry ledgers as the source of truth, with the card
  networks/banks as external rails reconciled daily.
- **TigerBeetle** is a purpose-built, open-source double-entry accounting database — a great way to
  see these ideas implemented at high performance.
- **The transactional outbox** pattern (often paired with change-data-capture, e.g. Debezium) is
  the standard production answer to the dual-write problem across the industry.

---

## Interview angle

Open by **inverting the usual priorities**: state that for payments, *correctness and auditability
beat latency and availability*, and you'll pick consistency over availability deliberately. Then
introduce **double-entry bookkeeping** and show that conservation of money falls out structurally
(debits + credits = 0; sum of balances invariant). Cover **idempotency keys** for exactly-once on a
retrying network — and stress that the key must be recorded *in the same atomic transaction* as the
transfer. Surface the **dual-write problem** with the external PSP and reach for the **transactional
outbox** (not 2PC). Close with **reconciliation** as the trust-but-verify safety net and
**money-as-integers** (never floats).

**Common follow-ups:**
- "The client times out and retries the same payment — how do you avoid double-charging?"
  → idempotency key; replay the stored result; record the key in the same transaction as the apply.
- "How do you guarantee a transfer is all-or-nothing?" → one ACID transaction wrapping
  validate + debit + credit + idempotency record; validation inside the txn to avoid check-then-act races.
- "You must update your ledger and call Stripe — what if one succeeds and the other fails?"
  → the dual-write problem; use the outbox pattern with PSP-side idempotency, not 2PC.
- "How do you know your books are correct?" → conservation invariant (sum of balances constant) +
  daily reconciliation against the PSP/bank statement; append-only ledger for audit.
- "Why integers, not floats?" → floating point can't represent money exactly; use the smallest unit
  (cents) as int64.

---

## Practice → the Go assignment

Now build the heart of it: an **idempotent, double-entry ledger**. Go to [`assignment/`](assignment/)
and implement, in order:

1. `CreateAccount(id, openingBalance)` — register an account.
2. `Transfer(idempotencyKey, from, to, amount)` — atomically debit `from` and credit `to`; reject
   `amount <= 0`, insufficient funds, and unknown accounts **with no state change**; and be
   **idempotent** — the same key returns the original `txnID` and applies exactly once.
3. `Balance(id)` and `TotalMoney()` — the latter must be **invariant** across transfers.
4. Make it **concurrency-safe** — the tests run with `-race`, hammering many concurrent transfers
   and asserting `TotalMoney` never changes and no balance goes negative.

```bash
cd assignment
go test ./...          # red → implement → green
go test -race ./...    # must pass: the ledger is shared across goroutines
```

The interface is given; you fill in the `// TODO`s. A reference solution is in
[`solution/`](solution/) — try first, peek after.

**Next:** [« Back to the case-study index](../) — or revisit [4.1 — Rate Limiter](../01-rate-limiter/).
