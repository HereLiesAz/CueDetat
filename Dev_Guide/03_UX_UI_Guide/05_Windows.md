# 3.5. Dialogs and Overlays

* **Opacity:** All popup `AlertDialog` windows must have their backgrounds set to 66% opacity (
  `0.66f`).

## Specific Implementations

* **Luminance & Glow Stick Dialogs**:
* Must be `AlertDialog`s containing a horizontal `Slider` and a "Done" button.
* **Table Size Selection Dialog**:
* An `AlertDialog` that displays a list of `TextButton`s for each available table size. Selecting an
  option triggers `SetTableSize` and dismisses the dialog.
* **Advanced Options Dialog**:
* An `AlertDialog` containing toggles for the CV model and refinement method, plus four `Slider`
  controls for Hough/Canny parameters.
* **Table Color Calibration Overlay**:
* A full-screen overlay that instructs the user to aim at the table felt and tap a crosshair to
  sample the color.
* **Interactive Tutorial Overlay**:
* **Requirement:** Must be a non-blocking, transparent overlay that allows the user to interact with
  the UI elements being described. The current implementation is a blocking placeholder and needs to
  be replaced.
* It must have "Next" and "Finish" buttons.