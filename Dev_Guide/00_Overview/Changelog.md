# Changelog & Issues

This document is the definitive record of completed work and open issues. It is the final word on
the state of the project, superseding all other records.

---

## I. Changelog

A log of completed work.

### [UNRELEASED] - 2025-07-21

#### Added

- **Feature: Quick Align:** Added a new "Quick Align" feature, allowing users to align the
  virtual table by tapping four known points in a single photo. This replaces the complex and
  unreliable full camera calibration feature.
- **Feature: Automatic World Lock:** Implemented an automatic "World Lock" feature. The virtual
  scene now automatically locks its position relative to the camera view when a user successfully
  snaps a virtual ball to a real one. The world unlocks automatically when a ball is dragged or the
  view is reset.

#### Changed

- **UI: Button Redesign:** The main action buttons are now circular, have a thicker 2dp outline,
  larger text, and unique colors drawn from the new application theme.
- **Theming: New Palette:** Replaced the entire application color palette with a new, systematic
  one. All UI components, including themes, buttons, and canvas renderings, have been updated to use
  the new color scheme. The primary yellow color was specifically muted as requested.
- **Perspective System:** Refined the perspective tilt logic with a cubic ease-out curve to create
  a more pronounced, non-linear slowing effect as the user approaches the physical tilt limit. The
  maximum virtual tilt was also adjusted to 87 degrees.
- **UI: Button Layout:** Removed the "Lock World" button and moved the "Spin" button to the
  bottom-right column for a more balanced layout. The "Add Ball" button's color was changed to a
  neutral blue to reserve red for warnings.

#### Fixed

- **Critical Bug: Banking Mode Aiming:** Resolved a major bug where aiming lines in Banking Mode
  were static and did not update during a drag gesture. The issue was caused by a conflicting,
  redundant gesture handler that has now been removed.
- **CV: Mask Orientation:** Fixed a bug where the CV debug mask was being displayed with an
  incorrect, squished orientation. The mask is now correctly rotated to match the device's display.
- **CV: Bounding Box Display:** Re-enabled the drawing of CV bounding boxes. The renderer now
  receives the necessary source image dimensions to correctly transform and display the boxes.
- **Build: Compilation Errors:** Corrected a series of compilation errors related to outdated color
  references in UI configuration files after the theme update.

### [UNRELEASED] - 2025-07-20 (Session 2)

#### Added

- **UI Component:** Created a new minimalist `CuedetatButton` component to replace the
  [cite_start]dated `Magic8BallButton`. [cite: 1]

#### Changed

- **Perspective System:** Completely overhauled the perspective tilt logic based on new ergonomic
  requirements. [cite_start]The new system provides a non-linear mapping with an ease-out function,
  creating a smooth, intuitive transition as the user's physical tilt approaches its effective
  limit. [cite: 2, 3, 4]
- **UI:** Redesigned the main menu with a `VoidBlack` background and removed the
  [cite_start]non-functional theme-toggling option to streamline the user experience. [cite: 5]

#### Fixed

- **Critical Regression: Rendering:** Corrected a severe visual bug where rendered balls would
  inappropriately resize during world rotation. The issue was traced to an incorrect matrix
  transformation order. [cite_start]The fix involved separating the position-calculation matrix (
  with rotation) from a new, rotation-agnostic size-calculation matrix. [cite: 6]
- **State Management:** Resolved a bug where the orientation lock menu option was
  unresponsive. [cite_start]The root cause was an event-routing failure in the main `StateReducer`,
  which was corrected to properly delegate the event. [cite: 7]
- **Calculation:** Fixed a major error in the distance calculation by correcting the
  `distanceReferenceConstant` in the `UpdateStateUseCase`, bringing the displayed distance in line
  [cite_start]with physical reality. [cite: 8]

### [UNRELEASED] - 2025-07-20 (Session 1)

#### Added

- **CV Dynamic Rangefinder:** Implemented the "Dynamic Rangefinder" for the CV pipeline. The system
  now calculates the expected on-screen size of a ball based on perspective, dramatically improving
  [cite_start]detection accuracy and reducing false positives from reflections or table
  patterns. [cite: 502, 507]

#### Changed

- **UI Rule:** Enforced a new UI rule: when the pool table is visible, the cue ball is mandatory and
  [cite_start]the option to hide it is removed from the UI. [cite: 668, 669]
- **Menu Streamlining:** Streamlined the main menu by removing the "Glow Stick," "Check for
  Updates," and non-functional "Send Feedback" options.
- **Tutorial:** Replaced the blocking tutorial overlay with a functional, non-blocking placeholder.
  [cite_start]The UI is now accessible while the tutorial text is displayed at the bottom of the
  screen. [cite: 886]

#### Fixed

- **Performance:** Optimized the camera image to OpenCV Mat conversion process, significantly
  [cite_start]reducing memory allocation and improving CV pipeline performance. [cite: 525, 526]
- [cite_start]**Rendering:** Spin path colors now correctly match the colors on the spin control
  wheel. [cite: 559]
- **UI:** The "About" menu item now correctly links to the main GitHub repository page instead of
  [cite_start]the releases page. [cite: 854]
