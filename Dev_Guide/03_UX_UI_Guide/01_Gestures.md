# 3.1. Gesture Interaction Model (`GestureHandler.kt`)

User touch input is handled by a custom gesture detector that prioritizes actions in a specific
order.

## Interaction Hierarchy

When a touch gesture begins, its intent is determined by the following order of precedence:

1. **Object Interaction (Highest Priority):** A touch that begins directly upon an interactive
   logical element will manipulate that element exclusively. This includes the `SpinControl`,
   `ActualCueBall`, `TargetBall`, `GhostCueBall`, and any `ObstacleBall`. The touch target for these
   balls is a generous `radius * 3.5f`.

2. **Banking Mode Aiming:** If `isBankingMode` is true, any drag gesture that does **not** begin on
   the `BankingBall` is interpreted as `InteractionMode.AIMING_BANK_SHOT`.

3. **Multi-Finger Gestures:** A gesture with more than one pointer manipulates the view itself:
    * **Pinching** (`calculateZoom`) dispatches a `ZoomScaleChanged` event.
    * **Twisting** (`calculateRotation`) dispatches a `TableRotationApplied` event.

4. **Default Rotational Drag (Fallback Action):** In Protractor Mode, a single-finger drag on any
   empty space defaults to controlling the protractor's rotation. The system calculates the absolute
   angle of the user's finger relative to the `TargetBall`'s center and sets the protractor's
   rotation directly to that angle.