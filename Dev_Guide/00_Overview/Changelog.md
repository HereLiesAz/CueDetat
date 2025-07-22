# Changelog & Issues

This document is the definitive record of completed work and open issues. It is the final word on
the state of the project, superseding all other records.

---

## I. Changelog

A log of completed work.

### [UNRELEASED] - 2025-07-22

#### Added

- **Feature: User Experience Modes:** Initial planning for three distinct user experience modes (Expert, Beginner, Hater) to be selected on first launch.
    - **Expert Mode:** The current, full-featured application state.
    - **Beginner Mode:** A simplified experience with helper labels enabled by default. Feature set to be streamlined for approachability.
    - **Hater Mode:** A "Magic 8-Ball" mode that provides cynical, non-committal answers in response to a device shake. Specification is complete.

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
  dated `Magic8BallButton`.

#### Changed

- **Perspective System:** Completely overhauled the perspective tilt logic based on new ergonomic
  requirements. The new system provides a non-linear mapping with an ease-out function,
  creating a smooth, intuitive transition as the user's physical tilt approaches its effective
  limit.
- **UI:** Redesigned the main menu with a `VoidBlack` background and removed the
  non-functional theme-toggling option to streamline the user experience.

#### Fixed

- **Critical Regression: Rendering:** Corrected a severe visual bug where rendered balls would
  inappropriately resize during world rotation. The issue was traced to an incorrect matrix
  transformation order. The fix involved separating the position-calculation matrix (
  with rotation) from a new, rotation-agnostic size-calculation matrix.
- **State Management:** Resolved a bug where the orientation lock menu option was
  unresponsive. The root cause was an event-routing failure in the main `StateReducer`,
  which was corrected to properly delegate the event.
- **Calculation:** Fixed a major error in the distance calculation by correcting the
  `distanceReferenceConstant` in the `UpdateStateUseCase`, bringing the displayed distance in line
  with physical reality.

### [UNRELEASED] - 2025-07-20 (Session 1)

#### Added

- **CV Dynamic Rangefinder:** Implemented the "Dynamic Rangefinder" for the CV pipeline. The system
  now calculates the expected on-screen size of a ball based on perspective, dramatically improving
  detection accuracy and reducing false positives from reflections or table
  patterns.

#### Changed

- **UI Rule:** Enforced a new UI rule: when the pool table is visible, the cue ball is mandatory and
  the option to hide it is removed from the UI.
- **Menu Streamlining:** Streamlined the main menu by removing the "Glow Stick," "Check for
  Updates," and non-functional "Send Feedback" options.
- **Tutorial:** Replaced the blocking tutorial overlay with a functional, non-blocking placeholder.
  The UI is now accessible while the tutorial text is displayed at the bottom of the
  screen.

#### Fixed

- **Performance:** Optimized the camera image to OpenCV Mat conversion process, significantly
  reducing memory allocation and improving CV pipeline performance.
- **Rendering:** Spin path colors now correctly match the colors on the spin control
  wheel.
- **UI:** The "About" menu item now correctly links to the main GitHub repository page instead of
  the releases page.
- **Theming:** Standardized all warning-related UI elements to use the official `WarningRed` color
  from the theme, ensuring visual consistency for elements like obstacle balls and the spin control
  wheel.
- **CV Functionality:** Implemented the Hough Circle Transform as an alternative CV refinement
  method, making the "Refinement Method" toggle fully functional.
- **CV Functionality:** The advanced options sliders for Canny edge detection thresholds are now
  functional and correctly applied in the CV pipeline.
- **UI Discoverability:** Added visual feedback (scaling, halo, and a "move" icon) to the spin
  control's double-tap-and-drag gesture to make it discoverable.

### [UNRELEASED] - 2025-07-19

#### Added

- **State Persistence:** Implemented a full state persistence layer using Jetpack DataStore and
  Gson. The application now saves and restores the user's complete setup (ball positions, selected
  modes, options) between sessions.