- **Theming:** Standardized all warning-related UI elements to use the official `WarningRed` color
  from the theme, ensuring visual consistency for elements like obstacle balls and the spin control
  [cite_start]wheel. [cite: 1094]
- **CV Functionality:** Implemented the Hough Circle Transform as an alternative CV refinement
  [cite_start]method, making the "Refinement Method" toggle fully functional. [cite: 505]
- **CV Functionality:** The advanced options sliders for Canny edge detection thresholds are now
  [cite_start]functional and correctly applied in the CV pipeline. [cite: 513]
- **UI Discoverability:** Added visual feedback (scaling, halo, and a "move" icon) to the spin
  [cite_start]control's double-tap-and-drag gesture to make it discoverable. [cite: 918]

### [UNRELEASED] - 2025-07-19

#### Added

- **State Persistence:** Implemented a full state persistence layer using Jetpack DataStore and
  Gson. The application now saves and restores the user's complete setup (ball positions, selected
  [cite_start]modes, options) between sessions. [cite: 460, 1038]
- **Orientation Lock:** Added a user preference to lock the screen orientation to Portrait,
  Landscape, or keep it Automatic. [cite_start]This setting is persisted across
  sessions. [cite: 663, 770]
- **Distance Display:** Added a real-time display in the top-right corner showing the calculated
  [cite_start]distance from the camera to the target ball. [cite: 721, 952]
- **Centralized Label Configuration:** Created a `LabelConfig.kt` file to centralize all styling and
  [cite_start]properties for UI text labels, allowing for granular control over each
  one. [cite: 1120]

#### Changed

- **Performance Optimization:** Refactored the `UpdateStateUseCase` and `MainViewModel` to perform
  granular updates. High-frequency sensor and vision data flows are now handled independently to
  [cite_start]prevent unnecessary, expensive matrix recalculations on every camera
  frame. [cite: 698, 1040]
- **Gesture Responsiveness:** Modified the `GestureReducer` to calculate the protractor's rotation
  based on the change in angle between drag events, providing a smoother, more intuitive feel that "
  [cite_start]picks up where it left off." [cite: 610, 611]
- **UI Refinements:**
  - Replaced `Magic8BallButton`s for zoom controls with standard `IconButton`s.
  - Reduced the visual thickness of all `Slider` components.
  - Moved the vertical zoom slider closer to the edge of the screen.
- **State Persistence:** Replaced the unreliable `onPause` save trigger with a debounced, reactive
  [cite_start]mechanism that saves state automatically and robustly after changes. [cite: 1042]
- **Gesture Handling:** The touch target for dragging logical balls is now a constant screen-space
  [cite_start]size, making it consistent regardless of the zoom level. [cite: 599, 600, 601]
- **UI: Protractor Guides:** The angle guide lines have been updated to show 10-degree increments
  [cite_start]from 10 to 80 degrees. [cite: 1228]
- **UI: Menu Behavior:** The Orientation Lock menu item no longer closes the menu on click, allowing
  [cite_start]the user to cycle through options. [cite: 1493]
- **Rendering:** Label rendering logic was refactored to apply configuration offsets in
  [cite_start]screen-space, preventing them from being distorted by perspective and
  zoom. [cite: 1505]

#### Fixed

- **Performance Regression:** Corrected a severe performance issue where the UI would lag in
  response to device tilt. The issue was caused by the sensor data flow incorrectly triggering a
  [cite_start]full state recalculation on every new camera frame. [cite: 1495]
- **Distance Calculation:** Corrected the distance calculation logic to use the final,
  perspective-correct `pitchMatrix` instead of the `flatMatrix`, ensuring the value updates
  [cite_start]dynamically with zoom, tilt, and rotation. [cite: 1497]
- [cite_start]**Pocket Opacity:** Adjusted the fill opacity of rendered pockets to 75%. [cite: 1110]
- **Build Process:** Corrected a critical flaw by removing a hardcoded, absolute keystore path from
  [cite_start]the debug build configuration, making the project portable. [cite: 383]
- **State Restoration:** Resolved a cascade of fatal `NullPointerException`s that occurred when
  restoring the app from a saved state. The issue was caused by Gson's handling of `@Transient`
  properties;
  all affected state properties were made nullable and their consumers were updated to
  [cite_start]handle them safely. [cite: 1501]
- **Feature Regression: Orientation Lock:** Fixed a race condition between the state-saving
  mechanism and Activity recreation that was causing the orientation lock to fail. An immediate save
  [cite_start]is now triggered for this specific event. [cite: 1504]
- **UI Regression: Obstruction Visualization:** Corrected a rendering bug where the shot guide line
  would obscure its translucent pathway. The issue stemmed from a flawed, secondary drawing function
  that was being used in specific scenarios;
  the rendering logic has been unified and the line's
  [cite_start]stroke width has been corrected in its configuration. [cite: 1506, 1507]
- **Gesture Logic:** Corrected the gesture detection logic in `GestureReducer` to ensure dragging
  [cite_start]the `GhostCueBall` correctly moves the entire protractor unit instead of causing
  rotation. [cite: 607]

---

