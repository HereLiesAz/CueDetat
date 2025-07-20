# 3.10. Resource Asset Manifest

This document catalogs the mandatory non-code assets required for the application's UI and branding.

## `res/drawable`

* **Vector Icons (XML):**
    * `ic_dark_mode_24.xml`: Icon for dark theme.
    * `ic_help_outline_24.xml`: Icon for help menu item.
    * `ic_jump_shot.xml`: Icon for toggling the cue ball.
    * `ic_light_mode_24.xml`: Icon for light theme.
    * `ic_undo_24.xml`: Icon for the reset button.
    * `ic_zoom_in_24.xml`: Decorative icon for the zoom slider area.
    * `pool_table.xml`: Icon for toggling the table visibility.
* **Raster Images (WEBP):**
    * `logo_cue_detat.webp`: The primary application logo used in the top-left menu button and
      splash screen.
* **XML Shapes & Layers:**
    * `ic_launcher_background.xml`: A simple background layer for the adaptive icon.
    * `seekbar_custom_track.xml`: Defines the visual style for horizontal sliders.

## `res/font`

* **Font File:**
    * `barbaro.ttf`: The primary typeface for the application.
* **Font Family Definition:**
    * `barbaro_family.xml`: Defines the font family for use in themes and styles.

## `res/mipmap`

* **Launcher Icons:** The application uses adaptive icons.
    * **`mipmap-anydpi-v26/`**: `ic_launcher.xml` and `ic_launcher_round.xml` reference the drawable
      assets.
    * **Density Buckets (`-hdpi`, `-mdpi`, etc.):** Each must contain `ic_launcher.webp` and
      `ic_launcher_round.webp` for legacy support, along with their foreground/background layers if
      needed.

## `res/values`

* **`colors.xml`**: Defines the application's full color palette, including thematic colors like
  `AccentGold`, `OracleBlue`, and `AcidPatina`.
* **`strings.xml`**: Contains all user-facing strings, including the `app_name`, `tagline`, and the
  `insulting_warnings` string array.
* **`themes.xml`**: Defines the base application theme (`Theme.CueDetat`).