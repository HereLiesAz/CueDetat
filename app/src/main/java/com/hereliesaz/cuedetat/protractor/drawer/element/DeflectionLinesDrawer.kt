package com.hereliesaz.cuedetat.protractor.drawer.element

import android.graphics.Canvas
import com.hereliesaz.cuedetat.protractor.ProtractorPaints
import com.hereliesaz.cuedetat.protractor.ProtractorState
import com.hereliesaz.cuedetat.protractor.calculator.DeflectionLineParams // Correct import

class DeflectionLinesDrawer {
    fun draw(
        canvas: Canvas,
        state: ProtractorState,
        paints: ProtractorPaints,
        deflectionParams: DeflectionLineParams, // Now resolved
        useErrorColor: Boolean
    ) {
        if (deflectionParams.tBPMagnitude <= 0.001f) return // Access via deflectionParams

        var paintForDir1 = paints.cueDeflectionDottedPaint
        var paintForDir2 = paints.cueDeflectionDottedPaint

        if (!useErrorColor) {
            val alphaDeg = state.protractorRotationAngle; val epsilon = 0.5f
            if (alphaDeg > epsilon && alphaDeg < (180f - epsilon)) {
                paintForDir2 = paints.cueDeflectionHighlightPaint
            } else if (alphaDeg > (180f + epsilon) && alphaDeg < (360f - epsilon)) {
                paintForDir1 = paints.cueDeflectionHighlightPaint
            }
        }
        canvas.drawLine(
            state.cueCircleCenter.x, state.cueCircleCenter.y,
            state.cueCircleCenter.x + deflectionParams.unitVecX * deflectionParams.drawLength, // Access via deflectionParams
            state.cueCircleCenter.y + deflectionParams.unitVecY * deflectionParams.drawLength, // Access via deflectionParams
            paintForDir1
        )
        canvas.drawLine(
            state.cueCircleCenter.x, state.cueCircleCenter.y,
            state.cueCircleCenter.x - deflectionParams.unitVecX * deflectionParams.drawLength, // Access via deflectionParams
            state.cueCircleCenter.y - deflectionParams.unitVecY * deflectionParams.drawLength, // Access via deflectionParams
            paintForDir2
        )
    }
}