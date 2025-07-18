// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/config/ball/GhostCueBall.kt

package com.hereliesaz.cuedetat.view.config.ball

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.MutedGray
import com.hereliesaz.cuedetat.ui.theme.OracleGlow
import com.hereliesaz.cuedetat.view.config.base.BallsConfig
import com.hereliesaz.cuedetat.view.config.base.CenterShape

internal class GhostCueBall : BallsConfig {
    override val label: String = "Ghost Ball"
    override val strokeColor: Color = Color.White
    override val strokeWidth: Float = 1f
    override val glowColor: Color = MutedGray
    override val glowWidth: Float = 5f
    override val centerColor: Color = MutedGray
    override val centerSize: Float = 0.8f
    override val centerShape: CenterShape = CenterShape.CROSSHAIR
    override val opacity: Float = 0.8f
    override val fillColor: Color = Color.Transparent
    override val additionalOffset: Float = 0f
    override val additionalOffset3d: Float = 0f
}