// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/config/ball/BankingBall.kt

package com.hereliesaz.cuedetat.view.config.ball

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.OracleBlue
import com.hereliesaz.cuedetat.ui.theme.OracleGlow
import com.hereliesaz.cuedetat.view.config.base.BallsConfig
import com.hereliesaz.cuedetat.view.config.base.CenterShape

class BankingBall : BallsConfig {
    override val label: String = "Cue Ball"
    override val strokeColor: Color = OracleBlue
    override val strokeWidth: Float = 3f
    override val glowColor: Color = OracleGlow
    override val glowWidth: Float = 8f
    override val centerColor: Color = Color.White.copy(alpha = 0.8f)
    override val centerSize: Float = 0.2f
    override val centerShape: CenterShape = CenterShape.DOT
    override val opacity: Float = 1f
    override val fillColor: Color = Color.Transparent
    override val additionalOffset: Float = 0f
    override val additionalOffset3d: Float = 0f
}