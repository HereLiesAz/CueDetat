# Changelog & Issues

This document tracks notable changes, known issues, and the future development roadmap for the project.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.9.1] - 2025-08-21

### Fixed

- Corrected the transformation pipeline to ensure all 2D transformations (zoom, rotation, placement) are applied *before* 3D perspective transformations (pitch/tilt). This resolves a rendering issue that caused improper "roll" when the view was tilted.

## [0.9.0] - 2025-07-30

### Changed

- Overhauled the physics simulation in Hater Mode, replacing the previous engine with Google's
  LiquidFun for improved stability and performance.
- Adjusted the size of the Hater Mode triangle to better contain its text, and then reduced it to a
  more reasonable size.

### Fixed

- Resolved a critical bug that prevented the main menu and navigation rail from opening correctly by
  fixing the underlying state management logic.
- Eliminated violent vibrations and erratic behavior of the triangle in Hater Mode by implementing a
  stable, synchronous physics update loop.

### Removed

- Removed the `kphysics` library dependency, which was the source of the physics instability.

---

## Current Project Status

### Known Issues

*   **Interactive Tutorial is Blocking:** The current implementation of the interactive tutorial is a placeholder that blocks interaction with the UI elements it is supposed to be explaining. It needs to be replaced with a non-blocking overlay as specified in `03_UX_UI_Guide/05_Windows.md`.
*   **Lens Distortion:** The "Quick Align" feature provides perspective correction but does not account for non-linear lens distortion, which can lead to inaccuracies away from the four alignment points. A full camera calibration is required to fix this.

### Planned Features

*   **Spin-Induced Throw:** Integrate spin-induced throw into the `CalculateBankShot` use case. The current model uses pure reflection and does not account for english.
*   **Camera Calibration Database (D.D.D.D.):** Develop the backend and submission logic for the "Dimensional Distortion Device Database" to collect anonymous camera calibration data.
*   **Automatic Calibration Profiles:** Implement automatic selection of camera calibration profiles based on the user's device model once the D.D.D.D. is populated.
*   **User Feedback Toasts:** Add user-facing toast messages for events like failed camera calibration or successful data submission.

### Refactoring & Code Health Roadmap

*   **State Management Refinement:** Refactor `CueDetatState` to separate core, user-configurable state from derived/transient state. This will reduce state bloat, improve serialization safety, and clarify the single source of truth.
*   **Render Loop Optimization:** Optimize the rendering loop by caching `Paint` objects for glow effects in `PaintCache` instead of creating new instances on every frame, reducing GC pressure.
*   **Proguard Rule Refinement:** Refine Proguard rules to be more specific (e.g., using `@Keep` annotations on model classes) instead of using broad wildcard rules to reduce final APK size.
*   **VisionAnalyzer Optimization:** The `VisionAnalyzer`'s `ImageProxy.toBitmap()` conversion is computationally expensive and should be made conditional or replaced with a more efficient method.
