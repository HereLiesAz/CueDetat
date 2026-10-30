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
- **lock / unlock** (`AR_ACTIVE`, ARCore devices only): anchors the virtual table to the real one.
- **save 24h** (locked table), **restore**, **forget saved**: ARCore Cloud Anchors; saving asks for
  confirmation each time because it uploads mapping data to Google.
- **scan table** (`AR_SETUP` / `AR_ACTIVE`, not while the scan overlay is up) reopens the felt capture;
  **forget scan** drops the held table model.
- **view / back**: the top-down snapshot, shown in Expert whenever the camera is in an AR mode and a
  table is projected.
- **cue ball / target ball** (Expert): tap one, then tap that ball on the screen. Coloured as the
  virtual balls are drawn.
- **Brightness** and **Glow stick** (drawer, all modes) open the overlay-luminance and glow dialogs.
- Toggle labels name the *action* for commands (lock/unlock, ar/off, view/back, bank/aim) and the
  *current state* for two-way settings (dynamic/static, solids/stripes, metric/imperial). The
  Mode and Orientation items show the value a tap is about to apply.

- All rail controls are gated by `experienceMode`.
- **Tutorial item**: re-launches the in-app tutorial, now driven by AzNavRail 10.18's status-driven guidance framework (`azStatus`/`azEdge`/`azGoal`/`azGuidanceTarget`). Highlights are framework-drawn spotlights on the in-camera target/ghost/cue ball, aiming line, and zoom slider; a bottom "Finish" affordance completes the flow.

There are no "Recalibrate Felt" or "Felt Capture" items. The **scan table** rail item is the entry point for reopening the table scan overlay.