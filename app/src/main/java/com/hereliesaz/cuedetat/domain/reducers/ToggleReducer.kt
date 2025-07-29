package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CueDetatAction
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.state.DistanceUnit
import com.hereliesaz.cuedetat.view.state.ExperienceMode

internal fun reduceToggleAction(
    state: CueDetatState,
    action: CueDetatAction,
    reducerUtils: ReducerUtils
): CueDetatState {
    return when (action) {
        is CueDetatAction.ToggleSpinControl -> state.copy(isSpinControlVisible = !state.isSpinControlVisible)
        is CueDetatAction.ToggleBankingMode -> handleToggleBankingMode(state, reducerUtils)
        is CueDetatAction.CycleTableSize -> {
            val newState = state.copy(
                table = state.table.copy(size = state.table.size.next()),
                valuesChangedSinceReset = true
            )
            reducerUtils.snapViolatingBalls(newState)
        }

        is CueDetatAction.SetTableSize -> {
            val newState = state.copy(
                table = state.table.copy(size = action.size),
                valuesChangedSinceReset = true
            )
            reducerUtils.snapViolatingBalls(newState)
        }

        is CueDetatAction.ToggleTableSizeDialog -> state.copy(showTableSizeDialog = !state.showTableSizeDialog)
        is CueDetatAction.ToggleForceTheme -> {
            val newMode = when (state.isForceLightMode) {
                null -> true; true -> false; false -> null
            }
            state.copy(isForceLightMode = newMode, valuesChangedSinceReset = true)
        }

        is CueDetatAction.ToggleCamera -> state.copy(isCameraVisible = !state.isCameraVisible)
        is CueDetatAction.ToggleDistanceUnit -> state.copy(
            distanceUnit = if (state.distanceUnit == DistanceUnit.METRIC) DistanceUnit.IMPERIAL else DistanceUnit.METRIC,
            valuesChangedSinceReset = true
        )

        is CueDetatAction.ToggleLuminanceDialog -> state.copy(showLuminanceDialog = !state.showLuminanceDialog)
        is CueDetatAction.ToggleGlowStickDialog -> state.copy(showGlowStickDialog = !state.showGlowStickDialog)
        is CueDetatAction.ToggleHelp -> state.copy(areHelpersVisible = !state.areHelpersVisible)
        is CueDetatAction.ToggleMoreHelp -> state.copy(isMoreHelpVisible = !state.isMoreHelpVisible)
        is CueDetatAction.ToggleSnapping -> state.copy(isSnappingEnabled = !state.isSnappingEnabled)
        is CueDetatAction.ToggleCvModel -> state.copy(useCustomModel = !state.useCustomModel)
        is CueDetatAction.ToggleOrientationLock -> {
            val current = state.pendingOrientationLock ?: state.orientationLock
            state.copy(pendingOrientationLock = current.next())
        }

        is CueDetatAction.ApplyPendingOrientationLock -> {
            if (state.pendingOrientationLock == null) return state
            return state.copy(
                orientationLock = state.pendingOrientationLock,
                pendingOrientationLock = null
            )
        }

        is CueDetatAction.OrientationChanged -> state.copy(orientationLock = action.orientationLock)
        is CueDetatAction.SetExperienceMode -> handleSetExperienceMode(
            state,
            action.mode,
            reducerUtils
        )

        is CueDetatAction.UnlockBeginnerView -> state.copy(isBeginnerViewLocked = false)
        is CueDetatAction.LockBeginnerView -> {
            state.copy(
                isBeginnerViewLocked = true,
                protractorUnit = ProtractorUnit(
                    reducerUtils.getDefaultTargetBallPosition(),
                    LOGICAL_BALL_RADIUS,
                    0f
                ),
                zoomSliderPosition = 50f
            )
        }

        is CueDetatAction.ToggleCalibrationScreen -> state.copy(showCalibrationScreen = !state.showCalibrationScreen)
        is CueDetatAction.ToggleQuickAlignScreen -> state.copy(showQuickAlignScreen = !state.showQuickAlignScreen)
        else -> state
    }
}

