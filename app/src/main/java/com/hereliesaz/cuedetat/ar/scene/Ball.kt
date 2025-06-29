package com.hereliesaz.cuedetat.ar.scene

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.xr.compose.Sphere
import androidx.xr.compose.material.ColorMaterial
import com.hereliesaz.cuedetat.ar.ARConstants
import com.hereliesaz.cuedetat.ui.state.BallState

@Composable
fun Ball(ballState: BallState) {
    val material = ColorMaterial(color = ballState.color)
    Sphere(
        radius = ARConstants.BALL_RADIUS,
        material = material,
        pose = ballState.pose
    )
}