[![Test Automation](https://github.com/heyrmi/framework/actions/workflows/test-automation.yml/badge.svg)](https://github.com/heyrmi/framework/actions/workflows/test-automation.yml)
[![Appium Preview](https://img.shields.io/badge/Appium-Demo-blue?logo=appium)](https://drive.google.com/file/d/18kbzRp8npspdhUVHMBLv8FZ1q_Guvore/view?usp=sharing)

# Full Stack SDET Framework

A production-grade test automation framework built with Java 21, covering Web, API, Mobile, and Performance testing. Designed for real-world adoption and structured around industry-standard design patterns.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Architecture and Design Patterns](#architecture-and-design-patterns)
- [Getting Started](#getting-started)
- [Running Tests](#running-tests)
- [Configuration](#configuration)
- [Reporting](#reporting)
- [CI/CD](#cicd)
- [Contributing Guidelines](#contributing-guidelines)

---

## Tech Stack

| Area | Technology | Version |
|------|-----------|---------|
| Language | Java (LTS) | 21 |
| Build | Maven | 3.9+ |
| Web Automation | Selenium WebDriver | 4.41.0 |
| API Testing | Rest Assured | 6.0.0 |
| Mobile Automation | Appium Java Client | 10.1.0 |
| Performance Testing | Gatling (Java DSL) | 3.15.0 |
| Test Runner | TestNG | 7.12.0 |
| Reporting | Allure | 2.33.0 |
| Logging | Log4j2 | 2.25.3 |
| Data Binding | Jackson | 3.1.0 |
| Boilerplate Reduction | Lombok | 1.18.44 |

---

## Project Structure

The framework follows a clean separation: `src/main/java` contains framework infrastructure (drivers, page objects, utilities), while `src/test/java` contains only test classes.

```
src/
  main/java/ra/hul/framework/
    core/
      config/          ConfigManager           -- 4-level config resolution
      constants/       FrameworkConstants       -- Browser and platform constants
                       Endpoints               -- URL path constants
                       TimeoutConstants         -- Wait durations from config
      listeners/       AllureTestListener       -- Screenshot on failure
                       RetryAnalyzer            -- Configurable retry logic
                       RetryTransformer         -- Auto-applies retry to all tests

    web/
      driver/          BrowserStrategy          -- Sealed interface (Chrome, Firefox, Edge)
                       WebDriverFactory         -- Local + RemoteWebDriver creation
                       DriverManager            -- ThreadLocal driver isolation
      pages/           BasePage                 -- Core interactions with @Step annotations
                       LoginPage, SecurePage, CheckboxPage, DropdownPage,
                       DynamicLoadingPage, FileUploadPage, FileDownloadPage,
                       JavaScriptAlertsPage, FramesPage, MultipleWindowsPage,
                       DragAndDropPage, HoverPage
      utils/           WaitUtils                -- Explicit, fluent, and custom waits

    api/
      client/          ApiClient                -- REST client with Allure filter
      models/          User, PostPayload        -- Lombok POJOs

    mobile/
      driver/          AppiumDriverFactory      -- Android (UiAutomator2) + iOS (XCUITest)
                       AppiumDriverManager      -- ThreadLocal Appium driver isolation
      screens/         BaseScreen               -- Mobile interactions with @Step annotations
                       HomeScreen, LoginScreen, FormScreen,
                       SwipeScreen, WebViewScreen
      utils/           GestureUtils             -- W3C Actions API (swipe, scroll)
                       MobileWaitUtils          -- Appium-specific waits

    performance/
      simulations/     HttpBinGetSimulation     -- GET load test with assertions
                       HttpBinPostSimulation    -- POST load test with payload

  test/java/ra/hul/tests/
    base/              BaseWebTest              -- Driver init/teardown per method
                       BaseApiTest              -- ApiClient setup per class
                       BaseMobileTest           -- Appium init/teardown per method
    web/               11 test classes          -- Login, Checkbox, Dropdown, etc.
    api/               8 test classes           -- GET, POST, PUT/PATCH/DELETE, Auth, etc.
    mobile/            5 test classes           -- Login, Swipe, Form, Navigation, WebView

  test/resources/
    config.properties                           -- Default config
    config-{dev,stage,prod}.properties          -- Environment overrides
    log4j2.xml                                  -- Logging configuration
    allure.properties                           -- Allure output directory
    categories.json                             -- Allure defect categorization
    schemas/user-schema.json                    -- JSON Schema for contract testing
    testdata/upload-test.txt                    -- Test data for file upload
    web-tests.xml, api-tests.xml,              -- TestNG suite definitions
    mobile-tests.xml, smoke-tests.xml,
    all-tests.xml
```

---

## Architecture and Design Patterns

| Pattern | Where | Why |
|---------|-------|-----|
| **Singleton** | `ConfigManager` | Single source of truth for configuration. Thread-safe static initialization. |
| **Factory** | `WebDriverFactory`, `AppiumDriverFactory` | Decouples test code from driver creation. Supports local and remote execution from the same test. |
| **Strategy** | `BrowserStrategy` (sealed interface) | Each browser has its own capability setup. Sealed interfaces (Java 21) restrict implementations to known browsers at compile time. |
| **ThreadLocal** | `DriverManager`, `AppiumDriverManager` | Each thread gets an isolated driver instance, enabling parallel execution without shared state. |
| **Page Object Model** | `BasePage` + page classes | Encapsulates UI locators and interactions. Tests never reference raw `By` locators. |
| **Screen Object Model** | `BaseScreen` + screen classes | Same principle as POM, applied to mobile screens with Appium-specific actions. |
| **Builder** | Lombok `@Builder` on POJOs | Fluent construction of data objects. |
| **Template Method** | `BasePage.isLoaded()`, base test classes | Defines a skeleton (init driver, run test, quit driver) while subclasses supply the specifics. |
| **Observer** | `AllureTestListener`, `RetryTransformer` | Reacts to test lifecycle events (failure screenshots, automatic retry) without modifying test code. |

### Config Resolution Order

`ConfigManager` resolves properties with the following precedence (highest wins):

1. **OS environment variable** -- `base.url` is looked up as `BASE_URL`
2. **JVM system property** -- `-Dbase.url=...`
3. **Environment-specific file** -- `config-stage.properties` (when `-Denv=stage`)
4. **Default file** -- `config.properties`

This supports 12-factor app conventions and makes CI overrides straightforward without changing files.

---

## Getting Started

### Prerequisites

- Java 21 or later
- Maven 3.9 or later
- Chrome, Firefox, or Edge (for web tests)
- Appium 2.x server + Android emulator or iOS simulator (for mobile tests, optional)

### Setup

```bash
# Clone and build
git clone https://github.com/heyrmi/framework
cd framework
mvn clean compile

# Verify tests compile
mvn test-compile
```

---

## Running Tests

### By Module

```bash
# Web tests (requires a browser installed)
mvn test -Pweb

# API tests (no browser needed)
mvn test -Papi

# Mobile tests (requires Appium server + device/emulator)
mvn test -Pmobile

# Smoke tests only (cross-module, tagged with groups={"smoke"})
mvn test -Psmoke

# All tests
mvn test
```

### Environment Selection

```bash
mvn test -Papi -Denv=stage       # Uses config-stage.properties overrides
mvn test -Pweb -Denv=prod        # Uses config-prod.properties overrides
mvn test -Pweb -Denv=dev         # Dev mode: headless=false, lower parallelism
```

### Browser Selection

```bash
mvn test -Pweb -Dbrowser=firefox
mvn test -Pweb -Dbrowser=edge
mvn test -Pweb -Dbrowser=chrome   # Default
```

### Headless Mode

```bash
mvn test -Pweb -Dheadless=true    # CI mode (default)
mvn test -Pweb -Dheadless=false   # Watch tests run in a visible browser
```

### Remote Execution (Selenium Grid / Cloud)

```bash
# Selenium Grid
mvn test -Pweb -Dgrid.url=http://localhost:4444

# Cloud providers (set grid.url to the provider's hub endpoint)
mvn test -Pweb -Dgrid.url=https://hub.lambdatest.com/wd/hub
```

When `grid.url` is set, the framework creates a `RemoteWebDriver` with the selected browser's capabilities. When it is absent, a local driver is used.

### Performance Tests

```bash
mvn gatling:test                                       # Run all simulations
mvn gatling:test -Dgatling.simulationClass=ra.hul.framework.performance.simulations.HttpBinGetSimulation
```

Gatling reports are generated in `target/gatling/` as self-contained HTML.

### Parallel Execution

Parallelism is managed by TestNG, not Maven. Each suite XML defines `parallel` and `thread-count`:

- `web-tests.xml` -- `parallel="methods" thread-count="10"`
- `api-tests.xml` -- `parallel="methods" thread-count="20"`
- `mobile-tests.xml` -- `parallel="classes" thread-count="1"`

For higher parallelism (e.g., 50 threads with Selenium Grid):

```bash
mvn test -Pweb -Dgrid.url=http://grid:4444 -Dparallel.count=50
```

---

## Configuration

All configuration lives in `src/test/resources/config.properties` with environment-specific overrides in `config-{env}.properties`.

### Key Properties

| Property | Default | Description |
|----------|---------|-------------|
| `browser` | `chrome` | Browser for web tests: chrome, firefox, edge |
| `headless` | `true` | Run browser without visible window |
| `base.url` | `https://the-internet.herokuapp.com` | Base URL for web tests |
| `api.base.url` | `https://httpbin.org` | Base URL for API tests |
| `grid.url` | _(empty)_ | Selenium Grid hub URL. Empty = local driver. |
| `env` | _(empty)_ | Environment name. Loads `config-{env}.properties`. |
| `explicit.wait` | `15` | Default explicit wait timeout (seconds) |
| `retry.count` | `1` | Number of retries for failed tests |
| `parallel.count` | `10` | Thread count for parallel execution |
| `api.sla.ms` | `5000` | API response time SLA threshold (milliseconds) |
| `mobile.platform` | `android` | Mobile platform: android or ios |
| `appium.server.url` | `http://127.0.0.1:4723` | Appium server URL |

Any property can be overridden via JVM system property (`-Dkey=value`) or OS environment variable (`KEY_NAME` in SCREAMING_SNAKE_CASE).

---

## Reporting

### Allure Reports

The framework uses Allure for test reporting. Every test is annotated with `@Epic`, `@Feature`, `@Story`, and `@Severity` for structured categorization.

```bash
# Generate and open the report in a browser
mvn allure:serve

# Generate the report to target/site/allure-maven-plugin
mvn allure:report
```

What gets captured automatically:

- **Screenshots on failure** -- via `AllureTestListener`
- **API request/response logs** -- via `AllureRestAssured` filter on `ApiClient`
- **Step-level breakdown** -- via `@Step` annotations on page object methods (requires AspectJ weaver)
- **Defect categorization** -- via `categories.json` (Product Defects, Test Defects, Known Issues)

### Logs

Log4j2 writes to both console and `logs/test-execution.log`. Rolling file policy: 10MB per file, 5 files retained.

---

## CI/CD

The GitHub Actions workflow (`.github/workflows/test-automation.yml`) runs on push to `main`/`develop` and on pull requests.

### Pipeline Structure

```
push / PR
  |
  +-- web-tests job    (installs Chrome, runs -Pweb, uploads allure-results)
  |
  +-- api-tests job    (runs -Papi, uploads allure-results)
  |
  +-- allure-report job (merges results, generates report, deploys to GitHub Pages)
```

- Web and API jobs run in parallel.
- Allure report is deployed to the `gh-pages` branch with historical trend data (last 20 runs).
- Performance and mobile tests are available via manual `workflow_dispatch` only.

### Manual Trigger

The workflow supports manual dispatch with selectable test type and environment:

```
Actions > Test Automation > Run workflow
  - test_type: web | api | performance | all
  - environment: dev | stage | prod
```

---

## Contributing Guidelines

### Adding a New Web Test

1. If a new page is involved, create a page object in `ra.hul.framework.web.pages` extending `BasePage`.
2. Implement `isLoaded()` to return true when the page is ready.
3. Keep locators `private final` at the top of the class. Never expose `By` locators to tests.
4. Annotate interaction methods with `@Step` for Allure visibility.
5. Create the test class in `ra.hul.tests.web` extending `BaseWebTest`.
6. Annotate with `@Epic`, `@Feature`, `@Story`, `@Severity`.
7. Assign `groups` (at minimum `"regression"`; add `"smoke"` for critical happy paths).
8. Add the test class to the appropriate suite XML.

### Adding a New API Test

1. Use the existing `ApiClient` for standard CRUD.
2. Create POJOs in `ra.hul.framework.api.models` with `@Data @Builder @NoArgsConstructor @AllArgsConstructor`.
3. If contract testing is needed, add a JSON schema to `src/test/resources/schemas/`.
4. Create the test class in `ra.hul.tests.api` extending `BaseApiTest`.

### Adding a New Mobile Test

1. Create a screen object in `ra.hul.framework.mobile.screens` extending `BaseScreen`.
2. Use `AppiumBy.accessibilityId()` as the preferred locator strategy.
3. Create the test class in `ra.hul.tests.mobile` extending `BaseMobileTest`.

### Test Naming Convention

All test methods follow this pattern:

```
methodUnderTest_condition_expectedBehavior
```

Examples:
- `login_validCredentials_shouldShowSecurePage`
- `get_withQueryParams_shouldEchoParams`
- `checkbox_toggleFirst_shouldChangeState`

### Rules

- **No raw driver calls in tests.** All Selenium/Appium interactions go through page/screen objects.
- **No `Thread.sleep()`.** Use `WaitUtils` or `MobileWaitUtils` for synchronization.
- **No hardcoded URLs.** Use `ConfigManager` for base URLs and `Endpoints` for paths.
- **No hardcoded timeouts.** Use `TimeoutConstants`, which reads from config.
- **No shared mutable state.** Every driver is ThreadLocal. Every test method gets a fresh browser session.
- **Tests must be independent.** No test should depend on another test having run first. Each `@Test` starts from a clean state.
- **Keep page objects thin.** A page object exposes what a user can do on the page. Assertions belong in the test class, not the page object.

### Code Organization Rules

- Framework infrastructure goes in `src/main/java`. Tests go in `src/test/java`. No exceptions.
- One page object per page. One screen object per screen.
- Constants go in `core/constants/`. Configuration keys go in `config.properties`.
- Test data files go in `src/test/resources/testdata/`.

---

## License

This project is for educational and professional use.
