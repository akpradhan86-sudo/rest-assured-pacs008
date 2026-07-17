![pacs.008 API tests](https://github.com/akpradhan86-sudo/rest-assured-pacs008/actions/workflows/api-tests.yml/badge.svg)

# rest-assured-pacs008

A Java API test automation framework that validates **ISO 20022 pacs.008** (`FIToFICstmrCdtTrf` — FI-to-FI Customer Credit Transfer) payment messages, built with **REST Assured**, **TestNG**, and **WireMock**.

This is a personal portfolio project built to demonstrate API test framework design for payments — payload construction, mocked service behavior, and positive/negative/system-failure test coverage. It runs entirely against a WireMock-simulated API, not a live payments backend.

## What it does

- Generates valid and deliberately invalid pacs.008 XML payloads (missing `MsgId`, invalid BIC, zero/negative amount, invalid currency, malformed XML) via a reusable payload builder.
- Stubs a realistic payments API response layer with WireMock — correct HTTP status codes (`200`, `400`, `415`, `422`, `500`) and structured JSON error bodies per failure type — so the suite runs independently of any live backend.
- Asserts on response structure, status codes, and field-level error content using REST Assured's JSON/XML path support and Hamcrest matchers.

## Tech stack

| Layer | Tool |
|---|---|
| Language | Java 17 |
| API testing | REST Assured 5.4.0 |
| Test framework | TestNG 7.10.2 |
| Service mocking | WireMock 3.5.2 |
| JSON handling | Jackson Databind 2.17.0 |
| Build | Maven |
| CI | GitHub Actions |

## Project structure

```
src/test/java/com/payments/
├── api/
│   ├── Pacs008Client.java          # REST client wrapper for sending pacs.008 requests
│   └── WireMockSetup.java          # Mocked API server: status codes + error responses per scenario
├── config/
│   └── ApiConfig.java              # Base URI / environment config
├── data/
│   └── Pacs008PayloadBuilder.java  # Builds valid + invalid pacs.008 XML payloads
├── tests/
│   ├── BaseApiTest.java            # Shared setup/teardown
│   ├── Pacs008HappyPathTest.java   # Valid payment scenarios
│   ├── Pacs008NegativeTest.java    # Field-validation rejection scenarios
│   └── Pacs008SystemFailureTest.java # Infra/system failure handling
└── validation/
    └── Pacs008ResponseAssertions.java # Reusable response assertion helpers
```

## Test coverage

- **Happy path:** valid payment accepted with correct response structure
- **Negative:** missing `MsgId`, invalid debtor BIC, zero/negative settlement amount, invalid currency code, missing creditor BIC, malformed XML
- **System failure:** simulated downstream/infrastructure failure responses

Run locally:

```bash
mvn test
```

This runs `testng.xml` at the project root, which defines a **Smoke** group (core happy-path + core negative checks, for fast pre-merge sanity) and a **Regression** group (all happy-path, negative, and system-failure tests).

## CI

GitHub Actions runs the full suite on every push to `main`, on every pull request, and nightly on a schedule, uploading Surefire reports as a build artifact.

## Known limitations / next steps

- All tests run against a WireMock stub, not a real payments API — this project demonstrates test *design*, not integration with a live system.
- The `suites/smoke.xml` and `suites/regression.xml` files reference a `Pacs008SchemaValidationTest` class that was never implemented; they are not currently wired into CI (CI uses the root `testng.xml`, which does not reference that class). Either implement the schema validation class or remove those unused suite files.
- `.idea/` and `target/` build output are currently committed — add them to `.gitignore` and clean history.
- Planned: add explicit XSD schema validation against the ISO 20022 pacs.008 schema, and a database/state-check layer to move beyond pure API-contract testing.
