package com.hereliesaz.cuedetat.view.config.line

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.SulfurDust
import com.hereliesaz.cuedetat.view.config.base.LinesConfig

data class AimingLine(
    override val label: String = "Aiming Line",
    override val opacity: Float = 1.0f,
    override val glowWidth: Float = 8f,
    override val glowColor: Color = SulfurDust,
    override val strokeWidth: Float = 5f,
    override val strokeColor: Color = SulfurDust,
    override val additionalOffset: Float = 0f
) : LinesConfig