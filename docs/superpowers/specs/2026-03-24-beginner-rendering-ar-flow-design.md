# Design Spec: Beginner Rendering Fixes + AR Flow Redesign

**Date:** 2026-03-24
**Issues:** 4 independent changes across dynamic beginner mode, static beginner rendering, and AR/CV flow

---

## Issue 1 — Dynamic Beginner: Shot Line Anchor & Gesture Rules

**Context:** In dynamic beginner mode (`experienceMode == BEGINNER && !isBeginnerViewLocked`), the shot line's near-end anchor drifts when the view pans. Pan events must be suppressed entirely in this mode.

### Rules

- `getLogicalShotLineAnchor` returns screen-center-bottom mapped through the pitch/tilt matrix — always, in dynamic beginner mode.
- `onPlaneBall` is guaranteed null in dynamic beginner mode by existing `LockBeginnerView` and `SetExperienceMode(BEGINNER)` handlers; no additional enforcement is needed.
- The user may **drag the target ball** (protractorUnit) to any position on screen — `MOVING_PROTRACTOR_UNIT` interaction mode remains available.
- The user may **rotate** to aim — `ROTATING_PROTRACTOR` interaction mode remains available.

### Pan suppression — `GestureHandler.kt`

There are two `PanView` emission sites that must both be guarded. The current outer `if` at line ~70 explicitly allows the two-finger block to run in dynamic beginner mode; adding only an inner guard is insufficient. The condition at line ~70 must be restructured:

**Two-finger block (~line 70):** Change the outer condition from:
```kotlin
if (uiState.table.isVisible || (uiState.experienceMode == ExperienceMode.BEGINNER && !uiState.isBeginnerViewLocked))
```
To (keep rotation but exclude pan from within the block):
```kotlin
if (uiState.table.isVisible) {
    // rotation and pan (unchanged)
} else if (uiState.experienceMode == ExperienceMode.BEGINNER && !uiState.isBeginnerViewLocked) {
    // rotation only — DO NOT emit PanView
    val rotation = event.calculateRotation()
    if (rotation != 0f) onEvent(MainScreenEvent.TableRotationApplied(rotation))
    // PanView NOT emitted
}
```

**Single-finger world-locked block (~line 93):** The current guard `uiState.isWorldLocked && (uiState.experienceMode != ExperienceMode.BEGINNER || !uiState.isBeginnerViewLocked)` emits pan when `isWorldLocked == true` and dynamic beginner is active. Add dynamic beginner suppression:
```kotlin
if (uiState.isWorldLocked &&
    !(uiState.experienceMode == ExperienceMode.BEGINNER && !uiState.isBeginnerViewLocked)) {
    onEvent(MainScreenEvent.PanView(...))
}
```

---

## Issue 2 — Static Beginner: Draw Order (Z-index)

**Context:** Direction lines and triangles render above balls in the current Pass 4. The inline text drawn by `drawAimingLines` and `drawTangentLines` must also move to the top pass. A new bubble center dot element is also being added (see Issue 3).

### Render Order (first drawn = bottom, last drawn = top)

| Pass | What is drawn | Notes |
|------|--------------|-------|
| 1–2 | Logical lines, rails | Unchanged |
| 2.5 | Direction lines + triangles only — NO text | Inserted between rails and balls in `OverlayRenderer` |
| 3a | Static stroke circles (stroke + glow) | In `BallRenderer.drawGhostedBall` |
| 3b | Bubble fill circles (translucent fill) | In `BallRenderer.drawGhostedBall` |
| 3c | Static center dots (white FILL, no stroke) | In `BallRenderer.drawGhostedBall` |
| 3d | Bubble center dots (white STROKE, no fill) | In `BallRenderer.drawGhostedBall` — new element |
| 4 | All text labels (line-attached + ball labels) | `drawBeginnerLabels` + existing `drawAllLabels` tail |

### Implementation — `LineRenderer.kt`

Add a `drawGeometry: Boolean = true` parameter to both `drawAimingLines` and `drawTangentLines`. When `drawGeometry = false`, the method skips all path/line drawing and renders only the text label. When `drawGeometry = true` (default), it skips text (passes `textToDraw = null` or equivalent) and draws only geometry.

