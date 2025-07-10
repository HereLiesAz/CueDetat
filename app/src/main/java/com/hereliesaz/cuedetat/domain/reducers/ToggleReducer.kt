package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.state.DistanceUnit
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToggleReducer @Inject constructor() {
    private val defaultBankingAimDistanceFactor = 15f

    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.ToggleOnPlaneBall -> handleToggleOnPlaneBall(currentState)
            is MainScreenEvent.ToggleBankingMode -> handleToggleBankingMode(currentState)
            is MainScreenEvent.ToggleTable -> handleToggleTable(currentState)
            is MainScreenEvent.CycleTableSize -> currentState.copy(tableSize = currentState.tableSize.next(), valuesChangedSinceReset = true)
            is MainScreenEvent.SetTableSize -> currentState.copy(tableSize = event.size, valuesChangedSinceReset = true)
            is MainScreenEvent.ToggleTableSizeDialog -> currentState.copy(showTableSizeDialog = !currentState.showTableSizeDialog)
            is MainScreenEvent.ToggleForceTheme -> {
                val newMode = when (currentState.isForceLightMode) { null -> true; true -> false; false -> null }
                currentState.copy(isForceLightMode = newMode, valuesChangedSinceReset = true)
            }
            is MainScreenEvent.ToggleDistanceUnit -> currentState.copy(
                distanceUnit = if (currentState.distanceUnit == DistanceUnit.METRIC) DistanceUnit.IMPERIAL else DistanceUnit.METRIC,
                valuesChangedSinceReset = true
            )
            is MainScreenEvent.ToggleLuminanceDialog -> currentState.copy(showLuminanceDialog = !currentState.showLuminanceDialog)
            is MainScreenEvent.ToggleHelp -> currentState.copy(areHelpersVisible = !currentState.areHelpersVisible)
            is MainScreenEvent.ToggleMoreHelp -> currentState.copy(isMoreHelpVisible = !currentState.isMoreHelpVisible)
            is MainScreenEvent.ToggleCamera -> currentState.copy(isCameraVisible = !currentState.isCameraVisible)
            else -> currentState
        }
    }

    private fun handleToggleTable(currentState: OverlayState): OverlayState {
        val newShowTable = !currentState.showTable
        var newState = currentState.copy(showTable = newShowTable, valuesChangedSinceReset = true)

        // If table is now shown in protractor mode and cue ball isn't there, add it.
        if (newShowTable && !newState.isBankingMode && newState.onPlaneBall == null) {
            newState = handleToggleOnPlaneBall(newState.copy(wasTableVisibleBeforeBallToggle = true))
        }
        return newState
    }

    private fun handleToggleOnPlaneBall(currentState: OverlayState): OverlayState {
        val viewCenterX = currentState.viewWidth / 2f
        val viewCenterY = currentState.viewHeight / 2f

        if (currentState.isBankingMode) return currentState

        return if (currentState.onPlaneBall == null) {
            // Create the ball
            val newCenter = if (currentState.wasTableVisibleBeforeBallToggle || currentState.showTable) {
                // Place at intersection of second diamond on bottom and left rails
                val tableToBallRatioLong = currentState.tableSize.getTableToBallRatioLong()
                val tableWidth = tableToBallRatioLong * ProtractorUnit.LOGICAL_BALL_RADIUS
                val halfW = tableWidth / 2f

                // Bottom rail, diamond 2: x = -halfW + 2/4 * tableWidth = -halfW + halfW = 0
                // Left rail, diamond 2: y = 0
                val logicalX = 0f
                val logicalY = 0f

                PointF(viewCenterX + logicalX, viewCenterY + logicalY)

            } else {
                // Default position when no table is visible
                val viewBottomY = currentState.viewHeight.toFloat()
                PointF(viewCenterX, (viewCenterY + viewBottomY) / 2f)
            }
            currentState.copy(
                onPlaneBall = OnPlaneBall(center = newCenter),
                showTable = currentState.wasTableVisibleBeforeBallToggle,
                wasTableVisibleBeforeBallToggle = false, // Reset flag
                valuesChangedSinceReset = true
            )
        } else {
            // Remove the ball, and if the table was visible, hide it too
            currentState.copy(
                onPlaneBall = null,
                showTable = if(currentState.showTable) false else currentState.showTable,
                wasTableVisibleBeforeBallToggle = currentState.showTable, // Store if table was visible
                valuesChangedSinceReset = true)
        }
    }

    private fun handleToggleBankingMode(currentState: OverlayState): OverlayState {
        val viewCenterX = currentState.viewWidth / 2f
        val viewCenterY = currentState.viewHeight / 2f
        val bankingEnabled = !currentState.isBankingMode
        val newState = if (bankingEnabled) {
            val bankingBallCenter = PointF(viewCenterX, viewCenterY)
            val newBankingBall = OnPlaneBall(center = bankingBallCenter)
            val initialAimTarget = calculateInitialBankingAimTarget(newBankingBall, 0f)
            currentState.copy(
                isBankingMode = true, onPlaneBall = newBankingBall,
                zoomSliderPosition = 0f, // Reset zoom for banking
                tableRotationDegrees = 0f,
                bankingAimTarget = initialAimTarget,
                protractorUnit = currentState.protractorUnit.copy(),
                showTable = true, // Always show table in banking mode
                warningText = null
            )
        } else {
            currentState.copy(
                isBankingMode = false, bankingAimTarget = null, onPlaneBall = null,
                showTable = false,
                protractorUnit = currentState.protractorUnit.copy(
                    center = PointF(viewCenterX, viewCenterY)
                ),
                tableRotationDegrees = 0f, warningText = null
            )
        }
        return newState.copy(
            valuesChangedSinceReset = true,
            showLuminanceDialog = false, showTutorialOverlay = false
        )
    }

    private fun calculateInitialBankingAimTarget(
        cueBall: OnPlaneBall,
        tableRotationDegrees: Float
    ): PointF {
        val aimDistance = ProtractorUnit.LOGICAL_BALL_RADIUS * defaultBankingAimDistanceFactor
        val angleRad = Math.toRadians((tableRotationDegrees - 90.0))
        return PointF(
            cueBall.center.x + (aimDistance * kotlin.math.cos(angleRad)).toFloat(),
            cueBall.center.y + (aimDistance * kotlin.math.sin(angleRad)).toFloat()
        )
    }
}