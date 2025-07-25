package com.hereliesaz.cuedetat.view.config.ball

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.SulfurDust
import com.hereliesaz.cuedetat.view.config.base.BallsConfig
import com.hereliesaz.cuedetat.view.config.base.CenterShape

data class TargetBall(
    override val label: String = "Target Ball",
    override val opacity: Float = 1.0f,
    override val glowWidth: Float = 8f,
    override val glowColor: Color = SulfurDust,
    override val strokeWidth: Float = 6f,
    override val strokeColor: Color = SulfurDust,
    override val additionalOffset: Float = 0f,
    override val centerShape: CenterShape = CenterShape.NONE, // Changed from CROSSHAIR
    override val centerSize: Float = 0.2f, // as a factor of radius
    override val centerColor: Color = Color.Transparent,
    override val fillColor: Color = Color.Transparent,
    override val additionalOffset3d: Float = 0f,
) : BallsConfig