package com.hereliesaz.cuedetat.drawing.utility

import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.geometry.GeometryCalculator
import com.hereliesaz.cuedetat.geometry.models.AimingLineLogicalCoords

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
        logicalCueCenter: android.graphics.PointF,
        logicalTargetCenter: android.graphics.PointF
    ): EvaluatedVisualStates {

        val logicalDistanceBetweenCenters = geometryCalculator.distance(logicalCueCenter, logicalTargetCenter)
        val isPhysicalOverlap = logicalDistanceBetweenCenters < (appState.currentLogicalRadius * 2 - 0.1f)

        val isCueOnFarSide = if (aimingLineCoords != null) {
            geometryCalculator.isCueOnFarSide(appState, aimingLineCoords)
        } else {
            false
        }

        val isDeflectionDominantAngle = (appState.protractorRotationAngle > 90.5f && appState.protractorRotationAngle < 269.5f)

        val isInvalidShot = isCueOnFarSide || isDeflectionDominantAngle
        val showGhostWarning = isPhysicalOverlap || isCueOnFarSide

        return EvaluatedVisualStates(
            isCurrentlyInvalidShotSetup = isInvalidShot,
            showWarningStyleForGhostBalls = showGhostWarning
        )
    }
}