package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.state.DistanceUnit
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2

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
            is MainScreenEvent.ToggleCamera -> currentState.copy(isCameraVisible = !currentState.isCameraVisible)
            is MainScreenEvent.ToggleDistanceUnit -> currentState.copy(
                distanceUnit = if (currentState.distanceUnit == DistanceUnit.METRIC) DistanceUnit.IMPERIAL else DistanceUnit.METRIC,
                valuesChangedSinceReset = true
            )
            is MainScreenEvent.ToggleLuminanceDialog -> currentState.copy(showLuminanceDialog = !currentState.showLuminanceDialog)
            is MainScreenEvent.ToggleHelp -> currentState.copy(areHelpersVisible = !currentState.areHelpersVisible)
            is MainScreenEvent.ToggleMoreHelp -> currentState.copy(isMoreHelpVisible = !currentState.isMoreHelpVisible)
            else -> currentState
        }
    }

    private fun handleToggleTable(currentState: OverlayState): OverlayState {
        val newShowTable = !currentState.showTable
        var newState = currentState.copy(showTable = newShowTable, valuesChangedSinceReset = true)

        // If table is now shown in protractor mode, perform a full reset of positions.
        if (newShowTable && !newState.isBankingMode) {
            newState = resetForTable(newState)
        } else if (!newShowTable && !newState.isBankingMode) {
            // If table is hidden, remove the ball as well.
            newState = newState.copy(onPlaneBall = null)
        }
        return newState
    }

    private fun handleToggleOnPlaneBall(currentState: OverlayState): OverlayState {
        if (currentState.isBankingMode || currentState.showTable) return currentState

        return if (currentState.onPlaneBall == null) {
            val newRadius = ReducerUtils.getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, currentState.zoomSliderPosition)
            val newCenter = PointF(currentState.viewWidth / 2f, (currentState.viewHeight / 2f + currentState.viewHeight) / 2f)
            currentState.copy(onPlaneBall = OnPlaneBall(center = newCenter, radius = newRadius), valuesChangedSinceReset = true)
        } else {
            currentState.copy(onPlaneBall = null, valuesChangedSinceReset = true)
        }
    }

    private fun resetForTable(currentState: OverlayState): OverlayState {
        val viewCenterX = currentState.viewWidth / 2f
        val viewCenterY = currentState.viewHeight / 2f
        val logicalRadius = ReducerUtils.getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, 0f)

        // Calculate table dimensions in the logical plane
        val tableToBallRatioLong = currentState.tableSize.getTableToBallRatioLong()
        val tableHeight = (tableToBallRatioLong / currentState.tableSize.aspectRatio) * logicalRadius
        val halfHeight = tableHeight / 2f
        val bottomRailY = viewCenterY + halfHeight

        // Define default logical positions
        val targetBallCenter = PointF(viewCenterX, viewCenterY) // 4th diamond line (center)
        val actualCueBallCenter = PointF(viewCenterX, bottomRailY - (tableHeight / 4f)) // 2nd diamond line (head spot)

        // Calculate rotation to place Ghost Ball between the other two, making them collinear.
        val angleRad = atan2((targetBallCenter.y - actualCueBallCenter.y), (targetBallCenter.x - actualCueBallCenter.x))
        val rotationDegrees = Math.toDegrees(angleRad.toDouble()).toFloat() - 90f

        return currentState.copy(
            onPlaneBall = OnPlaneBall(center = actualCueBallCenter, radius = logicalRadius),
            protractorUnit = ProtractorUnit(center = targetBallCenter, radius = logicalRadius, rotationDegrees = rotationDegrees),
            tableRotationDegrees = if (currentState.viewWidth > currentState.viewHeight) 0f else 90f,
            valuesChangedSinceReset = true,
            zoomSliderPosition = 0f
        )
    }

    private fun handleToggleBankingMode(currentState: OverlayState): OverlayState {
        val viewCenterX = currentState.viewWidth / 2f
        val viewCenterY = currentState.viewHeight / 2f
        val bankingEnabled = !currentState.isBankingMode
        val newState = if (bankingEnabled) {
            val bankingZoomSliderPos = 0f // Centered default
            val newLogicalRadius = ReducerUtils.getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, bankingZoomSliderPos)
            val bankingBallCenter = PointF(viewCenterX, viewCenterY)
            val newBankingBall = OnPlaneBall(center = bankingBallCenter, radius = newLogicalRadius)
            val initialAimTarget = calculateInitialBankingAimTarget(newBankingBall, 0f, newLogicalRadius)
            currentState.copy(
                isBankingMode = true, onPlaneBall = newBankingBall,
                zoomSliderPosition = bankingZoomSliderPos, tableRotationDegrees = 0f,
                bankingAimTarget = initialAimTarget,
                protractorUnit = currentState.protractorUnit.copy(radius = newLogicalRadius),
                showTable = true, // Always show table in banking mode
                warningText = null
            )
        } else {
            val defaultSliderPos = 0f // Centered default
            val defaultLogicalRadius = ReducerUtils.getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, defaultSliderPos)
            currentState.copy(
                isBankingMode = false, bankingAimTarget = null, onPlaneBall = null,
                zoomSliderPosition = defaultSliderPos,
                showTable = false,
                protractorUnit = currentState.protractorUnit.copy(
                    radius = defaultLogicalRadius,
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
        tableRotationDegrees: Float,
        cueBallRadius: Float
    ): PointF {
        val aimDistance = cueBallRadius * defaultBankingAimDistanceFactor
        val angleRad = Math.toRadians((tableRotationDegrees - 90.0))
        return PointF(
            cueBall.center.x + (aimDistance * kotlin.math.cos(angleRad)).toFloat(),
            cueBall.center.y + (aimDistance * kotlin.math.sin(angleRad)).toFloat()
        )
    }
}