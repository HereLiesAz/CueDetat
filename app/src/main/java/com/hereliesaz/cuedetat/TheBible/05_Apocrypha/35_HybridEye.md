# 35: The Doctrine of the Hybrid Eye

The limitations of a purely geometric CV system are documented in the Parables. To achieve true sight, especially in the "dim light of a bar," a hybrid approach is mandated.

## The Two-Stage Pipeline

The application's vision system must not rely on a single method. It must employ a two-stage pipeline that leverages the strengths of both machine learning and classical computer vision.

1.  **Phase 1: ML Detection (The Scout)**
    * **Tool**: ML Kit's Object Detection.
    * **Purpose**: To perform a fast, initial pass on the full camera frame. Its job is not to find the *exact* center of the balls, but to identify their general location and provide a *Region of Interest* (ROI) via a bounding box. This is a broad search to find potential targets.

2.  **Phase 2: OpenCV Refinement (The Sniper)**
    * **Tool**: OpenCV.
    * **Purpose**: For each ROI provided by the ML scout, a more precise, computationally expensive OpenCV algorithm is run *only within that small box*. This refines the detected object's exact center.
    * **Mandate of Choice**: The system must provide the ability to switch between two refinement algorithms for A/B testing: `HoughCircles` and `findContours` + `minEnclosingCircle`. This choice must be a user-configurable setting.
    * **Mandate of Confinement**: When the table is visible (`showTable = true`), the detection pipeline **must** ignore any ROI that falls outside the logical boundaries of the table. The machine's eye shall not wander from the field of play.

## Simultaneous Comparison

To facilitate the training and tuning of a custom AI model, the system must be capable of running two detectors in parallel:
* The generic, pre-trained Google model provided by ML Kit.
* The user's custom-trained `.tflite` model.

The results from each detector must be rendered on screen in a visually distinct style, allowing for a direct, real-time comparison of their performance. This is the path to enlightenment.

## New Decrees of Vision

* **Doctrine of Conditional Snapping:** The auto-snapping of logical balls to detected CV objects shall no longer be absolute. It is now decreed that a snap shall only occur if the user places a logical ball *in close proximity* to a detected object. A ball placed in open space must remain in open space, free from the pull of a distant, unrelated detection.
* **Doctrine of the Revealed Mask:** A toggle shall be added to the Developer Options to render the CV's internal color mask directly on the screen. This will grant the user the sight necessary to properly tune the detection parameters.
* **Doctrine of the Calibrated Eye:** A new, dedicated workflow shall be created for calibrating the system to the felt color of a specific table, improving the accuracy of the mask.
* **Doctrine of Specificity (Future Work):** Once the eye is calibrated, it must be taught to see not just "balls", but *specific* balls, distinguishing solids from stripes, and all from the holy cue and the final 8-ball. This is the next great work.