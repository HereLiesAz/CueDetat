# 11: The Menu

* **Access:** The menu is a modal navigation drawer opened from the top-left of the screen.
* **Background:** The menu background must be set to 80% opacity.
* **Controls:** All items in the menu must be text-only `TextButton`s. No `Switch` components are permitted.
* **Contextual Labels:** All toggles must have contextual text that reflects the action to be taken (e.g., "Hide Table" if the table is visible, "Show Table" if it is hidden).

***
## Addendum: Menu Item Specifications

The menu must contain the following items, triggering the specified events.

*   **Help & Tutorial**
  *   `"WTF is all this?"` / `"OK, I get it."`: Toggles help text visibility. Event: `ToggleHelp`.
  *   `"More Help"`: (Future) Will toggle an additional layer of instructional text. Event: `ToggleMoreHelp`.
  *   `"Start Tutorial"`: Starts the interactive tutorial. Event: `StartTutorial`.

*   **View & Mode Controls**
  *   `"Reset View"`: Resets all state to default. Event: `Reset`.
  *   `"Toggle Cue Ball"`: Toggles the `ActualCueBall` in Protractor mode. Event: `ToggleCueBall`.
  *   `"Calculate Bank"` / `"Ghost Ball Aiming"`: Toggles between Banking and Protractor modes. Event: `ToggleBankingMode`.
  *   `"Toggle Table"`: Toggles the visibility of the table wireframe. Event: `ToggleTable`.

*   **Theme & Appearance**
  *   `"Embrace the Darkness"` / `"Walk toward the Light"`: Toggles between dark and light themes. Event: `ToggleForceTheme`.
  *   `"Luminance"`: Opens the luminance adjustment dialog. Event: `ShowLuminanceDialog`.

*   **Meta Section**
  *   `"About Me"`: Launches an intent to open the developer's Instagram profile (`https://www.instagram.com/hereliesaz/`). Event: `ViewArt`.
  *   `"Chalk Your Tip"`: Opens the donation dialog. Event: `ShowDonationOptions`.
  *   `"Check for Updates"`: Triggers the application update check. Event: `CheckForUpdate`.