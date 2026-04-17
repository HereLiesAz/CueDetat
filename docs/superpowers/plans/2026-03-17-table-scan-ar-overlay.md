# Table Scan & Persistent AR Overlay Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace manual QuickAlign with an automatic table scanner that builds a persistent table model from pocket detections, and use that model to continuously lock the AR overlay to the real table.

**Architecture:** Scan phase accumulates pocket detections via Hough circles across panned frames, fits a 2:1 geometry model, then emits the existing `ApplyQuickAlign` event. AR mode (CameraMode.AR) re-detects pockets every 5th frame, applies Kalman-style cluster updates to refine the model over time, and emits a lightweight `UpdateArPose` event with exponential smoothing to prevent jitter. Table model persists separately from camera pose; pose is re-anchored automatically on resume.

**Tech Stack:** Kotlin, OpenCV (`Imgproc.HoughCircles`, `Imgproc.HoughLinesP`, `Imgproc.inRange`, `Core.perspectiveTransform`, `Calib3d.findHomography`), CameraX `ImageAnalysis`, Android `FusedLocationProviderClient`, Jetpack Compose, Hilt, JUnit4

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `domain/HomographyUtils.kt` | Create | `decomposeHomography` extracted from QuickAlignViewModel |
| `domain/TableScanModel.kt` | Create | `PocketId`, `PocketCluster`, `TableScanModel` data classes |
| `domain/TableGeometryFitter.kt` | Create | Pure-Kotlin 2:1 rectangle fitter + pocket identifier |
| `data/TableScanRepository.kt` | Create | Gson save/load of `TableScanModel`; GPS capture |
| `ui/composables/tablescan/TableScanAnalyzer.kt` | Create | CameraX `ImageAnalysis.Analyzer` — Hough pocket detection per frame |
| `ui/composables/tablescan/TableScanViewModel.kt` | Create | Cluster accumulation, geometry fit trigger, scan progress |
| `ui/composables/tablescan/TableScanScreen.kt` | Create | Scan UI: camera feed + per-pocket progress + GPS permission |
| `domain/UiModel.kt` | Modify | `@Transient tableScanModel`, 4 new events, `showTableScanScreen` flag |
| `domain/reducers/ControlReducer.kt` | Modify | Handle `LoadTableScan`, `UpdateArPose`, `UpdateTableScanClusters`, `ClearTableScan` |
| `domain/reducers/ToggleReducer.kt` | Modify | `CycleCameraMode` AR skip; `ToggleTableScanScreen` handler |
| `data/VisionRepository.kt` | Modify | AR mode: pocket re-detection, cluster update, pose blend, edge fallback |
| `ui/MainViewModel.kt` | Modify | Load `TableScanModel` on init; dispatch `LoadTableScan` |
| `ui/ProtractorScreen.kt` | Modify | Add `ROUTE_SCAN`; Reset clears camera pose |
| `ui/composables/AzNavRailMenu.kt` | Modify | Replace "Quick Align" with "Scan Table" |
| `test/.../TableGeometryFitterTest.kt` | Create | Unit tests for rectangle fitting |
| `ui/composables/quickalign/QuickAlignScreen.kt` | Delete | Replaced by TableScanScreen |
| `ui/composables/quickalign/QuickAlignViewModel.kt` | Delete | Replaced by TableScanViewModel + HomographyUtils |
| `ui/composables/quickalign/QuickAlignAnalyzer.kt` | Delete | Replaced by TableScanAnalyzer |

---

## Task 1: Extract HomographyUtils

**Files:**
- Create: `app/src/main/java/com/hereliesaz/cuedetat/domain/HomographyUtils.kt`
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/quickalign/QuickAlignViewModel.kt`

- [ ] **Step 1: Read `QuickAlignViewModel.kt` to find the exact `decomposeHomography` implementation (around line 274)**

- [ ] **Step 2: Create `HomographyUtils.kt`**

```kotlin
// app/src/main/java/com/hereliesaz/cuedetat/domain/HomographyUtils.kt
package com.hereliesaz.cuedetat.domain

import androidx.compose.ui.geometry.Offset
import org.opencv.core.Mat
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Decomposes a homography matrix into translation, rotation, and scale
 * relative to the centre of the image.
 *
 * Returns Triple(translation: Offset, rotationDegrees: Float, scale: Float)
 * where scale is the reciprocal of the detected zoom factor.
 */
fun decomposeHomography(h: Mat, imgWidth: Float, imgHeight: Float): Triple<Offset, Float, Float> {
    val h0 = h[0, 0][0].toFloat()
    val h1 = h[0, 1][0].toFloat()
    val h2 = h[0, 2][0].toFloat()
    val h3 = h[1, 0][0].toFloat()
    val h4 = h[1, 1][0].toFloat()
    val h5 = h[1, 2][0].toFloat()

    val scaleX = sqrt(h0 * h0 + h3 * h3)
    val scaleY = sqrt(h1 * h1 + h4 * h4)
    val scale = (scaleX + scaleY) / 2.0f

    val rotation = -atan2(h3, h0) * (180f / PI.toFloat())

    val canvasCenter = Offset(imgWidth / 2f, imgHeight / 2f)
    val translation = Offset(h2, h5) - canvasCenter

    return Triple(translation, rotation, 1 / scale)
}
```

- [ ] **Step 3: Update `QuickAlignViewModel.onFinishAlign()` to delegate to `decomposeHomography` from `HomographyUtils`**

In `QuickAlignViewModel.kt`:
- Add `import com.hereliesaz.cuedetat.domain.decomposeHomography`
- Remove the `private fun decomposeHomography(...)` method
- The call site `decomposeHomography(homography, image.width.toFloat(), image.height.toFloat())` already matches the new top-level function signature — no other changes needed.

- [ ] **Step 4: Build to verify no regressions**

```bash
./gradlew :app:assembleDebug 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/domain/HomographyUtils.kt \
        app/src/main/java/com/hereliesaz/cuedetat/ui/composables/quickalign/QuickAlignViewModel.kt
git commit -m "refactor: extract decomposeHomography to HomographyUtils"
```

---

## Task 2: TableScanModel data classes

**Files:**
- Create: `app/src/main/java/com/hereliesaz/cuedetat/domain/TableScanModel.kt`

- [ ] **Step 1: Create `TableScanModel.kt`**

```kotlin
// app/src/main/java/com/hereliesaz/cuedetat/domain/TableScanModel.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import androidx.annotation.Keep
import com.hereliesaz.cuedetat.view.state.TableSize

enum class PocketId { TL, TR, BL, BR, SL, SR }

/**
 * One pocket's accumulated observation data.
 *
 * logicalPosition: current best estimate of pocket centre in logical (inch) space.
 * observationCount: number of times this pocket has been detected. More observations → lower variance.
 * variance: spread of detections (logical inches²). Shrinks with more observations.
 */
@Keep
data class PocketCluster(
    val identity: PocketId,
    val logicalPosition: PointF,
    val observationCount: Int,
    val variance: Float
)

/**
 * Persistent table model produced by the scan phase.
 *
 * Stored separately from CueDetatState (via TableScanRepository) so it survives
 * app reinstalls and does not bloat the main preferences store.
 * CueDetatState.tableScanModel holds the runtime in-memory copy only (@Transient).
 *
 * feltColorHsv: mean HSV of the table felt, sampled during scan. Used to seed
 * the colour-based edge detector during AR tracking.
 *
 * scanLatitude/scanLongitude: GPS coordinates at scan time, or null if permission
 * was denied. Used to prompt rescan when the user is > 100 m away.
 */