- **Orientation Lock:** Added a user preference to lock the screen orientation to Portrait,
  Landscape, or keep it Automatic. This setting is persisted across
  sessions.
- **Distance Display:** Added a real-time display in the top-right corner showing the calculated
  distance from the camera to the target ball.
- **Centralized Label Configuration:** Created a `LabelConfig.kt` file to centralize all styling and
  properties for UI text labels, allowing for granular control over each
  one.

#### Changed

- **Performance Optimization:** Refactored the `UpdateStateUseCase` and `MainViewModel` to perform
  granular updates. High-frequency sensor and vision data flows are now handled independently to
  prevent unnecessary, expensive matrix recalculations on every camera
  frame.
- **Gesture Responsiveness:** Modified the `GestureReducer` to calculate the protractor's rotation
  based on the change in angle between drag events, providing a smoother, more intuitive feel that "
  picks up where it left off."
- **UI Refinements:**
  - Replaced `Magic8BallButton`s for zoom controls with standard `IconButton`s.
  - Reduced the visual thickness of all `Slider` components.
  - Moved the vertical zoom slider closer to the edge of the screen.
- **State Persistence:** Replaced the unreliable `onPause` save trigger with a debounced, reactive
  mechanism that saves state automatically and robustly after changes.
- **Gesture Handling:** The touch target for dragging logical balls is now a constant screen-space
  size, making it consistent regardless of the zoom level.
- **UI: Protractor Guides:** The angle guide lines have been updated to show 10-degree increments
  from 10 to 80 degrees.
- **UI: Menu Behavior:** The Orientation Lock menu item no longer closes the menu on click, allowing
  the user to cycle through options.
- **Rendering:** Label rendering logic was refactored to apply configuration offsets in
  screen-space, preventing them from being distorted by perspective and
  zoom.

#### Fixed

- **Performance Regression:** Corrected a severe performance issue where the UI would lag in
  response to device tilt. The issue was caused by the sensor data flow incorrectly triggering a
  full state recalculation on every new camera frame.
- **Distance Calculation:** Corrected the distance calculation logic to use the final,
  perspective-correct `pitchMatrix` instead of the `flatMatrix`, ensuring the value updates
  dynamically with zoom, tilt, and rotation.
- **Pocket Opacity:** Adjusted the fill opacity of rendered pockets to 75%.
- **Build Process:** Corrected a critical flaw by removing a hardcoded, absolute keystore path from
  the debug build configuration, making the project portable.
- **State Restoration:** Resolved a cascade of fatal `NullPointerException`s that occurred when
  restoring the app from a saved state. The issue was caused by Gson's handling of `@Transient`
  properties;
  all affected state properties were made nullable and their consumers were updated to
  handle them safely.
- **Feature Regression: Orientation Lock:** Fixed a race condition between the state-saving
  mechanism and Activity recreation that was causing the orientation lock to fail. An immediate save
  is now triggered for this specific event.
- **UI Regression: Obstruction Visualization:** Corrected a rendering bug where the shot guide line
  would obscure its translucent pathway. The issue stemmed from a flawed, secondary drawing function
  that was being used in specific scenarios;
  the rendering logic has been unified and the line's
  stroke width has been corrected in its configuration.
- **Gesture Logic:** Corrected the gesture detection logic in `GestureReducer` to ensure dragging
  the `GhostCueBall` correctly moves the entire protractor unit instead of causing
  rotation.

---

## II. The Issue Tracker

A list of known bugs and required features.

### Open Issues

- **Critical Bug: CV Crash:** Entering "Test Mask" mode when "Show CV Mask" is disabled causes a
  fatal crash.
- **Performance: Inefficient Glow:** The glow effect is rendered with a series of circles instead of
  a stroked path, causing visual artifacts and unnecessary performance overhead.
- **Rendering Bug: Warning Glow Color:** The glow effect for warnings is incorrectly white instead
  of the mandated `WarningRed`.
