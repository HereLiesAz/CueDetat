// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/config/table/Table.kt

package com.hereliesaz.cuedetat.view.config.table

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.AcidPatina
import com.hereliesaz.cuedetat.ui.theme.AcidSpill
import com.hereliesaz.cuedetat.ui.theme.RogueUmber
import com.hereliesaz.cuedetat.view.config.base.TableComponentConfig

class Table : TableComponentConfig {
    override val strokeColor: Color = AcidPatina
    override val strokeWidth: Float = 4f
    override val glowColor: Color = AcidSpill
    override val glowWidth: Float = 4f
    override val opacity: Float = 1f
    override val fillColor: Color = Color.Transparent
    override val label: String = "Table"
    override val additionalOffset: Float = 0f
}