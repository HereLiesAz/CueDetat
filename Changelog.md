# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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