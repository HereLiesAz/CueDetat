package com.hereliesaz.cuedetat.drawing.screen.elements

import android.graphics.Canvas
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.ui.theme.AppBlack
import com.hereliesaz.cuedetat.ui.theme.AppWhite

/**
 * Draws the visual overlay (circle and aiming sight) on the *actual* cue ball's screen position.
 * This is the visual aid that the user places over the real cue ball seen through the camera.
 * Its size is determined by the `logicalRadius` for consistency with the target ball.
 */
class ActualCueBallOverlayDrawer {
    fun draw(
        canvas: Canvas,
        appPaints: AppPaints,
        centerX: Float,
        centerY: Float,
        radius: Float, // This radius is expected to be appState.logicalBallRadius * appState.zoomFactor
        showErrorStyle: Boolean
    ) {
        if (radius <= 0.01f) return

        // Outline for the actual cue ball overlay
        appPaints.actualCueBallOverlayOutlinePaint.color = if (showErrorStyle) {
            appPaints.M3_COLOR_ERROR
        } else {
            AppWhite.toArgb()
        }
        canvas.drawCircle(centerX, centerY, radius, appPaints.actualCueBallOverlayOutlinePaint)

        // Center mark for the actual cue ball overlay
        val centerMarkRadius = radius / 5f
        val originalCenterMarkPaintColor = appPaints.centerMarkPaint.color
        appPaints.centerMarkPaint.color = if (showErrorStyle) {
            AppWhite.toArgb() // White center mark on error
        } else {
            AppBlack.toArgb() // Black center mark
        }
        canvas.drawCircle(centerX, centerY, centerMarkRadius, appPaints.centerMarkPaint)
        appPaints.centerMarkPaint.color = originalCenterMarkPaintColor // Restore original

        // Aiming Sight (Yellow) on the actual cue ball overlay
        // Sight color is usually fixed (e.g., yellow/black), not affected by error state of cue ball outline
        appPaints.actualCueBallAimingSightPaint.color = appPaints.targetCirclePaint.color // Match target circle color (AppYellow)
        val sightArmLength = radius * 0.6f
        canvas.drawLine(centerX - sightArmLength, centerY, centerX + sightArmLength, centerY, appPaints.actualCueBallAimingSightPaint)
        canvas.drawLine(centerX, centerY - sightArmLength, centerX, centerY + sightArmLength, appPaints.actualCueBallAimingSightPaint)
        canvas.drawCircle(centerX, centerY, sightArmLength * 0.15f, appPaints.actualCueBallAimingSightPaint)
    }
}