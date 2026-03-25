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
*   *Note:* This is a true 3D perspective projection achieved using Android's `android.graphics.Camera` helper class, which maps the 2D logical plane into 3D space.
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

### Cut Angle & Ghost Ball Position

The "ghost ball" is the position the cue ball must occupy at the moment of contact to send the target ball into the pocket. Given:
- Cue ball center **C**
- Target ball center **T**
- Pocket center **P**

The ghost ball center **G** is found by walking backwards from **T** toward **C** by one ball diameter:

    direction = normalize(T → P)
    G = T - direction × (2 × ballRadius)

### Tangent Line

After impact, the cue ball travels perpendicular to the line connecting the two ball centers at impact. The tangent line direction:

    tangentDir = perpendicular(normalize(G → T))

This is the theoretical trajectory of the cue ball post-impact (zero English). Sidespin modifies the actual path — see Spin Paths in `CalculateSpinPaths.kt`.

### Cut Angle

The cut angle θ is the angle between the original cue ball travel direction and the line **C→G**:

    θ = acos(dot(normalize(C → shotTarget), normalize(C → G)))

A cut of 0° is a straight shot; 90° is a full cut (barely grazes the target ball, rarely pocketable).

## 4. TPS Lens Distortion Correction

The AR overlay uses a **Thin-Plate Spline (TPS)** residual warp to correct for lens distortion beyond what a simple homography can model.

### Overview

1. **Table Scan** — `TableScanViewModel` collects 6 pocket positions in image space and fits a homography H mapping image → logical space.
2. **Geometry Fit** — `TableGeometryFitter` assigns identities (TL, TR, BL, BR, SL, SR) and produces the 6 *true* logical positions from the known 2:1 table model.
3. **Residual TPS** — `ThinPlateSpline` solves the system: src = homography-estimated logical positions, dst = true logical positions. The residual TPS captures what the homography got wrong.
4. **Rendering** — `TpsUtils.warpedBy(tps)` applies the *inverse* TPS (dst→src) to each draw point before passing it to the `pitchMatrix`. This corrects the rendered overlay to match real physical pocket positions.

### AR Overlay Confidence & Auto-Advance

`VisionData.tableOverlayConfidence` (0–1) is set by the CV pipeline during `AR_SETUP`. `CvReducer` auto-advances `AR_SETUP → AR_ACTIVE` when:
- `lockedHsvColor != null` (color step done)
- `tableScanModel != null` (scan step done)
- `tableOverlayConfidence >= 0.8`
