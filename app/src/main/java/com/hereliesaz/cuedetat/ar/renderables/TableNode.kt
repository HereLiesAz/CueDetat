package com.hereliesaz.cuedetat.ar.renderables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.xr.compose.Cube
import androidx.xr.compose.Group
import androidx.xr.compose.material.ColorMaterial
import com.hereliesaz.cuedetat.ar.ARConstants

private val ARConstants.Companion.TABLE_LENGTH: Any

/**
 * A composable that renders a 3D model of a pool table.
 * This is built declaratively using the primitives from androidx.xr.compose.
 */
@Composable
fun TableNode() {
    // Materials for the different parts of the table.
    // 'remember' ensures they are not recreated on every recomposition.
    val feltMaterial = remember { ColorMaterial(color = Color(0xFF006A4E)) } // Pool table green
    val woodMaterial = remember { ColorMaterial(color = Color(0xFF855E42)) } // A simple brown for the wood

    // A Group is used to combine multiple nodes into a single logical unit.
    // All transformations applied to the Group affect its children.
    Group {
        // Table Surface (the green felt area)
        Cube(
            size = floatArrayOf(
                ARConstants.TABLE_WIDTH,
                ARConstants.TABLE_HEIGHT,
                ARConstants.TABLE_LENGTH
            ),
            material = feltMaterial
        )

        // The four legs of the table.
        // They are positioned relative to the center of the Group.
        val legWidth = 0.1f
        val legHeight = 0.7f // Arbitrary height for the legs
        val legX = ARConstants.TABLE_WIDTH / 2 - legWidth / 2
        val legZ = ARConstants.TABLE_LENGTH / 2 - legWidth / 2
        val legY = -(legHeight / 2) - (ARConstants.TABLE_HEIGHT / 2)

        // Front-left leg
        Cube(
            size = floatArrayOf(legWidth, legHeight, legWidth),
            position = floatArrayOf(-legX, legY, -legZ),
            material = woodMaterial
        )

        // Front-right leg
        Cube(
            size = floatArrayOf(legWidth, legHeight, legWidth),
            position = floatArrayOf(legX, legY, -legZ),
            material = woodMaterial
        )

        // Back-left leg
        Cube(
            size = floatArrayOf(legWidth, legHeight, legWidth),
            position = floatArrayOf(-legX, legY, legZ),
            material = woodMaterial
        )

        // Back-right leg
        Cube(
            size = floatArrayOf(legWidth, legHeight, legWidth),
            position = floatArrayOf(legX, legY, legZ),
            material = woodMaterial
        )
    }
}