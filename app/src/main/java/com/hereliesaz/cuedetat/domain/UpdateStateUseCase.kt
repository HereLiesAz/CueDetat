// app/src/main/java/com/hereliesaz/cuedetat/domain/UpdateStateUseCase.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.sqrt

class UpdateStateUseCase @Inject constructor() {

    operator fun invoke(state: OverlayState, camera: Camera): OverlayState {
        if (state.viewWidth == 0 || state.viewHeight == 0) return state

        // Create the base matrix for the table surface and aiming tools
        val pitchMatrix = Perspective.createPitchMatrix(
            state.pitchAngle,
            state.viewWidth,
            state.viewHeight,
            camera
        )

        // Create the lifted matrix for the rails
        val railHeight = state.protractorUnit.radius * 1.5f
        val railPitchMatrix = Perspective.createPitchMatrix(
            state.pitchAngle,
            state.viewWidth,
            state.viewHeight,
            camera,
            lift = railHeight
        )

        // If in banking mode, apply a 90-degree rotation to both matrices
        if (state.isBankingMode) {
            val centerX = state.viewWidth / 2f
            val centerY = state.viewHeight / 2f
            pitchMatrix.preRotate(90f, centerX, centerY)
            railPitchMatrix.preRotate(90f, centerX, centerY)
        }


        val inverseMatrix = Matrix()
        val hasInverse = pitchMatrix.invert(inverseMatrix)

        val anchorPointA: PointF? = if (state.actualCueBall != null) {
            state.actualCueBall.center
        } else {
            if (hasInverse) {
                val screenAnchor = floatArrayOf(state.viewWidth / 2f, state.viewHeight.toFloat())
                val logicalAnchorArray = FloatArray(2)
                inverseMatrix.mapPoints(logicalAnchorArray, screenAnchor)
                PointF(logicalAnchorArray[0], logicalAnchorArray[1])
            } else {
                null
            }
        }

        val isImpossible = anchorPointA?.let { anchor ->
            val distAtoG = distance(anchor, state.protractorUnit.protractorCueBallCenter)
            val distAtoT = distance(anchor, state.protractorUnit.center)
            distAtoG > distAtoT
        } ?: false

        return state.copy(
            pitchMatrix = pitchMatrix,
            railPitchMatrix = railPitchMatrix, // Store the new rail matrix
            inversePitchMatrix = inverseMatrix,
            hasInverseMatrix = hasInverse,
            isImpossibleShot = isImpossible
        )
    }

    private fun distance(p1: PointF, p2: PointF): Float =
        sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
}