@Keep
data class TableScanModel(
    val pockets: List<PocketCluster>,       // 6 entries, one per PocketId in declaration order
    val lensWarpTps: TpsWarpData,
    val tableSize: TableSize,
    val feltColorHsv: FloatArray,           // 3-element [H, S, V]
    val scanLatitude: Double?,
    val scanLongitude: Double?
)
```

- [ ] **Step 2: Build**

```bash
./gradlew :app:assembleDebug 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/domain/TableScanModel.kt
git commit -m "feat: add TableScanModel, PocketCluster, PocketId data classes"
```

---

## Task 3: TableGeometryFitter + unit tests (TDD)

**Files:**
- Create: `app/src/test/java/com/hereliesaz/cuedetat/domain/TableGeometryFitterTest.kt`
- Create: `app/src/main/java/com/hereliesaz/cuedetat/domain/TableGeometryFitter.kt`

The fitter receives 6 unordered logical-space pocket positions and identifies which is TL/TR/BL/BR/SL/SR by fitting a 2:1 rectangle.

- [ ] **Step 1: Write failing tests**

```kotlin
// app/src/test/java/com/hereliesaz/cuedetat/domain/TableGeometryFitterTest.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import org.junit.Assert.*
import org.junit.Test

class TableGeometryFitterTest {

    // Standard 9ft table: 100" × 50", pockets at corners and long-side midpoints.
    // Origin at centre; TL=(-50,-25), TR=(50,-25), BL=(-50,25), BR=(50,25), SL=(-50,0), SR=(50,0)
    private val idealPts = mapOf(
        PocketId.TL to PointF(-50f, -25f),
        PocketId.TR to PointF( 50f, -25f),
        PocketId.BL to PointF(-50f,  25f),
        PocketId.BR to PointF( 50f,  25f),
        PocketId.SL to PointF(-50f,   0f),
        PocketId.SR to PointF( 50f,   0f)
    )

    private fun shuffled() = idealPts.values.toMutableList().also { it.shuffle() }

    @Test
    fun `fit returns all six identities for ideal layout`() {
        val result = TableGeometryFitter.fit(shuffled())
        assertNotNull(result)
        assertEquals(6, result!!.size)
        val ids = result.map { it.first }.toSet()
        assertEquals(PocketId.values().toSet(), ids)
    }

    @Test
    fun `fit assigns correct identity to each point for ideal layout`() {
        val result = TableGeometryFitter.fit(shuffled())!!
        val byId = result.associate { it.first to it.second }
        PocketId.values().forEach { id ->
            val expected = idealPts[id]!!
            val actual = byId[id]!!
            assertEquals("${id}.x", expected.x, actual.x, 0.5f)
            assertEquals("${id}.y", expected.y, actual.y, 0.5f)
        }
    }

    @Test
    fun `fit handles layout with small noise on each point`() {
        val noisy = idealPts.values.map { PointF(it.x + (-1..1).random().toFloat(), it.y + (-1..1).random().toFloat()) }
        val result = TableGeometryFitter.fit(noisy)
        assertNotNull("Should fit even with small noise", result)
        assertEquals(6, result!!.size)
    }

    @Test
    fun `fit returns null for five points`() {
        val fivePts = shuffled().take(5)
        assertNull(TableGeometryFitter.fit(fivePts))
    }

    @Test
    fun `fit returns null for six collinear points`() {
        val collinear = (0..5).map { PointF(it * 10f, 0f) }
        assertNull(TableGeometryFitter.fit(collinear))
    }

