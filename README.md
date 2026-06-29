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
└── framework/                      Full-stack test automation framework (Web + API + Mobile + Perf)
    ├── README.md                   Detailed framework docs (setup, patterns, CI)
    ├── MOBILE_SETUP.md             Appium / emulator setup guide
    ├── src/main/java/ra/hul/framework/
    │   ├── core/                   ConfigManager, constants, Allure/retry listeners
    │   ├── web/                    DriverFactory (Strategy), POM page objects, WaitUtils
    │   ├── api/                    ApiClient (Rest Assured) + Lombok POJOs
    │   ├── mobile/                 Appium driver/screen objects (Screen Object Model)
    │   └── performance/            Gatling load-test simulations
    ├── src/test/java/ra/hul/tests/
    │   ├── base/                   BaseWebTest, BaseApiTest, BaseMobileTest
    │   ├── web/                    11 web test classes
    │   ├── api/                    8 API test classes
    │   └── mobile/                 5 mobile test classes
    └── src/test/resources/
        ├── config.properties              Default config
        ├── config-{dev,stage,prod}.properties   Environment overrides
        ├── {web,api,mobile,smoke,all}-tests.xml  TestNG suites
        ├── allure.properties / categories.json  Allure reporting config
        ├── log4j2.xml                      Logging configuration
        └── schemas/                        JSON schema files
```

## Prep Guides

| Guide | Location | What it covers |
|-------|----------|----------------|
| **DSA** | `dsa/DSA_INTERVIEW_QUESTIONS.md` | ~200 problems across 18 topics, ranked by frequency at Google/Amazon/Meta/Microsoft/Apple/Netflix/Hotstar. MUST-DO vs GOOD-TO-KNOW. 8-week study plan. |
| **SDET** | `sdet/SDET_INTERVIEW_QUESTIONS.md` | 100+ practical questions across 13 categories. File ops, scraping, API, Selenium, DB, multithreading, regex, performance. 10 "build from scratch" challenges. 5-week study plan. |
| **Framework** | `framework/README.md` | Production-grade automation framework spanning Web, API, Mobile, and Performance. Design patterns, config resolution, CI/CD, contributing rules. |

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

# Run framework tests (per module via Maven profiles)
cd framework && mvn test -Pweb     # or -Papi / -Pmobile / -Psmoke; bare `mvn test` runs all
```

> See `framework/README.md` for environment/browser selection, remote/grid execution, Gatling performance runs, and Allure reporting.

## Tech Stack

- **Java 25** — toolchain (DSA/SDET modules); the framework module compiles at release 21
- **Selenium 4.41.0** — web automation
- **REST Assured 6.0.0** — API testing
- **Appium 10.1.0** — mobile automation (framework module)
- **Gatling 3.15.0** — performance / load testing (framework module)
- **TestNG 7.12.0** — test runner
- **Allure 2.33.0** — reporting (framework module)
- **Log4j2 2.25.3** — logging (framework module)
- **Jackson 3.1.0** — JSON serialization (framework module)
- **Lombok 1.18.44** — boilerplate reduction (framework module)
