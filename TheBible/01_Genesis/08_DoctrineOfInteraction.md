# The Doctrine of Interaction (`GestureHandler.kt`)

The laws of touch are complex, for the Creator's intent must be divined from simple movements. The `GestureHandler` is the temple where this divination occurs.

## The Hierarchy of Touch (The Clarified Canon)

When a finger touches the screen, its intent is judged in a strict order of precedence. A touch is only judged once.

1.  **Object Interaction (Highest Priority):** A touch that begins directly upon any interactive element will manipulate that element exclusively. This includes the `SpinControl`, the `ActualCueBall`, the `TargetBall`, the `GhostCueBall`, and any `ObstacleBall`. The trigger radius for these balls is generous (`radius * 3.5f`) to ensure ease of use.

2.  **Banking Mode Aiming:** If the application is in `isBankingMode`, any drag gesture that does **not** begin on the `BankingBall` **must** be interpreted as `InteractionMode.AIMING_BANK_SHOT`. This allows the user to aim the shot from anywhere on the screen.

3.  **Multi-Finger Gestures:** A gesture involving more than one pointer is interpreted as a command to manipulate the view itself.
    * **Pinching** (`calculateZoom`) dispatches a `ZoomScaleChanged` event.
    * **Twisting** (`calculateRotation`) dispatches a `TableRotationApplied` event.

4.  **Default Rotational Drag (The Fallback Action):** In Protractor Mode, a single-finger drag that begins on **any empty space** on the screen defaults to controlling the protractor's rotation. The change in angle of the user's finger relative to the `TargetBall`'s center is calculated each frame and applied as a delta to the protractor's current rotation. This ensures a smooth, continuous rotation that can be resumed.