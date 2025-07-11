// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/UpdateStateUseCase.kt

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

class UpdateStateUseCase @Inject constructor(
    private val calculateSpinPaths: CalculateSpinPaths,
    private val reducerUtils: ReducerUtils
) {

    private val tableToBallRatioShort = 44f
    private val railHeightToTableHeightRatio = 0.05f
    private val distanceReferenceConstant = 6480f

    operator fun invoke(state: OverlayState, camera: Camera): OverlayState {
        if (state.viewWidth == 0 || state.viewHeight == 0) return state

        val basePitchMatrix = Perspective.createPitchMatrix(
            currentOrientation = state.currentOrientation,
            viewWidth = state.viewWidth,
            viewHeight = state.viewHeight,
            camera = camera
        )
        val baseInverseMatrix = Matrix().apply { basePitchMatrix.invert(this) }

        val flatMatrix = Perspective.createPitchMatrix(
            currentOrientation = FullOrientation(0f, 0f, 0f),
            viewWidth = state.viewWidth,
            viewHeight = state.viewHeight,
            camera = camera
        )

        val logicalShotLineAnchor = getLogicalShotLineAnchor(state, baseInverseMatrix)
        val isTiltBeyondLimit = !state.isBankingMode && logicalShotLineAnchor.y <= state.protractorUnit.ghostCueBallCenter.y

        val (isShotImpossible, tangentDirection) = calculateShotPossibilityAndTangent(
            shotAnchor = logicalShotLineAnchor,
            ghostBall = state.protractorUnit.ghostCueBallCenter,
            targetBall = state.protractorUnit.center
        )

        // New obstruction check
        val isObstructed = checkForObstructions(state)
        val isImpossible = isShotImpossible || isObstructed

        val targetBallDistance = calculateDistance(state, flatMatrix)
        var (aimedPocketIndex, aimingLineEndPoint) = if (!state.isBankingMode) {
            checkPocketAim(state)
        } else {
            Pair(null, null)
        }
        val shotGuideImpactPoint = if (state.showTable && !state.isBankingMode) {
            calculateShotGuideImpact(state, useShotGuideLine = true)
        } else {
            null
        }

        var aimingLineBankPath = if (state.showTable && !state.isBankingMode) {
            calculateSingleBank(state.protractorUnit.ghostCueBallCenter, state.protractorUnit.center, state)
        } else {
            emptyList()
        }

        if(aimingLineBankPath.size > 2) {
            val (bankedPocketIndex, intersectionPoint) = checkBankedPocketAim(
                listOf(aimingLineBankPath[1], aimingLineBankPath[2]),
                state
            )
            if (bankedPocketIndex != null && intersectionPoint != null) {
                aimedPocketIndex = bankedPocketIndex
                aimingLineBankPath = listOf(aimingLineBankPath[0], aimingLineBankPath[1], intersectionPoint)
            }
        }

        val tangentLineBankPath = if (state.showTable && !state.isBankingMode) {
            val tangentStart = state.protractorUnit.ghostCueBallCenter
            val tangentTarget = PointF(
                tangentStart.x - (state.protractorUnit.center.y - tangentStart.y) * tangentDirection,
                tangentStart.y + (state.protractorUnit.center.x - tangentStart.x) * tangentDirection
            )
            calculateSingleBank(tangentStart, tangentTarget, state)
        } else {
            emptyList()
        }

        val finalPitchMatrix = Matrix(basePitchMatrix)
        val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
        val logicalTableShortSide = tableToBallRatioShort * referenceRadius
        val baseRailLiftAmount = logicalTableShortSide * railHeightToTableHeightRatio
        val railLiftAmount = baseRailLiftAmount * abs(sin(Math.toRadians(state.pitchAngle.toDouble()))).toFloat()
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

        val spinPaths = if (!state.isBankingMode) {
            calculateSpinPaths(state)
        } else {
            emptyMap()
        }

        return state.copy(
            pitchMatrix = finalPitchMatrix,
            railPitchMatrix = finalRailPitchMatrix,
            inversePitchMatrix = finalInverseMatrix,
            flatMatrix = flatMatrix,
            hasInverseMatrix = hasFinalInverse,
            shotLineAnchor = logicalShotLineAnchor,
            isImpossibleShot = isImpossible,
            isTiltBeyondLimit = isTiltBeyondLimit,
            tangentDirection = tangentDirection,
            targetBallDistance = targetBallDistance,
            aimedPocketIndex = aimedPocketIndex,
            aimingLineEndPoint = aimingLineEndPoint,
            shotGuideImpactPoint = shotGuideImpactPoint,
            aimingLineBankPath = aimingLineBankPath,
            tangentLineBankPath = tangentLineBankPath, // Add to state
            spinPaths = spinPaths
        )
    }

    private fun getLogicalShotLineAnchor(state: OverlayState, inverseMatrix: Matrix): PointF {
        state.onPlaneBall?.let { return it.center }
        val screenAnchor = PointF(state.viewWidth / 2f, state.viewHeight.toFloat())
        return Perspective.screenToLogical(screenAnchor, inverseMatrix)
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

    private fun checkBankedPocketAim(pathSegment: List<PointF>, state: OverlayState): Pair<Int?, PointF?> {
        if (pathSegment.size < 2) return Pair(null, null)
        val start = pathSegment[0]
        val end = pathSegment[1]

        val pockets = TableRenderer.getLogicalPockets(state)
        val pocketRadius = state.protractorUnit.radius * 1.8f
        var closestIntersection: PointF? = null
        var closestPocketIndex: Int? = null
        var minDistanceSq = Float.MAX_VALUE

        pockets.forEachIndexed { index, pocketCenter ->
            val intersection = getLineCircleIntersection(start, end, pocketCenter, pocketRadius)
            if (intersection != null) {
                val dxSegment = end.x - start.x
                val dySegment = end.y - start.y
                val t = if (abs(dxSegment) > abs(dySegment)) {
                    (intersection.x - start.x) / dxSegment
                } else {
                    if (abs(dySegment) > 0.001f) (intersection.y - start.y) / dySegment else 0f
                }

                if (t in 0.0f..1.0f) {
                    val distSq = (intersection.x - start.x).pow(2) + (intersection.y - start.y).pow(2)
                    if (distSq < minDistanceSq) {
                        minDistanceSq = distSq
                        closestIntersection = intersection
                        closestPocketIndex = index
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

        return reducerUtils.findRailIntersectionAndNormal(p1, PointF(p1.x + dirX, p1.y + dirY), state)?.first
    }

    private fun calculateSingleBank(start: PointF, end: PointF, state: OverlayState): List<PointF> {
        val intersectionResult = reducerUtils.findRailIntersectionAndNormal(start, end, state) ?: return emptyList()
        val (intersectionPoint, railNormal) = intersectionResult

        val incidentVector = PointF(end.x - start.x, end.y - start.y)
        val reflectedDir = reducerUtils.reflect(incidentVector, railNormal)
        val reflectedEnd = reducerUtils.findRailIntersectionAndNormal(intersectionPoint, PointF(intersectionPoint.x + reflectedDir.x, intersectionPoint.y + reflectedDir.y), state)?.first ?: intersectionPoint

        return listOf(start, intersectionPoint, reflectedEnd)
    }

    private fun checkForObstructions(state: OverlayState): Boolean {
        if (state.obstacleBalls.isEmpty()) return false

        val ballRadius = state.protractorUnit.radius
        val collisionDistance = ballRadius * 2

        // Path 1: Cue Ball to Ghost Ball (Shot Guide Line)
        val shotLineStart = state.shotLineAnchor
        val shotLineEnd = state.protractorUnit.ghostCueBallCenter
        for (obstacle in state.obstacleBalls) {
            if (isSegmentObstructed(shotLineStart, shotLineEnd, obstacle.center, collisionDistance)) {
                return true
            }
        }

        // Path 2: Ghost Ball to Target Ball (Aiming Line)
        val aimingLineStart = state.protractorUnit.ghostCueBallCenter
        val aimingLineEnd = state.protractorUnit.center
        for (obstacle in state.obstacleBalls) {
            if (isSegmentObstructed(aimingLineStart, aimingLineEnd, obstacle.center, collisionDistance)) {
                return true
            }
        }

        return false
    }

    private fun isSegmentObstructed(p1: PointF, p2: PointF, center: PointF, minDist: Float): Boolean {
        val l2 = (p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2)
        if (l2 == 0f) return false // Line segment has zero length

        // Project center point onto the line
        var t = ((center.x - p1.x) * (p2.x - p1.x) + (center.y - p1.y) * (p2.y - p1.y)) / l2
        t = t.coerceIn(0f, 1f) // Clamp projection to the segment

        val projection = PointF(p1.x + t * (p2.x - p1.x), p1.y + t * (p2.y - p1.y))
        val dist = hypot((center.x - projection.x).toDouble(), (center.y - projection.y).toDouble()).toFloat()

        return dist < minDist
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
        if (a < 0.0001f) return null
        val b = 2 * (dx * (p1.x - circleCenter.x) + dy * (p1.y - circleCenter.y))
        val c = (p1.x - circleCenter.x).pow(2) + (p1.y - circleCenter.y).pow(2) - radius * radius
        val delta = b * b - 4 * a * c
        if (delta < 0) return null
        val t = (-b - sqrt(delta)) / (2 * a)
        return PointF(p1.x + t * dx, p1.y + t * dy)
    }
}