package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.ui.state.UiEvent

@Composable
fun MenuDrawer(
    onEvent: (UiEvent) -> Unit,
    onClose: () -> Unit
) {
    // ModalDrawerSheet is the actual visual surface of the drawer.
    // By applying padding here, we ensure its contents respect the system bars.
    ModalDrawerSheet(
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Menu", style = MaterialTheme.typography.titleLarge)
            Button(onClick = {
                onEvent(UiEvent.OnReset)
                onClose()
            }) {
                Text("Reset Scene")
            }
            // Add other menu items here
        }
    }
}