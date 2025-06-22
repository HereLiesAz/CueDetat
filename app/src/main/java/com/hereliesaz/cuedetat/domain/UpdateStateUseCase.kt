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

    private val tableToBallRatioShort = 44f // For table height calculation
    private val railHeightToTableHeightRatio =
        0.05f // Rails are e.g., 5% of table's logical short side

    operator fun invoke(state: OverlayState, camera: Camera): OverlayState {
        if (state.viewWidth == 0 || state.viewHeight == 0) return state

        // Create the base matrix for the table surface and aiming tools
        val pitchMatrix = Perspective.createPitchMatrix(
            state.pitchAngle,
            state.viewWidth,
            state.viewHeight,
            camera
        )

        // Calculate rail height based on the table's logical short dimension
        // state.protractorUnit.radius here is the CURRENT ball radius, which is tied to zoom.
        val logicalTableShortSide = tableToBallRatioShort * state.protractorUnit.radius
        val railLiftAmount = logicalTableShortSide * railHeightToTableHeightRatio

        val railPitchMatrix = Perspective.createPitchMatrix(
            state.pitchAngle,
            state.viewWidth,
            state.viewHeight,
            camera,
            lift = railLiftAmount // Use table-proportional lift
        )

        val centerX = state.viewWidth / 2f
        val centerY = state.viewHeight / 2f

        if (state.isBankingMode) {
            pitchMatrix.preRotate(90f, centerX, centerY)
            railPitchMatrix.preRotate(90f, centerX, centerY)

            val effectiveTableRotation = state.tableRotationDegrees % 360f
            if (effectiveTableRotation != 0f) {
                pitchMatrix.preRotate(effectiveTableRotation, centerX, centerY)
                railPitchMatrix.preRotate(effectiveTableRotation, centerX, centerY)
            }
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

        // Calculate isImpossibleShot based on the current (potentially updated) state
        val calculatedIsImpossibleShot = anchorPointA?.let { anchor ->
            val distAtoG = distance(anchor, state.protractorUnit.protractorCueBallCenter)
            val distAtoT = distance(anchor, state.protractorUnit.center)
            distAtoG > distAtoT
        } ?: false

        return state.copy(
            pitchMatrix = pitchMatrix,
            railPitchMatrix = railPitchMatrix,
            inversePitchMatrix = inverseMatrix,
            hasInverseMatrix = hasInverse,
            isImpossibleShot = calculatedIsImpossibleShot // Corrected reference
        )
    }

    private fun distance(p1: PointF, p2: PointF): Float =
        sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
}