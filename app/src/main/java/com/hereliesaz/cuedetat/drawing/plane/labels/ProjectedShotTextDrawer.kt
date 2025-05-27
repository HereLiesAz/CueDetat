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
        if (!appState.isInitialized || !appState.areHelperTextsVisible) return
        if (aimingLineCoords.normDirX == 0f && aimingLineCoords.normDirY == 0f) return // No direction

        val paint = appPaints.projectedShotTextPaint
        paint.textSize = getDynamicTextSizePPD( // Using a local/adapted version for plane labels
            config.PLANE_LABEL_BASE_SIZE,
            appState.zoomFactor,
            config,
            true, // isHelperLineLabel
            config.PROJECTED_SHOT_TEXT_SIZE_FACTOR
        )

        val angleRad = atan2(aimingLineCoords.normDirY, aimingLineCoords.normDirX)
        val rotationDegrees = Math.toDegrees(angleRad.toDouble()).toFloat()

        // Position just beyond cue circle along the aiming line (far part)
        val distanceFromCueCenter = appState.currentLogicalRadius * 1.5f // Adjusted distance
        val preferredX = aimingLineCoords.cueX + aimingLineCoords.normDirX * distanceFromCueCenter
        val preferredY = aimingLineCoords.cueY + aimingLineCoords.normDirY * distanceFromCueCenter

        // Add a small perpendicular offset to tuck it alongside the line
        val perpendicularOffsetAmount = 20f / appState.zoomFactor.coerceAtLeast(0.5f)
        val perpOffsetX = sin(angleRad) * perpendicularOffsetAmount
        val perpOffsetY = -cos(angleRad) * perpendicularOffsetAmount

        val finalX = preferredX + perpOffsetX
        val finalY = preferredY + perpOffsetY

        // Nudge reference: center of the cue ball as this text relates to the line from it
        textLayoutHelper.layoutAndDrawText(
            canvas, TEXT_STRING, finalX, finalY, paint, rotationDegrees, appState.cueCircleCenter
        )
    }

    // Helper function for dynamic text sizing, adapted from old ProtractorPlaneTextDrawer
    private fun getDynamicTextSizePPD(
        baseSize: Float, zoomFactor: Float, config: AppConfig,
        isHelperLineLabel: Boolean = false, sizeMultiplier: Float = 1f
    ): Float {
        val effectiveZoom = if (isHelperLineLabel) zoomFactor.coerceIn(0.7f, 1.3f) else zoomFactor
        return (baseSize * sizeMultiplier * effectiveZoom.coerceIn(config.TEXT_MIN_SCALE_FACTOR, config.TEXT_MAX_SCALE_FACTOR))
            .coerceAtLeast(baseSize * sizeMultiplier * config.TEXT_MIN_SCALE_FACTOR * 0.7f)
    }
}