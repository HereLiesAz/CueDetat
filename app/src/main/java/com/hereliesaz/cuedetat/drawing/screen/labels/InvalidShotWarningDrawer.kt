package com.hereliesaz.cuedetat.drawing.screen.labels

import android.graphics.Canvas
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.drawing.utility.TextLayoutHelper // Received, but not used for drawing this specific text

class InvalidShotWarningDrawer(
    private val textLayoutHelper: TextLayoutHelper, // Kept for signature consistency, not used for drawing
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {
    private val WARNING_TEXT_AREA_START_X_PADDING_LOCAL = 32f
    private val WARNING_TEXT_AREA_RIGHT_MARGIN_LOCAL = 190f
    private val WARNING_TEXT_VERTICAL_CENTER_TARGET_PERCENT = 0.45f

    fun draw(
        canvas: Canvas,
        appState: AppState, // For config access if needed, though font size is direct from AppConfig
        appPaints: AppPaints,
        config: AppConfig, // Pass AppConfig explicitly
        warningStringToDisplay: String?
    ) {
        if (warningStringToDisplay == null) return

        val screenWidth = viewWidthProvider()
        val screenHeight = viewHeightProvider()

        val warningPaint = TextPaint(appPaints.invalidShotWarningPaint)
        warningPaint.textSize = config.INVALID_SHOT_WARNING_BASE_SIZE // Directly from AppConfig
        // textAlign for StaticLayout internal alignment: ALIGN_OPPOSITE will make lines right-align in the block
        // The paint's textAlign (set to CENTER in AppPaints) is less relevant here.

        val staticLayoutMaxWidth = (screenWidth - WARNING_TEXT_AREA_START_X_PADDING_LOCAL - WARNING_TEXT_AREA_RIGHT_MARGIN_LOCAL).toInt().coerceAtLeast(1)

        val staticLayout = StaticLayout.Builder.obtain(
            warningStringToDisplay, 0, warningStringToDisplay.length,
            warningPaint, staticLayoutMaxWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
            .setLineSpacing(0f, 1.0f)
            .setIncludePad(false)
            .build()

        val textBlockActualWidth = staticLayout.width
        val textBlockHeight = staticLayout.height

        val textBlockX = screenWidth - textBlockActualWidth - WARNING_TEXT_AREA_RIGHT_MARGIN_LOCAL
        val verticalCenterTarget = screenHeight * WARNING_TEXT_VERTICAL_CENTER_TARGET_PERCENT
        val textBlockY = verticalCenterTarget - (textBlockHeight / 2f)

        canvas.save()
        canvas.translate(textBlockX.coerceAtLeast(0f), textBlockY.coerceAtLeast(0f))
        staticLayout.draw(canvas)
        canvas.restore()
    }
}