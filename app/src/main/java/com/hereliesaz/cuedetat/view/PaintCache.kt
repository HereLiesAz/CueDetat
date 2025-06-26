// app/src/main/java/com/hereliesaz/cuedetat/view/PaintCache.kt
package com.hereliesaz.cuedetat.view

import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.material3.ColorScheme // For M3 ColorScheme instances
import androidx.compose.material3.darkColorScheme // For creating M3 darkColorScheme
import androidx.compose.material3.lightColorScheme // For creating M3 lightColorScheme
import androidx.compose.ui.graphics.Color // For Compose color manipulation
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.hereliesaz.cuedetat.ui.theme.* // Import all your theme colors
import com.hereliesaz.cuedetat.view.state.OverlayState

class PaintCache {
    private val GLOW_RADIUS_DEFAULT = 12f
    private val GLOW_ALPHA = 0.5f

    private val baseStrokeWidthMultiplier = 1.2f
    private val ballOutlineStrokeWidth = 6f * baseStrokeWidthMultiplier
    private val lineStrokeWidth = 5f * baseStrokeWidthMultiplier
    private val thickLineStrokeWidth = 6f * baseStrokeWidthMultiplier
    private val thinLineStrokeWidth = 3f * baseStrokeWidthMultiplier

    // Define base M3 ColorSchemes for drawing elements.
    // These will be used as the source for colors, then luminance adjusted.
    private val DrawingDarkColorScheme: ColorScheme = darkColorScheme(
        primary = AccentGold,
        secondary = AcidPatina,
        tertiary = MutedGray,
        outline = MutedGray.copy(alpha = 0.5f),
        surface = Color(0xFF1E1E1E), // Used for text shadow base
        onSurface = Color(0xFFE0E0E0), // Used for general text, lines
        primaryContainer = DarkerAccentGold, // Used for default shot line
        // Add other M3 slots if directly referenced below and not via primary/secondary/etc.
        background = Color(0xFF121212) // For text shadow base too
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
        style = Paint.Style.STROKE; strokeWidth = thickLineStrokeWidth
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
    val ghostLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = thinLineStrokeWidth
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
    val warningPaintRed3 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = thickLineStrokeWidth
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

        // Use properties from the M3 ColorScheme (baseDrawingScheme)
        val currentGlowColorCompose =
            baseDrawingScheme.primary.adjustLuminance(LUMINANCE_ADJUST * 0.5f)
        val currentGlowColorArgb = currentGlowColorCompose.copy(alpha = GLOW_ALPHA).toArgb()
        val currentTextShadowColorArgb =
            baseDrawingScheme.background.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.7f)
                .toArgb()


        tableOutlinePaint.apply {
            color = TargetAcid.adjustLuminance(LUMINANCE_ADJUST)
                .toArgb() // TargetAcid is a specific brand color
            applyGlow(TargetAcid.adjustLuminance(LUMINANCE_ADJUST))
        }

        targetCirclePaint.color =
            baseDrawingScheme.primary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        targetCirclePaint.applyGlow(baseDrawingScheme.primary.adjustLuminance(LUMINANCE_ADJUST))
        cueCirclePaint.color = baseDrawingScheme.tertiary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        cueCirclePaint.applyGlow(baseDrawingScheme.tertiary.adjustLuminance(LUMINANCE_ADJUST))

        targetCenterMarkPaint.color =
            baseDrawingScheme.onPrimaryContainer.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        cueCenterMarkPaint.color =
            baseDrawingScheme.onTertiaryContainer // Assuming onTertiaryContainer exists or use onTertiary
                .let { it ?: baseDrawingScheme.onTertiary }.adjustLuminance(LUMINANCE_ADJUST)
                .toArgb()


