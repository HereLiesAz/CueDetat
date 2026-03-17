# Constrained Pocket Warping Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace independent pocket dragging in QuickAlign with live TPS-constrained warping, and apply the resulting residual TPS to the overlay rendering and CV pipeline for accurate lens distortion correction.

**Architecture:** A pure-Kotlin TPS solver (`ThinPlateSpline`) computes two 9×9 linear systems (one per axis). During alignment, a live image-space TPS predicts unpinned pocket positions as the user drags. At finish, a residual TPS (homography-estimated logical → true logical) is stored in `CueDetatState` and applied per-point in renderers and the CV pipeline.

**Tech Stack:** Kotlin, OpenCV (`Core.perspectiveTransform`, `Calib3d.findHomography`), Android Canvas, Jetpack Compose, JUnit4

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `domain/TpsWarpData.kt` | Create | Serializable control-point data class |
| `domain/ThinPlateSpline.kt` | Create | TPS solver + forward/inverse evaluator + runtime cache |
| `view/renderer/TpsUtils.kt` | Create | `PointF.warpedBy` extension |
| `test/.../ThinPlateSplineTest.kt` | Create | Unit tests for TPS math |
| `domain/UiModel.kt` | Modify | Add `CameraMode`, `lensWarpTps`, update `ApplyQuickAlign` |
| `domain/reducers/ControlReducer.kt` | Modify | Store `lensWarpTps` in `ApplyQuickAlign` handler |
| `ui/composables/quickalign/QuickAlignViewModel.kt` | Modify | TPS drag, pin set, residual TPS output |
| `ui/composables/quickalign/QuickAlignScreen.kt` | Modify | Pinned/unpinned visuals, `onDragEnd` hook |
| `view/renderer/table/TableRenderer.kt` | Modify | Warp draw points through inverse TPS |
| `view/renderer/table/RailRenderer.kt` | Modify | Warp corners + recompute normals from warped edges |
| `view/renderer/line/LineRenderer.kt` | Modify | Warp aiming/tangent line endpoints |
| `data/VisionRepository.kt` | Modify | Apply forward TPS after logical coordinate conversion |

---

## Task 1: TpsWarpData.kt — Data model

**Files:**
- Create: `app/src/main/java/com/hereliesaz/cuedetat/domain/TpsWarpData.kt`

- [ ] **Step 1: Create `TpsWarpData.kt`**

```kotlin
// app/src/main/java/com/hereliesaz/cuedetat/domain/TpsWarpData.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import androidx.annotation.Keep

/**
 * Serializable TPS residual control points.
 *
 * srcPoints: 6 homography-estimated logical positions (where H maps image points to)
 * dstPoints: 6 true logical positions (known from table model)
 *
 * Used in two directions:
 * - Forward (src→dst): CV pipeline — corrects homography-estimated logical → true logical
 * - Inverse (dst→src): Rendering — corrects true logical point for drawing inside pitchMatrix
 *
 * Weights are NOT stored here. ThinPlateSpline solves them lazily and caches in a WeakHashMap.
 *
 * Serialized via Gson default reflection. PointF has public float fields x/y which Gson handles
 * without a custom adapter (same implicit behavior used by viewOffset, bankingAimTarget, etc. in
 * CueDetatState). Note: if Gson is replaced with a stricter library, a PointF adapter will be needed.
 */
@Keep
data class TpsWarpData(
    val srcPoints: List<PointF>,
    val dstPoints: List<PointF>
)
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/domain/TpsWarpData.kt
git commit -m "feat: add TpsWarpData serializable control-point model"
```

---

## Task 2: ThinPlateSpline.kt — Solver + tests

