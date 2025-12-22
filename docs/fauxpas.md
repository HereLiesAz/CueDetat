# Faux Pas: Common Mistakes & Anti-Patterns

This document lists common mistakes, anti-patterns, and deviations from best practices that should be avoided when working on the Cue d'Etat project. It is a living document that should be updated as new lessons are learned.

For a detailed historical context of *why* these are considered faux pas, please refer to the **[05 Lessons of the Changelog](./05_Lessons_of_the_Changelog/INDEX.md)**.

---

## **1. Violating Unidirectional Data Flow (UDF)**

*   **The Mistake:** Modifying state directly from the UI layer or having a View observe a data source other than its designated ViewModel.
*   **Why It's Wrong:** UDF is the architectural cornerstone of this project. Bypassing it leads to unpredictable state, race conditions, and makes debugging nearly impossible.
*   **The Correct Approach:** State flows down from the ViewModel. Events flow up from the UI. There are no exceptions.

## **2. Allocating Objects in the Rendering Loop**

*   **The Mistake:** Creating new `Paint`, `Path`, or other objects inside the `ProtractorOverlayView.onDraw()` method.
*   **Why It's Wrong:** This is the #1 cause of performance issues. It creates excessive work for the garbage collector, leading to skipped frames and a stuttering UI.
*   **The Correct Approach:** Pre-allocate and reuse all rendering objects. Use the `PaintCache` for `Paint` objects and member variables for other reusable objects.

## **3. Introducing Side Effects in Composable Functions**

*   **The Mistake:** Launching a coroutine, modifying a global variable, or making a network call directly from within a `@Composable` function.
*   **Why It's Wrong:** Composables should be pure functions of their inputs. They can be recomposed at any time, many times per second. Side effects in composables lead to unpredictable behavior and performance problems.
*   **The Correct Approach:** Use the appropriate `LaunchedEffect`, `SideEffect`, or `DisposableEffect` for managing side effects within the composable lifecycle.

## **4. Ignoring the Project Persona**

*   **The Mistake:** Writing user-facing text that is generic, bland, or overly formal.
*   **Why It's Wrong:** The cynical, witty persona is a key feature of the application. Inconsistent tone degrades the user experience.
*   **The Correct Approach:** All user-facing text must adhere to the guidelines in the **[01 Project Persona](./00_Project_Overview/01_ProjectPersona.md)** document.

## **5. Hardcoding Numbers and Strings**

*   **The Mistake:** Using magic numbers in calculations or hardcoding user-facing strings directly in the code.
*   **Why It's Wrong:** This makes the code difficult to understand, maintain, and localize.
*   **The Correct Approach:** Define constants for numerical values. Externalize all user-facing strings into `res/values/strings.xml`.
