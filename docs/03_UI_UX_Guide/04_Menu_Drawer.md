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

## Navigation Rail (AR Controls)

The AR-related controls live in the navigation rail (`AzNavRailMenu`), not the drawer.

- **AR toggle**: A rail toggle with options **"AR"** and **"off"**. Lit (active state) when
  `cameraMode == AR_SETUP` or `cameraMode == AR_ACTIVE`. Initiates AR setup or turns off camera.
- **Felt item**: Visible immediately upon AR initialization. Pressing it adds a new color sample
  to the persistent collection.
- **Cancel item**: Visible when `cameraMode == AR_SETUP`. Stops all CV/AR processing but leaves the
  camera feed active.

- All rail controls are gated by `experienceMode`.

There are no "Recalibrate Felt", "Felt Capture", or equivalent advanced menu items for felt re-sampling. The Felt rail button is the sole entry point for opening the table scan overlay.