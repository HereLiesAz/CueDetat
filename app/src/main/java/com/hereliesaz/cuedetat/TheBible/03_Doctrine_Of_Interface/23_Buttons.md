# Buttons

* **Shape:** All buttons, including `FloatingActionButton` and standard `Button`s in dialogs and menus, must be circular (`CircleShape`).
* **Persistent Cue Ball Toggle:** A circular `FloatingActionButton` must be present in the lower-left of the screen to toggle the visibility of the Actual Cue Ball. Its default state is to show an icon, not text.

***
## Addendum: Detailed FAB Specifications

Two to three `FloatingActionButton`s (FABs) must be persistently displayed at the bottom of the screen, outside of the menu.

### 1. Reset FAB

* **Placement**: Aligned to the bottom-right of the screen (`Alignment.BottomEnd`).
* **Action**: Triggers the `MainScreenEvent.Reset` event. This now functions as a two-state toggle.
  * **First Press:** Saves the current positions and rotations of all elements and resets them to their default state. If the table is visible, it resets to the table-centric default. If not, it resets to the screen-centric default. UI visibility toggles (like `showTable`) are preserved.
  * **Second Press:** Reverts all positions and rotations to their saved state from before the first press.
* **Dynamic Appearance**: The button's color must provide visual feedback on its relevance.
  * When `uiState.valuesChangedSinceReset` is `false`, its `containerColor` must be `MaterialTheme.colorScheme.surfaceVariant` (a neutral, inactive state).
  * When `uiState.valuesChangedSinceReset` is `true`, its `containerColor` must change to `MaterialTheme.colorScheme.secondaryContainer` to indicate that there are changes to reset.
* **Content (Icon vs. Text)**:
  * By default (`areHelpersVisible = false`), it displays the `ic_undo_24` icon.
  * When help text is enabled (`areHelpersVisible = true`), it displays the text "Reset\nView".

### 2. Toggle Cue Ball FAB

* **Placement**: Aligned to the bottom-left of the screen (`Alignment.BottomStart`).
* **Action**: Triggers the `MainScreenEvent.ToggleOnPlaneBall` event. This button is only active and visible when not in Banking Mode.
* **Dynamic Appearance**: The button's color must provide visual feedback on the toggle's state.
  * When `uiState.onPlaneBall` is `null`, its `containerColor` must be `MaterialTheme.colorScheme.surfaceVariant` (inactive state).
  * When `uiState.onPlaneBall` is not `null`, its `containerColor` must change to `MaterialTheme.colorScheme.secondaryContainer` (active state).
* **Content (Icon vs. Text)**:
  * By default (`areHelpersVisible = false`), it displays the `ic_jump_shot` icon.
  * When help text is enabled (`areHelpersVisible = true`), it displays the text "Cue Ball\nToggle".

### 3. Toggle Table FAB

* **Placement**: Positioned horizontally between the `ToggleCueBallFab` and the `ResetFab`.
* **Action**: Triggers the `MainScreenEvent.ToggleTable` event.
* **Visibility**: This button is **only** visible when in Protractor Mode and when the table is not currently shown (`!uiState.isBankingMode && !uiState.showTable`).
* **Content**: It displays the `pool_table` icon.