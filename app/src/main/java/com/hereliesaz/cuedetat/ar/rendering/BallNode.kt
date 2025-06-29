package com.hereliesaz.cuedetat.ar.rendering

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.ar.core.Pose
import com.hereliesaz.cuedetat.ar.ArConstants
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.gestures.rememberTapGestureRecognizer
import io.github.sceneview.material.MaterialInstance
import io.github.sceneview.material.metallic
import io.github.sceneview.material.roughness
import io.github.sceneview.node.SphereNode

@Composable
fun BallNode(
    id: Int,
    pose: Pose,
    isSelected: Boolean,
    onBallTapped: (Int) -> Unit,
    color: Color = Color.White,
    material: MaterialInstance? = null
) {
    ArNode(
        pose = pose,
        gestureRecognizers = rememberTapGestureRecognizer { onBallTapped(id) }
    ) {
        SphereNode(
            radius = ArConstants.BALL_RADIUS,
            material = material?.apply {
                metallic = 0.2f
                roughness = 0.8f
            }
        )
    }
}
