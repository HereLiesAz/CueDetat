# 4.1. Feature Specification: Protractor Mode

This document specifies the behavior of the primary shot visualization mode.

## Core Components

* **`TargetBall`**: The logical anchor of the system, representing the object ball.
* **`GhostCueBall`**: A calculated logical ball showing the required impact point on the
  `TargetBall`. Its position is determined by the `rotationDegrees` of the `ProtractorUnit`.
* **`ActualCueBall`**: An optional, user-draggable logical ball representing the real-world position
  of the cue ball. It provides the origin for the **Shot Guide Line**.

## Functional Visualization

1. The user aligns the **`TargetBall`** over the real-world object ball.
2. The user performs a **relative rotational drag** gesture on an empty part of the screen to aim
   the **Aiming Line**. This gesture controls the `rotationDegrees` of the `ProtractorUnit` by
   applying the angular change of the drag relative to the `TargetBall`.
3. If the **`ActualCueBall`** is enabled, the user drags it to match the real-world cue ball.
4. The rendered **Shot Line** now displays the exact path the user must shoot.
5. The **Tangent Lines** show the path the cue ball will take after impact if no spin is applied.
6. The **Spin Control** can be used to visualize post-contact trajectories with english.

## Massé Sub-Mode Visualization

* **Interaction**: The `ROTATING_PROTRACTOR` gesture is repurposed to rotate the Massé shot's
  direction of origin around the cue ball.
* **Conditional Visibility**:
    * Shot, Aiming, and Tangent lines are **hidden** by default.
    * They only appear if the Massé ghost ball impacts the target ball.
    * Upon impact, the ghost ball is rendered adjacent to the target ball (edge-to-edge).
    * An aiming line is then drawn from the ghost ball center through the target ball center
      toward the rails.
* **Suppression**: Protractor guides and rail labels (diamond numbers) are suppressed.


**Note on Forbidden Mechanics:** Any implementation interpreting the aiming gesture as a "direct
linear drag" (i.e., making the aiming line point *at* the user's finger) is incorrect. This model is
ambiguous and provides a poor, imprecise user experience.

## Aiming Calculations in Expert Mode (MATRICES_ONLY Path)

In Expert Mode, certain state updates use a `MATRICES_ONLY` update type to avoid full recalculation.
`updateAimingCalculations` must be called **before** `updateSpinCalculations` in this path, not
skipped. Skipping it leaves `@Transient` aiming fields (`tangentAimedPocketIndex`, `aimedPocketIndex`,
etc.) stale, which causes the Expert Mode red aiming line to stop updating until a full recalculation
is triggered. The `MATRICES_ONLY` path now correctly runs `updateAimingCalculations` first.

## Balls Stay On The Table

* With a visible table, the target ball, the cue ball and every obstacle ball are kept wholly on it:
  a ball's centre may come no closer to a rail than the ball's own radius (`Table.clampBallCenter`).
  A ball dragged past a rail slides along it and stops; it is never reset or removed for this.
* **The ghost cue ball** is derived (target centre + 2r at the aim angle), so it is kept on the table
  by moving the **aim**: to the nearest angle at which the ghost fits (`BallBounds.aimWithGhostOnTable`).
  Aiming into a rail, or dragging the target towards one, makes the ghost slide along the rail and the
  aim line follow; the ghost never leaves the table. The aim keeps its accumulated winding.
  Not applied in banking mode (no ghost) or massé mode (the ghost is the massé simulation's, derived
  after the reducers run, and is not covered).
* Enforced in one place, `BallBounds.confine`, where state leaves `stateReducer` and where
  `SnapReducer.reduce` returns, so every writer is covered: the three drag modes, tap-to-place,
  following a detected ball, undo and reset.
* With no visible table (infinite space) there are no bounds.
* `isGeometricallyImpossible` is unrelated: it concerns the shot line, ghost and target, and does
  not look at the table.
* Table resize still resets a target or cue ball whose centre is outside, and removes such
  obstacles (`ReducerUtils.snapViolatingBalls`); the confinement then slides any ball that is only
  partly over a rail.
