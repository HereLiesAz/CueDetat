package com.hereliesaz.cuedetat.view.config.line

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.SulfurDust
import com.hereliesaz.cuedetat.view.config.base.LinesConfig

/**
 * Configuration for the "Aiming Line".
 *
 * This line represents the path from the Cue Ball to the Ghost Ball (impact point).
 */
data class AimingLine(
    /** Fully opaque. */
    override val opacity: Float = 1.0f,
    /** Width of the glow effect. */
    override val glowWidth: Float = 8f,
    /** Glow color (Sulfur/Yellow). */
    override val glowColor: Color = SulfurDust,
    /** Stroke width. */
    override val strokeWidth: Float = 5f,
    /** Stroke color (Sulfur/Yellow). */
    override val strokeColor: Color = SulfurDust,
) : LinesConfig
