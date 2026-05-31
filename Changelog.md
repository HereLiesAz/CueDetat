# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed — Billing

- **Expert Mode is now a one-time purchase instead of a subscription.** New non-consumable
  in-app product `expert_mode_unlock` (INAPP), no base plans, no free trial, no auto-renew.
  Once purchased it is owned permanently and never re-verified away offline (the 14-day
  offline cap was removed). The paywall shows a single price with an "Unlock Expert Mode"
  button. Price remains configured in Play Console only. No data migration — the prior
  subscription had no purchasers.

### Changed — Battery & processing efficiency

- **Adaptive CV pipeline throttle.** The main vision/ML pass (`VisionRepository.processImage`)
  used to run on every camera frame (~30fps). It now runs at ~30fps only while the scene is
  moving and widens to ~12fps once detections are stable, gated by an allocation-free time
  check in `VisionAnalyzer` so throttled frames skip the per-pixel YUV→RGB conversion entirely.
- **Adaptive sensor rates.** The rotation-vector sensor registers at `SENSOR_DELAY_UI` (~15Hz)
  and only bumps to `SENSOR_DELAY_GAME` while the phone is being moved; the shake accelerometer
  dropped to `SENSOR_DELAY_UI`. Previously both ran at ~50Hz continuously.
- **Hater-mode physics** stepped at 60fps unconditionally; now 30fps with an idle-pause that
  stops stepping once the die settles and resumes on the next input.
- **Per-frame allocations removed.** Pooled the downscaled ML bitmap in `VisionRepository`;
  hoisted per-frame `Paint`/`Path` allocations out of the `BallRenderer`/`LineRenderer` draw
  loops into reused fields; LRU-bounded the glow-paint cache.
- **TFLite** CPU inference now scales threads to the device (was hardcoded to 2). A GPU/NNAPI
  delegate path was added but is gated off pending on-device validation (thread-affinity and
  in-graph-NMS constraints).
- **Entitlement re-verification** on app resume is TTL-coalesced and the redundant second
  foreground trigger was removed; table-scan location prefers the cached fix and time-bounds
  any active GPS request.

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