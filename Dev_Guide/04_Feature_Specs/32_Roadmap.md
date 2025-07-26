# 22: General Roadmap

* **Hater Mode Physics:**
  * ~~Implement a full physics simulation for gravity, drag, and rotational inertia.~~ **(Completed
    07/26/2025)**
* **Object/Ball Detection (Computer Vision):**
  * Initial ML Kit implementation for ball detection is complete. Awaiting real-world testing and parameter tuning for the custom model.
  * **Table Detection:** This feature has been **removed** as of 07/12/2025. All logic for finding table corners via CV has been purged from the codebase. It was deemed unreliable and may be revisited in a distant future.
* **Tutorial Enhancements:**
  * Make the tutorial more interactive.
* **Shot Difficulty Analysis:**
  * Provide a "difficulty rating" for the current shot.
* **"Drills" Mode:**
  * Create a predefined set of common practice shots.
* **Line-of-Aim Sensitivity Control:**
  * Add a user setting to adjust the sensitivity of the aiming rotation gesture.
* **Application Updater Replacement:**
  * ~~All in-app update check logic must be removed.~~ **(Completed 07/10/2025)**
  * ~~The UI should display the currently installed application version.~~ **(Completed 07/10/2025)**
  * ~~The UI must fetch and display the latest available version tag from the project's GitHub repository.~~ **(Completed 07/10/2025)**
  * ~~A static link must be provided to the repository's releases page: `https://github.com/HereLiesAz/CueDetat/releases`.~~ **(Completed 07/10/2025)**
* **Interactive Tutorial:**
  * ~~A menu option must exist that initiates a series of interactive, step-by-step instructions.~~ **(Completed 07/10/2025)**
  * ~~The tutorial must actively guide the user on how to use the app's features in both Protractor and Banking modes.~~ **(Completed 07/10/2025)**
* **Donation Dialog:**
  * ~~Implement a dialog, triggered from the menu via the `ShowDonationOptions` event, that presents multiple external donation links (PayPal, Venmo, CashApp).~~ **(Completed 07/10/2025)**
  * ~~Selecting an option must launch an `Intent` to open the corresponding URL.~~ **(Completed 07/10/2025)**