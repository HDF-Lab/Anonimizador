
# Testing Strategy

## 1. Overview

Testing is used to verify the correctness, reliability, and maintainability of the anonymization pipeline.

The main focus is on business rules, detection accuracy, interval validation, masking behavior, and file processing.

## 2. Testing Principles

- Test business rules independently of Android UI components.
- Cover valid and invalid input.
- Verify boundary conditions.
- Test overlapping detection intervals.
- Verify deterministic behavior.
- Test coroutine cancellation where applicable.
- Avoid using real personal information in test fixtures.

## 3. Test Execution

Run unit tests:

```bash
./gradlew test
```

Generate JaCoCo coverage reports for the `debug` unit tests:

```bash
./gradlew :app:jacocoTestReport
```

The task runs the debug unit tests and writes HTML and XML reports to:

- `app/build/reports/jacoco/jacocoTestReport/html/index.html`
- `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`

Build the debug application:

```bash
./gradlew assembleDebug
```

Run Android lint checks:

```bash
./gradlew lint
```

## 11. Test Data and Privacy

Use synthetic test data whenever possible.

Do not include real personal information in:

- Test fixtures.
- Screenshots.
- Logs.
- Committed sample documents.
- Test reports.

Test data should be representative without exposing actual personal information.
