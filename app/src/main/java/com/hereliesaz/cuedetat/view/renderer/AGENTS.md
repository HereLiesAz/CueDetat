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
3.  Renderers use the matrices from the state to draw the UI elements on the `Canvas`. **Renderers must not create their own `Paint` objects or perform their own transformations.**
4.  For glow effects, use the `getGlowPaint` method from `PaintCache`. Do not create new `Paint` objects for glows.
