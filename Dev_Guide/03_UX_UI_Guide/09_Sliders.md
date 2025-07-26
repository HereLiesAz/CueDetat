# 3.9. Slider Specifications

This document defines the rules for all sliders in the application.

## 1. General Requirements

* **Component:** All sliders must be implemented using standard Material Design 3 `Slider`
  components.
* **Default State:** All slider thumbs must default to the center of their track on initialization,
  unless a different default is specified for a particular mode (e.g., Beginner Mode).
* **Styling:** Slider thumbs and active tracks must use the `primary` color from the MaterialTheme.

## 2. Specific Slider Implementations

### Zoom Slider

* **Orientation:** Vertical (achieved by programmatically rotating a horizontal `Slider`).
* **Functionality**: A continuous slider with no steps.
* **Dynamic Range**: The effective zoom range (`minZoom` and `maxZoom`) of the slider is **not
  static**. It is determined by the `ZoomMapping.getZoomRange()` function. This function provides an
  expanded zoom range **only** when in the locked protractor sub-mode (
  `isBeginnerViewLocked == true`). All other modes and sub-modes use the standard, smaller zoom
  range.
* **Layout:**
* The slider's container is constrained to 60% of the screen's height.
* It is aligned to the right side of the screen.

### Table Rotation Slider

* **Orientation:** Horizontal.
* **Visibility:** Only visible when the table is visible (`uiState.table.isVisible` is `true`). This
  slider is not available in Beginner Mode.
* **Layout:** Centered horizontally at the bottom of the screen, leaving space for action buttons.

### Other Sliders

* **Luminance, Glow, CV Parameter Sliders:** All are standard horizontal sliders located within
  their respective `AlertDialog`s.