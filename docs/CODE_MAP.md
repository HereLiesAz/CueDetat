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
*   **`CueDetatState` notable fields:**
    *   `masseShotAngleDeg: Float` — stores the current massé shot direction in degrees. Set from geometry on massé enable; reset to `0f` on disable. Used directly by massé physics (`Math.toRadians(masseShotAngleDeg)`) instead of deriving the angle from ghost ball position.

### Gestures
*   **Gesture Modifier:** `app/src/main/java/com/hereliesaz/cuedetat/view/gestures/GestureHandler.kt`
    *   *Responsibility:* Compose `pointerInput` modifier that handles single-finger drag (move objects / pan), two-finger pinch (zoom), two-finger rotation, and two-finger pan. Pan is suppressed in dynamic beginner mode.

### AR / Camera
*   **Standard Camera:** `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/CameraBackground.kt`
    *   *Responsibility:* CameraX preview + `ImageAnalysis` composable. Used when `cameraMode` is `CAMERA` or `CAMERA_ONLY`.
*   **AR Camera:** `feature_expert_ar/src/main/java/com/hereliesaz/cuedetat/feature/expert/ar/ArCoreBackground.kt`
    *   *Responsibility:* ARCore-powered `GLSurfaceView` composable. Allocates the OES texture, renders the camera feed as a fullscreen quad, feeds CPU frames to `ArFrameProcessor`. When tracking drops from `TRACKING` to `PAUSED` it only logs a warning and holds the existing anchors — it does **not** dispatch `ArTrackingLost` (that event is a deliberate no-op elsewhere anyway; see `ControlReducer`).
*   **AR Frame Bridge:** `feature_expert_ar/src/main/java/com/hereliesaz/cuedetat/feature/expert/ar/ArFrameProcessor.kt`
    *   *Responsibility:* Thread-safe bridge from GL-thread ARCore `Frame` objects to `VisionRepository`, using `AtomicReference` so neither thread blocks.
*   **ARCore Session:** `feature_expert_ar/src/main/java/com/hereliesaz/cuedetat/feature/expert/ar/ArTableSession.kt`
    *   *Responsibility:* Owns the ARCore `Session` and the world anchors that define the table in expert mode. `requestLock` (the rail's Lock button) casts the virtual table's four on-screen corners onto the detected horizontal plane and anchors them; `computeFrameUpdate` serves that request on the GL thread, then re-projects the anchors to screen every frame and fits the logical→screen homography the renderer applies. Its `capability` property exposes `DepthCapability` (`DEPTH_API` if ARCore is available, `NONE` otherwise) — the ARCore Depth API itself is deliberately disabled, since anchors only need plane-finding and hit-testing. There is no `ArDepthSession.kt` in the repo; this is the real class doing ARCore session/tracking work.
*   **GL Background Renderer:** `feature_expert_ar/src/main/java/com/hereliesaz/cuedetat/feature/expert/ar/ArBackgroundRenderer.kt`
    *   *Responsibility:* OpenGL ES 2.0 renderer that allocates the OES texture and draws the ARCore camera feed as a fullscreen quad.
*   **AR Status Overlays:** `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/overlays/ArStatusOverlay.kt`
    *   *Key composables:* `ArTrackingBadge` (pulsing indicator when AR is active). `ArSetupPrompt` has been deleted.

### Table Scan
*   **Table Scan Screen:** `feature_expert_ar/src/main/java/com/hereliesaz/cuedetat/feature/expert/ar/TableScanScreen.kt`
    *   Rendered as an **inline overlay** inside `ProtractorScreen`, shown when `uiState.showTableScanScreen` is `true`. It is not a navigated route; `ROUTE_SCAN` has been removed from the `NavHost`. No GPS permission request, no Cancel button. Calls `viewModel.resetScan()` on entry via `LaunchedEffect`. Felt-colour capture only; pocket tapping was removed.
*   **Table Scan ViewModel:** `feature_expert_ar/src/main/java/com/hereliesaz/cuedetat/feature/expert/ar/TableScanViewModel.kt`
    *   *Responsibility:* `captureFeltAndComplete()` locks the felt color, loads a default table model and completes the scan. `onFrame` still accumulates automatic pocket detections into `PocketCluster`s and fits geometry via `TableGeometryFitter` (no user tapping). Persists the resulting `TableScanModel`.
*   **Table Scan Analyzer:** `feature_expert_ar/src/main/java/com/hereliesaz/cuedetat/feature/expert/ar/TableScanAnalyzer.kt`
    *   *Responsibility:* CameraX `ImageAnalysis.Analyzer` that detects pocket-sized blobs using `PocketDetector` (TFLite) or a Hough-circle fallback.
*   **Pocket Detector Interface:** `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/PocketDetector.kt`
*   **TFLite Pocket Detector:** `app/src/main/java/com/hereliesaz/cuedetat/data/MergedTFLiteDetector.kt`
    *   *Responsibility:* `PocketDetector` implementation backed by the merged `MASTER_POOL_MODEL.tflite` (YOLOv8n, multiple heads packed into one binary); falls back gracefully if the model asset is absent.
*   **Table Scan Repository:** `app/src/main/java/com/hereliesaz/cuedetat/data/TableScanRepository.kt`
    *   *Responsibility:* Persists `TableScanModel` to disk as JSON; optionally attaches GPS coordinates for location-based table identification.

## UI (Jetpack Compose)
*   **Main Screen:** `app/src/main/java/com/hereliesaz/cuedetat/ui/ProtractorScreen.kt`
*   **Nav Rail:** `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/AzNavRailMenu.kt`
    *   *Responsibility:* All mode-aware menu buttons. Routes AR setup/cancel/off events; gates controls by `experienceMode` and `cameraMode`. Also declares the AzNavRail 10.18 tutorial guidance (`azStatus`/`azEdge`/`azGoal`/`azGuidanceTarget`).
*   **Tutorial guidance targets:** `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/overlays/TutorialGuidanceTargets.kt`
    *   *Responsibility:* Maps `CueDetatState` to `AzGuideShape` spotlight targets (target/ghost/cue ball, aiming line, zoom slider) for the guidance tutorial.
*   **Top Controls:** `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/TopControls.kt`
