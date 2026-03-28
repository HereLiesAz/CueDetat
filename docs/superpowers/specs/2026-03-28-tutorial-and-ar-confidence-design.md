# Design: Non-Blocking Tutorial & AR Confidence Calculation

**Date:** 2026-03-28
**Status:** Approved
**Addresses:** TODO.md §1.1 (tutorial redesign) and §1.2 (tableOverlayConfidence never calculated)

---

## Overview

Two independent but co-planned features:

1. **Non-Blocking Action-Gated Tutorial** — replace the full-screen blocking overlay with a transparent, interaction-passthrough overlay where each step advances only when the user performs the described action. Skip and Back buttons always visible.

2. **AR Confidence Calculation** — implement `tableOverlayConfidence` in `TableGeometryFitter` (fit residual quality) and `CvReducer` (temporal smoothing + degraded auto-advance), making the `AR_SETUP → AR_ACTIVE` auto-advance actually reachable.

---

## Feature 1: Non-Blocking Action-Gated Tutorial

### Overlay Rendering Fix

`TutorialOverlay` currently draws a full-screen canvas that consumes all pointer input. Fix: remove all `pointerInput` modifiers from the overlay container. The dim + spotlight cutout effect is retained — drawn with Porter-Duff `Clear` mode to cut a hole over the highlighted element — but the overlay receives no touch events. Back and Skip are the only interactive children.

### Step-to-Action Mapping

`TutorialReducer` gains a `isTutorialStepCompleted(event, state): Boolean` function. Every event passes through it. When the tutorial is active and the event satisfies the current step's required action, `NextTutorialStep` fires alongside normal event processing. The triggering action executes normally — the ball moves, the toggle toggles, etc.

| Step | `TutorialHighlightElement` | Completing action |
|------|---------------------------|-------------------|
| 0 | `NONE` | Any `Drag` or `Rotate` event (introduction — any interaction advances) |
| 1 | `GHOST_BALL` | `Drag` event whose start point hits the target ball hit-box |
| 2 | `ZOOM_SLIDER` | `ZoomSliderChanged` or `Pinch` event |
| 3a Expert + `!table.isVisible` | `SCAN_TABLE` | `SetCameraMode(AR_SETUP)` event |
| 3b otherwise | `TARGET_BALL` | `Drag` event on target ball |
| 4 | World rotation widget | `TableRotationApplied` event |
| 5 | Banking button | `ToggleBankingMode` event |
| 6 | `NONE` | Skip button (relabelled "Done") — no action gate |

### Back Button

Decrements `currentTutorialStep` and re-derives `tutorialHighlight` from the new step index. Disabled on step 0. `TutorialHighlightElement` derivation moves fully into `TutorialReducer` (away from `TutorialOverlay`) so Back can recompute it correctly without UI-layer logic.

### State Changes

No new `CueDetatState` fields required. `currentTutorialStep`, `showTutorialOverlay`, `tutorialHighlight`, and `highlightAlpha` already exist. Moving `TutorialHighlightElement` derivation into the reducer (out of the composable) is the only structural change.

### Files Touched

| File | Change |
|------|--------|
| `domain/reducers/TutorialReducer.kt` | Add `isTutorialStepCompleted()`, move highlight derivation in, handle Back event |
| `domain/StateReducer.kt` | Call `TutorialReducer` for every event (not just tutorial events) |
| `ui/composables/overlays/TutorialOverlay.kt` | Remove `pointerInput` blocking; add Back button; remove highlight derivation |
| `domain/UiModel.kt` | Add `TutorialBack` to `MainScreenEvent` sealed class |

---

## Feature 2: AR Confidence Calculation

### TableGeometryFitter — Fit Quality Output

After solving the TPS system, the fitter has per-pocket residual errors. Add `fitQuality: Float` to the fitter's return type (wrap existing result in a `TableFitResult` data class, or add field to `TableScanModel`).

```
fitQuality = (1f - (meanPocketResidualPx / MAX_RESIDUAL_PX)).coerceIn(0f, 1f)
```

