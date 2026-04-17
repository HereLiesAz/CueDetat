# Beginner Rendering Fixes + AR Flow Redesign — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix 4 issues: dynamic-beginner pan suppression, static-beginner z-order + bubble-center-dot, strict fill/stroke rules, and an AR camera state machine with a guided setup wizard.

**Architecture:** Pure-reducer state changes for AR flow (Issues 1, 4); structural rendering refactors for z-order and paint rules (Issues 2, 3). Tasks ordered to build on compile-safe foundations — UiModel changes first, then reducers, then UI/rendering.

**Tech Stack:** Kotlin, Android Canvas, Jetpack Compose, JUnit 4 (pure unit tests in `app/src/test/`)

**Spec:** `docs/superpowers/specs/2026-03-24-beginner-rendering-ar-flow-design.md`

---

## File Map

| File | Change |
|------|--------|
| `view/gestures/GestureHandler.kt` | Task 1 — restructure two-finger and single-finger pan guards |
| `view/renderer/util/PaintUtils.kt` | Task 2 — add `blurType` param to `createGlowPaint`, update cache key |
| `view/renderer/ball/BallRenderer.kt` | Task 3 — fix draw order 3a–3e; add bubble center dot; pass `Blur.OUTER` |
| `view/renderer/line/LineRenderer.kt` | Task 4 — add `drawGeometry` param; new `drawBeginnerLines` + `drawBeginnerLabels` |
| `view/renderer/OverlayRenderer.kt` | Task 5 — insert Pass 2.5; replace Pass 4 call |
| `domain/UiModel.kt` | Task 6 — rename `AR` → `AR_ACTIVE`; add `AR_SETUP`, `CAMERA_ONLY`; `ArSetupStep` enum; 3 new events |
| `domain/StateReducer.kt` | Task 7 — route 3 new events to their reducers |
| `domain/reducers/ToggleReducer.kt` | Task 7 — rewrite `CycleCameraMode`; add `CancelArSetup`, `TurnCameraOff` |
| `domain/reducers/ControlReducer.kt` | Task 8 — add `ArTrackingLost` handler |
| `data/VisionData.kt` | Task 9 — add `tableOverlayConfidence: Float = 0f` |
| `domain/reducers/CvReducer.kt` | Task 9 — auto-advance AR_SETUP → AR_ACTIVE check |
| `data/ArCoreBackground.kt` | Task 10 — dispatch `ArTrackingLost` on tracking state change |
| `ui/composables/AzNavRailMenu.kt` | Task 11 — Cancel button in AR_SETUP; Off button in CAMERA_ONLY; gate AR button |
| `ui/composables/overlays/ArStatusOverlay.kt` | Task 12 — replace `ArSetupPrompt` with wizard step display |

**Test files:**
| File | Tests |
|------|-------|
| `app/src/test/.../domain/ArFlowReducerTest.kt` | Tasks 7–9 |

---

## Task 1 — Suppress pan in dynamic beginner (`GestureHandler.kt`)

**Files:** Modify `app/src/main/java/com/hereliesaz/cuedetat/view/gestures/GestureHandler.kt`

The two-finger block (line ~70) currently allows pan when `!isBeginnerViewLocked`; the single-finger world-locked block (line ~93) also emits pan in dynamic beginner. Both must suppress `PanView` in dynamic beginner.

- [ ] **Step 1: Update two-finger block**

  Find the block that starts with:
  ```kotlin
  if (uiState.table.isVisible || (uiState.experienceMode == ExperienceMode.BEGINNER && !uiState.isBeginnerViewLocked)) {
  ```
  The condition is true for both "expert with table" and "dynamic beginner". Rotation should only fire when the table is visible; pan should never fire in dynamic beginner. Restructure:
  ```kotlin
  if (uiState.table.isVisible) {
      val rotation = event.calculateRotation()
      if (rotation != 0f) {
          onEvent(MainScreenEvent.TableRotationApplied(rotation))
      }
      val pan = event.calculatePan()
      if (abs(pan.y) > 0.1f || abs(pan.x) > 0.1f) {
          onEvent(MainScreenEvent.PanView(PointF(pan.x, pan.y)))
      }
  } else if (uiState.experienceMode == ExperienceMode.BEGINNER && !uiState.isBeginnerViewLocked) {
      // Dynamic beginner: rotation only, pan suppressed
      // (No table visible, so TableRotationApplied is not used here — just swallow the gesture)
  }
  ```

- [ ] **Step 2: Update single-finger world-locked block**

  Find:
  ```kotlin
  if (uiState.isWorldLocked && (uiState.experienceMode != ExperienceMode.BEGINNER || !uiState.isBeginnerViewLocked)) {
  ```
  Add dynamic-beginner suppression:
  ```kotlin
  val isDynamicBeginner = uiState.experienceMode == ExperienceMode.BEGINNER && !uiState.isBeginnerViewLocked
  if (uiState.isWorldLocked && !isDynamicBeginner) {
  ```

