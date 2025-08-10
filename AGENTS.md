# Geministrator Agent Guide

This document provides guidance for AI agents working on the Geministrator project.

## Project Overview

Geministrator is an AI-powered development assistant built on a team of collaborative agents. It can deconstruct high-level tasks into a detailed, multi-step execution plan and then execute that plan.

## Architecture

The project is a multi-module Gradle project with the following structure:

-   `:cli`: The core logic of the application, including the `Orchestrator` and the council of agents. It is used as a library by the other modules.
-   `:app_android`: The Android app front-end.
-   `:plugin_android_studio`: The Android Studio plugin front-end.
-   `:plugin_vscode`: The VSCode extension front-end.
-   `:prompts`: Contains the JSON files that define the behavior of the AI agents.

### The Council of Agents

Geministrator operates not as a single monolithic AI, but as a team of specialists with distinct roles, managed by a central `Orchestrator`.

-   **Orchestrator**: Manages the master plan and deploys agents based on a triage assessment.
-   **Manager**: Executes the step-by-step workflow for a single task.
-   **Architect**: Analyzes existing code to provide context.
-   **Researcher**: Scours the web for best practices and documentation.
-   **Designer**: Creates specifications and updates changelogs.
-   **Antagonist**: Critiques plans to find flaws before execution.
-   **Tech Support**: Analyzes merge conflicts and other technical failures.

### `AbstractCommand` and `ExecutionAdapter`

The system uses a set of universal commands called `AbstractCommand`s, defined in `cli/src/main/kotlin/com/hereliesaz/geministrator/common/AbstractCommand.kt`. These commands are the only way the agents can interact with the environment (file system, Git, etc.).

Each front-end (CLI, Android, plugins) has its own implementation of the `ExecutionAdapter` interface, which is responsible for executing the `AbstractCommand`s in that specific environment.

## Development Conventions

### Adding New Features

To add a new feature, you will likely need to:

1.  Add a new `AbstractCommand` to `AbstractCommand.kt`.
2.  Implement the execution of the new command in the relevant `ExecutionAdapter`s (`CliAdapter`, `AndroidExecutionAdapter`, `AndroidStudioAdapter`).
3.  Modify the `Orchestrator` or one of the agents to use the new command.

### `CHANGELOG.md`

The `CHANGELOG.md` file is the source of truth for the project's history and the TODO list for future development. All changes should be reflected in this file. When you are asked to complete a task, you should look for it in the `CHANGELOG.md` TODO list and mark it as complete when you are done.

### Running Tests

The project has unit tests in the `app_android` module. To run them, use the following command from the root of the project:

```bash
./gradlew :app_android:test
```

### Version Catalogs

The project uses a Gradle Version Catalog (`gradle/libs.versions.toml`) to manage dependencies. All dependencies should be added to this file.
