# Comprehensive SDET Interview Questions Guide

> Practical, hands-on SDET (Software Development Engineer in Test) interview questions asked at top tech companies. These are NOT DSA questions -- these are real-world coding and automation problems.

---

## Table of Contents

1. [File Operations](#1-file-operations)
2. [Web Scraping](#2-web-scraping)
3. [API Testing](#3-api-testing)
4. [Data Processing (CSV/JSON/XML)](#4-data-processing-csvjsonxml)
5. [Selenium Practical Tasks](#5-selenium-practical-tasks)
6. [Database Operations](#6-database-operations)
7. [Linux/Unix Commands in Java](#7-linuxunix-commands-in-java)
8. [Multithreading & Concurrency](#8-multithreading--concurrency)
9. [Design Patterns for Test Automation](#9-design-patterns-for-test-automation)
10. [Regular Expressions & Text Processing](#10-regular-expressions--text-processing)
11. [Performance & Load Testing](#11-performance--load-testing)
12. [Build From Scratch Questions](#12-build-from-scratch-questions)
13. [Framework Design & Architecture](#13-framework-design--architecture)

---

## 1. File Operations

### Ques1: File Comparator
- **Company:** Microsoft, Amazon
- **Difficulty:** Medium
- **Description:** Write a program that reads two text files and compares them line by line. Report which lines are different, which lines are missing, and which lines are extra. Handle edge cases like empty files, files of different lengths, and binary vs text files.
- **What's expected:** Use `BufferedReader` or `Files.readAllLines()`. Demonstrate efficient line-by-line comparison. Output a diff-style report. Handle `IOException` gracefully.

### Ques2: Log Parser
- **Company:** Amazon, Flipkart, Netflix
- **Difficulty:** Medium-Hard
- **Description:** Parse a server log file (e.g., Apache/Nginx access log). Extract: total requests, unique IPs, most frequent endpoints, error counts (4xx/5xx), requests per hour. Output a summary report.
- **What's expected:** Use regex to parse log lines. Use `HashMap` to aggregate counts. Use `TreeMap` or sorting for ranked output. Handle malformed lines gracefully.

### Ques3: Search in Files (Grep Clone)
- **Company:** Google, Apple
- **Difficulty:** Medium
- **Description:** Implement a simplified `grep` in Java. Given a directory path and a search pattern, recursively search all `.txt` or `.log` files and print matching lines with file name and line number.
- **What's expected:** Use `Files.walk()` or recursive directory traversal. Support basic regex matching. Handle large files using streaming (not loading entire file into memory).

### Ques4: File Word Frequency Counter
- **Company:** Microsoft, Hotstar
- **Difficulty:** Easy-Medium
- **Description:** Read a text file and count the frequency of each word. Display the top N most frequent words, ignoring case and punctuation.
- **What's expected:** Use `HashMap<String, Integer>` for counting. Use `Stream` API or `PriorityQueue` for top-N. Clean words with `replaceAll("[^a-zA-Z]", "")`.

### Ques5: Merge Multiple Sorted Files
- **Company:** Google, Amazon
- **Difficulty:** Medium-Hard
- **Description:** Given N sorted files (each containing sorted integers, one per line), merge them into a single sorted output file without loading all data into memory.
- **What's expected:** Use a min-heap (`PriorityQueue`) with file pointers. Demonstrate external sort / k-way merge understanding. Memory-efficient streaming approach.

### Ques6: Find Duplicate Files
- **Company:** Amazon, Microsoft
- **Difficulty:** Medium
- **Description:** Given a directory, find all duplicate files (by content, not just name). Group duplicates together and report them.
- **What's expected:** Hash file contents using MD5/SHA-256. Use `HashMap<String, List<Path>>` to group. Handle large files by hashing in chunks.

### Ques7: Tail a Log File (Follow Mode)
- **Company:** Netflix, Flipkart
- **Difficulty:** Medium
- **Description:** Implement `tail -f` functionality in Java. Continuously monitor a log file and print new lines as they are appended.
- **What's expected:** Use `RandomAccessFile` with `seek()`. Poll for file length changes. Optionally use `WatchService` API for file change events.

### Ques8: Configuration File Parser
- **Company:** Apple, Microsoft
- **Difficulty:** Easy-Medium
- **Description:** Parse a `.properties` or `.ini` style configuration file. Support sections, key-value pairs, comments, and variable interpolation (e.g., `${key}`).
- **What's expected:** Line-by-line parsing. Support for section headers `[section]`. Variable substitution using previously defined keys. Store in nested `Map<String, Map<String, String>>`.

---

## 2. Web Scraping

### Ques1: Scrape Page Titles from Multiple URLs
- **Company:** Flipkart, Hotstar
- **Difficulty:** Easy-Medium
- **Description:** Given a list of URLs, use Selenium to open each one and extract the page title. Store the results in a CSV file with columns: URL, Title, Status (Success/Failed), Load Time.
- **What's expected:** Use `WebDriver` to navigate. Handle timeouts and unreachable pages. Measure load times. Write results using `FileWriter`.

### Ques2: Scrape Product Information from E-Commerce Site
- **Company:** Flipkart, Amazon
- **Difficulty:** Medium
- **Description:** Scrape product names, prices, ratings, and review counts from an e-commerce listing page. Handle pagination to scrape multiple pages of results.
- **What's expected:** Use Selenium locators (`xpath`, `cssSelector`). Handle dynamic loading with explicit waits. Navigate pagination. Store data in a structured format (CSV/JSON).

### Ques3: Scrape and Compare Prices Across Websites
- **Company:** Flipkart, Hotstar
- **Difficulty:** Medium-Hard
- **Description:** Given a product name, scrape its price from two or more e-commerce websites. Compare prices and generate a report showing the best deal.
- **What's expected:** Multiple site handling with different DOM structures. Robust error handling. Price normalization (remove currency symbols, handle decimals).

### Ques4: Extract All Links and Categorize
- **Company:** Google, Microsoft
- **Difficulty:** Easy-Medium
- **Description:** Scrape all hyperlinks from a webpage. Categorize them as internal links (same domain) vs external links. Report broken links by checking HTTP status codes.
- **What's expected:** Use `findElements(By.tagName("a"))`. Parse `href` attributes. Use `HttpURLConnection` to verify links. Group results by category.

### Ques5: Scrape Table Data into Structured Format
- **Company:** Amazon, Meta
- **Difficulty:** Medium
- **Description:** Scrape an HTML table from a webpage. Convert the table data into a list of Java objects or a `List<Map<String, String>>`. Handle merged cells, missing data, and nested tables.
- **What's expected:** Navigate table rows and cells using Selenium locators. Map headers to values. Handle `colspan`/`rowspan`. Export to CSV or JSON.

### Ques6: Screenshot Comparison Tool
- **Company:** Apple, Netflix
- **Difficulty:** Hard
- **Description:** Take screenshots of two versions of a web page. Compare the screenshots pixel-by-pixel and highlight the differences.
- **What's expected:** Use `TakesScreenshot` interface. Use `BufferedImage` for pixel comparison. Calculate similarity percentage. Generate a diff image highlighting changes.

---

## 3. API Testing

### Ques1: GET Request Validation
- **Company:** Amazon, Microsoft, Meta
- **Difficulty:** Easy
- **Description:** Write a test that sends a GET request to a public API (e.g., `jsonplaceholder.typicode.com/posts`). Validate: status code is 200, response body is a JSON array, each object has required fields (id, title, body, userId), response time is under 2 seconds.
- **What's expected:** Use RestAssured's `given().when().then()` chain. Assert status code, content type, body fields. Use Hamcrest matchers for assertions.

### Ques2: POST Request with Body Validation
- **Company:** Amazon, Flipkart
- **Difficulty:** Easy-Medium
- **Description:** Send a POST request with a JSON body to create a new resource. Validate the response contains the created resource with correct field values and a generated ID.
- **What's expected:** Build request body using `HashMap` or POJO serialization. Validate response body matches request data. Assert 201 status code.

### Ques3: API Chaining (CRUD Operations)
- **Company:** Microsoft, Amazon, Flipkart
- **Difficulty:** Medium
- **Description:** Implement a complete CRUD cycle: Create a resource (POST), Read it (GET), Update it (PUT/PATCH), Delete it (DELETE). Each step uses the response from the previous step. Validate at each stage.
- **What's expected:** Extract values from responses using `jsonPath()`. Pass extracted values to subsequent requests. Validate state changes at each step. Clean up test data.

### Ques4: OAuth2 / Token-Based Authentication
- **Company:** Google, Meta, Apple
- **Difficulty:** Medium-Hard
- **Description:** Write a test that obtains an OAuth2 access token using client credentials grant, then uses the token to access a protected endpoint. Handle token expiry and refresh.
- **What's expected:** Separate token acquisition from API calls. Use `auth().oauth2(token)` or header-based auth. Handle 401 responses by refreshing tokens. Demonstrate token lifecycle management.

### Ques5: Validate JSON Schema
- **Company:** Microsoft, Amazon
- **Difficulty:** Medium
- **Description:** Given an API endpoint, validate the response against a predefined JSON schema. Ensure all required fields are present, data types are correct, and nested objects conform to the schema.
- **What's expected:** Use `io.rest-assured:json-schema-validator`. Define expected schema as a JSON file. Handle both positive (valid response) and negative (schema violation) scenarios.

### Ques6: API Rate Limiting and Retry Logic
- **Company:** Netflix, Google
- **Difficulty:** Medium-Hard
- **Description:** Test an API that has rate limiting (e.g., 100 requests/minute). Implement retry logic with exponential backoff when receiving 429 (Too Many Requests) responses.
- **What's expected:** Send requests in a loop. Detect rate limit responses. Implement exponential backoff (1s, 2s, 4s...). Respect `Retry-After` headers. Log attempt counts.

### Ques7: Parallel API Testing
- **Company:** Amazon, Flipkart
- **Difficulty:** Medium
- **Description:** Execute multiple API tests in parallel using TestNG or Java's `ExecutorService`. Verify that the API handles concurrent requests correctly and returns consistent results.
- **What's expected:** Use TestNG `@Test(threadPoolSize=5, invocationCount=20)` or `CompletableFuture`. Collect and validate results from all threads. Check for race conditions in responses.

### Ques8: API Response Time Performance Test
- **Company:** Netflix, Hotstar
- **Difficulty:** Medium
- **Description:** Measure API response times for various endpoints under different conditions. Calculate min, max, average, P95, and P99 response times. Fail the test if P95 exceeds a threshold.
- **What's expected:** Execute repeated calls. Collect timing data. Calculate percentile statistics. Use assertions on aggregated metrics.

### Ques9: Multipart File Upload API
- **Company:** Microsoft, Apple
- **Difficulty:** Medium
- **Description:** Test a file upload API that accepts multipart form data. Upload different file types (image, PDF, text), validate the upload response, then download and verify the file content matches.
- **What's expected:** Use `multiPart()` in RestAssured. Test with various file sizes and types. Validate file metadata in response. Compare uploaded and downloaded file checksums.

### Ques10: GraphQL API Testing
- **Company:** Meta, Netflix
- **Difficulty:** Medium-Hard
- **Description:** Write tests for a GraphQL API. Send queries, mutations, and subscriptions. Validate partial response selection, nested object resolution, and error handling.
- **What's expected:** Build GraphQL query strings. Send as POST with `application/json`. Validate `data` and `errors` fields. Test query variables and fragments.

---

## 4. Data Processing (CSV/JSON/XML)

### Ques1: Parse CSV and Generate Report
- **Company:** Flipkart, Amazon
- **Difficulty:** Easy-Medium
- **Description:** Read a CSV file containing sales data (product, quantity, price, date). Calculate: total revenue, revenue per product, best-selling product, monthly sales trends. Output a summary report.
- **What's expected:** Use `BufferedReader` or `OpenCSV` library. Handle header rows, quoted fields, and escaped commas. Use `HashMap` for aggregation. Format output as a table.

### Ques2: JSON to Java Object Mapping
- **Company:** Microsoft, Meta
- **Difficulty:** Easy-Medium
- **Description:** Parse a complex nested JSON response into Java POJOs using Jackson/Gson. Handle arrays, nested objects, optional fields, and date formatting.
- **What's expected:** Create POJO classes with proper annotations (`@JsonProperty`, `@SerializedName`). Handle null values and missing fields. Demonstrate serialization and deserialization.

### Ques3: XML Parsing and Validation
- **Company:** Apple, Amazon
- **Difficulty:** Medium
- **Description:** Parse an XML file (e.g., a SOAP response or configuration file). Extract specific elements using XPath. Validate the XML against an XSD schema.
- **What's expected:** Use `DocumentBuilder` and `XPath` API. Navigate namespaces. Validate against XSD using `SchemaFactory`. Handle parsing exceptions.

### Ques4: Transform Data Between Formats
- **Company:** Google, Flipkart
- **Difficulty:** Medium
- **Description:** Convert data from one format to another: CSV to JSON, JSON to XML, XML to CSV. Preserve data integrity and handle edge cases (special characters, nested structures).
- **What's expected:** Read input format, build an intermediate representation, write output format. Handle encoding issues. Validate round-trip conversion (A -> B -> A).

### Ques5: Merge and Deduplicate Data from Multiple Sources
- **Company:** Amazon, Netflix
- **Difficulty:** Medium-Hard
- **Description:** Read data from multiple sources (one CSV, one JSON, one database). Merge the data on a common key. Deduplicate based on specified rules (keep latest, keep highest value, etc.).
- **What's expected:** Use a common data model. Implement merge logic with conflict resolution. Report merge statistics (total, merged, duplicates removed).

### Ques6: Data Validation Framework
- **Company:** Microsoft, Flipkart
- **Difficulty:** Medium
- **Description:** Build a reusable data validator that can validate CSV/JSON records against a set of rules: required fields, data types, value ranges, regex patterns, referential integrity.
- **What's expected:** Define rules in a configuration file or builder pattern. Apply rules to each record. Collect and report all validation errors. Support custom validation rules.

### Ques7: Large File Processing with Streams
- **Company:** Amazon, Google
- **Difficulty:** Medium-Hard
- **Description:** Process a very large CSV file (several GB) that cannot fit in memory. Calculate aggregates, filter rows, and write output -- all using Java Streams without loading the full file.
- **What's expected:** Use `Files.lines()` for lazy streaming. Demonstrate `Collectors.groupingBy()`, `Collectors.summarizingDouble()`. Handle `OutOfMemoryError` scenarios. Benchmark memory usage.

---

## 5. Selenium Practical Tasks

### Ques1: Broken Link Checker
- **Company:** Google, Amazon, Microsoft
- **Difficulty:** Medium
- **Description:** Navigate to a webpage. Find all anchor tags. Check each link's HTTP response code. Report broken links (4xx/5xx), redirected links (3xx), and working links (2xx).
- **What's expected:** Use `findElements(By.tagName("a"))` to collect links. Use `HttpURLConnection` to check status codes. Handle relative URLs. Run checks in parallel for performance.

### Ques2: Handle Dynamic Elements with Explicit Waits
- **Company:** Apple, Microsoft, Amazon
- **Difficulty:** Medium
- **Description:** Automate a page where elements appear/disappear dynamically (e.g., loading spinners, lazy-loaded content, AJAX updates). Wait for elements to be visible/clickable before interacting.
- **What's expected:** Use `WebDriverWait` with `ExpectedConditions`. Avoid `Thread.sleep()`. Handle `StaleElementReferenceException`. Implement custom wait conditions.

### Ques3: Handle JavaScript Alerts, Confirms, and Prompts
- **Company:** Flipkart, Hotstar
- **Difficulty:** Easy-Medium
- **Description:** Automate a page with JavaScript alerts (`alert()`, `confirm()`, `prompt()`). Accept, dismiss, and type text into prompts. Capture alert text.
- **What's expected:** Use `driver.switchTo().alert()`. Use `accept()`, `dismiss()`, `sendKeys()`, `getText()`. Handle `NoAlertPresentException`.

### Ques4: Handle iFrames (Nested Frames)
- **Company:** Microsoft, Apple
- **Difficulty:** Medium
- **Description:** Automate interaction with elements inside iFrames. Handle nested iFrames (frame within a frame). Switch between frames and the main document.
- **What's expected:** Use `driver.switchTo().frame()` by index, name, or WebElement. Switch back using `defaultContent()` or `parentFrame()`. Verify element visibility after switching.

### Ques5: File Upload and Download
- **Company:** Amazon, Flipkart
- **Difficulty:** Medium
- **Description:** Automate file upload using `<input type="file">` and non-standard upload dialogs. Automate file download and verify the downloaded file's name, size, and content.
- **What's expected:** For upload: use `sendKeys(filePath)` on file input. For download: configure browser preferences to auto-download. Use `File.exists()` and content verification. Handle timeouts for large files.

### Ques6: Take Screenshots on Test Failure
- **Company:** Google, Microsoft, Netflix
- **Difficulty:** Easy-Medium
- **Description:** Implement a mechanism that automatically captures a screenshot whenever a test fails. Save screenshots with meaningful names (test name + timestamp). Integrate with TestNG listeners.
- **What's expected:** Use `TakesScreenshot` interface. Implement `ITestListener` or `@AfterMethod` in TestNG. Generate unique file names. Optionally attach to test reports (Allure/Extent).

### Ques7: Handle Multiple Browser Windows/Tabs
- **Company:** Microsoft, Flipkart
- **Difficulty:** Medium
- **Description:** Automate a scenario where clicking a link opens a new window/tab. Switch to the new window, perform actions, close it, and switch back to the original window.
- **What's expected:** Use `getWindowHandles()` and `getWindowHandle()`. Switch using `driver.switchTo().window(handle)`. Close child window and return to parent. Handle multiple child windows.

### Ques8: Drag and Drop
- **Company:** Apple, Hotstar
- **Difficulty:** Medium
- **Description:** Automate drag-and-drop operations using Selenium's `Actions` class. Handle both HTML5 drag-and-drop and legacy JavaScript-based implementations.
- **What's expected:** Use `Actions.dragAndDrop(source, target)` or `clickAndHold().moveToElement().release()`. Handle HTML5 drag-and-drop with JavaScript executor workaround. Verify element position after drop.

### Ques9: Automate Dropdowns (Static and Dynamic)
- **Company:** Amazon, Microsoft
- **Difficulty:** Easy-Medium
- **Description:** Automate selection from standard `<select>` dropdowns, custom dropdowns (non-select elements), searchable dropdowns, and multi-select dropdowns.
- **What's expected:** Use `Select` class for `<select>` elements (`selectByValue`, `selectByVisibleText`, `selectByIndex`). For custom dropdowns: click to open, wait for options, click desired option. Handle `StaleElementReferenceException`.

### Ques10: Handle Shadow DOM Elements
- **Company:** Google, Apple
- **Difficulty:** Hard
- **Description:** Automate interactions with elements inside Shadow DOM. Access shadow root and interact with elements that are not accessible via regular locators.
- **What's expected:** Use `getShadowRoot()` in Selenium 4. Use `JavascriptExecutor` as fallback. Handle nested shadow DOMs. Understand the difference between open and closed shadow roots.

### Ques11: Data-Driven Testing with Excel/CSV
- **Company:** Amazon, Flipkart, Microsoft
- **Difficulty:** Medium
- **Description:** Read test data from an Excel file (using Apache POI) or CSV file. Execute the same test scenario with different data sets. Report pass/fail for each data row.
- **What's expected:** Use Apache POI for `.xlsx` files. Implement a `DataProvider` in TestNG. Handle different data types (string, numeric, date). Report which data rows passed/failed.

### Ques12: Automate Multi-Step Form with Validation
- **Company:** Microsoft, Apple, Amazon
- **Difficulty:** Medium
- **Description:** Automate a multi-step registration or checkout form. Fill in each step, handle field validations, navigate between steps, and verify the final submission.
- **What's expected:** Organize steps using Page Object Model. Handle client-side validations. Verify error messages. Test both happy path and error paths.

### Ques13: Capture Network Traffic / Console Logs
- **Company:** Netflix, Google
- **Difficulty:** Hard
- **Description:** Use Selenium 4's CDP (Chrome DevTools Protocol) to capture network traffic, console logs, and performance metrics during test execution.
- **What's expected:** Use `DevTools` API in Selenium 4. Capture request/response headers and bodies. Log JavaScript console errors. Measure page load performance metrics.

### Ques14: Cross-Browser Testing Setup
- **Company:** Microsoft, Apple
- **Difficulty:** Medium
- **Description:** Design a test that runs on multiple browsers (Chrome, Firefox, Edge, Safari) using a configurable setup. Use WebDriverManager for driver management. Run tests in parallel across browsers.
- **What's expected:** Use factory pattern for WebDriver creation. Configure browsers via properties/XML. Use TestNG `@Parameters` or `@DataProvider` for browser selection. Handle browser-specific quirks.

### Ques15: Cookie and Session Management
- **Company:** Amazon, Netflix
- **Difficulty:** Medium
- **Description:** Automate scenarios involving cookies: add, read, delete cookies. Test session persistence, cookie expiration, and "remember me" functionality.
- **What's expected:** Use `driver.manage().getCookies()`, `addCookie()`, `deleteCookie()`. Verify cookie attributes (name, value, domain, expiry). Test session timeout behavior.

---

## 6. Database Operations

### Ques1: JDBC Query and Validation
- **Company:** Amazon, Microsoft, Flipkart
- **Difficulty:** Medium
- **Description:** Connect to a database using JDBC. Execute SELECT queries and validate the results match expected values. Compare API response data with database records.
- **What's expected:** Use `DriverManager.getConnection()`. Create `PreparedStatement` with parameterized queries (prevent SQL injection). Iterate `ResultSet`. Close resources in `finally` block or use try-with-resources.

### Ques2: Database Data-Driven Testing
- **Company:** Flipkart, Amazon
- **Difficulty:** Medium
- **Description:** Read test data from a database table. Use the data to drive test execution. Write test results back to a results table.
- **What's expected:** Implement TestNG `DataProvider` backed by JDBC. Handle connection pooling. Map `ResultSet` rows to test parameters. Insert results after test execution.

### Ques3: API-to-Database Validation
- **Company:** Amazon, Microsoft, Netflix
- **Difficulty:** Medium-Hard
- **Description:** Send API requests that create/update/delete resources. Directly query the database to verify the changes were persisted correctly. Compare API response with database state.
- **What's expected:** Set up API client (RestAssured) and JDBC connection. Send API request. Query DB for the affected record. Assert field-by-field match. Handle eventual consistency (add waits if needed).

### Ques4: Stored Procedure Testing
- **Company:** Microsoft, Amazon
- **Difficulty:** Medium
- **Description:** Call stored procedures from Java using `CallableStatement`. Pass IN/OUT parameters. Validate the results and side effects of procedure execution.
- **What's expected:** Use `connection.prepareCall("{call procedure_name(?, ?)}")`. Register OUT parameters. Handle multiple result sets. Verify database state after execution.

### Ques5: Database Schema Validation
- **Company:** Apple, Google
- **Difficulty:** Medium
- **Description:** Write a utility that validates the database schema: table existence, column names and types, constraints (PK, FK, NOT NULL, UNIQUE), indexes.
- **What's expected:** Use `DatabaseMetaData` API. Extract table info with `getTables()`, `getColumns()`, `getPrimaryKeys()`. Compare against expected schema definition. Report discrepancies.

### Ques6: Data Migration Validation
- **Company:** Flipkart, Amazon
- **Difficulty:** Hard
- **Description:** Validate a data migration from source DB to target DB. Compare record counts, field values, data transformations, and referential integrity across both databases.
- **What's expected:** Connect to both databases. Run count comparisons per table. Sample-check individual records. Validate transformed fields (e.g., date format changes). Report mismatches.

---

## 7. Linux/Unix Commands in Java

### Ques1: Execute Shell Commands
- **Company:** Apple, Amazon
- **Difficulty:** Easy-Medium
- **Description:** Write a Java utility that executes shell commands (e.g., `ls`, `grep`, `ps`, `curl`) and captures stdout and stderr. Handle command timeouts and non-zero exit codes.
- **What's expected:** Use `ProcessBuilder` or `Runtime.getRuntime().exec()`. Read `InputStream` and `ErrorStream`. Handle process hanging with `waitFor(timeout, TimeUnit)`. Parse command output.

### Ques2: Remote Server Command Execution (SSH)
- **Company:** Netflix, Amazon
- **Difficulty:** Medium-Hard
- **Description:** Connect to a remote server via SSH and execute commands. Capture output. Use this to verify server-side state during integration tests (e.g., check log files, verify processes).
- **What's expected:** Use JSch library for SSH connections. Handle authentication (password/key-based). Execute commands and read output stream. Handle session management and timeouts.

### Ques3: File System Operations
- **Company:** Microsoft, Apple
- **Difficulty:** Easy-Medium
- **Description:** Using Java, implement common file system operations: create directories, copy/move files, change permissions, find files by pattern, calculate directory size.
- **What's expected:** Use `java.nio.file` package (`Files`, `Path`, `Paths`). Use `Files.walk()` for recursive operations. Set permissions with `PosixFilePermissions`. Handle `IOException`.

### Ques4: Process Monitor
- **Company:** Netflix, Google
- **Difficulty:** Medium
- **Description:** Write a Java utility that monitors a system process. Check if a process is running, get its PID, memory usage, and CPU usage. Restart the process if it dies.
- **What's expected:** Use `ProcessHandle` API (Java 9+). Parse `ps` or `/proc` output on Linux. Implement health check with retry. Log monitoring events.

### Ques5: Log Aggregator from Multiple Servers
- **Company:** Amazon, Netflix
- **Difficulty:** Medium-Hard
- **Description:** SSH into multiple remote servers, collect log files, merge them chronologically, and produce a unified log view. Handle time zone differences.
- **What's expected:** Use parallel SSH connections. Parse log timestamps. Merge using a timestamp-based comparator. Handle clock skew between servers. Output unified log stream.

---

## 8. Multithreading & Concurrency

### Ques1: Producer-Consumer Pattern
- **Company:** Amazon, Microsoft, Google
- **Difficulty:** Medium
- **Description:** Implement the producer-consumer pattern using `BlockingQueue`. One thread produces test data (e.g., URLs to test), another thread consumes and tests them. Handle graceful shutdown.
- **What's expected:** Use `LinkedBlockingQueue` or `ArrayBlockingQueue`. Use a poison pill or flag for shutdown. Handle `InterruptedException`. Demonstrate thread-safe result collection.

### Ques2: Thread-Safe Test Result Collector
- **Company:** Microsoft, Amazon
- **Difficulty:** Medium
- **Description:** Multiple test threads run in parallel, each producing results. Implement a thread-safe collector that aggregates results without data loss or corruption.
- **What's expected:** Use `ConcurrentHashMap` or `Collections.synchronizedList()`. Demonstrate understanding of `synchronized`, `volatile`, `AtomicInteger`. Avoid deadlocks. Generate final aggregated report.

### Ques3: Parallel Test Executor
- **Company:** Google, Netflix
- **Difficulty:** Medium-Hard
- **Description:** Build a custom parallel test executor using `ExecutorService`. Given a list of test cases (as `Runnable` or `Callable`), execute them with a configurable thread pool size. Collect results, handle timeouts, and report failures.
- **What's expected:** Use `Executors.newFixedThreadPool()`. Submit tasks and collect `Future` results. Handle `TimeoutException` and `ExecutionException`. Implement a callback mechanism for completion.

### Ques4: Concurrent Web Crawler
- **Company:** Google, Amazon
- **Difficulty:** Hard
- **Description:** Build a multi-threaded web crawler that starts from a seed URL, discovers links, and crawls them concurrently. Avoid visiting the same URL twice. Limit crawl depth and total pages.
- **What's expected:** Use `ConcurrentHashMap` for visited URLs. Use `ExecutorService` for parallel crawling. Use `Semaphore` or `CountDownLatch` for concurrency control. Handle `robots.txt`.

### Ques5: Read-Write Lock for Shared Test Configuration
- **Company:** Microsoft, Apple
- **Difficulty:** Medium
- **Description:** Implement a shared configuration store that allows multiple threads to read simultaneously but requires exclusive access for writes. Use `ReentrantReadWriteLock`.
- **What's expected:** Use `ReadWriteLock` interface. Demonstrate that readers don't block each other but writers block everyone. Handle lock upgrades. Test for deadlock scenarios.

### Ques6: Async API Call Aggregator
- **Company:** Netflix, Meta
- **Difficulty:** Medium-Hard
- **Description:** Make multiple API calls concurrently using `CompletableFuture`. Aggregate the results when all calls complete. Handle individual failures without failing the entire batch.
- **What's expected:** Use `CompletableFuture.supplyAsync()`. Combine with `allOf()` or `anyOf()`. Handle exceptions with `exceptionally()` or `handle()`. Demonstrate `thenCombine()` for dependent calls.

---

## 9. Design Patterns for Test Automation

### Ques1: Page Object Model (POM)
- **Company:** ALL major companies
- **Difficulty:** Medium
- **Description:** Design and implement a Page Object Model for a multi-page web application (e.g., e-commerce site with login, search, product detail, cart, checkout pages). Demonstrate separation of concerns.
- **What's expected:** One class per page. Web elements as private fields with `@FindBy`. Public methods representing user actions. `PageFactory.initElements()`. Fluent API returning next page object.

### Ques2: Singleton WebDriver Manager
- **Company:** Amazon, Microsoft, Apple
- **Difficulty:** Medium
- **Description:** Implement a thread-safe Singleton pattern for managing WebDriver instances. Ensure each test thread gets its own driver instance while reusing configuration.
- **What's expected:** Use `ThreadLocal<WebDriver>` for thread safety. Lazy initialization. Configuration-driven browser selection. Proper cleanup in `@AfterMethod`/`@AfterSuite`. Handle parallel execution.

### Ques3: Factory Pattern for WebDriver Creation
- **Company:** Google, Microsoft
- **Difficulty:** Medium
- **Description:** Implement a Factory pattern that creates different WebDriver instances (Chrome, Firefox, Edge, Safari, Remote) based on configuration. Support headless mode, custom options, and remote execution.
- **What's expected:** Define a `WebDriverFactory` class or interface. Use `enum` or `String` for browser type selection. Configure browser-specific options (`ChromeOptions`, `FirefoxOptions`). Support Selenium Grid URL.

### Ques4: Builder Pattern for Test Data
- **Company:** Amazon, Netflix
- **Difficulty:** Medium
- **Description:** Implement a Builder pattern for creating complex test data objects (e.g., User, Order, Product). Support defaults, overrides, and random data generation.
- **What's expected:** Nested `Builder` class with fluent setters. Immutable final object. Default values for optional fields. Integration with Faker library for random data. `build()` method with validation.

### Ques5: Strategy Pattern for Test Execution
- **Company:** Google, Apple
- **Difficulty:** Medium-Hard
- **Description:** Implement a Strategy pattern for different test execution strategies (e.g., smoke, regression, full, parallel). Each strategy defines which tests to run and how to run them.
- **What's expected:** Define a `TestStrategy` interface with `execute()` method. Concrete strategies for each type. Context class that delegates to the active strategy. Runtime strategy selection via config.

### Ques6: Observer Pattern for Test Events
- **Company:** Netflix, Meta
- **Difficulty:** Medium
- **Description:** Implement an Observer/Listener pattern for test lifecycle events (test started, passed, failed, skipped). Observers can log, take screenshots, send notifications, or update dashboards.
- **What's expected:** Define `TestEventListener` interface. Multiple implementations (Logger, ScreenshotCapture, SlackNotifier). Event publisher that notifies all registered listeners. Integration with TestNG listeners.

### Ques7: Decorator Pattern for Enhanced WebDriver
- **Company:** Apple, Microsoft
- **Difficulty:** Medium-Hard
- **Description:** Implement a Decorator pattern that adds cross-cutting concerns to WebDriver (e.g., logging every action, taking screenshots before/after clicks, adding implicit highlights).
- **What's expected:** Wrap `WebDriver` interface. Add behavior before/after delegating to the wrapped driver. Stack multiple decorators. Demonstrate Open/Closed principle.

---

## 10. Regular Expressions & Text Processing

### Ques1: Email Validator
- **Company:** Microsoft, Amazon
- **Difficulty:** Easy-Medium
- **Description:** Write a regex-based email validator that handles standard email formats, subdomains, plus-addressing (user+tag@domain), and rejects common invalid patterns.
- **What's expected:** Use `Pattern.compile()` and `Matcher`. Handle: `user@domain.com`, `user.name+tag@sub.domain.co.in`. Reject: `@domain.com`, `user@`, `user @domain.com`, `user@.com`.

### Ques2: Log Pattern Extractor
- **Company:** Netflix, Amazon
- **Difficulty:** Medium
- **Description:** Given server log lines in various formats, extract: timestamp, log level, class name, thread name, and message using regex. Handle multiple log formats (Log4j, custom formats).
- **What's expected:** Use named groups `(?<name>pattern)`. Handle multiple format variations. Build a `LogEntry` object from each match. Handle multi-line log entries (stack traces).

### Ques3: URL Parser
- **Company:** Google, Apple
- **Difficulty:** Medium
- **Description:** Parse URLs and extract components: protocol, host, port, path, query parameters, and fragment. Handle edge cases like URLs without ports, with multiple query params, encoded characters.
- **What's expected:** Use regex with groups or `java.net.URI`. Extract each component. Parse query string into `Map<String, String>`. Handle URL encoding/decoding.

### Ques4: Credit Card Number Masker
- **Company:** Amazon, Flipkart
- **Difficulty:** Easy-Medium
- **Description:** Write a function that finds credit card numbers in text and masks them, showing only the last 4 digits (e.g., `4111-1111-1111-1111` becomes `****-****-****-1111`).
- **What's expected:** Use regex to find card patterns (with/without dashes, spaces). Preserve the format while masking. Handle different card formats (16 digits, AMEX 15 digits).

### Ques5: HTML Tag Stripper
- **Company:** Microsoft, Flipkart
- **Difficulty:** Easy-Medium
- **Description:** Write a function that removes all HTML tags from a string, leaving only the text content. Handle nested tags, self-closing tags, and attributes.
- **What's expected:** Use `replaceAll("<[^>]*>", "")` for simple cases. Handle edge cases: script/style tags with content, HTML entities, malformed tags. Discuss why regex is not ideal for full HTML parsing.

### Ques6: CSV Field Extractor (Handling Quoted Fields)
- **Company:** Amazon, Google
- **Difficulty:** Medium
- **Description:** Write a regex-based CSV parser that correctly handles: quoted fields, fields containing commas, fields containing newlines, escaped quotes within fields.
- **What's expected:** Use a state machine or regex with alternation. Handle `"field with, comma"`, `"field with ""escaped"" quotes"`. Demonstrate edge cases. Discuss limitations of regex-only approach.

### Ques7: Version Number Comparator
- **Company:** Apple, Microsoft
- **Difficulty:** Easy-Medium
- **Description:** Parse semantic version strings (e.g., `1.2.3`, `2.0.0-beta.1`) using regex and implement comparison logic. Determine which version is newer.
- **What's expected:** Extract major, minor, patch using regex. Handle pre-release suffixes. Implement `Comparable<Version>`. Handle versions with different segment counts (e.g., `1.0` vs `1.0.0`).

---

## 11. Performance & Load Testing

### Ques1: Simple Load Test Framework
- **Company:** Amazon, Netflix, Google
- **Difficulty:** Medium-Hard
- **Description:** Build a simple load testing tool using Java threads. Send concurrent HTTP requests to an endpoint. Collect and report: throughput (req/sec), response times (min, max, avg, P95, P99), error rate.
- **What's expected:** Use `ExecutorService` for concurrent requests. Collect metrics in a thread-safe manner. Calculate percentiles from response time distribution. Generate a summary report.

### Ques2: JMeter Test Plan Concepts
- **Company:** Amazon, Flipkart, Hotstar
- **Difficulty:** Medium
- **Description:** Describe and design a JMeter test plan for a given scenario (e.g., e-commerce checkout flow). Explain: Thread Groups, Samplers, Assertions, Timers, Listeners, and parameterization.
- **What's expected:** Conceptual understanding of JMeter components. Design ramp-up strategy. Configure think times. Set up CSV Data Set Config for parameterization. Explain result interpretation.

### Ques3: API Throughput Measurement
- **Company:** Netflix, Google
- **Difficulty:** Medium
- **Description:** Write a test that measures the maximum throughput of an API endpoint. Gradually increase the number of concurrent users and find the point where response time degrades or errors increase.
- **What's expected:** Implement step-wise load increase. Monitor response time and error rate at each level. Identify the breaking point. Plot or report the results. Use `ScheduledExecutorService` for controlled ramp-up.

### Ques4: Memory Leak Detection Test
- **Company:** Apple, Google
- **Difficulty:** Hard
- **Description:** Write a test that detects potential memory leaks in a web application. Navigate through the application repeatedly while monitoring browser memory usage via Chrome DevTools Protocol.
- **What's expected:** Use Selenium 4 CDP integration. Capture `JSHeapUsedSize` at regular intervals. Compare memory before and after repeated operations. Flag if memory grows monotonically. Generate a memory trend report.

### Ques5: Gatling/K6 Script Concepts
- **Company:** Netflix, Meta
- **Difficulty:** Medium
- **Description:** Explain how you would write a Gatling (Scala) or K6 (JavaScript) performance test script for a user journey. Describe scenarios, feeders/data, assertions, and reporting.
- **What's expected:** Understanding of virtual users vs threads. Scenario definition with think times. Data parameterization. Assertion on response time SLAs. Report interpretation and CI integration.

---

## 12. Build From Scratch Questions

These are the "machine coding round" style questions where you are given 45-90 minutes to build a working solution.

### Build 1: File Comparator Tool
- **Company:** Microsoft, Amazon
- **Difficulty:** Medium
- **Estimated Time:** 45-60 minutes
- **Requirements:**
  - Accept two file paths as input
  - Compare files line by line
  - Output differences in a diff-like format (added lines with `+`, removed lines with `-`, unchanged lines with a space)
  - Handle files of different lengths
  - Support both text and binary comparison mode
  - Print summary: X lines added, Y lines removed, Z lines unchanged
- **Key Skills Tested:** File I/O, string comparison, edge case handling, clean code structure.

### Build 2: Log Parser and Analyzer
- **Company:** Amazon, Netflix, Flipkart
- **Difficulty:** Medium-Hard
- **Estimated Time:** 60-90 minutes
- **Requirements:**
  - Parse log file lines in the format: `[TIMESTAMP] [LEVEL] [CLASS] - Message`
  - Support filtering by: date range, log level, keyword search
  - Generate summary: count by log level, most common error messages, errors per hour
  - Detect anomalies: sudden spike in ERROR count
  - Output results as a formatted table or JSON
- **Key Skills Tested:** Regex, data aggregation, HashMap usage, streaming large files, report generation.

### Build 3: Broken Link Checker
- **Company:** Google, Microsoft, Apple
- **Difficulty:** Medium
- **Estimated Time:** 45-60 minutes
- **Requirements:**
  - Accept a URL as input
  - Crawl the page and find all `<a>` tags
  - Check each link's HTTP status code
  - Classify: working (2xx), redirected (3xx), broken (4xx), server error (5xx)
  - Support crawling depth (check links on linked pages too)
  - Output a report: URL, Status Code, Status, Parent Page
  - Handle relative URLs, mailto links, tel links, javascript links
- **Key Skills Tested:** Selenium/HTTP client, URL parsing, concurrent processing, error handling.

### Build 4: REST API Test Suite
- **Company:** Amazon, Flipkart, Microsoft
- **Difficulty:** Medium-Hard
- **Estimated Time:** 60-90 minutes
- **Requirements:**
  - Test a RESTful CRUD API (e.g., a user management API)
  - Implement: Create user (POST), Get user (GET), Update user (PUT), Delete user (DELETE)
  - Validate: status codes, response body fields, response headers, response time
  - Implement data cleanup (delete created test data after tests)
  - Use TestNG for test structure and assertions
  - Support running against different environments (dev, staging, prod) via configuration
- **Key Skills Tested:** RestAssured, TestNG, API testing patterns, data cleanup, configuration management.

### Build 5: Web Scraper
- **Company:** Flipkart, Hotstar, Amazon
- **Difficulty:** Medium
- **Estimated Time:** 45-60 minutes
- **Requirements:**
  - Accept a URL and target element selector as input
  - Scrape specific data elements from the page
  - Handle pagination (click "next" and scrape each page)
  - Handle dynamic content loading (wait for AJAX)
  - Store scraped data in a CSV file
  - Include error handling and retry logic
- **Key Skills Tested:** Selenium WebDriver, explicit waits, locator strategies, file writing, error handling.

### Build 6: Data Validator
- **Company:** Microsoft, Amazon, Flipkart
- **Difficulty:** Medium-Hard
- **Estimated Time:** 60-90 minutes
- **Requirements:**
  - Read records from a CSV/JSON file
  - Validate each record against configurable rules:
    - Required field check
    - Data type validation (string, number, date, email, phone)
    - Range validation (min/max for numbers, length for strings)
    - Regex pattern matching
    - Cross-field validation (e.g., end_date > start_date)
  - Collect all errors per record
  - Output a validation report: record number, field name, validation rule, error message
  - Support custom validation rules via interface/lambda
- **Key Skills Tested:** OOP design, strategy/validator pattern, file parsing, error collection, clean architecture.

### Build 7: Test Report Generator
- **Company:** Apple, Netflix
- **Difficulty:** Medium
- **Estimated Time:** 45-60 minutes
- **Requirements:**
  - Accept test results (JSON or XML format from TestNG/JUnit output)
  - Generate an HTML report with: summary (total, pass, fail, skip, duration), charts (pie chart for pass/fail ratio), detailed results per test class and method, failure stack traces
  - Support filtering by status
  - Include execution trend if historical data is available
- **Key Skills Tested:** XML/JSON parsing, HTML generation, data aggregation, file writing.

### Build 8: Configuration Manager
- **Company:** Microsoft, Google
- **Difficulty:** Medium
- **Estimated Time:** 30-45 minutes
- **Requirements:**
  - Read configuration from multiple sources: properties file, environment variables, command-line arguments
  - Implement priority: CLI args > env vars > config file > defaults
  - Support nested configuration (e.g., `database.host`, `database.port`)
  - Type-safe getters: `getString()`, `getInt()`, `getBoolean()`, `getList()`
  - Environment-specific overrides (dev, staging, prod)
- **Key Skills Tested:** Design patterns, properties handling, type conversion, priority resolution.

### Build 9: API Mock Server
- **Company:** Netflix, Google
- **Difficulty:** Hard
- **Estimated Time:** 60-90 minutes
- **Requirements:**
  - Build a lightweight mock server that listens on a port
  - Support defining mock responses for specific routes (method + path)
  - Support request matching by headers, query params, body patterns
  - Return configured status code, headers, and response body
  - Log all received requests for verification
  - Support dynamic responses (templates with placeholders)
- **Key Skills Tested:** HTTP server basics, routing, request parsing, response building, thread handling.

### Build 10: Retry Framework
- **Company:** Amazon, Apple
- **Difficulty:** Medium
- **Estimated Time:** 30-45 minutes
- **Requirements:**
  - Build a reusable retry mechanism for flaky operations
  - Configurable: max retries, delay between retries, exponential backoff, retry-on-exception types
  - Support both synchronous and async operations
  - Log each retry attempt with reason
  - Return the final result or throw after max retries exhausted
  - Integrate with TestNG as a retry analyzer
- **Key Skills Tested:** Generics, functional interfaces, exception handling, TestNG `IRetryAnalyzer`.

---

## 13. Framework Design & Architecture

### Ques1: Design a Test Automation Framework from Scratch
- **Company:** Amazon, Microsoft, Google, Apple (very common in senior SDET rounds)
- **Difficulty:** Hard
- **Description:** Design a complete test automation framework. Explain the architecture, layers, and components. Justify your technology choices.
- **What's expected:**
  - **Layers:** Test layer, Business logic layer, Page object layer, Utility layer, Configuration layer
  - **Components:** WebDriver manager, Configuration reader, Report generator, Logger, Data provider, Screenshot utility, Retry mechanism
  - **Patterns used:** POM, Singleton, Factory, Builder, Strategy
  - **Integration:** CI/CD (Jenkins/GitHub Actions), Test reporting (Allure/Extent), Version control (Git), Containerization (Docker/Selenium Grid)

### Ques2: Design a Test Data Management Strategy
- **Company:** Amazon, Flipkart
- **Difficulty:** Medium-Hard
- **Description:** Design a strategy for managing test data across different test environments. Handle data creation, cleanup, isolation, and parameterization.
- **What's expected:** External data files (CSV/JSON/Excel), Database seeding, API-based data setup, Builder pattern for test objects, Data cleanup strategies (delete after test, database rollback), Environment-specific data.

### Ques3: Design a CI/CD Pipeline for Test Automation
- **Company:** Google, Netflix, Microsoft
- **Difficulty:** Medium
- **Description:** Design a CI/CD pipeline that runs automated tests. Explain stages, triggers, parallelization, reporting, and failure handling.
- **What's expected:** Pipeline stages (build, unit test, integration test, E2E test, report), Trigger mechanisms (on commit, scheduled, manual), Parallel execution strategy, Test environment provisioning, Failure notifications, Test report archival.

### Ques4: Design a Cross-Platform Mobile Test Framework
- **Company:** Apple, Flipkart, Hotstar
- **Difficulty:** Hard
- **Description:** Design a test automation framework for mobile apps that supports both iOS and Android using Appium or similar tools.
- **What's expected:** Appium architecture, Device farm integration, Platform-specific locators with abstraction, Parallel execution on multiple devices, Screenshot and video capture, Cloud device farm (BrowserStack/Sauce Labs).

### Ques5: Design a Microservices Testing Strategy
- **Company:** Netflix, Amazon, Google
- **Difficulty:** Hard
- **Description:** Design a testing strategy for a microservices architecture. Cover unit, integration, contract, and E2E testing.
- **What's expected:** Testing pyramid for microservices, Contract testing (Pact), Service virtualization, API gateway testing, Circuit breaker testing, Distributed tracing validation, Chaos testing concepts.

---

## Quick Reference: Company-Specific Focus Areas

| Company | Primary Focus Areas | Interview Rounds | Typical Questions |
|---------|-------------------|-----------------|-------------------|
| **Google** | Testing strategy, system design for testing, code quality, scalability | 4-5 rounds (coding + design + behavioral) | Broken link checker, test framework design, concurrent test executor |
| **Microsoft** | OOP, design patterns, Selenium, API testing, JDBC | 4 rounds (puzzles + coding + scenarios + behavioral) | File comparator, POM design, API chaining, database validation |
| **Apple** | Automation framework design, debugging, Java/Python fundamentals | 5-7 rounds (online assessment + debugging + coding + behavioral) | Shadow DOM handling, framework design, mobile testing |
| **Amazon** | Coding proficiency, testing principles, automation at scale, leadership principles | Hackerrank + 4-5 rounds (coding + testing + LP) | Log parser, CRUD API suite, data-driven testing, producer-consumer |
| **Meta** | API testing, web services, CI/CD, concurrent testing | 3-4 rounds (coding + design + behavioral) | GraphQL testing, async API aggregator, test event system |
| **Netflix** | Performance testing, resilience testing, monitoring, scalability | 3-4 rounds (technical + system design + cultural) | Load test tool, memory leak detection, mock server, chaos testing |
| **Flipkart** | Selenium, API testing, data processing, e-commerce scenarios | 3-4 rounds (written + coding + QA + behavioral) | Web scraper, CSV processing, data validator, broken link checker |
| **Hotstar** | UI automation, streaming-specific testing, performance | 3-4 rounds (coding + testing + system design) | Video player testing, scraping, dynamic content handling |

---

## Difficulty Distribution

- **Easy (Warm-up):** ~15% -- Basic file I/O, simple GET request validation, alert handling, email regex
- **Medium (Core):** ~55% -- POM design, API chaining, JDBC validation, dynamic waits, CSV parsing, producer-consumer
- **Medium-Hard (Senior):** ~20% -- Framework design, parallel testing, data migration, log aggregator, mock server
- **Hard (Staff/Principal):** ~10% -- Memory leak detection, chaos testing, concurrent crawler, full framework architecture

---

## Recommended Preparation Order

1. **Week 1:** File operations (Ques 1-4), basic Selenium (Ques 1-3, 5-7), basic API testing (Ques 1-3)
2. **Week 2:** Data processing (Ques 1-4), database operations (Ques 1-3), regex (Ques 1-4)
3. **Week 3:** Advanced Selenium (Ques 8-14), advanced API testing (Ques 4-8), design patterns (Ques 1-4)
4. **Week 4:** Build from scratch (pick 3-4), multithreading (Ques 1-4), framework design (Ques 1-3)
5. **Week 5:** Performance testing, advanced topics, mock interviews, revise weak areas

---

## Sources & References

- [InterviewBit - Top SDET Interview Questions 2025](https://www.interviewbit.com/sdet-interview-questions/)
- [TestMu AI - Top 40+ SDET Interview Questions 2024](https://www.testmu.ai/learning-hub/sdet-interview-questions/)
- [GeeksforGeeks - SDET Interview Questions and Answers](https://www.geeksforgeeks.org/software-testing/sdet-interview-questions-and-answers/)
- [Naukri Code360 - Top 50 SDET Interview Questions 2025](https://www.naukri.com/code360/library/sdet-interview-questions)
- [DevLabsAlliance - Top 10 Java Programming Interview Questions for SDET](https://www.devlabsalliance.com/tutorial/top-10-java-programming-interview-questions-for-sdet)
- [Medium (Beknazar) - Top Java Coding Interview Questions for SDET](https://beknazarsuranchiyev.medium.com/top-17-java-coding-interview-questions-for-sdet-a978754eb078)
- [Interview Kickstart - Test Engineer/SDET/QE Interview Questions for Amazon](https://interviewkickstart.com/blogs/interview-questions/test-engineer-sdet-qe-interview-questions-amazon)
- [Interview Kickstart - Test Engineer/SDET/QE Interview Questions for Apple](https://interviewkickstart.com/blogs/interview-questions/test-engineer-sdet-qe-interview-questions-apple)
- [Interview Kickstart - Test Engineer/SDET/QE Interview Questions for Google](https://interviewkickstart.com/blogs/interview-questions/test-engineer-sdet-qe-interview-questions-google)
- [Glassdoor - Apple SDET Interview Questions](https://www.glassdoor.com/Interview/Apple-Software-Development-Engineer-In-Test-SDET-Interview-Questions-EI_IE1138.0,5_KO6,48.htm)
- [Glassdoor - Amazon SDET Interview Questions](https://www.glassdoor.com/Interview/Amazon-Software-Development-Engineer-In-Test-Interview-Questions-EI_IE6036.0,6_KO7,44.htm)
- [GeeksforGeeks - Amazon Interview Experience for SDET](https://www.geeksforgeeks.org/amazon-interview-experience-for-sdet/)
- [Preplaced - Amazon SDET Interview Process & Preparation Guide](https://www.preplaced.in/blog/amazon-sdet-interview-preparation-guide)
- [SoftwareTestingHelp - Top 20 SDET Interview Questions 2025](https://www.softwaretestinghelp.com/sdet-interview-questions-and-answers/)
- [Simplilearn - Selenium Interview Questions and Answers 2025](https://www.simplilearn.com/tutorials/selenium-tutorial/selenium-interview-questions-and-answers)
- [DevLabsAlliance - Top 50 Selenium Interview Questions for SDET](https://www.devlabsalliance.com/interview/top-50-selenium-interview-questions-for-sdet)
- [TestLeaf - Top 60+ API Testing Interview Questions 2025](https://www.testleaf.com/blog/top-60-api-testing-interview-questions-for-fresher-to-experience-2025/)
- [GeeksforGeeks - Top 50+ API Testing Interview Questions 2025](https://www.geeksforgeeks.org/software-testing/api-testing-interview-questions/)
- [FrugalTesting - Top 40 Rest Assured Interview Questions](https://www.frugaltesting.com/blog/master-your-next-interview-top-40-rest-assured-interview-questions-and-expert-answers)
- [BrowserStack - Design Patterns in Automation Framework](https://www.browserstack.com/guide/design-patterns-in-automation-framework)
- [QABash - 10 Test Automation Design Patterns](https://www.qabash.com/10-test-automation-design-patterns/)
- [GeeksforGeeks - Database Testing Interview Questions 2025](https://www.geeksforgeeks.org/software-testing/database-testing-interview-questions/)
- [DEV Community - Top 25 SQL Interview Questions for SDET](https://dev.to/sureshayyanna/top-25-sql-interview-question-for-sdet-2jlk)
- [Testmetry - 7 Java Topics You Need to Ace for SDET Interview](https://testmetry.com/sdet-interview/)
- [FinalRound AI - 25 Most Common SDET Interview Questions](https://www.finalroundai.com/blog/sdet-interview-questions)
- [DiviSolutions - 30+ Most Asked SDET Interview Questions](https://divisolutions.in/essential-sdet-interview-question-bank-covering-java-algorithms-data-structures-and-automation-concepts/)
- [Medium (Career Matrix) - Cracking Apple Interview as Test Automation Engineer](https://careermatrix.medium.com/cracking-apple-interview-as-test-automation-engineer-guest-interview-7cfe71bb1c2f)
- [Hirist Blog - Top 30+ SDET Interview Questions 2025](https://www.hirist.tech/blog/top-30-sdet-interview-questions-and-answers/)
- [MindMajix - Top 30 SDET Interview Questions 2025](https://mindmajix.com/sdet-interview-questions)
