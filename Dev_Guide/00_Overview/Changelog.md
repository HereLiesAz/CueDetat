# Changelog & Issues

[cite_start]This document is the definitive record of completed work and open issues. [cite: 1647]
It is the final word on
[cite_start]the state of the project, superseding all other records. [cite: 1647]
---

## I. Changelog

A log of completed work.

### [UNRELEASED] - 2025-07-23

#### Added

- **Feature: Hater Mode:** Implemented the full "Magic 8-Ball" feature, including a new UI,
  shake-to-activate logic, and a full set of cynical responses, as per the specification.
- [cite_start]**UI: "Table Alignment" Shortcut:** Added a new "Table Alignment" option to the main
  menu for more intuitive access to the Quick Align feature. [cite: 2262]

#### Changed

- **Feature: Quick Align Overhaul:** The entire Quick Align feature was redesigned. The previous
  four-point tap system and its underlying homography calculations (`QuickAlignAnalyzer.kt`) have
  been completely removed and replaced with a more robust, manual system where the user aligns a
  virtual table overlay using pan, zoom, and rotate gestures.
- **UI: Menu & Dialogs Reorganization:** The "Too Advanced Options" dialog was restructured for
  better usability, grouping related functions into sections like "Fix Lens Warp." The main menu
  layout was significantly reordered to improve logical flow and group related items.
- **UI: Menu Toggle Behavior:** The cooldown logic for multi-state menu options (Orientation, Mode)
  was replaced with a more intuitive debounce system. The UI now cycles instantly with each tap, and
  the selected option is only finalized after a one-second delay of inactivity or when the menu is
  closed.
- **CV: Robustness Enhancements:** The computer vision pipeline was made more resilient. A
  morphological closing operation was added to the masking process to mitigate errors from specular
  highlights, and the contour refinement algorithm was updated to a "minimum enclosing circle"
  method to better handle complex ball patterns.
- **UI: Button Aesthetics:** The stroke width and font size of the primary action buttons (
  `CuedetatButton`) were increased for better visibility and legibility.
- **Theming: Luminance Scope:** The luminance adjustment feature was extended to affect the base
  theme colors, allowing for broader visual tuning of the application's chrome.

#### Fixed

- **Critical Crash: State Deserialization:** Resolved a recurring `NullPointerException` that
  occurred when loading a saved state from a previous app version. The fix involved sanitizing the
  state data in the `UserPreferencesRepository` immediately after deserialization to ensure new,
  non-nullable properties are given a default value.
- **Build: Dependency Injection & Compilation:** Corrected a series of Hilt dependency injection and
  Compose compilation errors that prevented the application from building.
- **Rendering: Disappearing Table:** Fixed a critical bug where the table outline and rails would
  disappear and the diamond grid would be misaligned. The issue was traced to a faulty bitmap
  caching mechanism in the `OverlayRenderer`, which has been removed in favor of dynamic rendering.
- **Rendering: Tangent Line Mask:** Eliminated a visual artifact where a "rogue mask" would
  incorrectly apply to the tangent line. The issue was caused by a stateful `Paint` object whose
  shader was not being reset after use.
- **Logic: Illegitimate Warnings:** Corrected a logic flaw that allowed stale warning flags from
  Protractor Mode to incorrectly trigger warnings while in Banking Mode.

### [UNRELEASED] - 2025-07-22

#### Added

- [cite_start]**Feature: User Experience Modes:** Initial planning for three distinct user
  experience modes (Expert, Beginner, Hater) to be selected on first launch. [cite: 1649]
- [cite_start]**Expert Mode:** The current, full-featured application state. [cite: 1649]
  - [cite_start]**Beginner Mode:** A simplified experience with helper labels enabled by
    default. [cite: 1650] [cite_start]Feature set to be streamlined for
    approachability. [cite: 1650]
  - [cite_start]**Hater Mode:** A "Magic 8-Ball" mode that provides cynical, non-committal answers
    in response to a device shake. [cite: 1651] [cite_start]Specification is complete. [cite: 1651]

#### Fixed

- [cite_start]**Performance: Inefficient Glow:** Corrected a significant performance issue where
  fading lines were rendered via a CPU-intensive loop of discrete
  segments. [cite: 1652] [cite_start]The renderer now uses a single `LinearGradient` mask for a more
  performant, GPU-accelerated effect. [cite: 1652]
