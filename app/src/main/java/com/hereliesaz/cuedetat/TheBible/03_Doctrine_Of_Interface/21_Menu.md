# The Menu

* [cite_start]**Access:** The menu is a modal navigation drawer opened from the top-left of the screen. [cite: 775]
* [cite_start]**Background:** The menu background must be set to 80% opacity. [cite: 775]
* **Controls:** All items in the menu must be text-only `TextButton`s. [cite_start]No `Switch` components are permitted. [cite: 775]
* [cite_start]**Contextual Labels:** All toggles must have contextual text that reflects the action to be taken (e.g., "Hide Table" if the table is visible, "Show Table" if it is hidden). [cite: 775]
* **Organization:** The menu must be organized into logical sections separated by dividers. Headings are forbidden. The mandated order is:
  1.  **Core Controls:** Primary mode and visibility toggles.
  2.  **Table & Units:** Settings for the table and measurement units.
  3.  **Appearance:** Visual theme and color settings.
  4.  **Help & Info:** Tutorial and user assistance.
  5.  **Meta:** External links and application information.
  6.  **Developer:** Advanced, internal options.

***
## Addendum: Menu Item Specifications

The menu must contain the following items, triggering the specified events.

* **Section 1: Core Controls**
  * `"Calculate Bank"` / `"Ghost Ball Aiming"`: Toggles between Banking and Protractor modes. [cite: 777] Event: `ToggleBankingMode`.
  * `"Toggle Cue Ball"` / `"Hide Cue Ball"`: Toggles the `ActualCueBall` in Protractor mode. Event: `ToggleOnPlaneBall`.
  * `"Turn Camera Off"` / `"Turn Camera On"`: Toggles the visibility of the live camera feed background. [cite: 778] Event: `ToggleCamera`.

* **Section 2: Table & Unit Settings**
  * `"Toggle Table"` / `"Hide Table"`: Toggles the visibility of the table wireframe. This is only visible in Protractor Mode. [cite: 778] Event: `ToggleTable`.
  * [cite_start]`"Table Size"`: Opens the table size selection dialog. [cite: 778] Event: `ToggleTableSizeDialog`.
  * [cite_start]`"Use Imperial Units"` / `"Use Metric Units"`: Toggles the distance unit display. [cite: 778] Event: `ToggleDistanceUnit`.

* **Section 3: Appearance**
  * [cite_start]`"Embrace the Darkness"` / `"Walk toward the Light"` / `"Use System Theme"`: Toggles between dark, light, and system themes. [cite: 778] Event: `ToggleForceTheme`.
  * [cite_start]`"Luminance"`: Opens the luminance adjustment dialog. [cite: 778] Event: `ToggleLuminanceDialog`.
  * `"Glow Stick"`: Opens the glow adjustment dialog. [cite: 778] Event: `ToggleGlowStickDialog`.

* **Section 4: Help & Info**
  * `"WTF is all this?"` / `"OK, I get it."`: Toggles help text visibility. [cite: 777] Event: `ToggleHelp`.
  * [cite_start]`"Show Tutorial"`: Starts the interactive tutorial. [cite: 777] Event: `StartTutorial`.

* **Section 5: Meta**
  * [cite_start]`"Check for Updates"`: Opens the project's GitHub releases page. [cite: 779] Event: `CheckForUpdate`.
  * `"About Me"`: Launches an intent to open the developer's Instagram profile (`https://www.instagram.com/hereliesaz/`). [cite: 779] Event: `ViewArt`.
  * [cite_start]`"Chalk Your Tip"`: Opens the donation dialog. [cite: 779] Event: `ShowDonationOptions`.

* **Section 6: Developer**
  * `"Too Advanced Options"`: Opens the CV parameter tuning dialog. Event: `ToggleAdvancedOptionsDialog`.
  * `"Toggle Snapping"` / `"Disable Snapping"`: Toggles the auto-snapping of logical balls to detected balls. Event: `ToggleSnapping`.