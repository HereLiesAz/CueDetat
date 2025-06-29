package com.hereliesaz.cuedetat.ui.state

import androidx.compose.ui.geometry.Offset
import androidx.xr.runtime.math.Pose

enum class AppState { DetectingPlanes, ReadyToPlace, ScenePlaced }
enum class ShotType { CUT, BANK, KICK, JUMP, MASSE } // Added JUMP and MASSE
data class BallState(var pose: Pose, var isBeingDragged: Boolean = false)

data class MainUiState(
    val appState: AppState = AppState.DetectingPlanes,
    val statusText: String = "Move phone to detect a surface...",
    val tablePose: Pose? = null,
    val cueBallPose: Pose? = null,
    val objectBallPose: Pose? = null,
    val shotType: ShotType = ShotType.CUT,
    val selectedBall: Int? = null,
    val showHelpDialog: Boolean = false,
    val spinOffset: Offset = Offset.Zero,
    val cueElevation: Float = 0f, // For jump/masse shots, range 0.0 to 1.0
    val warningMessage: String? = null
)