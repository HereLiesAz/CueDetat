# Agent Instructions for the View/Renderer Layer

This document provides guidelines for working with the rendering pipeline of the application. The renderer classes are responsible for drawing the UI based on the `CueDetatState`.

## Perspective Transformation

The transformation from the 2D logical plane to the 3D screen view is a sacred and precise process. All transformation logic is handled in `UpdateStateUseCase.kt` and `Perspective.kt`. Renderers should not perform their own transformations.

### Mandates of Transformation

- **Order of Operations**: The final projection matrix (`pitchMatrix`) **must** be created by first applying 2D logical transformations (zoom) and *then* applying 3D camera transformations (rotation and pitch).
- **Decoupled Zoom**: Zoom is a 2D scaling of the logical plane. It must not affect the perspective distortion.
- **Unified Sizing**: The on-screen radius of a 3D "ghost" ball must remain constant regardless of its position on the table. Its size is a function of zoom and pitch only.
- **Rail Lift**: The `railLiftAmount` must be proportional to the sine of the pitch angle. This ensures the rails appear flush with the table at a 0° pitch.

### Rendering Pipeline

1.  All transformations are calculated in `UpdateStateUseCase` and stored in the `CueDetatState` as `Matrix` objects.
2.  Renderers receive the `CueDetatState` and the relevant `Paint` objects from `PaintCache`.
3.  Renderers use the matrices from the state to draw the UI elements on the `Canvas`. Renderers must not perform their own transformations. **Renderers must not allocate `Paint`, `Path`, `Matrix`, shaders or mask filters inside a draw call.** Themed paints live in `PaintCache`; a renderer may own a private `Paint`/`Path` field allocated once and reconfigured per use (via `set(base)` or `reset()`), as BallRenderer, LineRenderer, RecommendationRenderer and OverlayRenderer do.
4.  For line/rail glows use `createGlowPaint` (PaintUtils; cached in `PaintCache.glowPaints`). For ball halos use `drawGlowCircle` (cached RadialGradient). Treat returned glow paints as read-only.
5.  Lens warp: every renderer gets its TPS via `renderLensTps(state)` (TpsUtils) and line distortion arrays via `LensArrayCache`, so table, rails, balls, labels and lines are gated identically.
6.  The plane matrix used for a frame is OverlayRenderer's `matrixFor2DPlane` (pitch interpolated toward top-down). Anything drawn on the plane, balls included, uses that matrix.
