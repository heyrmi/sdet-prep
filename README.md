# HLPW — Playwright Test Automation Framework

[![Playwright Tests](https://github.com/heyrmi/hlpw/actions/workflows/playwright.yml/badge.svg)](https://github.com/heyrmi/hlpw/actions/workflows/playwright.yml) 

UI and API test automation in TypeScript, covering browser interactions, REST APIs, schema
validation, cross-browser execution, visual regression, accessibility, factory-based test data, and
consumer-driven contract testing.

## Tech Stack

| Tool | Role |
| --- | --- |
| [Playwright](https://playwright.dev/) | Test runner, browser automation, API request context, native visual regression |
| TypeScript | Strict mode, path aliases, full type coverage |
| [Zod v4](https://zod.dev/) | Runtime schema validation for API responses |
| [@axe-core/playwright](https://github.com/dequelabs/axe-core-npm) | Accessibility (WCAG 2 A/AA) scanning |
| [@faker-js/faker](https://fakerjs.dev/) | Seedable test-data factories |
| [Pact JS](https://docs.pact.io/) | Consumer-driven contract testing (mock provider) |
| [Biome](https://biomejs.dev/) | Linting and formatting |
| [tsx](https://tsx.is/) | TypeScript runner for the standalone contract suite |
| GitHub Actions | CI pipeline (parallel workers, retries, HTML reports) |

## Applications Under Test

| App | URL | Coverage |
| --- | --- | --- |
| The Internet | https://the-internet.herokuapp.com | UI tests — pages, auth, uploads, frames, network intercept |
| httpbin.org | https://httpbin.org | API basics — methods, headers, cookies, status codes, redirects |
| FreeAPI.app | https://api.freeapi.app | API schema validation, auth, CRUD, pagination, error handling |

## Quick Start

Prerequisites: **Node 24+**, npm

```bash
npm ci
npx playwright install --with-deps
cp .env.example .env
npm test
```

## Running Tests

| Command | Description |
| --- | --- |
| `npm test` | Run all tests (UI + API + FreeAPI) |
| `npm run test:ui` | Run UI tests only (Chrome project) |
| `npm run test:crossbrowser` | Run UI tests across Chrome, Firefox, Edge, Safari, Mobile Chrome, Mobile Safari |
| `npm run test:api` | Run httpbin API tests only |
| `npm run test:freeapi` | Run FreeAPI tests only |
| `npm run test:visual` | Run visual-regression specs (`@visual`, Chromium) |
| `npm run test:visual:update` | Regenerate committed visual baselines |
| `npm run test:a11y` | Run accessibility specs (`@a11y`, Chromium) |
| `npm run test:contract` | Run the Pact consumer contract suite (writes to `pacts/`) |
| `npm run test:smoke` | Run tests tagged `@smoke` |
| `npm run test:headed` | Run all tests in headed mode |
| `npm run test:debug` | Run with Playwright Inspector |
| `npm run report` | Open the HTML report |
| `npm run lint` | Check for lint issues |
| `npm run format` | Auto-format code |

> Cross-browser projects use real-browser channels (`chrome`, `msedge`). Install them once with
> `npx playwright install chromium firefox webkit` and, for the channel projects,
> `npx playwright install chrome msedge`.

## Project Structure

```
.
├── pageFactory/ui/              # Page Object Models (BasePage + 20 pages)
├── tests/
│   ├── ui/                      # 19 UI test files
│   ├── api/                     # 9 httpbin test files
│   └── freeapi/                 # 13 FreeAPI test files
├── lib/
│   ├── BaseTest.ts              # UI fixtures + axeScan / visualCheck maturity fixtures
│   ├── ApiBaseTest.ts           # httpbin fixtures (httpBin, authedHttpBin)
│   ├── FreeApiBaseTest.ts       # FreeAPI fixtures (freeApi, authedFreeApi) — dynamic data via factory
│   ├── data/                    # Faker-based, seedable test-data factories (aliased @lib/data)
│   │   ├── index.ts             # Barrel export + seedFactories()
│   │   ├── registration.factory.ts
│   │   ├── product.factory.ts
│   │   ├── book.factory.ts
│   │   └── todo.factory.ts
│   └── api/
│       ├── ApiClient.ts         # Generic HTTP client wrapping APIRequestContext
│       ├── ApiResponse.ts       # Response wrapper with status/header assertions
│       ├── HttpBinClient.ts     # httpbin convenience methods (extends ApiClient)
│       ├── FreeApiClient.ts     # FreeAPI convenience methods (extends ApiClient)
│       └── schemas/
│           └── freeapi.schemas.ts  # Zod v4 runtime schemas for FreeAPI responses
├── contract/
│   └── httpbin.pact.test.ts     # Pact consumer contract (node:test, runs outside PW projects)
├── pacts/                       # Generated consumer contracts (committed)
├── visual-baselines/            # Committed visual-regression snapshots (Chromium)
├── types/
│   ├── api.ts                   # Generic RequestOptions interface
│   ├── httpbin.ts               # httpbin response interfaces
│   └── freeapi.ts               # FreeAPI response interfaces
├── testData/
│   ├── ui/                      # JSON data for parameterized tests
│   └── files/                   # Upload/download test files
├── playwright.config.ts         # Projects: Chromium (maturity), Chrome, Firefox, Edge, Safari, Mobile ×2, API, FreeAPI
├── testConfig.ts                # Environment config (URLs, credentials)
└── globalSetup.ts               # Pre-test cleanup
```

## Architecture Overview

Playwright projects:

- **Chromium** — bundled-chromium project that runs ONLY the maturity specs (`@visual` + `@a11y`).
  Version-pinned so committed visual baselines are reproducible without a system browser.
- **Chrome / Firefox / Edge / Safari / Mobile Chrome / Mobile Safari** — run `tests/ui/` against
  the-internet.herokuapp.com across engines. These exclude `@visual`/`@a11y` (via `grepInvert`) so the
  maturity specs never multiply across browsers.
- **API** — runs `tests/api/` against httpbin.org
- **FreeAPI** — runs `tests/freeapi/` against api.freeapi.app

The Pact contract suite (`contract/`) runs **outside** the Playwright projects via its own
`node:test` + `tsx` runner (`npm run test:contract`).

Layered pattern:

```
testConfig.ts → ApiClient / Page Objects → Fixtures (BaseTest / ApiBaseTest) → Tests
```

Each test file imports fixtures from its base test module. UI tests get page objects injected. API tests get typed client instances with built-in response wrapping.

## Maturity Capabilities

### Cross-browser
UI specs run across Chromium-based (Chrome, Edge), Firefox, WebKit (Safari), and mobile emulation
(Pixel 7, iPhone 15 Pro Max) projects. `npx playwright test --list` enumerates every project. For CI
time, shard with `npx playwright test --shard=1/4`.

### Visual regression (native Playwright)
`toHaveScreenshot` baselines live under `visual-baselines/{projectName}/…` (committed) and are
generated on the pinned **Chromium** project only, keeping the baseline set small and deterministic.
Config sets `animations: 'disabled'` and `maxDiffPixelRatio`. Specs are tagged `@visual`.

```bash
npm run test:visual           # compare against committed baselines
npm run test:visual:update    # regenerate baselines (review the diff before committing)
```

### Accessibility (axe-core)
The `axeScan` fixture returns a pre-configured `AxeBuilder({ page })` filtered to WCAG 2 A/AA tags
(overridable per call). Specs (`@a11y`) scan pages, attach a readable JSON summary, and gate on zero
`critical` violations — one spec intentionally proves the gate by detecting the dropdown page's real
`select-name` defect.

```bash
npm run test:a11y
```

### Test-data factories
`lib/data/` provides typed, override-friendly factories (`buildRegistration`, `buildProduct`,
`buildBook`, `buildTodo`) matching the Zod-typed API shapes. They emit unique values by default and
are made deterministic with `seedFactories(seed)`. Dynamic data in `FreeApiBaseTest`'s `authedFreeApi`
fixture is routed through `buildRegistration`; static JSON in `testData/` remains for parameterized
scenarios.

```ts
import { buildRegistration, seedFactories } from '@lib/data';

const user = buildRegistration({ username: 'fixed_username' }); // override any field
seedFactories(1234); // deterministic output for reproducible runs
```

### Contract testing (Pact JS)
`contract/httpbin.pact.test.ts` is a real consumer contract test. It stands up Pact's mock provider,
points the actual `HttpBinClient` at the mock URL, exercises requests, and writes the consumer
contract to `pacts/` (committed). This complements — it does not replace — Zod schema validation.

```bash
npm run test:contract   # -> pacts/hlpw-client-httpbin.json
```

## How to Add Tests

### Adding a UI Test

1. Create a page object in `pageFactory/ui/` extending `BasePage`
2. Register it as a fixture in `lib/BaseTest.ts`
3. Write the test in `tests/ui/` importing `{ test, expect }` from `@lib/BaseTest`
4. Use `test.step()` for structured reporting and `@smoke` tag for the smoke suite

### Adding an API Test (httpbin)

1. Add a convenience method to `lib/api/HttpBinClient.ts` if needed
2. Write the test in `tests/api/` using the `httpBin` or `authedHttpBin` fixture from `@lib/ApiBaseTest`

### Adding an API Test (FreeAPI)

1. Add the TypeScript interface in `types/freeapi.ts`
2. Add the Zod schema in `lib/api/schemas/freeapi.schemas.ts`
3. Add a client method in `lib/api/FreeApiClient.ts`
4. Write the test in `tests/freeapi/` using the `freeApi` or `authedFreeApi` fixture from `@lib/FreeApiBaseTest`
5. For error-path tests, use `freeApiErrorSchema.parse()` to narrow the response type

## Path Aliases

| Alias | Resolves to |
| --- | --- |
| `@/*` | Project root |
| `@lib/*` | `lib/` (includes `@lib/data` factories) |
| `@pages/*` | `pageFactory/ui/` |
| `@testdata/*` | `testData/` |

## Linting

```bash
npm run lint      # Check for issues
npm run format    # Auto-format
```
