# Changelog

## [UNRELEASED] - 2025-07-18

### Added
- **Hybrid CV Pipeline:** Implemented the full "Scout/Sniper" computer vision pipeline. ML Kit now acts as a "scout" to find regions of interest, which are then passed to an OpenCV "sniper" for high-precision refinement.
- **Advanced CV Refinement Methods:** Implemented both `HOUGH` and `CONTOUR` based circle finding algorithms for the OpenCV refinement step. `CONTOUR` is now the default, as it's more robust against perspective distortion.
- **Dynamic Radius Calculation:** The CV pipeline now calculates the expected on-screen radius of a ball based on its Y-coordinate (depth), allowing for much more accurate detection at varying distances.
- **Statistical Color Calibration:** Replaced single-pixel color sampling with a statistical method. The app now samples a 5x5 patch and calculates the mean and standard deviation for HSV values, creating a far more robust mask that is adaptive to lighting conditions.
- **UI for CV Calibration:** Added a dedicated UI flow for calibrating the felt color and testing the resulting CV mask.
- **Table Diamond Grid:** Re-implemented the diamond-to-diamond grid lines on the table surface.

### Changed
- **Menu Animation & Style:** The main menu animation has been changed from a side-reveal to a top-down unfold. The menu background is now a dark, semi-transparent color, and all menu text is rendered in `AccentGold`.
- **Slider Thumb Color:** All sliders in the app now use `AccentGold` for their thumb and active track colors.
- **Splash Screen:** The splash screen logo has been significantly enlarged. The app name has been removed, replaced with the catchphrase "The Geometry of the Hustle" in `AccentGold`.
- **Architectural Refactoring:** Consolidated scattered constants into a single `domain/Constants.kt` file.

### Fixed
- **Table Rendering Alignment:** Corrected a critical bug where table rails and pockets were being drawn with different perspective matrices, causing visual misalignment. All table components now render from a single, unified `pitchMatrix`.
- **Numerous Compilation Errors:** Resolved a cascade of unresolved references (`diamonds`, `pocketRadius`, `SlateGray`, etc.) caused by previous refactoring efforts.
- **State Management Consistency:** Corrected the `UpdateStateUseCase` to remove dependencies on a previously deleted, redundant perspective matrix.