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

The camera pose is already in `CueDetatState`. The table model is a new top-level `TableScanModel` stored separately on disk via `TableScanRepository`.

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

Serialized via Gson to a dedicated file managed by `TableScanRepository` (not bundled with `CueDetatState`).

`CueDetatState` gains:
- `@Transient val tableScanModel: TableScanModel? = null`

The `@Transient` annotation prevents Gson from serializing this field into the `CueDetatState` preferences file. `TableScanRepository` is the sole on-disk owner of the model — `CueDetatState` holds only the runtime in-memory copy, populated on startup via `LoadTableScan`.

On app start, `MainViewModel.init` loads `TableScanModel` from `TableScanRepository` and dispatches `LoadTableScan(model)`. The `ControlReducer` handler for `LoadTableScan` sets `tableScanModel` and also copies `model.lensWarpTps` into `CueDetatState.lensWarpTps` — this is the mechanism that enforces Invariant 1 on resume.

### Location Threshold
100 metres. On app resume, if GPS is available and the device is more than 100 m from `scanLatitude/scanLongitude`, the app prompts: *"You may be at a different table — rescan?"* with options to rescan or keep. If GPS is unavailable, the stored model is kept silently.

### GPS Permission
`TableScanRepository.captureLocation()` uses `FusedLocationProviderClient`. Permission (`ACCESS_COARSE_LOCATION`) is requested at scan-start time (inside `TableScanScreen`) via the standard `rememberLauncherForActivityResult` Compose permission flow. If the user denies, `scanLatitude` and `scanLongitude` are stored as `null` — the location check is silently skipped on future resumes.

---

## Shared Utility: `HomographyUtils`

`decomposeHomography` currently lives as a private method in `QuickAlignViewModel`. Since `QuickAlignViewModel` is being removed and `VisionRepository` (and `TableScanViewModel`) both need homography decomposition, this function is extracted to a new top-level file `domain/HomographyUtils.kt` as a standalone function:

```kotlin
fun decomposeHomography(h: Mat, imgWidth: Float, imgHeight: Float): Triple<Offset, Float, Float>
```

`QuickAlignViewModel` (until removed) delegates to it. `TableScanViewModel` and `VisionRepository` call it directly.

---

## Section 1: Scan Phase (replaces QuickAlign)

### User Flow
1. User taps "Scan Table" in the nav menu (replaces the old "Quick Align" entry).
2. `TableScanScreen` opens with a live camera feed via a `CameraBackground`-style composable.
3. Six hollow pocket indicators are shown in the overlay, one per pocket identity, filling in as each is found. A label shows "N / 6 pockets found".
4. The user pans freely across the table.
5. Done unlocks when all 6 are found. Reset clears accumulated detections.
6. On Done, the model is saved and `CameraMode` cycles to `AR`.

### CameraX Wiring
`TableScanScreen` hosts a `CameraPreviewWithAnalysis` composable (analogous to the existing `CameraBackground`) that creates a CameraX `ImageAnalysis` use case and attaches a `TableScanAnalyzer`. `TableScanAnalyzer` is a new `ImageAnalysis.Analyzer` implementation that converts each `ImageProxy` to a Mat and calls `viewModel.onFrame(mat, imageToScreenMatrix)`. This class is added to the file map (see Section 4).

