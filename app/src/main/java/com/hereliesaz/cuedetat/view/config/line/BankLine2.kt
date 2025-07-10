package com.hereliesaz.cuedetat.view.config.line

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.BankLine2Yellow
import com.hereliesaz.cuedetat.view.config.base.LinesConfig

data class BankLine2(
    override val label: String = "Bank 2",
    override val opacity: Float = 1.0f,
    override val glowWidth: Float = 10f,
    override val glowColor: Color = Color.White.copy(alpha = 0.4f),
    override val strokeWidth: Float = 4f,
    override val strokeColor: Color = BankLine2Yellow,
    override val additionalOffset: Float = 0f
) : LinesConfig