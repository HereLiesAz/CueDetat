// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/config/ball/TargetBall.kt

package com.hereliesaz.cuedetat.view.config.ball

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.AccentGold
import com.hereliesaz.cuedetat.ui.theme.OracleGlow
import com.hereliesaz.cuedetat.view.config.base.BallsConfig
import com.hereliesaz.cuedetat.view.config.base.CenterShape

internal class TargetBall : BallsConfig {
    override val label: String = "Target Ball"
    override val strokeColor: Color = AccentGold
    override val strokeWidth: Float = 2f
    override val glowColor: Color = AccentGold
    override val glowWidth: Float = 8f
    override val centerColor: Color = AccentGold
    override val centerSize: Float = 0.8f
    override val centerShape: CenterShape = CenterShape.NONE
    override val opacity: Float = 1f
    override val fillColor: Color = Color.Transparent
    override val additionalOffset: Float = 0f
    override val additionalOffset3d: Float = 0f
}