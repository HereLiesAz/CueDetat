package com.hereliesaz.cuedetat.ui.state

import androidx.compose.ui.geometry.Offset
import androidx.xr.scenecore.Entity

data class UiState(
    val table: Entity? = null,
    val cueBall: Entity? = null,
    val objectBall: Entity? = null,
    val isAiming: Boolean = true,
    val shotPower: Float = 0.5f,
    val cueballSpin: Offset = Offset.Zero,
    val isDarkMode: Boolean = false,
    val showHelp: Boolean = false,
    val isArMode: Boolean = true
)