**Files:**
- Create: `app/src/main/java/com/hereliesaz/cuedetat/domain/ThinPlateSpline.kt`
- Create: `app/src/test/java/com/hereliesaz/cuedetat/domain/ThinPlateSplineTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// app/src/test/java/com/hereliesaz/cuedetat/domain/ThinPlateSplineTest.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import org.junit.Assert.assertEquals
import org.junit.Test

class ThinPlateSplineTest {

    private fun assertPointF(expected: PointF, actual: PointF, delta: Float = 0.05f) {
        assertEquals("x mismatch", expected.x, actual.x, delta)
        assertEquals("y mismatch", expected.y, actual.y, delta)
    }

    private fun sixPts(vararg coords: Float): List<PointF> {
        require(coords.size == 12)
        return (0 until 6).map { PointF(coords[it * 2], coords[it * 2 + 1]) }
    }

    @Test
    fun `identity warp - src equals dst - point maps to itself`() {
        val pts = sixPts(0f, 0f, 36f, 0f, 36f, 72f, 0f, 72f, -1f, 36f, 37f, 36f)
        val tps = TpsWarpData(srcPoints = pts, dstPoints = pts)
        assertPointF(PointF(18f, 36f), ThinPlateSpline.applyWarp(tps, PointF(18f, 36f)))
        assertPointF(PointF(5f, 10f), ThinPlateSpline.applyWarp(tps, PointF(5f, 10f)))
    }

    @Test
    fun `pure translation warp - all control points shifted - interior point shifts equally`() {
        val src = sixPts(0f, 0f, 36f, 0f, 36f, 72f, 0f, 72f, -1f, 36f, 37f, 36f)
        val dst = src.map { PointF(it.x + 5f, it.y + 3f) }
        val tps = TpsWarpData(srcPoints = src, dstPoints = dst)
        assertPointF(PointF(23f, 39f), ThinPlateSpline.applyWarp(tps, PointF(18f, 36f)))
    }

    @Test
    fun `applyWarp passes through all six control points exactly`() {
        val src = sixPts(0f, 0f, 36f, 0f, 36f, 72f, 0f, 72f, -1f, 36f, 37f, 36f)
        // Simulate small lens distortion residuals
        val dst = sixPts(0.4f, 0.3f, 35.8f, 0.2f, 35.9f, 72.1f, 0.2f, 71.9f, -0.8f, 36.1f, 37.1f, 35.9f)
        val tps = TpsWarpData(srcPoints = src, dstPoints = dst)
        for (i in src.indices) {
            assertPointF(dst[i], ThinPlateSpline.applyWarp(tps, src[i]), delta = 0.1f)
        }
    }

    @Test
    fun `applyInverseWarp passes through dst control points`() {
        val src = sixPts(0f, 0f, 36f, 0f, 36f, 72f, 0f, 72f, -1f, 36f, 37f, 36f)
        val dst = sixPts(0.4f, 0.3f, 35.8f, 0.2f, 35.9f, 72.1f, 0.2f, 71.9f, -0.8f, 36.1f, 37.1f, 35.9f)
        val tps = TpsWarpData(srcPoints = src, dstPoints = dst)
        for (i in dst.indices) {
            assertPointF(src[i], ThinPlateSpline.applyInverseWarp(tps, dst[i]), delta = 0.1f)
        }
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL (class doesn't exist)**

```bash
cd /home/az/StudioProjects/CueDetat
./gradlew :app:testDebugUnitTest --tests "com.hereliesaz.cuedetat.domain.ThinPlateSplineTest" 2>&1 | tail -20
```

Expected: compilation error — `ThinPlateSpline` not found.

- [ ] **Step 3: Create `ThinPlateSpline.kt`**

```kotlin
// app/src/main/java/com/hereliesaz/cuedetat/domain/ThinPlateSpline.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import kotlin.math.ln

/**
 * Thin-Plate Spline solver and evaluator.
 *
 * For N control points, solves two independent (N+3)×(N+3) linear systems
 * (one per output axis x, y) using Gaussian elimination with partial pivoting.
 *
 * Weights are cached at runtime in a WeakHashMap keyed by TpsWarpData instance.
 * Both forward (src→dst) and inverse (dst→src) directions are available
 * from a single stored TpsWarpData.
 */
object ThinPlateSpline {

    private data class SolvedTps(
        val srcPoints: List<PointF>,
        val weightsX: DoubleArray,
        val weightsY: DoubleArray
    )

    private val forwardCache = java.util.WeakHashMap<TpsWarpData, SolvedTps>()
    private val inverseCache = java.util.WeakHashMap<TpsWarpData, SolvedTps>()
    private val lock = Any()

    /**
     * Maps src→dst (forward direction).
     * CV use: homography-estimated logical → true logical.
     * Image-space drag use: ideal image position → warped image position.
     */
    fun applyWarp(tps: TpsWarpData, point: PointF): PointF {
        val solved = synchronized(lock) {
            forwardCache.getOrPut(tps) { solve(tps.srcPoints, tps.dstPoints) }
        }
        return evaluate(solved, point)
    }

