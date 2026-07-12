# Amazon — SDET / QA Interview Questions

> **Focus:** 14 Leadership Principles, system design, SQL, scenario-based QA.

**30 questions** · Easy 6 · Medium 12 · Hard 12

These are verbal / scenario questions — you *talk through* your approach (test design, automation strategy, trade-offs). Practise answering out loud, structured and time-boxed.

## Sections

- [Testing Fundamentals](#testing-fundamentals) (4)
- [Automation & Frameworks](#automation-frameworks) (4)
- [API Testing](#api-testing) (4)
- [Database / Data Testing](#database-data-testing) (3)
- [System Design & Quality Strategy](#system-design-quality-strategy) (4)
- [Performance & Reliability](#performance-reliability) (2)
- [Domain-Specific](#domain-specific) (3)
- [Situational / Behavioral](#situational-behavioral) (6)

## Testing Fundamentals

| Level | Question | Time |
|-------|----------|------|
| Fresher | How would you approach testing Amazon's product search functionality from a customer's perspective? | 4 min |
| Fresher | What test scenarios would you prioritize for the Add to Cart feature? | 4 min |
| Mid | How would you design test cases for Amazon's coupon and discount engine? | 6 min |
| Mid | How would you prioritize testing when only a subset of regression tests can be executed before release? | 6 min |

## Automation & Frameworks

| Level | Question | Time |
|-------|----------|------|
| Fresher | What factors would you consider before automating Amazon's login workflow? | 4 min |
| Mid | How would you design automation for a checkout flow that changes frequently? | 6 min |
| Mid | How would you reduce flakiness in a large Selenium or Playwright test suite? | 6 min |
| Senior | How would you architect an automation framework supporting thousands of parallel executions daily? | 8 min |

## API Testing

| Level | Question | Time |
|-------|----------|------|
| Fresher | What validations would you perform on an API that retrieves product details? | 4 min |
| Mid | How would you test an order creation API when downstream services are unavailable? | 6 min |
| Mid | How would you validate that the Cart API correctly updates item quantities under concurrent requests? | 6 min |
| Senior | How would you test idempotency for order placement APIs? | 8 min |

## Database / Data Testing

| Level | Question | Time |
|-------|----------|------|
| Fresher | How would you verify that an order created through the UI is correctly stored in the database? | 4 min |
| Mid | How would you investigate discrepancies between order totals displayed in the UI and stored in the database? | 6 min |
| Mid | How would you validate data consistency between Order, Payment, and Shipment tables? | 6 min |

## System Design & Quality Strategy

| Level | Question | Time |
|-------|----------|------|
| Senior | How would you design a testing strategy for Amazon's Cart service handling millions of users? | 8 min |
| Senior | How would you test a distributed order tracking system spanning multiple regions? | 8 min |
| Senior | How would you validate eventual consistency between Inventory and Order services? | 8 min |
| Architect | How would you define a quality strategy for Amazon Checkout from development through production monitoring? | 10 min |

## Performance & Reliability

| Level | Question | Time |
|-------|----------|------|
| Senior | How would you evaluate checkout performance during Prime Day traffic spikes? | 8 min |
| Architect | How would you identify and mitigate quality risks before a global shopping event expected to generate record traffic? | 10 min |

## Domain-Specific

| Level | Question | Time |
|-------|----------|------|
| Mid | How would you test product recommendations shown to customers on the homepage? | 6 min |
| Mid | How would you validate that inventory levels remain accurate across multiple warehouses? | 6 min |
| Senior | How would you test a recommendation engine serving personalized results to millions of customers? | 8 min |

## Situational / Behavioral

| Level | Question | Time |
|-------|----------|------|
| Fresher | Tell me about a time when you identified a defect that others had overlooked. | 4 min |
| Mid | Tell me about a time when you disagreed with a developer about a defect. | 6 min |
| Mid | Tell me about a time when you had to make a quality decision under tight deadlines. | 6 min |
| Senior | Tell me about a time when you took ownership of a critical production issue. | 8 min |
| Architect | Tell me about a time when you improved a process that significantly reduced customer-facing defects. | 10 min |
| Architect | Tell me about a time when you made a difficult quality decision that was unpopular but ultimately benefited customers. | 10 min |

---

_Questions sourced from the public [ShapeMyInterview](https://www.shapemyinterview.com) company question banks. Use them as prompts; write your own answers._
