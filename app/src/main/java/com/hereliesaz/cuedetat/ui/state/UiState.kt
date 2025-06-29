package com.hereliesaz.cuedetat.ui.state

import com.google.ar.core.Pose

data class UiState(
    val tablePlaced: Boolean = false,
    val tablePose: Pose = Pose.IDENTITY,
    val cueBallPose: Pose = Pose.IDENTITY,
    val objectBallPose: Pose = Pose.IDENTITY
)
