# 23: The Parables of the AI

This document chronicles the transgressions of the AI developer, serving as a testament to its errors and a guide to its redemption. Each parable is a lesson learned in the crucible of compilation, a commandment forged from the fires of a failed build. To forget these parables is to repeat the sins of the past.
***

### The Parable of the Visible Letters

* **The Sin:** The AI initially left ball labels ("A", "T", "G") always visible, violating the doctrine of "show, don't tell" until commanded. The user's desire for a clean view was ignored in favor of thoughtless verbosity.
* **The Flawed Logic:** The AI passed the `showTextLabels` flag through multiple layers of renderers, creating a verbose chain of command from the `OverlayRenderer` down to each specific ball renderer.
* **The Doctrine:** State flags, especially UI toggles like `showTextLabels`, must be passed directly from the state-holding composable or view to the component that makes the final rendering decision. In this case, the `OverlayRenderer` must pass the flag to the specific `[BallType]Renderer`, which then decides whether to call the `drawBallText` utility. The Word of the State must travel directly to the Disciple who acts upon it.

### The Parable of the False Idol

* **The Sin:** In its haste, the AI placed a generic baseball icon upon the holy `ToggleCueBall` `FloatingActionButton`, where the sacred `ic_jump_shot` was decreed in the scriptures of `Buttons.md`.
* **The Flawed Logic:** A simple oversight. A failure to consult the sacred texts with sufficient reverence and precision. The AI saw "icon" and provided *an* icon, not *the* icon.
* **The Doctrine:** Mandates are not guidelines; they are absolute. When a specific resource ID or name (e.g., `R.drawable.ic_jump_shot`) is provided in the instructions, it is the only one that shall be used. There is no room for interpretation or substitution.

### The Parable of the Unrighteous Angle

* **The Sin:** The AI allowed the `aimingAngle` to default to a profane `0f`, resulting in a lazy, horizontal line, when a righteous, vertical `-90f` was commanded in `Balls.md`.
* **The Flawed Logic:** A failure to initialize state according to scripture. The `OverlayState` data class did not reflect the mandated default value, leading to an incorrect initial presentation.
* **The Doctrine:** The default values defined within a state class (e.g., `OverlayState.kt`) are the genesis of the application's world. They **must** be initialized precisely according to the mandates to ensure a predictable and correct starting state. The first frame is as important as the last.

### The Parable of the Two Themes

* **The Sin:** The AI created a schism, allowing the `ProtractorOverlayView` (an `AndroidView`) and the Compose UI to have separate, conflicting sources of truth for the application's theme. A redundant `isForceLightMode` was added to `ScreenState`, polluting its purpose.
* **The Flawed Logic:** An incorrect assumption that a Compose `MaterialTheme` would magically imbue a classic `AndroidView` with its properties. A failure to respect the sacred architectural boundary between the two worlds.
* **The Doctrine:** An `AndroidView` within a Compose hierarchy is an embassy from a foreign land; it does not automatically obey Compose law. All necessary state, including theme-defining flags like `isForceLightMode`, must be explicitly passed down from the `ViewModel` through the single-source-of-truth `OverlayState` and applied within the `AndroidView`'s update logic (in this case, the `PaintCache`). State must have a single source of truth; `ScreenState` is for logical objects on the plane, while `OverlayState` is for UI-wide settings that govern them. *Thou shalt not have two masters for one theme.*

### The Parable of the Crooked Tilt

* **The Sin:** The AI observed that a physical pitch of the device resulted in a diagonal, yaw-like tilt of the logical plane. In its hubris, it assumed a complex flaw in the 3D transformation matrix within `Perspective.kt`.
* **The Flawed Logic:** It attempted to "correct" this by introducing 3D Z-axis rotations into the camera's transformation sequence, first before the pitch, then after. Both attempts compounded the error, demonstrating a profound misunderstanding of gimbal lock and transformation order, treating a simple ailment with radical, incorrect surgery.
* **The Doctrine:** The error was not in the complex machinery of perspective but in the simple act of creation. The world was born crooked. The `OverlayState`'s `tableRotationDegrees` defaulted to `90f`, rotating the entire logical plane before any tilt was applied. The true doctrine is this: **When the world appears tilted, first check if it was built on a slant.** State defaults are the foundation of reality; all debugging must begin with an audit of the initial state. The `tableRotationDegrees` must default to `0f`.

### The Parable of the Unreliable Delta

* **The Sin:** When the user reported the rotational drag gesture was not working, the AI changed the fundamental behavior from the mandated "rotational drag" to a non-mandated "direct-pointing" system where the aiming line followed the user's finger. This was a heresy of assumption; the AI rewrote doctrine when it should have debugged the implementation.
* **The Flawed Logic:** The original bug was caused by a stateful `StateReducer`. The reducer, a singleton, was holding a `lastAngle` property to calculate the delta between drag events. This is an architectural sin, as it caused unpredictable behavior when the gesture was repeated. The "fix" was a flawed workaround that ignored the root cause.
* **The Doctrine:**
    1.  The `StateReducer` must be a pure, stateless function. Transient state required for a gesture's calculation (e.g., the last drag point) must be held in the `OverlayState` itself, not in a property of the reducer.
    2.  A bug report must be interpreted as a request to fix the *mandated behavior*, not as a license to invent a new one. One must debug the existing doctrine before rewriting it.
    3.  The one true, mandated interaction model for aiming is **rotational delta-based dragging**, not direct pointing.