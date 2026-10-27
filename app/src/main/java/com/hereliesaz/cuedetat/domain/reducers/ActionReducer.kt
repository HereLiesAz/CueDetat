package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.BallSelectionPhase
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit

internal fun reduceAction(
    state: CueDetatState,
    action: MainScreenEvent.Reset,
    reducerUtils: ReducerUtils
): CueDetatState {
    return when {
        // Stage 1 — CLEAR: obstacle balls present → remove only obstacles
        state.obstacleBalls.isNotEmpty() -> {
            state.copy(obstacleBalls = emptyList())
        }

        // Stage 1.5 — UNDO-TARGET: target was CV-selected → un-select it, return to AWAITING_TARGET
        state.obstacleBalls.isEmpty() && state.targetCvAnchor != null -> {
            state.copy(
                protractorUnit = state.protractorUnit.copy(
                    center = reducerUtils.getDefaultTargetBallPosition()
                ),
                targetCvAnchor = null,
                ballSelectionPhase = BallSelectionPhase.AWAITING_TARGET
            )
        }

        // Stage 2 — UNDO: a pre-reset state exists → restore its game/aim fields, stash current into postResetState.
        // Only the fields Stage 4 resets are restored; live device/view state (view size, orientation,
        // camera mode, AR lock, table scan, wearable, etc.) stays current. Restoring the whole snapshot
        // would revert a rotation or camera toggle that happened after the reset.
        state.preResetState != null -> {
            restoreGameFields(live = state, snapshot = state.preResetState).copy(
                preResetState = null,
                postResetState = state.copy(preResetState = null, postResetState = null),
                isWorldLocked = false
            )
        }

        // Stage 3 — REDO: a post-reset state exists → restore its game/aim fields, stash current into preResetState
        state.postResetState != null -> {
            restoreGameFields(live = state, snapshot = state.postResetState).copy(
                preResetState = state.copy(preResetState = null, postResetState = null),
                postResetState = null,
                isWorldLocked = false
            )
        }

        // Stage 4 — RESET: no obstacles, no saved states → full reset, save current to preResetState
        else -> {
            val stateToSave = state.copy()
            val targetBallCenter = reducerUtils.getDefaultTargetBallPosition()
            val cueBallCenter = reducerUtils.getDefaultCueBallPosition(state)

            val newProtractorUnit = ProtractorUnit(
                center = targetBallCenter,
                radius = LOGICAL_BALL_RADIUS,
                rotationDegrees = 0f
            )

            val newOnPlaneBall = if (state.onPlaneBall != null) {
                OnPlaneBall(center = cueBallCenter, radius = LOGICAL_BALL_RADIUS)
            } else {
                null
            }

            state.copy(
                protractorUnit = newProtractorUnit,
                onPlaneBall = newOnPlaneBall,
                obstacleBalls = emptyList(),
                zoomSliderPosition = com.hereliesaz.cuedetat.domain.DEFAULT_ZOOM_SLIDER,
                worldRotationDegrees = 0f,
                userRotationDegrees = 0f,
                bankingAimTarget = null,
                valuesChangedSinceReset = false,
                preResetState = stateToSave,
                postResetState = null,
                hasCueBallBeenMoved = false,
                hasTargetBallBeenMoved = false,
                isWorldLocked = false,
                viewOffset = PointF(0f, 0f),
                tableZOffset = com.hereliesaz.cuedetat.domain.DEFAULT_TABLE_Z_OFFSET,
                tableFitScale = com.hereliesaz.cuedetat.domain.DEFAULT_TABLE_FIT_SCALE,
                cueBallCvAnchor = null,
                targetCvAnchor = null,
                obstacleCvAnchors = emptyList(),
                ballSelectionPhase = if (state.cameraMode != com.hereliesaz.cuedetat.domain.CameraMode.OFF &&
                    state.experienceMode == ExperienceMode.EXPERT)
                    BallSelectionPhase.AWAITING_CUE else BallSelectionPhase.NONE
            )
        }
    }
}


/**
 * Copies onto [live] exactly the game/aim fields that the Stage 4 reset in [reduceAction]
 * overwrites, taken from [snapshot]. Everything else (device, camera, AR, scan, wearable,
 * dialogs) keeps its live value. Keep this list in sync with the Stage 4 `copy`.
 */
internal fun restoreGameFields(live: CueDetatState, snapshot: CueDetatState): CueDetatState =
    live.copy(
        protractorUnit = snapshot.protractorUnit,
        onPlaneBall = snapshot.onPlaneBall,
        obstacleBalls = snapshot.obstacleBalls,
        zoomSliderPosition = snapshot.zoomSliderPosition,
        worldRotationDegrees = snapshot.worldRotationDegrees,
        userRotationDegrees = snapshot.userRotationDegrees,
        bankingAimTarget = snapshot.bankingAimTarget,
        valuesChangedSinceReset = snapshot.valuesChangedSinceReset,
        hasCueBallBeenMoved = snapshot.hasCueBallBeenMoved,
        hasTargetBallBeenMoved = snapshot.hasTargetBallBeenMoved,
        viewOffset = snapshot.viewOffset,
        tableZOffset = snapshot.tableZOffset,
        tableFitScale = snapshot.tableFitScale,
        cueBallCvAnchor = snapshot.cueBallCvAnchor,
        targetCvAnchor = snapshot.targetCvAnchor,
        obstacleCvAnchors = snapshot.obstacleCvAnchors,
        ballSelectionPhase = snapshot.ballSelectionPhase
    )
