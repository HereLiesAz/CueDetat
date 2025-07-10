package com.hereliesaz.cuedetat.view.config.line

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.BankLine1Yellow
import com.hereliesaz.cuedetat.view.config.base.LinesConfig

data class BankLine1(
    override val label: String = "Bank 1",
    override val opacity: Float = 1.0f,
    override val glowWidth: Float = 10f,
    override val glowColor: Color = Color.White.copy(alpha = 0.4f),
    override val strokeWidth: Float = 5f,
    override val strokeColor: Color = BankLine1Yellow,
    override val additionalOffset: Float = 0f
) : LinesConfig