package com.hereliesaz.cuedetat.ui.state

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.ar.core.Pose

data class UiState(
    val table: TableState? = null,
    val cueBall: BallState? = null,
    val objectBall: BallState? = null,
    val isAiming: Boolean = true,
    val isDrawerOpen: Boolean = false,
    val shotPower: Float = 0.5f,
    val cueballSpin: Offset = Offset.Zero,
    val isDarkMode: Boolean = false,
    val showHelp: Boolean = false,
    val isArMode: Boolean = true
)

data class TableState(val pose: Pose)
data class BallState(val pose: Pose, val color: Color = Color.White)