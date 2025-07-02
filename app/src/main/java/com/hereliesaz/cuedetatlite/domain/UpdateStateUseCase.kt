package com.hereliesaz.cuedetatlite.domain

import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetatlite.ui.ZoomMapping
import com.hereliesaz.cuedetatlite.view.model.ActualCueBall
import com.hereliesaz.cuedetatlite.view.model.Perspective
import com.hereliesaz.cuedetatlite.view.model.ProtractorUnit
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import com.hereliesaz.cuedetatlite.view.state.ScreenState
import javax.inject.Inject
import kotlin.math.*

class UpdateStateUseCase @Inject constructor(private val warningManager: WarningManager) {

    private val ROLL_DAMPENING_FACTOR = 0.2f

    operator fun invoke(
        overlayState: OverlayState
    ): OverlayState {
        val logicalRadius = if(overlayState.viewWidth == 0 || overlayState.viewHeight == 0) 30f else
            (min(overlayState.viewWidth, overlayState.viewHeight) * 0.30f / 2f) * ZoomMapping.sliderToZoom(overlayState.zoomSliderPosition)

        var updatedScreenState = overlayState.screenState.copy(
            protractorUnit = overlayState.screenState.protractorUnit.copy(
                targetBall = ProtractorUnit.LogicalBall(overlayState.screenState.protractorUnit.targetBall.logicalPosition, logicalRadius)
            ),
            actualCueBall = overlayState.screenState.actualCueBall?.let { ActualCueBall(it.logicalPosition, logicalRadius) }
        )

        val perspective = calculatePerspective(overlayState, updatedScreenState)
        val impossibleShot = isImpossibleShot(updatedScreenState, perspective.inverseMatrix, overlayState.viewWidth, overlayState.viewHeight)

        updatedScreenState = updatedScreenState.copy(
            isImpossibleShot = impossibleShot
        ).let { it.copy(warningText = warningManager.getWarning(it)) }

        return overlayState.copy(
            screenState = updatedScreenState,
            pitchMatrix = perspective.pitchMatrix,
            railPitchMatrix = perspective.railPitchMatrix,
            inversePitchMatrix = perspective.inverseMatrix,
            hasInverseMatrix = perspective.hasInverse
        )
    }

    private fun calculatePerspective(state: OverlayState, screenState: ScreenState): Perspective {
        val pitchMatrix = Matrix()
        val railPitchMatrix = Matrix()

        val orientation = state.anchorOrientation ?: state.currentOrientation
        val pitchRadians = Math.toRadians(orientation.pitch.toDouble())
        val rollRadians = Math.toRadians(orientation.roll.toDouble()) * ROLL_DAMPENING_FACTOR

        val baseRadius = min(state.viewWidth, state.viewHeight) * 0.30f / 2f
        val scaleFactor = if (baseRadius > 0) screenState.protractorUnit.targetBall.radius / baseRadius else 1f

        pitchMatrix.postTranslate(-screenState.protractorUnit.targetBall.logicalPosition.x, -screenState.protractorUnit.targetBall.logicalPosition.y)
        pitchMatrix.postScale(scaleFactor, scaleFactor)
        pitchMatrix.postTranslate(state.viewWidth / 2f, state.viewHeight / 2f)

        val yShear = sin(rollRadians).toFloat()
        val xScale = cos(rollRadians).toFloat()
        pitchMatrix.postSkew(yShear, 0f, state.viewWidth / 2f, state.viewHeight / 2f)
        pitchMatrix.postScale(xScale, 1f, state.viewWidth / 2f, state.viewHeight / 2f)

        val yPerspectiveScale = cos(pitchRadians).toFloat()
        pitchMatrix.postScale(1f, yPerspectiveScale, state.viewWidth / 2f, state.viewHeight / 2f)

        railPitchMatrix.set(pitchMatrix)
        railPitchMatrix.postScale(1f, 1/yPerspectiveScale, state.viewWidth / 2f, state.viewHeight / 2f)

        val inverseMatrix = Matrix()
        val hasInverse = pitchMatrix.invert(inverseMatrix)

        return Perspective(pitchMatrix, railPitchMatrix, inverseMatrix, hasInverse)
    }

    private fun isImpossibleShot(screenState: ScreenState, inverseMatrix: Matrix, viewWidth: Int, viewHeight: Int): Boolean {
        if (screenState.isBankingMode) return false

        val anchorPoint = screenState.actualCueBall?.logicalPosition ?: run {
            if (!inverseMatrix.isIdentity) {
                val screenAnchor = floatArrayOf(viewWidth / 2f, viewHeight.toFloat())
                val logicalAnchor = floatArrayOf(0f, 0f)
                inverseMatrix.mapPoints(logicalAnchor, screenAnchor)
                PointF(logicalAnchor[0], logicalAnchor[1])
            } else {
                PointF(viewWidth / 2f, viewHeight.toFloat())
            }
        }

        val targetBall = screenState.protractorUnit.targetBall

        val angleRad = Math.toRadians(screenState.protractorUnit.aimingAngleDegrees.toDouble()).toFloat()
        val totalRadius = targetBall.radius * 2
        val ghostBallX = targetBall.logicalPosition.x - cos(angleRad) * totalRadius
        val ghostBallY = targetBall.logicalPosition.y - sin(angleRad) * totalRadius
        val ghostBallPos = PointF(ghostBallX, ghostBallY)

        val distAnchorToTarget = distance(anchorPoint, targetBall.logicalPosition)
        val distAnchorToGhost = distance(anchorPoint, ghostBallPos)

        return distAnchorToGhost > distAnchorToTarget
    }

    private fun distance(p1: PointF, p2: PointF): Float {
        return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
    }
}