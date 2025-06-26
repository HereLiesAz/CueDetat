package com.hereliesaz.cuedetat.ar.rendering

import android.content.Context
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory

/**
 * BallNode represents a ghost ball rendered in 3D AR space.
 * Appears as a semi-transparent green sphere.
 */
class BallNode(context: Context) : Node() {

    private var sphereRenderable: ModelRenderable? = null

    init {
        MaterialFactory.makeTransparentWithColor(context, Color(0.1f, 1f, 0.2f, 0.5f))
            .thenAccept { material ->
                sphereRenderable = ShapeFactory.makeSphere(
                    0.03f, Vector3(0.0f, 0.03f, 0.0f), material
                )
                this.renderable = sphereRenderable
            }
            .exceptionally { throwable ->
                // Log error
                null
            }
    }
}