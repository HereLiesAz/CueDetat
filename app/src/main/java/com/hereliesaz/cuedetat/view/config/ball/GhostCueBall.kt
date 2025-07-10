package com.hereliesaz.cuedetat.view.config.ball

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.MutedGray
import com.hereliesaz.cuedetat.ui.theme.RebelYellow
import com.hereliesaz.cuedetat.view.config.base.BallsConfig
import com.hereliesaz.cuedetat.view.config.base.CenterShape

data class GhostCueBall(
    override val label: String = "G",
    override val opacity: Float = 1.0f,
    override val glowWidth: Float = 12f,
    override val glowColor: Color = Color.White.copy(alpha = 0.5f),
    override val strokeWidth: Float = 5f,
    override val strokeColor: Color = MutedGray,
    override val additionalOffset: Float = 0f,
    override val centerShape: CenterShape = CenterShape.CROSSHAIR,
    override val centerSize: Float = 0.5f, // Made larger
    override val centerColor: Color = RebelYellow,
    override val fillColor: Color = Color.Transparent,
    override val additionalOffset3d: Float = 0f
) : BallsConfig