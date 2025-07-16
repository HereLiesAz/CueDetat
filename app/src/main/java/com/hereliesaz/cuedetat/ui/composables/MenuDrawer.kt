package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.BuildConfig
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun MenuDrawerContent(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val context = LocalContext.current

    ModalDrawerSheet {
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Cue D'etat",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "v${BuildConfig.VERSION_NAME} ${uiState.latestVersionName?.let { "(latest: $it)" } ?: ""}",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodySmall
        )
        Divider(modifier = Modifier.padding(vertical = 12.dp))

        DrawerMenuItem(
            text = if (uiState.isBankingMode) "Protractor Mode" else "Banking Mode",
            icon = Icons.Default.SwapHoriz,
            onClick = {
                onEvent(MainScreenEvent.ToggleBankingMode)
                onCloseDrawer()
            }
        )

        DrawerMenuItem(
            text = "Distance: ${uiState.distanceUnit}",
            icon = Icons.Default.Straighten,
            onClick = { onEvent(MainScreenEvent.ToggleDistanceUnit) }
        )

        DrawerMenuItem(
            text = if (uiState.isCameraVisible) "Hide Camera" else "Show Camera",
            icon = if (uiState.isCameraVisible) Icons.Default.VideocamOff else Icons.Default.Videocam,
            onClick = { onEvent(MainScreenEvent.ToggleCamera) }
        )

        DrawerMenuItem(
            text = "Force Theme: ${if (uiState.isForceLightMode == true) "Light" else if (uiState.isForceLightMode == false) "Dark" else "System"}",
            icon = Icons.Default.Brightness4,
            onClick = { onEvent(MainScreenEvent.ToggleForceTheme) }
        )

        DrawerMenuItem(
            text = "Advanced",
            icon = Icons.Default.Settings,
            onClick = { onEvent(MainScreenEvent.ToggleAdvancedOptions) }
        )

        DrawerMenuItem(
            text = "Help",
            icon = Icons.Default.HelpOutline,
            onClick = { onEvent(MainScreenEvent.ToggleHelp) }
        )

        DrawerMenuItem(
            text = "Check for Update",
            icon = Icons.Default.SystemUpdate,
            onClick = { onEvent(MainScreenEvent.CheckForUpdate) }
        )
        DrawerMenuItem(
            text = "Visit the Gallery",
            icon = Icons.Default.PhotoLibrary,
            onClick = { onEvent(MainScreenEvent.ViewArt) }
        )
        DrawerMenuItem(
            text = context.getString(R.string.privacy_policy),
            icon = Icons.Default.PrivacyTip,
            onClick = { onEvent(MainScreenEvent.ViewArt) }
        )
    }
}

@Composable
fun DrawerMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(text) },
        icon = { Icon(icon, contentDescription = text) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}