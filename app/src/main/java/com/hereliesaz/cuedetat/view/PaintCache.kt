// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/PaintCache.kt

package com.hereliesaz.cuedetat.view

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.ui.theme.MutedGray
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import com.hereliesaz.cuedetat.view.config.ball.ActualCueBall
import com.hereliesaz.cuedetat.view.config.ball.GhostCueBall
import com.hereliesaz.cuedetat.view.config.ball.TargetBall
import com.hereliesaz.cuedetat.view.config.line.AimingLine
import com.hereliesaz.cuedetat.view.config.line.BankLine1
import com.hereliesaz.cuedetat.view.config.line.BankLine2
import com.hereliesaz.cuedetat.view.config.line.BankLine3
import com.hereliesaz.cuedetat.view.config.line.BankLine4
import com.hereliesaz.cuedetat.view.config.line.ShotGuideLine
import com.hereliesaz.cuedetat.view.config.line.TangentLine
import com.hereliesaz.cuedetat.view.config.table.Holes
import com.hereliesaz.cuedetat.view.config.table.Rail

/**
 * A centralized cache for Android [Paint] objects used in custom view rendering.
 *
 * Creating new Paint objects in the `onDraw` loop is a major performance anti-pattern
 * because it triggers frequent garbage collection. This class creates them once
 * and reuses them, updating their colors/properties only when necessary (e.g., theme changes).
 */
class PaintCache {

    // --- Primary Paint Objects ---

