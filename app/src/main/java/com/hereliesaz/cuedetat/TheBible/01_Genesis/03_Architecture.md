# Architectural Model & File Structure

The architecture strictly separates data, domain logic, and UI presentation following an MVI pattern.

## File Structure

app/src/main/java/com/hereliesaz/cuedetat/
├── data/
│   ├── UserPreferencesRepository.kt
│   └── ...
├── di/
├── domain/
│   ├── reducers/
│   │   ├── ActionReducer.kt
│   │   ├── ControlReducer.kt
│   │   ├── GestureReducer.kt
│   │   ├── SystemReducer.kt
│   │   └── ToggleReducer.kt
│   ├── CalculateBankShot.kt
│   ├── StateReducer.kt
│   ├── UpdateStateUseCase.kt
│   └── ...
├── network/
├── ui/
│   ├── composables/
│   │   ├── dialogs/
│   │   │   ├── LuminanceAdjustmentDialog.kt
│   │   │   └── TableSizeSelectionDialog.kt
│   │   ├── overlays/
│   │   │   ├── KineticWarning.kt
│   │   │   └── TutorialOverlay.kt
│   │   ├── sliders/
│   │   │   └── TableRotationSlider.kt
│   │   ├── ActionFabs.kt
│   │   ├── ...
│   ├── theme/
│   ├── MainScreen.kt
│   ├── MainScreenEvent.kt
│   ├── MainViewModel.kt
│   └── ...
├── view/
│   ├── ...
├── MainActivity.kt
└── MyApplication.kt


## The Golden Rule

ViewModel orchestrates. `StateReducer` delegates to its subordinate ministers to compute primary state changes. `UpdateStateUseCase` computes derived perspective data. `CalculateBankShot` computes banking paths. Renderers display. This is the law.