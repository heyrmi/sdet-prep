# CLAUDE.md

This file provides guidance to AI assistants working in the **`framework/` module**. For the
whole mega-repo (all seven pillars + toolchains), see the root [`../CLAUDE.md`](../CLAUDE.md).

## Project

Java 21 / Maven test automation framework covering Web (Selenium 4), API (Rest Assured), Mobile (Appium 2.x), and Performance (Gatling). TestNG is the runner; Allure is the reporter.

Package root: `ra.hul.framework` (production code in `src/main/java`) and `ra.hul.tests` (test classes in `src/test/java`). This split is enforced — framework infrastructure must never live under `src/test`.

## Common commands

```bash
# Build & verify
mvn clean compile
mvn test-compile

# Run by module (each profile points Surefire at a different TestNG suite XML)
mvn test -Pweb
mvn test -Papi
mvn test -Pmobile
mvn test -Psmoke           # cross-module, group="smoke"
mvn test                    # all-tests.xml

# Maturity capabilities (each profile swaps the suite XML)
mvn test -Pvisual  -Dheadless=true -Dbrowser=chrome   # visual regression (needs a browser)
mvn test -Pa11y    -Dheadless=true -Dbrowser=chrome   # accessibility scan (needs a browser)
mvn test -Pcontract                                    # Pact contract + datafaker tests (no browser)

# Single test class / method (Surefire still uses the suite XML, so -Dtest is filtering)
mvn test -Pweb -Dtest=LoginTest
mvn test -Pweb -Dtest=LoginTest#login_validCredentials_shouldShowSecurePage

# Environment + browser overrides
mvn test -Pweb -Denv=stage -Dbrowser=firefox -Dheadless=false
mvn test -Pweb -Dgrid.url=http://localhost:4444   # switches to RemoteWebDriver

# Performance
mvn gatling:test
mvn gatling:test -Dgatling.simulationClass=ra.hul.framework.performance.simulations.HttpBinGetSimulation

# Reporting
mvn allure:serve    # generates + opens report
mvn allure:report   # writes to target/site/allure-maven-plugin
```

Mobile tests require an emulator + running Appium server before `mvn test -Pmobile`. Full setup steps are in `MOBILE_SETUP.md` — `ANDROID_HOME` must be exported in the same shell that runs Maven.

## Architecture — what's non-obvious

**4-level config resolution (`ConfigManager`).** Precedence, highest first: OS env var (dot.key → `DOT_KEY`) → `-D` system property → `config-<env>.properties` (loaded when `-Denv=<env>` is set) → `config.properties`. Missing keys throw `IllegalStateException` — use `getOrDefault` if absence is legal. Any new tunable belongs in `config.properties` and read through this manager; never hardcode timeouts, URLs, or paths in tests.

**ThreadLocal driver isolation.** `DriverManager` (web) and `AppiumDriverManager` (mobile) each hold a `ThreadLocal<...Driver>`. Base test classes (`BaseWebTest`, `BaseMobileTest`) initialize per `@BeforeMethod` and quit per `@AfterMethod`. Parallel execution is safe because of this, *not* because of any locking. **Do not** introduce static driver fields or share drivers across threads.

**Parallelism is owned by TestNG suite XMLs, not Maven.** `web-tests.xml` runs methods in 10 threads; `api-tests.xml` runs methods in 20; `mobile-tests.xml` runs classes in 1. To change parallelism, edit the suite XML or override `parallel.count` via `-D`. Surefire's fork settings are not used for this.

**Sealed `BrowserStrategy`.** Java 21 sealed interface restricts implementations to `ChromeStrategy`, `FirefoxStrategy`, `EdgeStrategy` at compile time. `WebDriverFactory` selects a strategy based on the `browser` config and decides local vs. `RemoteWebDriver` based on whether `grid.url` is set. Adding a new browser means a new sealed permits entry plus a strategy class — there is no runtime registry.

**Allure `@Step` requires AspectJ weaver.** The Surefire `argLine` in `pom.xml` injects `-javaagent:.../aspectjweaver.jar`. If `@Step` annotations stop appearing in reports, that javaagent is the first thing to check. Don't remove the `argLine` block when editing other Surefire config.

**Surefire is pinned to the TestNG provider.** The Pact JVM `junit5` artifacts pull JUnit 5 onto the test classpath, which makes Surefire auto-select the JUnit Platform provider and silently ignore our TestNG suite XMLs (`Tests run: 0`). The Surefire plugin therefore declares a `surefire-testng` plugin-level dependency to force the TestNG provider. Do not remove it while the Pact deps are present.

**Auto-applied retry.** `RetryTransformer` is a TestNG `IAnnotationTransformer` registered in every suite XML — it attaches `RetryAnalyzer` (count from `retry.count`) to every `@Test` automatically. Individual tests do not declare retry. To disable for a specific test, the right move is to make `RetryAnalyzer` honor an opt-out attribute, not to special-case the transformer.