Split `drawBeginnerForeground` into two new methods:

```kotlin
fun drawBeginnerLines(canvas, state, paints, activeMatrix) {
    // canvas.save(); canvas.concat(activeMatrix) wrapper as today
    drawTangentLines(..., drawGeometry = true)   // geometry only, no text
    drawAimingLines(..., drawGeometry = true)    // geometry only, no text
    // canvas.restore()
}

fun drawBeginnerLabels(canvas, state, paints, typeface, activeMatrix) {
    // canvas.save(); canvas.concat(activeMatrix) wrapper
    drawTangentLines(..., drawGeometry = false)  // text only, no geometry
    drawAimingLines(..., drawGeometry = false)   // text only, no geometry
    // canvas.restore()
}
```

### Implementation — `OverlayRenderer.kt`

```kotlin
// Pass 2: rails (unchanged)

// Pass 2.5: beginner direction lines + triangles (no text)
if (isBeginnerLocked) lineRenderer.drawBeginnerLines(canvas, state, paints, matrixFor2DPlane)

// Pass 3: balls (unchanged — drawGhostedBall handles 3a–3d internally)
//   drawAllLabels is called at the end of ballRenderer.draw() — no change needed

// Pass 4: beginner line-attached text labels
if (isBeginnerLocked) lineRenderer.drawBeginnerLabels(canvas, state, paints, typeface, matrixFor2DPlane)
```

### Bubble Center Dot (new element, pass 3d)

This element does not exist in the current codebase and must be added in `BallRenderer.drawGhostedBall` beginner-locked branch. It is a small hollow circle (white `Paint.Style.STROKE`, no fill) centered on the bubble, with the same radius as the static center dot (~10% of circle radius). It moves with `bubbleCenter`. Draw it last within `drawGhostedBall`, after the static center dot.

---

## Issue 3 — Static Beginner: Circle Fill & Stroke Rules

**Context:** `createGlowPaint` in `PaintUtils.kt` always uses `BlurMaskFilter.Blur.NORMAL` (hardcoded at line ~59 and ~66), which bleeds inward. The current cache key `"glow_${color}_${width}"` does not include blur type.

### `PaintUtils.kt` — add `blurType` parameter

```kotlin
fun createGlowPaint(
    baseGlowColor: Color,
    baseGlowWidth: Float,
    state: CueDetatState,
    paints: PaintCache,
    blurType: BlurMaskFilter.Blur = BlurMaskFilter.Blur.NORMAL  // new parameter
): Paint
```

Update the cache key to include blur type:
```kotlin
val key = "glow_${baseGlowColor}_${baseGlowWidth}_${blurType.name}"
```

All existing callers omit the parameter and continue using `Blur.NORMAL` unchanged.

### Static Circle (locked position on table)

- Ring: `Paint.Style.STROKE` only — no fill
- Glow: call `createGlowPaint(..., blurType = BlurMaskFilter.Blur.OUTER)` — outward only
- Center dot: white `Paint.Style.FILL`, no stroke; radius ≈ 10% of circle radius

### Bubble (moves with tilt)

- Ring: `Paint.Style.FILL` only, translucent (≤ 15% alpha) — no stroke, no glow
- Center dot: white `Paint.Style.STROKE` only, no fill; same radius as static center dot; centered on `bubbleCenter`

### Draw order within `drawGhostedBall` (beginner-locked branch)

1. `glowPaint` on static circle — `createGlowPaint(..., blurType = Blur.OUTER)`
2. `strokePaint` on static circle (`STROKE` only)
3. Bubble fill circle (`FILL` only, translucent)
4. Static center dot (white `FILL`, at `logicalScreenPos`)
5. Bubble center dot (white `STROKE`, no fill, at `bubbleCenter`) ← new

---

## Issue 4 — AR / CV Flow Redesign

**Context:** Pocket identification is unreliable. The new flow introduces a guided AR setup wizard using felt color + edge detection. The camera mode enum gains two new values and new state-machine transitions.

### State Machine

