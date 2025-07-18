// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/config/line/ShotGuideLine.kt

package com.hereliesaz.cuedetat.view.config.line

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.*
import com.hereliesaz.cuedetat.view.config.base.LinesConfig

class ShotGuideLine : LinesConfig {
    override val strokeColor = ObstructionPathBlue
    override val strokeWidth = 2f
    override val glowColor = OracleGlow
    override val glowWidth = 6f
    override val opacity = 1f
    override val label: String = "Shot Guide"
    override val additionalOffset: Float = 0f
    // CORRECTED: Added specific properties for the obstacle path
    override val obstaclePathColor: Color = ObstructionPathBlue
    override val obstaclePathOpacity: Float = 0.3f
}