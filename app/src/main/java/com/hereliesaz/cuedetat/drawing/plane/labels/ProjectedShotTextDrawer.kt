package com.hereliesaz.cuedetat.drawing.plane.labels

import android.graphics.Canvas
import android.graphics.PointF
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.drawing.utility.TextLayoutHelper
import com.hereliesaz.cuedetat.geometry.models.AimingLineLogicalCoords
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class ProjectedShotTextDrawer(private val textLayoutHelper: TextLayoutHelper) {

    private val TEXT_STRING = "Projected Shot Line"

    fun draw(
        canvas: Canvas,
        appState: AppState,
        appPaints: AppPaints,
        config: AppConfig,
        aimingLineCoords: AimingLineLogicalCoords
    ) {
        // Only draw if in AIMING mode and helper texts are visible
        if (!appState.isInitialized || !appState.areHelperTextsVisible || appState.currentMode != AppState.SelectionMode.AIMING) return
        if (aimingLineCoords.normDirX == 0f && aimingLineCoords.normDirY == 0f) return // No direction

        val paint = appPaints.projectedShotTextPaint
        paint.textSize = getPlaneSpaceTextSize( // Using a local/adapted version for plane labels
            config.PLANE_LABEL_BASE_SIZE,
            appState.zoomFactor,
            config,
            true, // isHelperLineLabel
            config.PROJECTED_SHOT_TEXT_SIZE_FACTOR
        )

        // The angle of the line segment from logical ghost cue to line end
        val angleRad = atan2(aimingLineCoords.normDirY, aimingLineCoords.normDirX)
        val rotationDegrees = Math.toDegrees(angleRad.toDouble()).toFloat()

        // Position text along the line, starting from the logical ghost cue ball
        val distanceFromGhostCueCenter = appState.currentLogicalRadius * 1.5f // Adjusted distance from ghost cue
        val preferredX = aimingLineCoords.cueX + aimingLineCoords.normDirX * distanceFromGhostCueCenter
        val preferredY = aimingLineCoords.cueY + aimingLineCoords.normDirY * distanceFromGhostCueCenter

        // Add a small perpendicular offset to tuck it alongside the line
        val perpendicularOffsetAmount = 20f / appState.zoomFactor.coerceAtLeast(0.5f)
        val perpOffsetX = sin(angleRad) * perpendicularOffsetAmount
        val perpOffsetY = -cos(angleRad) * perpendicularOffsetAmount

        val finalX = preferredX + perpOffsetX
        val finalY = preferredY + perpOffsetY

        // Nudge reference: center of the logical cue ball (ghost ball on plane)
        textLayoutHelper.layoutAndDrawText(
            canvas, TEXT_STRING, finalX, finalY, paint, rotationDegrees, appState.cueCircleCenter
        )
    }

    // Helper function for dynamic text sizing, adapted from old ProtractorPlaneTextDrawer
    private fun getPlaneSpaceTextSize(
        baseSize: Float, zoomFactor: Float, config: AppConfig,
        isHelperLineLabel: Boolean = false, sizeMultiplier: Float = 1f
    ): Float {
        val effectiveZoom = if (isHelperLineLabel) zoomFactor.coerceIn(0.7f, 1.3f) else zoomFactor
        return (baseSize * sizeMultiplier * effectiveZoom.coerceIn(config.TEXT_MIN_SCALE_FACTOR, config.TEXT_MAX_SCALE_FACTOR))
            .coerceAtLeast(baseSize * sizeMultiplier * config.TEXT_MIN_SCALE_FACTOR * 0.7f)
    }
}