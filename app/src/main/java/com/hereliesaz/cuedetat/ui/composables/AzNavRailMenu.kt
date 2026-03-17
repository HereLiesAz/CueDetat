package com.hereliesaz.cuedetat.ui.composables

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.hereliesaz.aznavrail.AzHostActivityLayout
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.DistanceUnit

/**
 * The main navigation and control rail for the application.
 *
 * Uses [AzHostActivityLayout] as the mandatory top-level container, managing safe zones,
 * device rotation, and z-ordering. Navigation items drive the [navController] for routing.
 *
 * @param uiState The current state of the application.
 * @param onEvent Callback to dispatch events when menu items are clicked.
 * @param navController The NavHostController used for screen routing.
 * @param currentDestination The current back-stack route, used to highlight the active item.
 * @param content The screen content to display in the background layer.
 */
@Composable
fun AzNavRailMenu(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    navController: NavHostController,
    currentDestination: String?,
    content: @Composable () -> Unit,
) {
    val versionInfo = "v1.0" + (uiState.latestVersionName?.let { " (latest: $it)" } ?: "")

    val strAppName = stringResource(id = R.string.app_name)
    val strHideHelpers = stringResource(R.string.hide_helpers)
    val strShowHelpers = stringResource(R.string.show_helpers)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val activeColor = MaterialTheme.colorScheme.primary

    AzHostActivityLayout(
        navController = navController,
        modifier = Modifier,
        currentDestination = currentDestination,
        isLandscape = isLandscape,
        initiallyExpanded = false
    ) {
        azConfig(
            dockingSide = AzDockingSide.LEFT,
            packButtons = true
        )

        azTheme(
            defaultShape = AzButtonShape.CIRCLE,
            activeColor = activeColor,
            headerIconShape = AzHeaderIconShape.NONE
        )

        azAdvanced(
            isLoading = false,
            enableRailDragging = false,
            helpEnabled = false,
            onDismissHelp = {}
        )

        // --- Header Section ---
        azMenuItem(
            id = "app_name",
            text = strAppName,
            disabled = true
        )
        azRailItem(
            id = "version",
            text = versionInfo,
            onClick = { onEvent(MainScreenEvent.CheckForUpdate) }
        )

        azDivider()

        // --- Core Controls Section ---

        azRailToggle(
            id = "help",
            isChecked = uiState.areHelpersVisible,
            toggleOnText = strHideHelpers,
            toggleOffText = strShowHelpers,
            onClick = { onEvent(MainScreenEvent.ToggleHelp) }
        )

        azRailItem(
            id = "tutorial",
            text = "Show Tutorial",
            onClick = { onEvent(MainScreenEvent.StartTutorial) }
        )

        azRailToggle(
            id = "spin",
            isChecked = uiState.isSpinControlVisible,
            toggleOnText = "Spin Control On",
            toggleOffText = "Spin Control Off",
            onClick = { onEvent(MainScreenEvent.ToggleSpinControl) }
        )

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azRailToggle(
                id = "bank",
                isChecked = uiState.isBankingMode,
                toggleOnText = "Ghost Ball Aiming",
                toggleOffText = "Calculate Bank",
                onClick = { onEvent(MainScreenEvent.ToggleBankingMode) }
            )

            azRailItem(
                id = "add_obstacle",
                text = "Add Obstacle",
                onClick = { onEvent(MainScreenEvent.AddObstacleBall) }
            )
        }

        val lockResetText = when {
            uiState.experienceMode == ExperienceMode.BEGINNER && uiState.isBeginnerViewLocked -> "Unlock View"
            uiState.experienceMode == ExperienceMode.BEGINNER && !uiState.isBeginnerViewLocked -> "Lock View"
            else -> "Reset"
        }

        azRailItem(
            id = "reset",
            text = lockResetText,
            onClick = {
                val event = when {
                    uiState.experienceMode == ExperienceMode.BEGINNER && uiState.isBeginnerViewLocked -> MainScreenEvent.UnlockBeginnerView
                    uiState.experienceMode == ExperienceMode.BEGINNER && !uiState.isBeginnerViewLocked -> MainScreenEvent.LockBeginnerView
                    else -> MainScreenEvent.Reset
                }
                onEvent(event)
            }
        )

        azDivider()

        // --- Table & Units Section ---
        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azRailItem(
                id = "align",
                text = "Table Alignment",
                onClick = { onEvent(MainScreenEvent.ToggleQuickAlignScreen) }
            )
            azRailItem(
                id = "size",
                text = "Table Size",
                onClick = { onEvent(MainScreenEvent.ToggleTableSizeDialog) }
            )
            azRailItem(
                id = "units",
                text = if (uiState.distanceUnit == DistanceUnit.METRIC) "Use Imperial Units" else "Use Metric Units",
                onClick = { onEvent(MainScreenEvent.ToggleDistanceUnit) }
            )
            azDivider()
        }

        // --- Appearance Section ---
        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azRailToggle(
                id = "cam",
                isChecked = uiState.isCameraVisible,
                toggleOnText = "Turn Camera Off",
                toggleOffText = "Turn Camera On",
                onClick = { onEvent(MainScreenEvent.ToggleCamera) }
            )
        }

        azRailItem(
            id = "orientation",
            text = when (uiState.pendingOrientationLock ?: uiState.orientationLock) {
                CueDetatState.OrientationLock.AUTOMATIC -> "Orientation: Auto"
                CueDetatState.OrientationLock.PORTRAIT -> "Orientation: Portrait"
                CueDetatState.OrientationLock.LANDSCAPE -> "Orientation: Landscape"
            },
            onClick = { onEvent(MainScreenEvent.ToggleOrientationLock) }
        )

        azRailItem(
            id = "luminance",
            text = "Luminance",
            onClick = { onEvent(MainScreenEvent.ToggleLuminanceDialog) }
        )

        azRailItem(
            id = "glow",
            text = "Glow Stick",
            onClick = { onEvent(MainScreenEvent.ToggleGlowStickDialog) }
        )

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azRailItem(
                id = "advanced",
                text = "Too Advanced Options",
                onClick = { onEvent(MainScreenEvent.ToggleAdvancedOptionsDialog) }
            )
        }

        azDivider()

        // --- Meta/App Info Section ---
        azRailItem(
            id = "mode",
            text = "Mode: ${
                uiState.experienceMode?.name?.lowercase()
                    ?.replaceFirstChar { it.titlecase() }
            }",
            onClick = { onEvent(MainScreenEvent.ToggleExperienceModeSelection) }
        )

        azRailItem(
            id = "about",
            text = "About",
            onClick = { onEvent(MainScreenEvent.ViewAboutPage) }
        )

        azRailItem(
            id = "feedback",
            text = "Feedback",
            onClick = { onEvent(MainScreenEvent.SendFeedback) }
        )

        // --- Background Layer: main screen content ---
        background(weight = 0) {
            content()
        }
    }
}
