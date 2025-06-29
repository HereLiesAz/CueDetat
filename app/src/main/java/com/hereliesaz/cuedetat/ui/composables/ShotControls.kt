package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.ui.state.ShotType
import com.hereliesaz.cuedetat.ui.state.UiEvent
import com.hereliesaz.cuedetat.ui.state.UiState

@Composable
fun ShotControls(
    modifier: Modifier = Modifier,
    uiState: UiState,
    onMenuClick: () -> Unit,
    onEvent: (UiEvent) -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = uiState.statusText)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Example Button for Shot Type
            IconButton(onClick = { onEvent(UiEvent.OnShotTypeSelect(ShotType.JUMP)) }) {
                Text(text = "Jump")
            }
            // Add other controls for spin, elevation etc. here
        }

        IconButton(onClick = onMenuClick) {
            // Icon for menu
            Text(text = "Menu")
        }
    }
}