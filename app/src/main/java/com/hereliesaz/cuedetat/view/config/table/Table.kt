package com.hereliesaz.cuedetat.view.config.table

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.HippieGreen
import com.hereliesaz.cuedetat.view.config.base.TableComponentConfig

/**
 * Configuration for the "Table Surface" (the playing field).
 */
data class Table(
    /** Label used for identification. */
    override val label: String = "Table Surface",
    /** Fully opaque. */
    override val opacity: Float = 1.0f,
    /** Glow width. */
    override val glowWidth: Float = 12f,
    /** Glow color (HippieGreen). */
    override val glowColor: Color = HippieGreen,
    /** Stroke width. */
    override val strokeWidth: Float = 5f,
    /** Stroke color (HippieGreen). */
    override val strokeColor: Color = HippieGreen,
    /** No additional offset. */
    override val additionalOffset: Float = 0f,
    /** No fill color. */
    override val fillColor: Color = Color.Transparent
) : TableComponentConfig
