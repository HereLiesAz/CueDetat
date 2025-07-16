// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ToggleReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
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
                val newState = currentState.copy(tableSize = currentState.tableSize.next(), valuesChangedSinceReset = true)
                reducerUtils.snapViolatingBalls(newState)
            }
            is MainScreenEvent.SetTableSize -> {
                val newState = currentState.copy(tableSize = event.size, valuesChangedSinceReset = true)
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
        val newState = currentState.copy(showTable = !currentState.showTable, valuesChangedSinceReset = true)
        return resetForTable(newState)
    }


    private fun handleToggleOnPlaneBall(currentState: OverlayState): OverlayState {
        return if (currentState.onPlaneBall != null) {
            currentState.copy(onPlaneBall = null, valuesChangedSinceReset = true)
        } else {
            val newRadius = reducerUtils.getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, currentState.zoomSliderPosition)
            val newCenter = if (currentState.showTable) {
                reducerUtils.getDefaultCueBallPosition(currentState)
            } else {
                PointF(0f, currentState.protractorUnit.radius * 4)
            }
            currentState.copy(
                onPlaneBall = OnPlaneBall(center = newCenter, radius = newRadius),
                valuesChangedSinceReset = true
            )
        }
    }


    private fun resetForTable(currentState: OverlayState): OverlayState {
        val logicalRadius = reducerUtils.getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, 0f)
        val targetBallCenter = reducerUtils.getDefaultTargetBallPosition()
        val actualCueBallCenter = reducerUtils.getDefaultCueBallPosition(currentState)

        val updatedOnPlaneBall = if (currentState.onPlaneBall != null || currentState.showTable) {
            OnPlaneBall(center = actualCueBallCenter, radius = logicalRadius)
        } else {
            null
        }

        return currentState.copy(
            onPlaneBall = updatedOnPlaneBall,
            protractorUnit = ProtractorUnit(center = targetBallCenter, radius = logicalRadius, rotationDegrees = -90f),
            tableRotationDegrees = 90f, // Corrected default to portrait
            valuesChangedSinceReset = true,
            zoomSliderPosition = 0f,
            obstacleBalls = emptyList()
        )
    }

    private fun handleToggleBankingMode(currentState: OverlayState): OverlayState {
        val bankingEnabled = !currentState.isBankingMode
        val newState = if (bankingEnabled) {
            val bankingZoomSliderPos = 0f
            val newLogicalRadius = reducerUtils.getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, bankingZoomSliderPos)
            val bankingBallCenter = PointF(0f, 0f)
            val newBankingBall = OnPlaneBall(center = bankingBallCenter, radius = newLogicalRadius)
            val defaultTableRotation = 90f // Corrected default to portrait
            val initialAimTarget = calculateInitialBankingAimTarget(newBankingBall, defaultTableRotation, newLogicalRadius)
            currentState.copy(
                isBankingMode = true, onPlaneBall = newBankingBall,
                zoomSliderPosition = bankingZoomSliderPos, tableRotationDegrees = defaultTableRotation,
                bankingAimTarget = initialAimTarget,
                protractorUnit = currentState.protractorUnit.copy(radius = newLogicalRadius, center = PointF(0f,0f)),
                showTable = true,
                warningText = null
            )
        } else {
            val defaultSliderPos = 0f
            val defaultLogicalRadius = reducerUtils.getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, defaultSliderPos)
            currentState.copy(
                isBankingMode = false, bankingAimTarget = null,
                zoomSliderPosition = defaultSliderPos,
                showTable = false,
                onPlaneBall = OnPlaneBall(PointF(0f, defaultLogicalRadius * 4), defaultLogicalRadius),
                protractorUnit = currentState.protractorUnit.copy(
                    radius = defaultLogicalRadius,
                    center = PointF(0f, 0f)
                ),
                tableRotationDegrees = 0f, warningText = null
            )
        }
        return reducerUtils.snapViolatingBalls(newState.copy(
            valuesChangedSinceReset = true,
            showLuminanceDialog = false, showTutorialOverlay = false
        ))
    }

    private fun calculateInitialBankingAimTarget(
        cueBall: OnPlaneBall,
        tableRotationDegrees: Float,
        cueBallRadius: Float
    ): PointF {
        val defaultBankingAimDistanceFactor = 15f
        val aimDistance = cueBallRadius * defaultBankingAimDistanceFactor
        val angleRad = Math.toRadians((tableRotationDegrees - 90.0))
        return PointF(
            cueBall.center.x + (aimDistance * kotlin.math.cos(angleRad)).toFloat(),
            cueBall.center.y + (aimDistance * kotlin.math.sin(angleRad)).toFloat()
        )
    }
}