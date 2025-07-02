package com.hereliesaz.cuedetatlite.view

import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.hereliesaz.cuedetatlite.ui.theme.*
import com.hereliesaz.cuedetatlite.view.state.OverlayState

class PaintCache {
    private val GLOW_RADIUS_DEFAULT = 12f
    private val GLOW_ALPHA = 0.5f

    private val baseStrokeWidthMultiplier = 1.2f
    private val ballOutlineStrokeWidth = 6f * baseStrokeWidthMultiplier
    private val lineStrokeWidth = 5f * baseStrokeWidthMultiplier
    private val thickLineStrokeWidth = 6f * baseStrokeWidthMultiplier
    private val thinLineStrokeWidth = 3f * baseStrokeWidthMultiplier

    private val DrawingDarkColorScheme: ColorScheme = darkColorScheme(
        primary = AccentGold,
        secondary = AcidPatina,
        tertiary = MutedGray,
        outline = MutedGray.copy(alpha = 0.5f),
        surface = Color(0xFF1E1E1E),
        onSurface = Color(0xFFE0E0E0),
        primaryContainer = DarkerAccentGold,
        background = Color(0xFF121212)
    )
    private val DrawingLightColorScheme: ColorScheme = lightColorScheme(
        primary = DarkerAccentGold,
        secondary = RustedEmber,
        tertiary = OilSlick,
        outline = MutedGray.copy(alpha = 0.7f),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1A1A1A),
        primaryContainer = AccentGold,
        background = Color(0xFFFDFCFD)
    )

    private fun Color.adjustLuminance(factor: Float): Color {
        if (factor == 0f || this == Color.Transparent) return this
        val hsl = FloatArray(3)
        try {
            ColorUtils.colorToHSL(this.toArgb(), hsl)
            hsl[2] = (hsl[2] + factor).coerceIn(0f, 1f)
            return Color(ColorUtils.HSLToColor(hsl))
        } catch (e: IllegalArgumentException) {
            return this
        }
    }

    // Paint objects
    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE; style = Paint.Style.FILL_AND_STROKE; strokeWidth = thickLineStrokeWidth }
    val yellowCrosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = RebelYellow.toArgb(); strokeWidth = thinLineStrokeWidth; style = Paint.Style.STROKE }
    val tableOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = thickLineStrokeWidth
    }
    val targetCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = ballOutlineStrokeWidth
    }
    val cueCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = ballOutlineStrokeWidth
    }
    val targetCenterMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val cueCenterMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val protractorLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = lineStrokeWidth }
    val aimingLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = thickLineStrokeWidth }
    val ghostCueOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = ballOutlineStrokeWidth
    }
    val targetGhostBallOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = ballOutlineStrokeWidth
    }
    val actualCueBallGhostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = ballOutlineStrokeWidth
    }
    val actualCueBallBasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = thinLineStrokeWidth
    }
    val actualCueBallCenterMarkPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val shotLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = thickLineStrokeWidth; color = AndroidColor.WHITE
    }
    val aimingSightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = lineStrokeWidth; style = Paint.Style.STROKE
    }
    val tangentLineDottedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = thinLineStrokeWidth; pathEffect =
        DashPathEffect(floatArrayOf(15f, 10f), 0f)
    }
    val tangentLineSolidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = lineStrokeWidth
    }

    val bankShotLinePaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = thickLineStrokeWidth
    }
    val bankShotLinePaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = thickLineStrokeWidth
    }
    val bankShotLinePaint3 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = thickLineStrokeWidth
    }

    val cueBallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    val targetBallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    val actualCueBallTextPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    val ghostBallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    val lineTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    val warningPaintRed1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = thickLineStrokeWidth
    }
    val warningPaintRed2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = ballOutlineStrokeWidth
    }
    val warningDottedPaintRed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = thinLineStrokeWidth; pathEffect =
        DashPathEffect(floatArrayOf(15f, 10f), 0f)
    }


    fun setTypeface(typeface: Typeface?) {
        cueBallTextPaint.typeface = typeface
        targetBallTextPaint.typeface = typeface
        ghostBallTextPaint.typeface = typeface
        lineTextPaint.typeface = typeface
        actualCueBallTextPaint.typeface = typeface
    }

    private fun Paint.applyGlow(
        color: Color,
        alpha: Float = GLOW_ALPHA,
        radius: Float = GLOW_RADIUS_DEFAULT
    ) {
        if (this.style == Paint.Style.STROKE || this.style == Paint.Style.FILL_AND_STROKE) {
            this.setShadowLayer(radius, 0f, 0f, color.copy(alpha = alpha).toArgb())
        }
    }

    private fun Paint.applyGlow(colorArgb: Int, radius: Float = GLOW_RADIUS_DEFAULT) {
        if (this.style == Paint.Style.STROKE || this.style == Paint.Style.FILL_AND_STROKE) {
            this.setShadowLayer(radius, 0f, 0f, colorArgb)
        }
    }


    fun updateColors(uiState: OverlayState, systemIsDark: Boolean) {
        val LUMINANCE_ADJUST = uiState.luminanceAdjustment
        val baseDrawingScheme = when (uiState.isForceLightMode) {
            true -> DrawingLightColorScheme
            false -> DrawingDarkColorScheme
            null -> if (systemIsDark) DrawingDarkColorScheme else DrawingLightColorScheme
        }

        val currentGlowColorCompose =
            baseDrawingScheme.primary.adjustLuminance(LUMINANCE_ADJUST * 0.5f)
        val currentGlowColorArgb = currentGlowColorCompose.copy(alpha = GLOW_ALPHA).toArgb()
        val currentTextShadowColorArgb =
            baseDrawingScheme.background.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.7f)
                .toArgb()

        tableOutlinePaint.apply {
            color = TargetAcid.adjustLuminance(LUMINANCE_ADJUST).toArgb()
            applyGlow(TargetAcid.adjustLuminance(LUMINANCE_ADJUST))
        }

        yellowCrosshairPaint.color = RebelYellow.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        yellowCrosshairPaint.applyGlow(RebelYellow.adjustLuminance(LUMINANCE_ADJUST))


        targetCirclePaint.color =
            baseDrawingScheme.primary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        targetCirclePaint.applyGlow(baseDrawingScheme.primary.adjustLuminance(LUMINANCE_ADJUST))
        cueCirclePaint.color = baseDrawingScheme.tertiary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        cueCirclePaint.applyGlow(baseDrawingScheme.tertiary.adjustLuminance(LUMINANCE_ADJUST))

        targetCenterMarkPaint.color =
            baseDrawingScheme.onPrimaryContainer.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        cueCenterMarkPaint.color =
            baseDrawingScheme.onTertiary.adjustLuminance(LUMINANCE_ADJUST).toArgb()

        actualCueBallGhostPaint.color =
            baseDrawingScheme.secondary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        actualCueBallGhostPaint.applyGlow(
            baseDrawingScheme.secondary.adjustLuminance(LUMINANCE_ADJUST)
        )
        actualCueBallBasePaint.color =
            baseDrawingScheme.secondary.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.5f).toArgb()
        actualCueBallBasePaint.applyGlow(
            baseDrawingScheme.secondary.adjustLuminance(LUMINANCE_ADJUST), alpha = 0.3f
        )
        actualCueBallCenterMarkPaint.color =
            baseDrawingScheme.onSecondary.adjustLuminance(LUMINANCE_ADJUST).toArgb()

        ghostCueOutlinePaint.color =
            baseDrawingScheme.tertiary.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.7f).toArgb()
        ghostCueOutlinePaint.applyGlow(
            baseDrawingScheme.tertiary.adjustLuminance(LUMINANCE_ADJUST),
            alpha = 0.4f
        )
        targetGhostBallOutlinePaint.color =
            baseDrawingScheme.primary.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.7f).toArgb()
        targetGhostBallOutlinePaint.applyGlow(
            baseDrawingScheme.primary.adjustLuminance(LUMINANCE_ADJUST), alpha = 0.4f
        )

        protractorLinePaint.color =
            baseDrawingScheme.onSurface.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.4f).toArgb()
        protractorLinePaint.applyGlow(
            baseDrawingScheme.onSurface.adjustLuminance(LUMINANCE_ADJUST),
            alpha = 0.2f
        )

        aimingLinePaint.apply {
            color = baseDrawingScheme.primary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
            applyGlow(currentGlowColorArgb)
        }

        val bankYellowBase = RebelYellow
        bankShotLinePaint1.color = bankYellowBase.adjustLuminance(LUMINANCE_ADJUST + 0.1f).toArgb()
        bankShotLinePaint1.applyGlow(bankYellowBase.adjustLuminance(LUMINANCE_ADJUST + 0.1f))
        bankShotLinePaint2.color = bankYellowBase.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        bankShotLinePaint2.applyGlow(bankYellowBase.adjustLuminance(LUMINANCE_ADJUST))
        bankShotLinePaint3.color = bankYellowBase.adjustLuminance(LUMINANCE_ADJUST - 0.1f).toArgb()
        bankShotLinePaint3.applyGlow(bankYellowBase.adjustLuminance(LUMINANCE_ADJUST - 0.1f))

        shotLinePaint.color = Color.White.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        shotLinePaint.applyGlow(Color.White.adjustLuminance(LUMINANCE_ADJUST).copy(alpha=0.5f).toArgb())


        aimingSightPaint.apply {
            color = baseDrawingScheme.primary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
            applyGlow(currentGlowColorArgb)
        }

        tangentLineDottedPaint.color =
            baseDrawingScheme.outline.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.7f).toArgb()
        tangentLineDottedPaint.applyGlow(
            baseDrawingScheme.outline.adjustLuminance(LUMINANCE_ADJUST),
            alpha = 0.3f
        )
        tangentLineSolidPaint.apply {
            color = baseDrawingScheme.secondary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
            applyGlow(baseDrawingScheme.secondary.adjustLuminance(LUMINANCE_ADJUST))
        }

        val baseOnSurfaceForText = baseDrawingScheme.onSurface
        cueBallTextPaint.apply {
            color = baseOnSurfaceForText.adjustLuminance(LUMINANCE_ADJUST).toArgb(); setShadowLayer(
            2f, 1f, 1f, currentTextShadowColorArgb)
        }
        targetBallTextPaint.apply {
            color = baseOnSurfaceForText.adjustLuminance(LUMINANCE_ADJUST).toArgb(); setShadowLayer(
            2f, 1f, 1f, currentTextShadowColorArgb)
        }
        actualCueBallTextPaint.apply {
            color = baseOnSurfaceForText.adjustLuminance(LUMINANCE_ADJUST).toArgb(); setShadowLayer(
            2f, 1f, 1f, currentTextShadowColorArgb)
        }
        ghostBallTextPaint.apply {
            color = baseOnSurfaceForText.adjustLuminance(LUMINANCE_ADJUST).toArgb(); setShadowLayer(
            2f, 1f, 1f, currentTextShadowColorArgb)
        }
        lineTextPaint.apply {
            color = baseOnSurfaceForText.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.8f)
                .toArgb(); setShadowLayer(1f, 1f, 1f, currentTextShadowColorArgb)
        }

        // Warning Colors
        val warningColor = WarningRed.adjustLuminance(LUMINANCE_ADJUST)
        warningPaintRed1.color = warningColor.toArgb()
        warningPaintRed1.applyGlow(warningColor.toArgb(), radius = 5f)
        warningPaintRed2.color = warningColor.toArgb()
        warningPaintRed2.applyGlow(warningColor.toArgb(), radius = 5f)
        warningDottedPaintRed.color = warningColor.toArgb()
        warningDottedPaintRed.applyGlow(warningColor.toArgb(), radius = 5f)
    }
}