- [cite_start]**Rendering Bug: Warning Glow Color:** Fixed a visual bug where the glow effect for
  the `GhostCueBall` and aiming lines would remain white during a warning
  state. [cite: 1653] [cite_start]The glow now correctly adopts the mandated `WarningRed`
  color. [cite: 1654]

### [UNRELEASED] - 2025-07-21

#### Added

- **Feature: Quick Align:** Added a new "Quick Align" feature, allowing users to align the
  [cite_start]virtual table by tapping four known points in a single photo. [cite: 1655] This
  replaces the complex and
  [cite_start]unreliable full camera calibration feature. [cite: 1656]
- [cite_start]**Feature: Automatic World Lock:** Implemented an automatic "World Lock"
  feature. [cite: 1657] The virtual
  scene now automatically locks its position relative to the camera view when a user successfully
  [cite_start]snaps a virtual ball to a real one. [cite: 1658] The world unlocks automatically when
  a ball is dragged or the
  [cite_start]view is reset. [cite: 1659]

#### Changed

- **UI: Button Redesign:** The main action buttons are now circular, have a thicker 2dp outline,
  [cite_start]larger text, and unique colors drawn from the new application theme. [cite: 1660]
- **Theming: New Palette:** Replaced the entire application color palette with a new, systematic
  [cite_start]one. [cite: 1661] All UI components, including themes, buttons, and canvas renderings,
  have been updated to use
  [cite_start]the new color scheme. [cite: 1661] [cite_start]The primary yellow color was
  specifically muted as requested. [cite: 1662]
- **Perspective System:** Refined the perspective tilt logic with a cubic ease-out curve to create
  [cite_start]a more pronounced, non-linear slowing effect as the user approaches the physical tilt
  limit. [cite: 1663] The
  [cite_start]maximum virtual tilt was also adjusted to 87 degrees. [cite: 1664]
- **UI: Button Layout:** Removed the "Lock World" button and moved the "Spin" button to the
  [cite_start]bottom-right column for a more balanced layout. [cite: 1665] The "Add Ball" button's
  color was changed to a
  [cite_start]neutral blue to reserve red for warnings. [cite: 1666]

#### Fixed

- **Critical Bug: Banking Mode Aiming:** Resolved a major bug where aiming lines in Banking Mode
  [cite_start]were static and did not update during a drag gesture. [cite: 1667] The issue was
  caused by a conflicting,
  [cite_start]redundant gesture handler that has now been removed. [cite: 1668]
- **CV: Mask Orientation:** Fixed a bug where the CV debug mask was being displayed with an
  [cite_start]incorrect, squished orientation. [cite: 1669] [cite_start]The mask is now correctly
  rotated to match the device's display. [cite: 1670]
- [cite_start]**CV: Bounding Box Display:** Re-enabled the drawing of CV bounding
  boxes. [cite: 1671] The renderer now
  [cite_start]receives the necessary source image dimensions to correctly transform and display the
  boxes. [cite: 1672]
- **Build: Compilation Errors:** Corrected a series of compilation errors related to outdated color
  [cite_start]references in UI configuration files after the theme update. [cite: 1673]
---

## II. The Issue Tracker

[cite_start]A list of known bugs and required features. [cite: 1726]
### Open Issues

- **Interactive Tutorial:** The interactive placeholder needs to be expanded into a full, guided
  [cite_start]experience. [cite: 1735]
- **UI/UX: Beginner Mode Menu:**
  - [cite_start]The "Toggle Labels" option should be the first item, followed by "Show
    Tutorial." [cite: 1731]
  - [cite_start]The vertical spacing between all menu items needs to be increased for better
    readability. [cite: 1732]

### Closed Issues

- **[CLOSED] Critical Bug: CV Crash:** Entering "Test Mask" mode when "Show CV Mask" is disabled
  causes a
  [cite_start]fatal crash. [cite: 1727]
- **[CLOSED] UI/UX: Menu Redesign:** The menu drawer width has been reduced, a fixed footer has been
  implemented, and the main content now scrolls independently.
- **[CLOSED] UI/UX: Expert Mode Menu:** The "Turn Camera Off/On" option is now the first item in the
  menu.
- **[CLOSED] CV Robustness:** The CV pipeline is now more resilient to specular highlights and
  complex ball patterns.
- **[CLOSED] Performance: Inefficient Glow:** The glow effect was rendered with a series of circles instead of
  [cite_start]a stroked path, causing visual artifacts and unnecessary performance
  overhead. [cite: 1736] [cite_start]This has been corrected. [cite: 1736]
