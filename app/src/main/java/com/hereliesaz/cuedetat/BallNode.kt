package com.hereliesaz.cuedetat

import android.content.Context
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.math.Vector3

/**
 * BallNode represents a ghost ball rendered in 3D AR space.
 * Appears as a semi-transparent green sphere.
 */
class BallNode(context: Context) : Node() {

    private var renderable: ModelRenderable? = null

    init {
        MaterialFactory.makeTransparentWithColor(context, Color(0f, 1f, 0f, 0.5f))
            .thenAccept { material ->
                ModelRenderable.builder()
                    .setSource(
                        context,
                        com.google.ar.sceneform.rendering.ShapeFactory.makeSphere(
                            0.03f, Vector3.zero(), material
                        )
                    )
                    .build()
                    .thenAccept { model ->
                        renderable = model
                        this.renderable = model
                    }
            }
    }
}