    /**
     * Maps dst→src (inverse direction, solved by swapping src/dst).
     * Rendering use: true logical point → homography-estimated logical (for drawing inside pitchMatrix).
     */
    fun applyInverseWarp(tps: TpsWarpData, point: PointF): PointF {
        val solved = synchronized(lock) {
            inverseCache.getOrPut(tps) { solve(tps.dstPoints, tps.srcPoints) }
        }
        return evaluate(solved, point)
    }

    internal fun solve(srcPoints: List<PointF>, dstPoints: List<PointF>): SolvedTps {
        val n = srcPoints.size
        val m = n + 3
        val A = Array(m) { DoubleArray(m) }
        val bX = DoubleArray(m)
        val bY = DoubleArray(m)

        // Kernel block: U(r²) where U(r²) = r² * ln(r²)
        for (i in 0 until n) {
            for (j in 0 until n) {
                val dx = (srcPoints[i].x - srcPoints[j].x).toDouble()
                val dy = (srcPoints[i].y - srcPoints[j].y).toDouble()
                A[i][j] = tpsKernel(dx * dx + dy * dy)
            }
        }

        // Affine block P and P^T
        for (i in 0 until n) {
            A[i][n] = 1.0; A[i][n + 1] = srcPoints[i].x.toDouble(); A[i][n + 2] = srcPoints[i].y.toDouble()
            A[n][i] = 1.0; A[n + 1][i] = srcPoints[i].x.toDouble(); A[n + 2][i] = srcPoints[i].y.toDouble()
            bX[i] = dstPoints[i].x.toDouble()
            bY[i] = dstPoints[i].y.toDouble()
        }
        // Bottom-right 3×3 is zero (already initialized)

        val (wX, wY) = solveLinearSystem(A, bX, bY)
        return SolvedTps(srcPoints, wX, wY)
    }

    private fun evaluate(solved: SolvedTps, point: PointF): PointF {
        val n = solved.srcPoints.size
        // Affine terms: weights[n], weights[n+1], weights[n+2]
        var x = solved.weightsX[n] + solved.weightsX[n + 1] * point.x + solved.weightsX[n + 2] * point.y
        var y = solved.weightsY[n] + solved.weightsY[n + 1] * point.x + solved.weightsY[n + 2] * point.y
        for (i in 0 until n) {
            val dx = (point.x - solved.srcPoints[i].x).toDouble()
            val dy = (point.y - solved.srcPoints[i].y).toDouble()
            val u = tpsKernel(dx * dx + dy * dy)
            x += solved.weightsX[i] * u
            y += solved.weightsY[i] * u
        }
        return PointF(x.toFloat(), y.toFloat())
    }

    private fun tpsKernel(r2: Double): Double =
        if (r2 < 1e-10) 0.0 else r2 * ln(r2)

    /**
     * Solves [A | bX | bY] simultaneously via Gaussian elimination with partial pivoting.
     * Returns (weightsX, weightsY) each of length n+3.
     */
    internal fun solveLinearSystem(
        A: Array<DoubleArray>,
        bX: DoubleArray,
        bY: DoubleArray
    ): Pair<DoubleArray, DoubleArray> {
        val n = A.size
        // Augmented matrix [A | bX | bY]
        val aug = Array(n) { i ->
            DoubleArray(n + 2).also { row ->
                A[i].copyInto(row)
                row[n] = bX[i]
                row[n + 1] = bY[i]
            }
        }

        // Forward elimination
        for (col in 0 until n) {
            var maxRow = col
            for (row in col + 1 until n) {
                if (Math.abs(aug[row][col]) > Math.abs(aug[maxRow][col])) maxRow = row
            }
            val tmp = aug[col]; aug[col] = aug[maxRow]; aug[maxRow] = tmp

            val pivot = aug[col][col]
            if (Math.abs(pivot) < 1e-12) continue

            for (row in col + 1 until n) {
                val factor = aug[row][col] / pivot
                for (k in col until n + 2) aug[row][k] -= factor * aug[col][k]
            }
        }

        // Back substitution
        val resultX = DoubleArray(n)
        val resultY = DoubleArray(n)
        for (i in n - 1 downTo 0) {
            var sumX = aug[i][n]
            var sumY = aug[i][n + 1]
            for (j in i + 1 until n) {
                sumX -= aug[i][j] * resultX[j]
                sumY -= aug[i][j] * resultY[j]
            }
            resultX[i] = sumX / aug[i][i]
            resultY[i] = sumY / aug[i][i]
        }

        return Pair(resultX, resultY)
    }
}
```

- [ ] **Step 4: Run tests — expect PASS**

```bash
./gradlew :app:testDebugUnitTest --tests "com.hereliesaz.cuedetat.domain.ThinPlateSplineTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/domain/ThinPlateSpline.kt \
        app/src/test/java/com/hereliesaz/cuedetat/domain/ThinPlateSplineTest.kt