```
OFF  →(tap AR)→  AR_SETUP  →(auto-advance when confident)→  AR_ACTIVE
                     |
               (tap Cancel)
                     ↓
               CAMERA_ONLY  →(tap Off)→  OFF

AR_ACTIVE → AR_SETUP  (on tracking loss)
```

| State | Camera | CV | Description |
|-------|--------|----|-------------|
| OFF | off | off | Default state |
| AR_SETUP | on | on (calibration) | Guided wizard runs |
| AR_ACTIVE | on | on (tracking) | Ball selection active |
| CAMERA_ONLY | on | off | Post-cancel; "Off" button kills camera |

### `domain/UiModel.kt` changes

**`CameraMode` enum** — add `AR_SETUP` and `CAMERA_ONLY`:
```kotlin
enum class CameraMode { OFF, CAMERA, AR, AR_SETUP, AR_ACTIVE, CAMERA_ONLY; ... }
```
Note: the old `AR` value should be renamed `AR_ACTIVE` for clarity; update all existing references.

**New `MainScreenEvent` entries** in the sealed class:
```kotlin
object CancelArSetup : MainScreenEvent()       // AR_SETUP → CAMERA_ONLY
object TurnCameraOff : MainScreenEvent()       // CAMERA_ONLY → OFF
object ArTrackingLost : MainScreenEvent()      // AR_ACTIVE → AR_SETUP (tracking lost)
```

**`tableOverlayConfidence`** — add to `VisionData`:
```kotlin
val tableOverlayConfidence: Float = 0f   // 0..1, set by CV pipeline during AR_SETUP
```
The CV pipeline sets this to a value ≥ 0.8f when the table boundary is stably detected and aligned. The named constant `AR_AUTO_CONFIRM_CONFIDENCE_THRESHOLD = 0.8f` is defined in `ControlReducer` or a companion object.

**`arSetupStep`** — derived, not stored. Both `ArStatusOverlay` and `ControlReducer` compute it as needed:
```kotlin
val arSetupStep: ArSetupStep = when {
    state.lockedHsvColor == null -> ArSetupStep.PICK_COLOR
    state.tableScanModel == null -> ArSetupStep.SCAN_TABLE
    else -> ArSetupStep.VERIFY
}
```
Add `enum class ArSetupStep { PICK_COLOR, SCAN_TABLE, VERIFY }` to `UiModel.kt`.

### `domain/reducers/ToggleReducer.kt`

Replace the existing `CycleCameraMode` binary toggle with the new state-machine logic:
```kotlin
is MainScreenEvent.CycleCameraMode -> when (state.cameraMode) {
    CameraMode.OFF, CameraMode.CAMERA_ONLY -> state.copy(cameraMode = CameraMode.AR_SETUP)
    else -> state  // no-op from AR_SETUP or AR_ACTIVE
}
is MainScreenEvent.CancelArSetup -> state.copy(cameraMode = CameraMode.CAMERA_ONLY)
is MainScreenEvent.TurnCameraOff -> state.copy(cameraMode = CameraMode.OFF)
```

### `domain/reducers/ControlReducer.kt`

Handle `ArTrackingLost` — clears tracking state and returns to wizard:
```kotlin
is MainScreenEvent.ArTrackingLost -> state.copy(
    tableScanModel = null,
    cameraMode = if (state.cameraMode == CameraMode.AR_ACTIVE) CameraMode.AR_SETUP else state.cameraMode
)
```

Handle auto-advance during `CvDataUpdated` — when in `AR_SETUP` and all wizard conditions are met:
```kotlin
// Inside CvDataUpdated handler, after updating visionData:
val shouldAutoAdvance = state.cameraMode == CameraMode.AR_SETUP &&
    state.lockedHsvColor != null &&
    state.tableScanModel != null &&
    action.visionData.tableOverlayConfidence >= AR_AUTO_CONFIRM_CONFIDENCE_THRESHOLD
if (shouldAutoAdvance) updatedState.copy(cameraMode = CameraMode.AR_ACTIVE) else updatedState
```

### `data/ArCoreBackground.kt` (or `ArDepthSession.kt`)

