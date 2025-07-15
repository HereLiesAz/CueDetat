# Slider Implementation Gospel

This document outlines the non-negotiable rules for all sliders within the Cue D'état application. Deviation from this gospel is a mortal sin.

## 1. Functional Requirements

* **Style & Type:** All sliders (zoom, luminance, table rotation) **must** be implemented using standard Material Design 3 components.
* **Default State:** All slider thumbs **must** be set to the center of their track as their default position upon initialization.
* **Value Indicators:** Sliders should use the default thumb provided by `SliderDefaults.Thumb`.

## 2. Specific Slider Implementations

### Zoom Slider
* **Orientation:** Vertical. This is achieved by programmatically rotating a standard horizontal `Slider` using a custom `.vertical()` layout modifier.
* **Functionality**: The slider must be continuous, without any steps or snapping behavior. Its touch target must be wide enough for easy interaction.
* **Value Indicator**: The slider must have a text label above it indicating the calculated distance to the primary target (Target Ball or Banking Ball), formatted according to the selected unit system.
* **Layout:**
    * The slider's container must be constrained to 60% of the screen's total height.
    * It must be aligned to the right side of the screen with a standard padding.

### Table Rotation Slider
* **Orientation:** Horizontal.
* **Visibility:** It must only be visible when the table is visible (`uiState.showTable` is `true`).
* **Functionality:** It provides an absolute method of rotating the table, coexisting with the two-finger rotation gesture.
* **Layout:** It must be located horizontally, centered, and positioned to leave adequate space for the FABs at the bottom of the screen.

### Luminance Slider
* **Orientation:** Horizontal.
* **Context:** Appears within the Luminance adjustment dialog.

***
## Addendum: The Sanctity of Screen-Space

The touchable area of a slider cannot be proportional to a logical object on the `Canvas`. The balls exist in a logical plane; the sliders exist in the physical layout of the screen, measured in `dp`. These are two separate realities with two separate sets of laws.

To honor the Creator's command for a larger, more forgiving touch target, the `MainScreen.kt` layout has been modified. The `width` of the vertical zoom slider's container and the `height` of the horizontal rotation slider's container have been **statically increased**. This enlarges their hitboxes on the screen without violating the architectural separation of the two realms.