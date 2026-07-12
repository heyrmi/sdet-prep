# HLPW — Playwright Test Automation Framework

[![Playwright Tests](https://github.com/heyrmi/hlpw/actions/workflows/playwright.yml/badge.svg)](https://github.com/heyrmi/hlpw/actions/workflows/playwright.yml) 

UI and API test automation in TypeScript, covering browser interactions, REST APIs, and schema validation.

## Tech Stack

| Tool | Role |
| --- | --- |
| [Playwright](https://playwright.dev/) | Test runner, browser automation, API request context |
| TypeScript | Strict mode, path aliases, full type coverage |
| [Zod v4](https://zod.dev/) | Runtime schema validation for API responses |
| [Biome](https://biomejs.dev/) | Linting and formatting |
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
| `npm run test:api` | Run httpbin API tests only |
| `npm run test:freeapi` | Run FreeAPI tests only |
| `npm run test:smoke` | Run tests tagged `@smoke` |
| `npm run test:headed` | Run all tests in headed mode |
| `npm run test:debug` | Run with Playwright Inspector |
| `npm run report` | Open the HTML report |
| `npm run lint` | Check for lint issues |
| `npm run format` | Auto-format code |

## Project Structure

```
.
├── pageFactory/ui/              # Page Object Models (BasePage + 20 pages)
├── tests/
│   ├── ui/                      # 19 UI test files
│   ├── api/                     # 9 httpbin test files
│   └── freeapi/                 # 13 FreeAPI test files
├── lib/
│   ├── BaseTest.ts              # UI fixtures (page objects injected via test.extend)
│   ├── ApiBaseTest.ts           # httpbin fixtures (httpBin, authedHttpBin)
│   ├── FreeApiBaseTest.ts       # FreeAPI fixtures (freeApi, authedFreeApi)
│   └── api/
│       ├── ApiClient.ts         # Generic HTTP client wrapping APIRequestContext
│       ├── ApiResponse.ts       # Response wrapper with status/header assertions
│       ├── HttpBinClient.ts     # httpbin convenience methods (extends ApiClient)
│       ├── FreeApiClient.ts     # FreeAPI convenience methods (extends ApiClient)
│       └── schemas/
│           └── freeapi.schemas.ts  # Zod v4 runtime schemas for FreeAPI responses
├── types/
│   ├── api.ts                   # Generic RequestOptions interface
│   ├── httpbin.ts               # httpbin response interfaces
│   └── freeapi.ts               # FreeAPI response interfaces
├── testData/
│   ├── ui/                      # JSON data for parameterized tests
│   └── files/                   # Upload/download test files
├── playwright.config.ts         # 3 projects: Chrome, API, FreeAPI
├── testConfig.ts                # Environment config (URLs, credentials)
└── globalSetup.ts               # Pre-test cleanup
```

## Architecture Overview

Playwright is configured with three projects:

- **Chrome** — runs `tests/ui/` against the-internet.herokuapp.com
- **API** — runs `tests/api/` against httpbin.org
- **FreeAPI** — runs `tests/freeapi/` against api.freeapi.app

Layered pattern:

```
testConfig.ts → ApiClient / Page Objects → Fixtures (BaseTest / ApiBaseTest) → Tests
```

Each test file imports fixtures from its base test module. UI tests get page objects injected. API tests get typed client instances with built-in response wrapping.

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
| `@lib/*` | `lib/` |
| `@pages/*` | `pageFactory/ui/` |
| `@testdata/*` | `testData/` |

## Linting

```bash
npm run lint      # Check for issues
npm run format    # Auto-format
```
