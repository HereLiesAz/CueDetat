// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ToggleReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.state.DistanceUnit
import com.hereliesaz.cuedetat.view.state.ExperienceMode
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToggleReducer @Inject constructor(private val reducerUtils: ReducerUtils) {

    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.ToggleSpinControl -> currentState.copy(isSpinControlVisible = !currentState.isSpinControlVisible)
            is MainScreenEvent.ToggleBankingMode -> handleToggleBankingMode(currentState)
            is MainScreenEvent.CycleTableSize -> {
                val newState = currentState.copy(table = currentState.table.copy(size = currentState.table.size.next()), valuesChangedSinceReset = true)
                reducerUtils.snapViolatingBalls(newState)
            }
            is MainScreenEvent.SetTableSize -> {
                val newState = currentState.copy(table = currentState.table.copy(size = event.size), valuesChangedSinceReset = true)
                reducerUtils.snapViolatingBalls(newState)
            }
            is MainScreenEvent.ToggleTableSizeDialog -> currentState.copy(showTableSizeDialog = !currentState.showTableSizeDialog)
            is MainScreenEvent.ToggleForceTheme -> {
                val newMode = when (currentState.isForceLightMode) { null -> true; true -> false; false -> null }
                currentState.copy(isForceLightMode = newMode, valuesChangedSinceReset = true)
            }
            is MainScreenEvent.ToggleCamera -> currentState.copy(isCameraVisible = !currentState.isCameraVisible)
            is MainScreenEvent.ToggleDistanceUnit -> currentState.copy(
                distanceUnit = if (currentState.distanceUnit == DistanceUnit.METRIC) DistanceUnit.IMPERIAL else DistanceUnit.METRIC,
                valuesChangedSinceReset = true
            )
            is MainScreenEvent.ToggleLuminanceDialog -> currentState.copy(showLuminanceDialog = !currentState.showLuminanceDialog)
            is MainScreenEvent.ToggleGlowStickDialog -> currentState.copy(showGlowStickDialog = !currentState.showGlowStickDialog)
            is MainScreenEvent.ToggleHelp -> currentState.copy(areHelpersVisible = !currentState.areHelpersVisible)
            is MainScreenEvent.ToggleMoreHelp -> currentState.copy(isMoreHelpVisible = !currentState.isMoreHelpVisible)
            is MainScreenEvent.ToggleSnapping -> currentState.copy(isSnappingEnabled = !currentState.isSnappingEnabled)
            is MainScreenEvent.ToggleCvModel -> currentState.copy(useCustomModel = !currentState.useCustomModel)
            is MainScreenEvent.ToggleOrientationLock -> {
                val current = currentState.pendingOrientationLock ?: currentState.orientationLock
                currentState.copy(pendingOrientationLock = current.next())
            }

            is MainScreenEvent.ApplyPendingOrientationLock -> {
                if (currentState.pendingOrientationLock == null) return currentState
                return currentState.copy(
                    orientationLock = currentState.pendingOrientationLock,
                    pendingOrientationLock = null
                )
            }
            is MainScreenEvent.OrientationChanged -> currentState.copy(orientationLock = event.orientationLock)
            is MainScreenEvent.ToggleExperienceMode -> {
                val current = currentState.pendingExperienceMode ?: currentState.experienceMode
                ?: ExperienceMode.EXPERT
                currentState.copy(pendingExperienceMode = current.next())
            }

            is MainScreenEvent.ApplyPendingExperienceMode -> {
                if (currentState.pendingExperienceMode == null) return currentState
                return currentState.copy(
                    experienceMode = currentState.pendingExperienceMode,
                    pendingExperienceMode = null
                )
            }

            is MainScreenEvent.SetExperienceMode -> handleSetExperienceMode(
                currentState,
                event.mode
            )

            is MainScreenEvent.UnlockBeginnerView -> currentState.copy(isBeginnerViewLocked = false)
            is MainScreenEvent.LockBeginnerView -> {
                currentState.copy(
                    isBeginnerViewLocked = true,
                    // Reset to the initial locked state for a consistent experience
                    protractorUnit = ProtractorUnit(
                        reducerUtils.getDefaultTargetBallPosition(),
                        LOGICAL_BALL_RADIUS,
                        0f
                    ),
                    zoomSliderPosition = 50f // Default to max zoom
                )
            }
            is MainScreenEvent.ToggleCalibrationScreen -> currentState.copy(showCalibrationScreen = !currentState.showCalibrationScreen)
            is MainScreenEvent.ToggleQuickAlignScreen -> currentState.copy(showQuickAlignScreen = !currentState.showQuickAlignScreen)
            else -> currentState
        }
    }

    private fun handleSetExperienceMode(
        currentState: OverlayState,
        mode: ExperienceMode
    ): OverlayState {
        // Start with the current state to preserve view dimensions and saved settings.
        var newState = currentState.copy(
            experienceMode = mode,
            // Reset session-specific state to defaults for a clean slate in the new mode.
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

        // Apply mode-specific configurations.
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
                    zoomSliderPosition = 50f // Default to max zoom
                )
            }
            ExperienceMode.HATER -> {
                newState // Hater mode is self-contained.
            }
        }
    }


    private fun handleToggleBankingMode(currentState: OverlayState): OverlayState {
        val bankingEnabled = !currentState.isBankingMode
        val newState = if (bankingEnabled) {
            val bankingZoomSliderPos = 0f
            val bankingBallCenter = PointF(0f, 0f)
            val newBankingBall = OnPlaneBall(center = bankingBallCenter, radius = LOGICAL_BALL_RADIUS)
            val defaultTableRotation = 0f // Let's keep it 0 for consistency
            val initialAimTarget = calculateInitialBankingAimTarget(newBankingBall, defaultTableRotation)
            currentState.copy(
                isBankingMode = true, onPlaneBall = newBankingBall,
                zoomSliderPosition = bankingZoomSliderPos,
                table = currentState.table.copy(
                    isVisible = true
                ),
                worldRotationDegrees = defaultTableRotation,
                bankingAimTarget = initialAimTarget,
                protractorUnit = currentState.protractorUnit.copy(radius = LOGICAL_BALL_RADIUS, center = PointF(0f,0f)),
                warningText = null
            )
        } else {
            val defaultSliderPos = 0f
            currentState.copy(
                isBankingMode = false, bankingAimTarget = null,
                zoomSliderPosition = defaultSliderPos,
                table = currentState.table.copy(
                    // In expert mode, the table should remain visible when exiting banking.
                    isVisible = currentState.experienceMode == ExperienceMode.EXPERT
                ),
                worldRotationDegrees = 0f,
                onPlaneBall = OnPlaneBall(
                    reducerUtils.getDefaultCueBallPosition(currentState),
                    LOGICAL_BALL_RADIUS
                ),
                protractorUnit = currentState.protractorUnit.copy(
                    radius = LOGICAL_BALL_RADIUS,
                    center = PointF(0f, 0f)
                ),
                warningText = null
            )
        }
        return reducerUtils.snapViolatingBalls(newState.copy(
            valuesChangedSinceReset = true,
            showLuminanceDialog = false, showTutorialOverlay = false,
            viewOffset = PointF(0f, 0f) // Also reset pan
        ))
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
}