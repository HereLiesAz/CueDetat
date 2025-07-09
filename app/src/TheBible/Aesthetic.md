# 07: Aesthetic Mandates

This document details the required visual style of the application.
## Core Style
* **Visuals:** The default aesthetic is monochromatic, with a minimalistic but emotionally charged composition.
* **Theme:** Dark mode is the default state.
## Theme Toggle Implementation
* A text-only menu option must exist to toggle the color scheme.
* When the app is in its default dark mode, the button text must read "Walk toward the Light".
* When in light mode, the button text must read "Embrace the darkness".
* This button must dispatch the `ToggleForceTheme` event. The `StateReducer` will invert the `isForceLightMode` boolean in the state.
* `PaintCache.kt` must use this flag to correctly select between light and dark color palettes for all rendered elements, ensuring the theme change is visually applied.
## Stroke and Line Width
* The `strokeWidth` for all rendered balls and lines in `PaintCache.kt` should be increased for better visibility and a more substantial feel.
* A starting value of `3f` should be implemented.

## Glow Effect
* A "glow" effect must be applied to key stroked elements for emphasis.
* This is achieved in `PaintCache` by creating a secondary set of `Paint` objects.
* These "glow" paints should have a significantly thicker `strokeWidth` (e.g., `10f`) and a `BlurMaskFilter`.
* The `OverlayRenderer` must draw the glow paint object *before* drawing the primary paint object to create a halo effect.
* A contrasting glow should be implemented: a dark glow for light primary strokes, and a light glow for dark primary strokes.
## Opacity
* The menu background must be set to 80% opacity (`0.8f`).
* All popup dialog windows must have their backgrounds set to 66% opacity (`0.66f`).