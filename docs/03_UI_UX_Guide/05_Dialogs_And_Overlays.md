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
* **Table Color Calibration Overlay**:
  * A full-screen overlay that instructs the user to aim at the table felt and tap a crosshair to
    sample the color.
* **Interactive Tutorial Overlay**:
  * **Requirement:** Must be a non-blocking, transparent overlay that allows the user to interact
    with
    the UI elements being described. The current implementation is a blocking placeholder and needs
    to
    be replaced.
  * It must have "Next" and "Finish" buttons.
* **Quick Align Screen**:
  * A full-screen overlay for the "Four-Point Align" feature. It displays the camera feed and
    instructs the user to tap four known points on the table.
* **Camera Calibration Screen**:
  * A full-screen developer tool for performing a full camera calibration using an on-screen
    pattern.
* **Table Scan Screen** (`TableScanScreen`):
  * An **inline overlay** composable rendered inside `ProtractorScreen`, shown when
    `uiState.showTableScanScreen` is `true`. It is not a navigated route; `ROUTE_SCAN` does not
    exist in the `NavHost`. Displays the live camera feed and instructs the user to tap each of the
    six pocket positions. Includes `LaunchedEffect(Unit) { viewModel.resetScan() }` to reset state
    on entry. Does not request GPS permission and does not show a Cancel button.
    `TableScanAnalyzer` accumulates taps into `PocketCluster`s; `TableGeometryFitter` fits a 2:1
    geometry and computes a homography plus a residual TPS warp for lens-distortion correction. On
    success the resulting `TableScanModel` is persisted via `TableScanRepository`. Pocket-detection
    auto-complete does not occur in `onFrame()`; only an explicit `captureFeltAndComplete()` call
    can complete the scan.
* **AR Tracking Badge** (`ArTrackingBadge`):
  * A small pulsing indicator shown in `AR_ACTIVE` state to signal that ARCore is tracking.
  * When tracking drops from `TRACKING` to `PAUSED`, `ArCoreBackground` dispatches `ArTrackingLost`,
    which clears the scan model and returns to `AR_SETUP`.