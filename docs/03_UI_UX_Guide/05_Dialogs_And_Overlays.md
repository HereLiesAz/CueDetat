# 3.5. Dialogs and Overlays

* **Opacity:** All popup `AlertDialog` windows must have their backgrounds set to 66% opacity (
  `0.66f`).

## Specific Implementations

* **Luminance & Glow Stick Dialogs**:
  * Must be `AlertDialog`s containing a horizontal `Slider` and a "Done" button. Opened by the
    drawer items **Brightness** and **Glow stick**.
* **Table Size Selection Dialog**:
  * An `AlertDialog` that displays a list of `TextButton`s for each available table size.
    Selecting an option triggers `SetTableSize` and dismisses the dialog.
* **Advanced Options Dialog**:
  * An `AlertDialog` with one switch: show what the CV sees (the mask). The Canny sliders and the
    auto-tune button were removed because nothing read their values. There are no entry points for
    "Quick Align" or "Camera Calibration" (neither has UI in the app today) and no "Scan Table"
    button; the table scan is the **scan table** rail item.
  * **Spin/Massé Color Wheel**: The default location must be centered on the screen within its
    row.

* **Table Color Calibration Overlay**:
  * A full-screen overlay that instructs the user to aim at the table felt and tap a crosshair to
    sample the color.
* **Interactive Tutorial Overlay** (AzNavRail guidance framework, version 11.50 in `libs.versions.toml`):
  * Implemented via AzNavRail 10.18's status-driven guidance framework — `azStatus`/`azEdge`/`azGoal`/
    `azGuidanceTarget` in `AzNavRailMenu`, with `TutorialGuidanceTargets.kt` mapping app state to the
    spotlighted in-camera elements (target/ghost/cue ball, aiming line, zoom slider). The framework
    draws a non-blocking dim + spotlight; step text is reused verbatim from the `tutorial_*` string-arrays.
  * Steps advance on tap ("tap to continue"); a bottom **Finish** affordance completes the active goal
    (`controller.markReached`), persisted by the framework (`az_navrail_completed_goals`).
* **Quick Align Screen** and **Camera Calibration Screen**: specified in
  `04_Feature_Specs/06_Feature_Quick_Align.md` and `07_Feature_Camera_Calibration.md`, but there is
  no UI for either in the app today.
* **Table Scan Screen** (`TableScanScreen`):
  * An **inline overlay** composable rendered inside `ProtractorScreen`, shown automatically when
    AR is initialized.
  * **UI Elements**:
    * **Magnifying Circle**: Center-screen magnifying glass pointed at the felt.
    * **Capture Button**: Styled like a camera app's capture button.
    * **Color Sample Grid**: Displayed at the top of the screen.
      * Samples must not overlap NavRail or sliders; can occupy multiple rows.
      * Tap to select or deselect a sample; any number may be selected.
      * Selection Menu: **"Delete"**. (Hold-to-select, area selection and "Move" / reordering for
        influence weighting are not implemented.)
      * The samples are stored, but only the latest capture feeds the table colour; the gallery does
        not yet weight or choose between them.
  * **Functionality**: Capturing a color sample is the ONLY required user task for AR setup. All
    samples are saved persistently and remain for the life of the app installation. The system fits
    a table model without requiring the entire table to be in view.

* **AR Tracking Badge** (`ArTrackingBadge`):
  * A small pulsing indicator shown in `AR_ACTIVE` only while ARCore is delivering a live projection
    (`arTableMatrix != null`): not on a device without ARCore, and not while tracking is lost.
  * When tracking drops from `TRACKING` to `PAUSED`, `ArCoreBackground` does **not** clear the scan
    model or bounce the user back to `AR_SETUP`. It logs a warning and holds the last known
    anchors/matrix, letting ARCore recover on its own, and shows a persistent notice if the loss lasts.