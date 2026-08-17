# Cue D’état - An IRL Billiards Aiming Assistant for Android

**_May your aim be truer than your excuses._**


**Cue D’état** is here to ostensibly help billiards players aim, determine shot angles, make the right cuts and banks, understand the tangent line, and improve their geometric understanding of the game. Stop wondering which magical potion made of mostly alcohol best improves your game, and instead see pool as a series of physics problems you are consistently failing. Maybe you get called a cheater, even though using this app is entirely legal. At the very least, get yourself a high-tech understanding of how bad you are at pool.

This is an Android application that uses your device's camera and a frankly excessive amount of mathematics to overlay aiming guides onto a pool table. It exists because the universe is governed by knowable laws, and you don't really live in that universe. A problem this app might, reluctantly, help rectify.

**(Warning: May induce an inflated sense of skill, followed by the crushing reality of physics. Use with a healthy dose of self-deprecating humor.)**

## Features

* **Live Camera Augmented Reality Overlay:**
    * See the guides directly on your pool game.
    * Designed for easy one or two-handed use.
    * **Guided AR Table Setup Wizard:** A four-step wizard (lock felt color → tap the four corner pockets → optional manual per-pocket guide → auto-ready) walks you through calibrating the AR overlay. The system auto-confirms once the table overlay confidence crosses 0.8. If ARCore's tracking blips for a moment, the app just floats on the last known table pose instead of throwing your whole scan away — it turns out constantly nuking your progress every time a hand crosses the lens was *more* annoying than the tracking hiccup itself, so a full rescan is now only required if you back all the way out of AR setup yourself.
* **Protractor Mode**
    * To remind you of the basic, soul-crushing simplicity of a cut shot.
    * See where the balls will go before you hit them.
    * Rotates and zooms with on-screen gestures, tilts using the gyroscope.
    * Banking made easy, displaying a "diamond count," so you can gently rail your balls in the way they like best.
    * See what a tangent line is, why it's important for knowing what will happen to the cue ball, even if English (sidespin) is applied.
* **Make Bank**
    * Calculate your multi-rail bank shots.
* **Proof that even chaos subscribes to the laws of reflection, and it might be time to look at your own.**
* **All the Balls You Could Want**
    * (You definitely want them.)
    * Simulated motherf---ing balls on a projected motherf---ing plane.
    * Determine whether another ball is in the way.
* **Spin Control**
    * A tool for applying English. Maybe even British.
    * Explore the subtle arts of post-impact trajectory, and other new and exciting ways to scratch.
* **Massé Master**
    * Pick your impact point and the angle of attack to make the prettiest loop-de-loos on the table.
    * Leave your opponent's jaw on the floor as you tear the felt up with such precision.
* **Dynamic 3D Perspective**
    * Use your phone's sensor data to create a 3D illusion. This feature's primary purpose is to induce a subtle vertigo that mirrors the existential dread of a poorly-played safety.
* **VERY Helpful Help:**
    * Labels for key lines and what to do with them.
    * Instructions better than Ikea's.
    * Toggleable Help visibility for a cleaner view.
    * Uplifting messages of slightly disdainful encouragement.

## Screenshots Placeholder

* Pretend this is a screenshot.
* This, too.
* Imagine looking at a photo of the app in use.
* Note the craft.
* The flippant attitude towards detail.
* I'm a genieaouxess.
* And this is a photo from a vacation two years ago that I accidentally pretend included.

## How It Works: The Gore.
The Details.

Cue D’état is built upon a single, immutable truth:
Unidirectional Data Flow is the master of the master of the universe. State flows down, events flow up. To question this is to question physics, which is how you got yourself to this point in the first place.

This app is not a custom `View` with a `Canvas` and a prayer — it's a strict Model-View-Intent (MVI)
app, unidirectional data flow all the way down. State flows down, events flow up, and the moment
someone tries to sneak business logic into a Composable, physics itself should intervene.