- [ ] **Step 3: Compile check**
  ```
  ./gradlew :app:compileDebugKotlin
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**
  ```bash
  git add app/src/main/java/com/hereliesaz/cuedetat/view/gestures/GestureHandler.kt
  git commit -m "fix: suppress pan emission in dynamic beginner mode"
  ```

---

## Task 2 — `createGlowPaint` blur type parameter (`PaintUtils.kt`)

**Files:** Modify `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/util/PaintUtils.kt`

- [ ] **Step 1: Add `blurType` parameter with default `Blur.NORMAL`**

  Find the `createGlowPaint` function signature:
  ```kotlin
  fun createGlowPaint(
      baseGlowColor: Color,
      baseGlowWidth: Float,
      state: CueDetatState,
      paints: PaintCache
  ): Paint
  ```
  Change to:
  ```kotlin
  fun createGlowPaint(
      baseGlowColor: Color,
      baseGlowWidth: Float,
      state: CueDetatState,
      paints: PaintCache,
      blurType: android.graphics.BlurMaskFilter.Blur = android.graphics.BlurMaskFilter.Blur.NORMAL
  ): Paint
  ```

- [ ] **Step 2: Update cache key and BlurMaskFilter call**

  In the function body, find the cache key string (currently `"glow_${...}_${...}"`) and add blur type:
  ```kotlin
  val key = "glow_${baseGlowColor}_${blurRadius}_${blurType.name}"
  ```
  Then find the `BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)` line(s) — there are two (one for the normal path, one for the fallback) — and replace the hardcoded `Blur.NORMAL` with the parameter:
  ```kotlin
  paint.maskFilter = BlurMaskFilter(blurRadius, blurType)
  ```
  Do this for both occurrences.

- [ ] **Step 3: Compile check**
  ```
  ./gradlew :app:compileDebugKotlin
  ```
  Expected: `BUILD SUCCESSFUL` (all existing callers use default, no changes required elsewhere yet)

- [ ] **Step 4: Commit**
  ```bash
  git add app/src/main/java/com/hereliesaz/cuedetat/view/renderer/util/PaintUtils.kt
  git commit -m "feat: add blurType param to createGlowPaint (default NORMAL, beginner uses OUTER)"
  ```

---

## Task 3 — Static beginner ball draw order + bubble center dot (`BallRenderer.kt`)

**Files:** Modify `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/ball/BallRenderer.kt`

The beginner-locked branch of `drawGhostedBall` currently draws: glow → stroke → static dot → bubble fill. The correct order is: glow → stroke → bubble fill → static dot → **bubble dot (new)**. The glow must also switch to `Blur.OUTER`.

- [ ] **Step 1: Reorder draws and add bubble center dot**

  Find the beginner-locked drawing section (starts with `// 3. DRAW THE STATIONARY OUTLINE`). Replace the four draw calls:
  ```kotlin
  // STATIONARY OUTLINE: glow (OUTER), then stroke ring
  canvas.drawCircle(logicalScreenPos.x, logicalScreenPos.y, exactScreenRadius,
      createGlowPaint(config.glowColor, config.glowWidth, state, paints,
          blurType = android.graphics.BlurMaskFilter.Blur.OUTER))
  canvas.drawCircle(logicalScreenPos.x, logicalScreenPos.y, screenInnerRadius, strokePaint)

  // BUBBLE: translucent fill
  canvas.drawCircle(bubbleCenter.x, bubbleCenter.y, exactScreenRadius, translucentFillPaint)

  // STATIC CENTER DOT: white fill, at fixed position
  canvas.drawCircle(logicalScreenPos.x, logicalScreenPos.y, dotRadius, dotPaint)

  // BUBBLE CENTER DOT: white stroke (no fill), moves with bubble
  val bubbleDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = android.graphics.Color.WHITE
      style = Paint.Style.STROKE
      strokeWidth = 3f
  }
  canvas.drawCircle(bubbleCenter.x, bubbleCenter.y, dotRadius, bubbleDotPaint)
  ```

  Note: the existing `glowPaint` variable was created with `createGlowPaint` without a blur type — replace its usage here with an inline call passing `Blur.OUTER`, OR update `glowPaint` construction at line ~224 to pass `blurType = Blur.OUTER` since this path is beginner-locked only.

  Also note: the existing code draws `dotPaint` (white fill) at `logicalScreenPos`. Ensure the bubble fill circle uses `bubbleCenter` (the tilted position), not `logicalScreenPos`.