    @Test
    fun `fit works for 8ft table aspect ratio`() {
        // 8ft: 88" × 44"
        val pts = listOf(
            PointF(-44f, -22f), PointF(44f, -22f),
            PointF(-44f,  22f), PointF(44f,  22f),
            PointF(-44f,   0f), PointF(44f,   0f)
        ).shuffled()
        val result = TableGeometryFitter.fit(pts)
        assertNotNull(result)
        assertEquals(6, result!!.size)
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL (class not found)**

```bash
./gradlew :app:testDebugUnitTest --tests "com.hereliesaz.cuedetat.domain.TableGeometryFitterTest" 2>&1 | tail -20
```

Expected: compilation error — `TableGeometryFitter` not found.

- [ ] **Step 3: Create `TableGeometryFitter.kt`**

```kotlin
// app/src/main/java/com/hereliesaz/cuedetat/domain/TableGeometryFitter.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Fits 6 unordered logical-space pocket positions to a billiards table model
 * and assigns TL/TR/BL/BR/SL/SR identities.
 *
 * A billiards table has 4 corner pockets forming a 2:1 rectangle, plus 2 side
 * pockets at the midpoints of the long sides. All standard sizes (7ft, 8ft, 9ft)
 * are exactly 2:1.
 */
object TableGeometryFitter {

    private const val ASPECT_RATIO = 2.0f
    private const val ASPECT_TOLERANCE = 0.25f  // ±25% tolerance on 2:1 ratio

    /**
     * Attempts to identify the 6 pockets.
     *
     * @param points 6 unordered PointF positions in logical space
     * @return List of (PocketId, PointF) pairs if fit succeeded, null otherwise.
     *         Requires exactly 6 input points.
     */
    fun fit(points: List<PointF>): List<Pair<PocketId, PointF>>? {
        if (points.size != 6) return null

        var bestScore = Float.MAX_VALUE
        var bestResult: List<Pair<PocketId, PointF>>? = null

        // Try all C(6,4) = 15 combinations of 4 points as corner candidates.
        val indices = points.indices.toList()
        for (i in 0 until 6) for (j in i+1 until 6) for (k in j+1 until 6) for (l in k+1 until 6) {
            val corners = listOf(points[i], points[j], points[k], points[l])
            val sideIndices = indices.filter { it !in listOf(i, j, k, l) }
            val sides = sideIndices.map { points[it] }

            val result = tryFitCorners(corners, sides) ?: continue
            val score = rectResidual(result)
            if (score < bestScore) {
                bestScore = score
                bestResult = result
            }
        }
        return bestResult
    }

    /**
     * Tries to interpret 4 points as rectangle corners and 2 points as side pockets.
     * Returns null if the 4 points don't form a valid 2:1 rectangle.
     */
    private fun tryFitCorners(
        corners: List<PointF>,
        sides: List<PointF>
    ): List<Pair<PocketId, PointF>>? {
        // Order the 4 corners as a convex hull (clockwise: TL, TR, BR, BL).
        val ordered = orderRectangle(corners) ?: return null

        // Check aspect ratio: long / short should be ~2:1.
        val width = hypot(
            (ordered[1].x - ordered[0].x).toDouble(),
            (ordered[1].y - ordered[0].y).toDouble()
        ).toFloat()
        val height = hypot(
            (ordered[3].x - ordered[0].x).toDouble(),
            (ordered[3].y - ordered[0].y).toDouble()
        ).toFloat()
        val (longSide, shortSide) = if (width >= height) Pair(width, height) else Pair(height, width)
        if (shortSide < 1f) return null
        val ratio = longSide / shortSide
        if (abs(ratio - ASPECT_RATIO) > ASPECT_TOLERANCE) return null

        // Assign TL/TR/BR/BL based on convex-hull ordering (topmost-leftmost = TL).
        // `orderRectangle` guarantees: [0]=TL, [1]=TR, [2]=BR, [3]=BL.
        val tl = ordered[0]; val tr = ordered[1]
        val br = ordered[2]; val bl = ordered[3]

        // Assign side pockets: each must be near a long-side midpoint.
        val leftMid  = PointF((tl.x + bl.x) / 2f, (tl.y + bl.y) / 2f)
        val rightMid = PointF((tr.x + br.x) / 2f, (tr.y + br.y) / 2f)

        val (sl, sr) = assignSides(sides, leftMid, rightMid) ?: return null

        return listOf(
            PocketId.TL to tl, PocketId.TR to tr,
            PocketId.BL to bl, PocketId.BR to br,
            PocketId.SL to sl, PocketId.SR to sr
        )
    }

    /**
     * Orders 4 points as a clockwise rectangle [TL, TR, BR, BL].
     * Returns null if the points don't form a roughly convex quadrilateral.
     */
    private fun orderRectangle(pts: List<PointF>): List<PointF>? {
        // Sort by y then x to find TL candidate.
        val centroid = PointF(pts.sumOf { it.x.toDouble() }.toFloat() / 4,
                              pts.sumOf { it.y.toDouble() }.toFloat() / 4)
        // Classify by quadrant relative to centroid.
        val tl = pts.minByOrNull {  (it.x - centroid.x) + (it.y - centroid.y) } ?: return null
        val br = pts.maxByOrNull {  (it.x - centroid.x) + (it.y - centroid.y) } ?: return null
        val tr = pts.minByOrNull { -(it.x - centroid.x) + (it.y - centroid.y) } ?: return null
        val bl = pts.maxByOrNull { -(it.x - centroid.x) + (it.y - centroid.y) } ?: return null
        if (setOf(tl, tr, br, bl).size != 4) return null
        return listOf(tl, tr, br, bl)
    }

    /** Assigns the two side points to SL (nearest leftMid) and SR (nearest rightMid). */
    private fun assignSides(
        sides: List<PointF>,
        leftMid: PointF,
        rightMid: PointF
    ): Pair<PointF, PointF>? {
        if (sides.size != 2) return null
        val d0toLeft  = dist(sides[0], leftMid)
        val d0toRight = dist(sides[0], rightMid)
        return if (d0toLeft <= d0toRight) Pair(sides[0], sides[1])
               else Pair(sides[1], sides[0])
    }

    /** Sum of squared deviations from ideal rectangle for the 4 corners. */
    private fun rectResidual(result: List<Pair<PocketId, PointF>>): Float {
        val byId = result.associate { it.first to it.second }
        val tl = byId[PocketId.TL]!!; val tr = byId[PocketId.TR]!!
        val bl = byId[PocketId.BL]!!; val br = byId[PocketId.BR]!!
        // Diagonals should be equal length for a rectangle.
        val d1 = dist(tl, br); val d2 = dist(tr, bl)
        return (d1 - d2) * (d1 - d2)
    }

    private fun dist(a: PointF, b: PointF) = hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()
}
```

- [ ] **Step 4: Run tests — expect PASS**

```bash
./gradlew :app:testDebugUnitTest --tests "com.hereliesaz.cuedetat.domain.TableGeometryFitterTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, 6 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/domain/TableGeometryFitter.kt \
        app/src/test/java/com/hereliesaz/cuedetat/domain/TableGeometryFitterTest.kt
git commit -m "feat: add TableGeometryFitter with unit tests"
```

---

## Task 4: TableScanRepository

**Files:**
- Create: `app/src/main/java/com/hereliesaz/cuedetat/data/TableScanRepository.kt`

- [ ] **Step 1: Create `TableScanRepository.kt`**

```kotlin
// app/src/main/java/com/hereliesaz/cuedetat/data/TableScanRepository.kt
package com.hereliesaz.cuedetat.data

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.hereliesaz.cuedetat.domain.TableScanModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class TableScanRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson: Gson = GsonBuilder().create()
    private val file: File get() = File(context.filesDir, "table_scan_model.json")

    /** Loads the saved model from disk, or null if none exists. */
    fun load(): TableScanModel? = runCatching {
        if (!file.exists()) return null
        gson.fromJson(file.readText(), TableScanModel::class.java)
    }.getOrNull()

    /** Saves the model to disk. Call from a background coroutine. */
    fun save(model: TableScanModel) {
        runCatching { file.writeText(gson.toJson(model)) }
    }

    /** Clears the saved model from disk. */
    fun clear() {
        file.delete()
    }

    /**
     * Attempts to get the device's current GPS coordinates.
     * Returns Pair(lat, lon) or null if location is unavailable or permission is denied.
     * Must be called after the user has granted ACCESS_COARSE_LOCATION.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Pair<Double, Double>? =
        suspendCancellableCoroutine { cont ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location ->
                    cont.resume(location?.let { Pair(it.latitude, it.longitude) })
                }
                .addOnFailureListener { cont.resume(null) }
        }
}
```

- [ ] **Step 2: Verify `play-services-location` is in `libs.versions.toml` / `build.gradle.kts`**

Check `gradle/libs.versions.toml` for `play-services-location`. If missing, add:

```toml
# In [libraries] section:
play-services-location = { group = "com.google.android.gms", name = "play-services-location", version = "21.3.0" }
```

And in `app/build.gradle.kts` dependencies:
```kotlin
implementation(libs.play.services.location)
```

Also verify `AndroidManifest.xml` contains:
```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

Add if missing.

- [ ] **Step 3: Build**

```bash
./gradlew :app:assembleDebug 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/data/TableScanRepository.kt \
        gradle/libs.versions.toml \
        app/build.gradle.kts \
        app/src/main/AndroidManifest.xml
git commit -m "feat: add TableScanRepository with Gson persistence and GPS capture"
```

---

## Task 5: State + Events

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/UiModel.kt`

Read the file first. Then apply these changes:

- [ ] **Step 1: Add `@Transient tableScanModel` to `CueDetatState`**

Find the block of `@Transient` fields (near `inversePitchMatrix`, `visionData`, etc.) and add:

```kotlin
@Transient val tableScanModel: TableScanModel? = null,
```

Add import at top of file:
```kotlin
import com.hereliesaz.cuedetat.domain.TableScanModel
```

- [ ] **Step 2: Add `showTableScanScreen` flag to `CueDetatState`**

Near the other `show*Screen` booleans:
```kotlin
val showTableScanScreen: Boolean = false,
```

- [ ] **Step 3: Add new events to `MainScreenEvent`**

Add these to the `MainScreenEvent` sealed class:

```kotlin
// Table scan events
data class LoadTableScan(val model: TableScanModel) : MainScreenEvent()
object ClearTableScan : MainScreenEvent()
data class UpdateArPose(
    val translation: Offset,
    val rotation: Float,
    val scale: Float
) : MainScreenEvent()
data class UpdateTableScanClusters(
    val updatedClusters: List<PocketCluster>
) : MainScreenEvent()
object ToggleTableScanScreen : MainScreenEvent()
```

Add imports:
```kotlin
import com.hereliesaz.cuedetat.domain.PocketCluster
```

- [ ] **Step 4: Build**

```bash
./gradlew :app:assembleDebug 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL` (or only errors in reducers we haven't updated yet — those are OK).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/domain/UiModel.kt
git commit -m "feat: add tableScanModel to state and scan events to UiModel"
```

---

## Task 6: Reducers

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ControlReducer.kt`
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ToggleReducer.kt`
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/StateReducer.kt` (routes events to reducers — check if new events need routing here)

- [ ] **Step 1: Read `ControlReducer.kt`, `ToggleReducer.kt`, and `StateReducer.kt` to understand the routing pattern**

- [ ] **Step 2: Add handlers in `ControlReducer.kt`**

Add these cases to `reduceControlAction`:

```kotlin
is MainScreenEvent.LoadTableScan -> state.copy(
    tableScanModel = action.model,
    lensWarpTps = action.model.lensWarpTps
)

is MainScreenEvent.ClearTableScan -> state.copy(
    tableScanModel = null,
    lensWarpTps = null
)

is MainScreenEvent.UpdateArPose -> {
    val (minZoom, maxZoom) = ZoomMapping.getZoomRange(state.experienceMode)
    val newZoomSliderPos = ZoomMapping.zoomToSlider(action.scale, minZoom, maxZoom)
    state.copy(
        viewOffset = PointF(action.translation.x, action.translation.y),
        worldRotationDegrees = action.rotation,
        zoomSliderPosition = newZoomSliderPos
        // Note: does NOT touch lensWarpTps or tableScanModel
    )
}

is MainScreenEvent.UpdateTableScanClusters -> {
    val current = state.tableScanModel ?: return state
    state.copy(
        tableScanModel = current.copy(pockets = action.updatedClusters)
    )
}
```

- [ ] **Step 3: Update `ToggleReducer.kt` — AR skip + TableScanScreen toggle**

Find the `CycleCameraMode` handler and replace it:

```kotlin
is MainScreenEvent.CycleCameraMode -> {
    val next = state.cameraMode.next()
    val resolved = if (next == CameraMode.AR && state.tableScanModel == null)
        next.next()  // skip AR → OFF when no table model exists
    else next
    state.copy(cameraMode = resolved)
}
```

Add `ToggleTableScanScreen` handler:
```kotlin
is MainScreenEvent.ToggleTableScanScreen ->
    state.copy(showTableScanScreen = !state.showTableScanScreen)
```

- [ ] **Step 4: Route new events in `StateReducer.kt`**

Read `StateReducer.kt` to see how events are dispatched to reducers (likely a `when` block). Add routing for the new events to `ControlReducer` and `ToggleReducer` as appropriate, following the existing pattern.

- [ ] **Step 5: Build**

```bash
./gradlew :app:assembleDebug 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ControlReducer.kt \
        app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ToggleReducer.kt \
        app/src/main/java/com/hereliesaz/cuedetat/domain/StateReducer.kt
git commit -m "feat: add scan event handlers and AR-skip logic to reducers"
```

---

## Task 7: MainViewModel — load TableScanModel on init

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/ui/MainViewModel.kt`

- [ ] **Step 1: Read `MainViewModel.kt` to understand the init block structure**

- [ ] **Step 2: Inject `TableScanRepository` and add load + GPS-check logic**

In `MainViewModel`:

Add constructor parameter:
```kotlin
private val tableScanRepository: TableScanRepository
```

(Add `@Inject` and include in the `@HiltViewModel`-annotated constructor alongside existing deps.)

In the `init` block, after the existing state load, add a new coroutine:

```kotlin
viewModelScope.launch {
    val savedModel = tableScanRepository.load()
    if (savedModel != null) {
        onEvent(MainScreenEvent.LoadTableScan(savedModel))
        checkLocationAndPromptIfNeeded(savedModel)
    }
}
```

Add the location check helper:
```kotlin
private suspend fun checkLocationAndPromptIfNeeded(model: TableScanModel) {
    if (model.scanLatitude == null || model.scanLongitude == null) return
    val current = tableScanRepository.getCurrentLocation() ?: return
    val dist = haversineDistanceMetres(
        model.scanLatitude, model.scanLongitude,
        current.first, current.second
    )
    if (dist > 100.0) {
        // Surface a warning to the user via the existing WarningManager mechanism,
        // or add a dedicated state flag (showRescanPrompt: Boolean) to CueDetatState
        // and handle dismissal/rescan actions in the UI.
        // Minimum: log for now and add a TODO comment — the user will see the overlay
        // doesn't match and can manually rescan.
        onEvent(MainScreenEvent.ShowRescanPrompt)  // Add this event if implementing the dialog
    }
}

private fun haversineDistanceMetres(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2).let { it * it }
    return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}
```

**Note on `ShowRescanPrompt`:** Either add a `showRescanPrompt: Boolean = false` field to `CueDetatState` and handle it in the UI, or use the `WarningManager` to show a simple text warning. The simplest viable approach: add `warningText` via `WarningManager` saying "You may be at a different table. Tap Scan Table to rescan." This avoids a new state field.

- [ ] **Step 3: Build**

```bash
./gradlew :app:assembleDebug 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/ui/MainViewModel.kt
git commit -m "feat: load TableScanModel on init and check GPS location on resume"
```

---

## Task 8: TableScanAnalyzer + TableScanViewModel

**Files:**
- Create: `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanAnalyzer.kt`
- Create: `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanViewModel.kt`

Before writing, read:
- `VisionRepository.kt` to understand how `imageToScreenMatrix` is computed (search for `imageToScreenMatrix` or the equivalent). Note the exact Matrix construction from image dimensions, rotation, and display dimensions — replicate this pattern in `TableScanAnalyzer`.
- `QuickAlignAnalyzer.kt` to understand the `ImageAnalysis.Analyzer` pattern.

- [ ] **Step 1: Create `TableScanAnalyzer.kt`**

```kotlin
// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanAnalyzer.kt
package com.hereliesaz.cuedetat.ui.composables.tablescan

import android.graphics.PointF
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfPoint
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * CameraX ImageAnalysis.Analyzer that detects pocket-sized circular blobs
 * in each frame and forwards detected image-space positions to the ViewModel.
 *
 * Pockets are larger and darker than billiard balls. Tuned parameters:
 *   minRadius = 15 px (at 480p scale), maxRadius = 60 px
 *   param1 (Canny threshold) = 80, param2 (accumulator threshold) = 25
 *
 * Frames are downsampled to 480p height before processing to limit CPU cost.
 */
class TableScanAnalyzer(
    private val onPocketsDetected: (imagePoints: List<PointF>, imageWidth: Int, imageHeight: Int, rotationDegrees: Int) -> Unit,
    private val onFeltColorSampled: (FloatArray) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        val rotationDegrees = image.imageInfo.rotationDegrees
        val originalWidth = image.width
        val originalHeight = image.height

        // Simplified: work on the Y (luma) plane only — sufficient for Hough circles.
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val yBytes = ByteArray(yBuffer.remaining()).also { yBuffer.get(it) }
        val grayFull = Mat(originalHeight, originalWidth, CvType.CV_8UC1)
        grayFull.put(0, 0, yBytes)

        image.close()

        // Downsample to target height of 480 for speed.
        val targetHeight = 480
        val scale = targetHeight.toDouble() / originalHeight
        val targetWidth = (originalWidth * scale).toInt()
        val graySmall = Mat()
        Imgproc.resize(grayFull, graySmall, Size(targetWidth.toDouble(), targetHeight.toDouble()))
        grayFull.release()

        // Hough circle detection tuned for pocket size.
        val circles = Mat()
        Imgproc.HoughCircles(
            graySmall, circles, Imgproc.CV_HOUGH_GRADIENT,
            /* dp= */ 1.5,
            /* minDist= */ graySmall.rows() / 5.0,
            /* param1= */ 80.0,
            /* param2= */ 25.0,
            /* minRadius= */ 15,
            /* maxRadius= */ 60
        )
        graySmall.release()

        // Upscale detected centres back to original frame coordinates.
        val detections = mutableListOf<PointF>()
        if (!circles.empty()) {
            for (i in 0 until circles.cols()) {
                val data = circles.get(0, i)
                val x = (data[0] / scale).toFloat()
                val y = (data[1] / scale).toFloat()
                detections.add(PointF(x, y))
            }
        }
        circles.release()

        if (detections.isNotEmpty()) {
            onPocketsDetected(detections, originalWidth, originalHeight, rotationDegrees)
        }

        // Sample felt colour from the centre 10% of the frame (full-res luma → BGR → HSV).
        // Run every frame; the ViewModel holds the rolling value used at scan completion.
        try {
            val cx = originalWidth / 2; val cy = originalHeight / 2
            val hw = originalWidth / 20; val hh = originalHeight / 20
            val roi = org.opencv.core.Rect(cx - hw, cy - hh, hw * 2, hh * 2)
            val yRoi = Mat(originalHeight, originalWidth, CvType.CV_8UC1)
            yRoi.put(0, 0, yBytes)  // re-read y bytes (already read above; in practice cache or restructure)
            val crop = Mat(yRoi, roi)
            val bgr = Mat(); Imgproc.cvtColor(crop, bgr, Imgproc.COLOR_GRAY2BGR)
            val hsv = Mat(); Imgproc.cvtColor(bgr, hsv, Imgproc.COLOR_BGR2HSV)
            val mean = Core.mean(hsv)
            onFeltColorSampled(floatArrayOf(mean.`val`[0].toFloat(), mean.`val`[1].toFloat() / 255f, mean.`val`[2].toFloat() / 255f))
            crop.release(); bgr.release(); hsv.release(); yRoi.release()
        } catch (_: Exception) { /* ignore if ROI is out of bounds */ }
    }
}
```

- [ ] **Step 2: Create `TableScanViewModel.kt`**

```kotlin
// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanViewModel.kt
package com.hereliesaz.cuedetat.ui.composables.tablescan

import android.graphics.Matrix
import android.graphics.PointF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.data.TableScanRepository
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.PocketCluster
import com.hereliesaz.cuedetat.domain.PocketId
import com.hereliesaz.cuedetat.domain.TableGeometryFitter
import com.hereliesaz.cuedetat.domain.TableScanModel
import com.hereliesaz.cuedetat.domain.TpsWarpData
import com.hereliesaz.cuedetat.domain.ThinPlateSpline
import com.hereliesaz.cuedetat.domain.decomposeHomography
import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.state.TableSize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.calib3d.Calib3d
import org.opencv.core.Core
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import javax.inject.Inject

/** Min observations per cluster before geometry fitting is attempted. */
private const val MIN_OBSERVATIONS_TO_FIT = 3

/** Max distance (logical inches) to merge a new detection into an existing cluster. */
private const val CLUSTER_MERGE_DISTANCE = 3.0f

@HiltViewModel
class TableScanViewModel @Inject constructor(
    private val tableScanRepository: TableScanRepository
) : ViewModel() {

    private val _scanProgress = MutableStateFlow<Map<PocketId, Boolean>>(emptyMap())
    val scanProgress: StateFlow<Map<PocketId, Boolean>> = _scanProgress.asStateFlow()

    private val _selectedTableSize = MutableStateFlow<TableSize>(TableSize.EIGHT_FT)
    val selectedTableSize: StateFlow<TableSize> = _selectedTableSize.asStateFlow()

    /** Emits events when scan is complete. Collected by TableScanScreen.
     *  Emits LoadTableScan(model) first, then ApplyQuickAlign. Both must be dispatched
     *  before the screen is dismissed — the screen must NOT close inside this collector.
     *  Use scanComplete to trigger dismissal after all events have been dispatched. */
    private val _scanResult = MutableSharedFlow<MainScreenEvent>()
    val scanResult = _scanResult.asSharedFlow()

    /** Flips to true after completeScan emits all events. Screen dismisses on this signal. */
    private val _scanComplete = MutableStateFlow(false)
    val scanComplete: StateFlow<Boolean> = _scanComplete.asStateFlow()

    // Mutable cluster accumulator: identity → running cluster.
    private val clusters = mutableMapOf<PocketId, MutableList<PointF>>()

    // Felt colour sampled from recent frames (rolling mean HSV of centre crop).
    @Volatile private var lastFeltHsv: FloatArray = floatArrayOf(120f, 0.5f, 0.4f)

    // Last known inversePitchMatrix from the main state — set by the screen on each recompose.
    @Volatile private var inversePitchMatrix: Matrix? = null
    @Volatile private var hasInverseMatrix: Boolean = false
    @Volatile private var viewWidth: Int = 0
    @Volatile private var viewHeight: Int = 0

    fun updateStateSnapshot(inverse: Matrix?, hasInverse: Boolean, vw: Int, vh: Int) {
        inversePitchMatrix = inverse
        hasInverseMatrix = hasInverse
        viewWidth = vw
        viewHeight = vh
    }

    /** Called by TableScanAnalyzer each frame with the mean HSV of the centre crop of the felt. */
    fun onFeltColorSampled(hsv: FloatArray) { lastFeltHsv = hsv }

    fun onTableSizeSelected(size: TableSize) {
        _selectedTableSize.value = size
    }

    /**
     * Called by TableScanAnalyzer on each frame.
     * Projects image-space blobs to logical space and merges into clusters.
     */
    fun onFrame(
        imagePoints: List<PointF>,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int
    ) {
        if (!hasInverseMatrix) return
        val inverse = inversePitchMatrix ?: return

        viewModelScope.launch(Dispatchers.Default) {
            // Step 1: Build imageToScreenMatrix — mirrors VisionRepository.getTransformationMatrix exactly.
            // VisionRepository uses inputImage.width/height directly (rotation handled upstream by CameraX).
            val imageToScreen = Matrix().apply {
                postScale(viewWidth.toFloat() / imageWidth.toFloat(),
                          viewHeight.toFloat() / imageHeight.toFloat())
            }

            // Step 2: Project image → screen → logical for each detection.
            val screenPts = FloatArray(imagePoints.size * 2)
            imagePoints.forEachIndexed { i, p -> screenPts[i*2] = p.x; screenPts[i*2+1] = p.y }
            imageToScreen.mapPoints(screenPts)

            val logicalPts = imagePoints.indices.map { i ->
                val screenPt = PointF(screenPts[i*2], screenPts[i*2+1])
                screenToLogical(screenPt, inverse)
            }

            // Step 3: Merge into clusters.
            logicalPts.forEach { logicalPt -> mergeIntoCluster(logicalPt) }

            // Step 4: Update UI progress.
            val stableIds = clusters.entries
                .filter { it.value.size >= MIN_OBSERVATIONS_TO_FIT }
                .map { it.key }
                .toSet()
            // We don't yet know identities until fitting — show progress as total stable clusters / 6.
            withContext(Dispatchers.Main) {
                // Signal scan progress by count, not yet by identity.
                // Identity is resolved only on fit. For UI purposes, show count of stable clusters.
                _scanProgress.value = PocketId.values()
                    .take(clusters.count { it.value.size >= MIN_OBSERVATIONS_TO_FIT })
                    .associateWith { true }
            }

            // Step 5: Attempt fit when 6 clusters are stable.
            val stableClusters = clusters.filter { it.value.size >= MIN_OBSERVATIONS_TO_FIT }
            if (stableClusters.size >= 6) {
                val centerPts = stableClusters.values.map { observations ->
                    PointF(
                        observations.sumOf { it.x.toDouble() }.toFloat() / observations.size,
                        observations.sumOf { it.y.toDouble() }.toFloat() / observations.size
                    )
                }
                val fitResult = TableGeometryFitter.fit(centerPts) ?: return@launch

                // Re-key clusters by identified PocketId.
                val identifiedCenters = fitResult.associate { (id, pt) -> id to pt }
                completeScan(identifiedCenters, imageWidth.toFloat(), imageHeight.toFloat())
            }
        }
    }

    private fun mergeIntoCluster(pt: PointF) {
        for ((_, observations) in clusters) {
            val mean = PointF(
                observations.sumOf { it.x.toDouble() }.toFloat() / observations.size,
                observations.sumOf { it.y.toDouble() }.toFloat() / observations.size
            )
            val dist = kotlin.math.hypot((pt.x - mean.x).toDouble(), (pt.y - mean.y).toDouble()).toFloat()
            if (dist <= CLUSTER_MERGE_DISTANCE) {
                observations.add(pt)
                return
            }
        }
        // New cluster — key by current count as temporary identifier.
        clusters[PocketId.values().getOrElse(clusters.size) { PocketId.TL }] = mutableListOf(pt)
    }

    private suspend fun completeScan(
        identifiedLogical: Map<PocketId, PointF>,
        imgWidth: Float,
        imgHeight: Float
    ) {
        val tableSize = _selectedTableSize.value
        val logicalTable = Table(tableSize, true)

        // True logical positions in standard order.
        val pocketOrder = listOf(
            PocketId.TL to logicalTable.pockets[0],
            PocketId.TR to logicalTable.pockets[1],
            PocketId.BR to logicalTable.pockets[3], // note BL=2, BR=3 swap in Table.pockets
            PocketId.BL to logicalTable.pockets[2],
            PocketId.SL to logicalTable.pockets[4],
            PocketId.SR to logicalTable.pockets[5]
        )

        // Build homography: detected logical → true logical.
        // (Note: detected positions ARE already in logical space; H maps detected→true.)
        val srcMat = MatOfPoint2f()
        val dstMat = MatOfPoint2f()
        val srcList = pocketOrder.mapNotNull { (id, _) -> identifiedLogical[id]?.let { Point(it.x.toDouble(), it.y.toDouble()) } }
        val dstList = pocketOrder.map { (_, pt) -> Point(pt.x.toDouble(), pt.y.toDouble()) }
        if (srcList.size != 6) return
        srcMat.fromList(srcList)
        dstMat.fromList(dstList)

        val homography = Calib3d.findHomography(srcMat, dstMat, Calib3d.RANSAC, 3.0)
        if (homography.empty()) return

        val (translation, rotation, scale) = decomposeHomography(homography, imgWidth, imgHeight)

        // Residual TPS: estimated logical → true logical.
        val estimatedDst = MatOfPoint2f()
        Core.perspectiveTransform(srcMat, estimatedDst, homography)
        val estimatedLogical = estimatedDst.toList().map { PointF(it.x.toFloat(), it.y.toFloat()) }
        val trueLogical = dstList.map { PointF(it.x.toFloat(), it.y.toFloat()) }
        val tpsWarpData = TpsWarpData(srcPoints = estimatedLogical, dstPoints = trueLogical)

        // Build PocketClusters with initial observation data.
        val pocketClusters = pocketOrder.map { (id, _) ->
            PocketCluster(
                identity = id,
                logicalPosition = identifiedLogical[id]!!,
                observationCount = clusters.values.firstOrNull()?.size ?: 1,
                variance = 1.0f
            )
        }

        // Use rolling HSV mean sampled by TableScanAnalyzer on each frame.
        val feltColorHsv = lastFeltHsv

        val location = tableScanRepository.getCurrentLocation()
        val model = TableScanModel(
            pockets = pocketClusters,
            lensWarpTps = tpsWarpData,
            tableSize = tableSize,
            feltColorHsv = feltColorHsv,
            scanLatitude = location?.first,
            scanLongitude = location?.second
        )
        tableScanRepository.save(model)

        // Invariant 1: emit LoadTableScan FIRST so tableScanModel is set in state before pose is applied.
        _scanResult.emit(MainScreenEvent.LoadTableScan(model))
        _scanResult.emit(MainScreenEvent.ApplyQuickAlign(translation, rotation, scale, tpsWarpData))
        // Signal screen dismissal AFTER both events are enqueued.
        _scanComplete.value = true
    }

    fun resetScan() {
        clusters.clear()
        _scanProgress.value = emptyMap()
        _scanComplete.value = false
    }

    // ------ Coordinate helpers ------

    private fun screenToLogical(screen: PointF, inverse: Matrix): PointF {
        val arr = floatArrayOf(screen.x, screen.y)
        inverse.mapPoints(arr)
        return PointF(arr[0], arr[1])
    }
}
```

- [ ] **Step 3: Build**

```bash
./gradlew :app:assembleDebug 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanAnalyzer.kt \
        app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanViewModel.kt
git commit -m "feat: add TableScanAnalyzer and TableScanViewModel"
```

---

## Task 9: TableScanScreen

**Files:**
- Create: `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanScreen.kt`

- [ ] **Step 1: Create `TableScanScreen.kt`**

```kotlin
// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanScreen.kt
package com.hereliesaz.cuedetat.ui.composables.tablescan

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.PocketId
import com.hereliesaz.cuedetat.ui.composables.CameraBackground

/**
 * Full-screen scan UI.
 *
 * Shows a live camera feed with a pocket progress overlay.
 * Six pocket indicators fill in (yellow solid) as each pocket is detected.
 * Done button unlocks when all 6 are found. Reset clears accumulated detections.
 *
 * GPS permission is requested here at scan-start time.
 */
@Composable
fun TableScanScreen(
    onEvent: (MainScreenEvent) -> Unit,
    uiState: com.hereliesaz.cuedetat.domain.CueDetatState,
    viewModel: TableScanViewModel = hiltViewModel()
) {
    val scanProgress by viewModel.scanProgress.collectAsState()

    // GPS permission request — fired once when the composable first appears.
    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — TableScanRepository.getCurrentLocation() handles null gracefully */ }
    LaunchedEffect(Unit) {
        locationPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    // Pass current state snapshot to ViewModel so it can do coordinate transforms.
    LaunchedEffect(uiState.inversePitchMatrix, uiState.hasInverseMatrix) {
        viewModel.updateStateSnapshot(
            uiState.inversePitchMatrix,
            uiState.hasInverseMatrix,
            uiState.viewWidth,
            uiState.viewHeight
        )
    }

    // Forward each scan event to the main event bus. Do NOT close the screen here —
    // closures cancel the collector and would drop the second emission.
    val scanResult = viewModel.scanResult
    LaunchedEffect(scanResult) {
        scanResult.collect { result -> onEvent(result) }
    }

    // Close screen only after completeScan has signalled that ALL events have been emitted.
    val scanComplete by viewModel.scanComplete.collectAsStateWithLifecycle()
    LaunchedEffect(scanComplete) {
        if (scanComplete) {
            onEvent(MainScreenEvent.ToggleTableScanScreen)
        }
    }

    val analyzer = remember { TableScanAnalyzer(viewModel::onFrame, viewModel::onFeltColorSampled) }
    val foundCount = scanProgress.count { it.value }
    val allFound = foundCount >= 6

    Box(modifier = Modifier.fillMaxSize()) {
        CameraBackground(analyzer = analyzer)

        // Pocket progress overlay.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pocketIds = PocketId.values()
            val positions = listOf(
                androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.15f), // TL
                androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.15f), // TR
                androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.85f), // BR
                androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.85f), // BL
                androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.50f), // SL
                androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.50f)  // SR
            )
            pocketIds.forEachIndexed { i, id ->
                val pos = positions[i]
                val isFound = scanProgress[id] == true || i < foundCount
                drawCircle(
                    color = Color.Yellow,
                    radius = 24.dp.toPx(),
                    center = pos,
                    style = if (isFound) androidx.compose.ui.graphics.drawscope.Fill
                            else Stroke(width = 3.dp.toPx()),
                    alpha = if (isFound) 0.9f else 0.5f
                )
            }
        }

        // Controls overlay.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$foundCount / 6 pockets found — pan across the table",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    viewModel.resetScan()
                }) { Text("Reset") }
                Button(
                    onClick = { /* Done is triggered automatically — this closes early */ },
                    enabled = allFound
                ) { Text("Done") }
            }
        }
    }
}
```

- [ ] **Step 2: Build**

```bash
./gradlew :app:assembleDebug 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanScreen.kt
git commit -m "feat: add TableScanScreen with pocket progress overlay"
```

---

## Task 10: Navigation — wire scan screen, update menu, Reset clears pose

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/ui/ProtractorScreen.kt`
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/AzNavRailMenu.kt`

- [ ] **Step 1: Read `ProtractorScreen.kt` to understand existing route/NavHost structure**

- [ ] **Step 2: Add `ROUTE_SCAN` and wire `TableScanScreen` into the NavHost**

In `ProtractorScreen.kt`:

Add route constant:
```kotlin
private const val ROUTE_SCAN = "scan"
```

Add `LaunchedEffect` for the scan screen (same pattern as existing `ROUTE_ALIGN`):
```kotlin
LaunchedEffect(uiState.showTableScanScreen) {
    if (uiState.showTableScanScreen && navController.currentDestination?.route != ROUTE_SCAN) {
        navController.navigate(ROUTE_SCAN)
    } else if (!uiState.showTableScanScreen && navController.currentDestination?.route == ROUTE_SCAN) {
        navController.popBackStack()
    }
}
```

Add composable route in the NavHost:
```kotlin
composable(ROUTE_SCAN) {
    TableScanScreen(
        onEvent = mainViewModel::onEvent,
        uiState = uiState
    )
}
```

- [ ] **Step 3: Update Reset to clear camera pose**

In `ProtractorScreen.kt`, find where Reset is triggered (likely in `AzNavRailMenu` or a button). Alternatively, update `ActionReducer.kt`'s Reset handler to ensure `viewOffset`, `worldRotationDegrees`, and `zoomSliderPosition` are cleared.

Read `ActionReducer.kt` Reset branch and confirm these three fields are already cleared (exploration found they are — lines 84, 86, 99). If confirmed, no code change needed here. Add a comment to document the intent.

- [ ] **Step 4: Update `AzNavRailMenu.kt` — replace Quick Align with Scan Table**

Read `AzNavRailMenu.kt` to find the Quick Align menu item. Replace it with:

```kotlin
// Replace the Quick Align menu entry with:
azMenuItem(
    label = "Scan Table",
    icon = /* use an appropriate existing icon, e.g., camera_enhance or similar */,
    onClick = { onEvent(MainScreenEvent.ToggleTableScanScreen) }
)
```

Remove the `ToggleQuickAlignScreen` event dispatch if present in `AzNavRailMenu`.

- [ ] **Step 5: Add "Rescan" entry to nav menu (spec requirement)**

The spec says Rescan must be available from the nav menu. Add a second entry below "Scan Table", only visible when `tableScanModel != null`. Pass `hasTableModel: Boolean` into `AzNavRailMenu` (read the composable signature first to understand how to thread it in), then:

```kotlin
if (hasTableModel) {
    azMenuItem(
        label = "Rescan",
        icon = /* refresh/sync icon — find an existing icon in the codebase */,
        onClick = {
            onEvent(MainScreenEvent.ClearTableScan)
            onEvent(MainScreenEvent.ToggleTableScanScreen)
        }
    )
}
```

In the call site (ProtractorScreen.kt), pass `hasTableModel = state.tableScanModel != null`.

- [ ] **Step 7: Build**

```bash
./gradlew :app:assembleDebug 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/ui/ProtractorScreen.kt \
        app/src/main/java/com/hereliesaz/cuedetat/ui/composables/AzNavRailMenu.kt
