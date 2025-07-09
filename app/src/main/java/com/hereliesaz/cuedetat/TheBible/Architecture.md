# 02: Architectural Model & File Structure

The architecture strictly separates data, domain logic, and UI presentation following an MVI pattern.

## File Structure

app/src/main/java/com/hereliesaz/cuedetat/
├── data/
├── di/
├── domain/
├── network/
├── ui/
│   ├── composables/
│   ├── theme/
│   ├── MainScreen.kt
│   ├── MainScreenEvent.kt
│   └── MainViewModel.kt
├── view/
│   ├── config/
│   │   ├── base/
│   │   │   └── AppearanceDecree.kt
│   │   ├── ball/
│   │   │   ├── ActualCueBall.kt
│   │   │   ├── BankingBall.kt
│   │   │   ├── GhostCueBall.kt
│   │   │   └── TargetBall.kt
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
│   ├── model/
│   │   ├── ActualCueBallModel.kt
│   │   ├── LogicalCircular.kt
│   │   ├── Perspective.kt
│   │   └── ProtractorUnitModel.kt
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
│   │   │   └── DrawingUtils.kt
│   │   └── OverlayRenderer.kt
│   ├── state/
│   │   ├── InteractionMode.kt
│   │   ├── OverlayState.kt
│   │   ├── SingleEvent.kt
│   │   └── ToastMessage.kt
│   ├── PaintCache.kt
│   └── ProtractorOverlay.kt
├── MainActivity.kt
└── MyApplication.kt


## The Golden Rule

ViewModel orchestrates. `StateReducer` computes primary state. `UpdateStateUseCase` computes derived state. Renderers display.