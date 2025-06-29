package com.hereliesaz.cuedetat.ar.rendering

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.material.MaterialInstance
import io.github.sceneview.node.LineNode

@Composable
fun LineNode(
    start: Float3,
    end: Float3,
    color: Color,
    thickness: Float = 0.005f,
    material: MaterialInstance? = null
) {
    LineNode(
        startPosition = start,
        endPosition = end,
        thickness = thickness,
        material = material
    )
}