- [ ] **Step 2: Compile check**
  ```
  ./gradlew :app:compileDebugKotlin
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
  ```bash
  git add app/src/main/java/com/hereliesaz/cuedetat/view/renderer/ball/BallRenderer.kt
  git commit -m "fix: beginner locked ball draw order; add bubble center dot; use Blur.OUTER glow"
  ```

---

## Task 4 — Split `drawBeginnerForeground` (`LineRenderer.kt`)

**Files:** Modify `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/line/LineRenderer.kt`

Add `drawGeometry: Boolean = true` to private methods `drawAimingLines` and `drawTangentLines`. When `false`, skip canvas path/triangle drawing but still execute the text draw path. Then create two new public methods.

- [ ] **Step 1: Add `drawGeometry` param to `drawClippedLine`**

  Find `drawClippedLine` signature (line ~544). Add parameter at the end:
  ```kotlin
  private fun drawClippedLine(
      ...,
      textToDraw: String? = null,
      typeface: Typeface?,
      drawGeometry: Boolean = true   // ← add this
  )
  ```
  In the body, wrap the two path-draw calls with the flag:
  ```kotlin
  if (drawGeometry) {
      glowPaint?.let { canvas.drawPath(path, it) }
      canvas.drawPath(path, paint)
  }
  ```
  The triangle drawing (if any, inside the path building) also gate with `drawGeometry`. The text drawing block at the end remains unconditional (it already checks `textToDraw != null`).

- [ ] **Step 2: Propagate `drawGeometry` through `drawBankablePath`**

  Find `drawBankablePath` signature (line ~465). Add:
  ```kotlin
  private fun drawBankablePath(..., drawGeometry: Boolean = true)
  ```
  Pass it through to the `drawClippedLine` call(s) inside.

- [ ] **Step 3: Add `drawGeometry` param to `drawAimingLines` and `drawTangentLines`**

  Both are `private fun`. Add `drawGeometry: Boolean = true` to each. In `drawAimingLines`:
  - Change `val textToDraw = if (isBeginnerLocked) "Aim this line at the pocket." else null`
    to `val textToDraw = if (isBeginnerLocked && !drawGeometry) "Aim this line at the pocket." else null`
  - Pass `drawGeometry` to `drawBankablePath`.

  In `drawTangentLines` (beginner-locked branch):
  - When `drawGeometry = false`, only pass `"Tangent Line"` as text; when true, pass `null`.
  - Pass `drawGeometry` to `drawClippedLine`.

  Summary: geometry pass → text is null, geometry drawn. Labels pass → text is set, geometry skipped.

- [ ] **Step 4: Create `drawBeginnerLines` and `drawBeginnerLabels`**

  Keep the existing `drawBeginnerForeground` intact for now (it will be removed in Task 5). Add two new public methods:

  ```kotlin
  fun drawBeginnerLines(
      canvas: Canvas,
      state: CueDetatState,
      paints: PaintCache,
      activeMatrix: Matrix
  ) {
      val (camArray, distArray) = resolveLensArrays(state)
      canvas.save()
      canvas.concat(activeMatrix)
      drawTangentLines(canvas, state, paints, activeMatrix, camArray, distArray, null, drawGeometry = true)
      drawAimingLines(canvas, state, paints, activeMatrix, camArray, distArray, null, drawGeometry = true)
      canvas.restore()
  }

  fun drawBeginnerLabels(
      canvas: Canvas,
      state: CueDetatState,
      paints: PaintCache,
      typeface: Typeface?,
      activeMatrix: Matrix
  ) {
      val (camArray, distArray) = resolveLensArrays(state)
      canvas.save()
      canvas.concat(activeMatrix)
      drawTangentLines(canvas, state, paints, activeMatrix, camArray, distArray, typeface, drawGeometry = false)
      drawAimingLines(canvas, state, paints, activeMatrix, camArray, distArray, typeface, drawGeometry = false)
      canvas.restore()
  }
  ```

  Extract the lens-array boilerplate from `drawBeginnerForeground` into a private helper:
  ```kotlin
  private fun resolveLensArrays(state: CueDetatState): Pair<DoubleArray?, DoubleArray?> {
      val camMat = state.cameraMatrix
      val distMat = state.distCoeffs
      if (camMat == null || camMat.empty() || distMat == null || distMat.empty()) return null to null
      val cam = DoubleArray(camMat.total().toInt()).also { camMat.get(0, 0, it) }
      val dist = DoubleArray(distMat.total().toInt()).also { distMat.get(0, 0, it) }
      return cam to dist
  }
  ```

- [ ] **Step 5: Compile check**
  ```
  ./gradlew :app:compileDebugKotlin
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**
  ```bash
  git add app/src/main/java/com/hereliesaz/cuedetat/view/renderer/line/LineRenderer.kt
  git commit -m "feat: split drawBeginnerForeground into drawBeginnerLines + drawBeginnerLabels"
  ```

---

## Task 5 — Reorder rendering passes (`OverlayRenderer.kt`)

**Files:** Modify `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/OverlayRenderer.kt`

- [ ] **Step 1: Update the draw method**

  Replace the current Pass 3 and Pass 4 block:
  ```kotlin
  // Pass 2: rails (unchanged — already present)

  // Pass 2.5 NEW: Beginner direction lines + triangles (BELOW balls, no text)
  if (state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked) {
      lineRenderer.drawBeginnerLines(canvas, state, paints, matrixFor2DPlane)
  }

  // Pass 3: Draw all balls and their associated text (unchanged)
  ballRenderer.draw(canvas, state, paints, typeface)

  // Pass 4: Beginner text labels only (ABOVE balls)
  if (state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked) {
      lineRenderer.drawBeginnerLabels(canvas, state, paints, typeface, matrixFor2DPlane)
  }
  // REMOVE the old drawBeginnerForeground call that was here
  ```

