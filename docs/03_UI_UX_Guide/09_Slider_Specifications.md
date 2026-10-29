# 3.9. Slider Specifications

This document defines the rules for all sliders in the application.

## 1. General Requirements

* **Component:** All sliders must be implemented using standard Material Design 3 `Slider`
  components.
* **Default State:** Slider thumbs default to the center of their track, except the zoom slider,
  which opens at `DEFAULT_ZOOM_SLIDER` (-8) in Expert and at 0 in Beginner (`ToggleReducer`).
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
* **Under ARCore world tracking** (`ArProjectionPolicy.isWorldTracked`) it is a self-centring *jog*:
  zoom there would move the table along the camera line, so an absolute position means nothing.
  Dragging nudges and the thumb springs back to centre on release; the +/- buttons nudge one unit.
  Pinching resizes the table in place instead (`TableScaleChanged`).
* **Layout:**
* The slider's container is constrained to 60% of the screen's height.
* It is aligned to the right side of the screen.
* A second vertical slider below it sets the table's height (`tableZOffset`, -50..50); the three-
  finger vertical drag drives the same value.

### Table Rotation Slider

* **Orientation:** Horizontal.
* **Visibility:** Only visible when the table is visible (`uiState.table.isVisible` is `true`). This
  slider is not available in Beginner Mode.
* **Layout:** A horizontal swiper (not a centred slider), left-aligned at the bottom of the screen above the navigation bar, `ROTATION_SWIPER_WIDTH` (367 dp) wide so it clears the Lock View button. Dragging right turns the table the way the finger moves.

### Other Sliders

* **Luminance, Glow, CV Parameter Sliders:** All are standard horizontal sliders located within
  their respective `AlertDialog`s.