### Coordinate Transform (per frame)
Each detected pocket blob in image space must be converted to logical space via the same two-step pipeline used in `VisionRepository`:
1. **Image → Screen**: apply `imageToScreenMatrix` (provided to the analyzer from `CameraBackground`'s existing computation, or recomputed in `TableScanAnalyzer` from `ImageProxy` metadata and display dimensions).
2. **Screen → Logical**: apply `inversePitchMatrix` from `CueDetatState` (passed into the ViewModel or accessed via the state flow).

`inversePitchMatrix` is valid as long as `state.hasInverseMatrix == true` (set once the first `FullOrientationChanged` event is processed). The scan UI should show a loading indicator until `hasInverseMatrix` is true.

### Pocket Detection (per frame)
- Frame downsampled to ~480p before Hough processing to keep per-frame cost acceptable (pockets are large enough to survive downsampling).
- Hough circle transform on grayscale of the downsampled frame, radius range tuned for pocket size (larger and darker than balls — different parameter set from ball detection).
- ML Kit is not used as a pre-filter for pockets (it is tuned for balls); Hough runs directly on the downsampled frame.
- Each detected blob's centre upscaled back to full-frame coordinates, then projected to logical space via the two-step transform above.
- Projected positions accumulated into per-identity clusters using spatial deduplication: hits within 2 logical inches of an existing cluster centre are merged into that cluster.

### TableGeometryFitter
Pure-Kotlin class, fully unit testable. No Android dependencies.

**Input:** 6 unordered logical-space `PointF` cluster centers.

**Algorithm:**
1. Brute-force over all C(6,4) = 15 combinations of 4 points; for each, test whether the 4 points form a valid rectangle within aspect-ratio tolerance of the selected table's 2:1 ratio.
2. Select the combination with the lowest residual.
3. Assign the 2 remaining points as side pockets (nearest to long-edge midpoints).
4. Compute a confidence score from residuals.

**Output:** 6 identified, ordered `PointF` positions (TL/TR/BL/BR/SL/SR) + confidence score.

On success:
- Compute homography (detected image positions → identified logical positions) via `Calib3d.findHomography`.
- Decompose homography → `(translation, rotation, scale)` via `HomographyUtils.decomposeHomography`.
- Build residual TPS: `TpsWarpData(srcPoints = estimatedLogical, dstPoints = trueLogical)`.
- Emit `ApplyQuickAlign(translation, rotation, scale, tpsWarpData)` — unchanged event, unchanged reducer.
- Save `TableScanModel` to disk via `TableScanRepository`.

### TableScanViewModel
- Runs on `viewModelScope`, receives frames via `onFrame(mat, imageToScreenMatrix)`.
- Reads `inversePitchMatrix` and `hasInverseMatrix` from the state flow (injected via constructor or `@Inject`).
- Maintains mutable cluster state: `Map<PocketId, PocketCluster>`.
- Calls `TableGeometryFitter` once 6 clusters are stable (each with `observationCount ≥ 3`).
- Exposes `scanProgress: StateFlow<Map<PocketId, Boolean>>` for the UI pocket indicators.

---

## Section 2: AR Tracking (CameraMode.AR)

### Activation
`CameraMode` cycle: `OFF → CAMERA → AR → OFF`.
AR is only reachable if `state.tableScanModel != null`. The conditional skip is implemented in `ToggleReducer` for the `CycleCameraMode` event — not in `CameraMode.next()`, which has no access to state:

```kotlin
// In ToggleReducer:
CycleCameraMode -> {
    val next = state.cameraMode.next()
    val resolved = if (next == CameraMode.AR && state.tableScanModel == null)
        next.next()  // skip AR → OFF
    else next
    state.copy(cameraMode = resolved)
}
```

### Per-Frame Pipeline (every 5th frame)
Runs inside `VisionRepository` alongside existing ball detection. Uses the same two-step image→screen→logical transform as the scan phase.

**Step 1 — Pocket re-detection:**
- Hough circles on downsampled frame (same parameters as scan phase).
- Each detected blob projected to logical space via `imageToScreenMatrix` → `inversePitchMatrix`.
- Matched to nearest `PocketCluster.logicalPosition` within tolerance (~3 logical inches).
- Unmatched blobs discarded.

**Step 2 — Cluster update (persistent point cloud):**
- Each matched detection refines the cluster's `logicalPosition` via a weighted update:
  `newPosition = (observationCount * currentPosition + detectedPosition) / (observationCount + 1)`
- `observationCount` incremented, `variance` recalculated.
- Updated clusters are applied to `CueDetatState.tableScanModel` by emitting `UpdateTableScanClusters(updatedClusters)` — a new event handled by `ControlReducer`. This keeps `CueDetatState` as the single source of truth; `VisionRepository` never holds a separate mutable copy of the model.
- `TableScanRepository` saves the updated model to disk after every 10 new total observations or on app close.

**Step 3 — Pose update (if ≥ 2 matched pockets):**
- Solve new homography from matched image positions → known logical positions.
- Decompose to `(newTranslation, newRotation, newScale)` via `HomographyUtils.decomposeHomography`.
- Dead zone: skip if total pose delta is below threshold (< 0.5 logical inches equivalent).
- Exponential blend: `α = 0.15`.
  `blendedPose = α * newPose + (1 - α) * currentPose`
- Emit `UpdateArPose(translation, rotation, scale)` — new lightweight event that updates `viewOffset`, `worldRotationDegrees`, `zoomSliderPosition` only (does not touch `lensWarpTps` or `tableScanModel`).

**Step 4 — Edge-based fallback (if 0 pockets matched):**
- Detect the table boundary rectangle using contour detection, seeded by the stored `feltColorHsv`.
- Match detected edge lines to the expected table outline in logical space.
- If sufficient edges found, compute a lightweight pose correction from line correspondences.
- Same smoothing and dead zone apply.
- If neither pockets nor edges are found, IMU holds the current pose (existing behaviour).

### Full-Surface Warp Guarantee
All draw points in all renderers go through `PointF.warpedBy(state.lensWarpTps)` (implemented in the previous TPS feature). The `lensWarpTps` in `TableScanModel` feeds into `CueDetatState.lensWarpTps` via the `LoadTableScan` event on resume and via `ApplyQuickAlign` when the scan completes. Because every rendered point — table outline, rails, aiming lines, ball sprites — goes through this warp, the entire overlay moves as a single rigid body when the pose updates.

---

## Section 3: Reset & Resume Behaviour

### Reset
Pressing Reset clears:
- Camera pose: `viewOffset`, `worldRotationDegrees`, `zoomSliderPosition` reset to defaults.
- Current aiming state (existing Reset behaviour — unchanged).

`TableScanModel` survives Reset. The geometry is not lost; only the device's current positional calibration is cleared. Pocket re-detection will re-anchor on the next detected pocket.

### Rescan
Explicit "Rescan" action (available from the nav menu or by tapping the camera-cycle button while already in AR). Dispatches `ClearTableScan`, which sets `tableScanModel = null` and `lensWarpTps = null` in state, then navigates to `TableScanScreen`.

### Resume (app foregrounded)
1. `MainViewModel.init` calls `TableScanRepository.load()`.
2. If a model exists, dispatches `LoadTableScan(model)` → `ControlReducer` sets `tableScanModel` and copies `model.lensWarpTps` into `CueDetatState.lensWarpTps`.
3. GPS check: if location available and delta > 100 m, prompt to rescan or keep.
4. If model kept: `CameraMode` restores to the mode saved at last session close.
5. AR tracking immediately begins re-anchoring via pocket/edge detection in the live feed — no user action needed.

---

## Section 4: File Map

### New Files
| File | Responsibility |
|---|---|
| `domain/TableScanModel.kt` | `TableScanModel`, `PocketCluster`, `PocketId` |
| `domain/TableGeometryFitter.kt` | Pure-Kotlin 2:1 rectangle fitter |
| `domain/HomographyUtils.kt` | `decomposeHomography` extracted from `QuickAlignViewModel` |
| `data/TableScanRepository.kt` | Save/load `TableScanModel` to disk; GPS capture via `FusedLocationProviderClient` |
| `ui/composables/tablescan/TableScanScreen.kt` | Scan UI: camera feed + pocket progress + GPS permission request |
| `ui/composables/tablescan/TableScanViewModel.kt` | Frame accumulation, cluster management, fit trigger |
| `ui/composables/tablescan/TableScanAnalyzer.kt` | `ImageAnalysis.Analyzer` — converts `ImageProxy` to Mat, calls ViewModel |
| `test/.../TableGeometryFitterTest.kt` | Unit tests for geometry fitter |

### Modified Files
| File | Change |
|---|---|
| `domain/UiModel.kt` | Add `@Transient tableScanModel: TableScanModel?`; add `LoadTableScan`, `UpdateArPose`, `UpdateTableScanClusters`, `ClearTableScan` events; `CameraMode.next()` unchanged |
| `domain/reducers/ControlReducer.kt` | Handle `LoadTableScan` (set model + copy lensWarpTps), `UpdateArPose` (pose fields only), `UpdateTableScanClusters`, `ClearTableScan` |
| `domain/reducers/ToggleReducer.kt` | `CycleCameraMode`: skip AR if `tableScanModel == null` |
| `data/VisionRepository.kt` | AR mode: pocket re-detection + edge detection every 5th frame; emit `UpdateTableScanClusters` + `UpdateArPose` |
| `ui/MainViewModel.kt` | On init: load `TableScanModel`, dispatch `LoadTableScan`; GPS check on resume |
| `ui/ProtractorScreen.kt` | Reset clears camera pose; Rescan dispatches `ClearTableScan` |
| Navigation | Replace "Quick Align" entry with "Scan Table" |
| `domain/reducers/ActionReducer.kt` | Reset handler already clears `viewOffset`, `worldRotationDegrees`, `zoomSliderPosition` — no change needed to reset logic; verify these fields are cleared in the existing `MainScreenEvent.Reset` branch |

### Removed (user-facing only — pipeline code kept)
| File | Reason |
|---|---|
| `ui/composables/quickalign/QuickAlignScreen.kt` | Replaced by `TableScanScreen` |
| `ui/composables/quickalign/QuickAlignViewModel.kt` | Replaced by `TableScanViewModel`; `decomposeHomography` extracted to `HomographyUtils` first |
| `ui/composables/quickalign/QuickAlignAnalyzer.kt` | Replaced by `TableScanAnalyzer` |

### Kept Intact
`TpsWarpData`, `ThinPlateSpline`, `TpsUtils`, `ApplyQuickAlign` event, all renderers, `ControlReducer.ApplyQuickAlign` handler.

---

## Key Invariants

1. `lensWarpTps` in `CueDetatState` always mirrors `tableScanModel?.lensWarpTps`. Enforced by: `LoadTableScan` copies it on resume; `ApplyQuickAlign` sets both simultaneously on scan completion; `ClearTableScan` nulls both. Renderers never read from `tableScanModel` directly.
2. `UpdateArPose` never modifies `lensWarpTps` — the lens correction is fixed at scan time and only changes on rescan.
3. `CueDetatState.tableScanModel` is the single in-memory copy of the table model. `VisionRepository` updates it only by emitting `UpdateTableScanClusters` — it never holds a separate mutable copy. `TableScanRepository` owns the on-disk copy.
4. The scan's final step always emits `ApplyQuickAlign` — the same event QuickAlign used. The reducer and downstream state are identical regardless of whether alignment came from a scan or the old manual flow.
