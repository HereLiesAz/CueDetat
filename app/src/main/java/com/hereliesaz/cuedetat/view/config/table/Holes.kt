package com.hereliesaz.cuedetat.view.config.table

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.view.config.base.TableComponentDecree

data class Holes(
    override val label: String = "Pockets",
    override val opacity: Float = 1.0f,
    override val glowWidth: Float = 0f,
    override val glowColor: Color = Color.Transparent,
    override val strokeWidth: Float = 5f,
    override val strokeColor: Color = Color.Black,
    override val additionalOffset: Float = 0f,
    override val fillColor: Color = Color.Black
) : TableComponentDecree
