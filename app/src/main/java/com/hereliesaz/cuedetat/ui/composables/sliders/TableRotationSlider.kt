package com.hereliesaz.cuedetat.ui.composables.sliders

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun TableRotationSlider(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit
) {
    Column {
        Text("Table Rotation: ${uiState.table.rotationDegrees.toInt()}°")
        Slider(
            value = uiState.table.rotationDegrees,
            onValueChange = { onEvent(MainScreenEvent.TableRotationChanged(it)) },
            valueRange = 0f..360f
        )
    }
}