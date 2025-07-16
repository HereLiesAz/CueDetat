// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/MenuDrawer.kt
package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.BuildConfig
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.model.DistanceUnit
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun MenuDrawerContent(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center
                )
                val versionInfo = "v${BuildConfig.VERSION_NAME}" + (uiState.latestVersionName?.let { " (latest: $it)" } ?: "Checking...")
                Text(
                    text = versionInfo,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            MenuDivider()

            MenuItem(
                text = if (uiState.isBankingMode) "Ghost Ball Aiming" else "Calculate Bank",
                onClick = { onEvent(MainScreenEvent.ToggleBankingMode); onCloseDrawer() }
            )
            if (!uiState.isBankingMode) {
                MenuItem(
                    text = if (uiState.onPlaneBall != null) "Hide Cue Ball" else "Toggle Cue Ball",
                    onClick = { onEvent(MainScreenEvent.ToggleOnPlaneBall); onCloseDrawer() }
                )
            }
            MenuItem(
                text = if (uiState.isCameraVisible) "Turn Camera Off" else "Turn Camera On",
                onClick = { onEvent(MainScreenEvent.ToggleCamera); onCloseDrawer() }
            )
            MenuDivider()

            if (!uiState.isBankingMode && !uiState.table.isVisible) {
                MenuItem(
                    text = "Show Table",
                    onClick = { onEvent(MainScreenEvent.ToggleTable); onCloseDrawer() }
                )
            }
            MenuItem(
                text = "Table Size",
                onClick = { onEvent(MainScreenEvent.ToggleTableSizeDialog); onCloseDrawer() }
            )
            MenuItem(
                text = if (uiState.distanceUnit == DistanceUnit.IMPERIAL) "Use Metric Units" else "Use Imperial Units",
                onClick = { onEvent(MainScreenEvent.ToggleDistanceUnit); onCloseDrawer() }
            )
            MenuDivider()

            val systemIsCurrentlyDark = isSystemInDarkTheme()
            val themeToggleText = when (uiState.isForceLightMode) {
                true -> "Embrace the Darkness"
                false -> "Use System Theme"
                null -> if (systemIsCurrentlyDark) "Walk toward the Light" else "Embrace the Darkness"
            }
            MenuItem(
                text = themeToggleText,
                onClick = { onEvent(MainScreenEvent.ToggleForceTheme); onCloseDrawer() }
            )
            MenuItem(
                text = "Luminance",
                onClick = { onEvent(MainScreenEvent.ToggleLuminanceDialog); onCloseDrawer() }
            )
            MenuItem(
                text = "Glow Stick",
                onClick = { onEvent(MainScreenEvent.ToggleGlowStickDialog); onCloseDrawer() }
            )
            MenuDivider()

            MenuItem(
                text = stringResource(if (uiState.areHelpersVisible) R.string.hide_helpers else R.string.show_helpers),
                onClick = { onEvent(MainScreenEvent.ToggleHelp); onCloseDrawer() }
            )
            MenuItem(
                text = "Show Tutorial",
                onClick = { onEvent(MainScreenEvent.StartTutorial); onCloseDrawer() }
            )
            MenuDivider()

            MenuItem(
                text = "Check for Updates",
                onClick = { onEvent(MainScreenEvent.CheckForUpdate); onCloseDrawer() })
            MenuItem(
                text = "About Me",
                onClick = { onEvent(MainScreenEvent.ViewArt); onCloseDrawer() })
            MenuDivider()

            MenuItem(
                text = "Too Advanced Options",
                onClick = { onEvent(MainScreenEvent.ToggleAdvancedOptionsDialog); onCloseDrawer() }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}