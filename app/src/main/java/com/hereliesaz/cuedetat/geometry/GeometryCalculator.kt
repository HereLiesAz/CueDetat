package com.hereliesaz.cuedetat.geometry

import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.state.AppState // Using new AppState
import com.hereliesaz.cuedetat.geometry.models.* // Import new data models
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class GeometryCalculator(
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {

    // Helper to map a single logical point to screen/projected space using a given matrix
    private fun mapPoint(logicalPoint: PointF, matrixToUse: Matrix): PointF {
        val pointArray = floatArrayOf(logicalPoint.x, logicalPoint.y)
        matrixToUse.mapPoints(pointArray)
        return PointF(pointArray[0], pointArray[1])
    }

    // Calculates Euclidean distance between two PointF objects
    fun distance(p1: PointF, p2: PointF): Float {
        val dx = (p1.x - p2.x).toDouble()
        val dy = (p1.y - p2.y).toDouble()
        return sqrt(dx.pow(2) + dy.pow(2)).toFloat()
    }

    // Calculates Euclidean distance between two (x,y) pairs
    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = (x1 - x2).toDouble()
        val dy = (y1 - y2).toDouble()
        return sqrt(dx.pow(2) + dy.pow(2)).toFloat()
    }

    // Calculates projected screen coordinates and radii for cue and target circles
    fun calculateProjectedScreenData(appState: AppState): ProjectedCoords {
        val targetProjectedCenter = mapPoint(appState.targetCircleCenter, appState.pitchMatrix)
        val cueProjectedCenter = mapPoint(appState.cueCircleCenter, appState.pitchMatrix)

        // To calculate screen radius, project 4 cardinal points of each logical circle's circumference
        // and find the max distance between opposite projected points.
        val logicalRadius = appState.currentLogicalRadius

        // Target circle screen radius
        val tL = mapPoint(PointF(appState.targetCircleCenter.x - logicalRadius, appState.targetCircleCenter.y), appState.pitchMatrix)
        val tR = mapPoint(PointF(appState.targetCircleCenter.x + logicalRadius, appState.targetCircleCenter.y), appState.pitchMatrix)
        val tT = mapPoint(PointF(appState.targetCircleCenter.x, appState.targetCircleCenter.y - logicalRadius), appState.pitchMatrix)
        val tB = mapPoint(PointF(appState.targetCircleCenter.x, appState.targetCircleCenter.y + logicalRadius), appState.pitchMatrix)
        val targetScreenRadius = max(distance(tL, tR), distance(tT, tB)) / 2f

        // Cue circle screen radius
        val cL = mapPoint(PointF(appState.cueCircleCenter.x - logicalRadius, appState.cueCircleCenter.y), appState.pitchMatrix)
        val cR = mapPoint(PointF(appState.cueCircleCenter.x + logicalRadius, appState.cueCircleCenter.y), appState.pitchMatrix)
        val cT = mapPoint(PointF(appState.cueCircleCenter.x, appState.cueCircleCenter.y - logicalRadius), appState.pitchMatrix)
        val cB = mapPoint(PointF(appState.cueCircleCenter.x, appState.cueCircleCenter.y + logicalRadius), appState.pitchMatrix)
        val cueScreenRadius = max(distance(cL, cR), distance(cT, cB)) / 2f

        return ProjectedCoords(
            targetProjected = targetProjectedCenter,
            cueProjected = cueProjectedCenter,
            targetScreenRadius = targetScreenRadius,
            cueScreenRadius = cueScreenRadius
        )
    }

    // Calculates the logical coordinates for the aiming line (shot guide)
    // The aiming line now starts from the *tracked actual cue ball's screen position*
    fun calculateAimingLineLogicalCoords(appState: AppState, hasInversePitchMatrix: Boolean): AimingLineLogicalCoords? {
        if (!hasInversePitchMatrix || appState.selectedCueBallScreenCenter == null) return null

        // The start of the aiming line is the tracked actual cue ball's screen position
        val actualCueScreenX = appState.selectedCueBallScreenCenter!!.x
        val actualCueScreenY = appState.selectedCueBallScreenCenter!!.y

        // Map this screen point back to the logical protractor plane
        val actualCuePointScreenCoords = floatArrayOf(actualCueScreenX, actualCueScreenY)
        val actualCuePointLogicalCoordsArray = FloatArray(2)
        appState.inversePitchMatrix.mapPoints(actualCuePointLogicalCoordsArray, actualCuePointScreenCoords)
        val logicalStartX = actualCuePointLogicalCoordsArray[0]
        val logicalStartY = actualCuePointLogicalCoordsArray[1]

        // The line passes through the logical ghost cue ball position (appState.cueCircleCenter)
        val logicalGhostCueX = appState.cueCircleCenter.x
        val logicalGhostCueY = appState.cueCircleCenter.y

        val aimDirectionX = logicalGhostCueX - logicalStartX
        val aimDirectionY = logicalGhostCueY - logicalStartY
        val magnitudeSquared = aimDirectionX * aimDirectionX + aimDirectionY * aimDirectionY

        if (magnitudeSquared <= 0.0001f) { // Avoid division by zero or near-zero magnitude
            return AimingLineLogicalCoords(logicalStartX, logicalStartY, logicalGhostCueX, logicalGhostCueY, logicalGhostCueX, logicalGhostCueY, 0f, 0f)
        }

        val magnitude = sqrt(magnitudeSquared)
        val normalizedAimDirX = aimDirectionX / magnitude
        val normalizedAimDirY = aimDirectionY / magnitude

        // Extend the line far off-screen for drawing purposes
        val extendLengthFactor = max(viewWidthProvider(), viewHeightProvider()) * 5f // Arbitrary large factor
        val logicalEndX = logicalGhostCueX + normalizedAimDirX * extendLengthFactor
        val logicalEndY = logicalGhostCueY + normalizedAimDirY * extendLengthFactor

        return AimingLineLogicalCoords(
            startX = logicalStartX, startY = logicalStartY,
            cueX = logicalGhostCueX, cueY = logicalGhostCueY, // cueX/Y here refers to the GHOST cue ball's position on the plane
            endX = logicalEndX, endY = logicalEndY,
            normDirX = normalizedAimDirX, normDirY = normalizedAimDirY
        )
    }

    // Calculates parameters for drawing deflection lines (tangent and cue ball path)
    fun calculateDeflectionLineParams(appState: AppState): DeflectionLineParams {
        val deltaX_CueToTarget = appState.targetCircleCenter.x - appState.cueCircleCenter.x
        val deltaY_CueToTarget = appState.targetCircleCenter.y - appState.cueCircleCenter.y
        val magnitude_CueToTarget = sqrt(deltaX_CueToTarget * deltaX_CueToTarget + deltaY_CueToTarget * deltaY_CueToTarget)

        var unitPerpendicularX = 0f
        var unitPerpendicularY = 0f

        if (magnitude_CueToTarget > 0.001f) {
            // Normalized vector from logical cue to logical target
            val normCueToTargetX = deltaX_CueToTarget / magnitude_CueToTarget
            val normCueToTargetY = deltaY_CueToTarget / magnitude_CueToTarget
            // Perpendicular vector (one direction of tangent)
            unitPerpendicularX = -normCueToTargetY
            unitPerpendicularY = normCueToTargetX
        }

        val drawLength = max(viewWidthProvider(), viewHeightProvider()) * 1.5f // How far to draw deflection lines

        return DeflectionLineParams(
            cueToTargetDistance = magnitude_CueToTarget,
            unitPerpendicularX = unitPerpendicularX,
            unitPerpendicularY = unitPerpendicularY,
            visualDrawLength = drawLength
        )
    }

    // Checks if the logical cue ball (ghost ball) is on the "far side" of the logical target ball
    // relative to the aiming line's origin (which is now the actual cue ball).
    // This logic ensures the target isn't "behind" the actual cue ball along the shot path.
    fun isGhostCueOnFarSide(appState: AppState, aimingLineCoords: AimingLineLogicalCoords?): Boolean {
        if (aimingLineCoords == null || appState.selectedCueBallScreenCenter == null) return false

        // `aimingLineCoords.startX, startY` is now the actual cue ball's logical position.
        // `aimingLineCoords.cueX, cueY` is the logical ghost cue ball's position.
        // `appState.targetCircleCenter.x, y` is the logical target ball's position.

        // Vector from actual cue (line start) to ghost cue
        val vecActualCueToGhostCueX = aimingLineCoords.cueX - aimingLineCoords.startX
        val vecActualCueToGhostCueY = aimingLineCoords.cueY - aimingLineCoords.startY
        val distActualToGhostCue = distance(aimingLineCoords.startX, aimingLineCoords.startY, aimingLineCoords.cueX, aimingLineCoords.cueY)

        // Vector from actual cue (line start) to target ball
        val vecActualCueToTargetX = appState.targetCircleCenter.x - aimingLineCoords.startX
        val vecActualCueToTargetY = appState.targetCircleCenter.y - aimingLineCoords.startY

        // Project the target ball onto the aiming line (line from actual cue through ghost cue)
        // Dot product to find projected length
        val projectedTargetLength = if (distActualToGhostCue > 0.001f) {
            (vecActualCueToTargetX * vecActualCueToGhostCueX + vecActualCueToTargetY * vecActualCueToGhostCueY) / distActualToGhostCue
        } else {
            // If ghost cue is at actual cue, line has no direction, so target can't be "behind" it this way
            0f
        }

        // The target is on the "far side" if its projection along the aiming line is less than 0,
        // meaning it's "behind" the actual cue ball relative to the ghost cue ball's direction.
        return projectedTargetLength < 0
    }
}