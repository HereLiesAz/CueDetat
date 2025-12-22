# Development Workflow & Task Flow

This document outlines the standard workflow for contributing to the Cue d'Etat project, from picking up a task to getting it merged into the `main` branch.

---

## **Step 1: Assign a Task**

*   All tasks, features, and bugs are tracked in the **[Changelog & Issues](./04_Feature_Specs/08_Changelog_And_Issues.md)** document.
*   Assign the task to yourself to let others know you are working on it.
*   If the task is not yet documented, add it to the list.

## **Step 2: Create a Feature Branch**

*   Ensure your local `main` branch is up-to-date:
    ```bash
    git checkout main
    git pull origin main
    ```
*   Create a new feature branch with a descriptive name:
    ```bash
    git checkout -b feature/your-descriptive-feature-name
    ```

## **Step 3: Understand the Requirements**

*   Thoroughly read all relevant documentation before starting to code. This includes the architectural guides, feature specifications, and UI/UX guidelines.
*   **Crucially, you must read and understand the mandatory instructions in the root `AGENTS.md` file.**

## **Step 4: Implementation (The "Code" Part)**

*   Write your code, adhering to the project's architecture, code style, and persona.
*   **Write tests alongside your code.** All new features must have test coverage. All bug fixes must include a regression test.
*   Ensure your code is well-documented with inline comments where the logic is complex.

## **Step 5: Local Verification**

*   Before submitting your work, run all checks locally:
    *   **Run the linter:** `./gradlew ktlintCheck`
    *   **Run the tests:** `./gradlew testDebugUnitTest`
    *   **Build the app:** `./gradlew assembleDebug`

## **Step 6: Update Documentation**

*   If your changes affect any aspect of the application's functionality, architecture, or UI, you **must** update the corresponding documentation in the `/docs` directory.
*   Update the **[Changelog & Issues](./04_Feature_Specs/08_Changelog_And_Issues.md)** to reflect the completion of your task.

## **Step 7: Create a Pull Request (PR)**

*   Push your feature branch to the remote repository:
    ```bash
    git push origin feature/your-descriptive-feature-name
    ```
*   Create a pull request on GitHub, targeting the `main` branch.
*   The PR description should be clear and concise, explaining what you changed and why.

## **Step 8: Code Review & CI**

*   The CI pipeline will automatically run all tests and checks on your PR.
*   Request a review from at least one other developer.
*   Address any feedback or requested changes by pushing new commits to your feature branch. The PR will update automatically.

## **Step 9: Merge**

*   Once the PR has been approved and all CI checks are passing, it can be merged into the `main` branch.
*   Delete the feature branch after the PR is merged.
