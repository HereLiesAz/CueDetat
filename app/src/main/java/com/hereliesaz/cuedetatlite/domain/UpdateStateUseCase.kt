package com.hereliesaz.cuedetatlite.domain

import android.graphics.Matrix
import com.hereliesaz.cuedetatlite.ui.ZoomMapping
import com.hereliesaz.cuedetatlite.view.model.Perspective
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import com.hereliesaz.cuedetatlite.view.state.ScreenState
import kotlin.math.cos
import kotlin.math.sin

class UpdateStateUseCase(private val warningManager: WarningManager) {

    operator fun invoke(
        overlayState: OverlayState
    ): OverlayState {
        val perspective = calculatePerspective(overlayState)

        val updatedScreenState = overlayState.screenState.copy(
            isImpossibleShot = isImpossibleShot(overlayState.screenState)
        ).let { it.copy(warningText = warningManager.getWarning(it)) }

        return overlayState.copy(
            screenState = updatedScreenState,
            pitchMatrix = perspective.pitchMatrix,
            railPitchMatrix = perspective.railPitchMatrix,
            inversePitchMatrix = perspective.inverseMatrix,
            hasInverseMatrix = perspective.hasInverse
        )
    }

    private fun calculatePerspective(state: OverlayState): Perspective {
        val pitchMatrix = Matrix()
        val railPitchMatrix = Matrix()

        val orientation = state.anchorOrientation ?: state.currentOrientation
        val pitchRadians = Math.toRadians(orientation.pitch.toDouble())
        val rollRadians = Math.toRadians(orientation.roll.toDouble())

        val scaleFactor = ZoomMapping.sliderToZoom(state.zoomSliderPosition)

        // Center the view on the target ball
        pitchMatrix.postTranslate(-state.screenState.protractorUnit.targetBall.logicalPosition.x, -state.screenState.protractorUnit.targetBall.logicalPosition.y)
        // Apply zoom
        pitchMatrix.postScale(scaleFactor, scaleFactor)
        // Center in the view
        pitchMatrix.postTranslate(state.viewWidth / 2f, state.viewHeight / 2f)

        // Apply roll (as a skew/x-scale)
        val yShear = sin(rollRadians).toFloat()
        val xScale = cos(rollRadians).toFloat()
        pitchMatrix.postSkew(yShear, 0f, state.viewWidth / 2f, state.viewHeight / 2f)
        pitchMatrix.postScale(xScale, 1f, state.viewWidth / 2f, state.viewHeight / 2f)

        // Apply pitch (as a y-scale)
        val yPerspectiveScale = cos(pitchRadians).toFloat()
        pitchMatrix.postScale(1f, yPerspectiveScale, state.viewWidth / 2f, state.viewHeight / 2f)

        // Create the separate rail matrix before applying final perspective scaling
        railPitchMatrix.set(pitchMatrix)
        railPitchMatrix.postScale(1f, 1/yPerspectiveScale, state.viewWidth / 2f, state.viewHeight / 2f)

        val inverseMatrix = Matrix()
        val hasInverse = pitchMatrix.invert(inverseMatrix)

        return Perspective(pitchMatrix, railPitchMatrix, inverseMatrix, hasInverse)
    }

    private fun isImpossibleShot(screenState: ScreenState): Boolean {
        // This logic is less relevant now but can be repurposed later if needed.
        return false
    }
}