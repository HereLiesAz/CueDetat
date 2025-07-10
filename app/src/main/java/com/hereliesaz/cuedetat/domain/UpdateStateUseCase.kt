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

        // Calculate single bank for aiming line in ghost mode
        var aimingLineBankPath = if (state.showTable && !state.isBankingMode) {
            calculateSingleBank(state.protractorUnit.ghostCueBallCenter, state.protractorUnit.center, state)
        } else {
            emptyList()
        }

        // Check if the banked aiming line goes into a pocket
        if(aimingLineBankPath.size > 2) {
            val (bankedPocketIndex, intersectionPoint) = checkBankedPocketAim(
                listOf(aimingLineBankPath[1], aimingLineBankPath[2]),
                state
            )
            if (bankedPocketIndex != null && intersectionPoint != null) {
                aimedPocketIndex = bankedPocketIndex
                // Truncate the path to the intersection point
                aimingLineBankPath = listOf(aimingLineBankPath[0], aimingLineBankPath[1], intersectionPoint)
            }
        }


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
            shotGuideImpactPoint = shotGuideImpactPoint,
            aimingLineBankPath = aimingLineBankPath
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
        val aimingAngle = atan2(targetBall.y - ghostBall.y, targetBall.x - ghostBall.x)

        // The new righteousness: Distance-based impossibility check.
        val distToGhostSq = (ghostBall.y - shotAnchor.y).pow(2) + (ghostBall.x - shotAnchor.x).pow(2)
        val distToTargetSq = (targetBall.y - shotAnchor.y).pow(2) + (targetBall.x - shotAnchor.x).pow(2)
        val isImpossible = distToGhostSq > distToTargetSq

        // Tangent direction is still based on the angle for visual cues.
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
                // Check if intersection is on the segment
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

        return findRailIntersection(p1, dirX, dirY, state)
    }

    private fun calculateSingleBank(start: PointF, end: PointF, state: OverlayState): List<PointF> {
        val dirX = end.x - start.x
        val dirY = end.y - start.y
        val intersection = findRailIntersection(start, dirX, dirY, state) ?: return emptyList()

        val reflectedDir = reflect(PointF(dirX, dirY), intersection, state)
        val reflectedEnd = findRailIntersection(intersection, reflectedDir.x, reflectedDir.y, state) ?: intersection

        return listOf(start, intersection, reflectedEnd)
    }

    private fun findRailIntersection(startPoint: PointF, dirX: Float, dirY: Float, state: OverlayState): PointF? {
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
            val tLeft = (left - startPoint.x) / dirX
            val tRight = (right - startPoint.x) / dirX
            if (tLeft > 0.001f && tLeft < t) t = tLeft
            if (tRight > 0.001f && tRight < t) t = tRight
        }
        if (dirY != 0f) {
            val tTop = (top - startPoint.y) / dirY
            val tBottom = (bottom - startPoint.y) / dirY
            if (tTop > 0.001f && tTop < t) t = tTop
            if (tBottom > 0.001f && tBottom < t) t = tBottom
        }

        return if (t != Float.MAX_VALUE) {
            PointF(startPoint.x + t * dirX, startPoint.y + t * dirY)
        } else {
            null
        }
    }

    private fun reflect(v: PointF, p: PointF, state: OverlayState): PointF {
        val n = getRailNormal(p, state) ?: PointF(0f, 0f)
        val dot = v.x * n.x + v.y * n.y
        return PointF(v.x - 2 * dot * n.x, v.y - 2 * dot * n.y)
    }

    private fun getRailNormal(point: PointF, state: OverlayState): PointF? {
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

        val tolerance = 5f

        return when {
            abs(point.y - top) < tolerance -> PointF(0f, 1f)
            abs(point.y - bottom) < tolerance -> PointF(0f, -1f)
            abs(point.x - left) < tolerance -> PointF(1f, 0f)
            abs(point.x - right) < tolerance -> PointF(-1f, 0f)
            else -> null
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
        if (a < 0.0001f) return null
        val b = 2 * (dx * (p1.x - circleCenter.x) + dy * (p1.y - circleCenter.y))
        val c = (p1.x - circleCenter.x).pow(2) + (p1.y - circleCenter.y).pow(2) - radius * radius
        val delta = b * b - 4 * a * c
        if (delta < 0) return null
        val t = (-b - sqrt(delta)) / (2 * a)
        return PointF(p1.x + t * dx, p1.y + t * dy)
    }
}