# Cue D’état - An IRL Billiards Aiming Assistant for Android

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
![GitHub Release](https://img.shields.io/github/v/release/hereliesaz/CueDetat?include_prereleases&display_name=release)


**_May your aim be truer than your excuses._**


**Cue D’état** is here to ostensibly help billiards players aim, determine shot angles, make the right cuts and banks, understand the tangent line, and improve their geometric understanding of the game. Stop wondering which magical potion made of mostly alcohol best improves your game, and instead see pool as a series of physics problems you are consistently failing. Maybe you get called a cheater, even though using this app is entirely legal. At the very least, get yourself a high-tech understanding of how bad you are at pool.

This is an Android application that uses your device's camera and a frankly excessive amount of mathematics to overlay aiming guides onto a pool table. It exists because the universe is governed by knowable laws, and you don't really live in that universe. A problem this app might, reluctantly, help rectify.

**(Warning: May induce an inflated sense of skill, followed by the crushing reality of physics. Use with a healthy dose of self-deprecating humor.)**

## Features

*   **Live Camera Augmented Reality Overlay:**
    *   See the guides directly on your pool game.
    *   Designed for easy one or two-handed use. 
*   **Protractor Mode** 
    *   To remind you of the basic, soul-crushing simplicity of a cut shot.
    *   See where the balls will go before you hit them.  
    *   Rotates and zooms with on-screen gestures, tilts using the gyroscope.
    *   Banking made easy, displaying a "diamond count," so you can gently rail your balls in the way they like best.
    *   See what a tangent line is, why it's important for knowing what will happen to the cue ball, even if English (sidespin) is applied.   
*   **Make Bank**
    *   Calculate your multi-rail bank shots.
    *   Proof that even chaos subscribes to the laws of reflection, and it might be time to look at your own. 
*   **All of the Balls You Could Want**
    *   (You definitely want them.)
    *   Simulated motherf---ing balls on a projected motherf---ing plane.
    *   Determine whether or not another ball is in the way. 
*   **Spin Control**    
    *   A tool for applying English. Maybe even British.
    *   Explore the subtle arts of post-impact trajectory, and other new and exciting ways to scratch.
*   **Dynamic 3D Perspective** 
    *   Use your phone's sensor data to create a 3D illusion. This feature's primary purpose is to induce a subtle vertigo that mirrors the existential dread of a poorly-played safety.  
* **VERY Helpful Help:**
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

Cue D’état is built upon a single, immutable truth:
Unidirectional Data Flow is the master of the master of the universe. State flows down, events flow up. To question this is to question physics, which is how you got yourself to this point in the first place.

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

*   The ghosts of billiards past whose missed shots inspired all this.
*   The people I've tried to teach all these things.
*   Physics. And geometry. Where my hoes at?! Pythagoras! Decartes! Newton, you bish! 

---