Dispatch `ArTrackingLost` when ARCore tracking state transitions from `TRACKING` to `PAUSED`:
```kotlin
// In per-frame callback, compare previous vs current camera.trackingState
if (previousTrackingState == TrackingState.TRACKING &&
    camera.trackingState == TrackingState.PAUSED) {
    onEvent(MainScreenEvent.ArTrackingLost)
}
```

### AR Setup Wizard

**First-time (full wizard):**
1. `PICK_COLOR` — user taps table surface to sample felt color → `lockedHsvColor` set
2. `SCAN_TABLE` — user points camera at table; CV detects boundary → `tableScanModel` set + `tableOverlayConfidence` rises
3. `VERIFY` — auto-confirmed when `tableOverlayConfidence >= AR_AUTO_CONFIRM_CONFIDENCE_THRESHOLD`; if confidence is low, step stays active until confidence improves

**After screen-off / resume (tracking lost):**
- `ArTrackingLost` is dispatched → `tableScanModel` cleared, `cameraMode = AR_SETUP`
- Wizard starts at `SCAN_TABLE` (PICK_COLOR skipped because `lockedHsvColor != null`)
- `VERIFY` auto-confirms as before

**Low confidence:** Wizard remains on the current step (highlighted). Cancel is always available (→ `CAMERA_ONLY`).

### UI — `AzNavRailMenu.kt`

- AR button sends `CycleCameraMode` only when `cameraMode == OFF || cameraMode == CAMERA_ONLY`. From `AR_SETUP` or `AR_ACTIVE` the button does not emit the event (button appears active/lit).
- During `AR_SETUP`: show a Cancel button → emits `CancelArSetup`
- During `CAMERA_ONLY`: show an Off button → emits `TurnCameraOff`

### UI — `ArStatusOverlay.kt`

Show wizard step list with current step derived from `arSetupStep`. Completed steps are shown with a checkmark (crossed through if skipped). Pending step is highlighted. No user confirm step — all advances are automatic.

---

## Files Affected

| File | Issue(s) |
|------|---------|
| `view/gestures/GestureHandler.kt` | 1 (restructure outer two-finger `if`; add dynamic beginner guard to single-finger world-locked pan) |
| `view/renderer/line/LineRenderer.kt` | 2 (split `drawBeginnerForeground`; add `drawGeometry` param to `drawAimingLines` + `drawTangentLines`) |
| `view/renderer/OverlayRenderer.kt` | 2 (insert Pass 2.5 for lines; add Pass 4 label call) |
| `view/renderer/ball/BallRenderer.kt` | 2, 3 (add bubble center dot; reorder 3a–3d; pass `Blur.OUTER` to glow) |
| `view/renderer/util/PaintUtils.kt` | 3 (add `blurType` param + update cache key) |
| `domain/UiModel.kt` | 4 (add `AR_SETUP`, `AR_ACTIVE`, `CAMERA_ONLY` to `CameraMode`; rename `AR` → `AR_ACTIVE`; add `ArSetupStep` enum; add `CancelArSetup`, `TurnCameraOff`, `ArTrackingLost` events) |
| `data/VisionData.kt` | 4 (add `tableOverlayConfidence: Float = 0f`) |
| `domain/reducers/ToggleReducer.kt` | 4 (replace `CycleCameraMode` handler; add `CancelArSetup`, `TurnCameraOff` handlers) |
| `domain/reducers/ControlReducer.kt` | 4 (`ArTrackingLost` handler) |
| `domain/reducers/CvReducer.kt` | 4 (auto-advance check on `CvDataUpdated` — `CvDataUpdated` is routed here via `StateReducer`, not `ControlReducer`) |
| `domain/StateReducer.kt` | 4 (add routing for `ArTrackingLost` and `CancelArSetup`/`TurnCameraOff` to the appropriate reducers) |
| `data/ArCoreBackground.kt` | 4 (dispatch `ArTrackingLost` on tracking state transition) |
| `ui/composables/AzNavRailMenu.kt` | 4 (Cancel button in `AR_SETUP`, Off button in `CAMERA_ONLY`; gate `CycleCameraMode` emission) |
| `ui/composables/overlays/ArStatusOverlay.kt` | 4 (wizard step display; derive `arSetupStep` from state) |