**Page/Screen Object Model is enforced, not optional.**
- Tests must not reference Selenium `By` or Appium locators directly.
- Page objects (`web/pages/`) and screen objects (`mobile/screens/`) own all locators as `private final` fields.
- Public methods on page/screen objects are annotated `@Step` for Allure traceability.
- Assertions live in test classes, never inside page/screen objects.
- `BasePage.isLoaded()` and `BaseScreen.isLoaded()` are template-method hooks — every concrete page/screen implements one.

**No `Thread.sleep`.** Use `WaitUtils` (web) or `MobileWaitUtils` (mobile). Both wrap explicit/fluent waits with config-driven timeouts from `TimeoutConstants`.

## Test naming convention

`methodUnderTest_condition_expectedBehavior` — e.g. `login_validCredentials_shouldShowSecurePage`. Enforced by convention; new tests should match.

Every test method declares `@Epic`, `@Feature`, `@Story`, `@Severity` (Allure metadata) and `groups = {"regression"}` at minimum. Critical happy paths additionally tag `"smoke"` to be picked up by `smoke-tests.xml`.

## Adding new tests

- New web test: add a page object under `ra.hul.framework.web.pages` extending `BasePage`, then a test class under `ra.hul.tests.web` extending `BaseWebTest`. Register the test class in `web-tests.xml` (and `smoke-tests.xml` if applicable).
- New API test: extend `BaseApiTest`. POJOs go under `ra.hul.framework.api.models` with Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`. JSON schemas for contract tests go in `src/test/resources/schemas/`.
- New mobile test: add a screen object under `ra.hul.framework.mobile.screens` extending `BaseScreen`. Prefer `AppiumBy.accessibilityId()`. Test class extends `BaseMobileTest` and is registered in `mobile-tests.xml`.

A test class that is not added to its suite XML will silently not run.

## Maturity capabilities

Four self-contained, fully offline capabilities (no cloud/SaaS). All tunables are read via
`ConfigManager.getOrDefault`/`getIntOrDefault`/`getLongOrDefault` so absence never crashes; defaults
and keys live in `src/test/resources/config.properties` (this module has no `src/main/resources`, so
config is loaded from the test classpath).

- **Visual regression** — `web/utils/VisualRegressionUtils` (`src/main`). Homegrown per-pixel
  `BufferedImage` diff, no external visual SaaS. Captures via `DriverManager.getDriver()` +
  `TakesScreenshot` (reuses the AllureTestListener screenshot idiom), compares against a committed
  baseline under `visual.baseline.dir` (default `src/test/resources/visual/baseline/`), writes
  actual+diff to `visual.output.dir` (default `target/visual/`), attaches baseline/actual/diff to
  Allure. Never asserts — returns `VisualComparisonResult`; the test asserts. Keys:
  `visual.baseline.dir`, `visual.output.dir`, `visual.pixel.tolerance`, `visual.diff.threshold`,
  `visual.update.baselines` (set `true` to (re)write baselines instead of failing). Tests:
  `tests/visual/VisualRegressionTest`, suite `visual-tests.xml`, profile `visual`.
- **Accessibility** — `web/utils/AccessibilityUtils` (`src/main`) wraps axe-core's `AxeBuilder`,
  filters by `a11y.tags` (default `wcag2a,wcag2aa`), attaches violations (JSON via `AxeReporter`
  + readable summary) to Allure, returns `List<Rule>` for the test to assert. Because a `src/main`
  util references axe, the `com.deque.html.axe-core:selenium` dep is **compile** scope (not test).
  Tests: `tests/a11y/AccessibilityTest`, suite `a11y-tests.xml`, profile `a11y`.
- **Contract testing** — `tests/contract/` (test-only). Real Pact JVM consumer test using the DSL
  **programmatically** (`ConsumerPactBuilder` + `runConsumerTest`), NOT the JUnit5 extension. Uses
  the V3 model (`RequestResponsePact`) end-to-end so the embedded-`HttpServer` provider verification
  can replay `RequestResponseInteraction`s. Pact files land in `pact.output.dir` (default
  `target/pacts/`). Suite `contract-tests.xml` (also runs `DataFactoryTest`), profile `contract`.
- **Test-data management** — `data/` (`src/main`): `UserFactory`, `PostPayloadFactory`,
  `CredentialFactory` + `Credentials` value object, backed by datafaker via `FakerProvider`
  (seeded from `data.faker.seed` / `data.faker.locale` → deterministic). Fluent `withX(...)`
  overrides win over generated values; `build()` generates all fields in a fixed order so faker
  consumption stays deterministic regardless of overrides. `datafaker` is **compile** scope (used
  from `src/main`). Do NOT route the web login creds through a factory — `tomsmith`/
  `SuperSecretPassword!` must match the live demo site. Tests: `tests/data/DataFactoryTest`.

## CI

`.github/workflows/test-automation.yml` runs web + api jobs in parallel on push to main/develop and on PRs, then merges Allure results and deploys the report to `gh-pages` with 20-run history. Mobile + performance are `workflow_dispatch` only.
