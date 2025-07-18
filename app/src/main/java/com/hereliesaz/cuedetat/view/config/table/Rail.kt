// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/config/table/Rail.kt

package com.hereliesaz.cuedetat.view.config.table

import com.hereliesaz.cuedetat.ui.theme.AcidPatina
import com.hereliesaz.cuedetat.ui.theme.OracleGlow
import com.hereliesaz.cuedetat.view.config.base.LinesConfig

class Rail: LinesConfig {
    override val strokeColor = AcidPatina
    override val strokeWidth = 8f
    override val glowColor = OracleGlow
    override val glowWidth = 14f
    override val opacity = 1f
    override val label = "Rail"
    override val additionalOffset = 0f
}