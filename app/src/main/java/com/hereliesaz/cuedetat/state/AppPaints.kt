package com.hereliesaz.cuedetat.state

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.ui.theme.*

class AppPaints(private val context: Context, private val config: AppConfig) {

    private val archivoBlackTypeface: Typeface? = ResourcesCompat.getFont(context, R.font.archivo_black_regular)

    var M3_COLOR_PRIMARY: Int = AndroidColor.BLUE
    var M3_COLOR_SECONDARY: Int = AndroidColor.RED
    var M3_COLOR_TERTIARY: Int = AndroidColor.GREEN
    var M3_COLOR_ON_SURFACE: Int = AndroidColor.WHITE
    var M3_COLOR_OUTLINE: Int = AndroidColor.LTGRAY
    var M3_COLOR_ERROR: Int = AndroidColor.RED
    var M3_COLOR_PRIMARY_CONTAINER: Int = AndroidColor.CYAN
    var M3_COLOR_SECONDARY_CONTAINER: Int = AndroidColor.MAGENTA
    var M3_TEXT_SHADOW_COLOR: Int = AndroidColor.argb(180, 0, 0, 0)
    var M3_GLOW_COLOR: Int = AndroidColor.argb(100, 255, 255, 224)

    val targetCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = config.STROKE_MAIN_CIRCLES }
    val cueCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = config.STROKE_MAIN_CIRCLES }
    val centerMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val protractorAngleLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = config.STROKE_PROTRACTOR_ANGLE_LINES }
    val targetLineGuidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = config.STROKE_TARGET_LINE_GUIDE }
    val deflectionDottedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = config.STROKE_DEFLECTION_LINE; style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f) }
    val deflectionSolidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = config.STROKE_DEFLECTION_LINE + config.STROKE_DEFLECTION_LINE_BOLD_INCREASE
        style = Paint.Style.STROKE; pathEffect = null
    }
    val shotGuideNearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = config.STROKE_AIM_LINE_NEAR }
    val shotGuideFarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = config.STROKE_AIM_LINE_FAR }

    // Paints for the actual ball overlays (draggable, with sight on cue)
    val actualCueBallOverlayOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = config.STROKE_MAIN_CIRCLES }
    val actualCueBallAimingSightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = config.STROKE_AIMING_SIGHT; style = Paint.Style.STROKE }
    val actualTargetBallOverlayOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = config.STROKE_MAIN_CIRCLES }

    // Removed: Floating ghost ball paints are no longer needed here


    // Paint for indicating detected but unselected balls
    val detectedBallOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f // Can be a config constant
        color = AppPurple.toArgb() // A neutral color
    }

    // Corrected paint names for Follow and Draw paths
    val followPathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = config.STROKE_FOLLOW_DRAW_PATH
    }
    val drawPathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = config.STROKE_FOLLOW_DRAW_PATH
    }

    private fun createBaseHelperTextPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = archivoBlackTypeface; textAlign = Paint.Align.CENTER
    }
    val projectedShotTextPaint = createBaseHelperTextPaint().apply { color = AppHelpTextProjectedShotLineActual.toArgb() }
    val tangentLineTextPaint = createBaseHelperTextPaint().apply { color = AppHelpTextTangentLine.toArgb() } // Changed from AppPurple
    val cueBallPathTextPaint = createBaseHelperTextPaint().apply { color = AppHelpTextTangentLine.toArgb() }
    val pocketAimTextPaint = createBaseHelperTextPaint().apply { color = AppHelpTextPocketAim.toArgb() }
    val invalidShotWarningPaint = createBaseHelperTextPaint().apply {
        color = AppErrorRed.toArgb(); typeface = archivoBlackTypeface ?: Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(3f, 2f, 2f, AndroidColor.argb(180,0,0,0))
    }
    val ghostTargetNamePaint = createBaseHelperTextPaint().apply { color = AppHelpTextTargetBallLabel.toArgb() }
    val ghostCueNamePaint = createBaseHelperTextPaint().apply { color = AppHelpTextGhostBallLabel.toArgb() }
    val fitTargetInstructionPaint = createBaseHelperTextPaint().apply { color = AppHelpTextYellow.toArgb(); textAlign = Paint.Align.LEFT }
    val placeCueInstructionPaint = createBaseHelperTextPaint().apply { color = AppHelpTextPocketAim.toArgb() }
    val panHintPaint = createBaseHelperTextPaint().apply { color = AndroidColor.argb(
        AndroidColor.alpha(AppConfig.DEFAULT_HELP_TEXT_COLOR_ARGB),
        AndroidColor.red(AppConfig.DEFAULT_HELP_TEXT_COLOR_ARGB),
        AndroidColor.green(AppConfig.DEFAULT_HELP_TEXT_COLOR_ARGB),
        AndroidColor.blue(AppConfig.DEFAULT_HELP_TEXT_COLOR_ARGB)
    ); textAlign = Paint.Align.LEFT }
    val pinchHintPaint = createBaseHelperTextPaint().apply { color = AndroidColor.argb(
        AndroidColor.alpha(AppConfig.DEFAULT_HELP_TEXT_COLOR_ARGB),
        AndroidColor.red(AppConfig.DEFAULT_HELP_TEXT_COLOR_ARGB),
        AndroidColor.green(AppConfig.DEFAULT_HELP_TEXT_COLOR_ARGB),
        AndroidColor.blue(AppConfig.DEFAULT_HELP_TEXT_COLOR_ARGB)
    ); textAlign = Paint.Align.LEFT }

    // New instruction paint for selection modes
    val selectionInstructionPaint = createBaseHelperTextPaint().apply {
        color = AppHelpTextYellow.toArgb()
        setShadowLayer(2f, 1f, 1f, AndroidColor.argb(120, 0, 0, 0))
    }


    init {
        targetCirclePaint.color = AppYellow.toArgb()
        cueCirclePaint.color = AppWhite.toArgb() // This is for the *logical* cue ball on the plane
        centerMarkPaint.color = AppBlack.toArgb()
        protractorAngleLinePaint.color = AppMediumGray.toArgb()
        targetLineGuidePaint.color = AppYellow.toArgb()
        deflectionDottedPaint.color = AppWhite.toArgb()
        deflectionSolidPaint.color = AppWhite.toArgb()
        shotGuideNearPaint.color = AppWhite.toArgb()
        shotGuideFarPaint.color = AppPurple.toArgb()

        // Colors for the *actual* cue ball overlay and its aiming sight
        actualCueBallOverlayOutlinePaint.color = AppWhite.toArgb()
        actualCueBallAimingSightPaint.color = AppYellow.toArgb()
        // Color for the *actual* target ball overlay
        actualTargetBallOverlayOutlinePaint.color = AppYellow.toArgb()


        // Using AppConfig constants for opacity
        val followColorRGB = AppErrorRed.toArgb()
        followPathPaint.color = AndroidColor.argb(config.PATH_OPACITY_ALPHA, AndroidColor.red(followColorRGB), AndroidColor.green(followColorRGB), AndroidColor.blue(followColorRGB))
        val drawColorRGB = AppPurple.toArgb()
        drawPathPaint.color = AndroidColor.argb(config.PATH_OPACITY_ALPHA, AndroidColor.red(drawColorRGB), AndroidColor.green(drawColorRGB), AndroidColor.blue(drawColorRGB))
    }

    fun applyMaterialYouColors(colorScheme: ColorScheme) {
        M3_COLOR_PRIMARY = colorScheme.primary.toArgb(); M3_COLOR_SECONDARY = colorScheme.secondary.toArgb(); M3_COLOR_TERTIARY = colorScheme.tertiary.toArgb()
        M3_COLOR_ON_SURFACE = colorScheme.onSurface.toArgb(); M3_COLOR_OUTLINE = colorScheme.outline.toArgb(); M3_COLOR_ERROR = colorScheme.error.toArgb()
        M3_COLOR_PRIMARY_CONTAINER = colorScheme.primaryContainer.toArgb(); M3_COLOR_SECONDARY_CONTAINER = colorScheme.secondaryContainer.toArgb()
        val primaryComposeColor = colorScheme.primary
        M3_GLOW_COLOR = AndroidColor.argb(100, AndroidColor.red(primaryComposeColor.toArgb()), AndroidColor.green(primaryComposeColor.toArgb()), AndroidColor.blue(primaryComposeColor.toArgb()))
        val surfaceColor = colorScheme.surface.toArgb()
        val surfaceBrightness = (AndroidColor.red(surfaceColor) * 299 + AndroidColor.green(surfaceColor) * 587 + AndroidColor.blue(surfaceColor) * 114) / 1000
        M3_TEXT_SHADOW_COLOR = if (surfaceBrightness < 128) AndroidColor.argb(180,220,220,220) else AndroidColor.argb(180, 30,30,30)
        val tertiaryBase = M3_COLOR_TERTIARY
        protractorAngleLinePaint.color = AndroidColor.argb(170, AndroidColor.red(tertiaryBase), AndroidColor.green(tertiaryBase), AndroidColor.blue(tertiaryBase))
        centerMarkPaint.color = M3_COLOR_ON_SURFACE
        deflectionSolidPaint.setShadowLayer(config.GLOW_RADIUS_FIXED, 0f, 0f, M3_GLOW_COLOR)
        val helperShadowColor = AndroidColor.argb(120, 0,0,0)
        val allHelperTextPaints = listOf(
            projectedShotTextPaint, tangentLineTextPaint, cueBallPathTextPaint, pocketAimTextPaint,
            ghostTargetNamePaint, ghostCueNamePaint, fitTargetInstructionPaint, placeCueInstructionPaint,
            panHintPaint, pinchHintPaint, selectionInstructionPaint // Include new paint
        )
        allHelperTextPaints.forEach { paint ->
            paint.setShadowLayer(1.5f,1.5f,1.5f, helperShadowColor); paint.typeface = archivoBlackTypeface ?: paint.typeface
        }
        invalidShotWarningPaint.setShadowLayer(3f, 2f, 2f, M3_TEXT_SHADOW_COLOR)

        // Update Follow/Draw path colors if they should be themed, using config.PATH_OPACITY_ALPHA
        val themedFollowColor = colorScheme.secondary.toArgb()
        followPathPaint.color = AndroidColor.argb(config.PATH_OPACITY_ALPHA, AndroidColor.red(themedFollowColor), AndroidColor.green(themedFollowColor), AndroidColor.blue(themedFollowColor))
        val themedDrawColor = colorScheme.tertiary.toArgb()
        drawPathPaint.color = AndroidColor.argb(config.PATH_OPACITY_ALPHA, AndroidColor.red(themedDrawColor), AndroidColor.green(themedDrawColor), AndroidColor.blue(themedDrawColor))

        // Update actual cue/target ball overlay paints based on theme
        actualCueBallOverlayOutlinePaint.color = colorScheme.onBackground.toArgb() // White on yellow background
        actualCueBallAimingSightPaint.color = colorScheme.onSurface.toArgb() // Black on yellow surface
        actualTargetBallOverlayOutlinePaint.color = colorScheme.onSurface.toArgb() // Black on yellow surface

        // Removed: Floating ghost ball paints
    }
    fun resetDynamicPaintProperties() {
        targetLineGuidePaint.apply { strokeWidth = config.STROKE_TARGET_LINE_GUIDE; color = AppYellow.toArgb(); clearShadowLayer() }
        deflectionSolidPaint.clearShadowLayer()
    }
}