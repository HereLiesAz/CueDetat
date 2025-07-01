package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.hereliesaz.cuedetat.ui.state.UiEvent

@Composable
fun MenuDrawer(
    onEvent: (UiEvent) -> Unit,
    onClose: () -> Unit
) {
    // TODO: Re-implement the full menu UI using onEvent to signal actions
    Button(onClick = { onEvent(UiEvent.OnReset) }) {
        Text("Reset Scene")
    }
}
