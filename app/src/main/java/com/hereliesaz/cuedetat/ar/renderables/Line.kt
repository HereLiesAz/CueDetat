package com.hereliesaz.cuedetat.ar.renderables
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.xr.compose.material3.Shape
import androidx.xr.compose.material3.rememberMaterial
import androidx.xr.core.Pose
import com.hereliesaz.cuedetat.ar.MathUtils.length
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.lookAt

@Composable
fun Line(start: Float3, end: Float3, color: Color, thickness: Float = 0.002f) {
    val material = rememberMaterial(color = color)
    val diff = end - start
    val distance = length(diff)
    if (distance == 0f) return

    val center = start + diff * 0.5f
    val rotation = lookAt(Float3(0f, 1f, 0f), diff)

    Shape(
        pose = Pose(center, rotation.xyzw),
        shape = androidx.xr.compose.material3.Cylinder(height = distance.dp, radius = thickness.dp),
        material = material
    )
}