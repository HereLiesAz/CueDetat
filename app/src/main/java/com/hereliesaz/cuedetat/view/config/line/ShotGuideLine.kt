package com.hereliesaz.cuedetat.view.config.line

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import com.hereliesaz.cuedetat.ui.theme.MutedGray
import com.hereliesaz.cuedetat.ui.theme.RustedEmber
import com.hereliesaz.cuedetat.view.config.base.LinesConfig

data class ShotGuideLine(
    override val label: String = "Shot Guide Line",
    override val opacity: Float = 1.0f,
    override val glowWidth: Float = 10f,
    override val glowColor: Color = Color.White.copy(alpha = 0.4f),
    override val strokeWidth: Float = 8f,
    override val strokeColor: Color = White,
    override val additionalOffset: Float = 0f
) : LinesConfig