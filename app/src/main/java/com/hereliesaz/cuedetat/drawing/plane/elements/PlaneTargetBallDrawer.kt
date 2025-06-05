package com.hereliesaz.cuedetat.drawing.plane.elements

import android.graphics.Canvas
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.ui.theme.AppBlack
import com.hereliesaz.cuedetat.ui.theme.AppYellow

/**
 * Draws the main target circle on the protractor plane.
 * The position and radius are sourced from `AppState`, which reflects the tracked ball.
 *
 * @param canvas The canvas to draw on.
 * @param appState The current state of the application.
 * @param appPaints The collection of paints used for drawing.
 */
class PlaneTargetBallDrawer {
    fun draw(
        canvas: Canvas,
        appState: AppState,
        appPaints: AppPaints
    ) {
        // Only draw if initialized and radius is meaningful
        if (!appState.isInitialized || appState.logicalBallRadius <= 0.01f) return

        val centerX = appState.targetCircleCenter.x
        val centerY = appState.targetCircleCenter.y
        val radius = appState.logicalBallRadius * appState.zoomFactor // Scale by zoomFactor
        val centerMarkRadius = radius / 5f // Center mark is 1/5th of the main circle radius

        // Draw the target circle outline (typically yellow)
        appPaints.targetCirclePaint.color = AppYellow.toArgb()
        canvas.drawCircle(centerX, centerY, radius, appPaints.targetCirclePaint)

        // Draw the center mark for the target circle (typically black)
        val originalCenterMarkPaintColor = appPaints.centerMarkPaint.color // Save original color
        appPaints.centerMarkPaint.color = AppBlack.toArgb() // Set to black for target's center mark
        canvas.drawCircle(centerX, centerY, centerMarkRadius, appPaints.centerMarkPaint)
        appPaints.centerMarkPaint.color = originalCenterMarkPaintColor // Restore original color
    }
}