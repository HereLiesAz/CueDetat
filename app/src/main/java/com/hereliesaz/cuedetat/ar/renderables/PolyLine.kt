package com.hereliesaz.cuedetat.ar.renderables

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.xr.compose.material3.Polyline
import androidx.xr.compose.material3.rememberMaterial
import dev.romainguy.kotlin.math.Float3

/**
 * Renders a line that connects a list of 3D points.
 */
@Composable
fun Polyline(points: List<Float3>, color: Color, thickness: Float = 0.003f) {
    if (points.size < 2) return
    val material = rememberMaterial(color = color)
    Polyline(points = points, thickness = thickness.dp, material = material)
}