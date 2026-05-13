# 3.6. Button Specifications

This document specifies the design and behavior of the primary action buttons (`CuedetatButton`).

## Appearance

* **Component**: `CuedetatButton.kt`.
* **Shape:** The button's interactive area is a 72dp circle.
* **Border:** A 2dp outlined border.
* **Text:** All button text must be small (`12.sp`).
* **Color:** Each button must be assigned a unique, semantic color from the application's theme.
  Red (`WarningRed`) is reserved for warnings and must not be used for standard action buttons.

## Specific Buttons & Behavior

* **1. Reset View / Unlock View Button**
  * **Placement**: Bottom-right column.
  * **Text (Conditional)**:
    * Displays "Unlock View" when in Beginner Mode's locked state (`isBeginnerViewLocked == true`).
    * Displays "Reset View" in all other modes and states.
  * **Action (Conditional)**:
    * Dispatches `UnlockBeginnerView` when in Beginner Mode's locked state.
    * Dispatches `Reset` in all other modes and states. Toggles between saving and reverting state.
      Also unlocks the world view.
* **2. Toggle Spin Control Button**
  * **Placement**: Bottom-right column.
  * **Action**: `ToggleSpinControl`.
  * **Visibility**: Hidden in Beginner Mode.
* **3. Add Obstacle Ball Button**
  * **Placement**: Bottom-left column.
  * **Action**: `AddObstacleBall`.
  * **Visibility**: Hidden in Beginner Mode.