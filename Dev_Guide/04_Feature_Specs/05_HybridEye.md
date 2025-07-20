# 4.5. Feature Specification: CV Hybrid Eye

The application's vision system uses a hybrid, two-stage pipeline to achieve robust ball detection.

## The Two-Stage Pipeline

1. **Phase 1: ML Detection (The "Scout")**

* **Tool**: ML Kit's generic Object Detection.
* **Purpose**: To perform a fast, initial pass on the full camera frame and identify Regions of
  Interest (ROIs) via bounding boxes.

2. **Phase 2: OpenCV Refinement (The "Sniper")**

* **Tool**: OpenCV.
* **Purpose**: For each ROI provided by the Scout, a more precise algorithm is run *only within that
  box*.
* **Algorithm**: The default refinement method is **Contour Detection** (`findContours` +
  `minEnclosingCircle`), which is more robust against perspective distortion than the alternative
  `HoughCircles` method.
* **Dynamic Rangefinder**: The system calculates the expected on-screen pixel radius of a ball at
  the Y-coordinate of the Scout's bounding box. This provides the Sniper with a tight `minRadius`
  and `maxRadius`, reducing false positives.

## Color Calibration

* **Statistical Sampling**: To create the color mask for CV, the system samples a 5x5 patch of the
  felt, not a single pixel. It calculates the **mean and standard deviation** of the HSV values to
  create an adaptive mask resilient to lighting changes.

## Other CV Rules

* **Conditional Snapping:** Auto-snapping of logical balls only occurs if the user places a logical
  ball *in close proximity* to a detected object.
* **Mask Visualization:** A developer toggle exists to render the CV's internal color mask on-screen
  for tuning.
* **Calibration UI:** A dedicated UI workflow exists for calibrating the system to a specific
  table's felt color.