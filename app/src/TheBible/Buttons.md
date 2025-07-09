# 12: Buttons

* **Shape:** All buttons, including `FloatingActionButton` and standard `Button`s in dialogs and menus, must be circular (`CircleShape`).
* **Persistent Cue Ball Toggle:** A circular `FloatingActionButton` must be present in the lower-left of the screen to toggle the visibility of the Actual Cue Ball. Its default state is to show an icon, not text.

***
## Addendum: Detailed FAB Specifications

Two `FloatingActionButton`s (FABs) must be persistently displayed at the bottom of the screen, outside of the menu.

### 1. Reset FAB

* **Placement**: Aligned to the bottom-right of the screen (`Alignment.BottomEnd`).
* **Action**: Triggers the `MainScreenEvent.Reset` event.
* **Dynamic Appearance**: The button's color must provide visual feedback on its relevance.
    * When `uiState.valuesChangedSinceReset` is `false`, its `containerColor` must be `MaterialTheme.colorScheme.surfaceVariant` (a neutral, inactive state).
    * When `uiState.valuesChangedSinceReset` is `true`, its `containerColor` must change to `MaterialTheme.colorScheme.secondaryContainer` to indicate that there are changes to reset.
* **Content (Icon vs. Text)**:
    * By default (`areHelpersVisible = false`), it displays the `ic_undo_24` icon.
    * When help text is enabled (`areHelpersVisible = true`), it displays the text "Reset\nView".

### 2. Toggle Cue Ball FAB

* **Placement**: Aligned to the bottom-left of the screen (`Alignment.BottomStart`).
* **Action**: Triggers the `MainScreenEvent.ToggleCueBall` event. This button is only active and visible when not in Banking Mode.
* **Dynamic Appearance**: The button's color must provide visual feedback on the toggle's state.
    * When `uiState.screenState.showActualCueBall` is `false`, its `containerColor` must be `MaterialTheme.colorScheme.surfaceVariant` (inactive state).
    * When `uiState.screenState.showActualCueBall` is `true`, its `containerColor` must change to `MaterialTheme.colorScheme.secondaryContainer` (active state).
* **Content (Icon vs. Text)**:
    * By default (`areHelpersVisible = false`), it displays the `ic_jump_shot` icon.
    * When help text is enabled (`areHelpersVisible = true`), it displays the text "Cue Ball\nToggle".