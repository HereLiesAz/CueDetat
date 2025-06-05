package com.hereliesaz.cuedetat.drawing.screen.labels

import android.graphics.Canvas
import android.graphics.PointF
import com.hereliesaz.cuedetat.config.AppConfig

import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.drawing.utility.TextLayoutHelper

class PanHintDrawer(
    private val textLayoutHelper: TextLayoutHelper,
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {
    private val TEXT_STRING = "Pan: Rotate"
    private val X_OFFSET_DP = 20f

    fun draw(
        canvas: Canvas,
        appState: AppState,
        appPaints: AppPaints,
        config: AppConfig
    ) {
        // Only draw if in AIMING mode and helper texts are visible
        if (!appState.isInitialized || !appState.areHelperTextsVisible || appState.currentMode != AppState.SelectionMode.AIMING) return

        val paint = appPaints.panHintPaint
        paint.textSize = config.HINT_TEXT_BASE_SIZE * config.HINT_TEXT_SIZE_MULTIPLIER
        // paint.textAlign is LEFT from AppPaints

        val yBase = viewHeightProvider() - 20f // Bottom reference
        val preferredY = yBase - paint.fontSpacing // Y for baseline, above pinch hint

        // Nudge reference: a point near its typical location to keep nudging local
        val nudgeRef = PointF(X_OFFSET_DP + (paint.measureText(TEXT_STRING) / 2f), preferredY)

        textLayoutHelper.layoutAndDrawText(
            canvas, TEXT_STRING, X_OFFSET_DP, preferredY, paint, 0f, nudgeRef
        )
    }
}