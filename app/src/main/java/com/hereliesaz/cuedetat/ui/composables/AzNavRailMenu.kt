package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hereliesaz.aznavrail.AzNavRail
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.DistanceUnit

@Composable
fun AzNavRailMenu(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
) {
    val versionInfo = "v1.0" + (uiState.latestVersionName?.let { " (latest: $it)" } ?: "")

    // Pre-resolve strings to avoid calling @Composable in non-composable DSL
    val strAppName = stringResource(id = R.string.app_name)
    val strHideHelpers = stringResource(R.string.hide_helpers)
    val strShowHelpers = stringResource(R.string.show_helpers)

    AzNavRail(
        navController = null,
        currentDestination = null,
        isLandscape = false
    ) {
        azSettings(
            displayAppNameInHeader = false,
            headerIconShape = AzHeaderIconShape.CIRCLE,
            packRailButtons = true
        )

        azMenuItem(
            id = "app_name",
            text = strAppName,
            route = "header",
            disabled = true
        )
        azMenuItem(
            id = "version",
            text = versionInfo,
            route = "version",
            disabled = true
        )

        azDivider()

        // Core Controls
        // Fix: Removed railToggleOnText/railToggleOffText parameters which are not in the API
        azMenuToggle(
            id = "help",
            isChecked = uiState.areHelpersVisible,
            toggleOnText = strHideHelpers,
            toggleOffText = strShowHelpers,
            route = "help",
            onClick = { onEvent(MainScreenEvent.ToggleHelp) }
        )

        azMenuItem(
            id = "tutorial",
            text = "Show Tutorial",
            route = "tutorial",
            onClick = { onEvent(MainScreenEvent.StartTutorial) }
        )

        azRailToggle(
            id = "spin",
            isChecked = uiState.isSpinControlVisible,
            toggleOnText = "Spin Control On",
            toggleOffText = "Spin Control Off",
            route = "spin",
            onClick = { onEvent(MainScreenEvent.ToggleSpinControl) }
        )

        if (uiState.experienceMode != ExperienceMode.BEGINNER) {
             azRailToggle(
                id = "bank",
                isChecked = uiState.isBankingMode,
                toggleOnText = "Ghost Ball Aiming",
                toggleOffText = "Calculate Bank",
                route = "bank",
                onClick = { onEvent(MainScreenEvent.ToggleBankingMode) }
            )

             // Fix: Removed railText param
             azRailItem(
                id = "add_obstacle",
                text = "Add Obstacle",
                route = "add_obstacle",
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
            route = "reset",
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

        // Table & Units
        if (uiState.experienceMode != ExperienceMode.BEGINNER) {
            azMenuItem(
                id = "align",
                text = "Table Alignment",
                route = "align",
                onClick = { onEvent(MainScreenEvent.ToggleQuickAlignScreen) }
            )
            azMenuItem(
                id = "size",
                text = "Table Size",
                route = "size",
                onClick = { onEvent(MainScreenEvent.ToggleTableSizeDialog) }
            )
            azMenuItem(
                id = "units",
                text = if (uiState.distanceUnit == DistanceUnit.METRIC) "Use Imperial Units" else "Use Metric Units",
                route = "units",
                onClick = { onEvent(MainScreenEvent.ToggleDistanceUnit) }
            )
            azDivider()
        }

        // Appearance
        if (uiState.experienceMode != ExperienceMode.BEGINNER) {
            azRailToggle(
                id = "cam",
                isChecked = uiState.isCameraVisible,
                toggleOnText = "Turn Camera Off",
                toggleOffText = "Turn Camera On",
                route = "cam",
                onClick = { onEvent(MainScreenEvent.ToggleCamera) }
            )
        }

        azMenuItem(
            id = "orientation",
            text = when (uiState.pendingOrientationLock ?: uiState.orientationLock) {
                CueDetatState.OrientationLock.AUTOMATIC -> "Orientation: Auto"
                CueDetatState.OrientationLock.PORTRAIT -> "Orientation: Portrait"
                CueDetatState.OrientationLock.LANDSCAPE -> "Orientation: Landscape"
            },
            route = "orientation",
            onClick = { onEvent(MainScreenEvent.ToggleOrientationLock) }
        )

        azMenuItem(
            id = "luminance",
            text = "Luminance",
            route = "luminance",
            onClick = { onEvent(MainScreenEvent.ToggleLuminanceDialog) }
        )

        if (uiState.experienceMode != ExperienceMode.BEGINNER) {
            azMenuItem(
                id = "advanced",
                text = "Too Advanced Options",
                route = "advanced",
                onClick = { onEvent(MainScreenEvent.ToggleAdvancedOptionsDialog) }
            )
        }

        azDivider()

        // Meta
         azMenuItem(
            id = "mode",
            text = "Mode: ${
                uiState.experienceMode?.name?.lowercase()
                    ?.replaceFirstChar { it.titlecase() }
            }",
            route = "mode",
            onClick = { onEvent(MainScreenEvent.ToggleExperienceModeSelection) }
        )

        azMenuItem(
            id = "about",
            text = "About",
            route = "about",
            onClick = { onEvent(MainScreenEvent.ViewAboutPage) }
        )

        azMenuItem(
            id = "feedback",
            text = "Feedback",
            route = "feedback",
            onClick = { onEvent(MainScreenEvent.SendFeedback) }
        )
    }
}
