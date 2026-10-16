# 3.7. Icon Specifications

This document outlines the specific icons used in the application. Most buttons use text labels;
icons are reserved for specific UI affordances.

* **Top-Left Menu Icon (`logo_cue_detat.webp`)**
    * **Component:** `TopControls` area.
    * **Function:** Tapping opens the menu drawer.
    * **Behavior:** The icon is displayed by default. When helper text is enabled (
      `areHelpersVisible = true`), the icon is replaced by the app title and tagline text.

* **Other Icons**
    * The app ships no other drawable icons; rail items are text buttons and in-app icons come from
      Material Icons (e.g. `Icons.Default.OpenWith` on the spin control).