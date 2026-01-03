package com.hereliesaz.cuedetat.ui.state

import androidx.compose.ui.geometry.Offset
import com.google.ar.core.Anchor

enum class AppState { DetectingPlanes, ReadyToPlace, ScenePlaced }

data class UiState(
    val isArMode: Boolean = false,
    val isDarkMode: Boolean = true,
    val showHelp: Boolean = false,
    val instructionText: String = "Move phone to start AR.",
    val table: Anchor? = null,
    val cueBall: Anchor? = null,
    val objectBall: Anchor? = null,
    val isAiming: Boolean = true,
    val shotPower: Float = 0.5f,
    val cueballSpin: Offset = Offset.Zero,
    val shotType: ShotType = ShotType.CUT
)
