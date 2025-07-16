// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/UpdateStateUseCase.kt
package com.hereliesaz.cuedetat.domain

import android.R.attr.direction
import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.view.model.BankShotResult
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.domain.ReducerUtils
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class UpdateStateUseCase @Inject constructor(
    private val calculateSpinPaths: CalculateSpinPaths,
    private val calculateBankShot: CalculateBankShot
) {

    private val railHeightToTableHeightRatio = 0.05f
    private val distanceReferenceConstant = 6480f
    private val lineExtensionFactor = 5000f

    operator fun invoke(state: OverlayState, camera: Camera): OverlayState {
        if (state.viewWidth == 0 || state.viewHeight == 0) return state

        val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
        val updatedTable = state.table.recalculateGeometry(referenceRadius)
        val tableGeometry = updatedTable.geometry

        val basePitchMatrix = Perspective.createPitchMatrix(
            currentOrientation = state.currentOrientation,
            viewWidth = state.viewWidth,
            viewHeight = state.viewHeight,
            camera = camera
        )

        val flatMatrix = Perspective.createPitchMatrix(
            currentOrientation = FullOrientation(0f, 0f, 0f),
            viewWidth = state.viewWidth,
            viewHeight = state.viewHeight,
            camera = camera
        )

        val logicalShotLineAnchor = getLogicalShotLineAnchor(state)
        val isTiltBeyondLimit = !state.isBankingMode && logicalShotLineAnchor.y <= state.protractorUnit.ghostCueBallCenter.y

        val (isGeometricallyImpossible, tangentDirection) = calculateShotPossibilityAndTangent(
            shotAnchor = logicalShotLineAnchor,
            ghostBall = state.protractorUnit.ghostCueBallCenter,
            targetBall = state.protractorUnit.center
        )

        val isObstructed = checkForObstructions(state)
        val targetBallDistance = calculateDistance(state, flatMatrix)
        val shotGuideImpactPoint = if (updatedTable.isVisible && !state.isBankingMode && tableGeometry.isValid) {
            calculateShotGuideImpact(state)
        } else {
            null
        }

        val ghostCueCenter = state.protractorUnit.ghostCueBallCenter
        val targetCenter = state.protractorUnit.center

        val (finalAimingLineBankPath, aimedPocketIndex, aimingLineEndPoint) = if (updatedTable.isVisible && !state.isBankingMode && tableGeometry.isValid) {
            calculateAimAndBank(ghostCueCenter, targetCenter, state)
        } else {
            val extendedPath = getExtendedLinePath(ghostCueCenter, targetCenter)
            Triple(extendedPath, null, extendedPath.last())
        }

        val tangentStart = state.protractorUnit.ghostCueBallCenter
        val activeTangentTarget = PointF(
            tangentStart.x - (targetCenter.y - tangentStart.y) * tangentDirection,
            tangentStart.y + (targetCenter.x - tangentStart.x) * tangentDirection
        )

        val (finalTangentLineBankPath, tangentAimedPocketIndex, _) = if (updatedTable.isVisible && !state.isBankingMode && tableGeometry.isValid) {
            calculateAimAndBank(tangentStart, activeTangentTarget, state)
        } else {
            Triple(getExtendedLinePath(tangentStart, activeTangentTarget), null, null)
        }

        val finalPitchMatrix = Matrix(basePitchMatrix)
        val baseRailLiftAmount = tableGeometry.height * railHeightToTableHeightRatio
        val railLiftAmount = baseRailLiftAmount * abs(sin(Math.toRadians(state.pitchAngle.toDouble()))).toFloat()
        val finalRailPitchMatrix = Perspective.createPitchMatrix(
            currentOrientation = state.currentOrientation, viewWidth = state.viewWidth, viewHeight = state.viewHeight, camera = camera, lift = railLiftAmount
        )
        val finalInverseMatrix = Matrix()

        if (updatedTable.isVisible) {
            val effectiveTableRotation = updatedTable.rotationDegrees % 360f
            if (effectiveTableRotation != 0f) {
                finalPitchMatrix.preRotate(effectiveTableRotation, 0f, 0f)
                finalRailPitchMatrix.preRotate(effectiveTableRotation, 0f, 0f)
            }
        }
        val hasFinalInverse = finalPitchMatrix.invert(finalInverseMatrix)

        val spinPaths: Map<Color, List<PointF>> = if (!state.isBankingMode) {
            calculateSpinPaths(state)
        } else {
            emptyMap()
        }

        val bankShotResult: BankShotResult = if (state.isBankingMode) {
            calculateBankShot(state)
        } else {
            BankShotResult()
        }

        return state.copy(
            pitchMatrix = finalPitchMatrix,
            railPitchMatrix = finalRailPitchMatrix,
            inversePitchMatrix = finalInverseMatrix,
            hasInverseMatrix = hasFinalInverse,
            shotLineAnchor = logicalShotLineAnchor,
            isGeometricallyImpossible = isGeometricallyImpossible,
            isObstructed = isObstructed,
            isTiltBeyondLimit = isTiltBeyondLimit,
            tangentDirection = tangentDirection,
            targetBallDistance = targetBallDistance,
            aimedPocketIndex = aimedPocketIndex,
            tangentAimedPocketIndex = tangentAimedPocketIndex,
            aimingLineEndPoint = aimingLineEndPoint,
            shotGuideImpactPoint = shotGuideImpactPoint,
            aimingLineBankPath = finalAimingLineBankPath,
            tangentLineBankPath = finalTangentLineBankPath,
            spinPaths = spinPaths,
            table = updatedTable,
            bankShotPath = bankShotResult.path,
            pocketedBankShotPocketIndex = bankShotResult.pocketedIndex
        )
    }

    private fun getLogicalShotLineAnchor(state: OverlayState): PointF {
        state.onPlaneBall?.let { return it.center }
        return PointF(state.protractorUnit.center.x, state.protractorUnit.center.y + 10000f)
    }

    private fun calculateShotPossibilityAndTangent(shotAnchor: PointF, ghostBall: PointF, targetBall: PointF): Pair<Boolean, Float> {
        val aimingAngle = atan2(targetBall.y - ghostBall.y, targetBall.x - ghostBall.x)
        val distToGhostSq = (ghostBall.y - shotAnchor.y).pow(2) + (ghostBall.x - shotAnchor.x).pow(2)
        val distToTargetSq = (targetBall.y - shotAnchor.y).pow(2) + (targetBall.x - shotAnchor.x).pow(2)
        val isImpossible = distToGhostSq > distToTargetSq
        val shotAngle = atan2(ghostBall.y - shotAnchor.y, ghostBall.x - shotAnchor.x)
        var angleDifference = Math.toDegrees(aimingAngle.toDouble() - shotAngle.toDouble()).toFloat()
        while (angleDifference <= -180) angleDifference += 360
        while (angleDifference > 180) angleDifference -= 360
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

    private fun getExtendedLinePath(start: PointF, end: PointF): List<PointF> {
        val dirX = end.x - start.x; val dirY = end.y - start.y
        val mag = sqrt(dirX * dirX + dirY * dirY)
        val extendedEnd = if (mag > 0.001f) {
            val ndx = dirX / mag; val ndy = dirY / mag
            PointF(start.x + ndx * lineExtensionFactor, start.y + ndy * lineExtensionFactor)
        } else {
            end
        }
        return listOf(start, extendedEnd)
    }

    private fun calculateAimAndBank(start: PointF, end: PointF, state: OverlayState): Triple<List<PointF>, Int?, PointF> {
        val (directPocketIndex, directIntersection) = checkPocketAim(start, end, state)
        if (directPocketIndex != null && directIntersection != null) {
            return Triple(listOf(start, directIntersection), directPocketIndex, directIntersection)
        }
        val bankPath = calculateSingleBank(start, end, state)
        if (bankPath.size > 2) {
            val (bankedPocketIndex, bankedIntersection) = checkPocketAim(bankPath[1], bankPath[2], state)
            if (bankedPocketIndex != null && bankedIntersection != null) {
                return Triple(listOf(bankPath[0], bankPath[1], bankedIntersection), bankedPocketIndex, bankedIntersection)
            }
        }
        return Triple(bankPath, null, bankPath.last())
    }

    private fun checkPocketAim(start: PointF, end: PointF, state: OverlayState): Pair<Int?, PointF?> {
        val dirX = end.x - start.x; val dirY = end.y - start.y
        val mag = sqrt(dirX * dirX + dirY * dirY)
        if (mag < 0.001f) return Pair(null, null)
        val extendedEnd = PointF(start.x + dirX / mag * lineExtensionFactor, start.y + dirY / mag * lineExtensionFactor)
        val pockets = state.table.getLogicalPockets(state.protractorUnit.radius)
        val pocketRadius = state.protractorUnit.radius * 1.8f
        var closestIntersection: PointF? = null; var closestPocketIndex: Int? = null; var minDistanceSq = Float.MAX_VALUE
        pockets.forEachIndexed { index, pocket ->
            val intersection = getLineCircleIntersection(start, extendedEnd, pocket, pocketRadius)
            if (intersection != null) {
                val vecToPocketX = pocket.x - start.x; val vecToPocketY = pocket.y - start.y
                val dotProduct = vecToPocketX * (dirX / mag) + vecToPocketY * (dirY / mag)
                if (dotProduct > 0) {
                    val distSq = (intersection.x - start.x).pow(2) + (intersection.y - start.y).pow(2)
                    if (distSq < minDistanceSq) {
                        minDistanceSq = distSq; closestIntersection = intersection; closestPocketIndex = index
                    }
                }
            }
        }
        return Pair(closestPocketIndex, closestIntersection)
    }

    private fun calculateShotGuideImpact(state: OverlayState, useShotGuideLine: Boolean = false): PointF? {
        val p1 = if (useShotGuideLine) state.shotLineAnchor else state.protractorUnit.center
        val p2 = state.protractorUnit.ghostCueBallCenter
        val dirX = p2.x - p1.x
        val dirY = p2.y - p1.y
        return state.table.findRailIntersectionAndNormal(p1, PointF(p1.x + dirX * 5000f, p1.y + dirY * 5000f))?.first
    }

    private fun calculateSingleBank(start: PointF, end: PointF, state: OverlayState): List<PointF> {
        val dirX = end.x - start.x; val dirY = end.y - start.y
        val extendedEnd = PointF(start.x + dirX * lineExtensionFactor, start.y + dirY * lineExtensionFactor)
        val intersectionResult = state.table.findRailIntersectionAndNormal(start, extendedEnd) ?: return listOf(start, extendedEnd)
        val intersectionPoint = intersectionResult.first
        val railNormal = intersectionResult.second
        val reflectedDir = reducerUtils.reflect(direction, railNormal)
        val finalEndPoint = PointF(intersectionPoint.x + reflectedDir.x * lineExtensionFactor, intersectionPoint.y + reflectedDir.y * lineExtensionFactor)
        return listOf(start, intersectionPoint, finalEndPoint)
    }

    private fun checkForObstructions(state: OverlayState): Boolean {
        if (state.obstacleBalls.isEmpty() || !state.table.geometry.isValid) return false
        val ballRadius = state.protractorUnit.radius
        val collisionDistance = ballRadius * 2
        val shotLineStart = state.shotLineAnchor
        val shotLineEnd = state.protractorUnit.ghostCueBallCenter
        for (obstacle in state.obstacleBalls) {
            if (isSegmentObstructed(shotLineStart, shotLineEnd, obstacle.center, collisionDistance)) return true
        }
        val aimingLineStart = state.protractorUnit.ghostCueBallCenter
        val aimingLineEnd = state.protractorUnit.center
        for (obstacle in state.obstacleBalls) {
            if (isSegmentObstructed(aimingLineStart, aimingLineEnd, obstacle.center, collisionDistance)) return true
        }
        return false
    }

    private fun isSegmentObstructed(p1: PointF, p2: PointF, center: PointF, minDist: Float): Boolean {
        val l2 = (p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2)
        if (l2 == 0f) return false
        var t = ((center.x - p1.x) * (p2.x - p1.x) + (center.y - p1.y) * (p2.y - p1.y)) / l2
        t = t.coerceIn(0f, 1f)
        val projection = PointF(p1.x + t * (p2.x - p1.x), p1.y + t * (p2.y - p1.y))
        val dist = hypot((center.x - projection.x).toDouble(), (center.y - projection.y).toDouble()).toFloat()
        return dist < minDist
    }

    private fun getLineCircleIntersection(p1: PointF, p2: PointF, circleCenter: PointF, radius: Float): PointF? {
        val dx = p2.x - p1.x; val dy = p2.y - p1.y
        val a = dx * dx + dy * dy
        if (a < 0.0001f) return null
        val b = 2 * (dx * (p1.x - circleCenter.x) + dy * (p1.y - circleCenter.y))
        val c = (p1.x - circleCenter.x).pow(2) + (p1.y - circleCenter.y).pow(2) - radius * radius
        val delta = b * b - 4 * a * c
        if (delta < 0) return null
        val t = (-b - sqrt(delta)) / (2 * a)
        return PointF(p1.x + t * dx, p1.y + t * dy)
    }
}