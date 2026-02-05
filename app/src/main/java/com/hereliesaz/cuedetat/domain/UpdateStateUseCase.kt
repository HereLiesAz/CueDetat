// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/UpdateStateUseCase.kt

package com.hereliesaz.cuedetat.domain

import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Defines the scope of recalculation needed for a state update.
 * Optimizing these updates is crucial for keeping the UI responsive (60fps).
 */
enum class UpdateType {
    /**
     * Recalculate EVERYTHING.
     * Use this when the view dimensions, zoom, or device orientation changes.
     * This rebuilds the expensive transformation matrices.
     */
    FULL,

    /**
     * Recalculate aiming geometry and spin physics.
     * Use this when the ghost ball or cue ball moves, but the camera/table hasn't moved.
     * Reuses existing matrices.
     */
    AIMING,

    /**
     * Recalculate ONLY spin physics.
     * Use this when the spin control is adjusted.
     * Reuses aiming lines and matrices.
     */
    SPIN_ONLY
}

/**
 * The core domain logic for the application.
 *
 * This UseCase is responsible for taking the current state and a trigger event (UpdateType)
 * and producing the *next* valid state. It acts as the central "Reducer" logic for
 * geometric and physical calculations.
 *
 * Responsibilities:
 * 1.  Matrix Generation: Creating the 3D projection matrices based on sensor data.
 * 2.  Aiming Logic: Calculating where the ball will go (Ghost Ball -> Target -> Pocket).
 * 3.  Bank Shots: calculating reflections off rails.
 * 4.  Spin Physics: Delegating to [CalculateSpinPaths] to compute curved trajectories.
 */
