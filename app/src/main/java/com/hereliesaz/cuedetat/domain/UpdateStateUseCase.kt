// app/src/main/java/com/hereliesaz/cuedetat/domain/UpdateStateUseCase.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.renderer.table.TableRenderer
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import kotlin.math.*

class UpdateStateUseCase @Inject constructor() {

    private val tableToBallRatioShort = 44f
    private val railHeightToTableHeightRatio = 0.05f
    private val distanceReferenceConstant = 6480f

    operator fun invoke(state: OverlayState, camera: Camera): OverlayState {
        if (state.viewWidth == 0 || state.viewHeight == 0) return state

        // --- Stage 1: Base Un-rotated Matrix for Stable Logic ---
        val basePitchMatrix = Perspective.createPitchMatrix(
            currentOrientation = state.currentOrientation,
            viewWidth = state.viewWidth,
            viewHeight = state.viewHeight,
            camera = camera
        )
        val baseInverseMatrix = Matrix().apply { basePitchMatrix.invert(this) }

        // Create a "flat" matrix with zero pitch for stable radius calculations.
        val flatMatrix = Perspective.createPitchMatrix(
            currentOrientation = FullOrientation(0f, 0f, 0f),
            viewWidth = state.viewWidth,
            viewHeight = state.viewHeight,
            camera = camera
        )


        // --- Stage 2: Calculate Logical Values in a Stable Coordinate System ---
        val logicalShotLineAnchor = getLogicalShotLineAnchor(state, baseInverseMatrix)
        val isTiltBeyondLimit = !state.isBankingMode && logicalShotLineAnchor.y <= state.protractorUnit.ghostCueBallCenter.y

        val (isImpossible, tangentDirection) = calculateShotPossibilityAndTangent(
            shotAnchor = logicalShotLineAnchor,
            ghostBall = state.protractorUnit.ghostCueBallCenter,
            targetBall = state.protractorUnit.center
        )
        val targetBallDistance = calculateDistance(state, flatMatrix) // Use flat matrix for stable distance
        val (aimedPocketIndex, aimingLineEndPoint) = if (!state.isBankingMode) {
            checkPocketAim(state)
        } else {
            Pair(null, null)
        }
        val shotGuideImpactPoint = if (state.showTable) calculateShotGuideImpact(state) else null

        // --- Stage 3: Prepare Final Rotated Matrices for Rendering ---
        val finalPitchMatrix = Matrix(basePitchMatrix)
        val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
        val logicalTableShortSide = tableToBallRatioShort * referenceRadius
        val railLiftAmount = logicalTableShortSide * railHeightToTableHeightRatio
        val finalRailPitchMatrix = Perspective.createPitchMatrix(
            currentOrientation = state.currentOrientation,
            viewWidth = state.viewWidth,
            viewHeight = state.viewHeight,
            camera = camera,
            lift = railLiftAmount
        )
        val finalInverseMatrix = Matrix()

        if (state.showTable) {
            val effectiveTableRotation = state.tableRotationDegrees % 360f
            if (effectiveTableRotation != 0f) {
                val centerX = state.viewWidth / 2f
                val centerY = state.viewHeight / 2f
                finalPitchMatrix.preRotate(effectiveTableRotation, centerX, centerY)
                finalRailPitchMatrix.preRotate(effectiveTableRotation, centerX, centerY)
            }
        }
        val hasFinalInverse = finalPitchMatrix.invert(finalInverseMatrix)

        return state.copy(
            pitchMatrix = finalPitchMatrix,
            railPitchMatrix = finalRailPitchMatrix,
            inversePitchMatrix = finalInverseMatrix,
            flatMatrix = flatMatrix,
            hasInverseMatrix = hasFinalInverse,
            shotLineAnchor = logicalShotLineAnchor, // State now holds the true logical anchor
            isImpossibleShot = isImpossible,
            isTiltBeyondLimit = isTiltBeyondLimit,
            tangentDirection = tangentDirection,
            targetBallDistance = targetBallDistance,
            aimedPocketIndex = aimedPocketIndex,
            aimingLineEndPoint = aimingLineEndPoint,
            shotGuideImpactPoint = shotGuideImpactPoint
        )
    }

    private fun getLogicalShotLineAnchor(state: OverlayState, inverseMatrix: Matrix): PointF {
        // If the on-plane ball exists, its logical center IS the anchor.
        state.onPlaneBall?.let { return it.center }

        // If not, calculate the anchor based on a point at the bottom of the screen.
        val screenAnchor = PointF(state.viewWidth / 2f, state.viewHeight.toFloat())
        return Perspective.screenToLogical(screenAnchor, inverseMatrix)
    }

