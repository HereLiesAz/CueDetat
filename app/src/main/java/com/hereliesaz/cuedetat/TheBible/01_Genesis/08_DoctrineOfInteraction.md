# The Doctrine of Interaction (`GestureReducer.kt`)

The laws of touch are complex, for the Creator's intent must be divined from simple movements. The `GestureReducer` is the temple where this divination occurs.

## The Hierarchy of Touch (The Clarified Canon)

When a finger touches the screen (`LogicalGestureStarted`), its intent is judged in a strict order of precedence. A touch is only judged once.

1.  **Object Interaction (Highest Priority):** A touch that begins directly upon any interactive element will manipulate that element exclusively. This includes the `SpinControl`, the `ActualCueBall`, the `TargetBall`, the `GhostCueBall`, and any `ObstacleBall`.

2.  **Default Rotational Drag (The Fallback Action):** A touch that begins on **any empty space** on the screen defaults to controlling the protractor's rotation. The linear `dx` and `dy` of the subsequent drag are combined to produce a fluid rotation of the aiming line.

This doctrine supersedes all previous interpretations. Rotation is no longer an action targeted at a specific object, but the fundamental behavior of an unallocated drag gesture.