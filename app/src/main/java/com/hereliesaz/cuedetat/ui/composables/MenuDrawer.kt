package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.ui.MainViewModel
import com.hereliesaz.cuedetat.ui.UiEvent
import com.hereliesaz.cuedetat.ui.state.MainUiState

@Composable
fun MenuDrawerContent(uiState: MainUiState, viewModel: MainViewModel, onCloseDrawer: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    ModalDrawerSheet {
        Text(stringResource(id = R.string.app_name), modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge)
        HorizontalDivider()
        if (uiState.tablePose != null) {
            DrawerMenuItem(
                icon = Icons.Default.Refresh,
                text = "Reset Scene",
                onClick = { viewModel.onEvent(UiEvent.OnReset); onCloseDrawer() }
            )
        }
        DrawerMenuItem(
            icon = Icons.Default.HelpOutline,
            text = "How to Use",
            onClick = { viewModel.onEvent(UiEvent.ToggleHelpDialog); onCloseDrawer() }
        )
        DrawerMenuItem(
            icon = Icons.Default.MonetizationOn,
            text = "Chalk Your Tip (Support)",
            onClick = {
                uriHandler.openUri("https://www.buymeacoffee.com/placeholder") // Replace with your actual link
                onCloseDrawer()
            }
        )
    }
}

@Composable
private fun DrawerMenuItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = text) },
        label = { Text(text) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}