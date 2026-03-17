# Table Scan & Persistent AR Overlay — Design Spec

**Date:** 2026-03-17
**Status:** Approved

---

## Overview

Replace the manual QuickAlign flow with an automatic table scanning system. The user pans their phone over the pool table; the app detects all six pockets, fits the table geometry, and produces a persistent table model. In AR mode, the overlay locks to the real table using continuous pocket re-detection and edge tracking, surviving app backgrounding and sessions.

---

## Goals

- User scans once; the overlay auto-aligns on every subsequent launch at the same table.
- Overlay tracks the table as the user walks around it — no manual re-alignment.
- Full table surface (geometry, rails, aiming lines) warps together as the overlay re-anchors.
- Overlay resists jitter: glides smoothly, never snaps.
- Table model improves over time through accumulated observations.
- Location check prevents a stale scan from a different table from being applied.

---

## Architecture

### Two Persistent Concerns, Kept Separate

| Concern | Description | Lifetime |
|---|---|---|
| **Table model** | 6 pocket clusters, lens warp TPS, felt color, GPS location | Persistent — survives sessions indefinitely |
| **Camera pose** | `viewOffset`, `worldRotationDegrees`, `zoomSliderPosition` | Ephemeral — re-established on each resume via pocket detection |

The camera pose is already in `CueDetatState`. The table model is a new top-level `TableScanModel` stored separately on disk.

---

## Data Model

### `PocketId` (enum)
```
TL, TR, BL, BR, SL, SR
```

### `PocketCluster`
```kotlin
data class PocketCluster(
    val identity: PocketId,
    val logicalPosition: PointF,   // current best estimate, logical space
    val observationCount: Int,
    val variance: Float             // shrinks with more observations → higher confidence
)
```

### `TableScanModel`
```kotlin
data class TableScanModel(
    val pockets: List<PocketCluster>,   // 6 entries, one per PocketId
    val lensWarpTps: TpsWarpData,
    val tableSize: TableSize,
    val feltColorHsv: FloatArray,       // sampled during scan for edge detection tuning
    val scanLatitude: Double?,          // null if GPS unavailable at scan time
    val scanLongitude: Double?
)
```

Serialized via Gson to a dedicated file (not bundled with `CueDetatState`). Loaded on app start. `CueDetatState` gains `tableScanModel: TableScanModel? = null`.

### Location Threshold
100 metres. On app resume, if GPS is available and the device is more than 100 m from `scanLatitude/scanLongitude`, the app prompts: *"You may be at a different table — rescan?"* with options to rescan or keep. If GPS is unavailable, the stored model is kept silently.

---

## Section 1: Scan Phase (replaces QuickAlign)

### User Flow
1. User taps "Scan Table" in the nav menu (replaces the old "Quick Align" entry).
2. `TableScanScreen` opens with a live camera feed.
3. Six hollow pocket indicators are shown in the overlay, one per pocket identity, filling in as each is found. A label shows "N / 6 pockets found".
4. The user pans freely across the table.
5. Done unlocks when all 6 are found. Reset clears accumulated detections.
6. On Done, the model is saved and `CameraMode` cycles to `AR`.

### Pocket Detection (per frame)
- Hough circle transform on grayscale frame, radius range tuned for pocket size (not ball size — larger, darker).
- Each detected blob projected to logical space using current `inversePitchMatrix`.
- Projected positions accumulated into per-identity clusters using spatial deduplication (nearby hits merged).

### TableGeometryFitter
Pure-Kotlin class, fully unit testable. No Android dependencies.

**Input:** 6 unordered logical-space `PointF` cluster centers.

**Algorithm:**
1. Find the 4 points that best form a rectangle (brute-force over combinations for N=6 is tractable).
2. Verify the rectangle's aspect ratio is within tolerance of the selected table's 2:1 ratio.
3. Assign the 2 remaining points as side pockets (nearest to long-edge midpoints).
4. Compute a confidence score from residuals.

**Output:** 6 identified, ordered `PointF` positions (TL/TR/BL/BR/SL/SR) + confidence score.

On success:
- Compute homography (detected image positions → identified logical positions) via `Calib3d.findHomography`.
- Decompose homography → `(translation, rotation, scale)` via existing `decomposeHomography`.
- Build residual TPS: `TpsWarpData(srcPoints = estimatedLogical, dstPoints = trueLogical)`.
- Emit `ApplyQuickAlign(translation, rotation, scale, tpsWarpData)` — unchanged event, unchanged reducer.
- Save `TableScanModel` to disk via `TableScanRepository`.

### TableScanViewModel
- Runs on `viewModelScope`, processes frames from CameraX.
- Maintains mutable cluster state: `Map<PocketId, PocketCluster>`.
- Calls `TableGeometryFitter` once 6 clusters are stable (each with `observationCount ≥ 3`).
- Exposes `scanProgress: StateFlow<Map<PocketId, Boolean>>` for the UI pocket indicators.

---

## Section 2: AR Tracking (CameraMode.AR)

### Activation
`CameraMode` cycle: `OFF → CAMERA → AR → OFF`.
AR is only reachable if `state.tableScanModel != null`. If not available, the cycle skips AR (`OFF → CAMERA → OFF`).

### Per-Frame Pipeline (every 5th frame)
Runs inside `VisionRepository` alongside existing ball detection.

**Step 1 — Pocket re-detection:**
- Hough circles (same parameters as scan phase).
- Each detected blob projected to logical space.
- Matched to nearest `PocketCluster.logicalPosition` within tolerance (~3 logical inches).
- Unmatched blobs discarded.

