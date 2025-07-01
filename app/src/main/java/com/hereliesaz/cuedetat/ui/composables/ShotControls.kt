package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import com.hereliesaz.cuedetat.ui.state.UiEvent

@Composable
fun ShotControls(
    shotPower: Float,
    spin: Offset,
    onEvent: (UiEvent) -> Unit
) {
    // TODO: Re-implement the full shot controls UI using onEvent
    Slider(value = shotPower, onValueChange = { onEvent(UiEvent.SetShotPower(it)) })
    Button(onClick = { onEvent(UiEvent.ExecuteShot) }) {
        Text("Shoot")
    }
}
