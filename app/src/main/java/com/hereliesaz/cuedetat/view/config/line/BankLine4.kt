package com.hereliesaz.cuedetat.view.config.line

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.BankLine4Yellow
import com.hereliesaz.cuedetat.view.config.base.LinesConfig

data class BankLine4(
    override val label: String = "Bank 4",
    override val opacity: Float = 1.0f,
    override val glowWidth: Float = 10f,
    override val glowColor: Color = BankLine4Yellow.copy(alpha = 0.4f),
    override val strokeWidth: Float = 2f,
    override val strokeColor: Color = BankLine4Yellow,
    override val additionalOffset: Float = 0f
) : LinesConfig