# 3.1. Gesture Interaction Model (`GestureHandler.kt`)

User touch input is handled by a custom gesture detector that prioritizes actions in a specific
order.

## Interaction Hierarchy

When a touch gesture begins, its intent is determined by the following order of precedence:

1. **Multi-Finger Gestures (Highest Priority):** A gesture with more than one pointer manipulates
   the view itself:

    * **Pinching** (`calculateZoom`) dispatches a `ZoomScaleChanged` event.
    * **Twisting** (`calculateRotation`) dispatches a `TableRotationApplied` event.

2. **Object Interaction:** A touch that begins directly upon an interactive
   logical element will manipulate that element exclusively. This includes the `SpinControl`,
   `ActualCueBall`, `TargetBall`, `GhostCueBall`, and any `ObstacleBall`. The touch target for these
   balls is a generous `radius * 3.5f`.

3. **Banking Mode Aiming:** If `isBankingMode` is true, any drag gesture that does **not** begin on
   the `BankingBall` is interpreted as `InteractionMode.AIMING_BANK_SHOT`.

4. **Default Rotational Drag (Fallback Action):** In Protractor Mode, a single-finger drag on any
   empty space defaults to controlling the protractor's rotation. This **must** be implemented as a
   **relative rotational drag**. The system calculates the **change in angle** of the user's finger
   relative to the `TargetBall`'s center between pointer events. This angular delta is then applied
   to the protractor's current rotation, providing a smooth, intuitive interaction where the aiming
   apparatus "picks up" and follows the gesture.

### Heresies of Interaction

To prevent reintroduction of past bugs and flawed user experiences, the following interaction models
are explicitly forbidden:

* **Forbidden: Absolute Rotational Drag.** An implementation where the protractor's rotation snaps
  directly to the absolute angle of the user's finger is unacceptable. This behavior is jarring and
  disrupts the user's sense of control, violating the principle of a precision
  instrument.