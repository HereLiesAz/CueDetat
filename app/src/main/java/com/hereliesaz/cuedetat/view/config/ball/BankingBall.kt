package com.hereliesaz.cuedetat.view.config.ball

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.RebelYellow
import com.hereliesaz.cuedetat.view.config.base.BallsConfig
import com.hereliesaz.cuedetat.view.config.base.CenterShape

data class BankingBall(
    override val label: String = "B",
    override val opacity: Float = 1.0f,
    override val glowWidth: Float = 12f,
    override val glowColor: Color = Color.White.copy(alpha = 0.5f),
    override val strokeWidth: Float = 3f,
    override val strokeColor: Color = RebelYellow,
    override val additionalOffset: Float = 0f,
    override val centerShape: CenterShape = CenterShape.DOT,
    override val centerSize: Float = 0.2f,
    override val centerColor: Color = Color.White,
    override val fillColor: Color = Color.Transparent,
    override val additionalOffset3d: Float = 0f
) : BallsConfig
