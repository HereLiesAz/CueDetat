# TODO List

This document tracks upcoming features, necessary refactoring, and other tasks for the Cue d'Etat
project.

## High Priority

- ~~Implement a non-blocking interactive tutorial overlay.~~ **Done (PR #249):** reimplemented on
  AzNavRail 10.18's status-driven guidance framework (see `Changelog.md`). Non-blocking spotlight,
  tap-to-advance, Finish affordance, framework-persisted completion.

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
- Optimize the `VisionAnalyzer`'s `ImageProxy.toBitmap()` conversion (a per-pixel Kotlin loop).
  It is already gated by `VisionRepository.shouldAcceptFrame`, and the unused
  `currentFrameBitmap` StateFlow it fed has been removed. Remaining: HATER mode and
  beginner-locked frames still pay for the conversion before `analyze(bitmap, ...)` discards
  it; skip it for those, or replace it with the OpenCV path (`ImageProxy.toMat`) that
  `VisionRepository` already uses.