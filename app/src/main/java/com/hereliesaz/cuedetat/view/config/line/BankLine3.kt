package com.hereliesaz.cuedetat.view.config.line

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.BankLine3Yellow
import com.hereliesaz.cuedetat.view.config.base.LinesConfig

/**
 * Configuration for the third segment of a Bank Shot path.
 */
data class BankLine3(
    /** Label used for identification. */
    override val label: String = "Bank 3",
    /** Fully opaque. */
    override val opacity: Float = 1.0f,
    /** Width of the glow effect. */
    override val glowWidth: Float = 10f,
    /** Glow color (BankLine3Yellow with 40% alpha). */
    override val glowColor: Color = BankLine3Yellow.copy(alpha = 0.4f),
    /** Stroke width. */
    override val strokeWidth: Float = 2f,
    /** Stroke color (BankLine3Yellow). */
    override val strokeColor: Color = BankLine3Yellow,
    /** No additional offset. */
    override val additionalOffset: Float = 0f
) : LinesConfig
