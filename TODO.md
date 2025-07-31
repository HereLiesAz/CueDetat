# TODO List

This document tracks upcoming features, necessary refactoring, and other tasks for the Cue d'Etat
project.

## High Priority

- Implement a non-blocking interactive tutorial overlay as specified in
  `03_UX_UI_Guide/05_Windows.md`. The current implementation is a blocking placeholder and prevents
  user interaction.

## Features

- Integrate spin-induced throw into the `CalculateBankShot` use case. The current model uses pure
  reflection and does not account for english.
- Develop the backend and submission logic for the "Dimensional Distortion Device Database" (
  D.D.D.D.) to collect anonymous camera calibration data.
- Implement automatic selection of camera calibration profiles based on the user's device model once
  the D.D.D.D. is populated.

## UI/UX Enhancements

- Add user-facing toast messages for events like failed camera calibration or successful data
  submission to provide better feedback.

## Refactoring & Code Health

- Refactor `CueDetatState` to separate core, user-configurable state from derived/transient state.
  This will reduce state bloat, improve serialization safety, and clarify the single source of
  truth.
- Optimize the rendering loop by caching `Paint` objects for glow effects in `PaintCache` instead of
  creating new instances on every frame, reducing GC pressure.
- Refine Proguard rules to be more specific (e.g., using `@Keep` annotations on model classes)
  instead of using broad wildcard rules, which will help reduce final APK size.
- Optimize the `VisionAnalyzer`'s `ImageProxy.toBitmap()` conversion. It is computationally
  expensive and should be made conditional or replaced with a more efficient method.