1.  **Camera Preview:** Uses CameraX (and, for the AR flow, ARCore) to display a live feed from the device camera.
2.  **Sensor Input:** Leverages the `TYPE_ROTATION_VECTOR` sensor to determine the phone's pitch, roll, and yaw. The pitch is primarily used to tilt the 2D protractor plane. An offset is applied to account for natural phone holding angles.
3.  **MVI Pipeline:** Every user interaction becomes a `MainScreenEvent`, sent up to `MainViewModel`. The `StateReducer` (and its sub-reducers, one per concern — gestures, controls, CV, etc.) is the *only* thing allowed to touch `CueDetatState`, producing a new immutable state as a pure function of the old one. `UpdateStateUseCase` then runs on that new state to derive everything downstream of it — perspective matrices, aiming lines, bank shots, spin paths — before the final state is emitted from a `StateFlow` and the UI redraws itself as a pure function of it.
4.  **Rendering (Compose, not a custom `View`):** `ProtractorOverlay` is a Compose `Canvas` that drives `OverlayRenderer.draw` on every recomposition, delegating to sub-renderers (`TableRenderer`, `RailRenderer`, `BallRenderer`, `LineRenderer`, …) in strict z-order.
    * **Protractor Plane:** A logical 2D plane is defined. Circles representing the cue and target ball positions, protractor angle lines, and deflection lines are drawn on this plane.
    * **3D Projection (Simplified):** `UpdateStateUseCase` builds the projection with `android.graphics.Camera` — a `worldMatrix` handles 2D zoom, then a `perspectiveMatrix` applies table rotation (Y-axis) followed by device pitch/tilt (X-axis), assembled into the final `pitchMatrix` the renderer uses every frame.
    * **Ghost Balls:** Screen-space circles are drawn to represent the "3D" position of the cue and target balls. Their Y-offset from the projected plane centers is scaled by the sine of the pitch angle (raised to a power for a more pronounced effect) to simulate them floating above the plane.
    * **Helper Text:** Text labels are drawn either on the (lifted) protractor plane or directly in screen space, with basic collision avoidance and dynamic sizing.
5.  **Gesture Handling:** A single Compose `pointerInput` modifier, `view/gestures/GestureHandler.kt`, handles everything — single-finger drag (move objects / pan), two-finger pinch-to-zoom, and two-finger rotation — and turns raw touches into the same `MainScreenEvent`s as everything else. No `ScaleGestureDetector`, no raw `MotionEvent` plumbing.
6.  **Theming:** Uses Jetpack Compose Material 3 theming throughout; derived color values feed a `PaintCache` of pre-configured `Paint` objects for the `Canvas` drawing calls.

## Known Quirks & Future Delusions

* **A Virtual Table for Virtually Useful Bank Shot Projection:** Using more sophisticated dynamic layout involving a line drawing of a billiards table will come someday.
* **True 3D Rendering:** This app fakes 3D with 2D canvas tricks. Moving to OpenGL ES or a 3D engine like Filament would allow for actual 3D models and lighting, but would also drastically increase complexity. And probably anxiety. But probably not usefulness.
* **Ball, Table and Pocket Detection:** Partially real, believe it or not. Pocket/table detection uses a merged TFLite (YOLOv8n) model, `MergedTFLiteDetector`, with a Hough-circle fallback if the model asset isn't available. Table scanning accumulates observations across frames, fits a 2:1 geometry model, and builds a TPS warp map for lens-distortion correction. Ball detection runs via ML Kit for coarse region proposals, then `CvBallDetector` refines each region with a felt-mask-subtraction + connected-components pipeline, finished off with `HoughCircles` for sub-pixel centering — no contour detection involved. What remains fantasy: doing all of this reliably on a $12 phone held by someone who has had three beers.
* **Insulting Warnings:** The pool of sarcastic remarks is finite. Contributions welcome if they tickle me the required level of pink.
* **Performance:** Drawing many complex paths and text elements on every frame can be demanding. Optimizations are an ongoing battle. And yet, somehow, it feels more like a you-problem.

## Building & Releasing

Two distribution channels share one codebase via product flavors:

* **`play`** → Google Play, shipped as a **signed Android App Bundle (AAB)**.
* **`foss`** → standalone APK on GitHub Releases.

Quick local builds (`versionCode` = git commit count, kept monotonic for Play):

```bash
./gradlew bundlePlayRelease  -PversionBuild=$(git rev-list --count HEAD)   # signed Play AAB
./gradlew assembleFossRelease -PversionBuild=$(git rev-list --count HEAD)  # signed FOSS APK
```

Publishing to Play is automated by the **“Play Publish (AAB)”** GitHub Actions
workflow (`workflow_dispatch`): inputs `track` (default `internal`), `status`
(default `draft`), and `publish` (default `false` = upload the `.aab` artifact
only). The ~24 MB TFLite model is delivered to Play as an **on-demand dynamic
feature module** (`:feature_mlmodel`) and bundled directly into the FOSS APK.

Required repo secrets: `KEYSTORE_PRIVATE`, `KEYSTORE_CHAIN`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD` (signing) and `PLAY_SERVICE_ACCOUNT_JSON` (Play
publishing). **Full details, one-time Play Console setup, and the Data-safety
checklist are in [`docs/RELEASE.md`](docs/RELEASE.md).**

## License

Distributed under the MIT License. Basically, completely free to use however you'd like, just gimme a shoutout. I make money making art. So, like this:
Cue D’état by HereLiesAz (https://instagram.com/hereliesaz)

## Acknowledgments (of Who to Blame)

* The ghosts of billiards past whose missed shots inspired all this.
* The people I've tried to teach all these things.
* Physics. And geometry. Where my hoes at?! Pythagoras! Decartes! Newton, you bish!
