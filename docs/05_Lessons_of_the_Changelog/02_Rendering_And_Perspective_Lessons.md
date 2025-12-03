# 5.2. Historical Lessons: Rendering & Perspective

This document archives critical bug fixes related to the 3D perspective transformation and rendering
pipeline. These issues were complex, interdependent, and serve as a crucial guide for any future
work on the visual layer.

### Case Study Trilogy: The Warped World

This series of bugs stemmed from a fundamental misunderstanding of how to combine 2D and 3D matrix
transformations.

1. **The Warped Lens:** The zoom slider caused the view to tilt instead of zoom.
2. **The Warped World:** The table "rolled" like a coin instead of spinning on its central axis.
   Fixing this caused a new bug where balls resized incorrectly on tilt.
3. **The Twice-Spun World:** After fixing the above, the table and rails rotated at a different
   speed than the balls and lines, creating a visual "slip."

* **Root Cause:** A series of architectural and mathematical errors:
    * **Incorrect Order:** 2D rotation was being applied *before* the 3D tilt, causing the "roll"
      effect.
    * **Mixed Realities:** The `zoomFactor` was incorrectly being applied to the `Camera`'s Z-axis (
      a 3D operation) instead of as a 2D scaling of the logical plane.
    * **Stateful Model:** The `Table.kt` data class was pre-calculating its own rotated coordinates,
      while the `UpdateStateUseCase` was *also* applying a world rotation. The table was being
      rotated twice.
    * **Flawed Measurements:** The on-screen size of objects was being calculated from a
      `flatMatrix` (a matrix without tilt), which became incorrect once rotation was properly
      handled in 3D space.

* **The Doctrine (Lesson Learned):** There is only one correct way to handle the perspective
  transformation in this architecture:
    1. **Single Source of Rotational Truth:** All rotational logic must be removed from data models
       like `Table.kt`. They must only store their pure, un-rotated geometry.
    2. **Correct 3D Transformation Order:** All spatial transformations must be handled within the
       3D `Camera` object in the correct sequence: **1. Rotate** (spin), **2. Tilt** (pitch), **3.
       Translate** (lift).
    3. **Separation of Concerns:** 2D `zoom` is a `postScale` operation on the `worldMatrix`. 3D
       `rotation` and `pitch` are `rotateY` and `rotateX` operations on the `perspectiveMatrix`.
       They must not be conflated.
    4. **Unified Measurement:** The on-screen size of any logical object must be calculated by
       projecting two of its points (e.g., center and edge) through the single, final `pitchMatrix`.

### Case Study: The Crimson Blight & The Waning Spheres

* **Issue:** A warning color (red) would "stick" and be applied to all glows globally. Concurrently,
  balls would appear to shrink when the phone was tilted.
* **Cause:**
    * **Color:** A shared `Paint` object in `PaintCache` was being mutated for a temporary warning
      state and not reset, tainting it for all subsequent draw calls.
    * **Sizing:** The on-screen radius of balls was being calculated by measuring a single,
      projected horizontal vector. This measurement shrinks due to perspective foreshortening when
      the view is tilted.
* **Lesson Learned:**
    * Never mutate shared `Paint` objects for temporary states. Create a new, temporary paint
      instance instead.
    * The true, perspective-correct radius of a circle must be calculated by projecting its logical
      center and an edge point to screen space and measuring the distance between them. This is now
      handled by `DrawingUtils.getPerspectiveRadiusAndLift`.