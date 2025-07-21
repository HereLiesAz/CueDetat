package com.hereliesaz.cuedetat.view.config.line

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.Mariner
import com.hereliesaz.cuedetat.view.config.base.LinesConfig

data class ShotGuideLine(
    override val label: String = "Shot Guide Line",
    override val opacity: Float = 1.0f,
    override val glowWidth: Float = 10f,
    override val glowColor: Color = Mariner,
    override val strokeWidth: Float = 5f,
    override val strokeColor: Color = Mariner,
    override val additionalOffset: Float = 0f
) : LinesConfig