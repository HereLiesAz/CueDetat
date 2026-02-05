package com.hereliesaz.cuedetat.view.config.table

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.HippieGreen
import com.hereliesaz.cuedetat.view.config.base.TableComponentConfig

/**
 * Configuration for the "Pockets" (Holes) of the table.
 */
data class Holes(
    /** Label used for identification. */
    override val label: String = "Pockets",
    /** 80% opaque. */
    override val opacity: Float = 0.8f,
    /** No glow effect. */
    override val glowWidth: Float = 0f,
    /** Transparent glow color. */
    override val glowColor: Color = Color.Transparent,
    /** Stroke width. */
    override val strokeWidth: Float = 5f,
    /** Stroke color (HippieGreen). */
    override val strokeColor: Color = HippieGreen,
    /** No additional offset. */
    override val additionalOffset: Float = 0f,
    /** Filled with semi-transparent black to represent depth. */
    override val fillColor: Color = Color.Black.copy(alpha = 0.75f)
) : TableComponentConfig