**Step 2 — Cluster update (persistent point cloud):**
- Each matched detection refines the cluster's `logicalPosition` via a Kalman-style weighted update:
  `newPosition = (observationCount * currentPosition + detectedPosition) / (observationCount + 1)`
- `observationCount` incremented, `variance` recalculated.
- Updated `TableScanModel` saved to disk after every 10 new observations or on app close.

**Step 3 — Pose update (if ≥ 2 matched pockets):**
- Solve new homography from matched image positions → known logical positions.
- Decompose to `(newTranslation, newRotation, newScale)`.
- Dead zone: skip if total pose delta is below threshold (< 0.5 logical inches equivalent).
- Exponential blend: `α = 0.15`.
  `blendedPose = α * newPose + (1 - α) * currentPose`
- Emit `UpdateArPose(translation, rotation, scale)` — new lightweight event that updates camera pose fields only (does not touch `lensWarpTps` or `tableScanModel`).

**Step 4 — Edge-based fallback (if 0 pockets matched):**
- Detect the table boundary rectangle using contour detection, seeded by the stored `feltColorHsv`.
- Match detected edge lines to the expected table outline in logical space.
- If sufficient edges found, compute a lightweight pose correction from line correspondences.
- Same smoothing and dead zone apply.
- If neither pockets nor edges are found, IMU holds the current pose (existing behaviour).

### Full-Surface Warp Guarantee
All draw points in all renderers go through `PointF.warpedBy(state.lensWarpTps)` (implemented in the previous TPS feature). The `lensWarpTps` in `TableScanModel` feeds into `CueDetatState.lensWarpTps`. Because every rendered point — table outline, rails, aiming lines, ball sprites — goes through this warp, the entire overlay moves as a single rigid body when the pose updates.

---

## Section 3: Reset & Resume Behaviour

### Reset
Pressing Reset clears:
- Camera pose: `viewOffset`, `worldRotationDegrees`, `zoomSliderPosition` reset to defaults.
- Current aiming state (existing Reset behaviour — unchanged).

`TableScanModel` survives Reset. The geometry is not lost; only the device's current positional calibration is cleared. Pocket re-detection will re-anchor on the next detected pocket.

### Rescan
Explicit "Rescan" action (available from the nav menu or by tapping the camera-cycle button while already in AR). Clears `TableScanModel` entirely and navigates to `TableScanScreen`.

### Resume (app foregrounded)
1. Load `TableScanModel` from disk (if present).
2. GPS check: if location available and delta > 100 m, prompt to rescan or keep.
3. If model kept: `CameraMode` restores to AR (or Camera if AR was active at last session).
4. AR tracking immediately begins re-anchoring via pocket/edge detection in the live feed — no user action needed.

---

## Section 4: File Map

### New Files
| File | Responsibility |
|---|---|
| `domain/TableScanModel.kt` | `TableScanModel`, `PocketCluster`, `PocketId` |
| `domain/TableGeometryFitter.kt` | Pure-Kotlin 2:1 rectangle fitter |
| `data/TableScanRepository.kt` | Save/load `TableScanModel` to disk; GPS capture |
| `ui/composables/tablescan/TableScanScreen.kt` | Scan UI: camera feed + pocket progress |
| `ui/composables/tablescan/TableScanViewModel.kt` | Frame accumulation, cluster management, fit trigger |
| `test/.../TableGeometryFitterTest.kt` | Unit tests for geometry fitter |

### Modified Files
| File | Change |
|---|---|
| `domain/UiModel.kt` | Add `tableScanModel: TableScanModel?`; add `UpdateArPose`, `SaveTableScan`, `ClearTableScan` events; CameraMode.AR skips if no model |
| `domain/reducers/ControlReducer.kt` | Handle `UpdateArPose` (update pose fields only), `SaveTableScan`, `ClearTableScan` |
| `data/VisionRepository.kt` | AR mode: pocket re-detection + edge detection every 5th frame; cluster update; pose emit |
| `ui/ProtractorScreen.kt` | Wire AR tracking; Reset clears camera pose |
| Navigation | Replace "Quick Align" entry with "Scan Table" |

### Removed (user-facing only — pipeline code kept)
| File | Reason |
|---|---|
| `ui/composables/quickalign/QuickAlignScreen.kt` | Replaced by `TableScanScreen` |
| `ui/composables/quickalign/QuickAlignViewModel.kt` | Replaced by `TableScanViewModel` |
| `ui/composables/quickalign/QuickAlignAnalyzer.kt` | Functionality absorbed into `TableScanViewModel` and `VisionRepository` |

### Kept Intact
`TpsWarpData`, `ThinPlateSpline`, `TpsUtils`, `ApplyQuickAlign` event, all renderers, `ControlReducer.ApplyQuickAlign` handler.

---

## Key Invariants

1. `lensWarpTps` in `CueDetatState` always mirrors `tableScanModel?.lensWarpTps`. Renderers never read from `tableScanModel` directly.
2. `UpdateArPose` never modifies `lensWarpTps` — the lens correction is fixed at scan time and only changes on rescan.
3. `TableScanModel` is never modified in-memory during a session except by `VisionRepository`'s cluster update path. All other code treats it as read-only.
4. The scan's final step always emits `ApplyQuickAlign` — the same event QuickAlign used. The reducer and downstream state are identical regardless of whether alignment came from a scan or the old manual flow.
