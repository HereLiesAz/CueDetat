# Buttons

* [cite_start]**Shape:** All buttons, including `FloatingActionButton` and standard `Button`s in dialogs and menus, must be circular (`CircleShape`). [cite: 783]
* **Appearance Mandate:** As a general rule, buttons should use text labels. The one holy exception is the **Top-Left Menu Button**, which **must** display the application icon when helper text is hidden (`areHelpersVisible = false`), and the application title text otherwise. All other FABs must display text at all times.

***
## Addendum: Detailed FAB Specifications

* **1. Reset FAB**
  * [cite_start]**Placement**: Aligned to the bottom-right of the screen (`Alignment.BottomEnd`). [cite: 784]
  * **Action**: Triggers the `MainScreenEvent.Reset` event. [cite_start]It functions as a two-state toggle, saving the current state on the first press and reverting to it on the second. [cite: 784]
  * [cite_start]**Dynamic Appearance**: Its `containerColor` must change from `surfaceVariant` to `secondaryContainer` when `valuesChangedSinceReset` is `true`. [cite: 785]
  * [cite_start]**Content**: Displays the text "Reset\nView". [cite: 785]

* **2. Toggle Spin Control FAB**
  * [cite_start]**Placement**: Aligned to the bottom-left, stacked vertically. [cite: 786]
  * [cite_start]**Action**: Triggers the `ToggleSpinControl` event. [cite: 786]
  * [cite_start]**Dynamic Appearance**: Its `containerColor` changes to `secondaryContainer` when the spin control is visible. [cite: 786]
  * [cite_start]**Content**: Displays the text "Spin". [cite: 786]

* **3. Toggle Cue Ball FAB**
  * [cite_start]**Placement**: Aligned to the bottom-left of the screen (`Alignment.BottomStart`). [cite: 787]
  * **Action**: Triggers the `MainScreenEvent.ToggleOnPlaneBall` event. [cite_start]This button is only active and visible when not in Banking Mode. [cite: 787]
  * [cite_start]**Dynamic Appearance**: Its `containerColor` changes to `secondaryContainer` when `onPlaneBall` is not `null`. [cite: 787]
  * [cite_start]**Content**: Displays the text "Cue Ball\nToggle". [cite: 787]

* **4. Toggle Table FAB**
  * [cite_start]**Placement**: Positioned horizontally between the left-side FAB column and the `ResetFab`. [cite: 788]
  * [cite_start]**Action**: Triggers the `MainScreenEvent.ToggleTable` event. [cite: 788]
  * [cite_start]**Visibility**: This button is **only** visible when in Protractor Mode and when the table is not currently shown. [cite: 788]
  * [cite_start]**Content**: It displays the `pool_table` icon. [cite: 788]

* **5. Felt Color FAB**
  * [cite_start]**Placement**: Aligned to the bottom-left, at the top of the vertical FAB column. [cite: 788]
  * [cite_start]**Action**: Triggers the `LockOrUnlockColor` event. [cite: 788]
  * [cite_start]**Functionality**: Toggles between automatic color detection and locking the currently sampled color. [cite: 788]
  * **Content**: Text that reads "Felt\nColor".