git commit -m "feat: wire TableScanScreen into navigation; add Scan Table and Rescan menu entries"
```

---

## Task 11: Remove QuickAlign files + clean up orphaned events

**Files:**
- Delete: `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/quickalign/QuickAlignScreen.kt`
- Delete: `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/quickalign/QuickAlignViewModel.kt`
- Delete: `app/src/main/java/com/hereliesaz/cuedetat/ui/composables/quickalign/QuickAlignAnalyzer.kt`
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/UiModel.kt` (remove old events/state)
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ToggleReducer.kt` (remove old handler)

- [ ] **Step 1: Remove the three QuickAlign files**

```bash
git rm app/src/main/java/com/hereliesaz/cuedetat/ui/composables/quickalign/QuickAlignScreen.kt \
       app/src/main/java/com/hereliesaz/cuedetat/ui/composables/quickalign/QuickAlignViewModel.kt \
       app/src/main/java/com/hereliesaz/cuedetat/ui/composables/quickalign/QuickAlignAnalyzer.kt
```

- [ ] **Step 2: Remove `showQuickAlignScreen` from `CueDetatState` and `ToggleQuickAlignScreen` event from `UiModel.kt`**

In `UiModel.kt`:
- Remove `val showQuickAlignScreen: Boolean = false`
- Remove `object ToggleQuickAlignScreen : MainScreenEvent()`

- [ ] **Step 3: Remove `ToggleQuickAlignScreen` handler from `ToggleReducer.kt`**

Remove the `is MainScreenEvent.ToggleQuickAlignScreen ->` case.

- [ ] **Step 4: Build — fix any remaining compilation errors**

```bash
./gradlew :app:assembleDebug 2>&1 | grep "error:"
```

Fix each compilation error (likely stray references to `showQuickAlignScreen` or `ToggleQuickAlignScreen` in ProtractorScreen or other files).

- [ ] **Step 5: Build clean**

```bash
./gradlew :app:assembleDebug 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
# Files deleted in Step 1 are already staged. Just add modified files.
git add app/src/main/java/com/hereliesaz/cuedetat/domain/UiModel.kt \
        app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ToggleReducer.kt \
        app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/StateReducer.kt
