package com.hereliesaz.cuedetat.view

import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb

/**
 * A cache for all Paint objects used in the overlay.
 * This centralizes paint style definitions and allows for efficient color updates
 * when the theme changes, without needing to recreate Paint objects.
 */
class PaintCache {
    private val GLOW_RADIUS_FIXED = 8f
    private var glowColor: Int = Color.argb(100, 255, 255, 224)
    private var textShadowColor: Int = Color.argb(180, 0, 0, 0)

    val targetCirclePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f }
    val cueCirclePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f }
    val centerMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val protractorLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 3f }
    val shotPathLinePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 5f } // Formerly yellowTargetLinePaint
    val ghostCueOutlinePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
    val targetGhostBallOutlinePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
    val aimingAssistNearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val aimingAssistFarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val aimingSightPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 2f; style = Paint.Style.STROKE }
    val ghostBallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    val tangentLineDottedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
    }
    val tangentLineSolidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        pathEffect = null
    }

    /**
     * Updates all paint colors based on the provided Material 3 ColorScheme.
     */
    fun updateColors(colorScheme: ColorScheme) {
        val primaryColor = colorScheme.primary.toArgb()
        val onSurfaceColor = colorScheme.onSurface.toArgb()

        glowColor = colorScheme.primary.copy(alpha = 0.4f).toArgb()
        val surfaceBrightness =
            (Color.red(colorScheme.surface.toArgb()) * 299 + Color.green(colorScheme.surface.toArgb()) * 587 + Color.blue(
                colorScheme.surface.toArgb()
            ) * 114) / 1000
        textShadowColor =
            if (surfaceBrightness < 128) Color.argb(180, 220, 220, 220) else Color.argb(
                180,
                30,
                30,
                30
            )

        targetCirclePaint.color = colorScheme.secondary.toArgb()
        cueCirclePaint.color = primaryColor
        centerMarkPaint.color = onSurfaceColor
        protractorLinePaint.color = colorScheme.tertiary.copy(alpha = 0.7f).toArgb()
        shotPathLinePaint.color = colorScheme.tertiary.toArgb() // Replaces yellow

        ghostCueOutlinePaint.color = colorScheme.outline.toArgb()
        targetGhostBallOutlinePaint.color = colorScheme.tertiary.toArgb()
        aimingSightPaint.color = onSurfaceColor
        ghostBallTextPaint.apply {
            color = onSurfaceColor
            setShadowLayer(2f, 1f, 1f, textShadowColor)
        }

        tangentLineDottedPaint.color = colorScheme.outline.toArgb() // Muted color
        tangentLineSolidPaint.apply {
            color = onSurfaceColor // White / Black
            setShadowLayer(GLOW_RADIUS_FIXED, 0f, 0f, glowColor)
        }
    }
}
