// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/config/ball/ObstacleBall.kt

package com.hereliesaz.cuedetat.view.config.ball

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.MutedMaroon
import com.hereliesaz.cuedetat.ui.theme.OracleGlow
import com.hereliesaz.cuedetat.view.config.base.BallsConfig
import com.hereliesaz.cuedetat.view.config.base.CenterShape

class ObstacleBall : BallsConfig {
    override val label: String = ""
    override val strokeColor: Color = MutedMaroon
    override val strokeWidth: Float = 2f
    override val glowColor: Color = OracleGlow
    override val glowWidth: Float = 6f
    override val centerColor: Color = MutedMaroon
    override val centerSize: Float = 0.2f
    override val centerShape: CenterShape = CenterShape.DOT
    override val opacity: Float = 0.9f
    override val fillColor: Color = Color.Transparent
    override val additionalOffset: Float = 0f
    override val additionalOffset3d: Float = 0f
}