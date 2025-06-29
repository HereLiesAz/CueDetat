package com.hereliesaz.cuedetat.ar.rendering

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.xr.compose.Sphere
import androidx.xr.compose.material.ColorMaterial
import com.hereliesaz.cuedetat.ar.ARConstants

/**
 * Renders a sphere in the AR scene.
 *
 * @param color The color of the ball.
 * @param radius The radius of the ball, defaulting to the constant value.
 */
@Composable
fun BallNode(
    color: Color,
    radius: Float = ARConstants.BALL_RADIUS
) {
    val material = remember(color) {
        ColorMaterial(color = color)
    }

    Sphere(
        radius = radius,
        material = material
    )
}