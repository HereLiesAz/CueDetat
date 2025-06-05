package com.hereliesaz.cuedetat.drawing.utility

import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.geometry.GeometryCalculator
import com.hereliesaz.cuedetat.geometry.models.AimingLineLogicalCoords
import android.graphics.PointF

/**
 * Evaluates various visual states based on the application and geometry state.
 */
object VisualStateLogic {

    data class EvaluatedVisualStates(
        val isCurrentlyInvalidShotSetup: Boolean,
        val showWarningStyleForGhostBalls: Boolean
    )

    fun evaluate(
        appState: AppState,
        geometryCalculator: GeometryCalculator,
        aimingLineCoords: AimingLineLogicalCoords?,
        logicalCueCenter: PointF,
        logicalTargetCenter: PointF
    ): EvaluatedVisualStates {

        // Check for physical overlap of logical cue and logical target circles on the plane
        val logicalDistanceBetweenCenters = geometryCalculator.distance(logicalCueCenter, logicalTargetCenter)
        val isPhysicalOverlap = logicalDistanceBetweenCenters < (appState.logicalBallRadius * 2 - 0.1f) // Use logicalBallRadius

        // Check if logical target ball is on the "far side" relative to the aiming line's origin (actual cue ball)
        val isCueOnFarSide = if (aimingLineCoords != null) {
            geometryCalculator.isGhostCueOnFarSide(appState, aimingLineCoords)
        } else {
            false
        }

        // Check if protractor angle is in the deflection-dominant range (between 90.5 and 269.5 degrees)
        // This indicates a cut shot that ML Kit is not designed for, or an extreme angle.
        val isDeflectionDominantAngle = (appState.protractorRotationAngle > 90.5f && appState.protractorRotationAngle < 269.5f)

        // The shot is considered invalid if there's physical overlap OR if the logical target is on the far side OR the angle is deflection-dominant
        val isInvalidShot = isPhysicalOverlap || isCueOnFarSide || isDeflectionDominantAngle

        // Warning style for ghost balls and yellow target line is applied if there's physical overlap or if the logical target is on the far side.
        val showGhostWarning = isPhysicalOverlap || isCueOnFarSide

        return EvaluatedVisualStates(
            isCurrentlyInvalidShotSetup = isInvalidShot,
            showWarningStyleForGhostBalls = showGhostWarning
        )
    }
}