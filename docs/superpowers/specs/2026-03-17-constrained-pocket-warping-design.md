# Constrained Pocket Warping — Design Spec

**Date:** 2026-03-17
**Status:** Approved

---

## Problem

QuickAlign currently lets each of the six pocket drag points move independently. This makes alignment tedious — adjusting one pocket has no effect on the others, so the user must manually place all six through trial and error. The result is also limited: the final homography decomposes only to translation, rotation, and scale, which cannot express lens barrel or pincushion distortion.

---

## Goal

When the user drags any pocket during QuickAlign:
1. The other pockets update in real-time to positions that are physically consistent with the implied lens warp.
2. The final alignment result stores the full non-linear warp (Thin-Plate Spline), not just a homography decomposition.
3. The overlay is rendered and CV detections are corrected using this stored warp.

---

## Approach: Thin-Plate Spline (TPS)

TPS was chosen over homography (can't express lens distortion) and full per-pixel remap (30fps remap cost). TPS warps the overlay at the point level — cheap to evaluate per frame, expressively accurate for lens distortion.

The TPS is split from the coarse camera pose:
- **Homography decomposition** (translation, rotation, scale) continues to handle camera pose and real-time sensor tilt — unchanged from today.
- **TPS residual** captures the non-linear lens distortion that the homography cannot express. This is fixed after alignment and does not change as the device tilts.

---

## Section 1: QuickAlign Interaction Model

### Accumulating TPS Constraints

Pockets start at their ideal positions (undistorted rectangle on the captured photo, derived from the selected table size). As the user places pockets, constraints accumulate:

| Pinned pockets | Warp behavior |
|---|---|
| 0 | All at ideal positions |
| 1 | Pure translation — all others shift by the same delta |
| 2 | Similarity-like warp — rotation + scale + translation |
| 3–6 | Full TPS — non-linear lens distortion characterized |

**Drag behavior:**
- While the user is dragging a pocket, the TPS is solved live from all currently pinned constraints plus the current drag position. The remaining unpinned pockets update in real-time.
- On release, the dragged pocket becomes pinned at its final position.
- Previously pinned pockets can be re-dragged to refine them; releasing re-pins them at the new position.

**Visual distinction:**
- Unpinned pockets: hollow/dashed circle — indicates TPS-predicted, not user-confirmed.
- Pinned pockets: solid circle — user-confirmed.

**Removed constraint:** The existing side-pocket linear snap (snapping side pockets to the midpoint line between adjacent corners) is removed. All 6 pockets are fully free TPS control points.

---

## Section 2: TPS Solver and Data Model

### ThinPlateSpline.kt (new)

A pure-Kotlin TPS solver and evaluator. At 6 control points it solves a 9×9 linear system (6 control points + 3 affine terms) — negligible cost, computed once at alignment time.

**Direction:** Image space → logical space (natural for CV output).
- Source points: 6 pocket positions in image space (user-placed on the captured photo).
- Destination points: 6 known pocket positions in logical space (inches, from selected table size).

The inverse direction (logical → image, for rendering) is obtained by swapping src/dst in the same solver and stored alongside.

**TpsWarpData.kt (new) — serializable state:**

```kotlin
@Keep
data class TpsWarpData(
    val srcPoints: List<PointF>,   // 6 image-space positions
    val dstPoints: List<PointF>,   // 6 logical-space positions
    val weights: FloatArray,       // TPS kernel weights
    val affineTerms: FloatArray    // Affine component (3 terms per axis)
)
```

Stored as JSON in `CueDetatState` alongside the existing pose parameters.

### Output of QuickAlign (onFinishAlign)

1. Fit homography to the 6 final point positions → decompose to translation, rotation, scale (existing behavior, unchanged).
2. Compute residuals: difference between homography-predicted image positions and actual user-placed positions.
3. Solve TPS from residuals → produce `TpsWarpData`.
4. Emit `ApplyQuickAlign(translation, rotation, scale, tpsWarpData)`.

### CueDetatState change

```kotlin
val lensWarpTps: TpsWarpData? = null
```

`ApplyQuickAlign` handler in `ControlReducer` stores `lensWarpTps` from the event.

---

## Section 3: Rendering Integration

### Approach: per-point warp correction inside existing matrix context

The `canvas.withMatrix(pitchMatrix)` block is unchanged. Inside it, draw points pass through the inverse TPS residual before being used. If `lensWarpTps` is null (user has not aligned), points pass through unmodified.

**Helper extension:**
```kotlin
fun PointF.warpedBy(tps: TpsWarpData?): PointF
```

**Points warped in TableRenderer:**
- Table corner points (4)
- Pocket centers (6)
- Diamond grid intersections
- Rail line endpoints

**Aiming lines** (`LineRenderer`): start and end points warped; line drawn between them.

**Circles** (pockets, balls): only center point warped. Radius is not adjusted — pocket size does not vary meaningfully with lens position.

**No changes to:**
- `OverlayRenderer` matrix setup
- Sensor/pitch pipeline
- Aiming calculation logic (operates in logical space, which is already TPS-corrected by the time it gets there)

---

## Section 4: CV Integration

**Current ball detection pipeline:**
```
image pixel → imageToScreenMatrix → screen → inversePitchMatrix → logical
```

**With TPS:**
```
image pixel → imageToScreenMatrix → screen → inversePitchMatrix → logical → forward TPS → true logical
```

The forward TPS evaluation is added as a single step at the end of the coordinate transform in `VisionRepository`, at the same call site where logical coordinates are currently produced. If `lensWarpTps` is null, the step is skipped.

---

## Components Summary

| Component | Type | Change |
|---|---|---|
| `ThinPlateSpline.kt` | New | TPS solver + forward/inverse evaluator |
| `TpsWarpData.kt` | New | Serializable TPS state (weights + affine terms) |
| `QuickAlignViewModel.kt` | Modified | Accumulating TPS constraints on drag; emit TPS on finish |
| `QuickAlignScreen.kt` | Modified | Visual distinction between pinned and unpinned pockets |
| `CueDetatState` (UiModel.kt) | Modified | Add `lensWarpTps: TpsWarpData?` |
| `MainScreenEvent.ApplyQuickAlign` | Modified | Add `tpsWarpData: TpsWarpData` parameter |
| `ControlReducer.kt` | Modified | Store `lensWarpTps` from `ApplyQuickAlign` |
| `TableRenderer.kt` | Modified | Warp draw points through inverse TPS |
| `LineRenderer.kt` | Modified | Warp aiming line endpoints through inverse TPS |
| `VisionRepository.kt` | Modified | Apply forward TPS after logical coordinate conversion |

---

## Out of Scope

- Per-pixel camera frame remapping (ruled out: 30fps cost too high)
- TPS applied to the full rendering matrix (ruled out: incompatible with real-time sensor tilt)
- Automatic table detection / scanning (separate sub-project)
