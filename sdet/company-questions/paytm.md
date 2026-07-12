# Paytm — SDET / QA Interview Questions

> **Focus:** Payments, KYC, regulatory compliance, load testing.

**30 questions** · Easy 6 · Medium 12 · Hard 12

These are verbal / scenario questions — you *talk through* your approach (test design, automation strategy, trade-offs). Practise answering out loud, structured and time-boxed.

## Sections

- [Testing Fundamentals](#testing-fundamentals) (4)
- [Automation & Frameworks](#automation-frameworks) (3)
- [API Testing](#api-testing) (4)
- [Database / Data Testing](#database-data-testing) (2)
- [System Design & Quality Strategy](#system-design-quality-strategy) (4)
- [Performance & Reliability](#performance-reliability) (2)
- [Domain-Specific](#domain-specific) (6)
- [Situational / Behavioral](#situational-behavioral) (5)

## Testing Fundamentals

| Level | Question | Time |
|-------|----------|------|
| Fresher | How would you test a wallet recharge functionality in Paytm? | 4 min |
| Fresher | What test scenarios would you create for mobile number based login using OTP? | 4 min |
| Mid | How would you test a complete payment flow from initiation to successful completion? | 6 min |
| Mid | How would you test failed payment recovery scenarios? | 6 min |

## Automation & Frameworks

| Level | Question | Time |
|-------|----------|------|
| Fresher | Which Paytm user journeys would you prioritize for automation and why? | 4 min |
| Mid | How would you automate OTP-based workflows while maintaining test reliability? | 6 min |
| Mid | How would you design automation for a payments application with strict security requirements? | 6 min |

## API Testing

| Level | Question | Time |
|-------|----------|------|
| Fresher | How would you validate a payment initiation API? | 4 min |
| Mid | How would you validate APIs responsible for UPI payment processing? | 6 min |
| Mid | How would you validate APIs supporting wallet-to-bank transfers? | 6 min |
| Senior | How would you validate idempotency for payment APIs receiving duplicate requests? | 8 min |

## Database / Data Testing

| Level | Question | Time |
|-------|----------|------|
| Mid | How would you investigate mismatches between successful transactions and settlement records? | 6 min |
| Mid | How would you verify consistency between transaction records and account balances? | 6 min |

## System Design & Quality Strategy

| Level | Question | Time |
|-------|----------|------|
| Senior | How would you design a testing strategy for Paytm's payment processing platform handling millions of transactions daily? | 8 min |
| Senior | How would you test eventual consistency between payment, settlement, and reporting systems? | 8 min |
| Senior | How would you test resiliency when a banking partner or payment gateway becomes unavailable? | 8 min |
| Architect | How would you define a quality strategy for Paytm's end-to-end payments ecosystem? | 10 min |

## Performance & Reliability

| Level | Question | Time |
|-------|----------|------|
| Senior | How would you validate performance during major shopping events or IPL campaigns generating unusually high traffic? | 8 min |
| Architect | How would you identify and mitigate quality risks before a major event expected to generate record-breaking transaction volumes? | 10 min |

## Domain-Specific

| Level | Question | Time |
|-------|----------|------|
| Fresher | How would you test UPI payment functionality? | 4 min |
| Mid | How would you test KYC onboarding workflows? | 6 min |
| Mid | How would you test transaction status reconciliation between multiple payment systems? | 6 min |
| Senior | How would you test transaction integrity across wallet, UPI, banking, and merchant systems? | 8 min |
| Senior | How would you validate fraud detection mechanisms without impacting legitimate transactions? | 8 min |
| Architect | How would you validate compliance, security, and transaction correctness across multiple payment methods? | 10 min |

## Situational / Behavioral

| Level | Question | Time |
|-------|----------|------|
| Fresher | Tell me about a time when you found a defect that could have impacted customer transactions. | 4 min |
| Mid | Tell me about a time when you handled a critical issue under tight timelines. | 6 min |
| Mid | Tell me about a time when you improved quality for a critical business feature. | 6 min |
| Senior | Tell me about a time when you prevented a major production issue involving financial transactions. | 8 min |
| Architect | Tell me about a time when you led a cross-functional initiative that significantly improved transaction reliability, customer trust, or regulatory compliance. | 10 min |

---

_Questions sourced from the public [ShapeMyInterview](https://www.shapemyinterview.com) company question banks. Use them as prompts; write your own answers._
