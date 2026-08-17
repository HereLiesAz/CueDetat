# 4.5. Feature Specification: CV Hybrid Eye

The application's vision system uses a hybrid, two-stage pipeline to achieve robust ball detection.

## The Two-Stage Pipeline

1. **Phase 1: ML Detection (The "Scout")**

* **Tool**: ML Kit's generic Object Detection.
* **Purpose**: To perform a fast, initial pass on the full camera frame and identify Regions of
  Interest (ROIs) via bounding boxes.

2. **Phase 2: OpenCV Refinement (The "Sniper")**

* **Tool**: OpenCV, via `CvBallDetector`.
* **Purpose**: For each ROI provided by the Scout, a more precise algorithm is run *only within that
  box*.
* **Algorithm**: There is no contour/Hough toggle. `CvBallDetector` runs a single fixed pipeline:
  1. Build a felt color mask from the sampled HSV mean/stdDev (`inRange`).
  2. Morphologically close the mask to seal ball-sized holes, then subtract the original mask —
     the surviving blobs are ball candidates.
  3. Run `connectedComponentsWithStats` on the candidate mask and filter blobs by area and
     circularity (`approxRadius / bboxRadius`).
  4. For each surviving blob, run `HoughCircles` in a local crop around the blob to refine the
     center and radius to sub-pixel precision, falling back to the blob's centroid/bounding-box
     radius if Hough finds nothing.
* **Dynamic Rangefinder**: The system calculates the expected on-screen pixel radius of a ball at
  the Y-coordinate of the Scout's bounding box. This provides the Sniper with a tight `minRadius`
  and `maxRadius`, reducing false positives.

## Color Calibration

* **Statistical Sampling**: To create the color mask for CV, the system samples a 5x5 patch of the
  felt, not a single pixel. It calculates the **mean and standard deviation** of the HSV values to
  create an adaptive mask resilient to lighting changes.

## Automatic World Lock

* When a user successfully snaps a virtual ball to a CV-detected real ball, the `isWorldLocked`
  flag is automatically set to `true`.
* This lock is automatically disengaged if the user begins dragging a ball or presses the "Reset
  View" button.

## AR Table Tracking & Setup

The AR setup pipeline strictly adheres to the **"ONE SINGLE USER INTERACTION" mandate** for the
*minimum* path to `AR_ACTIVE`: a single felt-color capture is enough to start tracking. The
four-step `ScanStep` wizard (`FELT_CAPTURE`, `CORNER_QUAD`, `POCKET_GUIDE`, `AUTO_READY`) is the
active table-scan flow, not a deprecated one — see `docs/CODE_MAP.md` and
`docs/02_Core_Components/01_Operational_Modes.md` for the full state machine.

### Felt Capture Pipeline

1. **Magnifying UI**: The setup screen presents a magnifying circle UI. 
2. **Single Interaction**: The user points at the felt and taps the "Capture" button.
3. **Immediate AR**: This single tap adds the captured HSV color to a persistent list of `FeltSample`s, and `captureFeltAndComplete()` immediately locks the color, loads a default table model, and advances the wizard into `CORNER_QUAD` (world-anchored corner-pocket capture) rather than transitioning straight to `AR_ACTIVE`.
4. **Multiple Samples**: While on the capture screen, the user can manage previously captured samples (move, delete). Order dictates the weight of influence in the tracking algorithm.

The application relies entirely on the user for fine-tuning the table geometry (rotation, zoom) via manual sliders after the AR session has started. MVI state advancement happens instantly upon capture.


### ARCore Tracking Loss

`ArTrackingLost` still exists as an event, but it is a deliberate no-op in `ControlReducer`
(see `ControlReducer.kt` — "The nuclear payload has been disarmed") and `ArCoreBackground`
(now in `:feature_expert_ar`) no longer even dispatches it: when tracking drops from `TRACKING`
to `PAUSED` it just logs a warning ("Tracking paused. Holding anchors.") and keeps rendering from
the last known matrix. This is intentional — brief ARCore tracking blips (a hand crossing the
lens, a quick pan) are common, and an earlier version that reset `tableScanModel`/`lensWarpTps`
and forced the user back to `AR_SETUP` on every blip was disruptive enough that it was scrapped
(see the regression test `ArFlowReducerTest.kt`, `` `ArTrackingLost preserves AR session state
(float on last known matrix)` ``). The app now floats on the last known table pose through
tracking pauses and only requires a fresh scan if the user explicitly restarts AR setup.

## Other CV Rules

* **Conditional Snapping:** Auto-snapping of logical balls only occurs if the user places a logical
  ball *in close proximity* to a detected object.
* **Mask Visualization:** A developer toggle exists to render the CV's internal color mask on-screen
  for tuning.
* **Calibration UI:** A dedicated UI workflow exists for calibrating the system to a specific
  table's felt color.