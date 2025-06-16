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

        // 1. Create the world matrix (panning and zooming the logical plane)
        val worldMatrix = Matrix().apply {
            // Center the logical view point (panOffset) on the screen
            preTranslate(state.viewWidth / 2f, state.viewHeight / 2f)
            // Apply zoom
            preScale(state.scale, state.scale)
            // Move the logical plane so the viewCenter is at the origin
            preTranslate(-state.viewCenter.x, -state.viewCenter.y)
        }

        // 2. Create the perspective matrix
        val pitchMatrix = Perspective.createPitchMatrix(
            state.pitchAngle,
            state.viewWidth,
            state.viewHeight,
            camera
        )

        // 3. Combine them: world first, then pitch
        val worldToScreenMatrix = Matrix(pitchMatrix).apply {
            postConcat(worldMatrix)
        }

        // 4. Calculate the inverse for touch interactions
        val screenToWorldMatrix = Matrix()
        val hasInverse = worldToScreenMatrix.invert(screenToWorldMatrix)

        // 5. Determine the anchor point for the shot line
        val anchorPointA: PointF? = if (state.actualCueBall != null) {
            state.actualCueBall.center
        } else {
            if (hasInverse) {
                // The anchor is the logical point corresponding to the bottom-center of the screen
                Perspective.screenToLogical(
                    PointF(
                        state.viewWidth / 2f,
                        state.viewHeight.toFloat()
                    ), screenToWorldMatrix
                )
            } else {
                null
            }
        }

        // 6. Check if the shot is impossible based on the anchor
        val isImpossible = anchorPointA?.let { anchor ->
            val distAtoG = distance(anchor, state.protractorUnit.protractorCueBallCenter)
            val distAtoT = distance(anchor, state.protractorUnit.center)
            distAtoG > distAtoT
        } ?: false

        return state.copy(
            worldToScreenMatrix = worldToScreenMatrix,
            screenToWorldMatrix = screenToWorldMatrix,
            hasInverseMatrix = hasInverse,
            isImpossibleShot = isImpossible
        )
    }

    private fun distance(p1: PointF, p2: PointF): Float =
        sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
}
