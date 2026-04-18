# UI Enhancements and Manual Table Scanning Walkthrough

I have implemented the requested UI changes and the manual hole capture flow to improve the table alignment and control experience.

## Key Changes

### 1. Enhanced Navigation Rail Controls ([AzNavRailMenu.kt](file:///C:/Users/azrie/StudioProjects/CueDetat/app/src/main/java/com/hereliesaz/cuedetat/ui/composables/AzNavRailMenu.kt))
- **Solids/Stripes Toggle**: Switched to a toggle behavior with **white text** and a dark background for better visibility.
- **View Button**: Updated to use **white text**.
- **Felt Button**: Simplified to always show "felt" and open the scanning overlay.
- **New "Holes" Button**: Added under the "Felt" button to trigger the manual pocket capture sequence.

### 2. Universal Top-Down View ([OverlayRenderer.kt](file:///C:/Users/azrie/StudioProjects/CueDetat/app/src/main/java/com/hereliesaz/cuedetat/view/renderer/OverlayRenderer.kt))
- The "View" button now transitions the virtual table and balls to a **perfect top-down perspective** that fits the screen, regardless of whether a "patched" top-down bitmap has been generated.
- The animation logic in [ProtractorScreen.kt](file:///C:/Users/azrie/StudioProjects/CueDetat/app/src/main/java/com/hereliesaz/cuedetat/ui/ProtractorScreen.kt) was decoupled from the bitmap requirement.

### 3. Manual Hole Capture Wizard ([TableScanViewModel.kt](file:///C:/Users/azrie/StudioProjects/CueDetat/app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanViewModel.kt))
- Implemented `startManualHoleCapture()` which skips the initial felt color sampling and takes the user directly to the **Pocket Guide** step.
- Users can aim the central reticle at each pocket and tap to capture. The process continues until all **six pockets** are identified, at which point the table geometry is automatically fitted.

### 4. Event & State Logic ([UiModel.kt](file:///C:/Users/azrie/StudioProjects/CueDetat/app/src/main/java/com/hereliesaz/cuedetat/domain/UiModel.kt))
- Added `StartManualHoleCapture` to the `MainScreenEvent` system.
- Updated [ToggleReducer.kt](file:///C:/Users/azrie/StudioProjects/CueDetat/app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ToggleReducer.kt) to handle the new state transitions.

## Verification Summary
- **Build Success**: Successfully completed `:app:assembleDebug`.
- **UI Styling**: Verified that text colors for the "View" and "Solids/Stripes" buttons are set to white.
- **Manual Flow**: Confirmed the connection between the "Holes" button and the manual capture wizard in `ProtractorScreen.kt`.
- **Perspective Fitting**: The `OverlayRenderer` continues to use its 90% screen-fit calculation for the top-down view.
