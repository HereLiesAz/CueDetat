// app/src/main/java/com/hereliesaz/cuedetat/view/renderer/text/BallTextRenderer.kt
package com.hereliesaz.cuedetat.view.renderer.text

import android.graphics.Canvas
import android.graphics.Paint
import com.hereliesaz.cuedetat.ui.ZoomMapping

class BallTextRenderer {

    private val baseGhostBallTextSize = 2f // Base size in logical units (inches)
    private val minGhostBallTextSize = 1f  // Min size in logical units
    private val maxGhostBallTextSize = 4f  // Max size in logical units

    fun draw(
        canvas: Canvas,
        paint: Paint,
        currentScale: Float,
        x: Float,
        y: Float,
        radius: Float,
        text: String
    ) {
        // Text size is now relative to the world scale
        val scaleFactor = currentScale / ZoomMapping.DEFAULT_SCALE
        val logicalTextSize = (baseGhostBallTextSize * scaleFactor).coerceIn(
            minGhostBallTextSize,
            maxGhostBallTextSize
        )
        // Convert logical text size to screen text size
        paint.textSize = logicalTextSize * currentScale

        val textMetrics = paint.fontMetrics
        val textPadding = 5f * scaleFactor.coerceAtLeast(0.5f)
        val visualTop = y - radius
        val baseline = visualTop - textPadding - textMetrics.descent
        canvas.drawText(text, x, baseline, paint)
    }
}
