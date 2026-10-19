# Backlog

## Calibration follow-up: lens distortion never runs

Nothing sets `CueDetatState.cameraMatrix` or `CueDetatState.distCoeffs`
(`app/src/main/java/com/hereliesaz/cuedetat/domain/UiModel.kt`). Both are always `null`, so the
lens-distortion path in `view/renderer/util/DrawingUtils.kt` and the use in
`TutorialGuidanceTargets.kt` are never taken. Not implemented: it needs a calibration flow that
estimates intrinsics and distortion coefficients, stores them, and writes them to state. Until
then, treat lens distortion as unsupported.

## Orphaned features: resolved

Every `MainScreenEvent` that had a handler and no sender got an owner decision.

Deleted (event, reducer branch, router entry, and the state it alone fed):
`ViewArt`, `ViewAboutPage`, `SendFeedback` (with `SingleEvent.SendFeedbackEmail`, its
`MainActivity` handler and the `mailto` `<queries>` entry), `ToggleForceTheme`
(`isForceLightMode`), `ToggleCvModel` (`useCustomModel`), `EnterCvMaskTestMode` /
`ExitCvMaskTestMode` (`isTestingCvMask`), `StartCalibrationMode` / `ToggleCalibrationScreen`
(`showCalibrationScreen`), `SampleColorAt` / `ClearSamplePoint` (`colorSamplePoint`,
`isCalibratingColor`, the tapped-point branch in `VisionRepository`), `LockOrUnlockColor`,
`MoveFeltSample` (handler only; `savedFeltSamples` and its persistence stay),
`UpdateTableScanClusters`.

Kept after the check: `LockColor` and `lockedHsvColor` / `lockedHsvStdDev`. `LockColor` is not
an orphan: `TableScanViewModel` emits it when the felt is captured, and the locked colour gates
the `AR_SETUP -> AR_ACTIVE` auto-advance and the felt branches in `VisionRepository`.

Adopted (given a UI entry point):
- `ToggleSnapping`: `Snapping: on/off` row in the rail menu.
- `ToggleCvMask`: switch in the Advanced dialog.
- `AutoCalibrateCv`: auto-tune button in the Advanced dialog; the locked-colour gate is gone, and
  closing the dialog or turning the camera off stops the loop.
- `ToggleTableScanScreen`: `scan table` rail row in an AR mode (hidden while the overlay is up);
  from an AR mode the reducer now resets to a blank `AR_SETUP`, like `CycleCameraMode` from OFF.
- `ClearTableScan`: `forget scan` rail row when a scan is held; it also deletes the saved scan.

Follow-up: `SingleEvent.OpenUrl` and its `MainActivity` handler no longer have an emitter
(`ViewArt` / `ViewAboutPage` were the only ones).

## Depth plane has no producer

`MainScreenEvent.DepthPlaneUpdated` / `CueDetatState.depthPlane` are only written by that event, and
no code sends it. `feature_expert_ar` never builds a `DepthPlane` (it only probes depth
capability), and git history (`git log -S DepthPlaneUpdated`) shows the event arriving with no
sender. Consumers (`ProtractorScreen.kt`, `VisionRepository.kt`, `UpdateStateUseCase.kt`) are
therefore dead on the depth branch. Removal touches `ui/ProtractorScreen.kt` and is left for the
UI owner; alternative is to produce it from ARCore depth in `ArTableSession`.
