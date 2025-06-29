package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.ui.state.UiEvent

@Composable
fun MenuDrawer(
    drawerState: DrawerState,
    onEvent: (UiEvent) -> Unit,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Settings", style = MaterialTheme.typography.headlineSmall)
                    // Add settings toggles here in the future
                    // e.g., for dark mode, table color, etc.
                }
            }
        },
        content = content
    )
}