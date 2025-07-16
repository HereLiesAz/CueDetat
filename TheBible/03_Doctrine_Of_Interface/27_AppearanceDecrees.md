# 24: The Appearance Decrees

This document establishes the Appearance Decree Layer, a system designed for granular control over the aesthetic properties of every rendered element. It supersedes the general settings in `PaintCache` by providing a specific, per-element configuration object.

## Doctrine of Configuration

* **Granularity**: Every distinct visual component (e.g., `ActualCueBall`, `AimingLine`, `Table`) must have its own corresponding data class in the `app/src/main/java/com/hereliesaz/cuedetat/view/config/` package.
* **Hierarchy**: These configuration classes inherit from base interfaces (`VisualProperties`, `BallsConfig`, `LinesConfig`, etc.) to ensure a consistent set of controllable properties.
* **Mandate of Enforcement**: During the rendering process, all renderers (`BallRenderer`, `LineRenderer`, etc.) **must** first consult the specific appearance decree for an element. The properties defined within this object (e.g., `ActualCueBall.strokeColor`) override any default values set in `PaintCache`. This is not optional. A renderer that does not obey its specific config object is a heretical renderer.

## Decrees of Appearance

* **Ball Strokes**: The stroke of a rendered ball **must** be drawn on the outside of its logical radius. This is achieved by drawing the stroke with a radius of `logicalRadius + (strokeWidth / 2)`.
* **Glow Effect**: A "glow" effect must be applied to key stroked elements. This is achieved in `PaintCache` by creating two distinct sets of `Paint` objects for glows. **Line Glows** use `Paint.Style.FILL_AND_STROKE`. **Ball Glows** use `Paint.Style.STROKE`.
* **Stroke & Glow Widths**: The base stroke width shall be `6f`. The base glow width shall be `12f`.
* **Opacity**: Menu backgrounds must be 80% opaque (`0.8f`). Popup dialogs must be 66% opaque (`0.66f`).
* **Spin Control Color Wheel**: The wheel must be a radial gradient. As of the final correction, its colors **must** be aligned thusly: Yellow at the top (270°/`Topspin`), Red at the right (0°/`Right English`), Blue at the bottom (90°/`Draw`), and Green at the left (180°/`Left English`).