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

* **1. Reset Button**
    * **Placement**: Bottom-right column.
    * **Action**: `MainScreenEvent.Reset`. Toggles between saving and reverting state. Also unlocks
      the world view.
* **2. Toggle Spin Control Button**
    * **Placement**: Bottom-right column.
    * **Action**: `ToggleSpinControl`.
* **3. Toggle Cue Ball Button**
    * **Placement**: Bottom-left column.
    * **Action**: `ToggleOnPlaneBall`.
    * **Disabled State:** Must be disabled when the table is visible.
* **4. Toggle Table Button**
    * **Placement**: Bottom-left column.
    * **Action**: `ToggleTable`.
    * **Visibility**: Only visible when not in Banking Mode.
* **5. Add Obstacle Ball Button**
    * **Placement**: Bottom-left column.
    * **Action**: `AddObstacleBall`.