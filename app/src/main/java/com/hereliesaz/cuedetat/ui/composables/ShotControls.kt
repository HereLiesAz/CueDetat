package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.hereliesaz.cuedetat.ui.state.UiEvent

@Composable
fun ShotControls(
    modifier: Modifier = Modifier, // Add the modifier parameter here
    shotPower: Float,
    spin: Offset,
    onEvent: (UiEvent) -> Unit,
    onMenuClick: () -> Unit
) {
    // Apply the modifier to the Column
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TODO: Re-implement the full shot controls UI using onEvent, including a menu button that calls onMenuClick
        Slider(value = shotPower, onValueChange = { onEvent(UiEvent.SetShotPower(it)) })
        Button(onClick = { onEvent(UiEvent.ExecuteShot) }) {
            Text("Shoot")
        }
    }
}