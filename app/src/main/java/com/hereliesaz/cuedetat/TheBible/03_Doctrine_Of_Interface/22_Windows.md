# 10: Windows & Dialogs

* [cite_start]**Opacity:** All popup dialog windows (e.g., the luminance editor) must have their backgrounds set to 66% opacity (`0.66f`). [cite: 761]
* [cite_start]**Luminance Dialog:** The slider within this dialog must be horizontal. [cite: 761]

***
## Addendum: Specific Window Implementations

* **Luminance Adjustment Dialog**:
  * Must be an `AlertDialog`.
  * It must contain a horizontal `Slider` to control the `luminanceAdjustment` state property.
  * It must contain a "Done" `TextButton` to dismiss the dialog.

* **Glow Stick Dialog**:
  * Must be an `AlertDialog`.
  * It must contain a horizontal `Slider` to control the `glowStickValue` state property, ranging from -1.0 (full black glow) to 1.0 (full white glow), with 0.0 being no glow.
  * It must contain a "Done" `TextButton` to dismiss the dialog.

* **Table Size Selection Dialog**:
  * Must be an `AlertDialog` summoned from the menu.
  * [cite_start]It must display a list of all available table sizes (6', 7', 8', 9', 10'). [cite: 762]
  * [cite_start]Selecting an option must trigger a `SetTableSize` event and dismiss the dialog. [cite: 762]
  * It must have a "Cancel" button.

* **Too Advanced Options Dialog**:
  * Must be an `AlertDialog` summoned from the menu.
  * It must contain a `TextButton` to toggle between the "Generic" and "Custom" AI models.
  * It must contain a `TextButton` to toggle between `HOUGH` and `CONTOUR` refinement methods.
  * It must contain four horizontal `Slider` controls for the Hough and Canny CV parameters.
  * It must have a "Done" button to dismiss.

* **Table Color Calibration Window**:
  * A new, dedicated workflow must be implemented.
  * It must guide the user to aim at the center of the table and tap a crosshair to sample the color.
  * It must display the sampled color and provide "Accept" and "Try Again" options.

* **Interactive Tutorial Overlay**:
  * This is a full-screen overlay, not a standard dialog. **Its current implementation is flawed.**
  * **Requirement:** It must be redesigned to be non-blocking. The overlay must be transparent, allowing the user to see the UI elements being described. User interaction with the underlying UI must be possible while the tutorial text is displayed.
  * It must display instructional text from a predefined list, based on the current tutorial step.
  * It must have "Next" and "Finish" `TextButton`s to navigate through the steps or dismiss the overlay.
  * **Virtual Table**: A non-camera-based background, showing a simple, clean representation of a pool table, must be available for the tutorial so users can follow along without being in front of a physical table.