- [ ] **Step 2: Compile check**
  ```
  ./gradlew :app:compileDebugKotlin
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
  ```bash
  git add app/src/main/java/com/hereliesaz/cuedetat/view/renderer/OverlayRenderer.kt
  git commit -m "fix: reorder beginner rendering passes — lines below balls, labels above"
  ```

---

## Task 6 — New `CameraMode` values, `ArSetupStep`, new events (`UiModel.kt`)

**Files:** Modify `app/src/main/java/com/hereliesaz/cuedetat/domain/UiModel.kt`

- [ ] **Step 1: Update `CameraMode` enum**

  Find the current enum:
  ```kotlin
  enum class CameraMode {
      OFF, CAMERA, AR;
      fun next(): CameraMode { ... }
  }
  ```
  Replace with:
  ```kotlin
  enum class CameraMode {
      OFF, CAMERA, AR_SETUP, AR_ACTIVE, CAMERA_ONLY;
      fun next(): CameraMode {
          val nextOrdinal = (this.ordinal + 1) % values().size
          return values()[nextOrdinal]
      }
  }
  ```

- [ ] **Step 2: Add `ArSetupStep` enum** (add near `CameraMode`):
  ```kotlin
  enum class ArSetupStep { PICK_COLOR, SCAN_TABLE, VERIFY }
  ```

- [ ] **Step 3: Add 3 new events** to the `MainScreenEvent` sealed class:
  ```kotlin
  object CancelArSetup : MainScreenEvent()
  object TurnCameraOff : MainScreenEvent()
  object ArTrackingLost : MainScreenEvent()
  ```

- [ ] **Step 4: Compile — expect failures for `CameraMode.AR` references**
  ```
  ./gradlew :app:compileDebugKotlin
  ```
  Expected: multiple errors `Unresolved reference: AR`. Note all files listed in the error output.

- [ ] **Step 5: Fix all `CameraMode.AR` references** (from the error list):

  Files that reference `CameraMode.AR`:
  - `ui/ProtractorScreen.kt` line ~381: `CameraMode.AR` → `CameraMode.AR_ACTIVE`
  - `ui/composables/AzNavRailMenu.kt` line ~116: `CameraMode.AR` → `CameraMode.AR_ACTIVE` (will be further changed in Task 11)
  - `data/VisionRepository.kt` line ~367: `CameraMode.AR` → `CameraMode.AR_ACTIVE`
  - `domain/reducers/ToggleReducer.kt` line ~51: will be replaced entirely in Task 7
  - `data/ArCoreBackground.kt`: KDoc comment only — update comment text

- [ ] **Step 6: Compile check**
  ```
  ./gradlew :app:compileDebugKotlin
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**
  ```bash
  git add app/src/main/java/com/hereliesaz/cuedetat/domain/UiModel.kt \
          app/src/main/java/com/hereliesaz/cuedetat/ui/ProtractorScreen.kt \
          app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt \
          app/src/main/java/com/hereliesaz/cuedetat/data/ArCoreBackground.kt \
          app/src/main/java/com/hereliesaz/cuedetat/ui/composables/AzNavRailMenu.kt
  git commit -m "feat: add AR_SETUP/AR_ACTIVE/CAMERA_ONLY to CameraMode; ArSetupStep enum; new events"
  ```
  Note: `AzNavRailMenu.kt` is included here for the `CameraMode.AR` → `AR_ACTIVE` fix only; the Cancel/Off buttons are added in Task 11.

---

## Task 7 — Update reducers + StateReducer routing

**Files:**
- Modify `app/src/main/java/com/hereliesaz/cuedetat/domain/StateReducer.kt`
- Modify `app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ToggleReducer.kt`

