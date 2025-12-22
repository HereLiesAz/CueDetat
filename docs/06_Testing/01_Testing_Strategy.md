# 01. Testing Strategy

This document outlines the testing philosophy and strategy for the Cue d'Etat project. The primary goal is to ensure the application is reliable, performant, and free of regressions.

---

## **I. Testing Philosophy**

1.  **Pragmatism Over Dogma:** We prioritize tests that provide the most value in preventing bugs. We do not strictly adhere to a specific test coverage percentage, but instead focus on testing critical paths and complex logic.
2.  **The Testing Pyramid:** We follow the principles of the testing pyramid:
    *   **Unit Tests (Large Base):** Most of our tests are unit tests. They are fast, reliable, and isolate the smallest units of code (e.g., individual functions, classes).
    *   **Integration Tests (Smaller Mid-Layer):** We use integration tests to verify the interactions between different components (e.g., ViewModel, Use Cases, and Repositories).
    *   **End-to-End (E2E) Tests (Very Small Top):** We have a minimal set of E2E tests for critical user flows. These are the slowest and most brittle tests, so they are used sparingly.
3.  **CI/CD Integration:** All tests are run automatically on every pull request via our Continuous Integration (CI) pipeline. A passing test suite is a mandatory requirement for merging any code.

---

## **II. Types of Tests & Tooling**

### **A. Unit Tests**

*   **Location:** `app/src/test`
*   **Framework:** JUnit 5, MockK
*   **Purpose:** To test individual classes and functions in isolation. This includes:
    *   **ViewModels:** Testing state changes in response to events.
    *   **Use Cases:** Verifying business logic.
    *   **Mappers & Parsers:** Ensuring data is transformed correctly.
    *   **Utils/Helper Functions:** Testing utility functions with various inputs.
*   **Execution:** Run via the `./gradlew testDebugUnitTest` command.

### **B. Integration Tests**

*   **Location:** `app/src/androidTest`
*   **Framework:** AndroidX Test, Espresso, Hilt Android Testing
*   **Purpose:** To test the collaboration between different parts of the application. This includes:
    *   **Data Layer:** Testing the interaction between Repositories and data sources (e.g., DataStore).
    *   **UI Layer:** Testing that UI components correctly observe and react to ViewModel state changes.
*   **Execution:** Run on an emulator or physical device.

### **C. End-to-End (E2E) Tests**

*   **Location:** `app/src/androidTest`
*   **Framework:** UI Automator, Espresso
*   **Purpose:** To simulate a full user journey through the application. This is reserved for the most critical paths, such as:
    *   The main application startup flow.
    *   Core feature interaction (e.g., placing balls and seeing the aiming lines).
*   **Execution:** Run on an emulator or physical device.

---

## **III. When to Write Tests**

*   **New Features:** All new features must be accompanied by relevant unit and/or integration tests.
*   **Bug Fixes:** Every bug fix must include a test that reproduces the bug and verifies the fix. This prevents regressions.
*   **Refactoring:** When refactoring existing code, the existing tests should still pass. If the refactoring is significant, new tests may be required to cover the new implementation.
