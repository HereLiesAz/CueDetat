// app/src/main/java/com/hereliesaz/cuedetat/ar/rendering/TableNode.kt
package com.hereliesaz.cuedetat.ar.rendering

import android.content.Context
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import java.util.concurrent.CompletableFuture

/**
 * A procedurally generated 3D node representing a pool table.
 */
class TableNode(
    context: Context,
    tableWidth: Float, // Long side (X-axis) in meters
    tableDepth: Float, // Short side (Z-axis) in meters
    railHeight: Float = 0.035f, // Height of the rails
    railWidth: Float = 0.05f, // Width of the rails
    bedHeight: Float = 0.01f // Thickness of the table bed
) : Node() {

    companion object {
        private val BED_COLOR = Color(0.0f, 0.4f, 0.15f, 0.9f) // Dark green
        private val RAIL_COLOR = Color(0.35f, 0.2f, 0.05f, 1.0f) // Wood brown
    }

    init {
        val bedMaterialFuture = MaterialFactory.makeOpaqueWithColor(context, BED_COLOR)
        val railMaterialFuture = MaterialFactory.makeOpaqueWithColor(context, RAIL_COLOR)

        CompletableFuture.allOf(bedMaterialFuture, railMaterialFuture)
            .thenAccept {
                val bedMaterial = bedMaterialFuture.get()
                val railMaterial = railMaterialFuture.get()

                // 1. Create the table bed (playing surface)
                val bedNode = Node()
                bedNode.setParent(this)
                bedNode.renderable = ShapeFactory.makeCube(Vector3(tableWidth, bedHeight, tableDepth), Vector3(0f, -bedHeight / 2, 0f), bedMaterial)

                // 2. Create the rails
                val topRail = Node()
                topRail.setParent(this)
                topRail.localPosition = Vector3(0f, 0f, -tableDepth / 2)
                topRail.renderable = ShapeFactory.makeCube(Vector3(tableWidth + (2 * railWidth), railHeight, railWidth), Vector3.zero(), railMaterial)

                val bottomRail = Node()
                bottomRail.setParent(this)
                bottomRail.localPosition = Vector3(0f, 0f, tableDepth / 2)
                bottomRail.renderable = ShapeFactory.makeCube(Vector3(tableWidth + (2 * railWidth), railHeight, railWidth), Vector3.zero(), railMaterial)

                val leftRail = Node()
                leftRail.setParent(this)
                leftRail.localPosition = Vector3(-tableWidth / 2, 0f, 0f)
                leftRail.renderable = ShapeFactory.makeCube(Vector3(railWidth, railHeight, tableDepth), Vector3.zero(), railMaterial)

                val rightRail = Node()
                rightRail.setParent(this)
                rightRail.localPosition = Vector3(tableWidth / 2, 0f, 0f)
                rightRail.renderable = ShapeFactory.makeCube(Vector3(railWidth, railHeight, tableDepth), Vector3.zero(), railMaterial)
            }
    }
}