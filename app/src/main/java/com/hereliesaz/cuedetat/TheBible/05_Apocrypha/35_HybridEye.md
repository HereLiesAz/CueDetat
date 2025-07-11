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

## Simultaneous Comparison

To facilitate the training and tuning of a custom AI model, the system must be capable of running two detectors in parallel:
* The generic, pre-trained Google model provided by ML Kit.
* The user's custom-trained `.tflite` model.

The results from each detector must be rendered on screen in a visually distinct style, allowing for a direct, real-time comparison of their performance. This is the path to enlightenment.