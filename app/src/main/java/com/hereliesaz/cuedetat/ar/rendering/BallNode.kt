// app/src/main/java/com/hereliesaz/cuedetat/ar/rendering/BallNode.kt
package com.hereliesaz.cuedetat.ar.rendering

import android.content.Context
import android.util.Log
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory

import io.github.sceneview.collision.Vector3
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import io.github.sceneview.math.Color


/**
 * BallNode represents a ghost ball rendered in 3D AR space.
 * Its color and size can now be customized.
 */
class BallNode(
    context: Context,
    color: Color,
    radius: Float = 0.028575f // Standard pool ball radius in meters
) : Node() {

    init {
        MaterialFactory.makeTransparentWithColor(context, color)
            .thenAccept { material ->
                this.renderable = ShapeFactory.makeSphere(
                    radius, Vector3.zero(), material
                )
            }
            .exceptionally { throwable ->
                Log.e("BallNode", "Failed to create material", throwable)
                null
            }
    }
}