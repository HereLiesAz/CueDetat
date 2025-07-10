# The Menu

*   **Access:** The menu is a modal navigation drawer opened from the top-left of the screen.
*   **Background:** The menu background must be set to 80% opacity.
*   **Controls:** All items in the menu must be text-only `TextButton`s. No `Switch` components are permitted.
*   **Contextual Labels:** All toggles must have contextual text that reflects the action to be taken (e.g., "Hide Table" if the table is visible, "Show Table" if it is hidden).

***
## Addendum: Menu Item Specifications

The menu must contain the following items, triggering the specified events.

*   **Help & Tutorial**
*   `"WTF is all this?"` / `"OK, I get it."`: Toggles help text visibility. Event: `ToggleHelp`.
*   `"More Help"`: (Future) Will toggle an additional layer of instructional text. Event: `ToggleMoreHelp`.
*   `"Show Tutorial"`: Starts the interactive tutorial. Event: `StartTutorial`.

*   **View & Mode Controls**
*   `"Reset View"`: Resets all state to default. Event: `Reset`.
*   `"Toggle Cue Ball"` / `"Hide Cue Ball"`: Toggles the `ActualCueBall` in Protractor mode. **This option must be hidden if the table is visible (`showTable = true`) or if in Banking Mode (`isBankingMode = true`).** Event: `ToggleOnPlaneBall`.
*   `"Calculate Bank"` / `"Ghost Ball Aiming"`: Toggles between Banking and Protractor modes. Event: `ToggleBankingMode`.
*   `"Toggle Table"` / `"Hide Table"`: Toggles the visibility of the table wireframe. This is only visible in Protractor Mode. Event: `ToggleTable`.
*   `"Table Size"`: Opens the table size selection dialog. Event: `ToggleTableSizeDialog`.

*   **Theme & Appearance**
*   `"Embrace the Darkness"` / `"Walk toward the Light"` / `"Use System Theme"`: Toggles between dark, light, and system themes. Event: `ToggleForceTheme`.
*   `"Use Imperial Units"` / `"Use Metric Units"`: Toggles the distance unit display. Event: `ToggleDistanceUnit`.
*   `"Show Camera"` / `"Hide Camera"`: Toggles the visibility of the live camera feed background. Event: `ToggleCamera`.
*   `"Luminance"`: Opens the luminance adjustment dialog. Event: `ToggleLuminanceDialog`.

*   **Meta Section**
*   `"About Me"`: Launches an intent to open the developer's Instagram profile (`https://www.instagram.com/hereliesaz/`). Event: `ViewArt`.
*   `"Chalk Your Tip"`: Opens the donation dialog. Event: `ShowDonationOptions`.
*   `"Check for Updates"`: Opens the project's GitHub releases page. Event: `CheckForUpdate`.