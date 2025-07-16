# Buttons

* [cite_start]**Shape:** All buttons, including `FloatingActionButton` and standard `Button`s in dialogs and menus, must be circular (`CircleShape`). [cite: 783]
* **Appearance Mandate:** As a general rule, buttons should use text labels. The one holy exception is the **Top-Left Menu Button**, which **must** display the application icon when helper text is hidden (`areHelpersVisible = false`), and the application title text otherwise. All other FABs must display text at all times.
* **Color Mandate:** Buttons that can be used **must** be colored to indicate their active state. The default, "off" state shall use `primaryContainer`. A toggled "on" state shall use `secondaryContainer`. A disabled button shall appear faded.

***
## Addendum: Detailed FAB Specifications

* **1. Reset FAB**
  * [cite_start]**Placement**: Aligned to the bottom-right of the screen (`Alignment.BottomEnd`). [cite: 784]
  * [cite_start]**Action**: Triggers the `MainScreenEvent.Reset` event. [cite: 784] [cite_start]It functions as a two-state toggle, saving the current state on the first press and reverting to it on the second. [cite: 784]
  * [cite_start]**Dynamic Appearance**: Its `containerColor` must be `primaryContainer` by default, changing to `secondaryContainer` when `valuesChangedSinceReset` is `true`. [cite: 785]
  * [cite_start]**Content**: Displays the text "Reset\nView". [cite: 785]

* **2. Toggle Spin Control FAB**
  * [cite_start]**Placement**: Aligned to the bottom-left, stacked vertically. [cite: 786]
  * [cite_start]**Action**: Triggers the `ToggleSpinControl` event. [cite: 786]
  * [cite_start]**Dynamic Appearance**: Its `containerColor` is `primaryContainer` by default, changing to `secondaryContainer` when the spin control is visible. [cite: 786]
  * [cite_start]**Content**: Displays the text "Spin". [cite: 786]

* **3. Toggle Cue Ball FAB**
  * [cite_start]**Placement**: Aligned to the bottom-left of the screen (`Alignment.BottomStart`). [cite: 787]
  * [cite_start]**Action**: Triggers the `MainScreenEvent.ToggleOnPlaneBall` event. [cite: 787]
  * **Interactivity Mandate:** This button **must** be disabled when the table is visible, as the cue ball cannot be hidden when the table is shown. When disabled, it must appear faded. Its `onClick` action must be nullified.
  * [cite_start]**Dynamic Appearance**: Its `containerColor` is `primaryContainer` when off, changing to `secondaryContainer` when `onPlaneBall` is not `null`. [cite: 787]
  * [cite_start]**Content**: Displays the text "Cue Ball\nToggle". [cite: 787]

* **4. Toggle Table FAB**
  * **Placement**: Positioned at the bottom of the left-side FAB column.
  * **Action**: Triggers the `MainScreenEvent.ToggleTable` event. This is a two-way toggle.
  * **Visibility**: This button is **only** visible when not in Banking Mode.
  * **Dynamic Appearance**: Its `containerColor` is `primaryContainer` when the table is hidden, and `secondaryContainer` when the table is shown.
  * **Content**: It displays the text "Show\nTable" or "Hide\nTable" based on the current state.

* **5. Add Obstacle Ball FAB**
  * **Placement**: Aligned to the bottom-left, in the vertical FAB column.
  * **Action**: Triggers the `AddObstacleBall` event.
  * **Dynamic Appearance**: Its `containerColor` is always `primaryContainer`.
  * **Content**: Text that reads "Add\nBall".