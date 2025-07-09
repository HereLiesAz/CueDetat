package com.hereliesaz.cuedetat.view.config.table

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.AcidPatina
import com.hereliesaz.cuedetat.view.config.base.TableComponentDecree

data class Table(
    override val label: String = "Table Surface",
    override val opacity: Float = 1.0f,
    override val glowWidth: Float = 12f,
    override val glowColor: Color = Color.White.copy(alpha = 0.2f),
    override val strokeWidth: Float = 5f,
    override val strokeColor: Color = AcidPatina,
    override val additionalOffset: Float = 0f,
    override val fillColor: Color = Color.Transparent
) : TableComponentDecree
