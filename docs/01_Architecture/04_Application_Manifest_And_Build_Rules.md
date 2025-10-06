# 1.6. Application Manifest & Build Rules

This document specifies the application's contract with the Android OS (`AndroidManifest.xml`) and
its code shrinking rules for release builds (`proguard-rules.pro`).

## Android Manifest (`app/src/main/AndroidManifest.xml`)

The manifest must contain the following essential declarations:

* **Permissions:**
    * `android.permission.CAMERA`: Required for the core functionality.
    * `android.permission.INTERNET`: Required for the "Check for Updates" feature.
* **Features:**
    * `android.hardware.camera.any`: Declares that the app requires a camera.
* **Application Tag:**
    * `android:name=".MyApplication"`: This is **critical**. It specifies the custom Application
      class that initializes Hilt. The build will fail without it.
    * `android:icon` and `android:roundIcon` must point to the launcher icons in the `mipmap`
      directories.
    * `android:theme` must point to `@style/Theme.CueDetat`.
* **Activity Tag:**
    * The `MainActivity` must be declared as the single entry point for the application.
    * It must contain an `intent-filter` with the `android.intent.action.MAIN` action and
      `android.intent.category.LAUNCHER` category.

## Proguard Rules (`app/proguard-rules.pro`)

For release builds where code shrinking (`isMinifyEnabled = true`) is active, the following rules
are mandatory to prevent crashes:

* **Jetpack Compose:** Rules must be included to keep all `@Composable` functions and core Compose
  runtime classes.
* **Kotlin Coroutines:** Rules must keep `kotlinx.coroutines` internal classes, which are used via
  reflection.
* **Networking (Retrofit/Gson):** Rules must keep all classes within the `retrofit2`, `okhttp3`,
  `okio`, and `com.google.gson` packages, as they are heavily reliant on reflection. The
  application's own network data classes (e.g., `GithubRelease`) must also be kept.
* **Dependency Injection (Hilt):** Hilt's generated `_HiltModules`, `_Factory`, and
  `_MembersInjector` classes must be preserved.
* **OpenCV:** All classes in the `org.opencv.**` package must be kept. The JNI bindings are fragile
  and must not be obfuscated or removed.
* **Google ML Kit / TensorFlow Lite:** All classes within `com.google.mlkit.**` and
  `org.tensorflow.**` must be kept. Native JNI methods must be preserved across all classes.
* **Application Models:** All data, state, and model classes within the application's package must
  be kept to prevent issues with state management and serialization.