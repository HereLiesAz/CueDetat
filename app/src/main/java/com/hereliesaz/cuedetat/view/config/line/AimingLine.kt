package com.hereliesaz.cuedetat.view.config.line

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.AccentGold
import com.hereliesaz.cuedetat.view.config.base.LineDecree

data class AimingLine(
    override val label: String = "Aiming Line",
    override val opacity: Float = 1.0f,
    override val glowWidth: Float = 10f,
    override val glowColor: Color = Color.White.copy(alpha = 0.4f),
    override val strokeWidth: Float = 3f,
    override val strokeColor: Color = AccentGold,
    override val additionalOffset: Float = 0f
) : LineDecree
