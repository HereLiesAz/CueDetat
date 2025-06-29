package com.hereliesaz.cuedetat.ar.renderables

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.xr.compose.ar.Trackable
import androidx.xr.compose.ar.TrackableManager
import androidx.xr.compose.material3.Shape
import androidx.xr.compose.material3.rememberMaterial
import androidx.xr.core.Pose
import com.hereliesaz.cuedetat.ar.ARConstants

@Composable
fun Ball(
    pose: Pose,
    color: Color,
    id: Int,
    trackableManager: TrackableManager<Int>,
    isSelected: Boolean,
    onBallTapped: () -> Unit
) {
    val material = rememberMaterial(color = color)
    Trackable(
        manager = trackableManager,
        trackableId = id,
        pose = pose,
        onTap = { _, _ -> onBallTapped() }
    ) {
        Shape(
            shape = androidx.xr.compose.material3.Sphere(ARConstants.BALL_DIAMETER.dp),
            material = material
        )
        if (isSelected) {
            HighlightDisc(pose = Pose.IDENTITY)
        }
    }
}