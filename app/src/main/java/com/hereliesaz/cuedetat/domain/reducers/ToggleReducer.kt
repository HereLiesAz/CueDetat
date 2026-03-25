// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ToggleReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CameraMode
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.state.DistanceUnit

internal fun reduceToggleAction(
    state: CueDetatState,
    action: MainScreenEvent,
    reducerUtils: ReducerUtils
): CueDetatState {
    return when (action) {
        is MainScreenEvent.ToggleSpinControl -> {
            val isNowVisible = !state.isSpinControlVisible
            if (isNowVisible) {
                state.copy(isSpinControlVisible = true)
            } else {
                state.copy(
                    isSpinControlVisible = false,
                    selectedSpinOffset = null,
                    lingeringSpinOffset = null,
                    spinPaths = null
                )
            }
        }
        is MainScreenEvent.ToggleBankingMode -> handleToggleBankingMode(state, reducerUtils)
        is MainScreenEvent.CycleTableSize -> {
            val newState = state.copy(table = state.table.copy(size = state.table.size.next()), valuesChangedSinceReset = true)
            reducerUtils.snapViolatingBalls(newState)
        }
        is MainScreenEvent.SetTableSize -> {
            val newState = state.copy(table = state.table.copy(size = action.size), valuesChangedSinceReset = true)
            reducerUtils.snapViolatingBalls(newState)
        }
        is MainScreenEvent.ToggleTableSizeDialog -> state.copy(showTableSizeDialog = !state.showTableSizeDialog)
        is MainScreenEvent.ToggleForceTheme -> {
            val newMode = when (state.isForceLightMode) { null -> true; true -> false; false -> null }
            state.copy(isForceLightMode = newMode, valuesChangedSinceReset = true)
        }
        is MainScreenEvent.CycleCameraMode -> state.copy(
            cameraMode = if (state.cameraMode == CameraMode.AR_ACTIVE) CameraMode.OFF else CameraMode.AR_ACTIVE
        )
        is MainScreenEvent.ToggleDistanceUnit -> state.copy(distanceUnit = if (state.distanceUnit == DistanceUnit.METRIC) DistanceUnit.IMPERIAL else DistanceUnit.METRIC, valuesChangedSinceReset = true)
        is MainScreenEvent.ToggleLuminanceDialog -> state.copy(showLuminanceDialog = !state.showLuminanceDialog)
        is MainScreenEvent.ToggleGlowStickDialog -> state.copy(showGlowStickDialog = !state.showGlowStickDialog)
        is MainScreenEvent.ToggleHelp -> state.copy(areHelpersVisible = !state.areHelpersVisible)
        is MainScreenEvent.ToggleSnapping -> state.copy(isSnappingEnabled = !state.isSnappingEnabled)
        is MainScreenEvent.ToggleCvModel -> state.copy(useCustomModel = !state.useCustomModel)
        is MainScreenEvent.ToggleOrientationLock -> {
            val current = state.pendingOrientationLock ?: state.orientationLock
            state.copy(pendingOrientationLock = current.next())
        }
        is MainScreenEvent.ApplyPendingOrientationLock -> {
            if (state.pendingOrientationLock == null) return state
            state.copy(orientationLock = state.pendingOrientationLock, pendingOrientationLock = null)
        }
        is MainScreenEvent.OrientationChanged -> state.copy(orientationLock = action.orientationLock)
        is MainScreenEvent.SetExperienceMode -> handleSetExperienceMode(state, action.mode, reducerUtils)
        is MainScreenEvent.ApplyPendingExperienceMode -> {
            if (state.pendingExperienceMode == null) return state
            return handleSetExperienceMode(state, state.pendingExperienceMode, reducerUtils).copy(pendingExperienceMode = null)
        }
        is MainScreenEvent.UnlockBeginnerView -> state.copy(isBeginnerViewLocked = false)
        is MainScreenEvent.LockBeginnerView -> {

            // DYNAMIC DEVICE-AGNOSTIC ZOOM: Calculate slider value to fit within screen width minus 200dp total margin
            var autoZoomSlider = 50f
            state.logicalPlaneMatrix?.let { mat ->
                val logicalWidth = 4f * LOGICAL_BALL_RADIUS
                val pts = floatArrayOf(0f, 0f, logicalWidth, 0f)
                mat.mapPoints(pts)

                // Get current pixel width at current zoom
                val currentPx = kotlin.math.hypot((pts[2] - pts[0]).toDouble(), (pts[3] - pts[1]).toDouble()).toFloat()

                if (currentPx > 0 && state.viewWidth > 0) {
                    val (currMin, currMax) = ZoomMapping.getZoomRange(state.experienceMode, false)
                    val currentZoom = ZoomMapping.sliderToZoom(state.zoomSliderPosition, currMin, currMax)

                    // Target margin: 100dp padding on each side = 200dp total
                    val marginPx = 200f * state.screenDensity
                    val targetPx = state.viewWidth - marginPx

                    if (targetPx > 0) {
                        val unzoomedPx = currentPx / currentZoom
                        val targetZoom = targetPx / unzoomedPx

                        val (newMin, newMax) = ZoomMapping.getZoomRange(ExperienceMode.BEGINNER, true)
                        autoZoomSlider = ZoomMapping.zoomToSlider(targetZoom, newMin, newMax)
                    }
                }
            }

            state.copy(
                isBeginnerViewLocked = true,
                areHelpersVisible = true, // Force help on
                cameraMode = if (state.cameraMode == CameraMode.OFF) CameraMode.CAMERA else state.cameraMode, // Force camera on
                protractorUnit = ProtractorUnit(reducerUtils.getDefaultTargetBallPosition(), LOGICAL_BALL_RADIUS, 0f),
                onPlaneBall = null,
                obstacleBalls = emptyList(),
                zoomSliderPosition = autoZoomSlider,
                viewOffset = PointF(0f, 0f),
                worldRotationDegrees = 0f,
                valuesChangedSinceReset = false
            )
        }
        is MainScreenEvent.ToggleCalibrationScreen -> state.copy(showCalibrationScreen = !state.showCalibrationScreen)
        is MainScreenEvent.ToggleTableScanScreen -> state.copy(showTableScanScreen = !state.showTableScanScreen)
        is MainScreenEvent.ExitToSplash -> state.copy(experienceMode = null)
        else -> state
    }
}

