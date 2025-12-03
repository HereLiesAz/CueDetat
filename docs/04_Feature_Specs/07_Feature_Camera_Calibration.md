# 4.7. Feature Specification: Camera Calibration

This document specifies the behavior of the Camera Calibration feature. This is considered an
internal developer and power-user tool, not a primary feature for the average user.

## I. Purpose

The primary purpose of this feature is to calculate the precise **Camera Matrix** and **Distortion
Coefficients** for a specific device. This data is essential for implementing true lens distortion
correction in the rendering pipeline.

A secondary purpose is to collect this data from users who consent to share it, in order to build
the "Dimensional Distortion Device Database" (D.D.D.D.), which will eventually enable automatic
profile selection for new users.

## II. User Flow

1. The user initiates the feature from the "Too Advanced Options" dialog.
2. The `CalibrationScreen` appears, displaying a live camera feed.
3. The user is instructed to point their camera at a standard **4x11 asymmetric circle grid**
   pattern displayed on a second screen (e.g., a laptop or TV).
4. When the application successfully detects the pattern, it draws a **green overlay** on the
   screen for real-time visual feedback.
5. The user moves their camera to capture the pattern from various angles and distances, pressing a
   "Capture" button each time a stable detection is shown. A counter tracks the number of successful
   captures. A minimum of 10-15 captures is recommended.
6. When finished, the user presses "Finish." The app then performs the calibration calculation.
7. Upon successful calculation, a dialog appears asking for the user's consent to anonymously
   submit the results to the D.D.D.D.

## III. Implementation & Outcome

* **Detection**: The `CalibrationRepository` uses OpenCV's `Calib3d.findCirclesGrid()` to detect
  the pattern in each camera frame.
* **Data Collection**: The `CalibrationViewModel` collects a list of the 2D pixel coordinates for
  all detected grid points across multiple captures.
* **Calculation**: When finished, the ViewModel passes the list of 2D image points, along with a
  programmatically generated "blueprint" of the ideal 3D grid points, to OpenCV's
  `Calib3d.calibrateCamera()` function.
* **Result**: The function returns the 3x3 `cameraMatrix` and the 1x5 `distCoeffs` (distortion
  coefficients).
* **Persistence**: These two matrices are serialized to JSON strings and saved in the
  `UserPreferencesRepository` for future use by the rendering pipeline.