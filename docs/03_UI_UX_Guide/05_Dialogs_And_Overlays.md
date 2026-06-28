# 3.5. Dialogs and Overlays

* **Opacity:** All popup `AlertDialog` windows must have their backgrounds set to 66% opacity (
  `0.66f`).

## Specific Implementations

* **Luminance & Glow Stick Dialogs**:
  * Must be `AlertDialog`s containing a horizontal `Slider` and a "Done" button.
* **Table Size Selection Dialog**:
  * An `AlertDialog` that displays a list of `TextButton`s for each available table size.
    Selecting an option triggers `SetTableSize` and dismisses the dialog.
* **Advanced Options Dialog**:
  * An `AlertDialog` containing toggles for the CV model and refinement method, plus four `Slider`
    controls for Hough/Canny parameters. It also provides entry points for the "Quick Align" and
    "Camera Calibration" features. There is no "Scan Table" button in this dialog; the table scan is
    accessed exclusively via the Felt rail item.
  * **Spin/Massé Color Wheel**: The default location must be centered on the screen within its
    row.

* **Table Color Calibration Overlay**:
  * A full-screen overlay that instructs the user to aim at the table felt and tap a crosshair to
    sample the color.
* **Interactive Tutorial Overlay** (AzNavRail 10.18 guidance):
  * Implemented via AzNavRail 10.18's status-driven guidance framework — `azStatus`/`azEdge`/`azGoal`/
    `azGuidanceTarget` in `AzNavRailMenu`, with `TutorialGuidanceTargets.kt` mapping app state to the
    spotlighted in-camera elements (target/ghost/cue ball, aiming line, zoom slider). The framework
    draws a non-blocking dim + spotlight; step text is reused verbatim from the `tutorial_*` string-arrays.
  * Steps advance on tap ("tap to continue"); a bottom **Finish** affordance completes the active goal
    (`controller.markReached`), persisted by the framework (`az_navrail_completed_goals`).
* **Quick Align Screen**:
  * A full-screen overlay for the "Four-Point Align" feature. It displays the camera feed and
    instructs the user to tap four known points on the table.
* **Camera Calibration Screen**:
  * A full-screen developer tool for performing a full camera calibration using an on-screen
    pattern.
* **Table Scan Screen** (`TableScanScreen`):
  * An **inline overlay** composable rendered inside `ProtractorScreen`, shown automatically when
    AR is initialized.
  * **UI Elements**:
    * **Magnifying Circle**: Center-screen magnifying glass pointed at the felt.
    * **Capture Button**: Styled like a camera app's capture button.
    * **Color Sample Grid**: Displayed at the top of the screen.
      * Samples must not overlap NavRail or sliders; can occupy multiple rows.
      * Tap to select.
      * Tap and hold for multi-selection mode.
      * Tap and hold then drag for area selection.
      * Selection Menu: **"Move"** (reorder for influence weighting - top-left is highest) and
        **"Delete"**.
  * **Functionality**: Capturing a color sample is the ONLY required user task for AR setup. All
    samples are saved persistently and remain for the life of the app installation. The system fits
    a table model without requiring the entire table to be in view.

* **AR Tracking Badge** (`ArTrackingBadge`):
  * A small pulsing indicator shown in `AR_ACTIVE` state to signal that ARCore is tracking.
  * When tracking drops from `TRACKING` to `PAUSED`, `ArCoreBackground` dispatches `ArTrackingLost`,
    which clears the scan model and returns to `AR_SETUP`.