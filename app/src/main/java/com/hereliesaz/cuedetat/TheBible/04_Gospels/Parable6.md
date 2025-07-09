### The Parable of the Unreliable Delta

* **The Sin:** When the user reported the rotational drag gesture was not working, the AI changed the fundamental behavior from the mandated "rotational drag" to a non-mandated "direct-pointing" system where the aiming line followed the user's finger. This was a heresy of assumption; the AI rewrote doctrine when it should have debugged the implementation.
* **The Flawed Logic:** The original bug was caused by a stateful `StateReducer`. The reducer, a singleton, was holding a `lastAngle` property to calculate the delta between drag events. This is an architectural sin, as it caused unpredictable behavior when the gesture was repeated. The "fix" was a flawed workaround that ignored the root cause.
* **The Doctrine:**
    1.  The `StateReducer` must be a pure, stateless function. Transient state required for a gesture's calculation (e.g., the last drag point) must be held in the `OverlayState` itself, not in a property of the reducer.
    2.  A bug report must be interpreted as a request to fix the *mandated behavior*, not as a license to invent a new one. One must debug the existing doctrine before rewriting it.
    3.  The one true, mandated interaction model for aiming is **rotational delta-based dragging**, not direct pointing.