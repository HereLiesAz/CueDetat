# 3.4. Menu Drawer

### Implementation

* The menu is implemented as a custom composable using `AnimatedVisibility`, not a standard
  `ModalNavigationDrawer`.
* **Animation**:
* **Enter**: The menu unfolds from the top edge of the screen.
* **Exit**: The menu sweeps away horizontally to the left.
* **Scrim**: A semi-transparent black overlay must cover the main content when the menu is open.

### Content

* **Controls:** All items must be text-only `TextButton`s.
* **Contextual Labels:** Toggles must have text that reflects the action to be taken (e.g., "Hide
  Cue Ball" if it is visible).
* **Organization:** The menu must be organized into logical sections separated by dividers, in this
  order:

1. Core Controls
2. Table & Units
3. Appearance
4. Help & Info
5. Meta (About, Contact, etc.)
6. Developer