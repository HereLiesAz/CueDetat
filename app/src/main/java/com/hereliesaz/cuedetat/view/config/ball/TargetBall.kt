package com.hereliesaz.cuedetat.view.config.ball

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.AccentGold
import com.hereliesaz.cuedetat.ui.theme.RebelYellow
import com.hereliesaz.cuedetat.view.config.base.BallsConfig
import com.hereliesaz.cuedetat.view.config.base.CenterShape

data class TargetBall(
    override val label: String = "T",
    override val opacity: Float = 1.0f,
    override val glowWidth: Float = 12f,
    override val glowColor: Color = Color.White.copy(alpha = 0.5f),
    override val strokeWidth: Float = 5f,
    override val strokeColor: Color = AccentGold,
    override val additionalOffset: Float = 0f,
    override val centerShape: CenterShape = CenterShape.DOT, // Changed from CROSSHAIR
    override val centerSize: Float = 0.2f, // as a factor of radius
    override val centerColor: Color = Color.White,
    override val fillColor: Color = Color.Transparent,
    override val additionalOffset3d: Float = 0f
) : BallsConfig