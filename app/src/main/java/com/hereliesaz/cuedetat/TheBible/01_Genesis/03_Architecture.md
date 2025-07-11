# Architectural Model & File Structure

The architecture strictly separates data, domain logic, and UI presentation following an MVI pattern.

## File Structure

app/src/main/java/com/hereliesaz/cuedetat/
├── data/
│   ├── GithubRepository.kt
│   ├── SensorRepository.kt
│   ├── UserPreferencesRepository.kt
│   ├── VisionAnalyzer.kt
│   ├── VisionData.kt
│   └── VisionRepository.kt
├── di/
│   ├── AppModule.kt
│   └── Qualifiers.kt
├── domain/
│   ├── reducers/
│   │   ├── ActionReducer.kt
│   │   ├── AdvancedOptionsReducer.kt
│   │   ├── ControlReducer.kt
│   │   ├── CvReducer.kt
│   │   ├── GestureReducer.kt
│   │   ├── ObstacleReducer.kt
│   │   ├── SpinReducer.kt
│   │   ├── SystemReducer.kt
│   │   ├── ToggleReducer.kt
│   │   └── TutorialReducer.kt
│   ├── CalculateBankShot.kt
│   ├── CalculateSpinPaths.kt
│   ├── ReducerUtils.kt
│   ├── StateReducer.kt
│   ├── UpdateStateUseCase.kt
│   └── WarningManager.kt
├── network/
│   └── GithubApi.kt
├── ui/
│   ├── composables/
│   │   ├── dialogs/
│   │   │   ├── AdvancedOptions.kt
│   │   │   ├── GlowStickDialog.kt
│   │   │   ├── LuminanceAdjustmentDialog.kt
│   │   │   └── TableSizeSelectionDialog.kt
│   │   ├── overlays/
│   │   │   ├── KineticWarning.kt
│   │   │   └── TutorialOverlay.kt
│   │   ├── sliders/
│   │   │   └── TableRotationSlider.kt
│   │   ├── ActionFabs.kt
│   │   ├── CameraBackground.kt
│   │   ├── MenuDrawer.kt
│   │   ├── SpinControl.kt
│   │   ├── TopControls.kt
│   │   └── ZoomControls.kt
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Shape.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── CameraPreview.kt
│   ├── MainScreen.kt
│   ├── MainScreenEvent.kt
│   ├── MainViewModel.kt
│   ├── MenuAction.kt
│   ├── Modifiers.kt
│   ├── VerticalSlider.kt
│   └── ZoomMapping.kt
├── view/
│   ├── config/
│   │   ├── ball/
│   │   │   ├── ActualCueBall.kt
│   │   │   ├── BankingBall.kt
│   │   │   ├── GhostCueBall.kt
│   │   │   ├── ObstacleBall.kt
│   │   │   └── TargetBall.kt
│   │   ├── base/
│   │   │   └── VisualProperties.kt
│   │   ├── line/
│   │   │   ├── AimingLine.kt
│   │   │   ├── BankLine1.kt
│   │   │   ├── BankLine2.kt
│   │   │   ├── BankLine3.kt
│   │   │   ├── BankLine4.kt
│   │   │   ├── ShotGuideLine.kt
│   │   │   └── TangentLine.kt
│   │   ├── table/
│   │   │   ├── Diamonds.kt
│   │   │   ├── Holes.kt
│   │   │   ├── Rail.kt
│   │   │   └── Table.kt
│   │   └── ui/
│   │       └── ProtractorGuides.kt
│   ├── gestures/
│   │   └── GestureHandler.kt
│   ├── model/
│   │   ├── LogicalCircular.kt
│   │   ├── OnPlaneBall.kt
│   │   ├── Perspective.kt
│   │   └── ProtractorUnit.kt
│   ├── renderer/
│   │   ├── ball/
│   │   │   └── BallRenderer.kt
│   │   ├── line/
│   │   │   └── LineRenderer.kt
│   │   ├── table/
│   │   │   ├── RailRenderer.kt
│   │   │   └── TableRenderer.kt
│   │   ├── text/
│   │   │   ├── BallTextRenderer.kt
│   │   │   └── LineTextRenderer.kt
│   │   ├── util/
│   │   │   ├── DrawingUtils.kt
│   │   │   └── SpinColorUtil.kt
│   │   └── OverlayRenderer.kt
│   ├── state/
│   │   ├── OverlayState.kt
│   │   └── ScreenState.kt
│   ├── PaintCache.kt
│   └── ProtractorOverlay.kt
├── MainActivity.kt
└── MyApplication.kt


## The Golden Rule

ViewModel orchestrates. `StateReducer` delegates to its subordinate ministers to compute primary state changes. `UpdateStateUseCase` computes derived perspective data. `CalculateBankShot` computes banking paths. Renderers display. This is the law.