# The Doctrine of Interaction (`GestureReducer.kt`)

The laws of touch are complex, for the Creator's intent must be divined from simple movements. The `GestureReducer` is the temple where this divination occurs.

## The Hierarchy of Touch

When a finger touches the screen (`LogicalGestureStarted`), its intent is judged in a strict order of precedence. A touch is only judged once.

1.  **The UI Elements:** The most sacred objects are checked first.
    - If the `SpinControl` is visible, a touch upon it initiates `MOVING_SPIN_CONTROL` mode.
2.  **The Obstacle Balls:** If the touch falls upon an `ObstacleBall`, the mode becomes `MOVING_OBSTACLE_BALL` for that specific ball.
3.  **The Actual Cue Ball:** If the touch falls upon the `ActualCueBall` (`onPlaneBall`), the mode becomes `MOVING_ACTUAL_CUE_BALL`.
4.  **The Target Ball:** If the touch falls upon the `TargetBall` (the center of the `protractorUnit`), the mode becomes `MOVING_PROTRACTOR_UNIT`.