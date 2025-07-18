# Issues

## Open
- **Interactive Tutorial:** The full, interactive, non-blocking tutorial overlay still needs to be implemented. *Progress: Foundational UI for CV Calibration, a key part of the tutorial, is complete.*
- **Performance:** Investigate performance bottlenecks, particularly during complex CV processing.

## Closed
- **[CLOSED] Implement CV Refinement:** The initial implementation only used a generic ML Kit model. The full "Scout/Sniper" pipeline with OpenCV refinement is now complete.
- **[CLOSED] Improve Ball Detection Accuracy:** The previous model struggled with perspective and lighting. The new dynamic radius calculation and statistical color sampling have dramatically improved accuracy and robustness.
- **[CLOSED] Add CV Tuning UI:** The user had no way to see or correct what the CV system was seeing. The new calibration and mask testing UI resolves this.
- **[CLOSED] Fix Table Rendering:** The table rails and pockets were misaligned. This has been corrected by unifying all rendering under a single perspective matrix. The missing diamond grid has also been restored.
- **[CLOSED] Overhaul Menu UI:** The menu animation was standard and the colors were inconsistent. The animation is now a custom top-down reveal and the color scheme has been unified under `AccentGold`.
- **[CLOSED] Update Splash Screen:** The splash screen was generic. It has been updated with a larger logo and a more prominent catchphrase.
- **[CLOSED] Codebase Cleanup:** The codebase suffered from scattered constants and broken references after refactoring. This has been addressed by creating a central `Constants.kt` file and fixing all related compilation errors.