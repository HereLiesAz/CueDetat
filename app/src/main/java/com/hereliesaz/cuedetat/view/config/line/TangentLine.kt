package com.hereliesaz.cuedetat.view.config.line

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.IcedOpal
import com.hereliesaz.cuedetat.view.config.base.LinesConfig

/**
 * Configuration for the "Tangent Line".
 *
 * This line represents the 90-degree angle from the impact line, showing the theoretical path of the cue ball with no spin.
 */
data class TangentLine(
    /** Label used for identification. */
    override val label: String = "Tangent Line",
    /** 80% opaque. */
    override val opacity: Float = 0.8f,
    /** Width of the glow effect. */
    override val glowWidth: Float = 8f,
    /** Glow color (IcedOpal with 30% alpha). */
    override val glowColor: Color = IcedOpal.copy(alpha = 0.3f),
    /** Stroke width. */
    override val strokeWidth: Float = 5f,
    /** Stroke color (IcedOpal with 30% alpha). */
    override val strokeColor: Color = IcedOpal.copy(alpha = 0.3f),
    /** No additional offset. */
    override val additionalOffset: Float = 0f
) : LinesConfig
