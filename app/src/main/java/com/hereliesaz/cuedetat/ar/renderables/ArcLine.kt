package com.hereliesaz.cuedetat.ar.renderables

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.xr.compose.material3.rememberMaterial
import dev.romainguy.kotlin.math.Float3

/**
 * Renders a parabolic arc between a start and end point, with a specified height.
 */
@Composable
fun ArcLine(start: Float3, end: Float3, height: Float, color: Color, steps: Int = 20) {
    val material = rememberMaterial(color = color)
    val points = mutableListOf<Float3>()
    for (i in 0..steps) {
        val t = i.toFloat() / steps
        // Linear interpolation for X and Z
        val x = (1 - t) * start.x + t * end.x
        val z = (1 - t) * start.z + t * end.z
        // Parabolic interpolation for Y (height)
        // 4 * h * (t - t^2) gives a parabola that starts at 0, peaks at h (at t=0.5), and ends at 0.
        val y = start.y + 4 * height * (t - t * t)
        points.add(Float3(x, y, z))
    }
    Polyline(points = points, thickness = 0.003.dp, material = material)
}