git commit -m "feat: add ThinPlateSpline solver with unit tests"
```

---

## Task 3: TpsUtils.kt — warpedBy extension

**Files:**
- Create: `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/TpsUtils.kt`

- [ ] **Step 1: Create `TpsUtils.kt`**

```kotlin
// app/src/main/java/com/hereliesaz/cuedetat/view/renderer/TpsUtils.kt
package com.hereliesaz.cuedetat.view.renderer

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.ThinPlateSpline
import com.hereliesaz.cuedetat.domain.TpsWarpData

/**
 * Applies the inverse residual TPS to a logical draw point.
 * Used inside canvas.withMatrix(pitchMatrix) blocks to correct for lens distortion.
 * If tps is null (no alignment performed), returns this unchanged.
 */
fun PointF.warpedBy(tps: TpsWarpData?): PointF =
    if (tps == null) this else ThinPlateSpline.applyInverseWarp(tps, this)
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/view/renderer/TpsUtils.kt
git commit -m "feat: add PointF.warpedBy TPS rendering extension"
```

---

## Task 4: State + Event + Reducer

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/UiModel.kt`
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ControlReducer.kt`

- [ ] **Step 1: Add `lensWarpTps` to `CueDetatState` in `UiModel.kt`**

Find the block of state fields (around line 54, after `isCameraVisible` → `cameraMode`) and add:

```kotlin
// After: val cameraMode: CameraMode = CameraMode.CAMERA,
val lensWarpTps: TpsWarpData? = null,
```

Also add the import at the top of the file (if not auto-imported):
```kotlin
import com.hereliesaz.cuedetat.domain.TpsWarpData
```

- [ ] **Step 2: Update `ApplyQuickAlign` event signature in `UiModel.kt`**

Find (around line 208):
```kotlin
data class ApplyQuickAlign(val translation: Offset, val rotation: Float, val scale: Float) :
    MainScreenEvent()
```

Replace with:
```kotlin
data class ApplyQuickAlign(
    val translation: Offset,
    val rotation: Float,
    val scale: Float,
    val tpsWarpData: TpsWarpData
) : MainScreenEvent()
```

- [ ] **Step 3: Update `ApplyQuickAlign` handler in `ControlReducer.kt`**

Find (line 53):
```kotlin
is MainScreenEvent.ApplyQuickAlign -> {
    val (minZoom, maxZoom) = ZoomMapping.getZoomRange(state.experienceMode)
    val newZoomSliderPos = ZoomMapping.zoomToSlider(action.scale, minZoom, maxZoom)
    state.copy(
        viewOffset = PointF(action.translation.x, action.translation.y),
        worldRotationDegrees = action.rotation,
        zoomSliderPosition = newZoomSliderPos,
        isWorldLocked = true,
        valuesChangedSinceReset = true
    )
}
```

Replace with:
```kotlin
is MainScreenEvent.ApplyQuickAlign -> {
    val (minZoom, maxZoom) = ZoomMapping.getZoomRange(state.experienceMode)
    val newZoomSliderPos = ZoomMapping.zoomToSlider(action.scale, minZoom, maxZoom)
    state.copy(
        viewOffset = PointF(action.translation.x, action.translation.y),
        worldRotationDegrees = action.rotation,
        zoomSliderPosition = newZoomSliderPos,
        lensWarpTps = action.tpsWarpData,
        isWorldLocked = true,
        valuesChangedSinceReset = true
    )
}
```

- [ ] **Step 4: Build — expect compilation success**

```bash
./gradlew :app:assembleDebug 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL` (or errors only in `QuickAlignViewModel` which we haven't updated yet — that's OK, fix in next task).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/domain/UiModel.kt \
        app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ControlReducer.kt
git commit -m "feat: add lensWarpTps to state and ApplyQuickAlign event"
```

---

