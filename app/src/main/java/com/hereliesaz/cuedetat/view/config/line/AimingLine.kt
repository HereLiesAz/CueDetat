// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/config/line/AimingLine.kt

package com.hereliesaz.cuedetat.view.config.line

import com.hereliesaz.cuedetat.ui.theme.AccentGold
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
}