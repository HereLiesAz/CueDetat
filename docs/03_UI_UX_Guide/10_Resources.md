# 3.10. Resource Asset Manifest

This document catalogs the mandatory non-code assets required for the application's UI and branding.

## `res/drawable`

* **Raster Images:**
    * `logo_cue_detat.png`: The primary application logo used in the top-left menu button and
      splash screen.

The launcher icon layers live under `mipmap-*`, not `res/drawable`. Unreferenced vector icons,
shapes and the Hater-mode `group*.webp` images were removed; Hater mode draws its die face at
runtime from `HaterResponses.allAnswers`.

## `res/font`

* **Font File:**
    * `barbaro.ttf`: The primary typeface for the application.
* **Font Family Definition:**
    * `barbaro_family.xml`: Defines the font family for use in themes and styles.

## `res/mipmap`

* **Launcher Icons:** The application uses adaptive icons.
    * **`mipmap-anydpi-v26/`**: `ic_launcher.xml` and `ic_launcher_round.xml` reference the `mipmap`
      layers (`ic_launcher_background`, `ic_launcher_foreground`, `ic_launcher_monochrome`).
    * **Density Buckets (`-hdpi`, `-mdpi`, etc.):** Each must contain `ic_launcher.webp` and
      `ic_launcher_round.webp` for legacy support, along with their foreground/background layers if
      needed.

## `res/values`

* There is no `colors.xml`: the palette lives in Compose (`ui/theme/Color.kt`). The XML copy
  had no `@color`/`R.color` reader and was removed.
* **`strings.xml`**: Contains all user-facing strings, including the `app_name`, `tagline`, and the
  `insulting_warnings` string array.
* **`themes.xml`**: Defines the base application theme (`Theme.CueDetat`).