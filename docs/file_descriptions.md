# File & Directory Descriptions

This document provides a high-level overview of the key files and directories in the Cue d'Etat project.

---

## **Root Directory**

*   **`.github/`**: Contains GitHub Actions workflow definitions for Continuous Integration (CI).
*   **`AGENTS.md`**: **Mandatory reading for AI developers.** Contains essential directives and pre-commit requirements.
*   **`app/`**: The main application module, containing all the source code, resources, and build scripts for the Android app.
*   **`build.gradle.kts`**: The root Gradle build script. Defines project-wide configurations and dependencies.
*   **`docs/`**: Contains all project documentation, including architectural guides, feature specifications, and historical context.
*   **`gradle/`**: Contains the Gradle wrapper, which ensures a consistent Gradle version is used for the build.
*   **`settings.gradle.kts`**: The Gradle settings script. Includes the `app` module in the build.

---

## **`app/` Module**

*   **`app/build.gradle.kts`**: The build script for the `app` module. Defines app-specific dependencies, build types, product flavors, and other Android build configurations.
*   **`app/src/main/`**: The main source set for the application.
    *   **`AndroidManifest.xml`**: The core manifest file for the Android app. Declares permissions, activities, services, and other essential components.
    *   **`java/com/az/cuetodetat/`**: The root package for all Kotlin source code.
        *   **`presentation/`**: Contains the UI layer of the application (Jetpack Compose screens, ViewModels, and UI-related models).
        *   **`domain/`**: Contains the core business logic of the application (Use Cases, domain models).
        *   **`data/`**: Contains the data layer of the application (Repositories, data sources like DataStore).
        *   **`di/`**: Contains the Hilt dependency injection modules.
        *   **`util/`**: Contains utility classes and helper functions.
    *   **`res/`**: Contains all non-code resources.
        *   **`drawable/`**: Vector drawables and images.
        *   **`layout/`**: (Legacy) XML layout files.
        *   **`mipmap/`**: App launcher icons.
        *   **`values/`**: String resources, colors, themes, and styles.
*   **`app/src/test/`**: Contains unit tests that run on the local JVM.
*   **`app/src/androidTest/`**: Contains integration and E2E tests that run on an Android device or emulator.

---

## **`docs/` Directory**

*   **`TODO.md`**: High-level project roadmap and future vision.
*   **`00_Project_Overview/`**: Introduction to the project's purpose, persona, and core concepts.
*   **`01_Architecture/`**: Deep dive into the MVI architecture, state management, dependencies, and performance guidelines.
*   **`02_Core_Components/`**: Detailed explanations of the rendering engine, CV module, and other key components.
*   **`03_UI_UX_Guide/`**: Specifications for all visual and interactive elements.
*   **`04_Feature_Specs/`**: Detailed requirements for application features and the official project changelog.
*   **`05_Lessons_of_the_Changelog/`**: Historical context and lessons learned from past development cycles.
*   **`06_Testing/`**: The official testing strategy for the project.
