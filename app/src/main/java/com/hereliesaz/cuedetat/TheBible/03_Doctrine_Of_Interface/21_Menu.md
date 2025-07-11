# The Menu

* [cite_start]**Access:** The menu is a modal navigation drawer opened from the top-left of the screen. [cite: 775]
* [cite_start]**Background:** The menu background must be set to 80% opacity. [cite: 775]
* **Controls:** All items in the menu must be text-only `TextButton`s. [cite_start]No `Switch` components are permitted. [cite: 775]
* [cite_start]**Contextual Labels:** All toggles must have contextual text that reflects the action to be taken (e.g., "Hide Table" if the table is visible, "Show Table" if it is hidden). [cite: 775]
* **Organization:** The menu must be organized into logical sections separated by dividers. Headings are forbidden. The mandated order is:
    1.  **Core Controls:** Primary actions like Reset, Mode Toggling, and major visibility toggles.
    2.  **Settings & Appearance:** Secondary options for configuring the table, units, and visual theme.
    3.  **Developer:** Advanced options for tuning experimental features.
    4.  **Help & Info:** All user assistance and meta-application links.

***
## Addendum: Menu Item Specifications

The menu must contain the following items, triggering the specified events.
* **Section 1: Core Controls**
    * `"Reset View"`: Resets all state to default. [cite_start]Event: `Reset`. [cite: 777]
    * `"Calculate Bank"` / `"Ghost Ball Aiming"`: Toggles between Banking and Protractor modes. [cite_start]Event: `ToggleBankingMode`. [cite: 777]
    * `"Toggle Cue Ball"` / `"Hide Cue Ball"`: Toggles the `ActualCueBall` in Protractor mode. [cite_start]Event: `ToggleOnPlaneBall`. [cite: 777]
    * `"Toggle Table"` / `"Hide Table"`: Toggles the visibility of the table wireframe. This is only visible in Protractor Mode. [cite_start]Event: `ToggleTable`. [cite: 778]

* **Section 2: Settings & Appearance**
    * `"Table Size"`: Opens the table size selection dialog. [cite_start]Event: `ToggleTableSizeDialog`. [cite: 778]
    * `"Use Imperial Units"` / `"Use Metric Units"`: Toggles the distance unit display. [cite_start]Event: `ToggleDistanceUnit`. [cite: 778]
    * `"Turn Camera Off"` / `"Turn Camera On"`: Toggles the visibility of the live camera feed background. [cite_start]Event: `ToggleCamera`. [cite: 778]
    * `"Embrace the Darkness"` / `"Walk toward the Light"` / `"Use System Theme"`: Toggles between dark, light, and system themes. [cite_start]Event: `ToggleForceTheme`. [cite: 778]
    * `"Luminance"`: Opens the luminance adjustment dialog. [cite_start]Event: `ToggleLuminanceDialog`. [cite: 778]
    * `"Glow Stick"`: Opens the glow adjustment dialog. [cite_start]Event: `ToggleGlowStickDialog`. [cite: 778]

* **Section 3: Developer**
    * `"Too Advanced Options"`: Opens the CV parameter tuning dialog. Event: `ToggleAdvancedOptionsDialog`.
    * `"Use Generic AI"` / `"Use Custom AI"`: Toggles the active ML model. Event: `ToggleCvModel`.

* **Section 4: Help & Info**
    * `"WTF is all this?"` / `"OK, I get it."`: Toggles help text visibility. [cite_start]Event: `ToggleHelp`. [cite: 777]
    * `"Show Tutorial"`: Starts the interactive tutorial. [cite_start]Event: `StartTutorial`. [cite: 777]
    * `"Check for Updates"`: Opens the project's GitHub releases page. [cite_start]Event: `CheckForUpdate`. [cite: 779]
    * `"About Me"`: Launches an intent to open the developer's Instagram profile (`https://www.instagram.com/hereliesaz/`). [cite_start]Event: `ViewArt`. [cite: 779]
    * `"Chalk Your Tip"`: Opens the donation dialog. [cite_start]Event: `ShowDonationOptions`. [cite: 779]