package com.hereliesaz.cuedetat.drawing.plane.labels

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.text.TextPaint
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.drawing.utility.TextLayoutHelper

class PocketAimTextDrawer(private val textLayoutHelper: TextLayoutHelper) {

    private val TEXT_STRING = "Aim this at the pocket."

    fun draw(
        canvas: Canvas,
        appState: AppState,
        appPaints: AppPaints,
        config: AppConfig
        // No need to pass canvas transforms here, PlaneRenderer handles the main one
    ) {
        // Only draw if in AIMING mode and helper texts are visible
        if (!appState.isInitialized || !appState.areHelperTextsVisible || appState.currentMode != AppState.SelectionMode.AIMING) return

        val paint = appPaints.pocketAimTextPaint // textAlign is CENTER
        paint.textSize = getDynamicTextSizePPD(
            config.PLANE_LABEL_BASE_SIZE,
            appState.zoomFactor,
            config,
            true,
            config.POCKET_AIM_TEXT_SIZE_FACTOR
        )

        // This drawing happens AFTER PlaneRenderer has done:
        // canvas.save()
        // canvas.translate(appState.targetCircleCenter.x, appState.targetCircleCenter.y)
        // canvas.rotate(appState.protractorRotationAngle)
        // ... and the Y-lift for plane texts

        // So, coordinates are relative to the targetCircleCenter, along the protractor's current 0-degree axis.
        val preferredX = 0f // Center of the text block should be on the protractor's 0-degree line
        val preferredY = -(appState.logicalBallRadius * appState.zoomFactor + (35f / appState.zoomFactor.coerceAtLeast(0.5f))) // Use logicalBallRadius and scale by zoomFactor
        val rotationDegrees = 90f // Text itself is rotated 90 degrees to be perpendicular to the line

        // The nudge reference is (0,0) in this *local, already transformed* canvas space,
        // which corresponds to the targetCircleCenter in the original plane space.
        textLayoutHelper.layoutAndDrawText(
            canvas, TEXT_STRING, preferredX, preferredY, paint, rotationDegrees, PointF(0f, 0f)
        )
        // The restore for the main canvas transform is handled by PlaneRenderer
    }

    private fun getDynamicTextSizePPD(
        baseSize: Float, zoomFactor: Float, config: AppConfig,
        isHelperLineLabel: Boolean = false, sizeMultiplier: Float = 1f
    ): Float {
        val effectiveZoom = if (isHelperLineLabel) zoomFactor.coerceIn(0.7f, 1.3f) else zoomFactor
        return (baseSize * sizeMultiplier * effectiveZoom.coerceIn(config.TEXT_MIN_SCALE_FACTOR, config.TEXT_MAX_SCALE_FACTOR))
            .coerceAtLeast(baseSize * sizeMultiplier * config.TEXT_MIN_SCALE_FACTOR * 0.7f)
    }
}