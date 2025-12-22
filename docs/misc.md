# Miscellaneous Information

This document contains miscellaneous information and conventions for the Cue d'Etat project.

---

## **Branching Strategy**

This project follows a simplified GitFlow model:

*   **`main`**: The `main` branch is the single source of truth. It should always be stable and deployable. Direct commits to `main` are forbidden.
*   **Feature Branches**: All new work (features, bug fixes) must be done on a feature branch.
    *   Branch names should be descriptive (e.g., `feature/add-spin-control`, `fix/camera-crash`).
    *   Branches should be created from the latest `main`.
*   **Pull Requests (PRs)**: When a feature is complete, a pull request is created to merge the feature branch into `main`.
    *   All PRs must pass the CI build (including all tests).
    *   All PRs must be reviewed and approved by at least one other developer before being merged.

## **Code Style**

This project follows the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) and [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide).

The codebase is formatted using `ktlint`. The CI build includes a `ktlint` check, so any code that does not adhere to the style guide will fail the build.

## **Logging**

A custom logging wrapper may be implemented in the future. For now, use the standard `android.util.Log` class.

*   Use `Log.d` for debug messages.
*   Use `Log.w` for warnings.
*   Use `Log.e` for errors.

Avoid leaving excessive logging in production code.

## **Dependencies**

All project dependencies are managed via the Gradle Version Catalog (`libs.versions.toml`). When adding a new dependency, you must add it to the version catalog first.
