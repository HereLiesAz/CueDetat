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
2. The final alignment result stores the non-linear residual warp (Thin-Plate Spline) on top of the existing homography-derived pose parameters.
3. The overlay is rendered and CV detections are corrected using this stored residual warp.

---

## Approach: Thin-Plate Spline (TPS)

TPS was chosen over homography (can't express lens distortion) and full per-pixel remap (30fps remap cost). TPS warps the overlay at the point level — cheap to evaluate per frame, expressively accurate for lens distortion.

The TPS is split from the coarse camera pose:
- **Homography decomposition** (translation, rotation, scale) continues to handle camera pose and real-time sensor tilt — unchanged from today. Applied via `canvas.withMatrix(pitchMatrix)`.
- **TPS residual** captures the non-linear lens distortion the homography cannot express. Applied as a per-point correction *inside* the matrix context. Fixed after alignment; does not change as the device tilts.

The two layers compose correctly: `pitchMatrix` handles real-time sensor tilt; the TPS residual handles the static non-linear component on top of it.

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
- While the user is dragging a pocket, a temporary TPS is solved on a background dispatcher (`Dispatchers.Default`) from all currently pinned constraints plus the current drag position. The remaining unpinned pockets update in real-time via a `StateFlow`. Each new drag event cancels the previous in-flight coroutine `Job` before launching a new one (replace-on-new strategy), preventing solve backlog.
- On release, the dragged pocket becomes pinned at its final position.
- Previously pinned pockets can be re-dragged to refine them; releasing re-pins them at the new position.
- The temporary in-flight TPS during drag runs in the **logical → image** direction (predicting where unpinned pockets appear on the photo). This is a separate, ephemeral computation in `QuickAlignViewModel` and is distinct from the final stored `TpsWarpData` emitted at `onFinishAlign`.

**Reset:**
- Tapping Reset clears all pinned positions **and** the pinned-set, returning all 6 pockets to their ideal undistorted positions with no constraints.

**Visual distinction:**
- Unpinned pockets: hollow/dashed circle — TPS-predicted, not user-confirmed.
- Pinned pockets: solid circle — user-confirmed.

**Removed constraint:** The existing side-pocket linear snap (snapping side pockets to the midpoint line between adjacent corners) is removed. All 6 pockets are fully free TPS control points.

---

## Section 2: TPS Solver and Data Model

### ThinPlateSpline.kt (new)

A pure-Kotlin TPS solver and evaluator. For N control points it solves two independent (N+3)×(N+3) linear systems — one per output axis (x and y). At N=6 this is two 9×9 solves (6 control points + 3 affine terms: constant, x-coefficient, y-coefficient).

**`ThinPlateSpline` exposes two static evaluation methods (named by warp direction, not pipeline role):**
- `applyWarp(tps: TpsWarpData, point: PointF): PointF` — maps src→dst as stored (image-space residual correction; used in CV pipeline after `inversePitchMatrix`)
- `applyInverseWarp(tps: TpsWarpData, point: PointF): PointF` — maps dst→src (solved by swapping src/dst; used in rendering to correct logical overlay points)

The CV section refers to `applyWarp` as the "forward residual TPS" and rendering uses `applyInverseWarp`. These names are directional with respect to the stored control points, not the overall image→logical pipeline direction.

Weights are **not stored in `TpsWarpData`** — they are solved on first use and cached at runtime in a `WeakHashMap<TpsWarpData, SolvedTps>` inside `ThinPlateSpline`. This avoids serializing Float arrays entirely, and both directions are available from a single stored data object.

**TpsWarpData.kt (new) — serializable state:**

```kotlin
@Keep
data class TpsWarpData(
    val srcPoints: List<PointF>,  // 6 image-space residual positions
    val dstPoints: List<PointF>   // 6 logical-space residual positions
)
```

Only the control points are persisted. Gson's default reflective serializer handles `PointF` (two public `float` fields `x` and `y`) correctly without a custom adapter — the same implicit behavior that already serializes `viewOffset`, `bankingAimTarget`, and other `PointF` fields in `CueDetatState`. No custom adapter is registered in `AppModule.kt`; this is implicit Gson behavior to be aware of if the serialization library is ever replaced.

### Output of QuickAlign (onFinishAlign)

**Required fix to existing code:** `QuickAlignViewModel.onFinishAlign()` currently builds the homography from only the 4 corner pockets (`TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_RIGHT`, `BOTTOM_LEFT`). This must be changed to use all 6 points including the side pockets.

Steps:
1. Fit homography to **all 6** final point positions → decompose to translation, rotation, scale.
2. For each of the 6 pockets, compute the residual: `actual image position − homography-predicted image position`.
3. Solve the residual TPS: source = 6 homography-predicted positions, destination = 6 actual user-placed positions (both in image space).
4. Emit `ApplyQuickAlign(translation, rotation, scale, tpsWarpData)` via `_alignResult`.

**Update sites for the `ApplyQuickAlign` signature change:**
- `QuickAlignViewModel._alignResult.emit(...)` — add `tpsWarpData` argument
- `ControlReducer.kt` `ApplyQuickAlign` handler — add `state.copy(lensWarpTps = action.tpsWarpData)`

The residual TPS encodes only the non-linear component. Applying it inside `canvas.withMatrix(pitchMatrix)` (which already encodes the linear pose) does not double-count anything.

### CueDetatState change

```kotlin
val lensWarpTps: TpsWarpData? = null
```

The field is nullable with a default of null. Existing serialized states deserialized by Gson will populate it as null, which is handled gracefully throughout — all warp calls check for null and pass points through unchanged.

`ApplyQuickAlign` handler in `ControlReducer` stores `lensWarpTps` from the event.

---

## Section 3: Rendering Integration

### Approach: per-point residual correction inside existing matrix context

The `canvas.withMatrix(pitchMatrix)` block is unchanged. Inside it, draw points pass through the inverse residual TPS before being used. If `lensWarpTps` is null (no alignment), points pass through unmodified.

**Helper extension** (lives in `view/renderer/TpsUtils.kt`):
```kotlin
fun PointF.warpedBy(tps: TpsWarpData?): PointF
```

**Points warped in TableRenderer:**
- Table corner points (4)
- Pocket centers (6)
- Diamond grid intersections
- Rail line endpoints

**Points warped in RailRenderer:**
- Rail corner points (same logical corners as `TableRenderer`) — must also be warped to avoid visible misalignment between table felt and rail overlay.
- Diamond positions in `RailRenderer` are currently derived from corner offsets using hardcoded axis-aligned normal vectors (`PointF(0f, -1f)` etc.). After warping the corners, these normals become incorrect. Normals must be recomputed from the warped corner positions (e.g., edge direction rotated 90°) so that diamond marks remain on the physical rail surface.

**Balls** (`BallRenderer`): `BallRenderer` draws in screen space (Pass 3 in `OverlayRenderer`, outside the `withMatrix` block). Ball logical positions are already TPS-corrected before they reach the aiming/rendering pipeline (via CV integration in Section 4). No direct change to `BallRenderer` is needed — the corrected logical position flows through the existing perspective projection unchanged.

**Aiming lines** (`LineRenderer`): start and end points warped; line drawn between them.

**Circles** (pockets, balls): only center point warped. Radius is not adjusted — pocket size does not vary meaningfully with lens distortion.

**No changes to:**
- `OverlayRenderer` matrix setup
- Sensor/pitch pipeline
- Aiming calculation logic (operates in logical space, which is TPS-corrected before it arrives there)

---

## Section 4: CV Integration

**Current ball detection pipeline:**
```
image pixel → imageToScreenMatrix → screen → inversePitchMatrix → logical
```

**With TPS residual:**
```
image pixel → imageToScreenMatrix → screen → inversePitchMatrix → logical → forward residual TPS → true logical
```

The forward TPS evaluation is added as a single step at the end of the coordinate transform in `VisionRepository`. If `lensWarpTps` is null, the step is skipped.

---

## Components Summary

| Component | Type | Change |
|---|---|---|
| `ThinPlateSpline.kt` | New | TPS solver (two N+3 systems) + forward/inverse evaluator |
| `TpsWarpData.kt` | New | Serializable TPS state (control points only; weights cached at runtime) |
| `TpsUtils.kt` | New | `PointF.warpedBy(tps)` extension, lives in `view/renderer/` |
| `QuickAlignViewModel.kt` | Modified | Accumulating TPS constraints on drag (background thread); residual TPS output; Reset clears pin set; homography uses all 6 points |
| `QuickAlignScreen.kt` | Modified | Visual distinction between pinned and unpinned pockets |
| `CueDetatState` (UiModel.kt) | Modified | Add `lensWarpTps: TpsWarpData?` |
| `MainScreenEvent.ApplyQuickAlign` | Modified | Add `tpsWarpData: TpsWarpData` parameter |
| `ControlReducer.kt` | Modified | Store `lensWarpTps` from `ApplyQuickAlign` |
| `TableRenderer.kt` | Modified | Warp draw points through inverse residual TPS |
| `RailRenderer.kt` | Modified | Warp rail corner points through inverse residual TPS; recompute diamond normals from warped corners |
| `LineRenderer.kt` | Modified | Warp aiming line endpoints through inverse residual TPS |
| `VisionRepository.kt` | Modified | Apply forward residual TPS after logical coordinate conversion |

---

## Out of Scope

- Per-pixel camera frame remapping (ruled out: 30fps cost too high)
- TPS applied to the full rendering matrix (ruled out: incompatible with real-time sensor tilt)
- Automatic table detection / scanning (separate sub-project)
