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
*   **Composable Entry Point:** `app/src/main/java/com/hereliesaz/cuedetat/view/ProtractorOverlay.kt`
    *   *Responsibility:* Compose `Canvas` wrapper that drives `onDraw` and handles gesture detection via `GestureHandler`.
*   **Pass Orchestrator:** `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/OverlayRenderer.kt`
    *   *Responsibility:* Coordinates all rendering passes in z-order: rails → beginner lines → balls → beginner labels. Routes to the correct sub-renderers based on state.
*   **Table:** `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/table/TableRenderer.kt`
*   **Rails:** `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/table/RailRenderer.kt`
*   **Balls:** `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/ball/BallRenderer.kt`
*   **Lines:** `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/line/LineRenderer.kt`
    *   *Key methods:* `drawBeginnerLines` (geometry pass, below balls), `drawBeginnerLabels` (text pass, above balls).
*   **Ball Labels:** `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/text/BallTextRenderer.kt`
*   **Line Labels:** `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/text/LineTextRenderer.kt`
*   **CV Debug:** `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/CvDebugRenderer.kt`
    *   *Responsibility:* Renders the raw OpenCV binary mask on-screen when `showCvMask` is enabled.
*   **TPS Warp Utils:** `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/TpsUtils.kt`
    *   *Responsibility:* `PointF.warpedBy(tps)` extension — applies the inverse TPS residual to a logical draw point to correct for lens distortion.
*   **Paint Factory:** `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/util/PaintUtils.kt`
    *   *Key function:* `createGlowPaint(…, blurType)` — creates/caches `Paint` objects for glow effects. `Blur.OUTER` used for the beginner ball outline; `Blur.NORMAL` for everything else.
*   **Paint Cache:** `app/src/main/java/com/hereliesaz/cuedetat/view/PaintCache.kt`
    *   *Responsibility:* Pre-configured `Paint` objects for every visual element, keyed from the visual config objects in `view/config/`.
*   **Spin Color:** `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/util/SpinColorUtils.kt`

### State Management (MVI)
*   **Global State:** `app/src/main/java/com/hereliesaz/cuedetat/domain/StateReducer.kt`
    *   *Responsibility:* The root reducer that delegates to sub-reducers.
*   **Sub-Reducers:** `app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/`
    *   *Responsibility:* Handling specific slices of state (Gestures, System, Controls).

### Gestures
*   **Gesture Modifier:** `app/src/main/java/com/hereliesaz/cuedetat/view/gestures/GestureHandler.kt`
    *   *Responsibility:* Compose `pointerInput` modifier that handles single-finger drag (move objects / pan), two-finger pinch (zoom), two-finger rotation, and two-finger pan. Pan is suppressed in dynamic beginner mode.

### AR / Camera
*   **Standard Camera:** `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/CameraBackground.kt`
    *   *Responsibility:* CameraX preview + `ImageAnalysis` composable. Used when `cameraMode` is `CAMERA` or `CAMERA_ONLY`.
*   **AR Camera:** `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/ArCoreBackground.kt`
    *   *Responsibility:* ARCore-powered `GLSurfaceView` composable. Allocates the OES texture, renders the camera feed as a fullscreen quad, feeds CPU frames to `ArFrameProcessor`, and dispatches `ArTrackingLost` when tracking drops from `TRACKING` to `PAUSED`.
*   **AR Frame Bridge:** `app/src/main/java/com/hereliesaz/cuedetat/data/ArFrameProcessor.kt`
    *   *Responsibility:* Thread-safe bridge from GL-thread ARCore `Frame` objects to `VisionRepository`, using `AtomicReference` so neither thread blocks.
*   **ARCore Session:** `app/src/main/java/com/hereliesaz/cuedetat/data/ArDepthSession.kt`
    *   *Responsibility:* Creates and configures the ARCore `Session`; extracts `DepthPlane` data from the Depth API each frame.
*   **GL Background Renderer:** `app/src/main/java/com/hereliesaz/cuedetat/data/ArBackgroundRenderer.kt`
    *   *Responsibility:* OpenGL ES 2.0 renderer that allocates the OES texture and draws the ARCore camera feed as a fullscreen quad.
*   **AR Status Overlays:** `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/overlays/ArStatusOverlay.kt`
    *   *Key composables:* `ArTrackingBadge` (pulsing indicator when AR is active), `ArSetupPrompt` (wizard with PENDING/ACTIVE/DONE step states during `AR_SETUP`).

### Table Scan
*   **Table Scan Screen:** `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanScreen.kt`
*   **Table Scan ViewModel:** `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanViewModel.kt`
    *   *Responsibility:* Accumulates pocket detections into `PocketCluster`s, fits geometry via `TableGeometryFitter`, computes homography and residual TPS warp, and persists the resulting `TableScanModel`.
*   **Table Scan Analyzer:** `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanAnalyzer.kt`
    *   *Responsibility:* CameraX `ImageAnalysis.Analyzer` that detects pocket-sized blobs using `PocketDetector` (TFLite) or a Hough-circle fallback.
*   **Pocket Detector Interface:** `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/PocketDetector.kt`
*   **TFLite Pocket Detector:** `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TFLitePocketDetector.kt`
    *   *Responsibility:* YOLOv5 TFLite model (640×640 input) for pocket detection; falls back gracefully if the model file is absent.
*   **Table Scan Repository:** `app/src/main/java/com/hereliesaz/cuedetat/data/TableScanRepository.kt`
    *   *Responsibility:* Persists `TableScanModel` to disk as JSON; optionally attaches GPS coordinates for location-based table identification.

## UI (Jetpack Compose)
*   **Main Screen:** `app/src/main/java/com/hereliesaz/cuedetat/ui/ProtractorScreen.kt`
*   **Nav Rail:** `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/AzNavRailMenu.kt`
    *   *Responsibility:* All mode-aware menu buttons. Routes AR setup/cancel/off events; gates controls by `experienceMode` and `cameraMode`.
*   **Top Controls:** `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/TopControls.kt`
