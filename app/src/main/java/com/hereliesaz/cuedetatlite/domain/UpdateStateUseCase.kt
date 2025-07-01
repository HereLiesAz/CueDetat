package com.hereliesaz.cuedetatlite.domain

import com.hereliesaz.cuedetatlite.ui.ZoomMapping
import com.hereliesaz.cuedetatlite.view.model.Perspective
import com.hereliesaz.cuedetatlite.view.model.TableModel
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import com.hereliesaz.cuedetatlite.view.state.ScreenState

class UpdateStateUseCase {

    operator fun invoke(
        currentState: OverlayState,
        screenState: ScreenState,
        zoomMapping: ZoomMapping
    ): OverlayState {
        // Update zoom-dependent radii first
        val newRadius = zoomMapping.sliderToRadius(currentState.zoomSliderPosition)
        val updatedProtractorUnit = currentState.protractorUnit.copy(radius = newRadius)
        val updatedActualCueBall = currentState.actualCueBall.copy(radius = newRadius)

        // Create the perspective transformation matrix
        val pitchMatrix = Perspective.createPitchMatrix(
            screenState,
            currentState.currentOrientation,
            currentState.isJumpShot
        )

        // Create the rail pitch matrix
        val railPitchMatrix = Perspective.createRailPitchMatrix(
            screenState,
            currentState.currentOrientation,
            currentState.isJumpShot
        )

        var tableModel = currentState.tableModel
        var bankingPath: List<android.graphics.PointF> = emptyList()

        if (!currentState.isProtractorMode) {
            // Create or update the table model if needed (e.g., on screen resize)
            if (screenState.width > 0 && (tableModel == null || tableModel.surface.width() != screenState.width * 0.8f)) {
                tableModel = TableModel.create(screenState.width.toFloat(), screenState.height.toFloat())
            }
            // Calculate the banking path using the model
            bankingPath = tableModel?.calculateBankingPath(updatedActualCueBall.center, currentState.bankingAimTarget) ?: emptyList()
        } else {
            tableModel = null
            bankingPath = emptyList()
        }

        // Update the state with the new matrices and radii
        return currentState.copy(
            pitchMatrix = pitchMatrix,
            railPitchMatrix = railPitchMatrix,
            protractorUnit = updatedProtractorUnit,
            actualCueBall = updatedActualCueBall,
            tableModel = tableModel,
            bankingPath = bankingPath,
            screenState = screenState
        )
    }
}