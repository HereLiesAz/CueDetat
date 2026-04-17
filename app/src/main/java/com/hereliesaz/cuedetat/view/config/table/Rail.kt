package com.hereliesaz.cuedetat.view.config.table

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.HippieGreen
import com.hereliesaz.cuedetat.view.config.base.TableComponentConfig

/**
 * Configuration for the "Rails" (Cushions) of the table.
 */
data class Rail(
    /** Label used for identification. */
    override val label: String = "Rails",
    /** Fully opaque. */
    override val opacity: Float = 1.0f,
    /** Glow width. */
    override val glowWidth: Float = 10f,
    /** Glow color (HippieGreen). */
    override val glowColor: Color = HippieGreen,
    /** Stroke width. */
    override val strokeWidth: Float = 3f,
    /** Stroke color (HippieGreen). */
    override val strokeColor: Color = HippieGreen,
    /** No additional offset (lift is handled by rendering logic). */
    override val additionalOffset: Float = 0f,
    /** No fill color. */
    override val fillColor: Color = Color.Transparent
) : TableComponentConfig