    /** Paint for the table boundaries (rails). Anti-aliased stroke. */
    val tableOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    /** Paint for the target ball outline. Anti-aliased stroke. */
    val targetCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    /** Paint for the ghost cue ball outline. Anti-aliased stroke. */
    val cueCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    /** Paint for the actual cue ball outline. Anti-aliased stroke. */
    val actualCueBallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    /** Generic fill paint (white by default). Used for ball centers, etc. */
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    /** Paint for the aiming line (cue -> ghost). Anti-aliased stroke. */
    val shotLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    /** Paint for the solid portion of the tangent line. Anti-aliased stroke. */
    val tangentLineSolidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    /** Paint for the dotted portion of the tangent line. Uses a DashPathEffect. */
    val tangentLineDottedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        // Dash pattern: 15px line, 10px gap.
        pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
    }

    /** Paint for drawing text labels. Centered alignment. */
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    /** Paint for warning boxes (red). Anti-aliased stroke. */
    val warningPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    /** Paint for angle guide arcs. Thinner stroke. */
    val angleGuidePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }

    /** Paint for filling the table pockets. */
    val pocketFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    /** Paint for the grid background (if enabled). Dashed lines. */
    val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 5f;
        // Dash pattern: 15px line, 15px gap.
        pathEffect = DashPathEffect(floatArrayOf(15f, 15f), 0f)
    }

    /** Paint to visualize obstructed paths. Low alpha gray. */
    val pathObstructionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = MutedGray.copy(alpha = 0.2f).toArgb()
    }

    /** Paint for drawing Computer Vision result markers (dots). Fill. */
    val cvResultPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    /** Paint for drawing Bitmaps (CV debug overlay). Filter enabled for smooth scaling. */
    val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /** Paint for masking operations. Uses DST_IN mode to apply transparency masks. */
    val gradientMaskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    /** Cache for generated glow paints (dynamic based on color/width). */
    val glowPaints = mutableMapOf<String, Paint>()

    // --- Bank Line Paints ---
    // Paints for each segment of a bank shot to allow distinct styling if needed.
    val bankLine1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val bankLine2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val bankLine3Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val bankLine4Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    /** Reusable paint object for creating glow effects dynamically. */
    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * Configures and returns the reused [glowPaint] object for the current frame.
     *
     * Handles the "Glow Stick" feature logic:
     * - If [CueDetatState.glowStickValue] is near 0, applies the standard [baseGlowColor].
     * - If [CueDetatState.glowStickValue] is non-zero, overrides with White/Black and custom blur.
     *
     * @param baseGlowColor The default color defined in the config.
     * @param baseGlowWidth The default width defined in the config.
     * @param state The current application state.
     * @return The configured Paint object.
     */
    fun getGlowPaint(baseGlowColor: Color, baseGlowWidth: Float, state: CueDetatState): Paint {
        glowPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = baseGlowWidth

            val glowValue = state.glowStickValue
            // Check threshold for Glow Stick override.
            if (kotlin.math.abs(glowValue) > 0.05f) {
                // Glow Stick override is active (positive = white, negative = black).
                val glowAlpha = (kotlin.math.abs(glowValue) * 255).toInt()
                val color = if (glowValue > 0) Color.White.toArgb() else Color.Black.toArgb()
                // Blur radius scales with intensity.
                val blurRadius = 15f * kotlin.math.abs(glowValue)
                this.color = color
                this.alpha = glowAlpha
                maskFilter = android.graphics.BlurMaskFilter(blurRadius, android.graphics.BlurMaskFilter.Blur.NORMAL)
            } else {
                // Default behavior: Use the config color.
                this.color = baseGlowColor.toArgb()
                // Default glow is 70% of the base color's alpha.
                alpha = (baseGlowColor.alpha * 255 * 0.7f).toInt()
                // Fixed blur radius for standard glow.
                maskFilter = android.graphics.BlurMaskFilter(8f, android.graphics.BlurMaskFilter.Blur.NORMAL)
            }
        }
        return glowPaint
    }

    /**
     * Updates the typeface used by the text paint.
     * Called when the custom font is loaded.
     */
    fun setTypeface(typeface: Typeface?) {
        textPaint.typeface = typeface
    }

    /**
     * Refreshes all Paint colors based on the current theme configuration.
     *
     * This should be called whenever the theme changes or at initialization.
     * It reads values from the [view.config] classes and applies them to the Paints.
     *
     * @param uiState Current state (unused here but kept for API consistency).
     * @param isDark Boolean indicating if dark mode is active (can be used for logic branches).
     */
    fun updateColors(uiState: CueDetatState, isDark: Boolean) {
        // Instantiate configuration objects to access their color properties.
        val railConfig = Rail()
        val targetBallConfig = TargetBall()
        val ghostCueBallConfig = GhostCueBall()
        val actualCueBallConfig = ActualCueBall()
        val shotGuideLineConfig = ShotGuideLine()
        // AimingLine() instantiated but unused? (Logic check: seems like a leftover, safe to ignore for doc)
        val tangentLineConfig = TangentLine()
        val bankLine1Config = BankLine1()
        val bankLine2Config = BankLine2()
        val bankLine3Config = BankLine3()
        val bankLine4Config = BankLine4()
        val holesConfig = Holes()

        // Apply config colors and widths to their respective Paint objects.

        // Table Rails
        tableOutlinePaint.color = railConfig.strokeColor.toArgb()
        tableOutlinePaint.strokeWidth = railConfig.strokeWidth

        // Target Ball
        targetCirclePaint.color = targetBallConfig.strokeColor.toArgb()
        targetCirclePaint.strokeWidth = targetBallConfig.strokeWidth

        // Ghost Cue Ball
        cueCirclePaint.color = ghostCueBallConfig.strokeColor.toArgb()
        cueCirclePaint.strokeWidth = ghostCueBallConfig.strokeWidth

        // Actual Cue Ball
        actualCueBallPaint.color = actualCueBallConfig.strokeColor.toArgb()
        actualCueBallPaint.strokeWidth = actualCueBallConfig.strokeWidth

        // Shot Guide Line
        shotLinePaint.color = shotGuideLineConfig.strokeColor.toArgb()
        shotLinePaint.strokeWidth = shotGuideLineConfig.strokeWidth

        // Tangent Line (Solid part)
        tangentLineSolidPaint.color = tangentLineConfig.strokeColor.toArgb()
        tangentLineSolidPaint.strokeWidth = tangentLineConfig.strokeWidth

        // Tangent Line (Dotted part)
        tangentLineDottedPaint.color = tangentLineConfig.strokeColor.toArgb()
        tangentLineDottedPaint.strokeWidth = tangentLineConfig.strokeWidth
        tangentLineDottedPaint.alpha = (tangentLineConfig.opacity * 255).toInt()

        // Bank Shot Segments
        bankLine1Paint.color = bankLine1Config.strokeColor.toArgb()
        bankLine1Paint.strokeWidth = bankLine1Config.strokeWidth
        bankLine2Paint.color = bankLine2Config.strokeColor.toArgb()
        bankLine2Paint.strokeWidth = bankLine2Config.strokeWidth
        bankLine3Paint.color = bankLine3Config.strokeColor.toArgb()
        bankLine3Paint.strokeWidth = bankLine3Config.strokeWidth
        bankLine4Paint.color = bankLine4Config.strokeColor.toArgb()
        bankLine4Paint.strokeWidth = bankLine4Config.strokeWidth

        // Pockets
        pocketFillPaint.color = holesConfig.fillColor.toArgb()

        // Universal/System Paints
        warningPaint.color = WarningRed.toArgb()
        warningPaint.strokeWidth = 6f // Keep warning stroke consistent and thick.
        textPaint.color = Color.White.toArgb() // Debug text is always white for contrast.
        fillPaint.color = Color.White.toArgb() // Default fill is white.
    }
}
