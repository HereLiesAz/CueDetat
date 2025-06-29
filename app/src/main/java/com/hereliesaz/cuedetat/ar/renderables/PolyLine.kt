package com.hereliesaz.cuedetat.ar.renderables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.xr.compose.CustomNode
import androidx.xr.compose.material.ColorMaterial
import androidx.xr.compose.mesh.Mesh
import androidx.xr.compose.mesh.PrimitiveType
import dev.romainguy.kotlin.math.Float3

/**
 * A composable that renders a series of connected line segments in the AR scene.
 * This is built using the foundational primitives of the androidx.xr.compose library.
 *
 * @param points A list of 3D points that define the vertices of the polyline.
 * @param color The color of the line.
 *
 * Note: The thickness of a line drawn with PrimitiveType.LineStrip is not guaranteed
 * to be more than 1 pixel. For thicker lines, a more complex mesh, such as a series
 * of thin cylinders or a quad strip, would be required.
 */
@Composable
fun PolylineNode(
    points: List<Float3>,
    color: Color
) {
    // A line requires at least two points to be drawn.
    if (points.size < 2) {
        return
    }

    // Create a mesh and material that are remembered across recompositions.
    val lineMesh = remember(points) {
        // Flatten the list of Float3 points into a single FloatArray for the vertex buffer.
        val vertexBuffer = points.flatMap { listOf(it.x, it.y, it.z) }.toFloatArray()

        // Create an index buffer to draw the vertices as a connected strip.
        val indexBuffer = (points.indices).toList().toIntArray()

        Mesh(
            vertexData = mapOf(Mesh.VERTEX_BUFFER_KEY to vertexBuffer),
            indices = mapOf(Mesh.DEFAULT_INDEX_BUFFER_KEY to indexBuffer),
            primitiveType = PrimitiveType.LineStrip // This primitive type connects all vertices in order.
        )
    }

    val material = remember(color) {
        ColorMaterial(color = color)
    }

    // CustomNode allows for drawing a custom mesh with a specified material.
    CustomNode {
        drawMesh(lineMesh, material)
    }
}