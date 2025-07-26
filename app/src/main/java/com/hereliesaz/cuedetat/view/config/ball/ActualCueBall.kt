package com.hereliesaz.cuedetat.view.config.ball

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.Mariner
import com.hereliesaz.cuedetat.view.config.base.BallsConfig
import com.hereliesaz.cuedetat.view.config.base.CenterShape

data class ActualCueBall(
    override val label: String = "Actual Cue Ball",
    override val opacity: Float = 1.0f,
    override val glowWidth: Float = 12f,
    override val glowColor: Color = Mariner,
    override val strokeWidth: Float = 6f,
    override val strokeColor: Color = Mariner,
    override val additionalOffset: Float = 0f,
    override val centerShape: CenterShape = CenterShape.DOT,
    override val centerSize: Float = 0.2f, // as a factor of radius
    override val centerColor: Color = Color.White,
    override val fillColor: Color = Color.Transparent,
    override val additionalOffset3d: Float = 0f,
) : BallsConfig