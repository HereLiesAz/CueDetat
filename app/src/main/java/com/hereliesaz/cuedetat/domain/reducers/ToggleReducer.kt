// app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ToggleReducer.kt
package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.state.DistanceUnit

internal fun reduceToggleAction(
    state: CueDetatState,
    action: MainScreenEvent,
    reducerUtils: ReducerUtils
): CueDetatState {
    return when (action) {
        is MainScreenEvent.ToggleMenu -> {
            // When toggling the nav rail, always ensure the expanded menu is closed.
            state.copy(
                isMenuVisible = !state.isMenuVisible,
                isExpandedMenuVisible = false
            )
        }

        is MainScreenEvent.ToggleExpandedMenu -> {
            // When toggling the expanded menu, always ensure the nav rail is closed.
            state.copy(
                isExpandedMenuVisible = !state.isExpandedMenuVisible,
                isMenuVisible = false
            )
        }

        is MainScreenEvent.ToggleNavigationRail -> {
            state.copy(
                isNavigationRailExpanded = !state.isNavigationRailExpanded,
                isMenuVisible = false,
                isExpandedMenuVisible = false
            )
        }
        is MainScreenEvent.ToggleSpinControl -> state.copy(isSpinControlVisible = !state.isSpinControlVisible)
        is MainScreenEvent.ToggleBankingMode -> handleToggleBankingMode(state, reducerUtils)
        is MainScreenEvent.CycleTableSize -> {
            val newState = state.copy(
                table = state.table.copy(size = state.table.size.next()),
                valuesChangedSinceReset = true
            )
            reducerUtils.snapViolatingBalls(newState)
        }

        is MainScreenEvent.SetTableSize -> {
            val newState = state.copy(
                table = state.table.copy(size = action.size),
                valuesChangedSinceReset = true
            )
            reducerUtils.snapViolatingBalls(newState)
        }

        is MainScreenEvent.ToggleTableSizeDialog -> state.copy(showTableSizeDialog = !state.showTableSizeDialog)
        is MainScreenEvent.ToggleForceTheme -> {
            val newMode = when (state.isForceLightMode) {
                null -> true; true -> false; false -> null
            }
            state.copy(isForceLightMode = newMode, valuesChangedSinceReset = true)
        }

        is MainScreenEvent.ToggleCamera -> state.copy(isCameraVisible = !state.isCameraVisible)
        is MainScreenEvent.ToggleDistanceUnit -> state.copy(
            distanceUnit = if (state.distanceUnit == DistanceUnit.METRIC) DistanceUnit.IMPERIAL else DistanceUnit.METRIC,
            valuesChangedSinceReset = true
        )

        is MainScreenEvent.ToggleLuminanceDialog -> state.copy(showLuminanceDialog = !state.showLuminanceDialog)
        is MainScreenEvent.ToggleGlowStickDialog -> state.copy(showGlowStickDialog = !state.showGlowStickDialog)
        is MainScreenEvent.ToggleHelp -> state.copy(areHelpersVisible = !state.areHelpersVisible)
        is MainScreenEvent.ToggleMoreHelp -> state.copy(isMoreHelpVisible = !state.isMoreHelpVisible)
        is MainScreenEvent.ToggleSnapping -> state.copy(isSnappingEnabled = !state.isSnappingEnabled)
        is MainScreenEvent.ToggleCvModel -> state.copy(useCustomModel = !state.useCustomModel)
        is MainScreenEvent.ToggleOrientationLock -> {
            val current = state.pendingOrientationLock ?: state.orientationLock
            state.copy(pendingOrientationLock = current.next())
        }

        is MainScreenEvent.ApplyPendingOrientationLock -> {
            if (state.pendingOrientationLock == null) return state
            return state.copy(
                orientationLock = state.pendingOrientationLock,
                pendingOrientationLock = null
            )
        }

        is MainScreenEvent.OrientationChanged -> state.copy(orientationLock = action.orientationLock)
        is MainScreenEvent.SetExperienceMode -> handleSetExperienceMode(
            state,
            action.mode,
            reducerUtils
        )

        is MainScreenEvent.UnlockBeginnerView -> state.copy(isBeginnerViewLocked = false)
        is MainScreenEvent.LockBeginnerView -> {
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

        is MainScreenEvent.ToggleCalibrationScreen -> state.copy(showCalibrationScreen = !state.showCalibrationScreen)
        is MainScreenEvent.ToggleQuickAlignScreen -> state.copy(showQuickAlignScreen = !state.showQuickAlignScreen)
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
        val defaultTableRotation = 90f
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