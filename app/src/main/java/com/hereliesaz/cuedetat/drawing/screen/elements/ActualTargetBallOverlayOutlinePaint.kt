package com.hereliesaz.cuedetat.drawing.screen.elements

import android.graphics.Canvas
import androidx.compose.ui.graphics.toArgb // Explicitly import toArgb
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.ui.theme.AppYellow

/**
 * Draws the visual overlay (circle only, no sight) on the *actual* target ball's screen position.
 * Its size is determined by the `logicalRadius` for consistency with other elements.
 */
class ActualTargetBallOverlayDrawer { // Corrected class name
    fun draw(
        canvas: Canvas,
        appPaints: AppPaints, // Added parameter
        centerX: Float,
        centerY: Float,
        radius: Float
    ) {
        if (radius <= 0.01f) return

        // The paint is already defined in AppPaints, just use it to draw the circle.
        appPaints.actualTargetBallOverlayOutlinePaint.color = AppYellow.toArgb()
        canvas.drawCircle(centerX, centerY, radius, appPaints.actualTargetBallOverlayOutlinePaint)
    }
}