- [ ] **Step 1: Write failing test**

  Create `app/src/test/java/com/hereliesaz/cuedetat/domain/ArFlowReducerTest.kt`:
  ```kotlin
  package com.hereliesaz.cuedetat.domain

  import com.hereliesaz.cuedetat.domain.reducers.reduceToggleAction
  import org.junit.Assert.assertEquals
  import org.junit.Test

  class ArFlowReducerTest {

      private val base = CueDetatState()
      // ReducerUtils has no Android dependencies — instantiate directly
      private val utils = ReducerUtils()

      @Test
      fun `CycleCameraMode from OFF transitions to AR_SETUP`() {
          val s = base.copy(cameraMode = CameraMode.OFF)
          val result = reduceToggleAction(s, MainScreenEvent.CycleCameraMode, utils)
          assertEquals(CameraMode.AR_SETUP, result.cameraMode)
      }

      @Test
      fun `CycleCameraMode from CAMERA_ONLY transitions to AR_SETUP`() {
          val s = base.copy(cameraMode = CameraMode.CAMERA_ONLY)
          val result = reduceToggleAction(s, MainScreenEvent.CycleCameraMode, utils)
          assertEquals(CameraMode.AR_SETUP, result.cameraMode)
      }

      @Test
      fun `CycleCameraMode from AR_SETUP is a no-op`() {
          val s = base.copy(cameraMode = CameraMode.AR_SETUP)
          val result = reduceToggleAction(s, MainScreenEvent.CycleCameraMode, utils)
          assertEquals(CameraMode.AR_SETUP, result.cameraMode)
      }

      @Test
      fun `CycleCameraMode from AR_ACTIVE is a no-op`() {
          val s = base.copy(cameraMode = CameraMode.AR_ACTIVE)
          val result = reduceToggleAction(s, MainScreenEvent.CycleCameraMode, utils)
          assertEquals(CameraMode.AR_ACTIVE, result.cameraMode)
      }

      @Test
      fun `CancelArSetup transitions to CAMERA_ONLY`() {
          val s = base.copy(cameraMode = CameraMode.AR_SETUP)
          val result = reduceToggleAction(s, MainScreenEvent.CancelArSetup, utils)
          assertEquals(CameraMode.CAMERA_ONLY, result.cameraMode)
      }

      @Test
      fun `TurnCameraOff transitions to OFF`() {
          val s = base.copy(cameraMode = CameraMode.CAMERA_ONLY)
          val result = reduceToggleAction(s, MainScreenEvent.TurnCameraOff, utils)
          assertEquals(CameraMode.OFF, result.cameraMode)
      }
  }
  ```

- [ ] **Step 2: Run test — expect compile failure** (function `reduceToggleAction` is internal):
  ```
  ./gradlew :app:testDebugUnitTest --tests "*.ArFlowReducerTest" 2>&1 | tail -20
  ```
  Expected: compile error (events not yet handled)

- [ ] **Step 3: Update `ToggleReducer.kt`**

  Find the `CycleCameraMode` handler (currently `state.copy(cameraMode = if (...AR...) OFF else AR)`) and replace:
  ```kotlin
  is MainScreenEvent.CycleCameraMode -> when (state.cameraMode) {
      CameraMode.OFF, CameraMode.CAMERA_ONLY -> state.copy(cameraMode = CameraMode.AR_SETUP)
      else -> state
  }
  is MainScreenEvent.CancelArSetup -> state.copy(cameraMode = CameraMode.CAMERA_ONLY)
  is MainScreenEvent.TurnCameraOff -> state.copy(cameraMode = CameraMode.OFF)
  ```

- [ ] **Step 4: Update `StateReducer.kt` routing**

  Find the toggle-events routing block. Add the new events:
  ```kotlin
  is MainScreenEvent.CancelArSetup,
  is MainScreenEvent.TurnCameraOff,
  is MainScreenEvent.CycleCameraMode, ... ->
      reduceToggleAction(currentState, action)
  ```
  And for `ArTrackingLost` (add to the controls block):
  ```kotlin
  is MainScreenEvent.ArTrackingLost,
  is MainScreenEvent.PanView, ... ->
      reduceControlAction(currentState, action)
  ```

- [ ] **Step 5: Run tests**
  ```
  ./gradlew :app:testDebugUnitTest --tests "*.ArFlowReducerTest"
  ```
  Expected: all 6 tests PASS

