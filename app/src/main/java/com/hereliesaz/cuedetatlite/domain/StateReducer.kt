package com.hereliesaz.cuedetatlite.domain

import com.hereliesaz.cuedetatlite.ui.MainScreenEvent
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import com.hereliesaz.cuedetatlite.view.state.ScreenState

class StateReducer {

    fun reduce(
        event: MainScreenEvent,
        currentState: OverlayState,
        screenState: ScreenState
    ): OverlayState {
        return when (event) {
            is MainScreenEvent.FullOrientationChanged -> currentState.copy(currentOrientation = event.orientation)
            is MainScreenEvent.RotationChanged -> {
                val newProtractorUnit = currentState.protractorUnit.copy(rotationDegrees = event.rotation)
                currentState.copy(protractorUnit = newProtractorUnit)
            }
            is MainScreenEvent.UpdateLogicalUnitPosition -> {
                val logicalPoint = screenState.screenToLogical(event.x, event.y, currentState.pitchMatrix)
                val newProtractorUnit = currentState.protractorUnit.copy(
                    targetBallCenter = logicalPoint,
                )
                currentState.copy(protractorUnit = newProtractorUnit)
            }
            is MainScreenEvent.UpdateActualCueBallPosition -> {
                val logicalPoint = screenState.screenToLogical(event.x, event.y, currentState.pitchMatrix)
                val newActualCueBall = currentState.actualCueBall.copy(center = logicalPoint)
                currentState.copy(actualCueBall = newActualCueBall)
            }
            is MainScreenEvent.UpdateBankingAimTarget -> {
                val logicalPoint = screenState.screenToLogical(event.x, event.y, currentState.pitchMatrix)
                currentState.copy(bankingAimTarget = logicalPoint)
            }
            is MainScreenEvent.UpdateTableRotation -> {
                currentState.copy(tableRotationDegrees = event.rotation)
            }
            is MainScreenEvent.ZoomChanged -> currentState.copy(zoomSliderPosition = event.zoom)
            is MainScreenEvent.SwitchModes -> {
                val newMode = if (currentState.isProtractorMode) "Banking" else "Protractor"
                currentState.copy(
                    isProtractorMode = !currentState.isProtractorMode,
                    isJumpShot = if (newMode == "Banking") false else currentState.isJumpShot // No jump shots in banking mode
                )
            }
            is MainScreenEvent.ToggleDarkMode -> currentState.copy(isDarkMode = !currentState.isDarkMode)
            is MainScreenEvent.ToggleJumpShot -> currentState.copy(isJumpShot = !currentState.isJumpShot)
            is MainScreenEvent.ToggleProtractorCueBall -> currentState.copy(showProtractorCueBall = !currentState.showProtractorCueBall)
            is MainScreenEvent.ToggleActualCueBall -> currentState.copy(showActualCueBall = !currentState.showActualCueBall)
            is MainScreenEvent.Undo -> {
                // This is a placeholder. A proper undo would require a history of states.
                currentState
            }
        }
    }
}