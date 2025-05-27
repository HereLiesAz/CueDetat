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
import com.hereliesaz.cuedetat.ui.theme.AppHelpTextDefault



class ProtractorPaints(private val context: Context, private val config: ProtractorConfig) {

    private val archivoBlackTypeface: Typeface? = ResourcesCompat.getFont(context, R.font.archivo_black_regular)

    var M3_COLOR_PRIMARY: Int = android.graphics.Color.BLUE
    var M3_COLOR_SECONDARY: Int = android.graphics.Color.RED
    var M3_COLOR_TERTIARY: Int = android.graphics.Color.GREEN
    var M3_COLOR_ON_SURFACE: Int = android.graphics.Color.WHITE
    var M3_COLOR_OUTLINE: Int = android.graphics.Color.LTGRAY
    var M3_COLOR_ERROR: Int = android.graphics.Color.RED
    var M3_COLOR_PRIMARY_CONTAINER: Int = android.graphics.Color.CYAN
    var M3_COLOR_SECONDARY_CONTAINER: Int = android.graphics.Color.MAGENTA
    var M3_TEXT_SHADOW_COLOR: Int = android.graphics.Color.argb(180, 0, 0, 0)
    var m3GlowColor: Int = android.graphics.Color.argb(100, 255, 255, 224)

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
    val helperTextPhoneOverCuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AppHelpTextPhoneOverCue.toArgb(); typeface = archivoBlackTypeface; textAlign = Paint.Align.CENTER }
    val helperTextTangentLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AppHelpTextTangentLine.toArgb(); typeface = archivoBlackTypeface; textAlign = Paint.Align.CENTER }
    val helperTextPocketAimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AppHelpTextPocketAim.toArgb(); typeface = archivoBlackTypeface; textAlign = Paint.Align.CENTER }
    val helperTextProjectedShotLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AppHelpTextProjectedShotLineActual.toArgb(); typeface = archivoBlackTypeface; textAlign = Paint.Align.CENTER }

    // helperTextCueBallPathPaint is used for "Tangent Line" text, revert to CENTER
    val helperTextCueBallPathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AppPurple.toArgb(); typeface = archivoBlackTypeface; textAlign = Paint.Align.CENTER }


    // Labels for the Ghost Balls themselves
    val targetBallLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AppHelpTextTargetBallLabel.toArgb(); textAlign = Paint.Align.CENTER; typeface = archivoBlackTypeface }
    val cueBallLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AppHelpTextGhostBallLabel.toArgb(); textAlign = Paint.Align.CENTER; typeface = archivoBlackTypeface }


    val helperTextInteractionHintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AppHelpTextDefault.toArgb()
        textAlign = Paint.Align.LEFT
        typeface = archivoBlackTypeface
    }
    val insultingWarningTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AppWarningText.toArgb()
        textAlign = Paint.Align.CENTER
        typeface = archivoBlackTypeface ?: Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(3f, 2f, 2f, android.graphics.Color.argb(180,0,0,0))
    }

    fun applyMaterialYouColors(colorScheme: ColorScheme) {
        M3_COLOR_PRIMARY = colorScheme.primary.toArgb()
        M3_COLOR_SECONDARY = colorScheme.secondary.toArgb()
        M3_COLOR_TERTIARY = colorScheme.tertiary.toArgb()
        M3_COLOR_ON_SURFACE = colorScheme.onSurface.toArgb()
        M3_COLOR_OUTLINE = colorScheme.outline.toArgb()
        M3_COLOR_ERROR = colorScheme.error.toArgb()
        M3_COLOR_PRIMARY_CONTAINER = colorScheme.primaryContainer.toArgb()
        M3_COLOR_SECONDARY_CONTAINER = colorScheme.secondaryContainer.toArgb()

        val primaryComposeColor = colorScheme.primary
        m3GlowColor = android.graphics.Color.argb(100, android.graphics.Color.red(primaryComposeColor.toArgb()), android.graphics.Color.green(primaryComposeColor.toArgb()), android.graphics.Color.blue(primaryComposeColor.toArgb()))

        val surfaceColor = colorScheme.surface.toArgb()
        val surfaceBrightness = (android.graphics.Color.red(surfaceColor) * 299 + android.graphics.Color.green(surfaceColor) * 587 + android.graphics.Color.blue(surfaceColor) * 114) / 1000
        M3_TEXT_SHADOW_COLOR = if (surfaceBrightness < 128) android.graphics.Color.argb(180,220,220,220) else android.graphics.Color.argb(180, 30,30,30)

        targetCirclePaint.color = AppYellow.toArgb()
        centerMarkPaint.color = M3_COLOR_ON_SURFACE
        val tertiaryBase = M3_COLOR_TERTIARY
        protractorLinePaint.color = android.graphics.Color.argb(170, android.graphics.Color.red(tertiaryBase), android.graphics.Color.green(tertiaryBase), android.graphics.Color.blue(tertiaryBase))
        aimingSightPaint.color = AppYellow.toArgb()

        ghostBallInstructionTextPaint.setShadowLayer(2f, 1f, 1f, M3_TEXT_SHADOW_COLOR)

        cueDeflectionDottedPaint.color = AppWhite.toArgb()
        cueDeflectionHighlightPaint.color = AppWhite.toArgb()
        cueDeflectionHighlightPaint.setShadowLayer(config.GLOW_RADIUS_FIXED, 0f, 0f, android.graphics.Color.argb(100,255,255,255))

        val helperShadowColor = android.graphics.Color.argb(120, 0,0,0)

        val allPaints = listOf(
            helperTextPhoneOverCuePaint, helperTextTangentLinePaint, helperTextPocketAimPaint,
            helperTextProjectedShotLinePaint, helperTextCueBallPathPaint,
            targetBallLabelPaint, cueBallLabelPaint, ghostBallInstructionTextPaint,
            helperTextInteractionHintPaint // This one will have its textAlign set to LEFT below
        )
        allPaints.forEach {
            it.setShadowLayer(1.5f,1.5f,1.5f, helperShadowColor)
            it.typeface = archivoBlackTypeface ?: it.typeface
            if (it != helperTextInteractionHintPaint) { // Ensure most are centered
                it.textAlign = Paint.Align.CENTER
            }
        }
        helperTextInteractionHintPaint.textAlign = Paint.Align.LEFT // Explicitly LEFT
    }

    fun resetDynamicPaintProperties() {
        yellowTargetLinePaint.apply {
            strokeWidth = config.O_YELLOW_TARGET_LINE_STROKE
            color = AppYellow.toArgb(); clearShadowLayer()
        }
    }
}