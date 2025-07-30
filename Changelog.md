# Changelog

## v0.8.4.0 (2025-07-30)

### Feature

- Complete rewrite of Hater Mode, now featuring a robust physics simulation powered by the KPhysics
  engine. User input (device tilt, shake, drag) now applies physical forces to the on-screen
  objects.

### Fix

- **Build:** Removed duplicate `kphysics` and `DataStore` dependencies from `app/build.gradle.kts`.
- **Memory:** Patched memory leaks in `QuickAlignAnalyzer` and `CalibrationAnalyzer` by ensuring all
  OpenCV `Mat` objects are released and coroutines are properly scoped.
- **Performance:** Optimized the CV pipeline by reusing the `cvMask` `Mat` object across frames in
  `VisionRepository`, reducing memory churn.
- **Stability:**
  - Hardened the Hilt provider for the ML Kit Object Detector in `AppModule.kt` to prevent crashes
    on initialization failure.
  - Corrected a potential `NullPointerException` in `GestureHandler` related to the magnifier.
- **Architecture:**
  - Purged the obsolete `HaterReducer.kt` and all related logic from the main state management
    pipeline.
  - Consolidated snapping logic by removing the redundant implementation from `CvReducer.kt`, making
    `SnapReducer.kt` the single source of truth.
  - Corrected a state management violation in `GestureHandler`, which now emits events instead of
    modifying state directly.
- **Code Health:**
  - Replaced hardcoded physics and animation values in `HaterViewModel.kt` with named constants.
  - Added a missing Proguard rule for the `GithubRelease` data class to prevent deserialization
    failures in release builds.
- **Documentation:**
  - Updated `01_Gestures.md` to accurately reflect that multi-touch gestures have a higher priority
    than single-finger object interactions.