# 3.9. Slider Specifications

This document defines the rules for all sliders in the application.

## 1. General Requirements

* **Component:** All sliders must be implemented using standard Material Design 3 `Slider`
  components.
* **Default State:** All slider thumbs must default to the center of their track on initialization.
* **Styling:** Slider thumbs and active tracks must use the `primary` color from the MaterialTheme.

## 2. Specific Slider Implementations

### Zoom Slider

* **Orientation:** Vertical (achieved by programmatically rotating a horizontal `Slider`).
* **Functionality**: A continuous slider with no steps.
* **Layout:**
* The slider's container is constrained to 60% of the screen's height.
* It is aligned to the right side of the screen.

### Table Rotation Slider

* **Orientation:** Horizontal.
* **Visibility:** Only visible when the table is visible (`uiState.table.isVisible` is `true`).
* **Layout:** Centered horizontally at the bottom of the screen, leaving space for action buttons.

### Other Sliders

* **Luminance, Glow, CV Parameter Sliders:** All are standard horizontal sliders located within
  their respective `AlertDialog`s.