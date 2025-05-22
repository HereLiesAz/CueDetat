package com.hereliesaz.cuedetat.protractor.drawer

import android.graphics.Canvas
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.protractor.ProtractorConfig
import com.hereliesaz.cuedetat.protractor.ProtractorPaints
import com.hereliesaz.cuedetat.protractor.ProtractorState
import com.hereliesaz.cuedetat.protractor.calculator.DeflectionLineParams
import com.hereliesaz.cuedetat.protractor.drawer.element.DeflectionLinesDrawer
import com.hereliesaz.cuedetat.protractor.drawer.element.ProtractorAnglesDrawer
import com.hereliesaz.cuedetat.protractor.drawer.element.ProtractorCirclesDrawer
// Removed unused math imports like PI, atan2 if only used by text previously

class ProtractorPlaneDrawer(
    viewWidthProvider: () -> Int,
    viewHeightProvider: () -> Int
) {
    private val circlesDrawer = ProtractorCirclesDrawer()
    private val deflectionLinesDrawer = DeflectionLinesDrawer()
    private val anglesDrawer = ProtractorAnglesDrawer(viewWidthProvider, viewHeightProvider)

    // Renamed from 'draw' to be more specific
    fun drawPlaneVisuals(
        canvas: Canvas,
        state: ProtractorState,
        paints: ProtractorPaints,
        config: ProtractorConfig,
        useErrorColorForCueCircleAndShotGuide: Boolean,
        aimLineStartX: Float, aimLineStartY: Float,
        aimLineCueX: Float, aimLineCueY: Float,
        aimLineEndX: Float, aimLineEndY: Float,
        deflectionParams: DeflectionLineParams
    ) {
        // Aiming Assist Line (Shot Guide)
        if (aimLineStartX != 0f || aimLineStartY != 0f || aimLineEndX !=0f || aimLineEndY !=0f ) {
            canvas.drawLine(aimLineStartX, aimLineStartY, aimLineCueX, aimLineCueY, paints.aimingAssistNearPaint)
            canvas.drawLine(aimLineCueX, aimLineCueY, aimLineEndX, aimLineEndY, paints.aimingAssistFarPaint)
        }

        circlesDrawer.draw(canvas, state, paints, useErrorColorForCueCircleAndShotGuide)
        deflectionLinesDrawer.draw(canvas, state, paints, deflectionParams, useErrorColorForCueCircleAndShotGuide)
        anglesDrawer.draw(canvas, state, paints, config)
    }
}