## Task 5: QuickAlignViewModel — TPS drag + residual output

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/quickalign/QuickAlignViewModel.kt`

Key changes:
1. Track pinned pockets (set of `DraggablePocket`)
2. Store ideal positions (set on photo capture, never changed)
3. Replace `onPocketDrag` with TPS-based prediction using a cancellable coroutine
4. Add `onPocketReleased` to pin a pocket
5. Replace `onFinishAlign` with 6-point homography + residual TPS
6. Update `onResetPoints` to clear pin set

**Important: `Table.pockets` index order** (from `Table.kt` lines 39–43):
- `[0]` = TL, `[1]` = TR, `[2]` = **BL**, `[3]` = **BR**, `[4]` = SL, `[5]` = SR
- Note the BL/BR swap — index 2 is BL, index 3 is BR (not the intuitive order)

- [ ] **Step 1: Add new imports to `QuickAlignViewModel.kt`**

Add to the import block:
```kotlin
import com.hereliesaz.cuedetat.domain.ThinPlateSpline
import com.hereliesaz.cuedetat.domain.TpsWarpData
import com.hereliesaz.cuedetat.view.model.Table
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import org.opencv.core.Core
```

- [ ] **Step 2: Add new state fields (after `_alignResult` declaration, around line 71)**

```kotlin
// Ideal (undistorted) pocket positions — set once when photo is captured.
private var _idealPositions: Map<DraggablePocket, Offset> = emptyMap()

// Which pockets the user has explicitly placed (released a drag on).
private val _pinnedPockets = MutableStateFlow<Set<DraggablePocket>>(emptySet())
val pinnedPockets = _pinnedPockets.asStateFlow()

// Cancellable job for background TPS prediction during drag.
private var dragJob: Job? = null
```

- [ ] **Step 3: Update `initializePocketPositions` to save ideal positions**

Replace the entire `initializePocketPositions` function (lines 98–115):

```kotlin
private fun initializePocketPositions(size: IntSize) {
    val padding = 0.2f
    val positions = mapOf(
        DraggablePocket.TOP_LEFT     to Offset(size.width * padding,       size.height * padding),
        DraggablePocket.TOP_RIGHT    to Offset(size.width * (1 - padding), size.height * padding),
        DraggablePocket.BOTTOM_RIGHT to Offset(size.width * (1 - padding), size.height * (1 - padding)),
        DraggablePocket.BOTTOM_LEFT  to Offset(size.width * padding,       size.height * (1 - padding)),
        DraggablePocket.SIDE_LEFT    to Offset(size.width * padding,       size.height * 0.5f),
        DraggablePocket.SIDE_RIGHT   to Offset(size.width * (1 - padding), size.height * 0.5f)
    )
    _pocketPositions.value = positions
    _idealPositions = positions  // Save for TPS reference
}
```

- [ ] **Step 4: Replace `onPocketDrag` (lines 121–153) with TPS version**

```kotlin
/**
 * Updates the dragged pocket immediately for responsiveness, then predicts
 * all unpinned pockets on a background thread using TPS. Cancels any
 * in-progress prediction (replace-on-new strategy).
 */
fun onPocketDrag(pocket: DraggablePocket, newPosition: Offset) {
    // Update dragged pocket immediately.
    val current = _pocketPositions.value.toMutableMap()
    current[pocket] = newPosition
    _pocketPositions.value = current

    dragJob?.cancel()

    val idealPositions = _idealPositions
    val allPockets = DraggablePocket.values().toList()

    // Build constraint set: all pinned pockets + currently dragged pocket.
    val constrained = buildMap<DraggablePocket, Offset> {
        _pinnedPockets.value.forEach { p -> current[p]?.let { put(p, it) } }
        put(pocket, newPosition)  // Dragged overrides pinned if same pocket.
    }

    dragJob = viewModelScope.launch {
        val unpinned = allPockets - constrained.keys
        if (unpinned.isEmpty()) return@launch

        val predicted = withContext(Dispatchers.Default) {
            val srcPts = constrained.entries.map { (p, _) ->
                android.graphics.PointF(idealPositions[p]!!.x, idealPositions[p]!!.y)
            }
            val dstPts = constrained.entries.map { (_, pos) ->
                android.graphics.PointF(pos.x, pos.y)
            }
            val imageTps = TpsWarpData(srcPoints = srcPts, dstPoints = dstPts)
            unpinned.associateWith { p ->
                val ideal = idealPositions[p]!!
                val result = ThinPlateSpline.applyWarp(imageTps, android.graphics.PointF(ideal.x, ideal.y))
                Offset(result.x, result.y)
            }
        }

        val updated = _pocketPositions.value.toMutableMap()
        predicted.forEach { (p, pos) -> updated[p] = pos }
        _pocketPositions.value = updated
    }
}

