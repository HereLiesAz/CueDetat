# 3.8. Label Specifications

* **Visibility:** All descriptive text labels for UI elements on the main overlay are hidden by
  default.
* **Toggle:** Their visibility is controlled exclusively by the "WTF is all this?" menu option (
  `areHelpersVisible` state property). This includes labels on sliders, balls, and lines.

## Specific Label Content

When help text is enabled, the following labels must be used:

* **Balls (Protractor Mode):** "Target Ball", "Ghost Cue Ball", "Actual Cue Ball".
* **Ball (Banking Mode):** "Ball to Bank".
* **Lines (Protractor Mode):** "Aiming Line", "Shot Guide Line", "Tangent Line", and degree values
  for angle guides (e.g., "30°").
* **Lines (Banking Mode):** "Bank 1", "Bank 2", etc., for each segment of the reflected shot path.
* **Placement**: Labels must be placed near their respective objects at a distance proportional to
  the current zoom level. Line labels must be rotated to match the line's angle.