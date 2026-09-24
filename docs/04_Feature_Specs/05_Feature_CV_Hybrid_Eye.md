# 4.5. Feature Specification: CV Hybrid Eye

Ball detection is colour-based: a ball is a hole in the felt. It runs in `CvBallDetector` on the
full-resolution camera frame (a quarter-scale frame leaves a ball about two pixels across). The
trained TFLite model has no ball class (table, pocket and rail only), and ML Kit's generic
object detector, formerly the "scout" stage, never found balls and has been removed from the
path.

## Top-down detection (with a table pose)

Whenever the table pose is known (the sensor-driven virtual table, or an AR lock), the camera
frame is rectified onto the table plane (`TopDownBallDetector`): a top-down view at 8 px per
ball radius in which the table is a known rectangle and every ball the same size. Balls are
non-felt islands there. Because a ball stands off the felt, its image is stretched away from
the camera by `k = 1 / sin(elevation)` (read locally off the rectification); it stays one ball
wide across. `TopDownBallRules` (pure, tested) judges islands by width across and length along,
splits front/back and side-by-side pairs, and places each ball at its contact point: the near
end plus `R · tan(elevation / 2)`. The island pipeline below is the fallback without a pose.

## Table snap and pose memory

`TableFitter` searches the virtual table's pan, rotation and zoom for the outline that best
overlaps the felt in view (IoU), pulled toward the remembered orientation in proportion to its
trust. `TableSnapPolicy` sets three degrees: a dashed ghost of the fit (IoU ≥ 0.6), Lock snapping
to the fit (≥ 0.7), and a gentle drift toward it after 1.5 s without a touch (≥ 0.85).

Memory (`TablePoseStore`, `TablePosePrior`, `TableOrientationLearner`): the session's last
pose (fading over 10 min), every table's poses keyed by GPS (75 m), and the last table anywhere
at half trust. The learner predicts rotation from compass yaw (the table's heading is a constant
of the room; rotation = heading − yaw, modulo 180°) and zoom from pitch. When the camera comes
on, a prior trusted ≥ 0.3 seeds the table. Every Lock and every very good fit (IoU ≥ 0.9, at most
once a minute) is recorded, and appended to `table_pose_log.jsonl` (location coarsened to ~1 km)
for training a proper model later.

## The Pipeline (fallback, no pose)

1. **Felt mask**: `inRange` around the felt HSV mean ± spread (hue held tight; saturation and
   value loose, floor 25, so shadowed or dark felt still masks).
2. **Table region**: the playing surface projected into the frame from the table pose; without a
   pose, the convex hull of the largest felt area. Eroded slightly off the cushion line.
3. **Islands**: table region AND NOT felt, opened with a 3 px kernel to drop noise, then
   `connectedComponentsWithStats`.
4. **Judgement** (`BallIslandRules.judge`, pure and unit-tested): each island is compared with the
   ball radius expected at that spot. Area 0.35–1.8 of a ball's disk, aspect ≤ 1.5 and fill ≥ 0.5
   is one ball; roughly twice the area with aspect 1.5–2.6 is two touching balls, split along the
   long axis; anything else is rejected.
5. **Naming** (`BallIslandRules.classify`): from the shares of white and dark pixels inside the
   ball: mostly white is the cue; mostly dark with little white is the 8; a real share of white
   beside colour is a stripe; otherwise a solid. Rotation-free, because a stripe's band lies at
   any angle.

* **Expected radius**: with a table pose, the logical ball radius is projected into the frame at
  each island, so far balls are expected smaller. Without one, it is estimated from the visible
  surface area (`BallIslandRules.fallbackRadius`).
* **Frame to screen**: CameraX frames arrive in sensor orientation; `CameraViewMapping.fillCenter`
  applies the preview's rotation and centre-crop. ARCore frames use ARCore's own
  `Frame.transformCoordinates2d` mapping, since its CPU image is cropped differently from its
  on-screen feed.
* **Threading**: in AR, detection runs on a background worker; the GL thread only copies the
  image and releases it.

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
*minimum* path to `AR_ACTIVE`: a single felt-color capture is the whole table scan. Pocket
tapping (corner taps and the per-pocket guide) was removed.

### Felt Capture Pipeline

1. **Magnifying UI**: The setup screen presents a magnifying circle UI. 
2. **Single Interaction**: The user points at the felt and taps the "Capture" button.
3. **Immediate AR**: This single tap adds the captured HSV color to a persistent list of `FeltSample`s, and `captureFeltAndComplete()` immediately locks the color, loads a default table model, and completes the scan, handing off to `AR_ACTIVE`.
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