/**
 * Pins the pocket at its current position. Called on drag end from the screen.
 */
fun onPocketReleased(pocket: DraggablePocket) {
    _pinnedPockets.value = _pinnedPockets.value + pocket
}
```

- [ ] **Step 5: Also remove the now-unused `getClosestPointOnSegment` function (lines 158–168)**

Delete the `getClosestPointOnSegment` private function entirely — it is no longer called.

- [ ] **Step 6: Replace `onFinishAlign` (lines 174–214) with 6-point homography + residual TPS**

```kotlin
/**
 * Computes homography from all 6 pocket positions, decomposes it to
 * translation/rotation/scale, then computes the residual TPS from the
 * homography-estimated logical positions to the true logical positions.
 *
 * Note on Table.pockets index order: [0]=TL, [1]=TR, [2]=BL, [3]=BR, [4]=SL, [5]=SR
 * The BL/BR indices are swapped from the intuitive order — [2] is BL, [3] is BR.
 */
fun onFinishAlign() {
    val imagePoints = _pocketPositions.value
    val tableSize = _selectedTableSize.value
    val image = _capturedBitmap.value
    if (imagePoints.size != 6 || tableSize == null || image == null) return

    viewModelScope.launch {
        val logicalTable = Table(tableSize, true)

        // Map DraggablePocket → Table.pockets index (note BL=2, BR=3 swap)
        val pocketOrder = listOf(
            DraggablePocket.TOP_LEFT,
            DraggablePocket.TOP_RIGHT,
            DraggablePocket.BOTTOM_RIGHT,
            DraggablePocket.BOTTOM_LEFT,
            DraggablePocket.SIDE_LEFT,
            DraggablePocket.SIDE_RIGHT
        )
        val logicalOrder = listOf(
            logicalTable.pockets[0], // TL
            logicalTable.pockets[1], // TR
            logicalTable.pockets[3], // BR (index 3, not 2!)
            logicalTable.pockets[2], // BL (index 2, not 3!)
            logicalTable.pockets[4], // SL
            logicalTable.pockets[5]  // SR
        )

        val imagePts = pocketOrder.map { imagePoints[it]!! }

        val srcMat = MatOfPoint2f()
        srcMat.fromList(imagePts.map { Point(it.x.toDouble(), it.y.toDouble()) })

        val dstMat = MatOfPoint2f()
        dstMat.fromList(logicalOrder.map { Point(it.x.toDouble(), it.y.toDouble()) })

        val homography = Calib3d.findHomography(srcMat, dstMat, Calib3d.RANSAC, 5.0)

        if (!homography.empty()) {
            val (translation, rotation, scale) = decomposeHomography(
                homography, image.width.toFloat(), image.height.toFloat()
            )

            // Homography-estimated logical positions: H * image_pts
            val estimatedDst = MatOfPoint2f()
            Core.perspectiveTransform(srcMat, estimatedDst, homography)
            val estimatedLogical = estimatedDst.toList()
                .map { android.graphics.PointF(it.x.toFloat(), it.y.toFloat()) }

            // True logical positions
            val trueLogical = logicalOrder.map { android.graphics.PointF(it.x, it.y) }

            // Residual TPS: estimated logical → true logical.
            // NOTE: Both srcPoints and dstPoints are in LOGICAL space (not image space).
            // The spec's TpsWarpData field comments saying "image-space" are incorrect — this
            // logical-space formulation is what the rest of the pipeline (Tasks 7–9) expects.
            val tpsWarpData = TpsWarpData(srcPoints = estimatedLogical, dstPoints = trueLogical)

            _alignResult.emit(MainScreenEvent.ApplyQuickAlign(translation, rotation, scale, tpsWarpData))
        }
        onResetPoints()
    }
}
```

- [ ] **Step 7: Update `onResetPoints` to clear pin set (lines 251–257)**

```kotlin
fun onResetPoints() {
    _pinnedPockets.value = emptySet()
    _selectedTableSize.value?.let {
        _capturedBitmap.value?.let { bmp ->
            initializePocketPositions(IntSize(bmp.width, bmp.height))
        }
    }
}
```

- [ ] **Step 8: Build to confirm no compilation errors**

```bash
./gradlew :app:assembleDebug 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/ui/composables/quickalign/QuickAlignViewModel.kt
git commit -m "feat: replace independent pocket drag with live TPS constraint propagation"
```

---

## Task 6: QuickAlignScreen — Pinned/unpinned visuals + drag end hook

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/quickalign/QuickAlignScreen.kt`

