// app/src/main/java/com/hereliesaz/cuedetat/view/renderer/text/LineTextRenderer.kt
package com.hereliesaz.cuedetat.view.renderer.text

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import com.hereliesaz.cuedetat.ui.ZoomMapping
import kotlin.math.cos
import kotlin.math.sin

class LineTextRenderer {

    // These base font sizes are now in logical units (inches)
    private val minLineTextSize = 0.5f
    private val maxLineTextSize = 3.0f

    fun draw(
        canvas: Canvas,
        text: String,
        origin: PointF,
        lineAngleDegrees: Float,
        distanceFromOrigin: Float,
        angleOffsetDegrees: Float,
        rotationOffsetDegrees: Float,
        paint: Paint,
        baseFontSize: Float, // This is now a logical size
        currentScale: Float
    ) {
        // Adjust logical font size based on scale, relative to default
        val scaleFactor = currentScale / ZoomMapping.DEFAULT_SCALE
        val logicalTextSize =
            (baseFontSize * scaleFactor).coerceIn(minLineTextSize, maxLineTextSize)

        // Convert logical font size to screen font size for drawing
        // Note: this is tricky because the canvas is already scaled.
        // We set the text size in the original, un-scaled space.
        paint.textSize = logicalTextSize

        val textAngleRadians = Math.toRadians((lineAngleDegrees + angleOffsetDegrees).toDouble())

        val textX = origin.x + (distanceFromOrigin * cos(textAngleRadians)).toFloat()
        val textY = origin.y + (distanceFromOrigin * sin(textAngleRadians)).toFloat()

        canvas.save()
        canvas.rotate(lineAngleDegrees + rotationOffsetDegrees, textX, textY)
        canvas.drawText(text, textX, textY, paint)
        canvas.restore()
    }
}