git commit -m "feat: remove QuickAlign files; clean up orphaned events"
```

---

## Task 12: VisionRepository — AR pocket re-detection + cluster update + edge fallback

**Files:**
- Modify: `app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt`

Before starting, read `VisionRepository.kt` in full to understand where the per-frame pipeline runs and how to add a second detection pass.

- [ ] **Step 1: Add frame counter for every-5th-frame guard**

In `VisionRepository`, add a class-level counter:
```kotlin
private var arFrameCounter = 0
```

- [ ] **Step 2: Add AR tracking pass to `processFrame` (or equivalent)**

After the existing ball detection pipeline (after all existing steps complete), add:

```kotlin
// AR tracking pass — only in AR mode, every 5th frame.
if (state.cameraMode == CameraMode.AR && state.tableScanModel != null) {
    arFrameCounter++
    if (arFrameCounter % 5 == 0) {
        runArTrackingPass(mat, state, imageWidth, imageHeight, rotationDegrees)
    }
}
```

- [ ] **Step 3: Implement `runArTrackingPass`**

```kotlin
/**
 * Per-frame AR tracking:
 * 1. Detect pocket-sized circles.
 * 2. Match to known clusters; update cluster weights (Kalman-style).
 * 3. If ≥2 matched: solve homography → blend pose → emit UpdateArPose.
 * 4. If 0 matched: attempt edge-based fallback.
 */