- [ ] **Step 1: Collect `pinnedPockets` from ViewModel in `AlignmentStep`**

In the `AlignmentStep` composable (around line 126), add:
```kotlin
val pinnedPockets by viewModel.pinnedPockets.collectAsState()
```

- [ ] **Step 2: Update `onDragEnd` and `onDragCancel` to call `onPocketReleased`**

The existing `detectDragGestures` block (lines 138–163) uses a local `draggedPocket` variable and already has `onDragEnd` and `onDragCancel`. Update both callbacks to call `viewModel.onPocketReleased` before nulling the variable:

```kotlin
onDragEnd = {
    draggedPocket?.let { viewModel.onPocketReleased(it) }
    draggedPocket = null
},
onDragCancel = {
    draggedPocket?.let { viewModel.onPocketReleased(it) }
    draggedPocket = null
},
```

Leave `onDragStart` and `onDrag` unchanged.

- [ ] **Step 3: Draw pinned pockets solid, unpinned pockets hollow**

The existing `Canvas` block (lines 193–205) draws each pocket as a hollow circle + center dot. Update it to show pinned pockets as solid and unpinned as hollow, while preserving the existing per-pocket radius logic (`30f` for corners, `20f` for side pockets):

```kotlin
pocketPositions.forEach { (pocket, pos) ->
    val isCorner = pocket != DraggablePocket.SIDE_LEFT && pocket != DraggablePocket.SIDE_RIGHT
    val radius = if (isCorner) 30f else 20f
    val isPinned = pocket in pinnedPockets
    // Outer circle: solid when pinned, hollow when unpinned (TPS-predicted).
    drawCircle(
        color = Color.Yellow,
        radius = radius,
        center = pos,
        style = if (isPinned) Fill else Stroke(width = 4.dp.toPx()),
        alpha = if (isPinned) 0.9f else 0.5f
    )
    // Center dot: always drawn.
    drawCircle(Color.Yellow, radius = 8f, center = pos, alpha = 0.9f)
}
```

- [ ] **Step 4: Build and verify alignment screen still works**

```bash
./gradlew :app:assembleDebug 2>&1 | grep -E "error:|BUILD"
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/ui/composables/quickalign/QuickAlignScreen.kt
git commit -m "feat: show pinned/unpinned pocket distinction in QuickAlign UI"
```

---

## Task 7: TableRenderer + RailRenderer — Warp draw points

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/table/TableRenderer.kt`
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/table/RailRenderer.kt`

Add `import com.hereliesaz.cuedetat.view.renderer.warpedBy` to both files.

**TableRenderer changes:**

- [ ] **Step 1: Warp corner and pocket points in `drawSurface` and `drawPockets`**

In `drawSurface` (line 44), wherever `state.table.corners` or pocket positions are used to draw lines/paths, wrap each `PointF` with `.warpedBy(state.lensWarpTps)`:

```kotlin
// Example for corners:
val tps = state.lensWarpTps
val c = state.table.corners.map { it.warpedBy(tps) }
// Use c[0], c[1], c[2], c[3] instead of state.table.corners[0..3]

// Example for diamond grid — interpolated points:
val p = interpolate(c[0], c[1], fraction)  // already uses warped corners
```

In `drawPockets` (line 104), warp each pocket center:
```kotlin
val tps = state.lensWarpTps
val pockets = state.table.pockets.map { it.warpedBy(tps) }
// Use pockets[i] instead of state.table.pockets[i]
```

**RailRenderer changes:**

- [ ] **Step 2: Warp corners and recompute normals from warped edges in `draw`**

At the start of the `draw` function (line 29), after obtaining corners, add:

```kotlin
val tps = state.lensWarpTps
val warpedCorners = state.table.corners.map { it.warpedBy(tps) }

// Recompute normals from warped corner edges (outward normals).
// Table corners order: [0]=TL, [1]=TR, [2]=BR, [3]=BL (clockwise).
// For each edge i→(i+1)%4, outward normal = perpendicular pointing away from center.
val tableCenter = PointF(
    warpedCorners.sumOf { it.x.toDouble() }.toFloat() / 4,
    warpedCorners.sumOf { it.y.toDouble() }.toFloat() / 4
)
val warpedNormals = (0 until 4).map { i ->
    val a = warpedCorners[i]
    val b = warpedCorners[(i + 1) % 4]
    val perp = normalize(PointF(-(b.y - a.y), b.x - a.x))
    val edgeMid = PointF((a.x + b.x) / 2f, (a.y + b.y) / 2f)
    // Choose the direction pointing away from center (outward).
    val toCenter = PointF(tableCenter.x - edgeMid.x, tableCenter.y - edgeMid.y)
    val dot = perp.x * toCenter.x + perp.y * toCenter.y
    if (dot <= 0f) perp else PointF(-perp.x, -perp.y)
}
```

