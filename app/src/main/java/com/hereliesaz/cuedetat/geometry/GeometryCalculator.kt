package com.hereliesaz.cuedetat.geometry

import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.geometry.models.AimingLineLogicalCoords
import com.hereliesaz.cuedetat.geometry.models.DeflectionLineParams
import com.hereliesaz.cuedetat.geometry.models.ActualBallOverlayCoords
import com.hereliesaz.cuedetat.state.AppState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

class GeometryCalculator(
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {
    /**
     * Calculates the screen coordinates and radius for the visual overlays that are drawn
     * *on top of the actual detected/manual balls* visible through the camera feed.
     * The radii of these overlays will be `appState.logicalBallRadius` (unscaled) for consistent sizing.
     */
    fun calculateActualBallOverlayCoords(appState: AppState): ActualBallOverlayCoords {
        // For the overlay on the *actual* target ball, use its tracked/manual position.
        // Its visual radius should be the same as the logical radius for consistency.
        val targetOverlayX = appState.selectedTargetBall?.x ?: appState.targetCircleCenter.x
        val targetOverlayY = appState.selectedTargetBall?.y ?: appState.targetCircleCenter.y
        val targetOverlayRadius = appState.logicalBallRadius // Force to logical radius

        // For the overlay on the *actual* cue ball, use its tracked/manual position.
        // Its visual radius should be the same as the logical radius for consistency.
        val cueOverlayX = appState.selectedCueBall?.x ?: appState.cueCircleCenter.x
        val cueOverlayY = appState.selectedCueBall?.y ?: appState.cueCircleCenter.y
        val cueOverlayRadius = appState.logicalBallRadius // Force to logical radius

        return ActualBallOverlayCoords(
            actualTargetOverlayPosition = PointF(targetOverlayX, targetOverlayY),
            actualCueOverlayPosition = PointF(cueOverlayX, cueOverlayY),
            actualTargetOverlayRadius = targetOverlayRadius,
            actualCueOverlayRadius = cueOverlayRadius
        )
    }

    /**
     * Calculates the logical coordinates for the main aiming line, which extends from the actual cue ball
     * (if selected) to the ghost cue ball, and then projects indefinitely.
     * The `startX, startY` in `AimingLineLogicalCoords` should be the actual cue ball's projected screen position.
     * `cueX, cueY` is the logical ghost cue ball position.
     */
    fun calculateAimingLineLogicalCoords(appState: AppState, hasInversePitchMatrix: Boolean): AimingLineLogicalCoords {
        val viewWidth = viewWidthProvider()
        val viewHeight = viewHeightProvider()

        // Start point of the line: This is now taken directly from the selectedCueBall
        val startX = appState.selectedCueBall?.x ?: (viewWidth / 2f)
        val startY = appState.selectedCueBall?.y ?: (viewHeight * 0.9f) // Start near the bottom of the screen

        val cueX = appState.cueCircleCenter.x
        val cueY = appState.cueCircleCenter.y

        // Calculate the direction vector from the logical ghost cue ball (cueX, cueY) towards the logical target ball,
        // and extend it far beyond the target.
        val dx = appState.targetCircleCenter.x - cueX
        val dy = appState.targetCircleCenter.y - cueY

        val distance = hypot(dx, dy)
        val normDirX = if (distance > 0) dx / distance else 0f
        val normDirY = if (distance > 0) dy / distance else 0f

        // Extend the line to a point far off-screen
        val lineExtensionLength = maxOf(viewWidth, viewHeight) * 1.5f // Extend far enough
        val endX = cueX + normDirX * lineExtensionLength
        val endY = cueY + normDirY * lineExtensionLength

        return AimingLineLogicalCoords(
            startX = startX,
            startY = startY,
            cueX = cueX,
            cueY = cueY,
            endX = endX,
            endY = endY,
            normDirX = normDirX,
            normDirY = normDirY
        )
    }

    /**
     * Calculates parameters for drawing the deflection lines.
     * These lines extend perpendicularly from the cue-to-target line at the logical ghost cue ball.
     */
    fun calculateDeflectionLineParams(appState: AppState): DeflectionLineParams {
        val cueCenterX = appState.cueCircleCenter.x
        val cueCenterY = appState.cueCircleCenter.y
        val targetCenterX = appState.targetCircleCenter.x
        val targetCenterY = appState.targetCircleCenter.y
        val radius = appState.logicalBallRadius // Use logicalBallRadius

        // Vector from logical cue to logical target
        val dx = targetCenterX - cueCenterX
        val dy = targetCenterY - cueCenterY
        val cueToTargetDistance = hypot(dx, dy)

        // Perpendicular vector (unit vector)
        val perpX = -dy // Rotate (dx, dy) by 90 degrees counter-clockwise to get (-dy, dx) or (dy, -dx)
        val perpY = dx

        val perpDistance = hypot(perpX, perpY)
        val unitPerpX = if (perpDistance > 0) perpX / perpDistance else 0f
        val unitPerpY = if (perpDistance > 0) perpY / perpDistance else 0f

        // Visual length of the deflection lines
        val visualDrawLength = radius * 3f // A multiple of the ball radius

        return DeflectionLineParams(
            cueToTargetDistance = cueToTargetDistance,
            unitPerpendicularX = unitPerpX,
            unitPerpendicularY = unitPerpY,
            visualDrawLength = visualDrawLength
        )
    }

    /**
     * Checks if the logical ghost cue ball is "behind" the target ball relative to the current aiming line,
     * indicating an invalid shot (e.g., trying to pull the cue ball through the target).
     * This uses the *actual* start point of the aiming line (from the actual cue ball's screen position) as reference.
     */
    fun isGhostCueOnFarSide(appState: AppState, aimingLineCoords: AimingLineLogicalCoords): Boolean {
        // Line vector is from startX, startY (actual cue ball) to endX, endY (extended line)
        val lineVecX = aimingLineCoords.endX - aimingLineCoords.startX
        val lineVecY = aimingLineCoords.endY - aimingLineCoords.startY

        // Vector from line start (actual cue ball) to logical ghost cue ball
        val actualToGhostX = aimingLineCoords.cueX - aimingLineCoords.startX
        val actualToGhostY = aimingLineCoords.cueY - aimingLineCoords.startY

        // Vector from line start (actual cue ball) to target ball
        val actualToTargetX = (appState.selectedTargetBall?.x ?: appState.targetCircleCenter.x) - aimingLineCoords.startX
        val actualToTargetY = (appState.selectedTargetBall?.y ?: appState.targetCircleCenter.y) - aimingLineCoords.startY

        val dotProduct = (actualToTargetX * actualToGhostX) + (actualToTargetY * actualToGhostY)

        val magActualToTargetSq = (actualToTargetX * actualToTargetX) + (actualToTargetY * actualToTargetY)
        val magActualToGhostSq = (actualToGhostX * actualToGhostX) + (actualToGhostY * actualToGhostY)

        if (magActualToTargetSq < 0.1f || magActualToGhostSq < 0.1f) return false // Avoid division by zero or near-zero magnitudes

        // If dot product is negative or magnitude of actualToGhost is less than actualToTarget,
        // it means the logical ghost ball is 'behind' or 'not far enough' along the line relative to the target.
        // Specifically, for "far side", we mean the logical ghost ball is beyond the target.
        // If the dot product is negative, it means the angle between the two vectors is > 90 degrees.
        // We want them to be roughly in the same direction, and actualToGhost to be longer.

        val distActualToTarget = hypot(actualToTargetX, actualToTargetY)
        val distActualToGhost = hypot(actualToGhostX, actualToGhostY)

        // Check for alignment: if dot product is positive and sufficient (e.g. cos(theta) > 0.8)
        val alignmentThreshold = 0.9f // Cosine of ~25 degrees
        val cosTheta = if (distActualToTarget > 0.1f && distActualToGhost > 0.1f) {
            dotProduct / (distActualToTarget * distActualToGhost)
        } else {
            0f
        }

        // isGhostCueOnFarSide means the ghost ball is positioned such that the actual cue ball would need to pass
        // *through* the target ball to hit the ghost ball, or the ghost ball is too close/behind the target.
        // The condition (distActualToGhost < distActualToTarget - appState.logicalBallRadius / 2f)
        // checks if the ghost ball is effectively "inside" the target ball's projected area from the actual cue.
        return (distActualToGhost < distActualToTarget - appState.logicalBallRadius / 2f) && (cosTheta > alignmentThreshold)
    }

    /**
     * Calculates the distance between two PointF objects.
     */
    fun distance(p1: PointF, p2: PointF): Float {
        return hypot(p1.x - p2.x, p1.y - p2.y)
    }
}