// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/UpdateStateUseCase.kt

package com.hereliesaz.cuedetat.domain

import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.ui.ZoomMapping
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class UpdateStateUseCase @Inject constructor(
    private val calculateSpinPaths: CalculateSpinPaths,
    private val reducerUtils: ReducerUtils
) {

    private val railHeightToTableHeightRatio = 0.025f // Halved the lift amount
    private val distanceReferenceConstant = 6480f
    private val lineExtensionFactor = 5000f

    operator fun invoke(state: OverlayState): OverlayState {
        if (state.viewWidth == 0 || state.viewHeight == 0) return state

        val updatedStateWithSnapping = state
        val zoomFactor = ZoomMapping.sliderToZoom(state.zoomSliderPosition)

        val finalPitchMatrix = createFullMatrix(state, 0f, zoomFactor)
        val hasFinalInverse = finalPitchMatrix.invert(state.inversePitchMatrix)

        val logicalTableShortSide = state.table.logicalHeight
        val baseRailLiftAmount = logicalTableShortSide * railHeightToTableHeightRatio
        val railLiftAmount = baseRailLiftAmount * abs(sin(Math.toRadians(state.pitchAngle.toDouble()))).toFloat()
        val finalRailPitchMatrix = createFullMatrix(state, railLiftAmount, zoomFactor)

        val flatMatrix = createFullMatrix(state, 0f, 1f, applyPitch = false)

        val consistentRadius = calculateConsistentVisualRadius(state, zoomFactor)


        val logicalShotLineAnchor = getLogicalShotLineAnchor(updatedStateWithSnapping)
        val isTiltBeyondLimit = !state.isBankingMode && logicalShotLineAnchor.y <= updatedStateWithSnapping.protractorUnit.ghostCueBallCenter.y

        val (isGeometricallyImpossible, tangentDirection) = calculateShotPossibilityAndTangent(
            shotAnchor = logicalShotLineAnchor,
            ghostBall = updatedStateWithSnapping.protractorUnit.ghostCueBallCenter,
            targetBall = updatedStateWithSnapping.protractorUnit.center
        )

        val isStraightShot = isShotStraight(logicalShotLineAnchor, updatedStateWithSnapping.protractorUnit.ghostCueBallCenter, updatedStateWithSnapping.protractorUnit.center)
        val isObstructed = checkForObstructions(updatedStateWithSnapping)
        val targetBallDistance = calculateDistance(updatedStateWithSnapping, flatMatrix)
        val shotGuideImpactPoint = if (state.table.isVisible && !state.isBankingMode) {
            calculateShotGuideImpact(updatedStateWithSnapping, useShotGuideLine = true)
        } else {
            null
        }

        // --- Aiming Line & Tangent Line Logic ---
        val tangentStart = updatedStateWithSnapping.protractorUnit.ghostCueBallCenter
        val ghostCueCenter = updatedStateWithSnapping.protractorUnit.ghostCueBallCenter
        val targetCenter = updatedStateWithSnapping.protractorUnit.center

        val (finalAimingLineBankPath, aimedPocketIndex, aimingLineEndPoint) = if (state.table.isVisible && !state.isBankingMode) {
            val (path, pocketIndex, endPoint) = calculateAimAndBank(ghostCueCenter, targetCenter, updatedStateWithSnapping)
            Triple(path, pocketIndex, endPoint)
        } else {
            val extendedPath = getExtendedLinePath(ghostCueCenter, targetCenter)
            Triple(extendedPath, null, extendedPath.last())
        }

        val activeTangentTarget = PointF(
            tangentStart.x - (targetCenter.y - tangentStart.y) * tangentDirection,
            tangentStart.y + (targetCenter.x - tangentStart.x) * tangentDirection
        )

        var (finalTangentLineBankPath, tangentAimedPocketIndex, _) = if (state.table.isVisible && !state.isBankingMode) {
            calculateAimAndBank(tangentStart, activeTangentTarget, updatedStateWithSnapping)
        } else {
            Triple(getExtendedLinePath(tangentStart, activeTangentTarget), null, null)
        }

        if (isStraightShot) {
            tangentAimedPocketIndex = null // A straight shot has no "active" tangent, regardless of pocket alignment.
        }

        val spinPaths: Map<Color, List<PointF>> = if (!state.isBankingMode) {
            calculateSpinPaths(updatedStateWithSnapping)
        } else {
            emptyMap()
        }

        return updatedStateWithSnapping.copy(
            pitchMatrix = finalPitchMatrix,
            railPitchMatrix = finalRailPitchMatrix,
            inversePitchMatrix = state.inversePitchMatrix, // Use the one that was calculated and inverted
            hasInverseMatrix = hasFinalInverse,
            flatMatrix = flatMatrix,
            visualBallRadius = consistentRadius,
            shotLineAnchor = logicalShotLineAnchor,
            isGeometricallyImpossible = isGeometricallyImpossible,
            isStraightShot = isStraightShot,
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
            spinPaths = spinPaths
        )
    }

    private fun createFullMatrix(state: OverlayState, lift: Float, zoom: Float, applyPitch: Boolean = true): Matrix {
        val camera = Camera()

        // 1. Get the 3D transformation matrix (tilt and table rotation).
        val perspectiveMatrix = Perspective.createPerspectiveMatrix(
            currentOrientation = state.currentOrientation,
            tableRotationDegrees = if (state.table.isVisible) state.table.rotationDegrees else 0f,
            camera = camera,
            lift = lift,
            applyPitch = applyPitch
        )

        // 2. Create the world transformation matrix (2D scale only).
        val worldMatrix = Matrix().apply {
            postScale(zoom, zoom)
            // Rotation is now handled in 3D by the camera.
        }

        // 3. Assemble the final matrix with the correct transformation order.
        val finalMatrix = Matrix()
        // a. Apply the world's 2D scale transformation FIRST.
        finalMatrix.set(worldMatrix)
        // b. Apply the camera's 3D transformations.
        finalMatrix.postConcat(perspectiveMatrix)
        // c. Center the entire transformed scene on the view.
        finalMatrix.postTranslate(state.viewWidth / 2f, state.viewHeight / 2f)

        return finalMatrix
    }

    private fun calculateConsistentVisualRadius(state: OverlayState, zoomFactor: Float): Float {
        // Create a matrix with only tilt and zoom to calculate a radius independent of rotation.
        val sizingMatrix = Perspective.createPerspectiveMatrix(
            currentOrientation = state.currentOrientation,
            tableRotationDegrees = 0f, // No rotation for this specific calculation
            camera = Camera(),
            applyPitch = true
        )
        // Apply the 2D zoom scale after the 3D projection is calculated.
        sizingMatrix.postScale(zoomFactor, zoomFactor)

        // Project two points (center and edge) and measure the screen distance.
        val p1 = floatArrayOf(0f, 0f)
        val p2 = floatArrayOf(LOGICAL_BALL_RADIUS, 0f)
        sizingMatrix.mapPoints(p1)
        sizingMatrix.mapPoints(p2)
        return hypot((p1[0] - p2[0]).toDouble(), (p1[1] - p2[1]).toDouble()).toFloat()
    }


    private fun getLogicalShotLineAnchor(state: OverlayState): PointF {
        state.onPlaneBall?.let { return it.center }
        // When no cue ball, anchor to the bottom-center of the screen, transformed to logical space
        if (!state.hasInverseMatrix) {
            // Failsafe if matrix isn't ready
            return PointF(state.protractorUnit.ghostCueBallCenter.x, state.protractorUnit.ghostCueBallCenter.y + 1000f)
        }
        val screenAnchor = PointF(state.viewWidth / 2f, state.viewHeight.toFloat())
        return Perspective.screenToLogical(screenAnchor, state.inversePitchMatrix)
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

    private fun isShotStraight(shotAnchor: PointF, ghostBall: PointF, targetBall: PointF): Boolean {
        val shotAngle = atan2(ghostBall.y - shotAnchor.y, ghostBall.x - shotAnchor.x)
        val aimAngle = atan2(targetBall.y - ghostBall.y, targetBall.x - ghostBall.x)
        var angleDiff = abs(shotAngle - aimAngle)
        if (angleDiff > Math.PI) {
            angleDiff = (2 * Math.PI - angleDiff).toFloat()
        }
        return angleDiff < 0.01 // Use a small tolerance in radians for collinearity
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
        val dirX = end.x - start.x
        val dirY = end.y - start.y
        val mag = sqrt(dirX * dirX + dirY * dirY)
        val extendedEnd = if (mag > 0.001f) {
            val ndx = dirX / mag
            val ndy = dirY / mag
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
        val dirX = end.x - start.x
        val dirY = end.y - start.y
        val mag = sqrt(dirX * dirX + dirY * dirY)
        if (mag < 0.001f) return Pair(null, null)

        val extendedEnd = PointF(start.x + dirX / mag * lineExtensionFactor, start.y + dirY / mag * lineExtensionFactor)

        val pockets = state.table.unrotatedPockets.map { state.table.getRotatedPoint(it) }
        val pocketRadius = state.protractorUnit.radius * 1.8f

        var closestIntersection: PointF? = null
        var closestPocketIndex: Int? = null
        var minDistanceSq = Float.MAX_VALUE

        pockets.forEachIndexed { index, pocket ->
            val intersection = getLineCircleIntersection(start, extendedEnd, pocket, pocketRadius)
            if (intersection != null) {
                val vecToPocketX = pocket.x - start.x
                val vecToPocketY = pocket.y - start.y
                val dotProduct = vecToPocketX * (dirX / mag) + vecToPocketY * (dirY / mag)
                if (dotProduct > 0) {
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

        return state.table.findRailIntersectionAndNormal(p1, PointF(p1.x + dirX * 5000f, p1.y + dirY*5000f))?.first
    }

    private fun calculateSingleBank(start: PointF, end: PointF, state: OverlayState): List<PointF> {
        val dirX = end.x - start.x
        val dirY = end.y - start.y
        val extendedEnd = PointF(start.x + dirX * lineExtensionFactor, start.y + dirY * lineExtensionFactor)

        val intersectionResult = state.table.findRailIntersectionAndNormal(start, extendedEnd) ?: return listOf(start, extendedEnd)
        val (intersectionPoint, railNormal) = intersectionResult

        val incidentVector = PointF(extendedEnd.x - start.x, extendedEnd.y - start.y)
        val reflectedDir = state.table.reflect(incidentVector, railNormal)

        val finalEndPoint = PointF(
            intersectionPoint.x + reflectedDir.x * lineExtensionFactor,
            intersectionPoint.y + reflectedDir.y * lineExtensionFactor
        )

        return listOf(start, intersectionPoint, finalEndPoint)
    }


    private fun checkForObstructions(state: OverlayState): Boolean {
        if (state.obstacleBalls.isEmpty()) return false

        val ballRadius = state.protractorUnit.radius
        val collisionDistance = ballRadius * 2

        val shotLineStart = state.shotLineAnchor
        val shotLineEnd = state.protractorUnit.ghostCueBallCenter
        for (obstacle in state.obstacleBalls) {
            if (isSegmentObstructed(shotLineStart, shotLineEnd, obstacle.center, collisionDistance)) {
                return true
            }
        }

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
        if (l2 == 0f) return false

        var t = ((center.x - p1.x) * (p2.x - p1.x) + (center.y - p1.y) * (p2.y - p1.y)) / l2
        t = t.coerceIn(0f, 1f)

        val projection = PointF(p1.x + t * (p2.x - p1.x), p1.y + t * (p2.y - p1.y))
        val dist = hypot((center.x - projection.x).toDouble(), (center.y - projection.y).toDouble()).toFloat()

        return dist < minDist
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
        // Find the t for the first intersection point along the line's direction.
        val t = (-b - sqrt(delta)) / (2 * a)
        return PointF(p1.x + t * dx, p1.y + t * dy)
    }
}