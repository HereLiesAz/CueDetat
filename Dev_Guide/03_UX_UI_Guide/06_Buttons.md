# 3.6. Button Specifications

This document specifies the design and behavior of the primary action buttons (`Magic8BallButton`).

## Appearance

* **Shape:** The button's interactive area is a 56dp circle.
* **Background:** The button has no visible background color.
* **Motif:** A solid, upside-down triangle (`OracleBlue`) is drawn within the circular area.
* **Glow:** A soft, feathered `PeriwinkleGlow` is rendered around the triangle's stroke.
* **Text:** All button text must be small (`labelSmall`), bold, and colored `Color.White`.

## Specific Buttons & Behavior

* **1. Reset Button**
* **Placement**: Bottom-right.
* **Action**: `MainScreenEvent.Reset`. Toggles between saving and reverting state.
* **2. Toggle Spin Control Button**
* **Placement**: Bottom-left column.
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