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
    fun calculateAimingLineLogicalCoords(appState: AppState, hasInversePitchMatrix: Boolean): AimingLineLogicalCoords? {
        if (!hasInversePitchMatrix) return null

        // The "start" of the aiming line is considered to be at the bottom center of the screen.
        // We need to map this screen point back to the logical protractor plane.
        val screenAimPointScreenCoords = floatArrayOf(viewWidthProvider() / 2f, viewHeightProvider().toFloat())
        val screenAimPointLogicalCoordsArray = FloatArray(2)
        appState.inversePitchMatrix.mapPoints(screenAimPointLogicalCoordsArray, screenAimPointScreenCoords)

        val logicalStartX = screenAimPointLogicalCoordsArray[0]
        val logicalStartY = screenAimPointLogicalCoordsArray[1]
        val logicalCueX = appState.cueCircleCenter.x
        val logicalCueY = appState.cueCircleCenter.y

        val aimDirectionX = logicalCueX - logicalStartX
        val aimDirectionY = logicalCueY - logicalStartY
        val magnitudeSquared = aimDirectionX * aimDirectionX + aimDirectionY * aimDirectionY

        if (magnitudeSquared <= 0.0001f) { // Avoid division by zero or near-zero magnitude
            return AimingLineLogicalCoords(logicalStartX, logicalStartY, logicalCueX, logicalCueY, logicalCueX, logicalCueY, 0f, 0f)
        }

        val magnitude = sqrt(magnitudeSquared)
        val normalizedAimDirX = aimDirectionX / magnitude
        val normalizedAimDirY = aimDirectionY / magnitude

        // Extend the line far off-screen for drawing purposes
        val extendLengthFactor = max(viewWidthProvider(), viewHeightProvider()) * 5f // Arbitrary large factor
        val logicalEndX = logicalCueX + normalizedAimDirX * extendLengthFactor
        val logicalEndY = logicalCueY + normalizedAimDirY * extendLengthFactor

        return AimingLineLogicalCoords(
            startX = logicalStartX, startY = logicalStartY,
            cueX = logicalCueX, cueY = logicalCueY,
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
            // Normalized vector from cue to target
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

    // Checks if the cue ball is logically on the "far side" of the target ball relative to the aiming line's origin
    fun isCueOnFarSide(appState: AppState, aimingLineCoords: AimingLineLogicalCoords?): Boolean {
        if (aimingLineCoords == null) return false

        val targetLogicalX = appState.targetCircleCenter.x
        val targetLogicalY = appState.targetCircleCenter.y

        // Use the normalized direction of the aiming line (from screen bottom towards cue ball)
        val normAimDirX = aimingLineCoords.normDirX
        val normAimDirY = aimingLineCoords.normDirY

        // If aiming line has no direction (e.g., cue is exactly at screen bottom projected point)
        if (normAimDirX == 0f && normAimDirY == 0f &&
            aimingLineCoords.startX == aimingLineCoords.cueX && aimingLineCoords.startY == aimingLineCoords.cueY) {
            return false
        }

        // Vector from aiming line start (screen bottom projected) to cue ball
        val vecScreenToCueX = aimingLineCoords.cueX - aimingLineCoords.startX
        val vecScreenToCueY = aimingLineCoords.cueY - aimingLineCoords.startY
        // Distance along the aiming line from its start to the cue ball
        val distToCueProjected = vecScreenToCueX * normAimDirX + vecScreenToCueY * normAimDirY


        // Vector from aiming line start to target ball
        val vecScreenToTargetX = targetLogicalX - aimingLineCoords.startX
        val vecScreenToTargetY = targetLogicalY - aimingLineCoords.startY
        // Distance along the aiming line from its start to the target ball's projection on the line
        val distToTargetProjected = vecScreenToTargetX * normAimDirX + vecScreenToTargetY * normAimDirY

        // Cue is on the "far side" if its projected distance is greater than target's,
        // and target's projected distance is positive (meaning target is "in front" of aiming start point).
        return distToCueProjected > distToTargetProjected && distToTargetProjected > 0
    }
}