If fewer than 4 pockets are detected, `fitQuality = 0f` unconditionally.

### VisionRepository — Pass-Through

`runArTrackingPass()` already receives the fitter result. Forward `fitQuality` into `VisionData.tableOverlayConfidence`. No other changes in this layer.

### CvReducer — Temporal Smoothing + Degraded Advance

Two new `@Transient` fields on `CueDetatState`:

```kotlin
@Transient val arConfidenceHistory: List<Float> = emptyList()
@Transient val arLowConfidenceFrameCount: Int = 0
```

On every `CvFrameUpdate` while `cameraMode == AR_SETUP`:

1. Append new `visionData.tableOverlayConfidence` to history; drop oldest entry if `history.size > AR_CONFIDENCE_WINDOW`
2. `smoothedConfidence = history.average().toFloat()`
3. **Normal advance:** `smoothedConfidence >= AR_CONFIDENCE_THRESHOLD` → `cameraMode = AR_ACTIVE`; reset history and counter
4. **Degraded advance:** `smoothedConfidence in [AR_DEGRADED_FLOOR, AR_CONFIDENCE_THRESHOLD)` → increment `arLowConfidenceFrameCount`; if `arLowConfidenceFrameCount >= AR_DEGRADED_FRAME_COUNT` → `cameraMode = AR_ACTIVE` + set `warningText` to degraded-quality message; reset history and counter
5. **Floor miss:** `smoothedConfidence < AR_DEGRADED_FLOOR` → reset `arLowConfidenceFrameCount` to 0 (must hold floor confidence continuously)

### Constants

Extracted to a dedicated config object (e.g., `ArConfidenceConfig`):

| Constant | Value | Meaning |
|----------|-------|---------|
| `AR_CONFIDENCE_THRESHOLD` | `0.8f` | Normal auto-advance threshold |
| `AR_DEGRADED_FLOOR` | `0.5f` | Minimum for degraded advance eligibility |
| `AR_DEGRADED_FRAME_COUNT` | `150` | Frames at floor before degraded advance (~5s at 30fps) |
| `AR_CONFIDENCE_WINDOW` | `20` | Rolling average window size (~0.67s at 30fps) |
| `MAX_RESIDUAL_PX` | `50f` | Residual at which quality = 0 |

### Files Touched

| File | Change |
|------|--------|
| `domain/TableGeometryFitter.kt` | Add `fitQuality` to return type; compute from mean TPS residual |
| `data/VisionData.kt` | No change — field already exists |
| `data/VisionRepository.kt` | Forward `fitQuality` from fitter into `VisionData.tableOverlayConfidence` |
| `domain/reducers/CvReducer.kt` | Add smoothing, degraded advance logic, `ArConfidenceConfig` constants |
| `domain/UiModel.kt` | Add `@Transient arConfidenceHistory` and `@Transient arLowConfidenceFrameCount` |

### Doc Updates Required

| File | Change |
|------|--------|
| `docs/ALGORITHMS.md` | Update AR confidence section to describe actual formula and smoothing |
| `docs/superpowers/specs/2026-03-24-beginner-rendering-ar-flow-design.md` | Add status section: Issues 1–3 done, Issue 4 partially done (confidence now implemented) |
| `docs/TODO.md` | Mark §1.1 and §1.2 as in-progress once implementation begins |

---

## What Is Not In Scope

- Lens barrel distortion correction (TODO §1.4) — separate spec needed
- TFLite asset verification (TODO §1.3) — separate investigation
- Any changes to `AR_SETUP` UI beyond what's needed for confidence display
- Tutorial content/copy changes — steps and text remain as-is

---

## Testing Notes

- `TutorialReducer`: unit test each step's `isTutorialStepCompleted()` branch with mock events
- `TableGeometryFitter`: existing `TableGeometryFitterTest` — add assertions on `fitQuality` for known pocket layouts
- `CvReducer`: unit test smoothing window, normal advance, degraded advance, and floor-reset paths
