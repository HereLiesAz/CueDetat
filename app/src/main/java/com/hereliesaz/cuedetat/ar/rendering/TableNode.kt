package com.hereliesaz.cuedetat.ar.rendering

import androidx.compose.runtime.Composable
import com.google.ar.core.Pose
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.material.MaterialInstance
import io.github.sceneview.node.CubeNode

@Composable
fun TableNode(
    pose: Pose,
    material: MaterialInstance? = null
) {
    ArNode(
        pose = pose,
    ) {
        CubeNode(
            center = com.google.ar.sceneform.math.Vector3(0f, -0.025f, 0f),
            size = com.google.ar.sceneform.math.Vector3(1.27f, 0.05f, 2.54f),
            material = material
        )
    }
}
