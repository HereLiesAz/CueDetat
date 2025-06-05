package com.hereliesaz.cuedetat.drawing.screen.labels

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.state.AppState.SelectionMode

class SelectionInstructionDrawer(
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {
    private val PADDING_HORIZONTAL = 32f
    private val TEXT_AREA_TOP_PERCENT = 0.2f
    private val TEXT_AREA_BOTTOM_PERCENT = 0.8f

    fun draw(
        canvas: Canvas,
        appState: AppState,
        appPaints: AppPaints,
        config: AppConfig
    ) {
        // Only draw if not in AIMING mode and helper texts are visible
        if (appState.currentMode == SelectionMode.AIMING || !appState.areHelperTextsVisible) return

        val instructionText = when (appState.currentMode) {
            SelectionMode.SELECTING_CUE_BALL -> "Tap the cue ball to select it."
            SelectionMode.SELECTING_TARGET_BALL -> "Now tap the target ball."
            else -> "" // Should not happen given the initial check
        }

        if (instructionText.isEmpty()) return

        val paint = TextPaint(appPaints.selectionInstructionPaint)
        paint.textSize = config.GHOST_BALL_NAME_BASE_SIZE * 1.1f // Slightly larger than ghost ball names
        paint.textAlign = Paint.Align.CENTER // Ensure text is centered by StaticLayout

        val screenWidth = viewWidthProvider()
        val screenHeight = viewHeightProvider()

        val horizontalPadding = PADDING_HORIZONTAL
        val maxWidth = (screenWidth - 2 * horizontalPadding).toInt().coerceAtLeast(1)

        val staticLayout = StaticLayout.Builder.obtain(
            instructionText, 0, instructionText.length,
            paint, maxWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.0f)
            .setIncludePad(false)
            .build()

        val textBlockHeight = staticLayout.height

        // Calculate Y position to center the block vertically within a defined area
        val availableHeight = screenHeight * (TEXT_AREA_BOTTOM_PERCENT - TEXT_AREA_TOP_PERCENT)
        val areaTopY = screenHeight * TEXT_AREA_TOP_PERCENT
        val preferredY = areaTopY + (availableHeight / 2f) - (textBlockHeight / 2f)

        val drawX = (screenWidth / 2f) - (staticLayout.width / 2f) // Center horizontally

        canvas.save()
        canvas.translate(drawX.coerceAtLeast(0f), preferredY.coerceAtLeast(0f))
        staticLayout.draw(canvas)
        canvas.restore()
    }
}