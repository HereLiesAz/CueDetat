package com.hereliesaz.cuedetat.drawing.screen.labels

import android.graphics.Canvas
import android.graphics.PointF
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.drawing.utility.TextLayoutHelper

class PlaceCueInstructionDrawer(
    private val textLayoutHelper: TextLayoutHelper,
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {
    private val TEXT_STRING = "Center actual cue ball here,\nunder your phone."

    fun draw(
        canvas: Canvas,
        appState: AppState,
        appPaints: AppPaints,
        config: AppConfig
    ) {
        if (!appState.isInitialized || !appState.areHelperTextsVisible) return

        val paint = appPaints.placeCueInstructionPaint
        // This text is static size, not affected by appState.zoomFactor for its base size
        paint.textSize = config.GHOST_BALL_NAME_BASE_SIZE * config.PLACE_CUE_INSTRUCTION_BASE_SIZE_FACTOR
        // paint.textAlign is CENTER from AppPaints

        val preferredX = viewWidthProvider() / 2f
        // Positioned near bottom, moved up from old logic: e.g., 85% of screen height
        val preferredY = viewHeightProvider() * 0.85f // Y is the center of the text block

        // Nudge reference can be screen center or its own center
        val nudgeRef = PointF(preferredX, preferredY)

        textLayoutHelper.layoutAndDrawText(
            canvas, TEXT_STRING, preferredX, preferredY, paint, 0f, nudgeRef
        )
    }
}