## II. The Issue Tracker

A list of known bugs and required features.

### Open Issues

- **Performance:** Despite initial optimizations, the application still suffers from significant
  [cite_start]performance degradation and lag, particularly during 3D perspective
  transformations. [cite: 1511]
- **CV Robustness:** The current CV pipeline remains vulnerable to specular highlights and complex
  [cite_start]ball patterns (stripes, numbers). [cite: 1522]
- **Interactive Tutorial:** The interactive placeholder needs to be expanded into a full, guided
  [cite_start]experience. [cite: 1523]
- **Feature Incomplete: Quick Align:** The "Quick Align" feature needs its final calculation and
  state application logic implemented.

### Closed Issues

- **[CLOSED] UI Regression: Rail Alignment / Ball Resizing:** A visual bug caused table rails to
  misalign and balls to resize during rotation. This was traced to an incorrect matrix
  transformation order and was fixed by separating the rendering matrices for position and size.
- **[CLOSED] Interactive Tutorial:** The tutorial was a blocking overlay. It has been replaced with
  [cite_start]a non-blocking placeholder that allows user interaction. [cite: 1514]
- **[CLOSED] Undiscoverable UI:** The double-tap-to-drag gesture for moving the `SpinControl` had no
  visual feedback. A scaling effect, halo, and "move" icon have been added to indicate the move mode
  [cite_start]is active. [cite: 1516]
- **[CLOSED] Hardcoded CV Parameters:** The sliders in the advanced options dialog for Canny
  thresholds were not connected to the CV logic. The parameters are now passed from the UI state to
  [cite_start]the OpenCV functions. [cite: 1518]
- **[CLOSED] CV Pipeline Incomplete:** The Hough Circle Transform refinement method and the "Dynamic
  Rangefinder" were unimplemented. [cite_start]Both features have now been added to the
  `VisionRepository`. [cite: 1520]
- **[CLOSED] Rendering:** Corrected the 3D transformation order in the perspective pipeline, fixing
  a visual regression where table rails would misalign with the table surface during rotation and
  [cite_start]tilt. [cite: 1521]
- **[CLOSED] UI Regression: Obstruction Visualization:** The visual bug was traced to a flawed,
  secondary rendering function and an incorrect stroke width in the line's configuration. Both have
  been corrected, restoring the proper visual distinction between the shot line and its obstruction
  [cite_start]pathway. [cite: 1522]
- **[CLOSED] Feature Regression: Orientation Lock:** The feature was fixed by implementing an
  [cite_start]immediate state save to prevent a race condition during Activity
  recreation. [cite: 1524]
- **[CLOSED] Fragile State Persistence:** Replaced the unreliable `onPause` save trigger with a
  [cite_start]debounced, reactive mechanism that saves state automatically after
  changes. [cite: 1525]
- **[CLOSED] Incomplete State Restoration:** Hardened the state restoration process by making all
  [cite_start]complex `@Transient` properties nullable, preventing crashes from
  deserialization. [cite: 1526]
- **[CLOSED] Build-Breaking Keystore Path:** Removed the hardcoded absolute path to a debug
  [cite_start]keystore, making the project portable. [cite: 1527]
- **[CLOSED] State Preservation:** The application state was previously ephemeral. A full
  [cite_start]persistence layer has been implemented. [cite: 1528]
- **[CLOSED] Add Orientation Lock:** The feature has been implemented, but the UI toggle is
  [cite_start]currently broken. [cite: 1529]
- **[CLOSED] Implement CV Refinement:** The initial implementation only used a generic ML Kit model.
  [cite_start]The full "Scout/Sniper" pipeline with OpenCV refinement is now complete. [cite: 1530]
- **[CLOSED] Improve Ball Detection Accuracy:** The previous model struggled with perspective and
  lighting. The new dynamic radius calculation and statistical color sampling have dramatically
  [cite_start]improved accuracy and robustness. [cite: 1532]
- **[CLOSED] Add CV Tuning UI:** The user had no way to see or correct what the CV system was
  seeing. [cite_start]The new calibration and mask testing UI resolves this. [cite: 1534]
- **[CLOSED] Fix Table Rendering:** The table rails and pockets were misaligned due to an incorrect
  3D transformation order. This has been corrected by unifying all rendering under a single
  perspective matrix with the proper rotation sequence. The missing diamond grid has also been
  restored.
- **[CLOSED] Overhaul Menu UI:** The menu animation was standard. It is now a custom top-down reveal
  [cite_start]with a horizontal sweep dismissal, and the color scheme has been unified. [cite: 1537]
- **[CLOSED] Update Splash Screen:** The splash screen was generic. It has been updated with a
  [cite_start]larger logo and the app name has been removed. [cite: 1539]
- **[CLOSED] Button Aesthetics:** The primary action buttons (`Magic8BallButton`) have been
  redesigned with a transparent background, an inverted triangle, a periwinkle glow, and smaller
  [cite_start]text. [cite: 1541]
- **[CLOSED] Codebase Cleanup:** The codebase suffered from scattered constants and broken
  references after refactoring. This has been addressed by creating a central `Constants.kt` file
  and fixing all related compilation errors.