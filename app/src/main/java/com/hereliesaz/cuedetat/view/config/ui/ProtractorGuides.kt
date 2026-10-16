package com.hereliesaz.cuedetat.view.config.ui

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.MutedGray
import com.hereliesaz.cuedetat.view.config.base.LinesConfig

/**
 * Configuration for the "Protractor Guides" visualization (the arcs showing angles).
 */
data class ProtractorGuides(
    /** Low opacity (40%). */
    override val opacity: Float = 0.4f,
    /** No glow effect. */
    override val glowWidth: Float = 0f,
    /** Transparent glow color. */
    override val glowColor: Color = Color.Transparent,
    /** Stroke width. */
    override val strokeWidth: Float = 1.5f,
    /** Stroke color (MutedGray). */
    override val strokeColor: Color = MutedGray,
) : LinesConfig
