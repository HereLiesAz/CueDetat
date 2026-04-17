package com.hereliesaz.cuedetat.view.config.line

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.BankLine1Yellow
import com.hereliesaz.cuedetat.view.config.base.LinesConfig

/**
 * Configuration for the first segment of a Bank Shot path.
 */
data class BankLine1(
    /** Label used for identification. */
    override val label: String = "Bank 1",
    /** Fully opaque. */
    override val opacity: Float = 1.0f,
    /** Width of the glow effect. */
    override val glowWidth: Float = 10f,
    /** Glow color (BankLine1Yellow with 40% alpha). */
    override val glowColor: Color = BankLine1Yellow.copy(alpha = 0.4f),
    /** Stroke width. */
    override val strokeWidth: Float = 2f,
    /** Stroke color (BankLine1Yellow). */
    override val strokeColor: Color = BankLine1Yellow,
    /** No additional offset. */
    override val additionalOffset: Float = 0f
) : LinesConfig