- [ ] **Step 6: Compile check**
  ```
  ./gradlew :app:compileDebugKotlin
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**
  ```bash
  git add app/src/test/java/com/hereliesaz/cuedetat/domain/ArFlowReducerTest.kt \
          app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ToggleReducer.kt \
          app/src/main/java/com/hereliesaz/cuedetat/domain/StateReducer.kt
  git commit -m "feat: AR state machine — CycleCameraMode → AR_SETUP; Cancel/TurnOff handlers"
  ```

---

## Task 8 — `ArTrackingLost` handler (`ControlReducer.kt`)

**Files:** Modify `app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ControlReducer.kt`

- [ ] **Step 1: Add tests to `ArFlowReducerTest.kt`**

  Add to the existing test class (requires importing `reduceControlAction`):
  ```kotlin
  @Test
  fun `ArTrackingLost clears tableScanModel and lensWarpTps and returns to AR_SETUP from AR_ACTIVE`() {
      val scan = TableScanModel(
          pockets = emptyList(), lensWarpTps = TpsWarpData(emptyList(), emptyList()),
          tableSize = TableSize.NINE_FOOT, feltColorHsv = listOf(0f, 0f, 0f),
          scanLatitude = null, scanLongitude = null
      )
      val s = base.copy(
          cameraMode = CameraMode.AR_ACTIVE,
          tableScanModel = scan,
          lensWarpTps = scan.lensWarpTps
      )
      val result = reduceControlAction(s, MainScreenEvent.ArTrackingLost)
      assertEquals(CameraMode.AR_SETUP, result.cameraMode)
      assertNull(result.tableScanModel)
      assertNull(result.lensWarpTps)
  }

  @Test
  fun `ArTrackingLost when not AR_ACTIVE does not change cameraMode`() {
      val s = base.copy(cameraMode = CameraMode.CAMERA_ONLY)
      val result = reduceControlAction(s, MainScreenEvent.ArTrackingLost)
      assertEquals(CameraMode.CAMERA_ONLY, result.cameraMode)
  }
  ```

- [ ] **Step 2: Run tests — expect failure**
  ```
  ./gradlew :app:testDebugUnitTest --tests "*.ArFlowReducerTest"
  ```
  Expected: 2 new tests FAIL

- [ ] **Step 3: Add handler in `ControlReducer.kt`**

  Add a new `when` branch:
  ```kotlin
  is MainScreenEvent.ArTrackingLost -> state.copy(
      tableScanModel = null,
      lensWarpTps = null,
      cameraMode = if (state.cameraMode == CameraMode.AR_ACTIVE) CameraMode.AR_SETUP else state.cameraMode
  )
  ```

- [ ] **Step 4: Run tests**
  ```
  ./gradlew :app:testDebugUnitTest --tests "*.ArFlowReducerTest"
  ```
  Expected: all tests PASS

- [ ] **Step 5: Commit**
  ```bash
  git add app/src/test/java/com/hereliesaz/cuedetat/domain/ArFlowReducerTest.kt \
          app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ControlReducer.kt
  git commit -m "feat: ArTrackingLost handler clears tableScanModel and returns to AR_SETUP"
  ```

---

## Task 9 — `tableOverlayConfidence` + auto-advance (`VisionData.kt`, `CvReducer.kt`)

**Files:**
- Modify `app/src/main/java/com/hereliesaz/cuedetat/data/VisionData.kt`
- Modify `app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/CvReducer.kt`

- [ ] **Step 1: Add `tableOverlayConfidence` to `VisionData`**

  In the data class, add after the existing fields:
  ```kotlin
  val tableOverlayConfidence: Float = 0f,  // 0..1, set by CV pipeline during AR_SETUP
  ```
  Also update the `equals` and `hashCode` overrides (add the field to both).

- [ ] **Step 2: Add auto-advance logic in `CvReducer.kt`**

  In the `CvDataUpdated` branch, after `var nextState = state.copy(visionData = action.visionData)`, add:
  ```kotlin
  // Auto-advance AR_SETUP → AR_ACTIVE when confidence is high enough
  val AR_AUTO_CONFIRM_CONFIDENCE_THRESHOLD = 0.8f
  if (nextState.cameraMode == CameraMode.AR_SETUP &&
      nextState.lockedHsvColor != null &&
      nextState.tableScanModel != null &&
      action.visionData.tableOverlayConfidence >= AR_AUTO_CONFIRM_CONFIDENCE_THRESHOLD) {
      nextState = nextState.copy(cameraMode = CameraMode.AR_ACTIVE)
  }
  ```

- [ ] **Step 3: Add tests to `ArFlowReducerTest.kt`**

  ```kotlin
  @Test
  fun `CvDataUpdated auto-advances to AR_ACTIVE when all conditions met`() {
      val scan = TableScanModel(
          pockets = emptyList(), lensWarpTps = TpsWarpData(emptyList(), emptyList()),
          tableSize = TableSize.NINE_FOOT, feltColorHsv = listOf(0f, 0f, 0f),
          scanLatitude = null, scanLongitude = null
      )
      val s = base.copy(
          cameraMode = CameraMode.AR_SETUP,
          lockedHsvColor = floatArrayOf(60f, 0.5f, 0.8f),
          tableScanModel = scan
      )
      val visionData = VisionData(tableOverlayConfidence = 0.9f)
      val result = reduceCvAction(s, MainScreenEvent.CvDataUpdated(visionData))
      assertEquals(CameraMode.AR_ACTIVE, result.cameraMode)
  }

  @Test
  fun `CvDataUpdated does NOT auto-advance when confidence is low`() {
      val scan = TableScanModel(
          pockets = emptyList(), lensWarpTps = TpsWarpData(emptyList(), emptyList()),
          tableSize = TableSize.NINE_FOOT, feltColorHsv = listOf(0f, 0f, 0f),
          scanLatitude = null, scanLongitude = null
      )
      val s = base.copy(
          cameraMode = CameraMode.AR_SETUP,
          lockedHsvColor = floatArrayOf(60f, 0.5f, 0.8f),
          tableScanModel = scan
      )
      val visionData = VisionData(tableOverlayConfidence = 0.5f)
      val result = reduceCvAction(s, MainScreenEvent.CvDataUpdated(visionData))
      assertEquals(CameraMode.AR_SETUP, result.cameraMode)
  }
  ```

- [ ] **Step 4: Run all tests**
  ```
  ./gradlew :app:testDebugUnitTest --tests "*.ArFlowReducerTest"
  ```
  Expected: all tests PASS

- [ ] **Step 5: Compile check**
  ```
  ./gradlew :app:compileDebugKotlin
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**
  ```bash
  git add app/src/main/java/com/hereliesaz/cuedetat/data/VisionData.kt \
          app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/CvReducer.kt \
          app/src/test/java/com/hereliesaz/cuedetat/domain/ArFlowReducerTest.kt
  git commit -m "feat: tableOverlayConfidence field; auto-advance AR_SETUP → AR_ACTIVE at 0.8 threshold"
  ```

