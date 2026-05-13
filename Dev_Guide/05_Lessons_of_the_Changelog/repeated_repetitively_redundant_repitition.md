# 5.2. Historical Lessons: Rendering & Perspective

This document archives critical bug fixes related to the 3D perspective transformation and rendering
pipeline. These issues were complex, interdependent, and serve as a crucial guide for any future
work on the visual layer.

### Case Study: The Warped World Transformations

This series of bugs stemmed from a fundamental misunderstanding of how to combine 2D and 3D matrix
transformations.

1.  **Symptom 1:** The zoom slider caused the view to tilt instead of zoom.
2.  **Symptom 2:** The table "rolled" like a coin instead of spinning on its central axis.
   Fixing this caused a new bug where balls resized incorrectly on tilt.
3.  **Symptom 3:** After fixing the above, the table and rails rotated at a different
   speed than the balls and lines, creating a visual "slip."

* **Root Cause:** A series of architectural and mathematical errors:
    * **Incorrect Order:** 2D rotation was being applied *before* the 3D tilt, causing the "roll"
      effect.
    * **Mixed Transformations:** The `zoomFactor` was incorrectly being applied to the `Camera`'s Z-axis (
      a 3D operation) instead of as a 2D scaling of the logical plane.
    * **Stateful Model:** The `Table.kt` data class was pre-calculating its own rotated coordinates,
      while the `UpdateStateUseCase` was *also* applying a world rotation. The table was being
      rotated twice.
    * **Flawed Measurements:** The on-screen size of objects was being calculated from a
      `flatMatrix` (a matrix without tilt), which became incorrect once rotation was properly
      handled in 3D space.

* **Resolution (Architectural Rule):** The perspective transformation must be handled as follows:
    1.  **Stateless Models:** Rotational logic must be removed from data models
       like `Table.kt`. They must only store their pure, un-rotated geometry.
    2.  **Correct 3D Transformation Order:** All spatial transformations must be handled within the
       3D `Camera` object in the correct sequence: **1. Rotate** (spin), **2. Tilt** (pitch), **3.
       Translate** (lift).
    3.  **Separation of Concerns:** 2D `zoom` is a `postScale` operation on the `worldMatrix`. 3D
       `rotation` and `pitch` are `rotateY` and `rotateX` operations on the `perspectiveMatrix`.
       They must not be conflated.
    4.  **Unified Measurement:** The on-screen size of any logical object must be calculated by
       projecting two of its points (e.g., center and edge) through the single, final `pitchMatrix`.


### Case Study: The Inefficient Glow Effect

* **Issue:** The "glow" effect on lines was visually artifacted (showing a repeating pattern) and
  caused performance issues.
* **Cause:** The `lineGlowPaint` was incorrectly configured with `Paint.Style.FILL_AND_STROKE`
      and was being used to draw a series of overlapping circles instead of a single, stroked path.
      This was a brute-force, CPU-intensive method that created visual seams.
* **Lesson Learned:** Use the correct tool for the job. A line glow must be rendered with a
      `Paint` object whose style is `Paint.Style.STROKE`. Complex visual effects like a fade-out should be achieved with
      GPU-accelerated techniques like a `LinearGradient` mask, not CPU-bound loops.
