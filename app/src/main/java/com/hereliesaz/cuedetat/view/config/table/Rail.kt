package com.hereliesaz.cuedetat.view.config.table

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.AcidPatina
import com.hereliesaz.cuedetat.view.config.base.TableComponentConfig

data class Rail(
    override val label: String = "Rails",
    override val opacity: Float = 1.0f,
    override val glowWidth: Float = 12f,
    override val glowColor: Color = Color.White.copy(alpha = 0.3f),
    override val strokeWidth: Float = 3f,
    override val strokeColor: Color = AcidPatina, // Corrected to use the one true color
    override val additionalOffset: Float = 0f, // Lift is handled by railPitchMatrix
    override val fillColor: Color = Color.Transparent
) : TableComponentConfig