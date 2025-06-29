package com.hereliesaz.cuedetat.ar.renderables
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.xr.compose.material3.Shape
import androidx.xr.compose.material3.rememberMaterial
import androidx.xr.core.Pose
import com.hereliesaz.cuedetat.ui.theme.AccentGold

@Composable
fun HighlightDisc(pose: Pose) {
    val material = rememberMaterial(color = AccentGold.copy(alpha = 0.7f))
    Shape(
        pose = pose,
        shape = androidx.xr.compose.material3.Cylinder(
            height = 0.001.dp,
            radius = 0.04.dp
        ),
        material = material
    )
}