class UpdateStateUseCase @Inject constructor(
    private val calculateSpinPaths: CalculateSpinPaths,
    private val reducerUtils: ReducerUtils,
    private val calculateBankShot: CalculateBankShot
) {

    // Constants for visual tuning.
    // The rails are drawn slightly higher than the table surface to simulate 3D depth.
    private val railHeightToTableHeightRatio = 0.025f
    // Used to scale the "distance" text for accessibility.
    private val distanceReferenceConstant = 1200f
    // A large number to extend lines "infinitely" (or at least off-screen).
    private val lineExtensionFactor = 5000f

    /**
     * The main entry point.
     *
     * @param state The current immutable state.
     * @param type The level of update required.
     * @return A new State instance with updated calculations.
     */
    operator fun invoke(state: CueDetatState, type: UpdateType): CueDetatState {
        // Safety check: Cannot calculate projection if view has no size.
        if (state.viewWidth == 0 || state.viewHeight == 0) return state

        // PHASE 1: Matrix Updates (Expensive)
        // Only run if the view or camera definition has changed.
        val stateAfterMatrices = if (type == UpdateType.FULL) {
            updateMatricesAndTransforms(state)
        } else {
            state
        }

        // PHASE 2: Aiming Geometry (Moderate)
        // Run if matrices changed OR if balls moved.
        val stateAfterAiming = if (type == UpdateType.FULL || type == UpdateType.AIMING) {
            updateAimingCalculations(stateAfterMatrices)
        } else {
            stateAfterMatrices
        }

        // PHASE 3: Spin Physics (Fast-ish)
        // Always run if anything above changed, or if just spin changed.
        return updateSpinCalculations(stateAfterAiming)
    }

    /**
     * Rebuilds the transformation matrices that map Logical Space (Inches) to Screen Space (Pixels).
     */
    private fun updateMatricesAndTransforms(state: CueDetatState): CueDetatState {
        // 1. Zoom Calculation.
        // Map the linear slider (0.0 - 1.0) to a logarithmic zoom factor.
        val (minZoom, maxZoom) = ZoomMapping.getZoomRange(
            state.experienceMode,
            state.isBeginnerViewLocked
        )
        val zoomFactor = ZoomMapping.sliderToZoom(state.zoomSliderPosition, minZoom, maxZoom)

        // 2. Pitch/Tilt Logic.
        // Beginners usually get a locked top-down view (Pitch disabled).
        val isPitchApplied =
            state.experienceMode != ExperienceMode.BEGINNER || !state.isBeginnerViewLocked

        // 3. Pan Coercion.
        // Prevent the user from panning the table completely off-screen.
        val screenSpaceLimit = (state.table.logicalHeight / 2f) * zoomFactor
        val coercedOffsetY = state.viewOffset.y.coerceIn(-screenSpaceLimit, screenSpaceLimit)
        val coercedViewOffset = PointF(state.viewOffset.x, coercedOffsetY)
        val stateWithCoercedPan = state.copy(viewOffset = coercedViewOffset)

        // 4. Create the Perspective Matrix.
        val camera = Camera() // Allocate a temporary Camera object for 3D math.
        val perspectiveMatrix = Perspective.createPerspectiveMatrix(
            currentOrientation = stateWithCoercedPan.currentOrientation,
            camera = camera,
            lift = 0f, // Table surface is at Z=0
            applyPitch = isPitchApplied
        )

        // Combine Zoom + Rotation + Perspective + Pan into the final matrix.
        val finalPitchMatrix = createFullMatrix(stateWithCoercedPan, zoomFactor, perspectiveMatrix)

        // Calculate Inverse for touch handling (Screen -> Logical).
        val inverseMatrix = Matrix()
        val hasFinalInverse = finalPitchMatrix.invert(inverseMatrix)

        // 5. Create Rail Matrices (The "Lift" Effect).
        // Rails sit higher than the table. We create a separate matrix for them with a 'lift'.
        // The lift amount increases with pitch to simulate parallax.
        val logicalTableShortSide = stateWithCoercedPan.table.logicalHeight
        val baseRailLiftAmount = logicalTableShortSide * railHeightToTableHeightRatio
        val railLiftAmount =
            baseRailLiftAmount * abs(sin(Math.toRadians(stateWithCoercedPan.pitchAngle.toDouble()))).toFloat()

        val railPerspectiveMatrix = Perspective.createPerspectiveMatrix(
            currentOrientation = stateWithCoercedPan.currentOrientation,
            camera = camera,
            lift = railLiftAmount,
            applyPitch = isPitchApplied
        )
        val finalRailPitchMatrix =
            createFullMatrix(stateWithCoercedPan, zoomFactor, railPerspectiveMatrix)

        // 6. Auxiliary Matrices (Flat/Debug).
        val flatPerspectiveMatrix = Perspective.createPerspectiveMatrix(
            currentOrientation = stateWithCoercedPan.currentOrientation,
            camera = camera,
            applyPitch = false
        )
        val flatMatrix = createFullMatrix(stateWithCoercedPan, 1f, flatPerspectiveMatrix)
        val logicalPlaneMatrix = createFullMatrix(stateWithCoercedPan, zoomFactor, flatPerspectiveMatrix)
        val sizeCalculationMatrix = createFullMatrix(state, zoomFactor, perspectiveMatrix)

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
     * Calculates aiming lines, ghost ball positions, and bank shots.
     */
    private fun updateAimingCalculations(state: CueDetatState): CueDetatState {
        // CASE: Banking Mode (Specialized logic).
        if (state.isBankingMode) {
            val bankResult = calculateBankShot(state)
            return state.copy(
                bankShotPath = bankResult.path,
                pocketedBankShotPocketIndex = bankResult.pocketedPocketIndex,
                isGeometricallyImpossible = false,
                isObstructed = false,
                isTiltBeyondLimit = false,
                warningText = null
            )
        }

        // CASE: Beginner View (Simplified).
        if (state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked) {
            val ghostCueCenter = state.protractorUnit.ghostCueBallCenter
            val targetCenter = state.protractorUnit.center
            val extendedPath = getExtendedLinePath(ghostCueCenter, targetCenter)
            return state.copy(
                isGeometricallyImpossible = false,
                isObstructed = false,
                isTiltBeyondLimit = false,
                warningText = null,
                isStraightShot = true,
                aimingLineBankPath = extendedPath,
                aimingLineEndPoint = extendedPath.last()
            )
        }

        // STANDARD MODE CALCULATIONS

        // 1. Determine "Shot Anchor".
        // This is the Cue Ball position. It might be detected by CV (onPlaneBall) or assumed fixed.
        val logicalShotLineAnchor = getLogicalShotLineAnchor(state) ?: return state

        // 2. Validate Shot Geometry.
        // If the cue ball is "below" the ghost ball (relative to aim), it's a back cut or impossible.
        val isTiltBeyondLimit =
            !state.isBankingMode && logicalShotLineAnchor.y <= state.protractorUnit.ghostCueBallCenter.y

        // Check if the shot angle makes sense (not shooting through the ball).
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

        // 3. Obstruction Detection.
        // Check if any other detected balls block the path.
        val isObstructed = checkForObstructions(state)

        // 4. Accessibility Data.
        val targetBallDistance = calculateDistance(state, state.pitchMatrix ?: Matrix())

        // 5. Shot Guide (The "Helper" Line).
        val shotGuideImpactPoint = if (state.table.isVisible && !state.isBankingMode) {
            calculateShotGuideImpact(state, useShotGuideLine = true)
        } else {
            null
        }

        // 6. Tangent Lines (90-degree separation).
        val tangentStart = state.protractorUnit.ghostCueBallCenter
        val ghostCueCenter = state.protractorUnit.ghostCueBallCenter
        val targetCenter = state.protractorUnit.center

        // Calculate Aiming Line (Ghost Ball -> Pocket/Rail).
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

        // Calculate Tangent Line (Perpendicular to shot).
        // Tangent direction depends on cut angle (left or right).
        val activeTangentTarget = PointF(
            tangentStart.x - (targetCenter.y - tangentStart.y) * tangentDirection,
            tangentStart.y + (targetCenter.x - tangentStart.x) * tangentDirection
        )

        var (finalTangentLineBankPath, tangentAimedPocketIndex, _) = if (state.table.isVisible && !state.isBankingMode) {
            calculateAimAndBank(tangentStart, activeTangentTarget, state)
        } else {
            Triple(getExtendedLinePath(tangentStart, activeTangentTarget), null, null)
        }

        // Tangent line doesn't exist on straight shots.
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

    private fun updateSpinCalculations(state: CueDetatState): CueDetatState {
        val spinPaths: Map<Color, List<PointF>> = if (!state.isBankingMode) {
            calculateSpinPaths(state)
        } else {
            emptyMap()
        }
        return state.copy(spinPaths = spinPaths)
    }

    /**
     * Helper to composite Zoom + Rotation + Perspective + Translation.
     */
    private fun createFullMatrix(
        state: CueDetatState,
        zoom: Float,
        perspectiveMatrix: Matrix
    ): Matrix {
        val centerX = state.viewWidth / 2f
        val centerY = state.viewHeight / 2f

        // 1. World Matrix: Scales and rotates the logical world (2D).
        val worldMatrix = Matrix().apply {
            postScale(zoom, zoom)
            postRotate(state.worldRotationDegrees)
            postTranslate(state.viewOffset.x, state.viewOffset.y)
        }

        // 2. Final Matrix: Applies World Transform -> 3D Perspective -> Screen Center Offset.
        val finalMatrix = Matrix()
        finalMatrix.set(worldMatrix)
        finalMatrix.postConcat(perspectiveMatrix)
        finalMatrix.postTranslate(centerX, centerY)
        return finalMatrix
    }

    private fun calculateDistance(state: CueDetatState, matrix: Matrix): Float {
        val (logicalCenter, logicalRadius) = if (state.isBankingMode && state.onPlaneBall != null) {
            state.onPlaneBall.center to state.onPlaneBall.radius
        } else {
            state.protractorUnit.center to state.protractorUnit.radius
        }
        // Calculate how big the ball looks on screen.
        val screenRadius = DrawingUtils.getPerspectiveRadiusAndLift(
            logicalCenter,
            logicalRadius,
            state,
            matrix
        ).radius
        // Invert radius to estimate distance (Bigger = Closer).
        return if (screenRadius > 0) distanceReferenceConstant / screenRadius else 0f
    }

    private fun getLogicalShotLineAnchor(state: CueDetatState): PointF? {
        state.onPlaneBall?.let { return it.center }
        // Fallback: If no ball detected, assume user is shooting from bottom center of screen.
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

        // Impossible if Ghost Ball is FURTHER away than Target Ball.
        val isImpossible = distToGhostSq > distToTargetSq

        val shotAngle = atan2(ghostBall.y - shotAnchor.y, ghostBall.x - shotAnchor.x)
        var angleDifference =
            Math.toDegrees(aimingAngle.toDouble() - shotAngle.toDouble()).toFloat()

        // Normalize angle to -180..180
        while (angleDifference <= -180) angleDifference += 360
        while (angleDifference > 180) angleDifference -= 360

        // Determine cut direction.
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
        return angleDiff < 0.01 // Threshold for "Straightness"
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

    /**
     * Calculates single-bank shots.
     * Traces the line from start->end, checks for rail collision, and calculates reflection.
     */
    private fun calculateAimAndBank(
        start: PointF,
        end: PointF,
        state: CueDetatState
    ): Triple<List<PointF>, Int?, PointF> {
        // 1. Check direct pocket entry.
        val (directPocketIndex, directIntersection) = checkPocketAim(start, end, state)
        if (directPocketIndex != null && directIntersection != null) {
            return Triple(listOf(start, directIntersection), directPocketIndex, directIntersection)
        }

        // 2. Check Bank.
        val bankPath = calculateSingleBank(start, end, state)

        // 3. Check banked pocket entry.
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
        state: CueDetatState
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
        // Increase pocket size slightly for "slop".
        val pocketRadius = state.protractorUnit.radius * 1.8f

        var closestIntersection: PointF? = null
        var closestPocketIndex: Int? = null
        var minDistanceSq = Float.MAX_VALUE

        pockets.forEachIndexed { index, pocket ->
            val intersection = getLineCircleIntersection(start, extendedEnd, pocket, pocketRadius)
            if (intersection != null) {
                // Ensure intersection is "forward" along the ray.
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
        state: CueDetatState,
        useShotGuideLine: Boolean = false
    ): PointF? {
        val p1 = if (useShotGuideLine) state.shotLineAnchor
            ?: return null else state.protractorUnit.center
        val p2 = state.protractorUnit.ghostCueBallCenter
        val dirX = p2.x - p1.x
        val dirY = p2.y - p1.y
        // Find rail impact in infinite direction.
        return state.table.findRailIntersectionAndNormal(
            p1,
            PointF(p1.x + dirX * 5000f, p1.y + dirY * 5000f)
        )?.first
    }

    private fun calculateSingleBank(
        start: PointF,
        end: PointF,
        state: CueDetatState
    ): List<PointF> {
        val dirX = end.x - start.x
        val dirY = end.y - start.y
        val extendedEnd =
            PointF(start.x + dirX * lineExtensionFactor, start.y + dirY * lineExtensionFactor)

        // Find intersection with any table rail.
        val intersectionResult =
            state.table.findRailIntersectionAndNormal(start, extendedEnd) ?: return listOf(
                start,
                extendedEnd
            )

        val (intersectionPoint, railNormal) = intersectionResult
        val incidentVector = PointF(extendedEnd.x - start.x, extendedEnd.y - start.y)

        // Calculate Reflection Vector: R = V - 2(V.N)N
        val reflectedDir = state.table.reflect(incidentVector, railNormal)

        val finalEndPoint = PointF(
            intersectionPoint.x + reflectedDir.x * lineExtensionFactor,
            intersectionPoint.y + reflectedDir.y * lineExtensionFactor
        )
        return listOf(start, intersectionPoint, finalEndPoint)
    }

    private fun checkForObstructions(state: CueDetatState): Boolean {
        if (state.obstacleBalls.isEmpty()) return false
        val ballRadius = state.protractorUnit.radius
        val collisionDistance = ballRadius * 2
        val shotLineStart = state.shotLineAnchor ?: return false
        val shotLineEnd = state.protractorUnit.ghostCueBallCenter

        // Check 1: Cue Ball -> Ghost Ball
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

        // Check 2: Ghost Ball -> Target Ball
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

    /**
     * Checks if a point (center) is within [minDist] of the line segment p1-p2.
     */
    private fun isSegmentObstructed(
        p1: PointF,
        p2: PointF,
        center: PointF,
        minDist: Float
    ): Boolean {
        val l2 = (p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2)
        if (l2 == 0f) return false
        // Calculate projection scalar t.
        var t = ((center.x - p1.x) * (p2.x - p1.x) + (center.y - p1.y) * (p2.y - p1.y)) / l2
        t = t.coerceIn(0f, 1f) // Clamp to segment.
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
        val t = (-b - sqrt(delta)) / (2 * a)
        return PointF(p1.x + t * dx, p1.y + t * dy)
    }
}
