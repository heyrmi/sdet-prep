[![Run Framework Tests](https://github.com/heyrmi/sdet-prep/actions/workflows/run-tests.yml/badge.svg)](https://github.com/heyrmi/sdet-prep/actions/workflows/run-tests.yml)

# SDET Interview Prep

Unified interview preparation repo — DSA, SDET practical questions, and test automation framework — all under one roof.

## Project Structure

```
sdet-prep/                          (parent pom - multi-module)
│
├── dsa/                            Pure Java, no external deps
│   ├── DSA_INTERVIEW_QUESTIONS.md  Comprehensive prep guide (~200 problems, 18 topics)
│   └── src/main/java/ra/hul/dsa/
│       ├── arrays/                 Arrays & subarrays
│       ├── strings/                String manipulation
│       ├── hashmap/                HashMap & HashSet
│       ├── linkedlist/             Singly & doubly linked lists
│       ├── stack/                  Stack problems
│       ├── queue/                  Queue problems
│       ├── tree/                   Binary trees, BST
│       ├── graph/                  Graph traversal (BFS, DFS)
│       ├── heap/                   Priority queue / heap
│       ├── binarysearch/           Binary search variants
│       ├── slidingwindow/          Sliding window technique
│       ├── twopointers/            Two pointer technique
│       ├── dp/                     Dynamic programming
│       ├── backtracking/           Backtracking & recursion
│       ├── greedy/                 Greedy algorithms
│       ├── sorting/                Sorting algorithms
│       ├── matrix/                 2D array / matrix
│       └── recursion/              Pure recursion
│
├── sdet/                           Selenium, RestAssured, TestNG
│   ├── SDET_INTERVIEW_QUESTIONS.md Comprehensive prep guide (100+ questions, 13 categories)
│   └── src/main/java/ra/hul/sdet/
│       ├── fileops/                File comparison, log parsing, I/O
│       ├── scraping/               Web scraping & data extraction
│       ├── api/                    REST API testing scenarios
│       ├── dataprocessing/         CSV/JSON/XML parsing & transformations
│       ├── selenium/               Selenium practical questions
│       ├── database/               JDBC, data validation, schema checks
│       ├── linux/                  Shell commands, SSH, process monitoring
│       ├── multithreading/         Producer-consumer, thread-safe collections
│       ├── regex/                  Pattern matching, text processing
│       └── performance/            Load testing, throughput measurement
│
└── framework/                      Build this yourself — skeleton ready
    ├── FRAMEWORK_GUIDE.md          What to build in each package
    ├── src/main/java/ra/hul/framework/
    │   ├── api/                    ApiRequestBuilder, ApiResponseValidator
    │   ├── config/                 ConfigReader (Singleton, env layering)
    │   ├── constants/              Endpoints, TimeoutConstants
    │   ├── driver/                 DriverFactory (Factory + ThreadLocal)
    │   ├── listeners/              TestListener, RetryAnalyzer, RetryTransformer
    │   ├── models/                 POJOs for API serialization
    │   ├── pages/                  BasePage + Page Objects (POM)
    │   ├── reporting/              ExtentReportManager
    │   └── utils/                  WaitHelper, ScreenshotUtil, JsonUtils
    ├── src/test/java/ra/hul/tests/
    │   ├── base/                   BaseTest, BaseApiTest
    │   ├── ui/                     UI test classes
    │   └── api/                    API test classes
    └── src/test/resources/
        ├── config.properties       Default config
        ├── config-dev.properties   Dev overrides
        ├── testng.xml              Suite configuration
        ├── log4j2.xml              Logging configuration
        └── schemas/                JSON schema files
```

## Prep Guides

| Guide | Location | What it covers |
|-------|----------|----------------|
| **DSA** | `dsa/DSA_INTERVIEW_QUESTIONS.md` | ~200 problems across 18 topics, ranked by frequency at Google/Amazon/Meta/Microsoft/Apple/Netflix/Hotstar. MUST-DO vs GOOD-TO-KNOW. 8-week study plan. |
| **SDET** | `sdet/SDET_INTERVIEW_QUESTIONS.md` | 100+ practical questions across 13 categories. File ops, scraping, API, Selenium, DB, multithreading, regex, performance. 10 "build from scratch" challenges. 5-week study plan. |
| **Framework** | `framework/FRAMEWORK_GUIDE.md` | What to build in each package. Design patterns. Reference to hotstar-interview-framework. |

## Naming Convention

All questions follow: `Ques{N}_{ProblemName}.java` — every file has a runnable `main()` method.

## Running

```bash
# Build everything
mvn clean compile

# Run any DSA question
cd dsa && mvn exec:java -Dexec.mainClass="ra.hul.dsa.arrays.Ques1_TwoSum"

# Run any SDET question
cd sdet && mvn exec:java -Dexec.mainClass="ra.hul.sdet.api.Ques1_GetRequestValidation"

# Run framework tests
cd framework && mvn test
```

## Tech Stack

- **Java 25**
- **Selenium 4.41.0** — UI automation
- **REST Assured 6.0.0** — API testing
- **TestNG 7.12.0** — Test framework
- **ExtentReports 5.1.2** — HTML reporting (framework module)
- **Log4j2 2.24.3** — Logging (framework module)
- **Jackson 2.18.2** — JSON serialization (framework module)
