# 3.1. Gesture Interaction Model (`GestureHandler.kt`)

User touch input is handled by a custom gesture detector that prioritizes actions in a specific
order.

## Interaction Hierarchy

When a touch gesture begins, its intent is determined by the following order of precedence:

1. **Multi-Finger Gestures (Highest Priority):** A gesture with more than one pointer manipulates
   the view itself:

    * **Pinching** (`calculateZoom`) dispatches a `ZoomScaleChanged` event. With ARCore world
      tracking (`ArProjectionPolicy.isWorldTracked`) it dispatches `TableScaleChanged` instead:
      the AR table has a metric size, zoom there moves it toward or away from the camera, and the
      pinch resizes it in place (`tableFitScale`, 0.6–1.6× the selected table; reset by Reset).
      The zoom slider keeps the range.
    * **A tap while a ball pick is armed** (the rail's Cue / Target buttons, or first-run setup)
      places that ball (`ArSurfaceTapped`, on release). While a pick is armed, touch-down does
      nothing else.
    * **Three fingers** dragging vertically move the table's height (`MoveTableZ`), but only when
      all three move the same way (`isCoherentVerticalDrag`); a resting thumb doesn't count.
    * **Intent**: a pinch never lands perfectly, so the twist and drift that ride along are ignored
      until they pass a threshold (about 3° of accumulated rotation, 16 px of accumulated pan;
      `TwoFingerIntent`). Each channel opens independently and stays open for the rest of the gesture.
    * **Twisting** (`calculateRotation`) dispatches a `TableRotationApplied` event. Positive is
      clockwise on screen; the AR session converts it with
      `TableFrameHomography.clockwiseToWorldYaw` so the table turns with the fingers.

2. **Object Interaction:** A touch that begins directly upon an interactive
   logical element will manipulate that element exclusively. This includes the `SpinControl` (which uses a double-tap-to-drag relocation gesture), `MasseControl` (also uses double-tap-to-drag),
   `ActualCueBall`, `TargetBall`, `GhostCueBall`, and any `ObstacleBall`. The touch target for these
   balls is a generous `LOGICAL_BALL_RADIUS * 8 / zoom` (`GestureReducer`), so a near-miss still picks the ball. With an AR-anchored table the slider zoom is not applied, so the divisor is 1.

3. **Banking Mode Aiming:** If `isBankingMode` is true, any drag gesture that does **not** begin on
   the `BankingBall` is interpreted as `InteractionMode.AIMING_BANK_SHOT`.

4. **Default Rotational Drag (Fallback Action):** In Protractor Mode, a single-finger drag on any
   empty space defaults to controlling the protractor's rotation. This **must** be implemented as a
   **relative rotational drag**. The system calculates the **change in angle** of the user's finger
   relative to the `TargetBall`'s center between pointer events. This angular delta is then applied
   to the protractor's current rotation, providing a smooth, intuitive interaction where the aiming
   apparatus "picks up" and follows the gesture.

## Beginner Mode Gesture Restrictions

In **Beginner Locked** state (`isBeginnerViewLocked == true`): all pan, drag, and world-rotation
gestures are disabled. The only input active is the on-screen zoom slider.

In **Beginner Free Aim** state (`isBeginnerViewLocked == false`): single-finger pan (world pan) and
two-finger pan are suppressed. Ball dragging, rotational aiming, two-finger rotation, and
pinch-to-zoom remain active. The world stays locked; the user can only move elements relative to the
stationary world frame.

### Heresies of Interaction

To prevent reintroduction of past bugs and flawed user experiences, the following interaction models
are explicitly forbidden:

* **Forbidden: Absolute Rotational Drag.** An implementation where the protractor's rotation snaps
  directly to the absolute angle of the user's finger is unacceptable. This behavior is jarring and
  disrupts the user's sense of control, violating the principle of a precision
  instrument.