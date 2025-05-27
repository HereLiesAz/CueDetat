// app/src/main/java/com/hereliesaz/cuedetat/drawing/screen/elements/GhostTargetBallDrawer.kt
package com.hereliesaz.cuedetat.drawing.screen.elements

import android.graphics.Canvas
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.ui.theme.AppYellow // Target is always yellow

class GhostTargetBallDrawer {
    /**
     * Draws the "ghost" (projected) target ball. This is drawn in screen space
     * and represents the visual projection of the tracked target ball.
     *
     * @param canvas The canvas to draw on.
     * @param appPaints The collection of paints used for drawing.
     * @param centerX The X-coordinate of the ghost ball's center in screen pixels.
     * @param centerY The Y-coordinate of the ghost ball's center in screen pixels.
     * @param radius The radius of the ghost ball in screen pixels.
     */
    fun draw(
        canvas: Canvas,
        appPaints: AppPaints,
        centerX: Float,
        centerY: Float,
        radius: Float // This `radius` is already the projected screen radius, suitable for drawing.
    ) {
        if (radius <= 0.01f) return // Only draw if radius is meaningful

        // Ghost Target Ball Outline (Always yellow as per 8-ball aesthetic)
        appPaints.ghostTargetOutlinePaint.color = AppYellow.toArgb()
        canvas.drawCircle(centerX, centerY, radius, appPaints.ghostTargetOutlinePaint)

        // No aiming sight needed on the target ghost ball itself typically
    }
}