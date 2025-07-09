# 13: Icon Specifications

This document outlines the specific icons used in the application, their purpose, and their required behavior.

## Reset FAB Icon (`ic_undo_24`)

* **Description:** A circular arrow.
* **Component:** Used on the **Reset FAB**.
* **Function:** Triggers the `MainScreenEvent.Reset` event to return the application state to its default values.

## Toggle Cue Ball FAB Icon (`ic_jump_shot`)

* **Description:** A circle with an upward-pointing chevron.
* **Component:** Used on the **Toggle Cue Ball FAB**.
* **Function:** Triggers the `MainScreenEvent.ToggleActualCueBall` event, which toggles the visibility of the `ActualCueBall` model on the logical plane.

## Help Menu Icon (`ic_help_outline_24`)

* **Description:** A question mark within a circle.
* **Component:** Used in the menu for the help text toggle.
* **Function:** Triggers the `MainScreenEvent.ToggleHelp` event, which toggles the visibility of all descriptive text labels in the UI.

## Zoom Slider Icon (`ic_zoom_in_24`)

* **Description:** A magnifying glass.
* **Component:** Displayed above the vertical zoom slider.
* **Function:** This icon is purely decorative and serves as a visual anchor for the zoom control area. It has no associated event.

## Theme Icons (`ic_light_mode_24`, `ic_dark_mode_24`)

* **Description:** Abstract representations of a sun and moon.
* **Component:** Used in the menu for the theme toggle item.
* **Function:** The displayed icon is conditional, based on the current theme state. Tapping it triggers the `MainScreenEvent.ToggleForceTheme` event.

## Application Logo (`logo_cue_detat.webp`)

* **Description:** The primary application logo.
* **Component:** Serves as the application's launcher icon and as the menu entry point when helper text is disabled.
* **Function:** When tapped within the top controls area of the app, it opens the `ModalNavigationDrawer`.