        actualCueBallGhostPaint.color =
            baseDrawingScheme.secondary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        actualCueBallGhostPaint.applyGlow(
            baseDrawingScheme.secondary.adjustLuminance(
                LUMINANCE_ADJUST
            )
        )
        actualCueBallBasePaint.color =
            baseDrawingScheme.secondary.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.5f)
                .toArgb()
        actualCueBallBasePaint.applyGlow(
            baseDrawingScheme.secondary.adjustLuminance(
                LUMINANCE_ADJUST
            ), alpha = 0.3f
        )
        actualCueBallCenterMarkPaint.color =
            baseDrawingScheme.onSecondaryContainer // Assuming onSecondaryContainer exists
                .let { it ?: baseDrawingScheme.onSecondary }.adjustLuminance(LUMINANCE_ADJUST)
                .toArgb()


        ghostCueOutlinePaint.color =
            baseDrawingScheme.tertiary.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.7f).toArgb()
        ghostCueOutlinePaint.applyGlow(
            baseDrawingScheme.tertiary.adjustLuminance(LUMINANCE_ADJUST),
            alpha = 0.4f
        )
        targetGhostBallOutlinePaint.color =
            baseDrawingScheme.primary.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.7f).toArgb()
        targetGhostBallOutlinePaint.applyGlow(
            baseDrawingScheme.primary.adjustLuminance(
                LUMINANCE_ADJUST
            ), alpha = 0.4f
        )

        protractorLinePaint.color =
            baseDrawingScheme.onSurface.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.4f)
                .toArgb()
        protractorLinePaint.applyGlow(
            baseDrawingScheme.onSurface.adjustLuminance(LUMINANCE_ADJUST),
            alpha = 0.2f
        )

        ghostLinePaint.color =
            baseDrawingScheme.secondary.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.6f).toArgb()
        ghostLinePaint.applyGlow(
            baseDrawingScheme.secondary.adjustLuminance(LUMINANCE_ADJUST), alpha = 0.3f
        )

        aimingLinePaint.apply {
            color = baseDrawingScheme.primary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
            applyGlow(currentGlowColorArgb) // Use the pre-calculated ARGB glow
        }

        val bankYellowBase = RebelYellow // Defined in your Color.kt
        bankShotLinePaint1.color = bankYellowBase.adjustLuminance(LUMINANCE_ADJUST + 0.1f).toArgb()
        bankShotLinePaint1.applyGlow(bankYellowBase.adjustLuminance(LUMINANCE_ADJUST + 0.1f))
        bankShotLinePaint2.color = bankYellowBase.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        bankShotLinePaint2.applyGlow(bankYellowBase.adjustLuminance(LUMINANCE_ADJUST))
        bankShotLinePaint3.color = bankYellowBase.adjustLuminance(LUMINANCE_ADJUST - 0.1f).toArgb()
        bankShotLinePaint3.applyGlow(bankYellowBase.adjustLuminance(LUMINANCE_ADJUST - 0.1f))

        val defaultShotLineColorCompose =
            baseDrawingScheme.primaryContainer.adjustLuminance(LUMINANCE_ADJUST)
        shotLinePaint.apply {
            val warningColor1Argb = AndroidColor.parseColor("#C05D5D")
            val warningColor3BaseArgb = AndroidColor.parseColor("#80E57373")

            if (this.color != warningColor1Argb && this.color != warningColor3BaseArgb) {
                color = defaultShotLineColorCompose.toArgb()
            }
            if (this.color == defaultShotLineColorCompose.toArgb()) {
                applyGlow(
                    baseDrawingScheme.primary.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.5f)
                        .toArgb()
                )
            } else {
                if (this.color != warningColor3BaseArgb) clearShadowLayer()
            }
        }

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
            2f,
            1f,
            1f,
            currentTextShadowColorArgb
        )
        }
        targetBallTextPaint.apply {
            color = baseOnSurfaceForText.adjustLuminance(LUMINANCE_ADJUST).toArgb(); setShadowLayer(
            2f,
            1f,
            1f,
            currentTextShadowColorArgb
        )
        }
        actualCueBallTextPaint.apply {
            color = baseOnSurfaceForText.adjustLuminance(LUMINANCE_ADJUST).toArgb(); setShadowLayer(
            2f,
            1f,
            1f,
            currentTextShadowColorArgb
        )
        }
        ghostBallTextPaint.apply {
            color = baseOnSurfaceForText.adjustLuminance(LUMINANCE_ADJUST).toArgb(); setShadowLayer(
            2f,
            1f,
            1f,
            currentTextShadowColorArgb
        )
        }
        lineTextPaint.apply {
            color = baseOnSurfaceForText.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.8f)
                .toArgb(); setShadowLayer(1f, 1f, 1f, currentTextShadowColorArgb)
        }

        warningPaintRed1.color = AndroidColor.parseColor("#C05D5D")
        warningPaintRed1.applyGlow(
            WarningRed.toArgb(),
            radius = 5f
        ) // WarningRed is a Compose Color
        warningPaintRed2.color = AndroidColor.parseColor("#A04C4C")
        warningPaintRed2.applyGlow(WarningRed.copy(alpha = 0.8f).toArgb(), radius = 5f)
        warningPaintRed3.color = AndroidColor.parseColor("#80E57373")
        warningPaintRed3.setShadowLayer(
            GLOW_RADIUS_DEFAULT,
            0f,
            0f,
            AndroidColor.parseColor("#FF5252")
        )
    }
}