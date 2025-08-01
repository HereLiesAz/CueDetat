// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/MenuDrawer.kt

package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
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
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.ui.theme.VoidBlack
import com.hereliesaz.cuedetat.view.state.DistanceUnit

@Composable
fun MenuDrawerContent(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(260.dp),
        drawerContainerColor = VoidBlack
    ) {
        Column { // Main container for scroll + fixed footer
            // --- Scrolling Content ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
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
                    val versionInfo =
                        "v${BuildConfig.VERSION_NAME}" + (uiState.latestVersionName?.let { " (latest: $it)" }
                            ?: "")
                    Text(
                        text = versionInfo,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
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

                if (uiState.experienceMode != ExperienceMode.BEGINNER) {
                    val cameraToggleText =
                        if (uiState.isCameraVisible) "Turn Camera Off" else "Turn Camera On"
                    MenuItem(
                        text = cameraToggleText,
                        onClick = { onEvent(MainScreenEvent.ToggleCamera); onCloseDrawer() }
                    )
                }
                if (uiState.experienceMode != ExperienceMode.BEGINNER) {
                    val bankingModeToggleText =
                        if (uiState.isBankingMode) "Ghost Ball Aiming" else "Calculate Bank"
                    MenuItem(
                        text = bankingModeToggleText,
                        onClick = { onEvent(MainScreenEvent.ToggleBankingMode); onCloseDrawer() }
                    )
                }

                MenuDivider()

                if (uiState.experienceMode != ExperienceMode.BEGINNER) {
                    MenuItem(
                        text = "Table Alignment",
                        onClick = { onEvent(MainScreenEvent.ToggleQuickAlignScreen); onCloseDrawer() }
                    )
                    MenuItem(
                        text = "Table Size",
                        onClick = { onEvent(MainScreenEvent.ToggleTableSizeDialog); onCloseDrawer() }
                    )
                    val distanceUnitToggleText =
                        if (uiState.distanceUnit == DistanceUnit.METRIC) "Use Imperial Units" else "Use Metric Units"
                    MenuItem(
                        text = distanceUnitToggleText,
                        onClick = { onEvent(MainScreenEvent.ToggleDistanceUnit); onCloseDrawer() }
                    )
                }
                MenuDivider()

                val orientationToShow = uiState.pendingOrientationLock ?: uiState.orientationLock
                val orientationToggleText = when (orientationToShow) {
                    CueDetatState.OrientationLock.AUTOMATIC -> "Orientation: Auto"
                    CueDetatState.OrientationLock.PORTRAIT -> "Orientation: Portrait"
                    CueDetatState.OrientationLock.LANDSCAPE -> "Orientation: Landscape"
                }
                MenuItem(
                    text = orientationToggleText,
                    onClick = { onEvent(MainScreenEvent.ToggleOrientationLock) }
                )
                MenuItem(
                    text = "Luminance",
                    onClick = { onEvent(MainScreenEvent.ToggleLuminanceDialog); onCloseDrawer() }
                )
                MenuDivider()

                if (uiState.experienceMode != ExperienceMode.BEGINNER) {
                    MenuItem(
                        text = "Too Advanced Options",
                        onClick = { onEvent(MainScreenEvent.ToggleAdvancedOptionsDialog); onCloseDrawer() }
                    )
                }
            }

            // --- Fixed Footer ---
            Column {
                MenuDivider()
                val modeToShow =
                    uiState.experienceMode ?: ExperienceMode.EXPERT
                MenuItem(
                    text = "Mode: ${
                        modeToShow.name.lowercase().replaceFirstChar { it.titlecase() }
                    }",
                    onClick = { onEvent(MainScreenEvent.ToggleExperienceModeSelection); onCloseDrawer() }
                )
                MenuItem(
                    text = "About",
                    onClick = { onEvent(MainScreenEvent.ViewAboutPage); onCloseDrawer() }
                )
                MenuItem(
                    text = "Feedback",
                    onClick = { onEvent(MainScreenEvent.SendFeedback); onCloseDrawer() }
                )
                MenuItem(
                    text = "@HereLiesAz",
                    onClick = { onEvent(MainScreenEvent.ViewArt); onCloseDrawer() })
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium, // Reduced text size
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.5f
            )
        )
    }
}

@Composable
private fun MenuDivider() {
    Divider(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}