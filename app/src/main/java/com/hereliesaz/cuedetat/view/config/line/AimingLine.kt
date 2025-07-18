// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/config/line/AimingLine.kt

package com.hereliesaz.cuedetat.view.config.line

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.AccentGold
import com.hereliesaz.cuedetat.ui.theme.ObstructionPathBlue
import com.hereliesaz.cuedetat.ui.theme.OracleGlow
import com.hereliesaz.cuedetat.view.config.base.LinesConfig

class AimingLine: LinesConfig {
    override val strokeColor = AccentGold
    override val strokeWidth = 2f
    override val glowColor = OracleGlow
    override val glowWidth = 8f
    override val opacity = 1f
    override val label = "Aiming Line"
    override val additionalOffset = 0f
    // CORRECTED: Added specific properties for the obstacle path
    override val obstaclePathColor: Color = ObstructionPathBlue
    override val obstaclePathOpacity: Float = 0.3f
}