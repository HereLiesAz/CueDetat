package com.hereliesaz.cuedetat.view

import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb

class PaintCache {
    private val GLOW_RADIUS_FIXED = 8f
    private var glowColor: Int = Color.argb(100, 255, 196, 0)
    private var textShadowColor: Int = Color.argb(180, 0, 0, 0)

    val targetCirclePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f }
    val cueCirclePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f }
    val targetCenterMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val cueCenterMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val protractorLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 3f }
    val shotPathLinePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 5f
        }
    val ghostCueOutlinePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 6f }
    val targetGhostBallOutlinePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 6f }
    val jumpingGhostBallPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f }

    // MODIFIED: Aiming line paints now have a default color set, which will be updated by the theme.
    val aimingAssistNearPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f }
    val aimingAssistFarPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f }

    val aimingSightPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 4f; style = Paint.Style.STROKE }
    val cueBallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    val targetBallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

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

    val warningPaintRed1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C05D5D")
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    val warningPaintRed2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A04C4C")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    val warningPaintRed3 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80E57373")
        style = Paint.Style.STROKE
        strokeWidth = 5f
        setShadowLayer(GLOW_RADIUS_FIXED, 0f, 0f, Color.parseColor("#FF5252"))
    }

    fun updateColors(colorScheme: ColorScheme) {
        glowColor = colorScheme.primary.copy(alpha = 0.4f).toArgb()
        textShadowColor = colorScheme.background.copy(alpha = 0.6f).toArgb()

        targetCirclePaint.color = colorScheme.primary.toArgb()
        cueCirclePaint.color = colorScheme.tertiary.toArgb()
        targetCenterMarkPaint.color = cueCirclePaint.color
        cueCenterMarkPaint.color = targetCirclePaint.color

        jumpingGhostBallPaint.color = colorScheme.secondary.toArgb()

        ghostCueOutlinePaint.color = colorScheme.tertiary.copy(alpha = 0.7f).toArgb()
        targetGhostBallOutlinePaint.color = colorScheme.primary.copy(alpha = 0.7f).toArgb()

        protractorLinePaint.color = colorScheme.onSurface.copy(alpha = 0.2f).toArgb()
        shotPathLinePaint.apply {
            color = colorScheme.primary.toArgb()
            setShadowLayer(GLOW_RADIUS_FIXED, 0f, 0f, glowColor)
        }

        // MODIFIED: The aiming line is now brighter and uses the primary color.
        aimingAssistNearPaint.color = colorScheme.primary.toArgb()
        aimingAssistFarPaint.color = colorScheme.primary.copy(alpha = 0.5f).toArgb()


        aimingSightPaint.apply {
            color = colorScheme.primary.toArgb()
            setShadowLayer(GLOW_RADIUS_FIXED, 0f, 0f, glowColor)
        }

        tangentLineDottedPaint.color = colorScheme.outline.toArgb()
        tangentLineSolidPaint.apply {
            color = colorScheme.secondary.toArgb()
            setShadowLayer(GLOW_RADIUS_FIXED, 0f, 0f, glowColor)
        }

        cueBallTextPaint.apply {
            color = cueCirclePaint.color
            setShadowLayer(2f, 1f, 1f, textShadowColor)
        }
        targetBallTextPaint.apply {
            color = targetCirclePaint.color
            setShadowLayer(2f, 1f, 1f, textShadowColor)
        }
    }
}
