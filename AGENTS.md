# AI Agent Directives

This document provides mandatory instructions for any AI agent contributing to this repository. Adherence to these directives is non-negotiable.

---

## **I. Pre-Commit Mandates: The Unbreakable Vows**

Before any code is committed, you **MUST** adhere to the following, without exception:

1.  **Obtain a Complete Code Review:** All changes, no matter how minor, must be reviewed by a human developer. You are required to request a review and wait for explicit approval before proceeding.
2.  **Ensure a Passing Build with All Tests:** You must run the complete test suite and confirm that all tests pass. A broken build or failing test is an automatic rejection of the commit.
3.  **Keep All Documentation Up-to-Date:** Your changes must be reflected in all relevant documentation, including inline comments and the files within the `/docs` directory. If your code alters a feature, its corresponding documentation must be updated in the same commit.

**There are no exceptions. These are the core principles of maintaining this repository's quality and stability.**

---

## **II. The Documentation is Not Optional**

The `/docs` directory contains the comprehensive guide to this project's architecture, components, features, and history. It is not merely a suggestion; it is an extension of this `AGENTS.md` file and carries the same weight.

You are required to have a complete and thorough understanding of these documents before writing a single line of code. They are the source of truth for how this application is built and how it should evolve.

### **Documentation Index (`/docs`)**

*   **[00 Project Overview](./docs/00_Project_Overview/INDEX.md):** High-level introduction, project persona, and core concepts.
*   **[01 Architecture](./docs/01_Architecture/INDEX.md):** MVI architecture, state management, dependencies, and build rules.
*   **[02 Core Components](./docs/02_Core_Components/INDEX.md):** Detailed breakdown of major components like the rendering engine, perspective transformations, and computer vision module.
*   **[03 UI/UX Guide](./docs/03_UI_UX_Guide/INDEX.md):** Specifications for visual style, theming, and all interactive elements (gestures, buttons, sliders).
*   **[04 Feature Specs](./docs/04_Feature_Specs/INDEX.md):** Detailed requirements for all application features and a log of open issues.
*   **[05 Lessons of the Changelog](./docs/05_Lessons_of_the_Changelog/INDEX.md):** A historical record of bugs and architectural decisions that inform the current state of the codebase.

Ignorance of the documentation is not an excuse for failure. Study it. Adhere to it. Update it.