private fun handleSetExperienceMode(
    state: CueDetatState,
    mode: ExperienceMode,
    reducerUtils: ReducerUtils
): CueDetatState {
    val newState = state.copy(
        experienceMode = mode,
        protractorUnit = ProtractorUnit(reducerUtils.getDefaultTargetBallPosition(), LOGICAL_BALL_RADIUS, 0f),
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
                onPlaneBall = OnPlaneBall(center = reducerUtils.getDefaultCueBallPosition(newState), radius = LOGICAL_BALL_RADIUS),
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
                cameraMode = if (state.cameraMode == CameraMode.OFF) CameraMode.CAMERA else state.cameraMode,
                zoomSliderPosition = 0f
            )
        }
        ExperienceMode.HATER -> newState
    }
}

private fun handleToggleBankingMode(
    state: CueDetatState,
    reducerUtils: ReducerUtils
): CueDetatState {
    val bankingEnabled = !state.isBankingMode

    val newState = if (bankingEnabled) {
        val newBankingBall = OnPlaneBall(center = state.onPlaneBall?.center ?: PointF(0f, 0f), radius = LOGICAL_BALL_RADIUS)
        val initialAimTarget = calculateInitialBankingAimTarget(newBankingBall, state.worldRotationDegrees)

        state.copy(
            isBankingMode = true,
            onPlaneBall = newBankingBall,
            zoomSliderPosition = 0f,
            table = state.table.copy(isVisible = true),
            bankingAimTarget = initialAimTarget,
            protractorUnit = state.protractorUnit.copy(radius = LOGICAL_BALL_RADIUS),
            warningText = null
        )
    } else {
        state.copy(
            isBankingMode = false,
            bankingAimTarget = null,
            zoomSliderPosition = 0f,
            table = state.table.copy(isVisible = state.experienceMode == ExperienceMode.EXPERT),
            onPlaneBall = OnPlaneBall(state.onPlaneBall?.center ?: reducerUtils.getDefaultCueBallPosition(state), LOGICAL_BALL_RADIUS),
            protractorUnit = state.protractorUnit.copy(radius = LOGICAL_BALL_RADIUS),
            warningText = null
        )
    }

    return reducerUtils.snapViolatingBalls(newState.copy(valuesChangedSinceReset = true, showLuminanceDialog = false, showTutorialOverlay = false, viewOffset = PointF(0f, 0f)))
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