    private fun calculateShotPossibilityAndTangent(shotAnchor: PointF, ghostBall: PointF, targetBall: PointF): Pair<Boolean, Float> {
        val aimingAngle = atan2(targetBall.y - ghostBall.y, targetBall.x - ghostBall.x).toFloat()
        val shotAngle = atan2(ghostBall.y - shotAnchor.y, ghostBall.x - shotAnchor.x).toFloat()
        var angleDifference = aimingAngle - shotAngle
        while (angleDifference <= -PI) angleDifference += (2 * PI).toFloat()
        while (angleDifference > PI) angleDifference -= (2 * PI).toFloat()
        val isImpossible = abs(angleDifference) > PI / 2
        val tangentDirection = if (angleDifference < 0) 1.0f else -1.0f
        return Pair(isImpossible, tangentDirection)
    }

    private fun calculateDistance(state: OverlayState, matrix: Matrix): Float {
        val (logicalCenter, logicalRadius) = if (state.isBankingMode && state.onPlaneBall != null) {
            state.onPlaneBall.center to state.onPlaneBall.radius
        } else {
            state.protractorUnit.center to state.protractorUnit.radius
        }
        val screenRadius = DrawingUtils.getPerspectiveRadiusAndLift(
            logicalCenter, logicalRadius, state.copy(pitchMatrix = matrix, hasInverseMatrix = true)
        ).radius
        return if (screenRadius > 0) distanceReferenceConstant / screenRadius else 0f
    }

    private fun checkPocketAim(state: OverlayState): Pair<Int?, PointF?> {
        val ghostBall = state.protractorUnit.ghostCueBallCenter
        val targetBall = state.protractorUnit.center
        val dirX = targetBall.x - ghostBall.x
        val dirY = targetBall.y - ghostBall.y
        val mag = sqrt(dirX * dirX + dirY * dirY)
        if (mag < 0.001f) return Pair(null, null)
        val pockets = TableRenderer.getLogicalPockets(state)
        val pocketRadius = state.protractorUnit.radius * 1.8f

        for ((index, pocket) in pockets.withIndex()) {
            val dist = linePointDistance(ghostBall, targetBall, pocket)
            if (dist < pocketRadius) {
                // Check if pocket is in front of ghost ball
                val vecToPocketX = pocket.x - ghostBall.x
                val vecToPocketY = pocket.y - ghostBall.y
                val dotProduct = vecToPocketX * dirX + vecToPocketY * dirY
                if (dotProduct > 0) {
                    val intersection = getLineCircleIntersection(ghostBall, targetBall, pocket, pocketRadius)
                    if (intersection != null) return Pair(index, intersection)
                }
            }
        }
        return Pair(null, null)
    }

    private fun calculateShotGuideImpact(state: OverlayState): PointF? {
        val p1 = state.shotLineAnchor
        val p2 = state.protractorUnit.ghostCueBallCenter
        val dirX = p2.x - p1.x
        val dirY = p2.y - p1.y

        val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
        if (referenceRadius <= 0) return null

        val tableToBallRatioLong = state.tableSize.getTableToBallRatioLong()
        val tableToBallRatioShort = tableToBallRatioLong / state.tableSize.aspectRatio
        val tableWidth = tableToBallRatioLong * referenceRadius
        val tableHeight = tableToBallRatioShort * referenceRadius

        val halfW = tableWidth / 2f
        val halfH = tableHeight / 2f
        val canvasCenterX = state.viewWidth / 2f
        val canvasCenterY = state.viewHeight / 2f

        val left = canvasCenterX - halfW
        val top = canvasCenterY - halfH
        val right = canvasCenterX + halfW
        val bottom = canvasCenterY + halfH

        var t = Float.MAX_VALUE
        if (dirX != 0f) {
            val tLeft = (left - p1.x) / dirX
            val tRight = (right - p1.x) / dirX
            if (tLeft > 0 && tLeft < t) t = tLeft
            if (tRight > 0 && tRight < t) t = tRight
        }
        if (dirY != 0f) {
            val tTop = (top - p1.y) / dirY
            val tBottom = (bottom - p1.y) / dirY
            if (tTop > 0 && tTop < t) t = tTop
            if (tBottom > 0 && tBottom < t) t = tBottom
        }

        return if (t != Float.MAX_VALUE) {
            PointF(p1.x + t * dirX, p1.y + t * dirY)
        } else {
            null
        }
    }

    private fun linePointDistance(p1: PointF, p2: PointF, p: PointF): Float {
        val num = abs((p2.x - p1.x) * (p1.y - p.y) - (p1.x - p.x) * (p2.y - p1.y))
        val den = sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2))
        return if (den == 0f) 0f else num / den
    }

    private fun getLineCircleIntersection(p1: PointF, p2: PointF, circleCenter: PointF, radius: Float): PointF? {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val a = dx * dx + dy * dy
        val b = 2 * (dx * (p1.x - circleCenter.x) + dy * (p1.y - circleCenter.y))
        val c = (p1.x - circleCenter.x).pow(2) + (p1.y - circleCenter.y).pow(2) - radius * radius
        val delta = b * b - 4 * a * c
        if (delta < 0) return null
        val t = (-b - sqrt(delta)) / (2 * a)
        return PointF(p1.x + t * dx, p1.y + t * dy)
    }
}