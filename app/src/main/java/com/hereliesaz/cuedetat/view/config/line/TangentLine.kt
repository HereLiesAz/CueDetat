// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/config/line/TangentLine.kt

package com.hereliesaz.cuedetat.view.config.line

import com.hereliesaz.cuedetat.ui.theme.MutedGray
import com.hereliesaz.cuedetat.ui.theme.OracleGlow
import com.hereliesaz.cuedetat.view.config.base.LinesConfig

class TangentLine: LinesConfig {
    override val strokeColor = MutedGray
    override val strokeWidth = 2f
    override val glowColor = OracleGlow
    override val glowWidth = 8f
    override val opacity = 0.7f
    override val label = "Tangent Line"
    override val additionalOffset = 0f
}