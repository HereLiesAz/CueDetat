# Slider Implementation Gospel

This document outlines the non-negotiable rules for all sliders within the Cue D'état application. Deviation from this gospel is a mortal sin.

## 1. Functional Requirements

* **Style & Type:** All sliders (zoom, luminance, table rotation) **must** be implemented as "Material Design 3 Expressive centered sliders," small size, with value indicators. The custom `ExpressiveSlider` composable is to be used, not the standard Material component.
* **Default State:** All slider thumbs **must** be set to the center of their track as their default position upon initialization.
* **Value Indicators:** All sliders **must** display their current numerical value.

## 2. Specific Slider Implementations

### Zoom Slider
* **Orientation:** Vertical.
* **Range Calibration**: The slider's value range must be symmetrical around zero (e.g., -50f to 50f) to achieve the "centered" track appearance.
* The default value must be `0f`.
* The `ZoomMapping.kt` object must map this slider range to the defined `MIN_ZOOM` and `MAX_ZOOM` factors.
* The `ExpressiveSlider`'s value indicator must be permanently enabled.

### Table Rotation Slider
* **Orientation:** Horizontal.
* **Visibility:** It must only be visible when the table is visible (`uiState.showTable` is `true`).
* **Layout:** It must be located horizontally, centered, and positioned to leave adequate space for the FABs at the bottom of the screen.

### Luminance Slider
* **Orientation:** Horizontal.
* **Context:** Appears within the Luminance adjustment dialog.

## 3. Positioning and Sizing Mandates

* **Zoom Slider Height:** The vertical zoom slider's container height must be exactly **60%** of the total screen height.
* **Zoom Slider Width:** The vertical zoom slider's container width must be exactly **60%** of the total screen height.
* **Zoom Slider Position:** The vertical zoom slider must be positioned such that its **vertical and horizontal center** aligns perfectly with the **right edge of the screen**. This requires aligning the component to the screen's `CenterEnd` and then applying a negative horizontal offset equal to half of the component's own width. It is expected that this will cause half of the component to be rendered off-screen.