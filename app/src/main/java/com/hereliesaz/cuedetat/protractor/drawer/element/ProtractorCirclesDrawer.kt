package com.hereliesaz.cuedetat.protractor.drawer.element

import android.graphics.Canvas
import com.hereliesaz.cuedetat.protractor.ProtractorPaints
import com.hereliesaz.cuedetat.protractor.ProtractorState
import com.hereliesaz.cuedetat.ui.theme.AppBlack
import com.hereliesaz.cuedetat.ui.theme.AppWhite
import androidx.compose.ui.graphics.toArgb

class ProtractorCirclesDrawer {
    fun draw(
        canvas: Canvas,
        state: ProtractorState,
        paints: ProtractorPaints,
        useErrorColorForCueCircle: Boolean
    ) {
        // Target Circle
        canvas.drawCircle(state.targetCircleCenter.x, state.targetCircleCenter.y, state.currentLogicalRadius, paints.targetCirclePaint)
        val originalCenterMarkColor = paints.centerMarkPaint.color
        paints.centerMarkPaint.color = AppBlack.toArgb()
        canvas.drawCircle(state.targetCircleCenter.x, state.targetCircleCenter.y, state.currentLogicalRadius / 5f, paints.centerMarkPaint)

        // Cue Circle
        paints.cueCirclePaint.color = if (useErrorColorForCueCircle) paints.M3_COLOR_ERROR else AppWhite.toArgb()
        canvas.drawCircle(state.cueCircleCenter.x, state.cueCircleCenter.y, state.currentLogicalRadius, paints.cueCirclePaint)
        paints.centerMarkPaint.color = if (useErrorColorForCueCircle) AppWhite.toArgb() else AppBlack.toArgb()
        canvas.drawCircle(state.cueCircleCenter.x, state.cueCircleCenter.y, state.currentLogicalRadius / 5f, paints.centerMarkPaint)

        paints.centerMarkPaint.color = originalCenterMarkColor // Restore
    }
}