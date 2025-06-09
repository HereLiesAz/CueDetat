package com.hereliesaz.cuedetat.view

import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt
import com.hereliesaz.cuedetat.ui.theme.AccentGold

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

    // NEW: Paint for the jumping ghost ball anchor
    val jumpingGhostBallPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f }

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
        color = "#C05D5D".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    val warningPaintRed2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#A04C4C".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    val warningPaintRed3 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#80E57373".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = 5f
        setShadowLayer(GLOW_RADIUS_FIXED, 0f, 0f, "#FF5252".toColorInt())
    }

    fun updateColors(colorScheme: ColorScheme) {
        colorScheme.onSurface.toArgb()
        val mutedGold = "#9A7B4F".toColorInt()
        val lighterMutedGold = "#BCA683".toColorInt()
        val finalTeal = "#42C4B5".toColorInt()
        val cueBallGray = "#A9A9A9".toColorInt()

        glowColor = colorScheme.primary.copy(alpha = 0.4f).toArgb()
        textShadowColor = Color.argb(180, 0, 0, 0)

        targetCirclePaint.color = mutedGold
        cueCirclePaint.color = cueBallGray
        targetCenterMarkPaint.color = cueBallGray
        cueCenterMarkPaint.color = mutedGold

        // NEW: Set color for the jumping ghost ball
        jumpingGhostBallPaint.color = finalTeal

        ghostCueOutlinePaint.color = "#CCCCCC".toColorInt()
        protractorLinePaint.color = colorScheme.tertiary.copy(alpha = 0.7f).toArgb()
        targetGhostBallOutlinePaint.color = lighterMutedGold

        shotPathLinePaint.apply {
            color = mutedGold
            setShadowLayer(
                GLOW_RADIUS_FIXED,
                0f,
                0f,
                Color.argb(100, Color.red(mutedGold), Color.green(mutedGold), Color.blue(mutedGold))
            )
        }

        aimingSightPaint.apply {
            color = AccentGold.toArgb()
            setShadowLayer(GLOW_RADIUS_FIXED, 0f, 0f, AccentGold.copy(alpha = 0.7f).toArgb())
        }

        tangentLineDottedPaint.color = colorScheme.outline.toArgb()
        tangentLineSolidPaint.apply {
            color = finalTeal
            setShadowLayer(
                GLOW_RADIUS_FIXED,
                0f,
                0f,
                Color.argb(100, Color.red(finalTeal), Color.green(finalTeal), Color.blue(finalTeal))
            )
        }

        val cueSightLineColor = "#D3D3D3".toColorInt()
        val cueSightLineGlow = "#FFFFFF".toColorInt()
        aimingAssistNearPaint.apply {
            color = cueSightLineColor
            setShadowLayer(GLOW_RADIUS_FIXED, 0f, 0f, cueSightLineGlow)
        }
        aimingAssistFarPaint.apply {
            color = cueSightLineColor
            setShadowLayer(GLOW_RADIUS_FIXED, 0f, 0f, cueSightLineGlow)
        }

        cueBallTextPaint.apply {
            color = cueBallGray
            setShadowLayer(2f, 1f, 1f, textShadowColor)
        }
        targetBallTextPaint.apply {
            color = lighterMutedGold
            setShadowLayer(2f, 1f, 1f, textShadowColor)
        }
    }
}