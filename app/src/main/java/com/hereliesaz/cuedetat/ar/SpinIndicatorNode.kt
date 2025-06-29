package com.hereliesaz.cuedetat.ar

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.xr.scenecore.impl.math.Vector3.Pose
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.material.rememberMaterial
import io.github.sceneview.material.baseColor
import io.github.sceneview.node.CylinderNode
import kotlin.math.sqrt

@Composable
fun SpinIndicatorNode(
    pose: Pose, // Should be the pose of the cue ball
    spinOffset: Float3
) {
    // A simple red material for the indicator
    val material = rememberMaterial {
        baseColor(Color.Red)
    }

    // Place an AR node at the cue ball's position
    ArNode(pose = pose) {
        // Calculate the position on the surface of the ball
        val x = spinOffset.x * ArConstants.BALL_RADIUS
        val y = spinOffset.y * ArConstants.BALL_RADIUS
        val zSquared = (ArConstants.BALL_RADIUS * ArConstants.BALL_RADIUS) - (x * x) - (y * y)
        val z = if (zSquared > 0) sqrt(zSquared) else 0f

        // Add a small cylinder as a child to indicate the spin contact point
        CylinderNode(
            material = material,
            radius = 0.005f,
            height = 0.002f,
            center = Float3(x, y, z) // Position it on the ball's surface, facing the camera
        )
    }
}
