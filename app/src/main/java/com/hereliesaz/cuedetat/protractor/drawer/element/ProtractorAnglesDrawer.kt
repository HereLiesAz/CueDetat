package com.hereliesaz.cuedetat.protractor.drawer.element

import android.graphics.Canvas
import com.hereliesaz.cuedetat.protractor.ProtractorConfig
import com.hereliesaz.cuedetat.protractor.ProtractorPaints
import com.hereliesaz.cuedetat.protractor.ProtractorState
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class ProtractorAnglesDrawer(
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {
    fun draw(
        canvas: Canvas,
        state: ProtractorState,
        paints: ProtractorPaints,
        config: ProtractorConfig
    ) {
        canvas.save()
        canvas.translate(state.targetCircleCenter.x, state.targetCircleCenter.y)
        canvas.rotate(state.protractorRotationAngle)
        val lineLength = max(viewWidthProvider(), viewHeightProvider()) * 2f
        config.PROTRACTOR_ANGLES.forEach { angle ->
            val rad = Math.toRadians(angle.toDouble())
            val endX1 = (lineLength * sin(rad)).toFloat()
            val endY1 = (lineLength * cos(rad)).toFloat()
            if (angle == 0f) {
                canvas.drawLine(0f, 0f, endX1, endY1, paints.protractorLinePaint)
                canvas.drawLine(0f, 0f, -endX1, -endY1, paints.yellowTargetLinePaint)
            } else {
                canvas.drawLine(0f, 0f, endX1, endY1, paints.protractorLinePaint)
                canvas.drawLine(0f, 0f, -endX1, -endY1, paints.protractorLinePaint)
                val negRad = Math.toRadians(-angle.toDouble())
                val negEndX1 = (lineLength * sin(negRad)).toFloat()
                val negEndY1 = (lineLength * cos(negRad)).toFloat()
                canvas.drawLine(0f, 0f, negEndX1, negEndY1, paints.protractorLinePaint)
                canvas.drawLine(0f, 0f, -negEndX1, -negEndY1, paints.protractorLinePaint)
            }
        }
        canvas.restore()
    }
}