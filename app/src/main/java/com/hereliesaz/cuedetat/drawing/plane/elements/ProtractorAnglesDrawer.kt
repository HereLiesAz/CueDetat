package com.hereliesaz.cuedetat.drawing.plane.elements

import android.graphics.Canvas
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class ProtractorAnglesDrawer(
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {
    fun draw(
        canvas: Canvas,
        appState: AppState,
        appPaints: AppPaints,
        config: AppConfig
    ) {
        if (!appState.isInitialized) return

        canvas.save()
        canvas.translate(appState.targetCircleCenter.x, appState.targetCircleCenter.y)
        canvas.rotate(appState.protractorRotationAngle)

        val lineLength = max(viewWidthProvider(), viewHeightProvider()) * 2f

        config.PROTRACTOR_ANGLES.forEach { angle ->
            val rad = Math.toRadians(angle.toDouble())
            val endX = (lineLength * sin(rad)).toFloat()
            val endY = -(lineLength * cos(rad)).toFloat()

            if (angle == 0f) {
                // This draws the main axis line of the protractor, now using targetLineGuidePaint
                canvas.drawLine(0f, 0f, endX, endY, appPaints.targetLineGuidePaint) // Main axis line (yellow)
                canvas.drawLine(0f, 0f, -endX, -endY, appPaints.protractorAngleLinePaint) // Opposite side
            } else {
                canvas.drawLine(0f, 0f, endX, endY, appPaints.protractorAngleLinePaint)
                canvas.drawLine(0f, 0f, -endX, -endY, appPaints.protractorAngleLinePaint)

                val negRad = Math.toRadians(-angle.toDouble())
                val negEndX = (lineLength * sin(negRad)).toFloat()
                val negEndY = -(lineLength * cos(negRad)).toFloat()
                canvas.drawLine(0f, 0f, negEndX, negEndY, appPaints.protractorAngleLinePaint)
                canvas.drawLine(0f, 0f, -negEndX, -negEndY, appPaints.protractorAngleLinePaint)
            }
        }
        canvas.restore()
    }
}