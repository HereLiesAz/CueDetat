# Algorithms & Mathematical Models

This document explains the core mathematical models used in Cue d'Etat, specifically regarding perspective projection, object detection, and coordinate systems.

## 1. The Perspective Projection Model

The application renders a virtual pool table over a camera feed. This requires mapping 2D "Logical Coordinates" (top-down view of the table) to 2D "Screen Coordinates" (distorted 3D view).

### Coordinate Systems
1.  **Logical Space (Inches):**
    *   Origin (0,0): Center of the table.
    *   Y-axis: Length of the table (positive is down/towards player).
    *   X-axis: Width of the table.
    *   Unit: 1.0 = 1 inch (approx).
    *   *This space is invariant to zoom/pan.*

2.  **Screen Space (Pixels):**
    *   Origin (0,0): Top-left corner of the Android View.
    *   Y-axis: Down.
    *   X-axis: Right.

### The Transformation Pipeline
The `pitchMatrix` (calculated in `UpdateStateUseCase`) combines the following transformations in order:

1.  **Translation to Center:** Move logical (0,0) to the center of the drawing area.
2.  **Scale (Zoom):** Apply uniform scaling based on the user's zoom level.
3.  **Rotation (Pitch):** Rotate around the X-axis to simulate camera tilt.
    *   *Note:* This is a "fake" 3D effect achieved via `Matrix.setPolyToPoly` or similar affine skews in some implementations, or true 3D projection in others. In this app, we use a custom `Perspective` helper that likely uses standard projection math.
4.  **Translation to Screen Center:** Move the result to the center of the device screen.

### "Billboarding" Text
Text labels must remain readable even when the table is tilted. We use a "Billboarding" technique:
1.  Map the logical center of the text to a screen coordinate $(x, y)$ using the `pitchMatrix`.
2.  Draw the text at $(x, y)$ *without* applying the `pitchMatrix` to the Canvas itself.
3.  This ensures text is always flat facing the screen (2D) but positioned correctly in 3D space.

## 2. Computer Vision (HSV Detection)

We use OpenCV to detect balls based on color.

### The Algorithm
1.  **Input:** Camera frame (YUV/NV21 converted to RGB or BGR).
2.  **Preprocessing:** Convert BGR to HSV (Hue, Saturation, Value). HSV is more robust to lighting changes than RGB.
3.  **Thresholding (inRange):**
    *   We define a `lowerBound` and `upperBound` for the target color (e.g., White for cue ball).
    *   `Core.inRange(hsvMat, lowerBound, upperBound, mask)` creates a binary mask where white pixels = target color.
4.  **Morphological Operations:**
    *   `Imgproc.morphologyEx(..., MORPH_OPEN)`: Removes small noise (erosion followed by dilation).
    *   `Imgproc.morphologyEx(..., MORPH_CLOSE)`: Closes small holes inside the object (dilation followed by erosion).
5.  **Contour Detection:**
    *   `Imgproc.findContours` identifies connected white regions in the mask.
    *   We filter contours by area and circularity to reject non-ball objects.

## 3. Ghost Ball Physics

(To be implemented: Describe the calculation of the cut angle and tangent lines).
