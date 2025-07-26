// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/UpdateStateUseCase.kt

package com.hereliesaz.cuedetat.domain

import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.ExperienceMode
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Defines the level of recalculation needed for a state update. */
enum class UpdateType {
    /** Recalculate everything: matrices, aiming, spin paths. */
    FULL,

    /** Recalculate aiming and spin paths; matrices are assumed to be current. */
    AIMING,

    /**
     * Recalculate only a spin path; matrices and aiming are assumed to be
     * current.
     */
    SPIN_ONLY
}

class UpdateStateUseCase @Inject constructor(
    private val calculateSpinPaths: CalculateSpinPaths,
    private val reducerUtils: ReducerUtils,
    private val calculateBankShot: CalculateBankShot
) {

    private val railHeightToTableHeightRatio = 0.025f
    private val distanceReferenceConstant = 1200f
    private val lineExtensionFactor = 5000f

    /**
     * The main entry point for updating the derived state.
     *
     * @param state The current state after being processed by a reducer.
     * @param type The type of update to perform, determining which
     *    calculations are necessary.
     * @return A new state object with all derived properties recalculated as
     *    needed.
     */
    operator fun invoke(state: OverlayState, type: UpdateType): OverlayState {
        if (state.viewWidth == 0 || state.viewHeight == 0) return state

        val stateAfterMatrices = if (type == UpdateType.FULL) {
            updateMatricesAndTransforms(state)
        } else {
            state
        }

        val stateAfterAiming = if (type == UpdateType.FULL || type == UpdateType.AIMING) {
            updateAimingCalculations(stateAfterMatrices)
        } else {
            stateAfterMatrices
        }

        return updateSpinCalculations(stateAfterAiming)
    }

    /**
     * Recalculates all perspective and projection matrices. This is the
     * most expensive part of the update and should only be run when
     * view dimensions, zoom, pan, pitch, or table rotation change.
     */
    private fun updateMatricesAndTransforms(state: OverlayState): OverlayState {
        val (minZoom, maxZoom) = ZoomMapping.getZoomRange(
            state.experienceMode,
            state.isBeginnerViewLocked
        )
        val zoomFactor = ZoomMapping.sliderToZoom(state.zoomSliderPosition, minZoom, maxZoom)
        val isPitchApplied =
            state.experienceMode != ExperienceMode.BEGINNER || !state.isBeginnerViewLocked


        // Enforce the pan limit here, where we have full context.
        val screenSpaceLimit = (state.table.logicalHeight / 2f) * zoomFactor
        val coercedOffsetY = state.viewOffset.y.coerceIn(-screenSpaceLimit, screenSpaceLimit)
        val coercedViewOffset = PointF(state.viewOffset.x, coercedOffsetY)
        val stateWithCoercedPan = state.copy(viewOffset = coercedViewOffset)

        val camera = Camera()

        val perspectiveMatrix = Perspective.createPerspectiveMatrix(
            currentOrientation = stateWithCoercedPan.currentOrientation,
            worldRotationDegrees = stateWithCoercedPan.worldRotationDegrees,
            camera = camera,
            lift = 0f,
            applyPitch = isPitchApplied
        )

        val finalPitchMatrix = createFullMatrix(stateWithCoercedPan, zoomFactor, perspectiveMatrix)
        val inverseMatrix = Matrix()
        val hasFinalInverse = finalPitchMatrix.invert(inverseMatrix)

        val logicalTableShortSide = stateWithCoercedPan.table.logicalHeight
        val baseRailLiftAmount = logicalTableShortSide * railHeightToTableHeightRatio
        val railLiftAmount =
            baseRailLiftAmount * abs(sin(Math.toRadians(stateWithCoercedPan.pitchAngle.toDouble()))).toFloat()

        val railPerspectiveMatrix = Perspective.createPerspectiveMatrix(
            currentOrientation = stateWithCoercedPan.currentOrientation,
            worldRotationDegrees = stateWithCoercedPan.worldRotationDegrees,
            camera = camera,
            lift = railLiftAmount,
            applyPitch = isPitchApplied
        )
        val finalRailPitchMatrix =
            createFullMatrix(stateWithCoercedPan, zoomFactor, railPerspectiveMatrix)

        val flatPerspectiveMatrix = Perspective.createPerspectiveMatrix(
            currentOrientation = stateWithCoercedPan.currentOrientation,
            worldRotationDegrees = stateWithCoercedPan.worldRotationDegrees,
            camera = camera,
            applyPitch = false
        )
        val flatMatrix = createFullMatrix(stateWithCoercedPan, 1f, flatPerspectiveMatrix)

        val logicalPlanePerspectiveMatrix = Perspective.createPerspectiveMatrix(
            currentOrientation = stateWithCoercedPan.currentOrientation,
            worldRotationDegrees = stateWithCoercedPan.worldRotationDegrees,
            camera = camera,
            applyPitch = false
        )
        val logicalPlaneMatrix =
            createFullMatrix(stateWithCoercedPan, zoomFactor, logicalPlanePerspectiveMatrix)


        // Create a separate matrix for size calculation that EXCLUDES world rotation
        // to prevent perspective distortion from affecting the apparent size of objects as they rotate.
        val sizeCalculationPerspectiveMatrix =
            Perspective.createPerspectiveMatrix(
                currentOrientation = state.currentOrientation,
                worldRotationDegrees = 0f, // No rotation for sizing
                camera = camera,
                lift = 0f,
                applyPitch = isPitchApplied
            )
        val sizeCalculationMatrix =
            createFullMatrix(state, zoomFactor, sizeCalculationPerspectiveMatrix)


        return stateWithCoercedPan.copy(
            pitchMatrix = finalPitchMatrix,
            railPitchMatrix = finalRailPitchMatrix,
            sizeCalculationMatrix = sizeCalculationMatrix,
            inversePitchMatrix = inverseMatrix,
            hasInverseMatrix = hasFinalInverse,
            flatMatrix = flatMatrix,
            logicalPlaneMatrix = logicalPlaneMatrix
        )
    }

    /**
     * Recalculates all aiming-related properties: shot possibility,
     * obstructions, line paths, banking, etc. Assumes matrices are up-to-date.
     */
    private fun updateAimingCalculations(state: OverlayState): OverlayState {
        if (state.isBankingMode) {
            val bankResult = calculateBankShot(state)
            return state.copy(
                bankShotPath = bankResult.path,
                pocketedBankShotPocketIndex = bankResult.pocketedPocketIndex,
                // Reset protractor-mode-specific flags
                isGeometricallyImpossible = false,
                isObstructed = false,
                isTiltBeyondLimit = false,
                warningText = null
            )
        }

        if (state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked) {
            return state.copy(
                isGeometricallyImpossible = false,
                isObstructed = false,
                isTiltBeyondLimit = false,
                warningText = null,
                isStraightShot = true // It is always straight in this mode
            )
        }

        val logicalShotLineAnchor =
            getLogicalShotLineAnchor(state) ?: return state // Can't aim without an anchor

        val isTiltBeyondLimit =
            !state.isBankingMode && logicalShotLineAnchor.y <= state.protractorUnit.ghostCueBallCenter.y

        val (isGeometricallyImpossible, tangentDirection) = calculateShotPossibilityAndTangent(
            shotAnchor = logicalShotLineAnchor,
            ghostBall = state.protractorUnit.ghostCueBallCenter,
            targetBall = state.protractorUnit.center
        )

        val isStraightShot = isShotStraight(
            logicalShotLineAnchor,
            state.protractorUnit.ghostCueBallCenter,
            state.protractorUnit.center
        )
        val isObstructed = checkForObstructions(state)
        // HERESY CORRECTED: Use the final pitchMatrix for distance calculation.
        val targetBallDistance = calculateDistance(state, state.pitchMatrix ?: Matrix())
        val shotGuideImpactPoint = if (state.table.isVisible && !state.isBankingMode) {
            calculateShotGuideImpact(state, useShotGuideLine = true)
        } else {
            null
        }

        val tangentStart = state.protractorUnit.ghostCueBallCenter
        val ghostCueCenter = state.protractorUnit.ghostCueBallCenter
        val targetCenter = state.protractorUnit.center

        val (finalAimingLineBankPath, aimedPocketIndex, aimingLineEndPoint) = if (state.table.isVisible && !state.isBankingMode) {
            val (path, pocketIndex, endPoint) = calculateAimAndBank(
                ghostCueCenter,
                targetCenter,
                state
            )
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
            calculateAimAndBank(tangentStart, activeTangentTarget, state)
        } else {
            Triple(getExtendedLinePath(tangentStart, activeTangentTarget), null, null)
        }

        if (isStraightShot) {
            tangentAimedPocketIndex = null
        }

        return state.copy(
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
            tangentLineBankPath = finalTangentLineBankPath
        )
    }

    /**
     * Recalculates only the spin path visualization. This is the least
     * expensive update.
     */
    private fun updateSpinCalculations(state: OverlayState): OverlayState {
        val spinPaths: Map<Color, List<PointF>> = if (!state.isBankingMode) {
            calculateSpinPaths(state)
        } else {
            emptyMap()
        }
        return state.copy(spinPaths = spinPaths)
    }

    private fun createFullMatrix(
        state: OverlayState,
        zoom: Float,
        perspectiveMatrix: Matrix,
    ): Matrix {
        val worldMatrix = Matrix().apply {
            postScale(zoom, zoom)
        }

        val finalMatrix = Matrix()
        finalMatrix.set(worldMatrix)
        finalMatrix.postConcat(perspectiveMatrix)
        finalMatrix.postTranslate(
            (state.viewWidth / 2f) + state.viewOffset.x,
            (state.viewHeight / 2f) + state.viewOffset.y
        )

        return finalMatrix
    }

    private fun calculateDistance(state: OverlayState, matrix: Matrix): Float {
        val (logicalCenter, logicalRadius) = if (state.isBankingMode && state.onPlaneBall != null) {
            state.onPlaneBall.center to state.onPlaneBall.radius
        } else {
            state.protractorUnit.center to state.protractorUnit.radius
        }
        val screenRadius = DrawingUtils.getPerspectiveRadiusAndLift(
            logicalCenter, logicalRadius, state, matrix
        ).radius
        return if (screenRadius > 0) distanceReferenceConstant / screenRadius else 0f
    }

    private fun getLogicalShotLineAnchor(state: OverlayState): PointF? {
        state.onPlaneBall?.let { return it.center }
        val inverseMatrix = state.inversePitchMatrix ?: return null
        val screenAnchor = PointF(state.viewWidth / 2f, state.viewHeight.toFloat())
        return Perspective.screenToLogical(screenAnchor, inverseMatrix)
    }

    private fun calculateShotPossibilityAndTangent(
        shotAnchor: PointF,
        ghostBall: PointF,
        targetBall: PointF
    ): Pair<Boolean, Float> {
        val aimingAngle = atan2(targetBall.y - ghostBall.y, targetBall.x - ghostBall.x)
        val distToGhostSq =
            (ghostBall.y - shotAnchor.y).pow(2) + (ghostBall.x - shotAnchor.x).pow(2)
        val distToTargetSq =
            (targetBall.y - shotAnchor.y).pow(2) + (targetBall.x - shotAnchor.x).pow(2)
        val isImpossible = distToGhostSq > distToTargetSq
        val shotAngle = atan2(ghostBall.y - shotAnchor.y, ghostBall.x - shotAnchor.x)
        var angleDifference =
            Math.toDegrees(aimingAngle.toDouble() - shotAngle.toDouble()).toFloat()
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

    private fun calculateAimAndBank(
        start: PointF,
        end: PointF,
        state: OverlayState
    ): Triple<List<PointF>, Int?, PointF> {
        val (directPocketIndex, directIntersection) = checkPocketAim(start, end, state)
        if (directPocketIndex != null && directIntersection != null) {
            return Triple(listOf(start, directIntersection), directPocketIndex, directIntersection)
        }

        val bankPath = calculateSingleBank(start, end, state)
        if (bankPath.size > 2) {
            val (bankedPocketIndex, bankedIntersection) = checkPocketAim(
                bankPath[1],
                bankPath[2],
                state
            )
            if (bankedPocketIndex != null && bankedIntersection != null) {
                return Triple(
                    listOf(bankPath[0], bankPath[1], bankedIntersection),
                    bankedPocketIndex,
                    bankedIntersection
                )
            }
        }
        return Triple(bankPath, null, bankPath.last())
    }

    private fun checkPocketAim(
        start: PointF,
        end: PointF,
        state: OverlayState
    ): Pair<Int?, PointF?> {
        val dirX = end.x - start.x
        val dirY = end.y - start.y
        val mag = sqrt(dirX * dirX + dirY * dirY)
        if (mag < 0.001f) return Pair(null, null)

        val extendedEnd = PointF(
            start.x + dirX / mag * lineExtensionFactor,
            start.y + dirY / mag * lineExtensionFactor
        )

        val pockets = state.table.pockets
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
                    val distSq =
                        (intersection.x - start.x).pow(2) + (intersection.y - start.y).pow(2)
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

    private fun calculateShotGuideImpact(
        state: OverlayState,
        useShotGuideLine: Boolean = false
    ): PointF? {
        val p1 = if (useShotGuideLine) state.shotLineAnchor
            ?: return null else state.protractorUnit.center
        val p2 = state.protractorUnit.ghostCueBallCenter
        val dirX = p2.x - p1.x
        val dirY = p2.y - p1.y

        return state.table.findRailIntersectionAndNormal(
            p1,
            PointF(p1.x + dirX * 5000f, p1.y + dirY * 5000f)
        )?.first
    }

    private fun calculateSingleBank(start: PointF, end: PointF, state: OverlayState): List<PointF> {
        val dirX = end.x - start.x
        val dirY = end.y - start.y
        val extendedEnd =
            PointF(start.x + dirX * lineExtensionFactor, start.y + dirY * lineExtensionFactor)

        val intersectionResult =
            state.table.findRailIntersectionAndNormal(start, extendedEnd) ?: return listOf(
                start,
                extendedEnd
            )
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

        val shotLineStart = state.shotLineAnchor ?: return false
        val shotLineEnd = state.protractorUnit.ghostCueBallCenter
        for (obstacle in state.obstacleBalls) {
            if (isSegmentObstructed(
                    shotLineStart,
                    shotLineEnd,
                    obstacle.center,
                    collisionDistance
                )
            ) {
                return true
            }
        }

        val aimingLineStart = state.protractorUnit.ghostCueBallCenter
        val aimingLineEnd = state.protractorUnit.center
        for (obstacle in state.obstacleBalls) {
            if (isSegmentObstructed(
                    aimingLineStart,
                    aimingLineEnd,
                    obstacle.center,
                    collisionDistance
                )
            ) {
                return true
            }
        }

        return false
    }

    private fun isSegmentObstructed(
        p1: PointF,
        p2: PointF,
        center: PointF,
        minDist: Float
    ): Boolean {
        val l2 = (p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2)
        if (l2 == 0f) return false

        var t = ((center.x - p1.x) * (p2.x - p1.x) + (center.y - p1.y) * (p2.y - p1.y)) / l2
        t = t.coerceIn(0f, 1f)

        val projection = PointF(p1.x + t * (p2.x - p1.x), p1.y + t * (p2.y - p1.y))
        val dist = hypot(
            (center.x - projection.x).toDouble(),
            (center.y - projection.y).toDouble()
        ).toFloat()

        return dist < minDist
    }

    private fun getLineCircleIntersection(
        p1: PointF,
        p2: PointF,
        circleCenter: PointF,
        radius: Float
    ): PointF? {
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