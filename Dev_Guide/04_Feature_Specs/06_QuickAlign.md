# 4.6. Feature Specification: Quick Align

This document specifies the behavior of the "Four-Point Align" feature, which serves as the primary
method for manually aligning the virtual overlay with the real-world table.

## I. Purpose

The Quick Align feature provides a user-friendly, "good enough" alignment solution that works even
when a full, unobstructed view of the pool table is not possible. It calculates the necessary
pan, zoom, and rotation to match the virtual table to the real one from a single photograph.

## II. User Flow

1. The user initiates the feature from the "Too Advanced Options" dialog.
2. The `QuickAlignScreen` appears, displaying a live camera feed.
3. The user takes a single, wide-angle photo of the pool table.
4. The application then prompts the user to tap a series of four known points, in order (e.g.,
   "Tap the Top-Left Corner Pocket"). The UI will provide a small diagram of the virtual table for
   reference.
5. As the user taps, numbered markers appear on the photo to provide visual feedback. A "Reset
   Points" button is available.
6. After four points are selected, a "Finish" button becomes active.
7. Pressing "Finish" triggers the calculation and returns the user to the main screen.

## III. Implementation & Outcome

* **Logic**: The feature uses the four user-tapped screen-space coordinates and their
  corresponding known logical-space coordinates (e.g., the four corner pockets of the virtual
  table model). These four pairs of points are fed into OpenCV's `Calib3d.findHomography()`
  function.
* **Decomposition**: The resulting 3x3 homography matrix is decomposed to extract the effective
  translation, rotation, and scale.
* **State Application**: These values are used to update the main view's `viewOffset`,
  `worldRotationDegrees`, and `zoomSliderPosition` state properties.
* **Automatic Lock**: Upon successful alignment, the `isWorldLocked` state is automatically set to
  `true`, locking the newly aligned overlay in place for subsequent camera movement.

## IV. Limitations

* This feature calculates a **perspective transformation**, not a lens distortion correction. It
  will correctly align the four selected points but will not account for the non-linear curvature
  of straight lines caused by the camera lens.
* The alignment is only perfectly accurate from the single viewpoint where the photo was taken.