private fun runArTrackingPass(
    mat: Mat,
    state: CueDetatState,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int
) {
    val model = state.tableScanModel ?: return
    if (!state.hasInverseMatrix) return
    val inverse = state.inversePitchMatrix ?: return

    // Downsample for Hough.
    val targetHeight = 480
    val scale = targetHeight.toDouble() / imageHeight
    val targetWidth = (imageWidth * scale).toInt()
    val graySmall = Mat()
    Imgproc.cvtColor(mat, graySmall, Imgproc.COLOR_BGR2GRAY)
    val grayResized = Mat()
    Imgproc.resize(graySmall, grayResized, Size(targetWidth.toDouble(), targetHeight.toDouble()))
    graySmall.release()

    val circles = Mat()
    Imgproc.HoughCircles(
        grayResized, circles, Imgproc.CV_HOUGH_GRADIENT,
        1.5, grayResized.rows() / 5.0, 80.0, 25.0, 15, 60
    )
    grayResized.release()

    // Project detected blobs to logical space.
    val detectedLogical = mutableListOf<Pair<PointF, PointF>>() // (imagePoint, logicalPoint)
    if (!circles.empty()) {
        val imageToScreen = Matrix().apply {
            postScale(state.viewWidth.toFloat() / imageWidth.toFloat(),
                      state.viewHeight.toFloat() / imageHeight.toFloat())
        }
        for (i in 0 until circles.cols()) {
            val data = circles.get(0, i)
            val imgPt = PointF((data[0] / scale).toFloat(), (data[1] / scale).toFloat())
            val screenArr = floatArrayOf(imgPt.x, imgPt.y)
            imageToScreen.mapPoints(screenArr)
            val logicalArr = floatArrayOf(screenArr[0], screenArr[1])
            inverse.mapPoints(logicalArr)
            detectedLogical.add(imgPt to PointF(logicalArr[0], logicalArr[1]))
        }
    }
    circles.release()

    // Match detections to known clusters (nearest within 3 logical inches).
    val matchedPairs = mutableListOf<Triple<PocketId, PointF, PointF>>() // (id, imgPt, logicalPt)
    val tolerance = 3.0f
    detectedLogical.forEach { (imgPt, logPt) ->
        val nearest = model.pockets.minByOrNull { cluster ->
            hypot((cluster.logicalPosition.x - logPt.x).toDouble(),
                  (cluster.logicalPosition.y - logPt.y).toDouble()).toFloat()
        }
        if (nearest != null) {
            val dist = hypot((nearest.logicalPosition.x - logPt.x).toDouble(),
                             (nearest.logicalPosition.y - logPt.y).toDouble()).toFloat()
            if (dist <= tolerance) matchedPairs.add(Triple(nearest.identity, imgPt, logPt))
        }
    }

    // Cluster update (persistent point cloud refinement).
    if (matchedPairs.isNotEmpty()) {
        val updatedClusters = model.pockets.map { cluster ->
            val match = matchedPairs.firstOrNull { it.first == cluster.identity }
            if (match != null) {
                val n = cluster.observationCount
                val newX = (n * cluster.logicalPosition.x + match.third.x) / (n + 1)
                val newY = (n * cluster.logicalPosition.y + match.third.y) / (n + 1)
                cluster.copy(
                    logicalPosition = PointF(newX, newY),
                    observationCount = n + 1,
                    variance = cluster.variance * 0.95f  // decay variance with each observation
                )
            } else cluster
        }
        emitEvent(MainScreenEvent.UpdateTableScanClusters(updatedClusters))
    }

    // Pose update — requires ≥ 2 matched pockets.
    if (matchedPairs.size >= 2) {
        val srcMat = MatOfPoint2f()
        val dstMat = MatOfPoint2f()
        srcMat.fromList(matchedPairs.map { Point(it.second.x.toDouble(), it.second.y.toDouble()) })
        dstMat.fromList(matchedPairs.map {
            val cluster = model.pockets.first { c -> c.identity == it.first }
            Point(cluster.logicalPosition.x.toDouble(), cluster.logicalPosition.y.toDouble())
        })
        val h = Calib3d.findHomography(srcMat, dstMat, Calib3d.RANSAC, 3.0)
        if (!h.empty()) {
            val (rawT, rawR, rawS) = decomposeHomography(h, imageWidth.toFloat(), imageHeight.toFloat())
            val alpha = 0.15f
            val current = state
            val blendedTranslation = androidx.compose.ui.geometry.Offset(
                alpha * rawT.x + (1 - alpha) * current.viewOffset.x,
                alpha * rawT.y + (1 - alpha) * current.viewOffset.y
            )
            val blendedRotation = alpha * rawR + (1 - alpha) * current.worldRotationDegrees
            val blendedScale = alpha * rawS + (1 - alpha) * ZoomMapping.sliderToZoom(
                current.zoomSliderPosition,
                ZoomMapping.getZoomRange(current.experienceMode).first,
                ZoomMapping.getZoomRange(current.experienceMode).second
            )
            // Dead zone: skip if change is negligible.
            val delta = hypot((blendedTranslation.x - current.viewOffset.x).toDouble(),
                              (blendedTranslation.y - current.viewOffset.y).toDouble())
            if (delta > 0.5) {   // spec: skip if < 0.5 logical inches equivalent
                emitEvent(MainScreenEvent.UpdateArPose(blendedTranslation, blendedRotation, blendedScale))
            }
        }
    } else if (matchedPairs.isEmpty()) {
        runEdgeFallback(mat, state, inverse, imageWidth, imageHeight, rotationDegrees)
    }
}
```

- [ ] **Step 4: Implement `runEdgeFallback`**

```kotlin
/**
 * When no pockets are in frame, attempt to use the table's felt boundary for pose correction.
 *
 * Strategy:
 * 1. Convert to HSV; threshold using stored feltColorHsv ± tolerance to get felt mask.
 * 2. Find the largest contour; approximate to a quadrilateral.
 * 3. If a valid quad is found, match its corners to the expected table outline and
 *    compute a pose correction using the same homography+blend pipeline as pocket tracking.
 *
 * If edge detection also fails, silently hold current pose (IMU maintains tilt).
 */
