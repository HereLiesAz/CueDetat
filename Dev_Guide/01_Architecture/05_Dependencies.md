# 1.5. Build Configuration & Dependencies

This document specifies the mandatory Gradle configuration and library dependencies required to
compile the application. These settings are the foundation upon which the entire structure is built.

## Project Structure

The root project, defined in `settings.gradle.kts`, must contain two modules:

- `:app`: The main application module.
- `:opencv`: The pre-compiled OpenCV library module.

## App Module Configuration (`app/build.gradle.kts`)

The main application module must be configured with the following parameters:

* **Plugins:**
    * `com.android.application`
    * `org.jetbrains.kotlin.android`
    * `kotlin-kapt`
    * `com.google.dagger.hilt.android`
    * `org.jetbrains.kotlin.plugin.compose`

* **Android Block:**
    * **`namespace`**: `com.hereliesaz.cuedetat`
    * **`compileSdk`**: 36
    * **`defaultConfig`**:
        * `minSdk`: 26
        * `targetSdk`: 36
        * `versionCode`: 20
        * `versionName`: "0.8.3.1"
        * **ABI Filters**: The build must be restricted to `arm64-v8a` to manage APK size.
    * **`buildTypes`**:
        * `release`: Must have `isMinifyEnabled` and `isShrinkResources` set to `true`, using the
          standard Android optimization profile and the project's `proguard-rules.pro`.
    * **`buildFeatures`**: `compose` and `buildConfig` must be enabled.
    * **`kotlinOptions`**: `jvmTarget` must be "17".

## Core Dependencies

The project's dependencies are managed via a `libs.versions.toml` file. The following are the
essential libraries and their purpose:

* **Android Jetpack:**
    * `core-ktx`: Kotlin extensions for the core framework.
    * `lifecycle-runtime-ktx`, `lifecycle-runtime-compose`: For lifecycle-aware coroutines and
      composables.
    * `activity-compose`: Integration for Jetpack Compose in activities.

* **Jetpack Compose:**
    * `compose-bom`: Manages versions for all Compose libraries.
    * `ui`, `ui-graphics`, `ui-tooling-preview`: Core Compose UI components.
    * `material3`, `material-icons-extended`: Material Design 3 components and icons.

* **Dependency Injection (Hilt):**
    * `hilt-android`: Core Hilt/Dagger library.
    * `hilt-compiler`: Kapt annotation processor for Hilt.
    * `hilt-navigation-compose`: Integration for Hilt with Compose Navigation.

* **CameraX:**
    * `camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`: The core components for
      creating a camera preview and analyzing image streams.

* **Networking (Retrofit):**
    * `retrofit`, `converter-gson`: For making network calls to the GitHub API.

* **Computer Vision:**
    * `project(":opencv")`: The local OpenCV module.
    * `object-detection-custom` & `vision-common` (Google ML Kit): For the "Scout" phase of the
      vision pipeline.
    * `tensorflow-lite-task-vision`: The TFLite Task Library for custom models.