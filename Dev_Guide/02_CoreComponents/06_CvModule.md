# 2.6. OpenCV Module Integration

The application depends on a local `:opencv` module that contains the pre-compiled native libraries
and their Java/Kotlin wrappers.

## Module Structure

The `:opencv` module is an Android Library module (`com.android.library`). Its primary purpose is to
package the necessary OpenCV assets.

* **Source:** The contents of this module are derived directly from the official **OpenCV Android
  SDK**.
* **Native Libraries (`.so` files):**
    * The pre-compiled native shared object files (`libopencv_java4.so`) must be placed in
      `opencv/src/main/native/libs/`.
    * Subdirectories for each required ABI (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`) must be
      created within this folder, each containing its respective `.so` file.
* **Java Wrappers:**
    * The entire `org.opencv.*` Java source tree from the SDK must be placed in
      `opencv/src/main/java/`.

## Build Configuration (`opencv/build.gradle`)

The module's `build.gradle` file is responsible for correctly identifying and packaging the native
libraries. It must be configured as an Android Library and its `sourceSets` block must point the
`jniLibs` source directory to `src/main/native/libs`.

## Application-Level Integration

The `MyApplication.kt` class is responsible for loading the native library at runtime. This **must**
be done before any OpenCV functions are called. The `onCreate` method must call
`OpenCVLoader.initDebug()` to ensure the native library is available to the application.