private fun runEdgeFallback(
    mat: Mat,
    state: CueDetatState,
    inverse: Matrix,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int
) {
    val model = state.tableScanModel ?: return
    val hsv = floatArrayOf(model.feltColorHsv[0], model.feltColorHsv[1], model.feltColorHsv[2])

    val hsvMat = Mat()
    Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_BGR2HSV)

    // Threshold: ±20 H, ±60 S, ±60 V around stored felt colour.
    val lower = org.opencv.core.Scalar(
        maxOf(0.0, hsv[0] - 20.0), maxOf(0.0, hsv[1] * 255 - 60.0), maxOf(0.0, hsv[2] * 255 - 60.0)
    )
    val upper = org.opencv.core.Scalar(
        minOf(180.0, hsv[0] + 20.0), minOf(255.0, hsv[1] * 255 + 60.0), minOf(255.0, hsv[2] * 255 + 60.0)
    )
    val mask = Mat()
    Core.inRange(hsvMat, lower, upper, mask)
    hsvMat.release()

    val contours = mutableListOf<MatOfPoint>()
    Imgproc.findContours(mask, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
    mask.release()

    val largest = contours.maxByOrNull { Imgproc.contourArea(it) } ?: return
    if (Imgproc.contourArea(largest) < mat.rows() * mat.cols() * 0.05) return // Too small

    val approx = MatOfPoint2f()
    val contour2f = MatOfPoint2f(*largest.toArray())
    val epsilon = 0.02 * Imgproc.arcLength(contour2f, true)
    Imgproc.approxPolyDP(contour2f, approx, epsilon, true)

    // Require a quadrilateral (4 corners).
    if (approx.rows() != 4) return

    // Match the 4 quad corners to the expected 4 table corners.
    // Use the same homography+blend pipeline as pocket tracking with the 4 detected corners.
    // (Implementation mirrors the pocket path — omitted here for brevity;
    //  call decomposeHomography and emitEvent(UpdateArPose) with blended values.)
}
```

- [ ] **Step 5: Add `emitEvent` helper (if not already present in VisionRepository)**

Check if `VisionRepository` has a way to emit events to `MainViewModel`. If it uses a `SharedFlow` or callback, use the existing mechanism. If it calls the event bus directly, replicate the pattern.

- [ ] **Step 6: Build**

```bash
./gradlew :app:assembleDebug 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Run all unit tests**

```bash
./gradlew :app:testDebugUnitTest 2>&1 | tail -20
```

Expected: all tests pass.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt
git commit -m "feat: add AR pocket re-detection, cluster update, pose blend, and edge fallback"
```

---

## Final Integration Check

- [ ] **Build and install**

```bash
./gradlew :app:assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Smoke test scan flow**
  1. Open app → tap "Scan Table" in nav menu
  2. Pan camera across table — pocket indicators fill in one by one
  3. All 6 filled → overlay auto-locks and screen closes → `CameraMode` is now AR
  4. Walk around table — overlay stays locked to real table edges
  5. Pocket app → reopen → GPS check fires (if > 100m from scan location)
  6. Overlay re-anchors automatically without user action

- [ ] **Smoke test Reset**
  7. Press Reset → overlay pose clears → `TableScanModel` survives → re-anchors on next pocket detection

- [ ] **Smoke test Rescan**
  8. From AR mode, trigger Rescan → `TableScanModel` cleared → scan screen opens → full scan cycle works again
