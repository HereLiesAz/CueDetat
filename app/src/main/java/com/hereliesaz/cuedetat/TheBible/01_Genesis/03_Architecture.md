# Architectural Model & File Structure

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
│   ├── MainViewModel.kt
│   └── Modifiers.kt
├── view/
│   ├── config/
│   │   ├── base/
│   │   ├── ball/
│   │   ├── line/
│   │   └── table/
│   ├── gestures/
│   │   └── GestureHandler.kt
│   ├── model/
│   ├── renderer/
│   │   ├── ball/
│   │   ├── line/
│   │   ├── table/
│   │   └── text/
│   ├── state/
│   ├── PaintCache.kt
│   └── ProtractorOverlay.kt
├── MainActivity.kt
└── MyApplication.kt


## The Golden Rule

ViewModel orchestrates. `StateReducer` computes primary state. `UpdateStateUseCase` computes derived state. Renderers display.