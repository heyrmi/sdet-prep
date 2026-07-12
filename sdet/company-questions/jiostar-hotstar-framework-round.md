# JioStar - Test Frameworks, Design & Orchestration Round Preparation

**Role:** Senior SDET I | **Round:** Test Frameworks, Design and Orchestration | **Date:** 2026-03-24, 3:30 PM

---

## Table of Contents

1. [Framework Walkthrough & Architecture](#1-framework-walkthrough--architecture)
2. [Design Patterns in Your Framework](#2-design-patterns-in-your-framework)
3. [Java Concurrency & Thread Safety](#3-java-concurrency--thread-safety)
4. [WebDriver Management Deep Dive](#4-webdriver-management-deep-dive)
5. [Page Object Model & Page Design](#5-page-object-model--page-design)
6. [TestNG In-Depth](#6-testng-in-depth)
7. [Wait Strategies & Synchronization](#7-wait-strategies--synchronization)
8. [Configuration & Environment Management](#8-configuration--environment-management)
9. [API Testing with RestAssured](#9-api-testing-with-restassured)
10. [Reporting & Logging](#10-reporting--logging)
11. [CI/CD & Test Orchestration](#11-cicd--test-orchestration)
12. [Parallel Execution & Selenium Grid](#12-parallel-execution--selenium-grid)
13. [Docker & Containerization](#13-docker--containerization)
14. [Flaky Tests & Retry Mechanisms](#14-flaky-tests--retry-mechanisms)
15. [Streaming Platform (JioStar/Hotstar) Specific Testing](#15-streaming-platform-jiostarhotstar-specific-testing)
16. [Scenario-Based / System Design Questions](#16-scenario-based--system-design-questions)
17. [General SDET Concepts](#17-general-sdet-concepts)

---

## 1. Framework Walkthrough & Architecture

### Q1.1: Walk me through your framework architecture. Explain each layer and why it exists.

**Answer:**

My framework follows a **layered architecture** with clear separation of concerns:

```
src/main/java/ra/hul/framework/
├── driver/          → WebDriver lifecycle (ThreadLocal-based)
├── config/          → Configuration management (Properties + System overrides)
├── constants/       → Framework-wide constants (paths, browser names)
├── pages/           → Page Object Model (BasePage + concrete pages)
├── api/             → REST API client (RestAssured wrapper)
├── models/          → Data POJOs (Lombok @Data @Builder)
├── listeners/       → TestNG event hooks (reporting, retry, screenshots)
├── reporting/       → ExtentReports management (thread-safe)
├── utils/           → Wait utilities (Explicit + Fluent waits)

src/test/java/ra/hul/tests/
├── base/            → BaseTest with @BeforeMethod/@AfterMethod
├── ui/              → UI test classes (LoginTest, DropdownTest, etc.)
├── api/             → API test classes (HttpBinApiTest)
```

**Why this layering?**

- **Driver layer** is isolated so switching from local to Grid/Cloud requires changes in ONE class
- **Config layer** supports environment overlays (`config-dev.properties`) and system property overrides for CI/CD flexibility
- **Pages layer** encapsulates locators (private) and exposes business actions — tests never touch `By` locators directly
- **Listeners are registered in testng.xml**, not via `@Listeners` annotation, keeping test classes clean
- **Utils are stateless** — they pull the driver from `DriverManager.getDriver()` via ThreadLocal, so they work in parallel

**Key design decisions:**
1. No PageFactory — avoids `StaleElementReferenceException` caused by cached elements
2. `@BeforeMethod` (not `@BeforeClass`) — ensures fresh browser per test for isolation
3. `ThreadLocal<WebDriver>` — enables safe parallel execution
4. Config priority: System props > Env config > Base config — CI/CD can override anything without touching files

---

### Q1.2: If you had to add mobile testing support (Appium) to this framework, what would you change?

**Answer:**

I'd make these changes while keeping backward compatibility:

1. **Abstract the DriverManager** — Extract a `DriverProvider` interface with methods `initDriver()`, `getDriver()`, `quitDriver()`. Create `WebDriverProvider` (existing logic) and `AppiumDriverProvider` (for mobile)
2. **Use a Factory/Strategy pattern** — A `DriverFactory` that reads `platform=web|android|ios` from config and returns the right provider
3. **BasePage remains the same** — Since Appium's `MobileElement` implements `WebElement`, the `click()`, `type()`, `getText()` methods in BasePage work without changes
4. **Add mobile-specific page methods** — Swipe, scroll, tap gestures in a `MobileBasePage extends BasePage`
5. **Config additions** — `platform`, `device.name`, `app.path`, `appium.server.url`

```java
public interface DriverProvider {
    WebDriver initDriver();
    WebDriver getDriver();
    void quitDriver();
}
```

The key insight: **your framework's ThreadLocal pattern works identically for Appium** since `AppiumDriver` extends `RemoteWebDriver`.

---

### Q1.3: What are the limitations of your current framework? What would you improve?

**Answer:**

| Limitation | Improvement |
|---|---|
| No data-driven support from external files | Add Excel/JSON/CSV data readers with a `DataProvider` factory |
| No screenshot on every step (only on failure) | Add optional step-level screenshots via an `@Screenshot` annotation or AOP |
| Config uses `.properties` (flat structure) | Migrate to YAML for hierarchical config with nested sections |
| No visual regression testing | Integrate Ashot or Percy for layout comparison |
| No service virtualization | Add WireMock for stubbing downstream APIs during integration tests |
| Only local browser execution | Add Selenium Grid / BrowserStack `RemoteWebDriver` capability in DriverManager |
| No Cucumber/BDD layer | Add optional Cucumber runners for teams that need Gherkin specs |
| Hard-coded test data in DataProvider | Externalize to JSON/CSV files for better maintainability |

---

## 2. Design Patterns in Your Framework

### Q2.1: What design patterns have you used? Explain each with the specific class.

**Answer:**

| Pattern | Class | How It's Used |
|---|---|---|
| **ThreadLocal + Factory** | `DriverManager` | ThreadLocal isolates driver per thread; factory method `createDriver()` builds Chrome/Firefox/Edge based on config |
| **Singleton** | `ConfigManager` | Static block loads properties once at class loading; immutable after init |
| **Template Method** | `BasePage` | Defines `click()`, `type()`, `getText()` as reusable steps; forces subclasses to implement `isLoaded()` |
| **Page Object Model** | `LoginPage`, `CheckboxPage`, etc. | Encapsulates locators + page actions; returns `this` for fluent chaining or next page object for navigation |
| **Observer/Listener** | `TestListener` | Implements `ITestListener` to hook into TestNG lifecycle — auto-captures screenshots on failure, logs pass/fail to ExtentReports |
| **Strategy** | `RetryAnalyzer` | Implements `IRetryAnalyzer` — pluggable retry logic; count configurable via `retry.count` property |
| **Builder** | `User` (Lombok `@Builder`) | Constructs test data objects: `User.builder().name("Rahul").email("r@co.com").build()` |
| **Fluent Interface** | Page objects | Method chaining: `loginPage.open().enterUsername("x").enterPassword("y")` |
| **Annotation Transformer** | `RetryTransformer` | `IAnnotationTransformer` auto-applies `RetryAnalyzer` to ALL `@Test` methods without manual per-test annotation |

---

### Q2.2: Why did you use the Factory pattern in DriverManager instead of just `new ChromeDriver()`?

**Answer:**

Three reasons:

1. **Open/Closed Principle** — Adding a new browser (Safari, Opera) means adding one `case` in the switch — no changes to test code or BaseTest
2. **Encapsulation of creation logic** — Browser options like `--headless=new`, `--no-sandbox`, `--disable-dev-shm-usage` are Chrome-specific. Tests shouldn't know about browser internals
3. **Config-driven** — The browser is decided by `config.properties` or `-Dbrowser=firefox`, not by code. Same test suite runs on any browser without recompilation

```java
private static WebDriver createDriver(String browser, boolean headless) {
    switch (browser) {
        case "chrome" -> {
            ChromeOptions options = new ChromeOptions();
            if (headless) options.addArguments("--headless=new");
            options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
            return new ChromeDriver(options);
        }
        case "firefox" -> { /* ... */ }
        case "edge" -> { /* ... */ }
        default -> throw new IllegalArgumentException("Unsupported browser: " + browser);
    }
}
```

---

### Q2.3: Explain the Template Method pattern in BasePage. Why is `isLoaded()` abstract?

**Answer:**

`BasePage` defines the **skeleton** of common page operations (click, type, getText) while deferring the **page-specific verification** to subclasses via `abstract boolean isLoaded()`.

```java
public abstract class BasePage {
    public abstract boolean isLoaded();  // Each page defines its own "ready" check

    protected void click(By locator) {
        WaitUtils.waitForClickable(locator).click();
    }
    // ... other common methods
}
```

**Why `isLoaded()` is abstract:**
- `LoginPage.isLoaded()` checks if the login button is visible
- `SecurePage.isLoaded()` checks if the logout button is visible
- Each page has a **unique indicator** of readiness — there's no one-size-fits-all
- Tests can call `Assert.assertTrue(loginPage.isLoaded())` to verify navigation succeeded
- It's a **contract** — every new page MUST define how to verify it loaded correctly, preventing developers from forgetting this check

---

### Q2.4: What is the difference between Factory and Abstract Factory pattern? Where would Abstract Factory be useful in test frameworks?

**Answer:**

- **Factory** — One method creates objects of a single family (e.g., `createDriver()` returns `WebDriver`)
- **Abstract Factory** — Creates families of related objects (e.g., a `TestEnvironmentFactory` that creates driver + config + reporter together)

**Where Abstract Factory helps:**

```java
interface TestEnvironmentFactory {
    WebDriver createDriver();
    ApiClient createApiClient();
    ReportManager createReporter();
}

class LocalEnvironmentFactory implements TestEnvironmentFactory { /* local chrome, localhost API, file report */ }
class CloudEnvironmentFactory implements TestEnvironmentFactory { /* BrowserStack, staging API, S3 report */ }
```

This ensures **consistency** — you can't accidentally pair a local driver with a production API client. One factory produces all the pieces that go together.

---

## 3. Java Concurrency & Thread Safety

### Q3.1: What is `synchronized` in Java? Where and why is it used in your framework?

**Answer:**

`synchronized` is a Java keyword that ensures only **one thread at a time** can execute a block/method, preventing race conditions on shared resources.

**In my framework, `synchronized` is used in `ReportManager`:**

```java
public static synchronized void initReports() {
    if (extent == null) {  // Double-check needed because of synchronized
        ExtentSparkReporter spark = new ExtentSparkReporter("target/extent-report.html");
        spark.config().setTheme(Theme.DARK);
        extent = new ExtentReports();
        extent.attachReporter(spark);
    }
}

public static synchronized ExtentTest createTest(String testName) {
    ExtentTest test = extent.createTest(testName);
    testThreadLocal.set(test);
    return test;
}
```

**Why synchronized here but NOT in DriverManager?**
- `ReportManager.extent` is a **shared singleton** — all threads write to the same `ExtentReports` instance. Without `synchronized`, two threads calling `initReports()` simultaneously could create two `ExtentReports` objects, one overwriting the other
- `DriverManager` uses **ThreadLocal** — each thread has its own driver, so there's no shared state to protect. `synchronized` would add unnecessary overhead

**Key rule:** Use `synchronized` when multiple threads access **shared mutable state**. Use `ThreadLocal` when each thread needs **its own isolated copy**.

---

### Q3.2: What is `ThreadLocal` in Java? Explain with the driver flow in your code.

**Answer:**

`ThreadLocal` provides **thread-confined variables** — each thread has its own independent copy. No synchronization needed because threads never see each other's values.

**Complete driver flow:**

```
Thread-1 (LoginTest)              Thread-2 (DropdownTest)
────────────────────              ─────────────────────────
@BeforeMethod                     @BeforeMethod
  DriverManager.initDriver()        DriverManager.initDriver()
  ├─ driverThreadLocal.get()        ├─ driverThreadLocal.get()
  │  → null (first time)            │  → null (first time)
  ├─ createDriver("chrome")         ├─ createDriver("chrome")
  │  → new ChromeDriver()           │  → new ChromeDriver()
  ├─ driverThreadLocal.set(d1)      ├─ driverThreadLocal.set(d2)
  │                                 │
@Test                             @Test
  DriverManager.getDriver()        DriverManager.getDriver()
  → returns d1 (Thread-1's)        → returns d2 (Thread-2's)
  │                                 │
@AfterMethod                      @AfterMethod
  DriverManager.quitDriver()        DriverManager.quitDriver()
  ├─ d1.quit()                      ├─ d2.quit()
  ├─ driverThreadLocal.remove()     ├─ driverThreadLocal.remove()
```

**Critical: Why `remove()` is essential:**
- Thread pools (used by TestNG) **reuse threads**
- Without `remove()`, a recycled thread would find the **previous driver** (now quit) in its ThreadLocal
- This causes `SessionNotFoundException` or memory leaks
- `remove()` cleans up the ThreadLocal entry, ensuring the next test on this thread creates a fresh driver

---

### Q3.3: What happens if two threads call `initDriver()` at the exact same time? Is there a race condition?

**Answer:**

**No race condition.** Here's why:

```java
private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

public static WebDriver initDriver() {
    if (driverThreadLocal.get() != null) {  // Thread-local read — only sees OWN value
        return driverThreadLocal.get();
    }
    WebDriver driver = createDriver(browser, headless);  // New instance per thread
    driverThreadLocal.set(driver);  // Thread-local write — only affects OWN slot
    return driver;
}
```

- `driverThreadLocal.get()` reads from the **calling thread's own slot** — Thread-1 cannot see Thread-2's value
- `driverThreadLocal.set()` writes to the **calling thread's own slot**
- `createDriver()` creates a **new object** each time — no shared state
- There is **no shared mutable field** being read-then-written, so no race condition exists

This is precisely why ThreadLocal is preferred over Singleton + synchronized for WebDriver — **no locking overhead, no contention, true parallelism**.

---

### Q3.4: What is the difference between `synchronized`, `volatile`, `ThreadLocal`, and `Lock`?

**Answer:**

| Mechanism | Purpose | Use Case in Testing |
|---|---|---|
| `synchronized` | Mutual exclusion — only one thread enters the block | `ReportManager.initReports()` — protect shared `ExtentReports` singleton |
| `volatile` | Visibility guarantee — changes are immediately visible to all threads | A shared `boolean isRunning` flag to signal tests to stop |
| `ThreadLocal` | Thread isolation — each thread gets its own copy | `DriverManager` — each thread gets its own `WebDriver` |
| `ReentrantLock` | Same as synchronized but with tryLock, fairness, interruptibility | When you need timeout on acquiring lock (e.g., waiting for shared resource with a deadline) |

**In my framework:**
- `ThreadLocal` for **driver** and **ExtentTest** (per-thread isolation)
- `synchronized` for **ExtentReports init** and **createTest** (shared singleton access)
- No `volatile` or `Lock` needed — ThreadLocal eliminated the need

---

## 4. WebDriver Management Deep Dive

### Q4.1: Why `--headless=new` instead of `--headless`?

**Answer:**

Chrome has **two headless modes**:
- `--headless` (old) — Uses a separate headless Chrome shell with rendering differences. Some CSS/JS behaves differently than headed mode
- `--headless=new` (Chrome 112+) — Uses the **same Chrome binary** as headed mode but without a visible window. Behavior is identical to headed Chrome

**Why this matters for testing:**
- Tests that pass in `--headless=new` are **guaranteed to pass** in headed mode
- The old `--headless` had known issues with file downloads, print-to-PDF, and some JavaScript APIs
- Selenium team officially recommends `--headless=new`

---

### Q4.2: What are `--no-sandbox` and `--disable-dev-shm-usage`? Why are they in your framework?

**Answer:**

```java
options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
```

- **`--no-sandbox`** — Disables Chrome's sandbox security model. Required in Docker/CI environments where Chrome runs as root (sandbox can't run inside a container's already-sandboxed environment). Without this, Chrome crashes with "Failed to move to new namespace"
- **`--disable-dev-shm-usage`** — `/dev/shm` (shared memory) in Docker defaults to 64MB, but Chrome uses it for inter-process communication. When it fills up, Chrome crashes. This flag makes Chrome write to `/tmp` instead

**When to use:** Only in containerized/CI environments. In local development with `headless=false`, these flags are still harmless but unnecessary.

---

### Q4.3: How would you modify DriverManager to support RemoteWebDriver for Selenium Grid?

**Answer:**

```java
private static WebDriver createDriver(String browser, boolean headless) {
    String gridUrl = ConfigManager.getOrDefault("grid.url", null);

    if (gridUrl != null) {
        // Remote execution on Selenium Grid / BrowserStack / SauceLabs
        ChromeOptions options = new ChromeOptions();
        if (headless) options.addArguments("--headless=new");
        return new RemoteWebDriver(new URL(gridUrl), options);
    }

    // Local execution (existing switch-case)
    switch (browser) {
        case "chrome" -> { /* existing code */ }
        // ...
    }
}
```

**Config:**
```properties
# For Grid execution
grid.url=http://selenium-hub:4444/wd/hub

# For BrowserStack
grid.url=https://user:key@hub-cloud.browserstack.com/wd/hub
```

**Key point:** The rest of the framework (pages, tests, waits) doesn't change at all — they all call `DriverManager.getDriver()` which returns `WebDriver` regardless of local or remote.

---

### Q4.4: What is the difference between `driver.close()` and `driver.quit()`? Why do you use `quit()` in teardown?

**Answer:**

- `driver.close()` — Closes the **current browser window/tab** only. If there's only one window, it closes the browser BUT the WebDriver session remains active (leaked process)
- `driver.quit()` — Closes **all windows** AND terminates the WebDriver session (kills the chromedriver/geckodriver process)

**I use `quit()` because:**
1. It ensures the browser process is fully terminated — no zombie chromedriver processes accumulating in CI
2. It releases the port the driver was using
3. Combined with `driverThreadLocal.remove()`, it fully cleans up both the Java-side reference and the OS-side process

---

## 5. Page Object Model & Page Design

### Q5.1: Why are locators `private` in your page objects? Why not `protected` or `public`?

**Answer:**

```java
public class LoginPage extends BasePage {
    private final By usernameInput = By.id("username");      // PRIVATE
    private final By passwordInput = By.id("password");      // PRIVATE
    private final By loginButton = By.cssSelector("button[type='submit']");
```

**Reasons:**
1. **Encapsulation** — If the HTML changes from `id="username"` to `data-testid="login-username"`, I change ONE line in `LoginPage`. No test file is affected
2. **Preventing anti-patterns** — If locators were `protected`, test classes could directly use `driver.findElement(loginPage.usernameInput)`, bypassing the wait logic in `type()`
3. **Single Responsibility** — Page objects own locators; tests own assertions. Public locators blur this line
4. **Maintainability** — With 200+ page objects, public locators mean a locator change could break tests across multiple files. Private locators guarantee the blast radius is ONE file

---

### Q5.2: Why didn't you use Selenium's `PageFactory` and `@FindBy`?

**Answer:**

```java
// PageFactory approach (I avoided this)
@FindBy(id = "username")
private WebElement usernameInput;

public LoginPage() {
    PageFactory.initElements(driver, this);  // Initializes ALL elements at construction
}
```

**Why I avoid it:**
1. **StaleElementReferenceException** — `PageFactory.initElements()` finds elements at page object construction time and caches them. If the DOM changes (AJAX reload, SPA navigation), cached references become stale
2. **No built-in wait** — `@FindBy` doesn't wait for visibility or clickability. You still need explicit waits before interacting
3. **Implicit coupling** — The page object is tied to the driver at construction time, making it harder to test or mock

**My approach is safer:**
```java
protected void click(By locator) {
    WaitUtils.waitForClickable(locator).click();  // Fresh lookup every time
}
```

Every interaction does a **fresh element lookup** with a proper wait — no stale references possible.

---

### Q5.3: What is the Fluent Interface pattern? Show it in your page objects.

**Answer:**

Fluent Interface means methods **return `this`** (or the next page object), enabling method chaining:

```java
// LoginPage.java
public LoginPage enterUsername(String username) {
    type(usernameInput, username);
    return this;  // Returns self for chaining
}

public LoginPage enterPassword(String password) {
    type(passwordInput, password);
    return this;  // Returns self for chaining
}

// Test usage — reads like English
loginPage.open()
    .enterUsername("tomsmith")
    .enterPassword("SuperSecretPassword!");
```

**When to return a different page object:**
```java
// If login navigates to SecurePage
public SecurePage clickLogin() {
    click(loginButton);
    return new SecurePage();  // Returns NEXT page — guides the test flow
}
```

**Benefits:**
- Tests read like user stories
- IDE autocomplete guides you through the valid flow
- Compile-time safety — you can't call `getFlashMessage()` until you've navigated to `SecurePage`

---

### Q5.4: Should page objects contain assertions? Why or why not?

**Answer:**

**No. Assertions belong in test classes, not page objects.**

**Reasons:**
1. **Single Responsibility** — Page objects model the page; tests verify behavior. Mixing them makes both harder to maintain
2. **Reusability** — `loginPage.enterUsername("admin")` can be used in a happy-path test AND a negative test. If `enterUsername()` asserted success, it would break the negative test
3. **Clarity** — When a test fails, the assertion stack trace should point to the TEST, not deep inside a page object

**Exception:** `isLoaded()` returns a boolean — it checks state but doesn't assert. The TEST decides whether to assert on it:
```java
Assert.assertTrue(securePage.isLoaded(), "Secure page should be loaded after login");
```

---

## 6. TestNG In-Depth

### Q6.1: Why `@BeforeMethod` instead of `@BeforeClass` for driver setup?

**Answer:**

```java
// BaseTest.java
@BeforeMethod
public void setUp() {
    DriverManager.initDriver();  // Fresh driver for EACH test
}

@AfterMethod(alwaysRun = true)
public void tearDown() {
    DriverManager.quitDriver();  // Clean shutdown after EACH test
}
```

| Aspect | `@BeforeMethod` | `@BeforeClass` |
|---|---|---|
| **Isolation** | Each test gets a fresh browser — no cookie/session leakage | All tests share one browser — login state persists |
| **Parallel safety** | Works with `parallel="methods"` | Breaks with `parallel="methods"` — shared driver across threads |
| **Failure containment** | Test 2's crash doesn't affect Test 3 | Test 2's crash can leave browser in broken state for Test 3 |
| **Speed** | Slower (browser launch per test) | Faster (one browser for all tests) |
| **Trade-off** | Reliability over speed | Speed over reliability |

**I chose `@BeforeMethod` because reliability > speed.** Flaky tests from shared state waste more time than extra browser launches.

**`alwaysRun = true`** on `@AfterMethod` ensures teardown runs even when the test fails/skips — prevents zombie browser processes.

---

### Q6.2: What is `IAnnotationTransformer`? How does `RetryTransformer` work?

**Answer:**

`IAnnotationTransformer` is a TestNG listener that **modifies annotations at runtime** before tests execute. It's a suite-level listener (runs once per suite), not a test-level listener.

```java
public class RetryTransformer implements IAnnotationTransformer {
    @Override
    public void transform(ITestAnnotation annotation, Class testClass,
                          Constructor testConstructor, Method testMethod) {
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}
```

**What this does:** Before each `@Test` method runs, TestNG calls `transform()`. This injects `RetryAnalyzer` into every test's annotation — equivalent to writing `@Test(retryAnalyzer = RetryAnalyzer.class)` on every single method.

**Why use this instead of manual annotation:**
1. **DRY** — Don't repeat `retryAnalyzer = RetryAnalyzer.class` on 500 test methods
2. **Enforceable** — New tests automatically get retry logic without developers remembering to add it
3. **Configurable** — Can add conditions (e.g., only apply retry to tests tagged `@Flaky`)

**Registration in testng.xml (NOT @Listeners):**
```xml
<listeners>
    <listener class-name="ra.hul.framework.listeners.RetryTransformer"/>
</listeners>
```

---

### Q6.3: Explain TestNG execution order with all annotations.

**Answer:**

```
@BeforeSuite          → Once per suite (DB setup, Grid init)
  @BeforeTest         → Once per <test> tag in testng.xml
    @BeforeClass      → Once per test class
      @BeforeMethod   → Before each @Test method
        @Test         → The actual test
      @AfterMethod    → After each @Test method
    @AfterClass       → Once per test class
  @AfterTest          → Once per <test> tag
@AfterSuite           → Once per suite (report flush, cleanup)
```

**In my framework:**
- `@BeforeMethod` / `@AfterMethod` → BaseTest (driver init/quit)
- `@BeforeClass` → HttpBinApiTest (ApiClient init — no browser needed for API tests)
- `ITestListener.onStart()` → ReportManager.initReports() (suite-level)
- `ITestListener.onFinish()` → ReportManager.flushReport() (suite-level)

---

### Q6.4: What are the different parallel execution modes in TestNG?

**Answer:**

```xml
<suite parallel="X" thread-count="3">
```

| Mode | Behavior | When to Use |
|---|---|---|
| `parallel="methods"` | Each `@Test` method runs in its own thread | Maximum parallelism, tests must be independent |
| `parallel="classes"` | Each test class runs in its own thread, methods within a class are sequential | When tests within a class share state |
| `parallel="tests"` | Each `<test>` tag in testng.xml runs in its own thread | When you want UI tests and API tests in parallel |
| `parallel="instances"` | Each class instance runs in its own thread | With `@Factory` for data-driven class instances |
| `false` (default) | Sequential execution | Debugging, or when tests have dependencies |

**My framework uses `parallel="methods"` with `thread-count="3"` because:**
- Each test is fully independent (fresh driver via `@BeforeMethod`)
- ThreadLocal ensures driver isolation
- `thread-count="3"` balances speed vs. machine resource limits

---

### Q6.5: What is the difference between `@DataProvider` and `@Factory` in TestNG?

**Answer:**

**`@DataProvider`** — Runs the SAME test method multiple times with different parameters:
```java
@Test(dataProvider = "invalidCredentials")
public void loginTest(String user, String pass, String error) { /* ONE method, THREE runs */ }

@DataProvider
public Object[][] invalidCredentials() {
    return new Object[][] {
        {"tomsmith", "wrong", "Your password is invalid!"},
        {"invalid", "SuperSecretPassword!", "Your username is invalid!"},
    };
}
```

**`@Factory`** — Creates MULTIPLE INSTANCES of the test class, each with different constructor args:
```java
public class LoginTest {
    private String browser;
    @Factory
    public static Object[] createInstances() {
        return new Object[] {
            new LoginTest("chrome"),
            new LoginTest("firefox"),
            new LoginTest("edge")
        };
    }
}
```

| Aspect | `@DataProvider` | `@Factory` |
|---|---|---|
| Scope | Per method | Per class |
| Use case | Same test, different data | Same tests, different configuration (browser, env) |
| Parallel | `parallel=true` on DataProvider | `parallel="instances"` on suite |

---

### Q6.6: What is the difference between Hard Assert and Soft Assert? When do you use each?

**Answer:**

**Hard Assert** (`Assert.assertEquals`) — Fails immediately. Test stops at the first failure.

**Soft Assert** (`SoftAssert`) — Collects all failures, continues execution, reports all at the end.

```java
// Hard Assert — stops at first failure
Assert.assertEquals(response.statusCode(), 200);  // If this fails...
Assert.assertNotNull(response.body());             // ...this never runs

// Soft Assert — collects all failures
SoftAssert soft = new SoftAssert();
soft.assertTrue(loginPage.getFlashMessage().contains(expectedError));
soft.assertTrue(loginPage.isUsernameFieldDisplayed());
soft.assertAll();  // Reports ALL failures at once
```

**When I use each:**
- **Hard Assert** — For preconditions that must pass for the test to be meaningful (status code 200, page loaded)
- **Soft Assert** — For validating multiple independent properties of a response/page (all form fields present, all error messages correct)

**In my framework,** the `login_invalidData_shouldShowAppropriateError` test uses Soft Assert because we want to check all error messages even if one fails.

---

## 7. Wait Strategies & Synchronization

### Q7.1: Explain the three types of waits in Selenium. Which do you use and why?

**Answer:**

| Wait Type | How It Works | Pros | Cons |
|---|---|---|---|
| **Implicit Wait** | Polls DOM for element existence for a set duration | Simple, applies globally | Can't wait for specific conditions (clickable, visible); mixes with explicit waits causing unpredictable timeouts |
| **Explicit Wait** (`WebDriverWait`) | Waits for a specific `ExpectedCondition` with timeout | Precise, condition-based | Slightly more verbose |
| **Fluent Wait** | Like explicit but with custom polling interval + ignored exceptions | Full control over polling + exception handling | Most verbose |
| **Thread.sleep()** | Hard pause | None | **NEVER use** — wastes time or races |

**My framework uses Explicit + Fluent waits only:**

```java
// Explicit Wait — for standard interactions
public static WebElement waitForClickable(By locator) {
    return new WebDriverWait(DriverManager.getDriver(),
            Duration.ofSeconds(ConfigManager.getInt("explicit.wait")))
        .until(ExpectedConditions.elementToBeClickable(locator));
}

// Fluent Wait — for dynamic/AJAX elements (spinners, animations)
public static WebElement fluentWait(By locator) {
    return new FluentWait<>(DriverManager.getDriver())
        .withTimeout(Duration.ofSeconds(ConfigManager.getInt("fluent.wait")))
        .pollingEvery(Duration.ofMillis(ConfigManager.getInt("polling.time")))
        .ignoring(NoSuchElementException.class)
        .ignoring(StaleElementReferenceException.class)
        .until(driver -> driver.findElement(locator));
}
```

**Why no implicit waits:**
- Mixing implicit and explicit waits causes **unpredictable timeout behavior** (they can compound)
- Implicit waits only check element existence, not visibility/clickability
- You lose control over what condition you're waiting for

---

### Q7.2: What is `StaleElementReferenceException`? How does your framework handle it?

**Answer:**

**Cause:** A `StaleElementReferenceException` occurs when:
1. You found an element (`WebElement e = driver.findElement(...)`)
2. The DOM changed (AJAX update, page navigation, JS re-render)
3. You try to interact with the old reference (`e.click()`) — it's now "stale"

**How my framework prevents it:**

1. **No PageFactory** — I don't cache elements. Every interaction does a fresh lookup:
   ```java
   protected void click(By locator) {
       WaitUtils.waitForClickable(locator).click();  // Find + wait + click in one shot
   }
   ```

2. **FluentWait ignores it** — For dynamic elements, FluentWait retries on stale references:
   ```java
   .ignoring(StaleElementReferenceException.class)
   ```

3. **Locators stored as `By`, not `WebElement`** — `By` objects are just "descriptions of how to find an element." They never go stale. Only resolved `WebElement` references can go stale.

---

## 8. Configuration & Environment Management

### Q8.1: Explain the config priority in your framework. How does environment-specific config work?

**Answer:**

**Priority (highest to lowest):**
1. **System properties** (`-Dbrowser=firefox` via Maven/CLI)
2. **Environment config** (`config-dev.properties` loaded via `-Denv=dev`)
3. **Base config** (`config.properties`)

```java
// ConfigManager.java
static {
    // Step 1: Load base config
    properties.load(new FileInputStream("src/test/resources/config.properties"));

    // Step 2: Overlay environment config (if -Denv=X is passed)
    String env = System.getProperty("env");
    if (env != null) {
        properties.load(new FileInputStream("config-" + env + ".properties"));
        // Properties.load() OVERWRITES duplicate keys — dev values override base
    }
}

public static String get(String key) {
    // Step 3: System property wins over everything
    String systemProp = System.getProperty(key);
    if (systemProp != null) return systemProp;
    return properties.getProperty(key);
}
```

**Example:**

| Config | `browser` value | `headless` value |
|---|---|---|
| `config.properties` | chrome | true |
| `config-dev.properties` | chrome | **false** |
| `-Dbrowser=firefox` | **firefox** | false |

```bash
# CI: uses base config (chrome, headless)
mvn clean test

# Dev: uses dev overlay (chrome, headed)
mvn clean test -Denv=dev

# Override everything: firefox, headed
mvn clean test -Denv=dev -Dbrowser=firefox
```

**Why this design:** CI/CD pipelines can customize execution without touching code or config files — just pass system properties.

---

### Q8.2: Why use a static block in ConfigManager instead of a constructor?

**Answer:**

```java
public class ConfigManager {
    private static final Properties properties = new Properties();

    static {
        // Executes ONCE when class is loaded — before any method call
        properties.load(new FileInputStream("config.properties"));
    }
}
```

**Reasons:**
1. **Guaranteed initialization order** — The static block runs when the JVM loads the class, BEFORE any `get()` call. No risk of reading from uninitialized properties
2. **Thread safety for free** — JVM guarantees static initializers run exactly once, even with multiple threads. No need for `synchronized` or double-checked locking
3. **No instance needed** — All methods are `static`. A constructor would require `new ConfigManager()`, which is unnecessary since there's no instance state
4. **Fail-fast** — If config loading fails, the `RuntimeException` thrown in the static block prevents the class from loading — tests fail immediately with a clear error, not with mysterious `NullPointerException`s later

---

## 9. API Testing with RestAssured

### Q9.1: Explain the architecture of your ApiClient. Why wrap RestAssured?

**Answer:**

```java
public class ApiClient {
    private final RequestSpecification requestSpec;

    public ApiClient() {
        requestSpec = new RequestSpecBuilder()
            .setBaseUri(ConfigManager.get("api.base.url"))
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .setConfig(/* timeout config */)
            .build();
    }

    public Response get(String endpoint) {
        return RestAssured.given().spec(requestSpec).when().get(endpoint)
            .then().extract().response();
    }
}
```

**Why wrap it:**
1. **DRY** — Base URI, content type, timeouts configured once, not in every test
2. **Consistency** — All API calls go through the same pipeline (logging, headers, auth)
3. **Swappability** — If we migrate from RestAssured to another HTTP client, only `ApiClient` changes
4. **Timeout enforcement** — Connection and socket timeouts from config, not forgotten by individual tests
5. **Auth centralization** — `getWithAuth()` adds basic auth without tests knowing the mechanism

---

### Q9.2: What is JSON Schema Validation? How do you use it in tests?

**Answer:**

JSON Schema defines the **structure and types** of a JSON response — like a contract. Schema validation verifies the API response matches the expected contract.

```java
// Test
response.then().assertThat()
    .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/user-schema.json"));

// Schema (src/test/resources/schemas/user-schema.json)
{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "required": ["json"],
    "properties": {
        "json": {
            "type": "object",
            "required": ["id", "name", "email", "job"],
            "properties": {
                "id": { "type": "integer" },
                "name": { "type": "string" }
            },
            "additionalProperties": false
        }
    }
}
```

**Why schema validation matters:**
- Catches **structural changes** — new field added, type changed from string to number, required field removed
- Acts as a **contract test** — backend changes that break the schema fail tests before reaching production
- `additionalProperties: false` catches **unexpected fields** that could indicate data leaks

---

### Q9.3: How do you handle API authentication in your framework?

**Answer:**

```java
// Basic Auth
public Response getWithAuth(String endpoint, String username, String password) {
    return RestAssured.given()
        .spec(requestSpec)
        .auth().preemptive().basic(username, password)
        .when().get(endpoint);
}
```

**`preemptive()` is key** — Without it, RestAssured sends the request WITHOUT auth first, waits for a 401, THEN resends with credentials. `preemptive()` sends credentials on the first request — faster and avoids issues with APIs that don't return proper 401 challenges.

**For OAuth2/JWT (extension):**
```java
public Response getWithToken(String endpoint, String token) {
    return RestAssured.given()
        .spec(requestSpec)
        .header("Authorization", "Bearer " + token)
        .when().get(endpoint);
}
```

---

## 10. Reporting & Logging

### Q10.1: How does your reporting work with parallel execution? Walk through the thread safety.

**Answer:**

```java
public class ReportManager {
    private static ExtentReports extent;                           // SHARED — needs sync
    private static final ThreadLocal<ExtentTest> testThreadLocal;  // PER-THREAD — no sync needed

    public static synchronized void initReports() {       // synchronized — one report instance
        if (extent == null) {
            extent = new ExtentReports();
            extent.attachReporter(new ExtentSparkReporter("target/extent-report.html"));
        }
    }

    public static synchronized ExtentTest createTest(String testName) {  // synchronized
        ExtentTest test = extent.createTest(testName);   // writes to shared ExtentReports
        testThreadLocal.set(test);                        // stores in thread's own slot
        return test;
    }

    public static ExtentTest getTest() {
        return testThreadLocal.get();   // NOT synchronized — reads from own thread only
    }

    public static void removeTest() {
        testThreadLocal.remove();       // NOT synchronized — cleans own thread only
    }
}
```

**Flow with 3 parallel threads:**
```
Thread-1: createTest("login") → extent.createTest() [LOCKED] → testThreadLocal.set(t1)
Thread-2: [WAITS for lock]
Thread-3: [WAITS for lock]

Thread-2: createTest("dropdown") → extent.createTest() [LOCKED] → testThreadLocal.set(t2)

Later:
Thread-1: getTest() → returns t1 (its own ExtentTest)
Thread-2: getTest() → returns t2 (its own ExtentTest)
// No interference — ThreadLocal isolation
```

**Why `getTest()` doesn't need `synchronized`:** It only reads from ThreadLocal — there's no shared state access.

---

### Q10.2: How do you capture screenshots on failure? Explain the flow.

**Answer:**

**Flow:**

1. Test fails → TestNG triggers `TestListener.onTestFailure()`
2. Listener captures screenshot as Base64:
   ```java
   String base64 = ((TakesScreenshot) DriverManager.getDriver())
       .getScreenshotAs(OutputType.BASE64);
   ```
3. Embeds in ExtentReport HTML:
   ```java
   ReportManager.getTest().fail(result.getThrowable(),
       MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
   ```

**Why Base64 instead of file path:**
- **Portable** — The HTML report is self-contained; no external image dependencies
- **CI-friendly** — Report can be emailed/archived as a single file
- **No cleanup needed** — No screenshot directories to manage

**Thread safety:** `DriverManager.getDriver()` returns the CURRENT thread's driver via ThreadLocal, so the screenshot is always from the correct browser instance.

---

### Q10.3: Why is Log4j2 preferred over System.out.println for test frameworks?

**Answer:**

| Feature | `System.out.println` | Log4j2 |
|---|---|---|
| **Log levels** | None | TRACE, DEBUG, INFO, WARN, ERROR, FATAL |
| **Output destinations** | Console only | Console, file, email, Slack, ELK stack |
| **Thread info** | Manual | `[%t]` pattern — auto-prints thread name |
| **Timestamps** | Manual | `%d{yyyy-MM-dd HH:mm:ss}` — auto-formatted |
| **Filtering** | Can't filter | Set level per package (e.g., `ra.hul=DEBUG`, `org.selenium=WARN`) |
| **Performance** | Always executes | Lazy evaluation, async appenders |
| **Parallel debugging** | Impossible to trace which thread | Each log line tagged with thread name |

**My framework's log4j2.xml:**
```xml
<PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss} [%t] %-5level %logger{36} - %msg%n"/>
```
Output: `2026-03-24 15:30:45 [TestNG-method-1] INFO  LoginPage - Entering username: tomsmith`

---

## 11. CI/CD & Test Orchestration

### Q11.1: How would you set up a CI/CD pipeline for this framework?

**Answer:**

**GitHub Actions / Jenkins pipeline stages:**

```
1. Code Push → Trigger Pipeline
2. Build Stage:
   - mvn clean compile
   - Validate framework compiles without errors

3. Smoke Test Stage:
   - mvn test -Dgroups=smoke -Dbrowser=chrome -Dheadless=true
   - Quick validation (5-10 critical tests)
   - Gate: If smoke fails → block pipeline, notify team

4. Regression Stage (parallel):
   - mvn test -Denv=staging -Dbrowser=chrome -Dheadless=true
   - Full suite with parallel execution
   - Optional: Matrix strategy (chrome + firefox)

5. Report Stage:
   - Archive target/extent-report.html as artifact
   - Parse results, post summary to Slack/Teams

6. Deploy Stage (if tests pass):
   - Deploy to production
   - Run post-deploy smoke tests
```

**Key CI/CD configurations:**
```bash
# Run with headless (CI default from config.properties)
mvn clean test

# Run specific suite (smoke vs regression)
mvn clean test -Dgroups=smoke

# Override browser for matrix testing
mvn clean test -Dbrowser=firefox
```

**My framework's CI-ready features:**
- `headless=true` by default in `config.properties`
- `--no-sandbox` and `--disable-dev-shm-usage` for Docker compatibility
- `retry.count=2` for transient CI failures
- System property overrides for all configs
- ExtentReport at `target/extent-report.html` for archiving

---

### Q11.2: How do you manage test execution across environments (dev, staging, production)?

**Answer:**

**Config layering:**
```
config.properties            → Base (all environments)
config-dev.properties        → Dev overrides (headless=false, localhost URLs)
config-staging.properties    → Staging overrides (staging URLs, real data)
config-prod.properties       → Prod overrides (prod URLs, read-only tests)
```

**CI commands:**
```bash
mvn test -Denv=dev          # Dev environment
mvn test -Denv=staging      # Staging environment
mvn test -Denv=prod -Dgroups=smoke  # Prod — only smoke tests
```

**Test safety by environment:**
- **Dev:** Full CRUD operations, test data cleanup via API
- **Staging:** Full regression, shared test data with other teams
- **Production:** Read-only smoke tests (verify pages load, API health checks — no data mutations)

---

### Q11.3: How would you implement test selection — running only smoke, regression, or specific modules?

**Answer:**

**TestNG Groups:**
```java
@Test(groups = {"smoke", "login"})
public void login_validCredentials_shouldShowSecurePage() { /* ... */ }

@Test(groups = {"regression", "login"})
public void login_invalidData_shouldShowAppropriateError() { /* ... */ }
```

**Execution:**
```bash
# Run only smoke tests
mvn test -Dgroups=smoke

# Run smoke + login tests
mvn test -Dgroups="smoke,login"

# Exclude slow tests
mvn test -DexcludedGroups=slow
```

**Alternative — Multiple testng.xml files:**
```
testng-smoke.xml       → Critical path tests only
testng-regression.xml  → Full suite
testng-api.xml         → API tests only
```
```bash
mvn test -DsuiteXmlFile=src/test/resources/testng-smoke.xml
```

---

## 12. Parallel Execution & Selenium Grid

### Q12.1: Explain Selenium Grid 4 architecture. How is it different from Grid 3?

**Answer:**

**Grid 3 (Hub-Node):**
```
Tests → Hub (single point) → Node-1 (Chrome)
                            → Node-2 (Firefox)
```
- Hub was a bottleneck and single point of failure

**Grid 4 (distributed components):**
```
Tests → Router → Distributor → Session Map
                              → Node-1 (Chrome)
                              → Node-2 (Firefox)
                Event Bus ←→ (all components communicate via events)
```

| Component | Purpose |
|---|---|
| **Router** | Entry point — receives test requests |
| **Distributor** | Matches requests to available nodes based on capabilities |
| **Session Map** | Tracks which session runs on which node |
| **Node** | Executes tests (can run multiple browsers) |
| **Event Bus** | Async communication between components |

**Key differences:**
- Grid 4 supports **fully distributed deployment** (each component can run separately)
- Grid 4 has a **modern UI** at `http://hub:4444/ui`
- Grid 4 supports **Docker node auto-scaling** — spin up containers on demand
- Grid 4 uses **GraphQL** for querying session info
- Grid 4 is built on **Selenium 4** with W3C protocol (no more JSON Wire Protocol)

---

### Q12.2: How would you implement cross-browser parallel testing?

**Answer:**

**Option 1: TestNG XML with parallel tests:**
```xml
<suite parallel="tests" thread-count="3">
    <test name="Chrome Tests">
        <parameter name="browser" value="chrome"/>
        <classes>
            <class name="ra.hul.tests.ui.LoginTest"/>
        </classes>
    </test>
    <test name="Firefox Tests">
        <parameter name="browser" value="firefox"/>
        <classes>
            <class name="ra.hul.tests.ui.LoginTest"/>
        </classes>
    </test>
</suite>
```

**Option 2: TestNG @Factory:**
```java
@Factory
public Object[] createInstances() {
    return new Object[] {
        new LoginTest("chrome"),
        new LoginTest("firefox"),
        new LoginTest("edge")
    };
}
```

**Option 3: CI Matrix Strategy (GitHub Actions):**
```yaml
strategy:
  matrix:
    browser: [chrome, firefox, edge]
steps:
  - run: mvn test -Dbrowser=${{ matrix.browser }}
```

**My recommendation for JioStar scale:** CI Matrix Strategy — each browser gets its own isolated CI job with its own reporting. Failures are isolated and easy to debug.

---

## 13. Docker & Containerization

### Q13.1: How would you Dockerize your test framework?

**Answer:**

**Dockerfile for the framework:**
```dockerfile
FROM maven:3.9-eclipse-temurin-17
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:resolve        # Cache dependencies in layer
COPY src ./src
CMD ["mvn", "clean", "test", "-Dheadless=true"]
```

**Docker Compose for full stack:**
```yaml
version: "3"
services:
  selenium-hub:
    image: selenium/hub:4.18
    ports:
      - "4444:4444"

  chrome-node:
    image: selenium/node-chrome:4.18
    depends_on:
      - selenium-hub
    environment:
      - SE_EVENT_BUS_HOST=selenium-hub
      - SE_NODE_MAX_SESSIONS=3

  tests:
    build: .
    depends_on:
      - chrome-node
    environment:
      - grid.url=http://selenium-hub:4444/wd/hub
    command: mvn clean test -Dgrid.url=http://selenium-hub:4444/wd/hub
```

**Benefits:**
- **Reproducible** — Same environment in local, CI, and any machine
- **Scalable** — Scale Chrome nodes: `docker-compose up --scale chrome-node=5`
- **Isolated** — No "works on my machine" issues
- **Disposable** — Tear down everything after tests: `docker-compose down`

---

### Q13.2: Why `--no-sandbox` and `--disable-dev-shm-usage` in Docker?

**Answer:**

See [Q4.2](#q42-what-are---no-sandbox-and---disable-dev-shm-usage-why-are-they-in-your-framework) — these are Docker-specific Chrome flags to prevent crashes in containerized environments.

---

## 14. Flaky Tests & Retry Mechanisms

### Q14.1: How does your retry mechanism work? Is retry a good practice?

**Answer:**

**How it works:**

```java
public class RetryAnalyzer implements IRetryAnalyzer {
    private int currentRetry = 0;
    private static final int MAX_RETRY = ConfigManager.getIntOrDefault("retry.count", 1);

    @Override
    public boolean retry(ITestResult result) {
        if (currentRetry < MAX_RETRY) {
            currentRetry++;
            return true;  // Tell TestNG to re-run this test
        }
        return false;     // Max retries reached — mark as failed
    }
}
```

**Auto-applied via RetryTransformer** — no per-test annotation needed.

**Is retry a good practice?**

**Yes, but with caveats:**
- **Good:** Handles genuinely transient failures — network blips, CI machine load spikes, timing issues
- **Bad:** Retries can **mask real bugs** — if a test passes 1/3 times, the underlying issue is hidden
- **Best practice:** Track flake rate. If a test needs retries > 10% of the time, fix the root cause, don't rely on retry

**My approach:**
- `retry.count=2` for CI (handles transient issues)
- `retry.count=0` for local debugging (see failures immediately)
- Monitor retry frequency — tests that frequently retry get investigated

---

### Q14.2: What are common causes of flaky tests? How do you prevent them?

**Answer:**

| Cause | Prevention in My Framework |
|---|---|
| **Timing issues** | Explicit waits via `WaitUtils` — never `Thread.sleep()` |
| **Shared test state** | `@BeforeMethod` creates fresh driver; no state leaks |
| **Hardcoded data** | Dynamic data via DataProvider + Builder pattern |
| **Element not interactable** | `waitForClickable()` before every click |
| **Stale element reference** | Fresh lookup every interaction (no PageFactory) |
| **Environment instability** | Config-driven timeouts, retry mechanism for transient failures |
| **Test order dependency** | Tests are fully independent — `@BeforeMethod` + `@AfterMethod` ensure isolation |
| **Race conditions** | ThreadLocal for driver + synchronized for shared resources |

**Debugging a flaky test:**
1. Check **logs** (`logs/test-execution.log`) for timing patterns
2. Check **screenshot** in ExtentReport — what state was the page in?
3. Run the test **10 times sequentially** — if it fails > once, it's a real flake
4. Check if it only fails in **parallel** — indicates shared state or resource contention
5. Check if it only fails in **CI** — indicates environment/timing differences

---

## 15. Streaming Platform (JioStar/Hotstar) Specific Testing

### Q15.1: How would you test video playback on a streaming platform like JioStar?

**Answer:**

**Key areas to test:**

1. **Playback initiation:**
   - Video starts within SLA (e.g., < 3 seconds)
   - Correct content plays (match title, thumbnail, metadata)
   - Playback controls visible and functional (play, pause, seek, volume)

2. **Adaptive Bitrate Streaming (ABR):**
   - Video quality adjusts dynamically with bandwidth changes
   - Smooth transitions: 480p → 720p → 1080p without artifacts
   - No buffering on stable connections
   - Graceful degradation on slow networks

3. **Seek & Scrub:**
   - Seeking forward/backward loads the correct frame
   - Seek bar position matches actual playback time
   - Rapid seeking doesn't crash the player

4. **Network resilience:**
   - Playback resumes after brief network loss
   - Appropriate error message on prolonged disconnection
   - Buffering indicator shows during rebuffering

5. **Automation approach:**
   - Use **JavaScript execution** to interact with video player APIs:
     ```java
     JavascriptExecutor js = (JavascriptExecutor) driver;
     Long currentTime = (Long) js.executeScript("return document.querySelector('video').currentTime");
     Boolean isPaused = (Boolean) js.executeScript("return document.querySelector('video').paused");
     ```
   - Validate video is actually playing: `currentTime` is increasing
   - Use **Chrome DevTools Protocol** to throttle network and test ABR

---

### Q15.2: How would you test DRM (Digital Rights Management) on a streaming platform?

**Answer:**

**DRM systems by platform:**
- **Widevine** — Android, Chrome, Firefox (L1 = hardware-backed, L3 = software)
- **FairPlay** — iOS, Safari, Apple TV
- **PlayReady** — Windows, Xbox, Smart TVs

**Test scenarios:**

| Scenario | What to Verify |
|---|---|
| Valid license | Content plays in expected quality |
| Expired license | Playback denied with clear error message |
| Concurrent streams limit | 5th stream rejected when limit is 4 |
| HDCP enforcement | L1 content won't play without HDCP-compatible display |
| Offline download | Content plays offline within rental period |
| License renewal | Background renewal doesn't interrupt playback |
| Tampered content | Modified manifest/segments are rejected |

**Testing approach:**
- Use **instrumented test builds** (ExoPlayer on Android) to inspect DRM events
- Mock license server responses for edge cases (expired, malformed, delayed)
- Cross-platform matrix: each DRM system on its target devices

---

### Q15.3: How would you test live streaming (like IPL on JioStar)?

**Answer:**

**Key challenges:**
- **Latency SLA:** < 5 seconds for standard live, < 3 seconds for interactive (polls, quizzes)
- **Scale:** Millions of concurrent viewers during IPL
- **Reliability:** Zero tolerance for drops during key moments (wickets, goals)

**Test types:**

1. **Functional:**
   - Live indicator visible
   - Seek-to-live button works
   - DVR (rewind in live stream) functions within buffer window
   - Chat/reactions overlay doesn't interfere with video

2. **Performance/Load:**
   - Use **Gatling/JMeter** to simulate millions of concurrent HLS/DASH manifest requests
   - Measure: time to first frame, rebuffering ratio, stream start failures
   - CDN cache hit ratio under load

3. **Failover:**
   - Primary CDN failure → seamless failover to backup
   - Encoder failure → redundant encoder takes over
   - Origin server failure → edge servers continue serving from cache

4. **Monitoring (production):**
   - Real-time dashboards: buffering ratio, error rate, concurrent viewers
   - Synthetic monitoring: bots playing streams from multiple geolocations
   - Alerting on: startup failure rate > threshold, P95 latency spike

---

### Q15.4: How would you test across multiple platforms (Mobile, Web, Smart TV, Streaming Stick)?

**Answer:**

**Cross-platform testing strategy:**

| Platform | Tool | Approach |
|---|---|---|
| Web (Chrome, Firefox, Safari) | Selenium + Selenium Grid | Parallel execution on Grid/Cloud |
| Android | Appium + UiAutomator2 | Real device farm (BrowserStack) |
| iOS | Appium + XCUITest | Real device farm (BrowserStack) |
| Smart TV (Android TV, Fire TV) | Appium + adb | Limited automation — focus on manual + visual testing |
| Smart TV (Samsung Tizen, LG webOS) | Remote debug + custom tools | JS-based testing via DevTools |

**Shared vs. platform-specific tests:**
- **Shared:** Business logic tests (login, browse, search, playback start) — write once, run via platform-specific drivers
- **Platform-specific:** Gestures (swipe on mobile), remote control navigation (TV), picture-in-picture, offline mode

**Testing matrix (Netflix approach):**
```
              Phone    Tablet    Web    TV    Stick
Android        ✅       ✅       -      ✅     ✅
iOS            ✅       ✅       -      -      -
Windows        -        -       ✅      -      -
macOS          -        -       ✅      -      -
Tizen          -        -       -      ✅     -
```

---

### Q15.5: How would you test A/B features and feature flags?

**Answer:**

**Approach:**

1. **Override feature flags in test config:**
   ```java
   // Set feature flag via cookie/header/API before test
   driver.manage().addCookie(new Cookie("feature_new_player", "enabled"));
   ```

2. **Test both variants:**
   ```java
   @Test(dataProvider = "featureVariants")
   public void newPlayerExperience(boolean featureEnabled) {
       setFeatureFlag("new_player", featureEnabled);
       // Verify correct UI/behavior for each variant
   }
   ```

3. **Contract testing:**
   - A/B service API returns consistent assignment for same user
   - Assignment doesn't change mid-session
   - Metrics tracking fires for correct variant

4. **Visual testing:**
   - Screenshot comparison between variants
   - Ensure no layout breaks in either variant

---

### Q15.6: How would you handle testing for CDN and geo-restricted content?

**Answer:**

1. **Geo-restriction testing:**
   - Use **proxy/VPN services** to simulate different geolocations
   - In BrowserStack/SauceLabs: set `geoLocation` capability
   - Verify: correct content library per region, proper error for restricted content

2. **CDN performance testing:**
   - Test from multiple geographic regions (India, US, EU, SEA)
   - Measure: Time to First Byte (TTFB), download throughput, cache hit ratio
   - Use tools like **WebPageTest** or **custom scripts** with Selenium Performance Logging

3. **CDN failover:**
   - Simulate primary CDN failure → verify failover to backup CDN
   - Verify no user-visible disruption during failover
   - Measure failover time

---

## 16. Scenario-Based / System Design Questions

### Q16.1: Design a test automation framework for a streaming platform from scratch. What would you include?

**Answer:**

```
test-platform/
├── core/
│   ├── driver/         → Multi-platform driver factory (Web, Mobile, TV)
│   ├── config/         → Environment-aware config (dev/staging/prod)
│   ├── reporting/      → Allure/Extent with video recording
│   └── utils/          → Waits, retries, data generators
│
├── api-tests/
│   ├── auth/           → Login, OAuth, token refresh
│   ├── catalog/        → Content search, recommendations, metadata
│   ├── playback/       → Stream URL resolution, DRM license
│   ├── payments/       → Subscription, billing, coupons
│   └── contracts/      → Pact contract tests for microservices
│
├── ui-tests/
│   ├── web/            → Selenium-based page objects
│   ├── mobile/         → Appium-based page objects
│   ├── tv/             → TV-specific page objects
│   └── shared/         → Common business flows (login, browse, play)
│
├── performance/
│   ├── load/           → Gatling scripts for API load testing
│   ├── streaming/      → Video playback performance (startup time, rebuffer)
│   └── cdn/            → CDN performance from multiple geos
│
├── visual/
│   ├── baselines/      → Reference screenshots per platform
│   └── comparisons/    → Percy/Ashot visual diff tests
│
├── data/
│   ├── test-users/     → Pre-provisioned test accounts per environment
│   ├── content/        → Test content metadata
│   └── schemas/        → JSON schemas for API contracts
│
└── infra/
    ├── docker/         → Docker Compose for Grid + dependencies
    ├── ci/             → Jenkins/GitHub Actions pipeline configs
    └── monitoring/     → Grafana dashboards for test metrics
```

**Key architectural decisions:**
1. **Separate API and UI tests** — API tests are faster, run more frequently in pipeline
2. **Contract tests** (Pact) between microservices — catch integration issues before E2E tests
3. **Visual regression** as a first-class citizen — streaming UIs change frequently
4. **Performance built in** — not an afterthought; stream quality metrics are product KPIs
5. **Multi-platform driver abstraction** — same business tests, different platform drivers

---

### Q16.2: Design a real-time video view counter for a streaming application like Hotstar (system design question).

**Answer:**

**Requirements:**
- Track concurrent viewers per content in real-time
- Handle millions of concurrent connections (IPL-scale)
- Display count with < 5 second delay

**Architecture:**

```
Clients (viewers) → Load Balancer → WebSocket Servers (stateless)
                                         ↓
                                    Kafka Topic ("viewer-events")
                                         ↓
                                    Stream Processor (Flink/Kafka Streams)
                                    ├── Aggregates counts per content_id
                                    └── Writes to Redis (INCR/DECR)
                                         ↓
                                    Read API → Redis GET → Return count
```

**Key decisions:**
- **WebSocket** for persistent connection (detects viewer leaving)
- **Kafka** for durable, ordered event stream (handles spikes)
- **Redis** for fast read/write counters (single-digit ms latency)
- **Approximate counting** is acceptable — HyperLogLog for unique viewers if exact count isn't needed
- **Heartbeat mechanism** — clients send heartbeat every 30 seconds; missed heartbeats trigger decrement

**How to test it:**
- Load test: simulate 10M concurrent WebSocket connections using Gatling
- Accuracy test: known viewer count vs. displayed count (within 5% tolerance)
- Failover test: kill a WebSocket server, verify no count drops
- Latency test: viewer joins → count updates in < 5 seconds

---

### Q16.3: You have 10,000 test cases and the regression suite takes 8 hours. How do you reduce it to 1 hour?

**Answer:**

**Step 1: Analyze (before optimizing)**
- Identify the slowest 20% of tests (Pareto principle — they likely consume 80% of time)
- Identify redundant tests (multiple tests covering the same code path)
- Categorize: smoke (critical path), regression (full), edge-case

**Step 2: Parallelize**
- Move from sequential to `parallel="methods"` with `thread-count` based on machine cores
- Use Selenium Grid with 20+ browser nodes (or cloud: BrowserStack Automate)
- `8 hours / 20 parallel threads ≈ 24 minutes`

**Step 3: Shift-left (push to earlier/faster layers)**
- Convert UI tests to API tests where possible (API tests are 10-100x faster)
- Use contract tests instead of E2E for microservice integration
- Test pyramid: 70% unit, 20% API/integration, 10% UI

**Step 4: Optimize existing tests**
- Replace `Thread.sleep()` with explicit waits
- Reduce unnecessary navigation (login via API cookie instead of UI)
- Share expensive setup via `@BeforeClass` where safe

**Step 5: Smart selection**
- Run only tests affected by code changes (test impact analysis)
- Run full regression nightly; PRs only run smoke + impacted tests
- Tag tests by risk: high-risk tests always run, low-risk tests run nightly

---

### Q16.4: How would you test a microservices-based backend?

**Answer:**

**Test levels for microservices:**

```
Level 1: Unit Tests (per service)
  → Test individual service logic
  → Mock external dependencies
  → Fast: seconds

Level 2: Contract Tests (between services)
  → Pact: consumer defines expected API; provider verifies it matches
  → Catches breaking changes at interface level
  → Fast: seconds

Level 3: Integration Tests (per service)
  → Service + its real database + mocked downstream services (WireMock)
  → Verifies SQL queries, caching, error handling
  → Medium: minutes

Level 4: E2E Tests (across services)
  → Full stack in staging environment
  → Minimal — only critical user journeys
  → Slow: minutes to hours
```

**Service virtualization with WireMock:**
```java
// Stub a downstream recommendation service
wireMock.stubFor(get("/recommendations/user123")
    .willReturn(okJson("{\"titles\": [\"Movie A\", \"Movie B\"]}")));

// Test your service's behavior when recommendations return empty
wireMock.stubFor(get("/recommendations/user123")
    .willReturn(okJson("{\"titles\": []}")));
```

**Key principle:** Each service team owns tests for levels 1-3. A shared QE team owns level 4 E2E tests and monitors.

---

## 17. General SDET Concepts

### Q17.1: What is the Test Pyramid? How do you apply it?

**Answer:**

```
        /  E2E  \         → Few (10%)  — slow, expensive, high confidence
       /  API    \        → Medium (20%) — fast, validate integrations
      /  Unit     \       → Many (70%) — fastest, test logic in isolation
```

**How I apply it:**
- **Unit tests:** Developers write for business logic (not my primary focus as SDET, but I review coverage)
- **API tests:** I write extensively — validate service contracts, response schemas, error handling. Fast and reliable
- **UI E2E tests:** I write selectively — only critical user journeys (login, browse, play, subscribe). These are the most expensive to maintain

**Anti-pattern (ice cream cone):**
```
      /  Unit  \          → Few
     /  API     \
    /  E2E       \        → Many
   / Manual       \       → Most
```
This is expensive — most testing happens at the slowest, most fragile layers.

---

### Q17.2: What is the difference between `driver.findElement()` and `driver.findElements()`?

**Answer:**

| Aspect | `findElement()` | `findElements()` |
|---|---|---|
| Returns | Single `WebElement` | `List<WebElement>` |
| Not found | Throws `NoSuchElementException` | Returns empty list `[]` |
| Multiple matches | Returns first match | Returns all matches |
| Use case | Interact with a specific element | Count elements, iterate over lists, verify absence |

**Practical use — verifying element absence:**
```java
// WRONG — throws exception if not found
boolean exists = driver.findElement(By.id("error")).isDisplayed(); // 💥

// RIGHT — returns empty list, no exception
boolean exists = driver.findElements(By.id("error")).size() > 0;
```

---

### Q17.3: What are Selenium 4's key new features?

**Answer:**

| Feature | Description |
|---|---|
| **W3C Protocol** | Native W3C WebDriver protocol (no JSON Wire Protocol) |
| **Relative Locators** | `RelativeLocator.with(By.tagName("input")).above(submitBtn)` |
| **Chrome DevTools Protocol** | Network interception, geolocation mocking, performance metrics |
| **New Window API** | `driver.switchTo().newWindow(WindowType.TAB)` — open new tab/window |
| **Element Screenshots** | `element.getScreenshotAs(OutputType.FILE)` — screenshot single element |
| **Selenium Grid 4** | Fully distributed, Docker support, GraphQL, modern UI |
| **BiDi Protocol** | Bi-directional communication for real-time events (experimental) |

**CDP example (network throttling for testing ABR):**
```java
DevTools devTools = ((ChromeDriver) driver).getDevTools();
devTools.createSession();
devTools.send(Network.emulateNetworkConditions(
    false, 100, 50000, 50000, Optional.of(ConnectionType.CELLULAR3G)));
```

---

### Q17.4: How do you handle dynamic elements and AJAX-heavy applications?

**Answer:**

1. **Explicit waits with specific conditions:**
   ```java
   // Wait for element to become visible after AJAX load
   WaitUtils.waitForVisible(By.id("search-results"));

   // Wait for element to disappear (loading spinner)
   WaitUtils.waitForInvisible(By.cssSelector(".spinner"));
   ```

2. **FluentWait for polling:**
   ```java
   WaitUtils.fluentWait(By.id("dynamic-content"));
   // Polls every 500ms, ignores NoSuchElement + StaleElement
   ```

3. **Wait for JavaScript/jQuery to complete:**
   ```java
   new WebDriverWait(driver, Duration.ofSeconds(30))
       .until(d -> ((JavascriptExecutor) d)
           .executeScript("return document.readyState").equals("complete"));

   new WebDriverWait(driver, Duration.ofSeconds(30))
       .until(d -> (Boolean) ((JavascriptExecutor) d)
           .executeScript("return jQuery.active == 0"));
   ```

4. **Custom ExpectedConditions:**
   ```java
   wait.until(driver -> driver.findElements(By.cssSelector(".result-item")).size() >= 10);
   ```

---

### Q17.5: What locator strategies do you prefer and why?

**Answer:**

**Priority order:**

| Priority | Locator | Why |
|---|---|---|
| 1 | `By.id()` | Fastest, unique, stable |
| 2 | `By.cssSelector()` | Fast, flexible, readable |
| 3 | `By.name()` | Good for form elements |
| 4 | `data-testid` attribute | Purpose-built for testing, decoupled from styling |
| 5 | `By.xpath()` | Most powerful but slowest; use for text-based or complex traversals |

**What I avoid:**
- `By.className()` — Classes change with styling updates
- `By.linkText()` — Text changes with i18n or copy updates
- Long XPath chains — `//div[3]/span[2]/a` is fragile

**Best practice for SPAs like JioStar:**
Request `data-testid` attributes from developers:
```html
<button data-testid="play-button">Play</button>
```
```java
By.cssSelector("[data-testid='play-button']")
```
Immune to styling changes, clear intent, fast lookups.

---

### Q17.6: What is the difference between POM and Screenplay pattern?

**Answer:**

| Aspect | Page Object Model | Screenplay Pattern |
|---|---|---|
| Abstraction | Page-centric (LoginPage, HomePage) | Actor-centric (user performs tasks) |
| Structure | One class per page with methods | Tasks, Questions, Abilities |
| Readability | `loginPage.enterUsername("x")` | `actor.attemptsTo(Login.withCredentials("x", "y"))` |
| Scalability | Can become bloated with large pages | Tasks are composable and reusable |
| Complexity | Simple to understand | Steeper learning curve |
| Best for | Small-medium projects | Large projects with complex workflows |

**When to choose Screenplay over POM:**
- Pages have 50+ methods (POM class becomes unmaintainable)
- Same business flows are tested across web + mobile + API (tasks are platform-agnostic)
- Team uses BDD and wants Gherkin steps to map cleanly to reusable tasks

**For my framework:** POM is the right choice — the scale doesn't justify Screenplay's complexity overhead.

---

### Q17.7: How do you handle file uploads and downloads in Selenium?

**Answer:**

**File Upload:**
```java
// Standard <input type="file">
WebElement uploadInput = driver.findElement(By.id("file-upload"));
uploadInput.sendKeys("/path/to/file.pdf");  // No click needed

// Non-standard (JS-based) upload
// Use Robot class or AutoIt for OS-level file dialog
```

**File Download (headless-friendly):**
```java
// Set Chrome download directory
ChromeOptions options = new ChromeOptions();
Map<String, Object> prefs = new HashMap<>();
prefs.put("download.default_directory", "/tmp/downloads");
prefs.put("download.prompt_for_download", false);
options.setExperimentalOption("prefs", prefs);

// Verify download
File downloaded = new File("/tmp/downloads/report.pdf");
new WebDriverWait(driver, Duration.ofSeconds(30))
    .until(d -> downloaded.exists() && downloaded.length() > 0);
```

---

### Q17.8: Explain the `@Listeners` annotation vs. listener registration in `testng.xml`. Which do you prefer?

**Answer:**

**Option 1: `@Listeners` annotation on test class:**
```java
@Listeners({TestListener.class, RetryTransformer.class})
public class LoginTest extends BaseTest { }
```

**Option 2: Registration in `testng.xml`:**
```xml
<listeners>
    <listener class-name="ra.hul.framework.listeners.TestListener"/>
    <listener class-name="ra.hul.framework.listeners.RetryTransformer"/>
</listeners>
```

| Aspect | `@Listeners` | `testng.xml` |
|---|---|---|
| Scope | Per class — must add to every test class | Global — applies to entire suite |
| Maintainability | Scattered across files | Centralized in one place |
| Flexibility | Hardcoded | Easily toggle on/off without code changes |
| Forget risk | New test class might miss the annotation | Automatically applies to all tests |

**I use `testng.xml`** — centralized, impossible to forget, and can be changed without recompiling.

---

## Quick Reference: Key Code Locations

| Component | File |
|---|---|
| Driver Management | `src/main/java/ra/hul/framework/driver/DriverManager.java` |
| Config Management | `src/main/java/ra/hul/framework/config/ConfigManager.java` |
| Base Page | `src/main/java/ra/hul/framework/pages/BasePage.java` |
| Wait Utilities | `src/main/java/ra/hul/framework/utils/WaitUtils.java` |
| Test Listener | `src/main/java/ra/hul/framework/listeners/TestListener.java` |
| Report Manager | `src/main/java/ra/hul/framework/reporting/ReportManager.java` |
| Retry Analyzer | `src/main/java/ra/hul/framework/listeners/RetryAnalyzer.java` |
| Retry Transformer | `src/main/java/ra/hul/framework/listeners/RetryTransformer.java` |
| API Client | `src/main/java/ra/hul/framework/api/ApiClient.java` |
| Base Test | `src/test/java/ra/hul/tests/base/BaseTest.java` |
| TestNG Config | `src/test/resources/testng.xml` |
| Properties | `src/test/resources/config.properties` |

---

## Tips for the Interview

1. **Lead with WHY, not WHAT** — Don't just say "I used ThreadLocal." Say "I needed thread-safe parallel execution, so I used ThreadLocal because..."
2. **Reference your code** — "In my `DriverManager.java`, line 15, you can see the ThreadLocal declaration..."
3. **Acknowledge trade-offs** — "I chose `@BeforeMethod` over `@BeforeClass` — it's slower but guarantees test isolation"
4. **Think streaming-first** — JioStar cares about video quality, latency, cross-platform, and scale. Tie your answers to these concerns
5. **Be honest about limitations** — "My current framework doesn't support Grid, but here's exactly how I'd add it" shows growth mindset
6. **Quantify when possible** — "Parallel execution reduced our suite from 45 minutes to 12 minutes" (even if hypothetical, show you think in metrics)

---

**Good luck tomorrow, Rahul! You've built a solid framework — own it with confidence.**