- **UI/UX: Menu Redesign:**
    - The menu drawer width needs to be reduced.
    - Implement a fixed footer containing the "About" and "@hereliezaz" links, as well as a new
      toggle for the user experience modes (Expert/Beginner/Hater). The main content of the menu
      should scroll independently above this footer.
- **UI/UX: Beginner Mode Menu:**
    - The "Toggle Labels" option should be the first item, followed by "Show Tutorial."
    - The vertical spacing between all menu items needs to be increased for better readability.
- **UI/UX: Expert Mode Menu:**
    - The "Turn Camera Off/On" option should be the first item in the menu.
- **CV Robustness:** The current CV pipeline remains vulnerable to specular highlights and complex
  ball patterns (stripes, numbers).
- **Interactive Tutorial:** The interactive placeholder needs to be expanded into a full, guided
  experience.

### Closed Issues

- **[CLOSED] Performance: 3D Lag:** The application suffered from significant lag during device
  tilt. The issue was traced to a flawed rendering cache that was being invalidated on every frame.
  The renderer has been refactored to cache only static, un-transformed geometry, resolving the
  bottleneck.
- **[CLOSED] Feature Incomplete: Quick Align:** The "Quick Align" feature was a UI placeholder. The
  homography calculation and state application logic have been fully implemented.
- **[CLOSED] Rendering Bug: Missing Tilt:** A logical error in the perspective pipeline (`translate`
  before `rotate`) was preventing the 3D tilt effect from being applied to the table rails. The
  transformation order has been corrected.
- **[CLOSED] UI Regression: Rail Alignment / Ball Resizing:** A visual bug caused table rails to
  misalign and balls to resize during rotation. This was traced to an incorrect matrix
  transformation order and was fixed by separating the rendering matrices for position and size.
- **[CLOSED] Interactive Tutorial:** The tutorial was a blocking overlay. It has been replaced with
  a non-blocking placeholder that allows user interaction.
- **[CLOSED] Undiscoverable UI:** The double-tap-to-drag gesture for moving the `SpinControl` had no
  visual feedback. A scaling effect, halo, and "move" icon have been added to indicate the move mode
  is active.
- **[CLOSED] Hardcoded CV Parameters:** The sliders in the advanced options dialog for Canny
  thresholds were not connected to the CV logic. The parameters are now passed from the UI state to
  the OpenCV functions.
- **[CLOSED] CV Pipeline Incomplete:** The Hough Circle Transform refinement method and the "Dynamic
  Rangefinder" were unimplemented. Both features have now been added to the
  `VisionRepository`.
- **[CLOSED] Improve Ball Detection Accuracy:** The previous model struggled with perspective and
  lighting. The new dynamic radius calculation and statistical color sampling have dramatically
  improved accuracy and robustness.
- **[CLOSED] Add CV Tuning UI:** The user had no way to see or correct what the CV system was
  seeing. The new calibration and mask testing UI resolves this.
- **[CLOSED] Fix Table Rendering:** The table rails and pockets were misaligned due to an incorrect
  3D transformation order. This has been corrected by unifying all rendering under a single
  perspective matrix with the proper rotation sequence. The missing diamond grid has also been
  restored.
- **[CLOSED] Overhaul Menu UI:** The menu animation was standard. It is now a custom top-down reveal
  with a horizontal sweep dismissal, and the color scheme has been unified.
- **[CLOSED] Update Splash Screen:** The splash screen was generic. It has been updated with a
  larger logo and the app name has been removed.
- **[CLOSED] Button Aesthetics:** The primary action buttons (`Magic8BallButton`) have been
  redesigned with a transparent background, an inverted triangle, a periwinkle glow, and smaller
  text.
- **[CLOSED] Codebase Cleanup:** The codebase suffered from scattered constants and broken
  references after refactoring. This has been addressed by creating a central `Constants.kt` file
  and fixing all related compilation errors.
