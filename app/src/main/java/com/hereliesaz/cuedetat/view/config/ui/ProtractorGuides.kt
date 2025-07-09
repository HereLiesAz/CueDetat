package com.hereliesaz.cuedetat.view.config.ui

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.MutedGray
import com.hereliesaz.cuedetat.view.config.base.LinesConfig

data class ProtractorGuides(
    override val label: String = "Angle Guide",
    override val opacity: Float = 0.4f,
    override val glowWidth: Float = 0f,
    override val glowColor: Color = Color.Transparent,
    override val strokeWidth: Float = 1.5f,
    override val strokeColor: Color = MutedGray,
    override val additionalOffset: Float = 0f
) : LinesConfig
