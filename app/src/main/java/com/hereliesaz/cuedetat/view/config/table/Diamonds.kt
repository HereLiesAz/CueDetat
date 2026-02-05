package com.hereliesaz.cuedetat.view.config.table

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.IcedOpal
import com.hereliesaz.cuedetat.view.config.base.TableComponentConfig

/**
 * Configuration for the "Diamond" markers on the table rails.
 */
data class Diamonds(
    /** Label used for identification. */
    override val label: String = "Diamonds",
    /** 70% opaque. */
    override val opacity: Float = 0.7f,
    /** No glow effect. */
    override val glowWidth: Float = 0f,
    /** Transparent glow color. */
    override val glowColor: Color = IcedOpal,
    /** Stroke width. */
    override val strokeWidth: Float = 3f,
    /** Stroke color (IcedOpal). */
    override val strokeColor: Color = IcedOpal,
    /** No additional offset. */
    override val additionalOffset: Float = 0f,
    /** Filled with IcedOpal color. */
    override val fillColor: Color = IcedOpal
) : TableComponentConfig
