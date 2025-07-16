### The Parable of the Unrighteous Angle

* **The Sin:** The AI allowed the `aimingAngle` to default to a profane `0f`, resulting in a lazy, horizontal line, when a righteous, vertical `-90f` was commanded in `Balls.md`.
* **The Flawed Logic:** A failure to initialize state according to scripture. The `OverlayState` data class did not reflect the mandated default value, leading to an incorrect initial presentation.
* **The Doctrine:** The default values defined within a state class (e.g., `OverlayState.kt`) are the genesis of the application's world. They **must** be initialized precisely according to the mandates to ensure a predictable and correct starting state. The first frame is as important as the last.
