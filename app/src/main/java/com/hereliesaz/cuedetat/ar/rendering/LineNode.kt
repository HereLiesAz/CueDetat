// app/src/main/java/com/hereliesaz/cuedetat/ar/rendering/LineNode.kt
package com.hereliesaz.cuedetat.ar.rendering

import android.content.Context
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import io.github.sceneview.collision.Quaternion
import io.github.sceneview.collision.Vector3
import io.github.sceneview.node.Node

/**
 * Creates a 3D line represented by a thin cylinder between two points.
 */
class LineNode(
    context: Context,
    start: Vector3,
    end: Vector3,
    color: Color,
    radius: Float = 0.0025f // 2.5mm radius for the line
) : Node() {

    init {
        MaterialFactory.makeOpaqueWithColor(context, color)
            .thenAccept { material ->
                setupLine(material, start, end, radius)
            }
    }

    private fun setupLine(material: Material, start: Vector3, end: Vector3, radius: Float) {
        val diff = Vector3.subtract(end, start)
        val length = diff.length()

        // A cylinder's default orientation is along its Y-axis. We need to rotate it
        // to align with the direction vector (diff).
        val direction = diff.normalized()
        val rotation = Quaternion.lookRotation(direction, Vector3.up())

        // Create the cylinder renderable.
        val cylinder = ShapeFactory.makeCylinder(radius, length, Vector3.zero(), material)

        // Position and orient the line node.
        this.renderable = cylinder
        this.worldPosition = Vector3.add(start, end).scaled(0.5f) // Center of the line
        this.worldRotation = rotation
    }
}