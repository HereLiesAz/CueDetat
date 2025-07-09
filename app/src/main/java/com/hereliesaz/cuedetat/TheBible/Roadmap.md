# 22: General Roadmap

* **Bank/Kick Shot Calculator (Refinement):**
  * Improve reflection logic for more than 2 banks.
  * Consider pocket geometry for line termination and shot success/failure.
* **Object/Table Detection (Computer Vision):**
  * Use OpenCV or ML Kit to detect table boundaries and ball positions automatically.
* **"English" / Spin Visualization & Guide:**
  * Add a UI control to simulate striking the cue ball off-center.
  * Display a standard set of resulting cue ball paths based on spin.
* **Tutorial Enhancements:**
  * Make the tutorial more interactive.
* **Shot Difficulty Analysis:**
  * Provide a "difficulty rating" for the current shot.
* **"Drills" Mode:**
  * Create a predefined set of common practice shots.
* **Line-of-Aim Sensitivity Control:**
  * Add a user setting to adjust the sensitivity of the aiming rotation gesture.
* **Application Updater Replacement:**
  * All in-app update check logic must be removed.
  * The UI should display the currently installed application version.
  * The UI must fetch and display the latest available version tag from the project's GitHub repository.
  * A static link must be provided to the repository's releases page: `https://github.com/HereLiesAz/CueDetat/releases`.
* **Interactive Tutorial:**
  * A menu option must exist that initiates a series of interactive, step-by-step instructions.
  * The tutorial must actively guide the user on how to use the app, highlighting relevant UI elements that correspond to the current step of the instructions.
* **Bank/Kick Shot Calculator (Refinement):**
  * Improve reflection logic for more than 2 banks.
  * Consider pocket geometry for line termination/success.
* **Object/Table Detection (Computer Vision):**
  * Use OpenCV or ML Kit to detect table boundaries and ball positions automatically.
* **"Drills" Mode:**
  * Create a predefined set of common practice shots (e.g., stop shot, follow shot, standard cut angles).
* **Line-of-Aim Sensitivity Control:**
  * Add a user setting to adjust the sensitivity of the aiming rotation gesture.

***
## Addendum: Features for Re-implementation

The following features were present in the v0.3.5 build and must be re-implemented to restore full functionality.

* **Application Update Checker:**
  * Implement a mechanism to query the GitHub repository's "latest release" endpoint (`https://api.github.com/repos/hereliesaz/CueDetat/releases/latest`).
  * Compare the fetched `tag_name` with the `BuildConfig.VERSION_NAME`.
  * Display the result (up-to-date, update available, or check failed) to the user via a `ToastMessage`.
  * Provide a static link to the repository's releases page: `https://github.com/HereLiesAz/CueDetat/releases`.

* **Interactive Tutorial Overlay:**
  * Implement a modal, multi-step tutorial that overlays the entire UI, triggered from the menu via the `StartTutorial` event.
  * The overlay must display instructional text and have "Next"/"Finish" navigation controls.
  * The tutorial should actively guide the user on how to use the app's features in both Protractor and Banking modes.

* **Donation Dialog:**
  * Implement a dialog, triggered from the menu via the `ShowDonationOptions` event, that presents multiple external donation links (PayPal, Venmo, CashApp).
  * Selecting an option must launch an `Intent` to open the corresponding URL.