Then use `warpedCorners` and `warpedNormals` throughout the `draw` function in place of `state.table.corners` and `state.table.normals`.

- [ ] **Step 3: Build**

```bash
./gradlew :app:assembleDebug 2>&1 | grep -E "error:|BUILD"
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/view/renderer/table/TableRenderer.kt \
        app/src/main/java/com/hereliesaz/cuedetat/view/renderer/table/RailRenderer.kt
git commit -m "feat: warp table and rail draw points through inverse residual TPS"
```

---

## Task 8: LineRenderer — Warp aiming line endpoints

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/view/renderer/line/LineRenderer.kt`

Add `import com.hereliesaz.cuedetat.view.renderer.warpedBy`.

- [ ] **Step 1: Warp aiming/tangent line start and end points**

In `drawAimingLines` (line 131) and `drawTangentLines` (line 183), wherever `start: PointF` and `end: PointF` are computed from state (e.g., `state.aimingLineEndPoint`, ball centers), wrap with `.warpedBy(state.lensWarpTps)`:

```kotlin
val tps = state.lensWarpTps
val start = state.protractorUnit.center.warpedBy(tps)
val end = state.aimingLineEndPoint?.warpedBy(tps) ?: return
```

Apply the same pattern in `drawBankingLines` (line 300) and `drawBankablePath` (line 381) for each point in the path:

```kotlin
val tps = state.lensWarpTps
val warpedPath = path.map { it.warpedBy(tps) }
```

- [ ] **Step 2: Build**

```bash
./gradlew :app:assembleDebug 2>&1 | grep -E "error:|BUILD"
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/view/renderer/line/LineRenderer.kt
git commit -m "feat: warp aiming line endpoints through inverse residual TPS"
```

---

## Task 9: VisionRepository — Forward TPS correction

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt`

Add `import com.hereliesaz.cuedetat.domain.ThinPlateSpline`.

- [ ] **Step 1: Apply forward TPS after logical coordinate conversion**

Find Step 10 (lines 306–315):

```kotlin
// Step 10: Map to Logical Space.
val detectedLogicalPoints = if (state.hasInverseMatrix) {
    val inverseMatrix = state.inversePitchMatrix ?: Matrix()
    refinedScreenPoints.map { screenPoint ->
        Perspective.screenToLogical(screenPoint, inverseMatrix)
    }
} else {
    emptyList()
}
```

Replace with:

```kotlin
// Step 10: Map to Logical Space.
val detectedLogicalPoints = if (state.hasInverseMatrix) {
    val inverseMatrix = state.inversePitchMatrix ?: Matrix()
    val tps = state.lensWarpTps
    refinedScreenPoints.map { screenPoint ->
        val logical = Perspective.screenToLogical(screenPoint, inverseMatrix)
        // Apply forward residual TPS: homography-estimated logical → true logical.
        if (tps != null) ThinPlateSpline.applyWarp(tps, logical) else logical
    }
} else {
    emptyList()
}
```

- [ ] **Step 2: Build**

```bash
./gradlew :app:assembleDebug 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Run all unit tests**

```bash
./gradlew :app:testDebugUnitTest 2>&1 | tail -20
```

Expected: all tests pass.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt
git commit -m "feat: apply forward residual TPS to CV ball detections"
```

---

## Final Integration Check

- [ ] **Build and install**

```bash
./gradlew :app:assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Smoke test QuickAlign**
  1. Open app → Expert mode
  2. Tap Table Alignment
  3. Select table size → capture photo of a table
  4. Drag one pocket — verify all other pockets shift consistently (not independently)
  5. Release (pocket pins solid) — drag a second pocket — verify remaining unpinned pockets predict
  6. Tap Reset — verify all pockets return to defaults and become hollow again
  7. Align all 6 pockets, tap Finish — overlay should align to table
  8. Verify overlay lines follow table geometry correctly

- [ ] **Final commit**

```bash
git commit --allow-empty -m "feat: constrained pocket warping with TPS lens distortion correction"
```
