package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.ui.state.UiEvent
import com.hereliesaz.cuedetat.ui.state.UiState
import perfetto.protos.UiState

@Composable
fun MenuDrawer(
    uiState: UiState,
    onEvent: (UiEvent) -> Unit,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = uiState.drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    icon = { Icon(painterResource(id = R.drawable.ic_undo_24), contentDescription = null) },
                    label = { Text("Reset Scene") },
                    selected = false,
                    onClick = { onEvent(UiEvent.OnReset) }
                )
                NavigationDrawerItem(
                    icon = { Icon(painterResource(id = R.drawable.ic_help_outline_24), contentDescription = null) },
                    label = { Text("Help") },
                    selected = false,
                    onClick = { onEvent(UiEvent.ToggleHelpDialog) }
                )
                // Add other items here
            }
        },
        content = content
    )
}
