// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/PaintCache.kt

package com.hereliesaz.cuedetat.view

import android.graphics.BlurMaskFilter
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.ui.theme.RebelYellow
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import com.hereliesaz.cuedetat.view.state.OverlayState

class PaintCache {
    val textPaint: Paint = createTextPaint()
    val shotLinePaint: Paint = createLinePaint()
    val targetCirclePaint: Paint = createCirclePaint()
    val tableOutlinePaint: Paint = createLinePaint()
    val lineGlowPaint: Paint = createGlowPaint()
    val ballGlowPaint: Paint = createGlowPaint()
    val tangentLineSolidPaint: Paint = createLinePaint()
    val tangentLineDottedPaint: Paint = createDottedLinePaint()
    val angleGuidePaint: Paint = createDottedLinePaint()
    val bankLine1Paint: Paint = createLinePaint()
    val warningPaint: Paint = createWarningPaint()
    val fillPaint: Paint = createFillPaint()
    val pathObstructionPaint: Paint = createPathObstructionPaint()
    val tableFillPaint: Paint = createTableFillPaint()
    val pocketPaint: Paint = createPocketPaint()
    val cvResultPaint: Paint = createCvResultPaint()

    fun setTypeface(typeface: Typeface?) {
        textPaint.typeface = typeface
    }

    fun updateColors(uiState: OverlayState, isDark: Boolean) {
        // This function can be expanded to update paint colors based on theme changes if needed.
    }

    private fun createTextPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }

    private fun createLinePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.WHITE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private fun createGlowPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        // The one true way to create a fuzzy glow
        maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
    }

    private fun createCirclePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = RebelYellow.toArgb()
    }

    private fun createDottedLinePaint() = Paint(createLinePaint()).apply {
        pathEffect = DashPathEffect(floatArrayOf(20f, 20f), 0f)
    }

    private fun createWarningPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = WarningRed.toArgb()
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }

    private fun createFillPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private fun createPathObstructionPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = WarningRed.copy(alpha = 0.1f).toArgb()
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    }

    private fun createTableFillPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.DKGRAY
    }

    private fun createPocketPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }

    private fun createCvResultPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RebelYellow.copy(alpha = 0.5f).toArgb()
    }
}