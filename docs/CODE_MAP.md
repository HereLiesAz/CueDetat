# Code Map

This document serves as an index, mapping high-level concepts to their concrete implementations in the source code.

## Core Domains

### Vision & Computer Vision (OpenCV)
*   **Main Entry Point:** `app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt`
    *   *Responsibility:* Manages CameraX input, converts `ImageProxy` to OpenCV `Mat`, and runs detection pipelines.
*   **State Analysis:** Integrated directly into `VisionRepository.kt` as part of the `processImage` pipeline.
*   **Data Structure:** `app/src/main/java/com/hereliesaz/cuedetat/data/VisionData.kt`
    *   *Responsibility:* Holds the frame-by-frame results (balls, mask, timestamps).
*   **Redux Integration:** `app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/CvReducer.kt`
    *   *Responsibility:* Updates the global state based on vision results.

### Geometry & Perspective
*   **Transformation Logic:** `app/src/main/java/com/hereliesaz/cuedetat/domain/UpdateStateUseCase.kt`
    *   *Responsibility:* Calculates the `pitchMatrix` based on zoom, tilt, and screen size.
*   **Math Utilities:** `app/src/main/java/com/hereliesaz/cuedetat/view/model/Perspective.kt`
    *   *Responsibility:* Static helper methods for matrix multiplication and coordinate mapping.
*   **Rendering Utilities:** `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/util/DrawingUtils.kt`
    *   *Responsibility:* Maps 3D points to 2D screen coordinates for drawing.

### Rendering (Custom Views)
*   **Orchestrator:** `app/src/main/java/com/hereliesaz/cuedetat/view/ProtractorOverlay.kt`
    *   *Responsibility:* The main custom View that triggers `onDraw`.
*   **Table:** `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/table/TableRenderer.kt`
*   **Balls:** `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/ball/BallRenderer.kt`
*   **Lines:** `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/line/LineRenderer.kt`

### State Management (MVI)
*   **Global State:** `app/src/main/java/com/hereliesaz/cuedetat/domain/StateReducer.kt`
    *   *Responsibility:* The root reducer that delegates to sub-reducers.
*   **Sub-Reducers:** `app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/`
    *   *Responsibility:* Handling specific slices of state (Gestures, System, Controls).

## UI (Jetpack Compose)
*   **Main Screen:** `app/src/main/java/com/hereliesaz/cuedetat/ui/ProtractorScreen.kt`
*   **Top Controls:** `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/TopControls.kt`
*   **AR/Camera:** `app/src/main/java/com/hereliesaz/cuedetat/ui/CameraPreview.kt`
