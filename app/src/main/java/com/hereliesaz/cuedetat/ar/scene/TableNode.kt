package com.hereliesaz.cuedetat.ar.scene

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.ar.ARConstants
import androidx.xr.scenecore.Node
import androidx.xr.scenecore.Primitive
import androidx.xr.scenecore.material.Material

class TableNode : Node() {
    init {
        val feltMaterial = Material.builder()
            .setBaseColor(Color(0xFF006A4E).toArgb())
            .build()

        val tableSurface = Node().apply {
            primitive = Primitive.createCube(
                ARConstants.TABLE_WIDTH,
                ARConstants.TABLE_HEIGHT,
                ARConstants.TABLE_DEPTH
            )
            primitive?.material = feltMaterial
        }
        addChild(tableSurface)

        // TODO: Add rails and pockets as child nodes
    }
}
