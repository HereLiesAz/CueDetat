# Backlog

## Calibration follow-up: lens distortion never runs

Nothing sets `CueDetatState.cameraMatrix` or `CueDetatState.distCoeffs`
(`app/src/main/java/com/hereliesaz/cuedetat/domain/UiModel.kt`). Both are always `null`, so the
lens-distortion path in `view/renderer/util/DrawingUtils.kt` and the use in
`TutorialGuidanceTargets.kt` are never taken. Not implemented: it needs a calibration flow that
estimates intrinsics and distortion coefficients, stores them, and writes them to state. Until
then, treat lens distortion as unsupported.

## Orphaned features (events with no sender)

Each event below is handled by a reducer but nothing in the app sends it. Verified by grep for
the event name outside its declaration and reducer.

- `ViewArt` - opened the art/gallery view. Needs a UI entry point or deletion - owner decision.
- `ViewAboutPage` - opened the About page. Needs a UI entry point or deletion - owner decision.
- `SendFeedback` - launched the feedback flow. Needs a UI entry point or deletion - owner decision.
- `ToggleSnapping` - switched ball snapping (`isSnappingEnabled`) on and off. Needs a UI entry point or deletion - owner decision.
- `ToggleForceTheme` - forced a light or dark theme regardless of system. Needs a UI entry point or deletion - owner decision.
- `ToggleCvModel` - switched between the stock and custom CV model (`useCustomModel`). Needs a UI entry point or deletion - owner decision.
- `ToggleCvMask` - showed the CV debug mask. Needs a UI entry point or deletion - owner decision.
- `EnterCvMaskTestMode` / `ExitCvMaskTestMode` - entered and left the CV mask test mode. Needs a UI entry point or deletion - owner decision.
- `StartCalibrationMode` - started colour calibration. Needs a UI entry point or deletion - owner decision.
- `ToggleCalibrationScreen` - showed the calibration screen. Needs a UI entry point or deletion - owner decision.
- `SampleColorAt` - sampled a felt colour at a tapped point. Needs a UI entry point or deletion - owner decision.
- `ClearSamplePoint` - cleared the sampled colour point. Needs a UI entry point or deletion - owner decision.
- `LockOrUnlockColor` - locked or unlocked the detected colour. Needs a UI entry point or deletion - owner decision.
- `AutoCalibrateCv` - auto-calibrated CV colour thresholds. Needs a UI entry point or deletion - owner decision.
- `MoveFeltSample` - reordered saved felt samples. Needs a UI entry point or deletion - owner decision.
- `ToggleTableScanScreen` - showed the table scan screen. Needs a UI entry point or deletion - owner decision.
- `UpdateTableScanClusters` - updated the table scan colour clusters. Needs a UI entry point or deletion - owner decision.
- `ClearTableScan` - discarded the saved table scan. Needs a UI entry point or deletion - owner decision.

## Depth plane has no producer

`MainScreenEvent.DepthPlaneUpdated` / `CueDetatState.depthPlane` are only written by that event, and
no code sends it. `feature_expert_ar` never builds a `DepthPlane` (it only probes depth
capability), and git history (`git log -S DepthPlaneUpdated`) shows the event arriving with no
sender. Consumers (`ProtractorScreen.kt`, `VisionRepository.kt`, `UpdateStateUseCase.kt`) are
therefore dead on the depth branch. Removal touches `ui/ProtractorScreen.kt` and is left for the
UI owner; alternative is to produce it from ARCore depth in `ArTableSession`.
