package com.hereliesaz.cuedetat.ui.state

import com.google.ar.core.Pose
import androidx.compose.ui.geometry.Offset

data class UiState(
    // AR State
    val table: TableState? = null,
    val cueBall: BallState? = null,
    val objectBall: BallState? = null,

    // UI and Shot State
    val isDrawerOpen: Boolean = false,
    val shotPower: Float = 50f,
    val cueballSpin: Offset = Offset.Zero,
    val isAiming: Boolean = true // To differentiate between placing objects and taking a shot
)

data class TableState(val pose: Pose)
data class BallState(val pose: Pose, val isMoving: Boolean = false)