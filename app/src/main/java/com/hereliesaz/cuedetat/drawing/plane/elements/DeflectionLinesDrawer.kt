package com.hereliesaz.cuedetat.drawing.plane.elements

import android.graphics.Canvas
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.geometry.models.DeflectionLineParams
import com.hereliesaz.cuedetat.ui.theme.AppWhite

class DeflectionLinesDrawer {
    fun draw(
        canvas: Canvas,
        appState: AppState,
        appPaints: AppPaints,
        deflectionParams: DeflectionLineParams,
        useErrorColor: Boolean
    ) {
        if (!appState.isInitialized || deflectionParams.cueToTargetDistance <= 0.001f) {
            return
        }

        val cueCenterX = appState.cueCircleCenter.x
        val cueCenterY = appState.cueCircleCenter.y
        // deflectionParams.unitPerpendicularX/Y is the "positive" direction (e.g., right of cue-to-target)
        val unitVecXPositive = deflectionParams.unitPerpendicularX
        val unitVecYPositive = deflectionParams.unitPerpendicularY
        val drawLength = deflectionParams.visualDrawLength

        // Ensure base color is white and solid paint has no glow before applying logic
        appPaints.deflectionDottedPaint.color = AppWhite.toArgb()
        appPaints.deflectionSolidPaint.color = AppWhite.toArgb()
        appPaints.deflectionSolidPaint.clearShadowLayer()

        var paintForPositiveDir = appPaints.deflectionDottedPaint
        var paintForNegativeDir = appPaints.deflectionDottedPaint

        if (useErrorColor) {
            val errorColor = appPaints.M3_COLOR_ERROR
            appPaints.deflectionDottedPaint.color = errorColor // Use this for both if error
            paintForPositiveDir = appPaints.deflectionDottedPaint
            paintForNegativeDir = appPaints.deflectionDottedPaint
            // Ensure solid paint is also error colored, though not directly used for line style
            appPaints.deflectionSolidPaint.color = errorColor
        } else {
            // Apply glow to solid paint for non-error state
            appPaints.deflectionSolidPaint.setShadowLayer(
                appState.config.GLOW_RADIUS_FIXED, 0f, 0f, appPaints.M3_GLOW_COLOR
            )
            // Ensure colors are white after potential error state in previous frame
            appPaints.deflectionDottedPaint.color = AppWhite.toArgb()
            appPaints.deflectionSolidPaint.color = AppWhite.toArgb()

            val alphaDeg = appState.protractorRotationAngle
            val epsilon = 0.5f

            // This logic determines if the "positive" deflection vector (unitVecXPositive, unitVecYPositive)
            // should get the solid line treatment.
            // Original logic was:
            // if (alphaDeg > epsilon && alphaDeg < (180f - epsilon)) -> paintForDir2 (negative vector) was solid.
            // else if (alphaDeg > (180f + epsilon) && alphaDeg < (360f - epsilon)) -> paintForDir1 (positive vector) was solid.

            if (alphaDeg > epsilon && alphaDeg < (180f - epsilon)) {
                // Protractor points "left-ish" on unit circle; negative deflection vector is "more aligned" / solid
                paintForNegativeDir = appPaints.deflectionSolidPaint
                paintForPositiveDir = appPaints.deflectionDottedPaint
            } else if (alphaDeg > (180f + epsilon) && alphaDeg < (360f - epsilon)) {
                // Protractor points "right-ish" on unit circle; positive deflection vector is "more aligned" / solid
                paintForPositiveDir = appPaints.deflectionSolidPaint
                paintForNegativeDir = appPaints.deflectionDottedPaint
            } else {
                // Default (near 0, 180, 360): make the positive deflection vector solid.
                paintForPositiveDir = appPaints.deflectionSolidPaint
                paintForNegativeDir = appPaints.deflectionDottedPaint
            }
        }

        // Line along positive deflection vector: (+unitVecXPositive, +unitVecYPositive)
        canvas.drawLine(
            cueCenterX, cueCenterY,
            cueCenterX + unitVecXPositive * drawLength,
            cueCenterY + unitVecYPositive * drawLength,
            paintForPositiveDir
        )
        // Line along negative deflection vector: (-unitVecXPositive, -unitVecYPositive)
        canvas.drawLine(
            cueCenterX, cueCenterY,
            cueCenterX - unitVecXPositive * drawLength,
            cueCenterY - unitVecYPositive * drawLength,
            paintForNegativeDir
        )
    }
}