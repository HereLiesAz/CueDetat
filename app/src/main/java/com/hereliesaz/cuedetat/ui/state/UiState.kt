package com.hereliesaz.cuedetat.ui.state

import androidx.compose.ui.geometry.Offset
import com.google.ar.core.Anchor

data class UiState(
    val table: Anchor? = null,
    val cueBall: Anchor? = null,
    val objectBall: Anchor? = null,
    val isAiming: Boolean = true,
    val shotPower: Float = 0.5f,
    val cueballSpin: Offset = Offset.Zero,
    val shotType: ShotType = ShotType.CUT,
    val isDarkMode: Boolean = false,
    val showHelp: Boolean = false,

    // This will now control which mode we're in.
    val isArMode: Boolean = true,

    val instructionText: String = "Move phone to find a surface..."
)