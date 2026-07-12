# Stripe — SDET / QA Interview Questions

> **Focus:** API correctness, idempotency, edge-case heavy test design.

**29 questions** · Easy 6 · Medium 12 · Hard 11

These are verbal / scenario questions — you *talk through* your approach (test design, automation strategy, trade-offs). Practise answering out loud, structured and time-boxed.

## Sections

- [Testing Fundamentals](#testing-fundamentals) (1)
- [Automation & Frameworks](#automation-frameworks) (2)
- [API Testing](#api-testing) (8)
- [Database / Data Testing](#database-data-testing) (3)
- [System Design & Quality Strategy](#system-design-quality-strategy) (2)
- [Performance & Reliability](#performance-reliability) (2)
- [Domain-Specific](#domain-specific) (6)
- [Situational / Behavioral](#situational-behavioral) (5)

## Testing Fundamentals

| Level | Question | Time |
|-------|----------|------|
| Fresher | What test scenarios would you create for a successful payment flow? | 4 min |

## Automation & Frameworks

| Level | Question | Time |
|-------|----------|------|
| Mid | How would you automate payment workflows without relying on real transactions? | 6 min |
| Mid | How would you design automation for hundreds of payment scenarios across multiple regions? | 6 min |

## API Testing

| Level | Question | Time |
|-------|----------|------|
| Fresher | How would you test a payment creation API that charges a customer's card? | 4 min |
| Fresher | How would you validate a refund API? | 4 min |
| Mid | How would you test idempotency for a payment API receiving duplicate requests? | 6 min |
| Mid | How would you validate APIs supporting invoice generation and payment collection? | 6 min |
| Mid | How would you validate webhook events generated after payment completion? | 6 min |
| Senior | How would you test idempotent payment processing under network retries and duplicate requests? | 8 min |
| Senior | How would you validate webhook delivery guarantees when client systems are unavailable? | 8 min |
| Architect | How would you validate correctness, reliability, and recoverability of a payment system under partial failures? | 10 min |

## Database / Data Testing

| Level | Question | Time |
|-------|----------|------|
| Fresher | How would you verify that payment transactions are correctly stored in the database? | 4 min |
| Mid | How would you investigate discrepancies between payment records and settlement reports? | 6 min |
| Mid | How would you verify transaction consistency across payment, refund, and invoice systems? | 6 min |

## System Design & Quality Strategy

| Level | Question | Time |
|-------|----------|------|
| Senior | How would you design a testing strategy for a payment platform processing millions of transactions daily? | 8 min |
| Architect | How would you define a quality strategy for a global payment platform handling billions of dollars in transactions? | 10 min |

## Performance & Reliability

| Level | Question | Time |
|-------|----------|------|
| Senior | How would you assess performance of a payment API during major shopping events? | 8 min |
| Architect | How would you identify and mitigate risks before a large-scale event expected to generate record payment volumes? | 10 min |

## Domain-Specific

| Level | Question | Time |
|-------|----------|------|
| Fresher | How would you test saving and reusing payment methods? | 4 min |
| Mid | How would you test multi-currency payment processing? | 6 min |
| Mid | How would you test payment failures caused by insufficient funds or declined cards? | 6 min |
| Mid | How would you test recurring subscription billing workflows? | 6 min |
| Senior | How would you test payment processing across multiple countries with different regulations and payment methods? | 8 min |
| Senior | How would you validate fraud detection mechanisms without impacting legitimate customers? | 8 min |

## Situational / Behavioral

| Level | Question | Time |
|-------|----------|------|
| Fresher | Tell me about a time when you found a defect that could have impacted customers financially. | 4 min |
| Mid | Tell me about a time when you had to make a difficult trade-off between quality and delivery. | 6 min |
| Mid | Tell me about a time when you improved reliability of a critical service. | 6 min |
| Senior | Tell me about a time when you prevented a production issue involving financial transactions. | 8 min |
| Architect | Tell me about a time when you led an initiative that significantly improved reliability or customer trust in a critical system. | 10 min |

---

_Questions sourced from the public [ShapeMyInterview](https://www.shapemyinterview.com) company question banks. Use them as prompts; write your own answers._
