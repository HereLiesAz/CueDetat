package com.hereliesaz.cuedetat.view.config.table

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.IcedOpal
import com.hereliesaz.cuedetat.view.config.base.TableComponentConfig

data class Diamonds(
    override val label: String = "Diamonds",
    override val opacity: Float = 0.9f,
    override val glowWidth: Float = 0f,
    override val glowColor: Color = Color.Transparent,
    override val strokeWidth: Float = 1.5f,
    override val strokeColor: Color = IcedOpal.copy(alpha = 0.7f),
    override val additionalOffset: Float = 0f,
    override val fillColor: Color = IcedOpal.copy(alpha = 0.7f)
) : TableComponentConfig
