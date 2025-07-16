# Aesthetic Mandates

This document details the required visual style of the application.
## Core Style
* **Visuals:** The default aesthetic is monochromatic, with a minimalistic but emotionally charged composition.
* **Theme:** Dark mode is the default state.
* **Menu Text:** All text within the navigation menu must use the primary theme color (`MaterialTheme.colorScheme.primary`) to maintain brand consistency.
## Theme Toggle Implementation
* A text-only menu option must exist to toggle the color scheme.
* When the app is in its default dark mode, the button text must read "Walk toward the Light".
* When in light mode, the button text must read "Embrace the darkness".
* This button must dispatch the `ToggleForceTheme` event. The `StateReducer` will invert the `isForceLightMode` boolean in the state.
* `PaintCache.kt` must use this flag to correctly select between light and dark color palettes for all rendered elements.
## Glow Effect
* A "glow" effect must be applied to key stroked elements for emphasis.
* This is achieved in `PaintCache` by creating two distinct sets of `Paint` objects for glows.
* **Line Glows:** To give lines a volumetric feel, their glow paint uses `Paint.Style.FILL_AND_STROKE`.
* **Ball Glows:** To give balls a simple aura, their glow paint uses `Paint.Style.STROKE`.
* These glow paints use a `BlurMaskFilter` and should be drawn *before* the primary paint object to create a halo effect.
## Stroke & Glow Decrees
* The base stroke width for all primary lines and outlines (balls, rails, aiming lines) shall be `6f`. The `PaintCache` must reflect this.
* The base glow width shall be `12f` to provide a subtle but noticeable aura without being overpowering.
## Opacity
* The menu background must be set to 80% opacity (`0.8f`).
* All popup dialog windows must have their backgrounds set to 66% opacity (`0.66f`).
## Spin Control Color Wheel
* **Description:** A circular UI element for selecting cue ball spin (English).
* **Gradient:** The wheel must be a radial gradient that transitions through the full spectrum as commanded: White (center) -> Yellow -> Red -> Violet -> Blue -> Green (edge).
* **Interaction:** The user drags their finger on the wheel to select a point. A white circular indicator must show the precise point of selection.