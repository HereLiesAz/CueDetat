package com.hereliesaz.cuedetat.protractor

import android.content.Context
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.ui.theme.*


class ProtractorPaints(private val context: Context, private val config: ProtractorConfig) {

    private val archivoBlackTypeface: Typeface? = ResourcesCompat.getFont(context, R.font.archivo_black_regular)

    var M3_COLOR_PRIMARY: Int = Color.BLUE
    // ... (other M3 vars)
    var M3_COLOR_SECONDARY: Int = Color.RED
    var M3_COLOR_TERTIARY: Int = Color.GREEN
    var M3_COLOR_ON_SURFACE: Int = Color.WHITE
    var M3_COLOR_OUTLINE: Int = Color.LTGRAY
    var M3_COLOR_ERROR: Int = Color.RED
    var M3_COLOR_PRIMARY_CONTAINER: Int = Color.CYAN
    var M3_COLOR_SECONDARY_CONTAINER: Int = Color.MAGENTA
    var M3_TEXT_SHADOW_COLOR: Int = Color.argb(180, 0, 0, 0)
    var m3GlowColor: Int = Color.argb(100, 255, 255, 224)

    // ... (visual element paints: targetCirclePaint, cueCirclePaint, etc. remain the same)
    val targetCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f; color = AppYellow.toArgb() }
    val cueCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f }
    val centerMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val protractorLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 3f }
    val yellowTargetLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AppYellow.toArgb(); strokeWidth = config.O_YELLOW_TARGET_LINE_STROKE }
    val ghostCueOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
    val targetGhostBallOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AppYellow.toArgb(); style = Paint.Style.STROKE; strokeWidth = 3f }
    val aimingAssistNearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = config.O_NEAR_DEFAULT_STROKE }
    val aimingAssistFarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = config.O_FAR_DEFAULT_STROKE }
    val aimingSightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 2f; style = Paint.Style.STROKE }

    val ghostBallInstructionTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = archivoBlackTypeface }

    val cueDeflectionDottedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = config.O_CUE_DEFLECTION_STROKE_WIDTH; style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f) }
    val cueDeflectionHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = config.O_CUE_DEFLECTION_STROKE_WIDTH + config.BOLD_STROKE_INCREASE; style = Paint.Style.STROKE; pathEffect = null }

    // Helper Text Paints
    val helperTextPhoneOverCuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AppHelpTextPhoneOverCue.toArgb(); typeface = archivoBlackTypeface } // Size set in drawer
    val helperTextTangentLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AppHelpTextTangentLine.toArgb(); typeface = archivoBlackTypeface } // Size set in drawer
    val helperTextPocketAimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AppHelpTextPocketAim.toArgb(); typeface = archivoBlackTypeface } // Size set in drawer
    val helperTextProjectedShotLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AppHelpTextPurple.toArgb(); typeface = archivoBlackTypeface } // Size set in drawer

    // New paint for "Cue Ball Path" - using AppPurple
    val helperTextCueBallPathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AppPurple.toArgb(); typeface = archivoBlackTypeface } // Size set in drawer


    // Labels for the Ghost Balls themselves
    val targetBallLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AppHelpTextTargetBallLabel.toArgb(); textAlign = Paint.Align.CENTER; typeface = archivoBlackTypeface } // Size set in drawer
    val cueBallLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AppHelpTextCueBallLabel.toArgb(); textAlign = Paint.Align.CENTER; typeface = archivoBlackTypeface } // Size set in drawer


    val helperTextInteractionHintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AppHelpTextDefault.toArgb()
        textAlign = Paint.Align.LEFT
        typeface = archivoBlackTypeface
    }
    val insultingWarningTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AppWarningText.toArgb()
        textAlign = Paint.Align.CENTER
        typeface = archivoBlackTypeface ?: Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(3f, 2f, 2f, Color.argb(180,0,0,0))
    }

    fun applyMaterialYouColors(colorScheme: ColorScheme) {
        // ... M3 color assignments ...
        M3_COLOR_PRIMARY = colorScheme.primary.toArgb()
        M3_COLOR_SECONDARY = colorScheme.secondary.toArgb()
        M3_COLOR_TERTIARY = colorScheme.tertiary.toArgb()
        M3_COLOR_ON_SURFACE = colorScheme.onSurface.toArgb()
        M3_COLOR_OUTLINE = colorScheme.outline.toArgb()
        M3_COLOR_ERROR = colorScheme.error.toArgb()
        M3_COLOR_PRIMARY_CONTAINER = colorScheme.primaryContainer.toArgb()
        M3_COLOR_SECONDARY_CONTAINER = colorScheme.secondaryContainer.toArgb()

        val primaryComposeColor = colorScheme.primary
        m3GlowColor = Color.argb(100, Color.red(primaryComposeColor.toArgb()), Color.green(primaryComposeColor.toArgb()), Color.blue(primaryComposeColor.toArgb()))

        val surfaceColor = colorScheme.surface.toArgb()
        val surfaceBrightness = (Color.red(surfaceColor) * 299 + Color.green(surfaceColor) * 587 + Color.blue(surfaceColor) * 114) / 1000
        M3_TEXT_SHADOW_COLOR = if (surfaceBrightness < 128) Color.argb(180,220,220,220) else Color.argb(180, 30,30,30)

        // Base paints
        targetCirclePaint.color = AppYellow.toArgb()
        centerMarkPaint.color = M3_COLOR_ON_SURFACE
        val tertiaryBase = M3_COLOR_TERTIARY
        protractorLinePaint.color = Color.argb(170, Color.red(tertiaryBase), Color.green(tertiaryBase), Color.blue(tertiaryBase))
        aimingSightPaint.color = AppYellow.toArgb()

        ghostBallInstructionTextPaint.setShadowLayer(2f, 1f, 1f, M3_TEXT_SHADOW_COLOR)

        cueDeflectionDottedPaint.color = AppWhite.toArgb()
        cueDeflectionHighlightPaint.color = AppWhite.toArgb()
        cueDeflectionHighlightPaint.setShadowLayer(config.GLOW_RADIUS_FIXED, 0f, 0f, Color.argb(100,255,255,255))

        // Apply shadows to helper texts
        val helperShadowColor = Color.argb(120, 0,0,0)
        val allHelperTextPaints = listOf(
            helperTextPhoneOverCuePaint, helperTextTangentLinePaint, helperTextPocketAimPaint,
            helperTextProjectedShotLinePaint, helperTextCueBallPathPaint,
            targetBallLabelPaint, cueBallLabelPaint, helperTextInteractionHintPaint
        )
        allHelperTextPaints.forEach {
            it.setShadowLayer(1.5f,1.5f,1.5f, helperShadowColor)
            it.typeface = archivoBlackTypeface ?: it.typeface // Ensure font is set
        }
        // insultingWarningTextPaint shadow and typeface set at init
    }
    fun resetDynamicPaintProperties() {
        yellowTargetLinePaint.apply {
            strokeWidth = config.O_YELLOW_TARGET_LINE_STROKE
            color = AppYellow.toArgb(); clearShadowLayer()
        }
    }
}