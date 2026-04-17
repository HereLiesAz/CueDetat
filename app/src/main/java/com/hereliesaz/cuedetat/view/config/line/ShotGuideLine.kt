package com.hereliesaz.cuedetat.view.config.line

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.Mariner
import com.hereliesaz.cuedetat.view.config.base.LinesConfig

/**
 * Configuration for the "Shot Guide Line".
 *
 * This line represents the path of the Cue Ball *after* impact (the tangent line).
 */
data class ShotGuideLine(
    /** Label used for identification. */
    override val label: String = "Shot Guide Line",
    /** Fully opaque. */
    override val opacity: Float = 1.0f,
    /** Width of the glow effect. */
    override val glowWidth: Float = 10f,
    /** Glow color (Mariner Blue). */
    override val glowColor: Color = Mariner,
    /** Stroke width. */
    override val strokeWidth: Float = 5f,
    /** Stroke color (Mariner Blue). */
    override val strokeColor: Color = Mariner,
    /** No additional offset. */
    override val additionalOffset: Float = 0f
) : LinesConfig
