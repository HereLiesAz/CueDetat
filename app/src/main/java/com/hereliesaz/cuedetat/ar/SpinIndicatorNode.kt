package com.hereliesaz.cuedetat.ar

import androidx.compose.ui.graphics.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.math.toOldColor
import io.github.sceneview.node.Node
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SpinIndicatorNode(
    coroutineScope: CoroutineScope,
    color: Color,
    radius: Float = 0.005f // Small dot
) : Node() {
    init {
        coroutineScope.launch {
            val material = MaterialFactory.makeOpaqueWithColor(engine, color.toOldColor()).await()
            val renderable = ShapeFactory.makeCylinder(engine,
                radius = radius,
                height = 0.001f, // Almost flat
                center = Float3(),
                material = material
            )
            setRenderable(renderable)
            // Rotate it to be flat
            rotation = Float3(x = 90.0f, y = 0.0f, z = 0.0f)
        }
    }
}