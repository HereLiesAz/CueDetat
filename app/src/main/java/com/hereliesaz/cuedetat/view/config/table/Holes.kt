package com.hereliesaz.cuedetat.view.config.table

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.AcidPatina
import com.hereliesaz.cuedetat.view.config.base.TableComponentConfig

data class Holes(
    override val label: String = "Pockets",
    override val opacity: Float = 0.8f,
    override val glowWidth: Float = 0f,
    override val glowColor: Color = Color.Transparent,
    override val strokeWidth: Float = 8f,
    override val strokeColor: Color = AcidPatina,
    override val additionalOffset: Float = 0f,
    override val fillColor: Color = Color.Black
) : TableComponentConfig
