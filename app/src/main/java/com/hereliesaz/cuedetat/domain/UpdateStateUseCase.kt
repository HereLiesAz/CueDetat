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

    private val tableToBallRatioShort = 44f
    private val railHeightToTableHeightRatio = 0.05f

    operator fun invoke(state: OverlayState, camera: Camera): OverlayState {
        if (state.viewWidth == 0 || state.viewHeight == 0) return state

        val pitchMatrix = Perspective.createPitchMatrix(
            currentOrientation = state.currentOrientation,
            viewWidth = state.viewWidth,
            viewHeight = state.viewHeight,
            camera = camera
        )

        val referenceRadiusForTable = state.actualCueBall?.radius ?: state.protractorUnit.radius
        val logicalTableShortSide = tableToBallRatioShort * referenceRadiusForTable
        val railLiftAmount = logicalTableShortSide * railHeightToTableHeightRatio

        val railPitchMatrix = Perspective.createPitchMatrix(
            currentOrientation = state.currentOrientation,
            viewWidth = state.viewWidth,
            viewHeight = state.viewHeight,
            camera = camera,
            lift = railLiftAmount
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

        val anchorPointA: PointF? = state.actualCueBall?.center ?: run {
            if (hasInverse) {
                val screenAnchor = floatArrayOf(state.viewWidth / 2f, state.viewHeight.toFloat())
                val logicalAnchorArray = FloatArray(2)
                inverseMatrix.mapPoints(logicalAnchorArray, screenAnchor)
                PointF(logicalAnchorArray[0], logicalAnchorArray[1])
            } else {
                null
            }
        }

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
            isImpossibleShot = calculatedIsImpossibleShot
        )
    }

    private fun distance(p1: PointF, p2: PointF): Float =
        sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
}