- **[CLOSED] Rendering Bug: Warning Glow Color:** The glow effect for warnings was incorrectly white instead
  [cite_start]of the mandated `WarningRed`. [cite: 1737] [cite_start]This has been
  corrected. [cite: 1737]
- **[CLOSED] Performance: 3D Lag:** The application suffered from significant lag during device
  [cite_start]tilt. [cite: 1738] [cite_start]The issue was traced to a flawed rendering cache that
  was being invalidated on every frame. [cite: 1739] The renderer has been refactored to cache only
  static, un-transformed geometry, resolving the
  [cite_start]bottleneck. [cite: 1740]
- [cite_start]**[CLOSED] Feature Incomplete: Quick Align:** The "Quick Align" feature was a UI
  placeholder. [cite: 1741] The
  [cite_start]homography calculation and state application logic have been fully
  implemented. [cite: 1742]
- **[CLOSED] Rendering Bug: Missing Tilt:** A logical error in the perspective pipeline (`translate`
  [cite_start]before `rotate`) was preventing the 3D tilt effect from being applied to the table
  rails. [cite: 1743] The
  [cite_start]transformation order has been corrected. [cite: 1743]
- **[CLOSED] UI Regression: Rail Alignment / Ball Resizing:** A visual bug caused table rails to
  [cite_start]misalign and balls to resize during rotation. [cite: 1744] This was traced to an
  incorrect matrix
  [cite_start]transformation order and was fixed by separating the rendering matrices for position
  and size. [cite: 1745]
- [cite_start]**[CLOSED] Interactive Tutorial:** The tutorial was a blocking overlay. [cite: 1746]
  It has been replaced with
  [cite_start]a non-blocking placeholder that allows user interaction. [cite: 1747]
- **[CLOSED] Undiscoverable UI:** The double-tap-to-drag gesture for moving the `SpinControl` had no
  [cite_start]visual feedback. [cite: 1748] A scaling effect, halo, and "move" icon have been added
  to indicate the move mode
  [cite_start]is active. [cite: 1749]
- **[CLOSED] Hardcoded CV Parameters:** The sliders in the advanced options dialog for Canny
  [cite_start]thresholds were not connected to the CV logic. [cite: 1750] The parameters are now
  passed from the UI state to
  [cite_start]the OpenCV functions. [cite: 1751]
- **[CLOSED] CV Pipeline Incomplete:** The Hough Circle Transform refinement method and the "Dynamic
  [cite_start]Rangefinder" were unimplemented. [cite: 1752] Both features have now been added to the
  [cite_start]`VisionRepository`. [cite: 1753]
- **[CLOSED] Improve Ball Detection Accuracy:** The previous model struggled with perspective and
  [cite_start]lighting. [cite: 1754] The new dynamic radius calculation and statistical color
  sampling have dramatically
  [cite_start]improved accuracy and robustness. [cite: 1755]
- **[CLOSED] Add CV Tuning UI:** The user had no way to see or correct what the CV system was
  [cite_start]seeing. [cite: 1756] [cite_start]The new calibration and mask testing UI resolves
  this. [cite: 1756]
- **[CLOSED] Fix Table Rendering:** The table rails and pockets were misaligned due to an incorrect
  [cite_start]3D transformation order. [cite: 1757] This has been corrected by unifying all
  rendering under a single
  [cite_start]perspective matrix with the proper rotation sequence. [cite: 1758] The missing diamond
  grid has also been
  [cite_start]restored. [cite: 1759]
- [cite_start]**[CLOSED] Overhaul Menu UI:** The menu animation was standard. [cite: 1759] It is now
  a custom top-down reveal
  [cite_start]with a horizontal sweep dismissal, and the color scheme has been unified. [cite: 1760]
- [cite_start]**[CLOSED] Update Splash Screen:** The splash screen was generic. [cite: 1761] It has
  been updated with a
  [cite_start]larger logo and the app name has been removed. [cite: 1762]
- **[CLOSED] Button Aesthetics:** The primary action buttons (`Magic8BallButton`) have been
  redesigned with a transparent background, an inverted triangle, a periwinkle glow, and smaller
  [cite_start]text. [cite: 1763]
- **[CLOSED] Codebase Cleanup:** The codebase suffered from scattered constants and broken
  [cite_start]references after refactoring. [cite: 1764] This has been addressed by creating a
  central `Constants.kt` file
  [cite_start]and fixing all related compilation errors. [cite: 1765]