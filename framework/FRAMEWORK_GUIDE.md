# Framework Module - Package Guide

This module is your production-grade test automation framework. Below is what each package is for and what you should build in it.

## Source Packages (`src/main/java/ra/hul/framework/`)

### `api/` - API Testing Infrastructure
Build fluent REST API testing utilities.
- **ApiRequestBuilder** — Builder pattern wrapping RestAssured's `RequestSpecification`. Fluent methods: `withHeader()`, `withBody()`, `withAuth()`, `get()`, `post()`, `put()`, `delete()`
- **ApiResponseValidator** — Static assertion helpers: `validateStatusCode()`, `validateJsonField()`, `validateJsonSchema()`, `validateHeader()`, `validateResponseTime()`

### `config/` - Configuration Management
Singleton config reader with environment layering.
- **ConfigReader** — Loads `config.properties` as base, overlays `config-{env}.properties` when `-Denv=dev|staging` is passed. System properties override everything. Exposes: `getBrowser()`, `isHeadless()`, `getBaseUrl()`, `getApiBaseUrl()`, timeout getters

### `constants/` - Centralized Constants
No magic strings scattered across tests.
- **Endpoints** — API endpoint paths as `static final String`
- **TimeoutConstants** — `IMPLICIT_WAIT`, `EXPLICIT_WAIT`, `PAGE_LOAD_TIMEOUT`, `FLUENT_WAIT`, `FLUENT_POLLING`

### `driver/` - WebDriver Lifecycle
Factory pattern + ThreadLocal for thread-safe parallel execution.
- **DriverFactory** — `ThreadLocal<WebDriver>` for isolation. `createDriver(browser)` with switch for Chrome/Firefox/Edge. `getDriver()` and `quitDriver()`. Headless toggle from config. Chrome args: `--no-sandbox`, `--disable-dev-shm-usage`, `--window-size=1920,1080`

### `listeners/` - TestNG Event Handlers
Hook into the test lifecycle for reporting, retry, screenshots.
- **TestListener** (`ITestListener`) — `onTestStart()` creates ExtentTest, `onTestFailure()` captures screenshot, `onFinish()` flushes report
- **RetryAnalyzer** (`IRetryAnalyzer`) — Retries failed tests up to `retry.count` from config
- **RetryTransformer** (`IAnnotationTransformer`) — Auto-applies RetryAnalyzer to all `@Test` methods without manual annotation

### `models/` - POJO Data Classes
Request/response POJOs for API testing with Jackson.
- **HttpBinResponse**, **PostPayload**, or any domain POJOs you need
- Use Jackson annotations: `@JsonProperty`, `@JsonIgnoreProperties(ignoreUnknown = true)`

### `pages/` - Page Object Model
One class per page. Pure POM (no PageFactory — avoids `StaleElementReferenceException`).
- **BasePage** (abstract) — Constructor takes `WebDriver`. Reusable methods: `waitAndClick(By)`, `waitAndType(By, String)`, `waitAndGetText(By)`, `selectDropdown()`, `isLoaded()` (abstract)
- **LoginPage**, **SecurePage**, **DropdownPage**, etc. — Extend BasePage. Private `By` locators. Public action methods returning the next page object (fluent)

### `reporting/` - Test Reporting
ExtentReports integration with thread safety.
- **ExtentReportManager** — Singleton. `ThreadLocal<ExtentTest>` for parallel tests. Creates `test-output/ExtentReport.html`. Methods: `createTest()`, `getTest()`, `flush()`

### `utils/` - Shared Utilities
Stateless helper methods used across tests.
- **WaitHelper** — Static methods: `waitForVisibility(driver, locator, timeout)`, `waitForClickability()`, `waitForInvisibility()`, `fluentWait()`
- **ScreenshotUtil** — `captureScreenshotAsBase64(driver)` for report embedding, `captureScreenshot(driver, filePath)` for file save
- **JsonUtils** — Jackson wrapper: `serialize(object)`, `deserialize(json, class)`, `getObjectMapper()`

---

## Test Packages (`src/test/java/ra/hul/tests/`)

### `base/` - Abstract Test Classes
- **BaseTest** — `@BeforeMethod` calls `DriverFactory.createDriver()`, `@AfterMethod` calls `DriverFactory.quitDriver()`. Has `navigateTo(path)` helper
- **BaseApiTest** — `@BeforeMethod` sets `RestAssured.baseURI` from config. No WebDriver needed

### `ui/` - UI Test Classes
Each extends `BaseTest`. Use page objects, never raw Selenium calls in tests.
- **LoginTest**, **DropdownTest**, **DynamicLoadingTest**, **FileUploadTest**, **JavaScriptAlertsTest**, etc.

### `api/` - API Test Classes
Each extends `BaseApiTest`. Use ApiRequestBuilder or raw RestAssured.
- **GetRequestTest**, **PostRequestTest**, **AuthTest**, **StatusCodeTest**, **ResponseHeadersTest**, **SchemaValidationTest**, etc.

---

## Test Resources (`src/test/resources/`)

| File | Purpose |
|------|---------|
| `config.properties` | Default config (Chrome, headless, timeouts, URLs) |
| `config-dev.properties` | Dev overrides (headless=false for visual debugging) |
| `testng.xml` | Test suite config with listeners and parallel settings |
| `log4j2.xml` | Logging config (console + file output) |
| `schemas/` | JSON schema files for API contract validation |

---

## Design Patterns to Implement

| Pattern | Where | Why |
|---------|-------|-----|
| **Factory** | DriverFactory | Browser creation abstraction |
| **Singleton** | ConfigReader, ExtentReportManager | One instance shared across threads |
| **Builder** | ApiRequestBuilder | Fluent request construction |
| **Template Method** | BasePage, BaseTest | Skeleton in parent, behavior in child |
| **ThreadLocal** | DriverFactory, ExtentReportManager | Thread-safe parallel execution |
| **Page Object Model** | pages/ package | Encapsulate locators and interactions |
| **Listener/Observer** | TestListener | Event-driven reporting and screenshots |
| **Strategy** | DriverFactory (browser switch) | Pluggable browser creation |

---

## Reference Implementation

Your hotstar-interview-framework repo at `/Users/rahulmishra/Desktop/Code/hotstar-interview-framework` has a working implementation of all the above. Use it as reference but code this one from scratch for deeper understanding.
