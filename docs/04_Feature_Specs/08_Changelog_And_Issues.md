# Changelog & Issues

This document tracks notable changes and known issues for the project.

The format is based on [Keep a Changelog](httpshttps://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.9.3] - 2026-03-25

### Added

- **AR state machine**: `CameraMode` enum expanded with `AR_SETUP`, `AR_ACTIVE`, and `CAMERA_ONLY` states. Tapping the AR button now enters `AR_SETUP` rather than going directly to AR active; the system auto-advances to `AR_ACTIVE` once the table overlay confidence reaches 0.8. Cancel returns to `CAMERA_ONLY`; Off returns to `OFF`.
- **AR setup wizard**: The `ArSetupPrompt` overlay now shows a three-step wizard (Lock Felt Color → Scan Table → Verify) with live PENDING/ACTIVE/DONE states, replacing the static instruction text.
- **AR nav rail buttons**: Cancel button appears in `AR_SETUP`; Off button appears in `CAMERA_ONLY`; the AR toggle is gated so it cannot be tapped while AR is already active.
- **`ArTrackingLost` event**: `ArCoreBackground` dispatches this event when ARCore tracking drops from `TRACKING` to `PAUSED`. The handler clears `tableScanModel` and `lensWarpTps` and returns to `AR_SETUP`.
- **`tableOverlayConfidence` field** in `VisionData`: CV pipeline can report 0–1 overlay confidence; `CvReducer` uses it to auto-advance `AR_SETUP → AR_ACTIVE`.

### Fixed

- **Dynamic beginner pan suppression**: Two-finger and single-finger pan events are now fully suppressed in dynamic beginner mode. Previously, the outer condition at the two-finger block allowed pan through in dynamic beginner; the single-finger world-locked block also leaked pan in this mode.
- **Static beginner ball draw order (z-order)**: Direction lines and triangles are now drawn in a dedicated pass *below* the ball circles. Text labels are drawn in a separate pass *above* balls. Previously all beginner foreground elements were drawn in a single pass after balls.
- **Beginner glow now uses `Blur.OUTER`**: The stationary outline glow in beginner-locked mode now uses outward-only blur, so the circle interior remains transparent. `createGlowPaint` accepts a `blurType` parameter (defaults to `Blur.NORMAL` for all other callers).
- **Bubble center dot**: A new hollow white circle (stroke, no fill) is drawn centered on the bubble in the beginner-locked view, making the bubble's current position visible when it drifts from the stationary anchor.

## [0.9.2] - 2026-03-22

### Changed

- Reverted MasseControl back to the side-view stick graphic tethered to the color wheel.
- Made the MasseControl widget relocatable around the screen exactly like the SpinControl widget by using the double-tap to drag gesture and updating its positioning in ProtractorScreen.

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

## Known Issues

*   **Interactive Tutorial is Blocking:** The current implementation of the interactive tutorial is a placeholder that blocks interaction with the UI elements it is supposed to be explaining. It needs to be replaced with a non-blocking overlay.
*   **Lens Distortion:** The "Quick Align" feature provides perspective correction but does not account for non-linear lens distortion, which can lead to inaccuracies away from the four alignment points. A full camera calibration is required to fix this.