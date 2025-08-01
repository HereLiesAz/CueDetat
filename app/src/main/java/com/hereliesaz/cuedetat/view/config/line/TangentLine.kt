package com.hereliesaz.cuedetat.view.config.line

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.IcedOpal
import com.hereliesaz.cuedetat.view.config.base.LinesConfig

data class TangentLine(
    override val label: String = "Tangent Line",
    override val opacity: Float = 0.8f,
    override val glowWidth: Float = 8f,
    override val glowColor: Color = IcedOpal.copy(alpha = 0.3f),
    override val strokeWidth: Float = 5f,
    override val strokeColor: Color = IcedOpal.copy(alpha = 0.3f),
    override val additionalOffset: Float = 0f
) : LinesConfig
