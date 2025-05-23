# Cue D’état - Pool Protractor & Aiming Assistant

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
![GitHub Release](https://img.shields.io/github/v/release/hereliesaz/CueDetat)

## _"They'll never see it coming."_

**Cue D’état** is an Android application designed to (ostensibly) help pool players visualize shot angles, make cut shots, understand the cue ball tangent, improve their geometric understanding of the game. Maybe get called a cheater, even though using this app is entirely legal. At the very least, get yourself a high-tech understanding of how bad you are at pool.

It uses your phone's camera and orientation sensors to overlay a dynamic protractor and aiming guide onto the real-world view of a pool table.

**(Warning: May induce an inflated sense of skill, followed by the crushing reality of physics. Use with a healthy dose of self-deprecating humor.)**

## Features

*   **Live Camera Overlay:** See the guides directly on your table view.
*   **Dynamic Protractor:**
    *   Visualizes common cut angles.
    *   Rotates with on-screen gestures (pan to rotate).
*   **Cue & Target Ball Representation:**
    *   Simulated "ghost balls" on a projected plane.
    *   Visual feedback for aiming path and potential collisions.
*   **Pitch-Adjusted Perspective:** The protractor plane tilts based on your phone's orientation, giving a pseudo-3D effect.
*   **Zoom Functionality:** Pinch to zoom in/out for a closer look or a wider view. Slider control with a stretched "zoomed-out" range for finer adjustments.
*   **Deflection / Sidespin Axis Lines:** Visualizes the tangent line and the resulting cue ball path if English (sidespin) were applied.
*   **Pocket Aiming Line:** A guide extending from the target ball to help line up with pockets.
*   **Informative (and sometimes Sarcastic) Text Helpers:**
    *   Labels for key lines and aiming points.
    *   Toggleable visibility for a cleaner view.
    *   Delightfully condescending warnings for invalid shot setups.
*   **Themed UI:** A sleek 8-ball aesthetic (Yellow, Black, White).

## Screenshots Placeholder

*   Pretend this is a screenshot.
*   This, too.
*   Imagine looking at a photo of the app in use.
*   Note the craft.
*   The flippant attitude towards detail.
*   I'm a genieaouxess.
*   And this is a photo from a vacation two years ago that I accidentally pretend included.



## How It Works: The Gore. The Details.

1.  **Camera Preview:** Uses CameraX to display a live feed from the device camera.
2.  **Sensor Input:** Leverages the `TYPE_ROTATION_VECTOR` sensor to determine the phone's pitch, roll, and yaw. The pitch is primarily used to tilt the 2D protractor plane. An offset is applied to account for natural phone holding angles.
3.  **Custom View (`ProtractorOverlayView`):** All guides and visual elements are drawn on a custom `View` that overlays the camera preview.
4.  **Drawing Logic:**
    *   **Protractor Plane:** A logical 2D plane is defined. Circles representing the cue and target ball positions, protractor angle lines, and deflection lines are drawn on this plane.
    *   **3D Projection (Simplified):** An `android.graphics.Camera` object is used to apply an X-axis rotation (based on phone pitch) to this logical plane, creating a 3D perspective effect. This transformed matrix is then applied to the canvas.
    *   **Ghost Balls:** Screen-space circles are drawn to represent the "3D" position of the cue and target balls. Their Y-offset from the projected plane centers is scaled by the sine of the pitch angle (raised to a power for a more pronounced effect) to simulate them floating above the plane.
    *   **Helper Text:** Text labels are drawn either on the (lifted) protractor plane or directly in screen space, with basic collision avoidance and dynamic sizing.
5.  **Gesture Handling:**
    *   `ScaleGestureDetector` for pinch-to-zoom.
    *   `MotionEvent` tracking for single-finger pan-to-rotate.
6.  **Theming:** Uses Jetpack Compose for Material 3 theming, with color values then passed to the custom view's `Paint` objects.

## Tech Stack & Libraries

*   **Kotlin**
*   **Android SDK**
*   **CameraX:** For camera preview.
*   **Android Sensors:** For device orientation.
*   **Custom View Drawing:** `Canvas` and `Paint` for all visual overlays.
*   **Jetpack Compose:** For Material 3 theming (colors applied to legacy View system).
*   **Material Components for Android:** For UI elements like FABs and Seekbar.
*   **ConstraintLayout:** For UI layout.

## Setup & Build

1.  Clone the repository: `git clone https://github.com/hereliesaz/CueDetat.git`
2.  Open the project in Android Studio (latest stable version recommended).
3.  Ensure you have the necessary Android SDK Platforms and Build Tools installed.
4.  The project uses Gradle. Sync the project with Gradle files.
5.  Build and run on an Android device or emulator (API 26+).
    *   A device with a camera and rotation vector sensor is required for full functionality.

## Known Quirks & Future Delusions

*   **Text Collision Avoidance:** Current implementation is basic (first-drawn wins space).
*   **A Virtual Table for Virtually Useful Bank Shot Projection:** Using more sophisticated dynamic layout involving a line drawing of a billiards table will come someday.
*   **True 3D Rendering:** This app fakes 3D with 2D canvas tricks. Moving to OpenGL ES or a 3D engine like Filament would allow for actual 3D models and lighting, but would also drastically increase complexity. And probably anxiety. But probably not usefulness.
*   **Ball, Table and Pocket Detection:** The ultimate fantasy. Using CV to detect the table, balls, and pockets automatically. For now, you are the CV.
*   **Insulting Warnings:** The pool of sarcastic remarks is finite. Contributions welcome if they tickle me the required level of pink.
*   **Performance:** Drawing many complex paths and text elements on every frame can be demanding. Optimizations are an ongoing battle. And yet, somehow, it feels more like a you-problem.

## Contributing

Found a bug? Have a suggestion for an even more cutting remark? Feel free to open an issue or submit a pull request. Please adhere to the existing coding style (or lack thereof, depending on the file you're looking at).

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

## License

Distributed under the MIT License. Basically, completely free to use however you'd like, just gimme a shoutout. I make money making art. So, like this:
Cue D’état by HereLiesAz (https://instagram.com/hereliesaz)

## Acknowledgments (Or Who to Blame)

*   The ghosts of pool players past whose missed shots inspired this.
*   The people I've tried to teach all these things.
*   Physics. And geometry. Where my hoes at?!

---

**_May your aim be truer than your excuses._**