---

## Task 10 — Dispatch `ArTrackingLost` on tracking loss (`ArCoreBackground.kt`)

**Files:** Modify `app/src/main/java/com/hereliesaz/cuedetat/data/ArCoreBackground.kt`

- [ ] **Step 1: Add tracking state tracking and dispatch**

  In the `AndroidView` factory / per-frame renderer callback that reads `camera.trackingState`, add state tracking:

  Find the per-frame callback (the lambda passed to `ArFrameProcessor` or the `GLSurfaceView.Renderer.onDrawFrame`). Add a field to track the previous state:
  ```kotlin
  var previousTrackingState: com.google.ar.core.TrackingState? = null
  ```
  Then in the per-frame callback, after reading `camera.trackingState`:
  ```kotlin
  val currentTracking = camera.trackingState
  if (previousTrackingState == com.google.ar.core.TrackingState.TRACKING &&
      currentTracking == com.google.ar.core.TrackingState.PAUSED) {
      onEvent(MainScreenEvent.ArTrackingLost)
  }
  previousTrackingState = currentTracking
  ```

  Note: `ArCoreBackground.kt` already has access to `onEvent` — confirm at the call site at line ~103.

- [ ] **Step 2: Compile check**
  ```
  ./gradlew :app:compileDebugKotlin
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
  ```bash
  git add app/src/main/java/com/hereliesaz/cuedetat/data/ArCoreBackground.kt
  git commit -m "feat: dispatch ArTrackingLost when ARCore tracking state drops to PAUSED"
  ```

---

## Task 11 — AR buttons in nav rail (`AzNavRailMenu.kt`)

**Files:** Modify `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/AzNavRailMenu.kt`

This file is the CueDetat app composable — not the AzNavRail library under `/home/az/StudioProjects/AzNavRail/`.

- [ ] **Step 1: Update the AR toggle button**

  Find the existing AR toggle (`azRailToggle` for "ar"). Replace with logic that:
  - Shows as "active/lit" when `cameraMode == AR_SETUP || cameraMode == AR_ACTIVE`
  - Only emits `CycleCameraMode` when in `OFF` or `CAMERA_ONLY` state

  ```kotlin
  val isArActive = uiState.cameraMode == CameraMode.AR_SETUP || uiState.cameraMode == CameraMode.AR_ACTIVE
  azRailToggle(
      id = "ar",
      isChecked = isArActive,
      toggleOnText = "AR", toggleOffText = "AR",
      fillColor = b3R, textColor = Color.White,
      onClick = {
          if (!isArActive) onEvent(MainScreenEvent.CycleCameraMode)
      }
  )
  ```

- [ ] **Step 2: Add Cancel button (visible in AR_SETUP)**

  After the AR button, add (or inside the `onscreen` block):
  ```kotlin
  if (uiState.cameraMode == CameraMode.AR_SETUP) {
      azRailItem(
          id = "ar_cancel",
          text = "Cancel",
          fillColor = /* subtle red */ Color(0xFF8B0000),
          textColor = Color.White,
          onClick = { onEvent(MainScreenEvent.CancelArSetup) }
      )
  }
  ```

- [ ] **Step 3: Add Off button (visible in CAMERA_ONLY)**

  ```kotlin
  if (uiState.cameraMode == CameraMode.CAMERA_ONLY) {
      azRailItem(
          id = "camera_off",
          text = "Off",
          fillColor = b12P,  // use existing dark palette color
          textColor = Color.White,
          onClick = { onEvent(MainScreenEvent.TurnCameraOff) }
      )
  }
  ```

- [ ] **Step 4: Compile check**
  ```
  ./gradlew :app:compileDebugKotlin
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**
  ```bash
  git add app/src/main/java/com/hereliesaz/cuedetat/ui/composables/AzNavRailMenu.kt
  git commit -m "feat: AR nav rail — Cancel in AR_SETUP, Off in CAMERA_ONLY, gated AR button"
  ```

---

## Task 12 — AR wizard overlay (`ArStatusOverlay.kt`)

**Files:** Modify `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/overlays/ArStatusOverlay.kt`

