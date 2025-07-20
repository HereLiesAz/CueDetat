# 3.3. Visual Style and Theming

This document specifies the application's visual identity.

## Core Style

* **Visuals:** Monochromatic, with a minimalistic aesthetic.
* **Theme:** Dark mode is the default.

## Theme Toggle

* A menu option must exist to toggle between dark, light, and system-default themes.
* The `isForceLightMode: Boolean?` property in `OverlayState` controls this: `true` for light,
  `false` for dark, `null` for system.

## Glow Effect

* A soft, feathered "glow" effect must be applied to key stroked elements.
* This is achieved in `PaintCache` by creating dedicated `Paint` objects for glows that use a
  `BlurMaskFilter`.
* **Line Glows:** Use `Paint.Style.FILL_AND_STROKE`.
* **Ball Glows:** Use `Paint.Style.STROKE`.
* Glows must be drawn *before* the primary element to create a halo effect.

## Stroke & Opacity Rules

* **Stroke Width**: The base stroke width for primary lines and ball outlines is `6f`. The glow
  stroke width is `12f`.
* **Opacity**:
    * Menu backgrounds: 80% (`0.8f`).
    * Popup dialogs: 66% (`0.66f`).

## Spin Control Color Wheel

* **Description:** A circular UI element for selecting cue ball spin.
* **Gradient:** The wheel must be a radial color gradient.
* **Color Alignment**:
    * **Top (270°):** Yellow (Topspin)
    * **Right (0°):** Red (Right English)
    * **Bottom (90°):** Blue (Draw/Backspin)
    * **Left (180°):** Green (Left English)
* **Interaction:** A white circular indicator must show the precise point of selection.