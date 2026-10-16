// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/PaintCache.kt

package com.hereliesaz.cuedetat.view

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.ui.theme.MutedGray
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import com.hereliesaz.cuedetat.view.config.ball.ActualCueBall
import com.hereliesaz.cuedetat.view.config.ball.GhostCueBall
import com.hereliesaz.cuedetat.view.config.ball.TargetBall
import com.hereliesaz.cuedetat.view.config.line.BankLine1
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

    /** Paint for marking ball positions on the top-down snapshot overlay. */
    val ballOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    /**
     * Cache for generated glow paints (dynamic based on color/width). LRU-bounded: the
     * glow-stick key embeds a continuous slider value, so an unbounded map would grow one
     * entry per slider position over a session. Capped to the most-recently-used entries.
     */
    val glowPaints = object : LinkedHashMap<com.hereliesaz.cuedetat.view.renderer.util.GlowKey, Paint>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<com.hereliesaz.cuedetat.view.renderer.util.GlowKey, Paint>): Boolean = size > GLOW_CACHE_MAX
    }

    /** Reusable lookup probe for [glowPaints] (render thread only). */
    val glowKeyProbe = com.hereliesaz.cuedetat.view.renderer.util.GlowKey()

    /** Unit-circle halo gradients keyed by colour/alpha/stops; see drawGlowCircle. LRU-bounded. */
    val haloShaders = object : LinkedHashMap<Long, android.graphics.RadialGradient>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<Long, android.graphics.RadialGradient>): Boolean = size > GLOW_CACHE_MAX
    }

    /** Reused local matrix for halo shaders. */
    val haloMatrix = android.graphics.Matrix()

    // --- Bank Line Paint ---
    /**
     * Base paint for every bank-shot segment. LineRenderer copies it per segment and
     * overrides colour/width from the BankLine1..4 configs, so only one cached paint exists.
     */
    val bankLine1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    /**
     * Reusable paint for the hardware-accelerated RadialGradient glow halo
     * (see [com.hereliesaz.cuedetat.view.renderer.util.drawGlowCircle]). A cached
     * shader from [haloShaders] is assigned per draw.
     */
    val glowHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    /**
     * Updates the typeface used by the text paint.
     * Called when the custom font is loaded.
     */
    fun setTypeface(typeface: Typeface?) {
        textPaint.typeface = typeface
    }

    /**
     * Refreshes all Paint colors from the [com.hereliesaz.cuedetat.view.config] classes,
     * shifted by the user's global luminance adjustment.
     *
     * [luminanceAdjustment] is the same -0.4..0.4 HSL-lightness offset the Compose theme
     * applies (ui/theme/Theme.kt `adjustLuminance`); it is applied here to every
     * config-derived overlay colour so the dimmer dialog reaches the canvas too. Alpha is
     * preserved. System dark mode is deliberately not an input: the overlay is drawn over
     * the camera feed / felt, not over a theme surface, so it has no light variant.
     *
     * @param luminanceAdjustment HSL lightness offset; 0 leaves config colours unchanged.
     */
    fun updateColors(luminanceAdjustment: Float) {
        val lum = luminanceAdjustment
        fun Color.adj(): Int = adjustLightness(this.toArgb(), lum)
        // Instantiate configuration objects to access their color properties.
        val railConfig = Rail()
        val targetBallConfig = TargetBall()
        val ghostCueBallConfig = GhostCueBall()
        val actualCueBallConfig = ActualCueBall()
        val shotGuideLineConfig = ShotGuideLine()
        val tangentLineConfig = TangentLine()
        val bankLine1Config = BankLine1()
        val holesConfig = Holes()

        // Apply config colors and widths to their respective Paint objects.

        // Table Rails
        tableOutlinePaint.color = railConfig.strokeColor.adj()
        tableOutlinePaint.strokeWidth = railConfig.strokeWidth

        // Target Ball
        targetCirclePaint.color = targetBallConfig.strokeColor.adj()
        targetCirclePaint.strokeWidth = targetBallConfig.strokeWidth

        // Ghost Cue Ball
        cueCirclePaint.color = ghostCueBallConfig.strokeColor.adj()
        cueCirclePaint.strokeWidth = ghostCueBallConfig.strokeWidth

        // Actual Cue Ball
        actualCueBallPaint.color = actualCueBallConfig.strokeColor.adj()
        actualCueBallPaint.strokeWidth = actualCueBallConfig.strokeWidth

        // Shot Guide Line
        shotLinePaint.color = shotGuideLineConfig.strokeColor.adj()
        shotLinePaint.strokeWidth = shotGuideLineConfig.strokeWidth

        // Tangent Line (Solid part)
        tangentLineSolidPaint.color = tangentLineConfig.strokeColor.adj()
        tangentLineSolidPaint.strokeWidth = tangentLineConfig.strokeWidth

        // Tangent Line (Dotted part)
        tangentLineDottedPaint.color = tangentLineConfig.strokeColor.adj()
        tangentLineDottedPaint.strokeWidth = tangentLineConfig.strokeWidth
        tangentLineDottedPaint.alpha = (tangentLineConfig.opacity * 255).toInt()

        // Bank Shot Segments (base paint; per-segment overrides happen in LineRenderer)
        bankLine1Paint.color = bankLine1Config.strokeColor.adj()
        bankLine1Paint.strokeWidth = bankLine1Config.strokeWidth

        // Pockets
        pocketFillPaint.color = holesConfig.fillColor.adj()

        // Top-down snapshot ball markers
        ballOverlayPaint.color = Color.White.toArgb()
        ballOverlayPaint.strokeWidth = 3f

        // Universal/System Paints
        warningPaint.color = WarningRed.toArgb()
        warningPaint.strokeWidth = 6f // Keep warning stroke consistent and thick.
        textPaint.color = Color.White.toArgb() // Debug text is always white for contrast.
        fillPaint.color = Color.White.toArgb() // Default fill is white.
    }

    private companion object {
        const val GLOW_CACHE_MAX = 32

        /** Shifts HSL lightness by [factor] (clamped 0..1), preserving alpha. */
        fun adjustLightness(argb: Int, factor: Float): Int {
            if (factor == 0f) return argb
            val hsl = FloatArray(3)
            androidx.core.graphics.ColorUtils.colorToHSL(argb, hsl)
            hsl[2] = (hsl[2] + factor).coerceIn(0f, 1f)
            val rgb = androidx.core.graphics.ColorUtils.HSLToColor(hsl)
            return (argb and 0xFF000000.toInt()) or (rgb and 0x00FFFFFF)
        }
    }
}
