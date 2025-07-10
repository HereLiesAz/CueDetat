// app/src/main/java/com/hereliesaz/cuedetat/domain/UpdateStateUseCase.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

class UpdateStateUseCase @Inject constructor() {

    private val tableToBallRatioShort = 44f
    private val railHeightToTableHeightRatio = 0.05f

    // These constants are used for the distance estimation calculation.
    private val LOGICAL_FOCAL_LENGTH_PX = 1500f
    private val REAL_BALL_DIAMETER_INCHES = 2.25f

    operator fun invoke(state: OverlayState, camera: Camera): OverlayState {
        if (state.viewWidth == 0 || state.viewHeight == 0) return state

        val pitchMatrix = Perspective.createPitchMatrix(
            currentOrientation = state.currentOrientation,
            viewWidth = state.viewWidth,
            viewHeight = state.viewHeight,
            camera = camera
        )

        val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
        val logicalTableShortSide = tableToBallRatioShort * referenceRadius
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

        if (state.showTable) {
            val effectiveTableRotation = state.tableRotationDegrees % 360f
            if (effectiveTableRotation != 0f) {
                pitchMatrix.preRotate(effectiveTableRotation, centerX, centerY)
                railPitchMatrix.preRotate(effectiveTableRotation, centerX, centerY)
            }
        }

        val inverseMatrix = Matrix()
        val hasInverse = pitchMatrix.invert(inverseMatrix)

        val shotLineAnchorPoint: PointF = state.onPlaneBall?.center ?: run {
            if (hasInverse) {
                val screenAnchor = floatArrayOf(state.viewWidth / 2f, state.viewHeight.toFloat())
                val logicalAnchorArray = FloatArray(2)
                inverseMatrix.mapPoints(logicalAnchorArray, screenAnchor)
                PointF(logicalAnchorArray[0], logicalAnchorArray[1])
            } else {
                PointF(state.viewWidth / 2f, state.viewHeight.toFloat())
            }
        }

        val (isImpossible, tangentDirection) = calculateShotPossibilityAndTangent(
            shotAnchor = shotLineAnchorPoint,
            ghostBall = state.protractorUnit.ghostCueBallCenter,
            targetBall = state.protractorUnit.center
        )

        val isTiltBeyondLimit = !state.isBankingMode && shotLineAnchorPoint.y <= state.protractorUnit.ghostCueBallCenter.y

        val estimatedDistance = calculateEstimatedDistance(state)

        return state.copy(
            pitchMatrix = pitchMatrix,
            railPitchMatrix = railPitchMatrix,
            inversePitchMatrix = inverseMatrix,
            hasInverseMatrix = hasInverse,
            shotLineAnchor = shotLineAnchorPoint,
            isImpossibleShot = isImpossible,
            isTiltBeyondLimit = isTiltBeyondLimit,
            tangentDirection = tangentDirection,
            estimatedDistanceInches = estimatedDistance
        )
    }

    private fun calculateEstimatedDistance(state: OverlayState): Float {
        val logicalBall = state.onPlaneBall ?: state.protractorUnit
        val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(logicalBall.center, logicalBall.radius, state)
        val apparentDiameterPx = radiusInfo.radius * 2

        return if (apparentDiameterPx > 0) {
            (LOGICAL_FOCAL_LENGTH_PX * REAL_BALL_DIAMETER_INCHES) / apparentDiameterPx
        } else {
            0f
        }
    }

    private fun calculateShotPossibilityAndTangent(shotAnchor: PointF, ghostBall: PointF, targetBall: PointF): Pair<Boolean, Float> {
        val aimingAngle = atan2(targetBall.y - ghostBall.y, targetBall.x - ghostBall.x).toFloat()
        val shotAngle = atan2(ghostBall.y - shotAnchor.y, ghostBall.x - shotAnchor.x).toFloat()

        var angleDifference = aimingAngle - shotAngle
        while (angleDifference <= -PI) angleDifference += 2 * PI.toFloat()
        while (angleDifference > PI) angleDifference -= 2 * PI.toFloat()

        val isImpossible = abs(angleDifference) > PI / 2
        val tangentDirection = if (angleDifference < 0) 1.0f else -1.0f

        return Pair(isImpossible, tangentDirection)
    }
}