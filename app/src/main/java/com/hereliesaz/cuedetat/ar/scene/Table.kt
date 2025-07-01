package com.hereliesaz.cuedetat.ar.scene

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.xr.compose.subspace.layout.SubspaceLayout
import androidx.xr.compose.spatial.components.Box
import androidx.xr.compose.spatial.components.Material
import com.hereliesaz.cuedetat.ar.ARConstants

@Composable
fun Table(modifier: Modifier = Modifier) {
    SubspaceLayout(modifier = modifier) {
        Box(
            modifier = Modifier.size(
                width = ARConstants.TABLE_WIDTH,
                height = ARConstants.TABLE_HEIGHT,
                depth = ARConstants.TABLE_DEPTH
            ),
            material = Material.color(Color(0xFF006A4E))
        )
    }
}