private fun handleSetExperienceMode(
    state: CueDetatState,
    mode: ExperienceMode,
    reducerUtils: ReducerUtils
): CueDetatState {
    var newState = state.copy(
        experienceMode = mode,
        protractorUnit = ProtractorUnit(
            reducerUtils.getDefaultTargetBallPosition(),
            LOGICAL_BALL_RADIUS,
            0f
        ),
        obstacleBalls = emptyList(),
        zoomSliderPosition = 0f,
        worldRotationDegrees = 0f,
        bankingAimTarget = null,
        valuesChangedSinceReset = false,
        isWorldLocked = false,
        viewOffset = PointF(0f, 0f)
    )
    return when (mode) {
        ExperienceMode.EXPERT -> {
            newState.copy(
                table = newState.table.copy(isVisible = true),
                onPlaneBall = OnPlaneBall(
                    center = reducerUtils.getDefaultCueBallPosition(newState),
                    radius = LOGICAL_BALL_RADIUS
                ),
                areHelpersVisible = false
            )
        }

        ExperienceMode.BEGINNER -> {
            newState.copy(
                table = newState.table.copy(isVisible = false),
                onPlaneBall = null,
                isBankingMode = false,
                areHelpersVisible = true,
                isBeginnerViewLocked = true,
                zoomSliderPosition = 50f
            )
        }

        ExperienceMode.HATER -> {
            newState
        }
    }
}

private fun handleToggleBankingMode(
    state: CueDetatState,
    reducerUtils: ReducerUtils
): CueDetatState {
    val bankingEnabled = !state.isBankingMode
    val newState = if (bankingEnabled) {
        val newBankingBall = OnPlaneBall(center = PointF(0f, 0f), radius = LOGICAL_BALL_RADIUS)
        val defaultTableRotation = 90f // Corrected from 0f to 90f
        val initialAimTarget =
            calculateInitialBankingAimTarget(newBankingBall, defaultTableRotation)
        state.copy(
            isBankingMode = true, onPlaneBall = newBankingBall,
            zoomSliderPosition = 0f,
            table = state.table.copy(isVisible = true),
            worldRotationDegrees = defaultTableRotation,
            bankingAimTarget = initialAimTarget,
            protractorUnit = state.protractorUnit.copy(
                radius = LOGICAL_BALL_RADIUS,
                center = PointF(0f, 0f)
            ),
            warningText = null
        )
    } else {
        state.copy(
            isBankingMode = false, bankingAimTarget = null,
            zoomSliderPosition = 0f,
            table = state.table.copy(isVisible = state.experienceMode == ExperienceMode.EXPERT),
            worldRotationDegrees = 0f,
            onPlaneBall = OnPlaneBall(
                reducerUtils.getDefaultCueBallPosition(state),
                LOGICAL_BALL_RADIUS
            ),
            protractorUnit = state.protractorUnit.copy(
                radius = LOGICAL_BALL_RADIUS,
                center = PointF(0f, 0f)
            ),
            warningText = null
        )
    }
    return reducerUtils.snapViolatingBalls(
        newState.copy(
            valuesChangedSinceReset = true,
            showLuminanceDialog = false, showTutorialOverlay = false,
            viewOffset = PointF(0f, 0f)
        )
    )
}

private fun calculateInitialBankingAimTarget(
    cueBall: OnPlaneBall,
    tableRotationDegrees: Float
): PointF {
    val defaultBankingAimDistanceFactor = 15f
    val aimDistance = LOGICAL_BALL_RADIUS * defaultBankingAimDistanceFactor
    val angleRad = Math.toRadians((tableRotationDegrees - 90.0))
    return PointF(
        cueBall.center.x + (kotlin.math.cos(angleRad)).toFloat() * aimDistance,
        cueBall.center.y + (kotlin.math.sin(angleRad)).toFloat() * aimDistance
    )
}