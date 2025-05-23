# Cue D’état - Pool Protractor & Aiming Assistant

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
![GitHub Release](https://img.shields.io/github/v/release/hereliesaz/CueDetat)


**_May your aim be truer than your excuses._**


**Cue D’état** is an Android app to ostensibly help billiards players line up and aim, determine shot angles, make cut shots, understand the cue ball tangent, improve their geometric understanding of the game. Maybe get called a cheater, even though using this app is entirely legal. At the very least, get yourself a high-tech understanding of how bad you are at pool.

It uses your phone's camera and orientation sensors to overlay a dynamic protractor and aiming guide onto the real-world view of a pool table.

**(Warning: May induce an inflated sense of skill, followed by the crushing reality of physics. Use with a healthy dose of self-deprecating humor.)**



## Features

*   **Live Camera Augmented Reality Overlay:**
    *   See the guides directly on your pool game.
    *   Designed for easy one or two-handed use. 
*   **Dynamic Protractor:**
    *   See where the balls will go before you hit them.  
    *   Rotates and zooms with on-screen gestures, tilts using the gyroscope. 
*   **Cue Ball & Target Ball Representation:**
    *   Simulated "ghost balls" on a projected plane.
    *   Visual feedback for aiming path and potential collisions.
*   **Pitch-Adjusted Perspective:** The protractor plane tilts based on your phone's orientation for a three-dimensional augmented reality.
*   **Where will the Cue ball go?** Visualizes the tangent line and the resulting cue ball path if English (sidespin) were applied.
*   **Guided Shots** Guide lines extend from the target ball to line up with your table's pockets.
*   **VERY Helpful Help:**
    *   Labels for key lines and what to do with them.
    *   Instructions better than Ikea's.
    *   Toggleable Help visibility for a cleaner view.
    *   Uplifting messages of slightly disdainful encouragement.



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



## Known Quirks & Future Delusions

*   **Text Collision Avoidance:** Current implementation is basic (first-drawn wins space).
*   **A Virtual Table for Virtually Useful Bank Shot Projection:** Using more sophisticated dynamic layout involving a line drawing of a billiards table will come someday.
*   **True 3D Rendering:** This app fakes 3D with 2D canvas tricks. Moving to OpenGL ES or a 3D engine like Filament would allow for actual 3D models and lighting, but would also drastically increase complexity. And probably anxiety. But probably not usefulness.
*   **Ball, Table and Pocket Detection:** The ultimate fantasy. Using CV to detect the table, balls, and pockets automatically. For now, you are the CV.
*   **Insulting Warnings:** The pool of sarcastic remarks is finite. Contributions welcome if they tickle me the required level of pink.
*   **Performance:** Drawing many complex paths and text elements on every frame can be demanding. Optimizations are an ongoing battle. And yet, somehow, it feels more like a you-problem.



## License

Distributed under the MIT License. Basically, completely free to use however you'd like, just gimme a shoutout. I make money making art. So, like this:
Cue D’état by HereLiesAz (https://instagram.com/hereliesaz)


## Acknowledgments (of Who to Blame)

*   The ghosts of billiards past whose missed shots inspired this.
*   The people I've tried to teach all these things.
*   Physics. And geometry. Where my hoes at?!

---

