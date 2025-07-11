# 22: General Roadmap

* **Bank/Kick Shot Calculator (Refinement):**
  * ~~Improve reflection logic for more than 2 banks.~~ **(Completed 07/10/2025)**
  * ~~Consider pocket geometry for line termination and shot success/failure.~~ **(Completed 07/10/2025)**
  * ~~Integrate spin/english to modify bank shot rebound angles.~~ **(Completed 07/10/2025)**
* **"English" / Spin Visualization & Guide:**
  * ~~Add a UI control to simulate striking the cue ball off-center.~~ **(Completed 07/10/2025)**
  * ~~Display a standard set of resulting cue ball paths based on spin.~~ **(Completed 07/10/2025)**
  * ~~Implement banking preview for spin paths.~~ **(Completed 07/10/2025)**
* **Object/Table Detection (Computer Vision):**
  * Use OpenCV or ML Kit to detect table boundaries and ball positions automatically.
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