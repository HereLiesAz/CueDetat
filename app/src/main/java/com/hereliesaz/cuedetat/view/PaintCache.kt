package com.hereliesaz.cuedetat.view

import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.ui.theme.AccentGold

/**
 * A cache for all Paint objects used in the overlay.
 * This centralizes paint style definitions and allows for efficient color updates
 * when the theme changes, without needing to recreate Paint objects.
 */
class PaintCache {
    private val GLOW_RADIUS_FIXED = 8f
    private var glowColor: Int = Color.argb(100, 255, 196, 0)
    private var textShadowColor: Int = Color.argb(180, 0, 0, 0)

    // --- Standard Paints ---
    val targetCirclePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f }
    val cueCirclePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f }
    val centerMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val protractorLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 3f }
    val shotPathLinePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 5f
            color = AccentGold.toArgb()
            setShadowLayer(GLOW_RADIUS_FIXED, 0f, 0f, AccentGold.copy(alpha = 0.5f).toArgb())
        }
    val ghostCueOutlinePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 6f }
    val targetGhostBallOutlinePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 6f }

    val aimingAssistNearPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f }
    val aimingAssistFarPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f }

    val aimingSightPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 4f; style = Paint.Style.STROKE }

    val ghostBallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    val tangentLineDottedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
    }
    val tangentLineSolidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        pathEffect = null
    }

    // --- Paints for Warning State (Muted Reds) ---
    val warningPaintRed1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C05D5D") // More muted red
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    val warningPaintRed2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A04C4C") // Darker muted red
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    val warningPaintRed3 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80E57373") // Muted Red 3 with 50% alpha
        style = Paint.Style.STROKE
        strokeWidth = 5f
        setShadowLayer(GLOW_RADIUS_FIXED, 0f, 0f, Color.parseColor("#FF5252"))
    }


    /**
     * Updates all paint colors based on the provided Material 3 ColorScheme.
     */
    fun updateColors(colorScheme: ColorScheme) {
        val onSurfaceColor = colorScheme.onSurface.toArgb()

        glowColor = colorScheme.primary.copy(alpha = 0.4f).toArgb()
        textShadowColor = Color.argb(180, 0, 0, 0)

        // --- Update Standard Paints ---
        targetCirclePaint.color = Color.parseColor("#A98B00")

        // USER REQUEST: Swapped cue ball colors.
        cueCirclePaint.color = Color.parseColor("#A9A9A9") // Was #CCCCCC
        ghostCueOutlinePaint.color = Color.parseColor("#CCCCCC") // Was #A9A9A9

        centerMarkPaint.color = onSurfaceColor
        protractorLinePaint.color = colorScheme.tertiary.copy(alpha = 0.7f).toArgb()

        targetGhostBallOutlinePaint.color = AccentGold.toArgb()

        aimingSightPaint.apply {
            color = AccentGold.toArgb()
            setShadowLayer(GLOW_RADIUS_FIXED, 0f, 0f, AccentGold.copy(alpha = 0.7f).toArgb())
        }

        tangentLineSolidPaint.apply {
            color = Color.parseColor("#4DB6AC") // Muted Teal
            setShadowLayer(GLOW_RADIUS_FIXED, 0f, 0f, glowColor)
        }

        val cueSightLineColor = Color.parseColor("#D3D3D3")
        val cueSightLineGlow = Color.parseColor("#FFFFFF")
        aimingAssistNearPaint.apply {
            color = cueSightLineColor
            setShadowLayer(GLOW_RADIUS_FIXED, 0f, 0f, cueSightLineGlow)
        }
        aimingAssistFarPaint.apply {
            color = cueSightLineColor
            setShadowLayer(GLOW_RADIUS_FIXED, 0f, 0f, cueSightLineGlow)
        }
        ghostBallTextPaint.apply {
            color = onSurfaceColor
            setShadowLayer(2f, 1f, 1f, textShadowColor)
        }
    }
}