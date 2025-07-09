# 10: Windows & Dialogs

* **Opacity:** All popup dialog windows (e.g., the luminance editor) must have their backgrounds set to 66% opacity (`0.66f`).
* **Luminance Dialog:** The slider within this dialog must be horizontal.

***
## Addendum: Specific Window Implementations

* **Luminance Adjustment Dialog**:
    * Must be an `AlertDialog`.
    * It must contain a horizontal `ExpressiveSlider` to control the `luminanceAdjustment` state property.
    * It must contain a "Done" `TextButton` to dismiss the dialog.

* **Donation Dialog**:
    * Must be an `AlertDialog`.
    * It must display a list of donation options ("PayPal", "Venmo", "CashApp").
    * Selecting an option must trigger an `Intent` to open the corresponding external URL.

* **Interactive Tutorial Overlay**:
    * This is a full-screen overlay, not a standard dialog.
    * It must have a semi-transparent background (`scrim` color with 85% opacity).
    * It must display instructional text from a predefined list, based on the current tutorial step.
    * It must have "Next" and "Finish" `TextButton`s to navigate through the steps or dismiss the overlay.
    * It must consume all touch events to prevent interaction with the underlying UI while active.