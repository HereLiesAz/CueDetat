package com.hereliesaz.cuedetat.ar.scene

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.xr.compose.Cube
import androidx.xr.compose.Group
import androidx.xr.compose.material.ColorMaterial
import com.hereliesaz.cuedetat.ar.ARConstants
import com.hereliesaz.cuedetat.ui.state.TableState

@Composable
fun Table(tableState: TableState) {
    val feltMaterial = ColorMaterial(color = Color(0xFF006A4E))
    val woodMaterial = ColorMaterial(color = Color(0xFF855E42))

    Group(pose = tableState.pose) {
        // Table Surface
        Cube(
            size = floatArrayOf(ARConstants.TABLE_WIDTH, ARConstants.TABLE_HEIGHT, ARConstants.TABLE_DEPTH),
            material = feltMaterial
        )
        // Add Rails and other components here
    }
}