// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ToggleReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.state.DistanceUnit
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.view.state.TableSize
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToggleReducer @Inject constructor(private val reducerUtils: ReducerUtils) {

    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.ToggleSpinControl -> currentState.copy(isSpinControlVisible = !currentState.isSpinControlVisible)
            is MainScreenEvent.ToggleOnPlaneBall -> handleToggleOnPlaneBall(currentState)
            is MainScreenEvent.ToggleBankingMode -> handleToggleBankingMode(currentState)
            is MainScreenEvent.ToggleTable -> handleToggleTable(currentState)
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
            else -> currentState
        }
    }

    private fun handleToggleTable(currentState: OverlayState): OverlayState {
        val newState = currentState.copy(
            table = currentState.table.copy(isVisible = !currentState.table.isVisible),
            valuesChangedSinceReset = true
        )
        return resetForTable(newState)
    }


    private fun handleToggleOnPlaneBall(currentState: OverlayState): OverlayState {
        return if (currentState.onPlaneBall != null) {
            currentState.copy(onPlaneBall = null, valuesChangedSinceReset = true)
        } else {
            // Always use the logical default position, regardless of table visibility.
            val newCenter = reducerUtils.getDefaultCueBallPosition(currentState)
            currentState.copy(
                onPlaneBall = OnPlaneBall(center = newCenter, radius = LOGICAL_BALL_RADIUS),
                valuesChangedSinceReset = true
            )
        }
    }


    private fun resetForTable(currentState: OverlayState): OverlayState {
        val targetBallCenter = reducerUtils.getDefaultTargetBallPosition()
        val actualCueBallCenter = reducerUtils.getDefaultCueBallPosition(currentState)

        val updatedOnPlaneBall = if (currentState.onPlaneBall != null || currentState.table.isVisible) {
            OnPlaneBall(center = actualCueBallCenter, radius = LOGICAL_BALL_RADIUS)
        } else {
            null
        }

        return currentState.copy(
            onPlaneBall = updatedOnPlaneBall,
            protractorUnit = ProtractorUnit(center = targetBallCenter, radius = LOGICAL_BALL_RADIUS, rotationDegrees = 0f),
            table = currentState.table.copy(rotationDegrees = 0f), // Portrait is default
            valuesChangedSinceReset = true,
            zoomSliderPosition = 0f,
            obstacleBalls = emptyList()
        )
    }

    private fun handleToggleBankingMode(currentState: OverlayState): OverlayState {
        val bankingEnabled = !currentState.isBankingMode
        val newState = if (bankingEnabled) {
            val bankingZoomSliderPos = 0f
            val bankingBallCenter = PointF(0f, 0f)
            val newBankingBall = OnPlaneBall(center = bankingBallCenter, radius = LOGICAL_BALL_RADIUS)
            val defaultTableRotation = 90f // Corrected: Portrait is default
            val initialAimTarget = calculateInitialBankingAimTarget(newBankingBall, defaultTableRotation)
            currentState.copy(
                isBankingMode = true, onPlaneBall = newBankingBall,
                zoomSliderPosition = bankingZoomSliderPos,
                table = currentState.table.copy(
                    isVisible = true,
                    rotationDegrees = defaultTableRotation
                ),
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
                    isVisible = false,
                    rotationDegrees = 0f,
                ),
                onPlaneBall = OnPlaneBall(PointF(0f, LOGICAL_BALL_RADIUS * 4), LOGICAL_BALL_RADIUS),
                protractorUnit = currentState.protractorUnit.copy(
                    radius = LOGICAL_BALL_RADIUS,
                    center = PointF(0f, 0f)
                ),
                warningText = null
            )
        }
        return reducerUtils.snapViolatingBalls(newState.copy(
            valuesChangedSinceReset = true,
            showLuminanceDialog = false, showTutorialOverlay = false
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
            cueBall.center.x + (aimDistance * kotlin.math.cos(angleRad)).toFloat(),
            cueBall.center.y + (aimDistance * kotlin.math.sin(angleRad)).toFloat()
        )
    }
}