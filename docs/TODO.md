# Cue D'état — Known Issues & Technical Debt

Last updated: 2026-03-28

---

## Priority 1 — Incomplete Implementations (Documented But Not Done)

### 1.1 Non-Blocking Tutorial Redesign
- **File:** `ui/composables/overlays/TutorialOverlay.kt`, `domain/reducers/TutorialReducer.kt`
- **Problem:** Current tutorial is a full-screen blocking canvas that prevents user interaction with the UI elements being described. The spec (`docs/03_UI_UX_Guide/05_Dialogs_And_Overlays.md`) explicitly calls this a placeholder that must be replaced.
- **Required:** Non-blocking transparent overlay that highlights specific UI elements while leaving them interactive, so the user can actually perform the described action during the tutorial.

### 1.2 `tableOverlayConfidence` Never Calculated
- **File:** `data/VisionRepository.kt`, `data/VisionData.kt`, `domain/reducers/CvReducer.kt`
- **Problem:** `VisionData.tableOverlayConfidence` is always `0.0f`. The field was added and `CvReducer` checks `>= 0.8` for auto-advance from `AR_SETUP → AR_ACTIVE`, but the actual computation was never implemented in `VisionRepository`. Auto-advance can therefore never trigger.
- **Spec:** `docs/superpowers/specs/2026-03-24-beginner-rendering-ar-flow-design.md` — confidence should rise during the `SCAN_TABLE` phase as table boundary coverage increases.

### 1.3 TFLite Pocket Detector Asset Verification
- **File:** `data/TFLitePocketDetector.kt`, `app/src/main/assets/`
- **Problem:** The TFLite YOLOv5 model file path/asset presence is not verified across all build variants. Graceful fallback to Hough circles exists, but if the asset is silently missing, the more capable path is always skipped without any diagnostic signal.

### 1.4 Lens Barrel Distortion Correction
- **File:** `ui/composables/calibration/`, `data/VisionRepository.kt`
- **Problem:** Quick Align corrects 4-point perspective homography only. Non-linear barrel/pincushion distortion (common on wide-angle phone cameras) is not corrected. Requires a full camera intrinsic matrix calibration workflow.
- **Note:** This is a large feature; needs its own spec before implementation.

---

## Priority 2 — Inconsistencies Between Docs and Code

### 2.1 ALGORITHMS.md AR Confidence Section Is Misleading
- **File:** `docs/ALGORITHMS.md`
- **Problem:** Describes `tableOverlayConfidence` as if it is actively computed and rises during table scanning. In reality it is always `0.0f`. Needs to be updated to either describe the intended (future) computation or flag the field as a stub.

### 2.2 Beginner AR Flow Spec Partially Complete
- **File:** `docs/superpowers/specs/2026-03-24-beginner-rendering-ar-flow-design.md`
- **Problem:** Issues 1–3 from that spec are implemented; Issue 4 (the AR/CV flow redesign including confidence) is half-done. The spec reads as if all issues are open; needs a status section reflecting what is and isn't done.

---

## Priority 3 — Architectural Risks

### 3.1 `@Transient` Field Invariant Not Type-Enforced
- **File:** `domain/UiModel.kt`, `domain/UpdateStateUseCase.kt`
- **Problem:** Many fields are `@Transient` (matrices, paths, vision data). If `CueDetatState` is ever serialized without immediately running `UpdateStateUseCase` afterwards, these fields silently default to `null`/zero, causing crashes or silent incorrect rendering. There is no compile-time or runtime guard enforcing this invariant.

### 3.2 Screen-Density-Dependent Spin Offset
- **File:** `domain/reducers/SpinReducer.kt`, `domain/UiModel.kt` (`selectedSpinOffset`)
- **Problem:** `selectedSpinOffset` stores raw pixel coordinates (not normalized). If screen density or display size changes mid-session (e.g., multi-window resize, display cutout), the stored offset becomes invalid.

### 3.3 Banking Mode Does Not Respect Active Spin State
- **File:** `domain/UpdateStateUseCase.kt`, `domain/CalculateBankShot.kt`
- **Problem:** `CalculateBankShot` accepts spin-adjusted angles, but the integration between active `SpinReducer` state and the bank path calculation is not enforced. It is possible to have spin active while in banking mode without the bank path reflecting the spin offset.

---

## Priority 4 — Code Quality & Maintainability

### 4.1 No Reducer Unit Tests
- **Files:** `domain/reducers/*.kt`
- **Problem:** All 11 reducers are untested. Physics engines, AR flow, and TPS have tests; the reducer pipeline — the core correctness mechanism — does not.

### 4.2 GestureReducer and ToggleReducer Size
- **Files:** `domain/reducers/GestureReducer.kt` (~9.5 KB), `domain/reducers/ToggleReducer.kt` (~10.5 KB)
- **Problem:** These are the two largest reducers and handle many conditionals. At risk of accumulating more logic. Candidates for splitting if further feature work lands in them.

### 4.3 `isGeometricallyImpossible` Threshold Hardcoded and Undocumented
- **File:** `domain/UpdateStateUseCase.kt`
- **Problem:** The cut angle beyond which a shot is declared geometrically impossible is hardcoded. The threshold value and whether spin adjustments affect it are not documented anywhere.

### 4.4 No KDoc / Inline Comments in Source
- **Scope:** Entire `app/src/` tree
- **Problem:** Code relies entirely on external `.md` docs for explanations. No KDoc on classes or public functions. Acceptable by design but increases the gap between docs and code over time.

---

## Priority 5 — Platform & Runtime

### 5.1 ARCore Initialization May Block
- **Files:** `data/ArDepthSession.kt`, `data/ArFrameProcessor.kt`
- **Problem:** ARCore initialization may run on the main thread in some paths. On supported devices with slow initialization, there is no loading state presented to the user during this window.

### 5.2 Camera Permission Denial Handling
- **File:** `ui/composables/CameraBackground.kt`, `MainActivity.kt`
- **Problem:** App requires camera permission on launch; behavior on denial is functional but the UX path is not explicitly designed or tested for repeated denial or "never ask again" scenarios.

---

## Completed / Tracked Elsewhere

- Beginner AR flow — Issues 1, 2, 3 from `2026-03-24` spec: **done**
- Table scan AR overlay — `2026-03-17` spec: **done**
- Pocket detection in path calculations: **done** (commit `ffbc2c4`)
- Spin control state refinements: **done** (commit `ffbc2c4`)