- [ ] **Step 1: Replace `ArSetupPrompt` with a wizard step composable**

  Delete the existing `ArSetupPrompt` body and replace with:

  ```kotlin
  @Composable
  fun ArSetupPrompt(
      visible: Boolean,
      lockedHsvColor: FloatArray?,
      tableScanModel: TableScanModel?,
      modifier: Modifier = Modifier
  ) {
      val arSetupStep = when {
          lockedHsvColor == null -> ArSetupStep.PICK_COLOR
          tableScanModel == null -> ArSetupStep.SCAN_TABLE
          else -> ArSetupStep.VERIFY
      }

      AnimatedVisibility(
          visible = visible,
          enter = fadeIn(tween(400)),
          exit = fadeOut(tween(300)),
          modifier = modifier.fillMaxSize()
      ) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Column(
                  modifier = Modifier
                      .clip(RoundedCornerShape(16.dp))
                      .background(Color.Black.copy(alpha = 0.75f))
                      .padding(horizontal = 28.dp, vertical = 24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
              ) {
                  Text("AR Setup", color = MaterialTheme.colorScheme.primary,
                      fontSize = 20.sp, fontWeight = FontWeight.Bold)
                  Spacer(modifier = Modifier.height(16.dp))
                  WizardStep(
                      number = "1",
                      text = "Lock felt color — tap the table surface",
                      state = if (lockedHsvColor != null) WizardStepState.DONE else WizardStepState.ACTIVE
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  WizardStep(
                      number = "2",
                      text = "Point camera at the table",
                      state = when {
                          tableScanModel != null -> WizardStepState.DONE
                          lockedHsvColor != null -> WizardStepState.ACTIVE
                          else -> WizardStepState.PENDING
                      }
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  WizardStep(
                      number = "3",
                      text = "Verifying alignment…",
                      state = when {
                          arSetupStep == ArSetupStep.VERIFY -> WizardStepState.ACTIVE
                          else -> WizardStepState.PENDING
                      }
                  )
              }
          }
      }
  }

  private enum class WizardStepState { PENDING, ACTIVE, DONE }

  @Composable
  private fun WizardStep(number: String, text: String, state: WizardStepState) {
      val (bgColor, textColor, numberText) = when (state) {
          WizardStepState.DONE    -> Triple(Color(0xFF1B5E20), Color(0xFFA5D6A7), "✓")
          WizardStepState.ACTIVE  -> Triple(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), Color.White, number)
          WizardStepState.PENDING -> Triple(Color(0xFF333333), Color(0xFF777777), number)
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
              modifier = Modifier.size(24.dp).clip(CircleShape).background(bgColor),
              contentAlignment = Alignment.Center
          ) {
              Text(numberText, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
          Spacer(modifier = Modifier.width(10.dp))
          Text(
              text = text,
              color = if (state == WizardStepState.PENDING) Color(0xFF777777) else Color.White,
              fontSize = 13.sp,
              textDecoration = if (state == WizardStepState.DONE)
                  androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
              lineHeight = 18.sp
          )
      }
  }
  ```

- [ ] **Step 2: Update call site in `ProtractorScreen.kt`**

  Find where `ArSetupPrompt(visible = ...)` is called and add the new parameters:
  ```kotlin
  ArSetupPrompt(
      visible = uiState.cameraMode == CameraMode.AR_SETUP,
      lockedHsvColor = uiState.lockedHsvColor,
      tableScanModel = uiState.tableScanModel
  )
  ```
  Also update the existing visibility condition (was `CameraMode.AR && tableScanModel == null` → now just `CameraMode.AR_SETUP`).

- [ ] **Step 3: Compile check**
  ```
  ./gradlew :app:compileDebugKotlin
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Run all tests**
  ```
  ./gradlew :app:testDebugUnitTest
  ```
  Expected: all tests PASS

- [ ] **Step 5: Commit**
  ```bash
  git add app/src/main/java/com/hereliesaz/cuedetat/ui/composables/overlays/ArStatusOverlay.kt \
          app/src/main/java/com/hereliesaz/cuedetat/ui/ProtractorScreen.kt
  git commit -m "feat: replace ArSetupPrompt with wizard step display (PICK_COLOR → SCAN → VERIFY)"
  ```

---

## Final Verification

- [ ] **Full compile + test run**
  ```
  ./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest
  ```
  Expected: `BUILD SUCCESSFUL`, all tests PASS

- [ ] **Visual checks (on device or emulator)**

  Build and install debug APK:
  ```
  ./gradlew :app:installDebug
  ```

  | Scenario | What to verify |
  |----------|---------------|
  | Dynamic beginner mode | Two-finger swipe does NOT pan the view; single-finger drags the target ball; rotation works |
  | Static beginner (level phone) | Direction lines appear below ball circles; center dots are above lines; glow is outward-only (circle interior is transparent); bubble center dot (hollow) overlaps static center dot (solid) |
  | Static beginner (tilted phone) | Bubble drifts; bubble center dot (hollow) moves away from static center dot (solid); lines still drawn below |
  | AR: tap AR button from OFF | Wizard overlay appears; step 1 is active |
  | AR: lock color | Step 1 shows ✓; step 2 becomes active |
  | AR: cancel setup | Nav rail shows Off button; camera stays on |
  | AR: tap Off | Camera turns off; returns to OFF state |
  | AR: screen-off resume | Wizard restarts at step 2 (color step shows ✓ and crossed through) |
