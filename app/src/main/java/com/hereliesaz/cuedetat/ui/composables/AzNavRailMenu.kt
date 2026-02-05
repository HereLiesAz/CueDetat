package com.hereliesaz.cuedetat.ui.composables

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.hereliesaz.aznavrail.AzNavRail
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.DistanceUnit

/**
 * The main navigation and control rail for the application.
 *
 * Uses the external `AzNavRail` library to provide a side menu with toggles and actions.
 * It adapts its content based on the current [ExperienceMode] (e.g., hiding advanced options for beginners).
 *
 * @param uiState The current state of the application.
 * @param onEvent Callback to dispatch events when menu items are clicked.
 */
@Composable
fun AzNavRailMenu(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
) {
    // Get local context for launching Intents (browser, email).
    val context = LocalContext.current
    // Retrieve app name dynamically.
    val appName: String = context.packageManager.getApplicationLabel(context.applicationInfo).toString()
    // Construct version string with optional update info.
    val versionInfo = "v1.0" + (uiState.latestVersionName?.let { " (latest: $it)" } ?: "")

    // Pre-resolve strings to avoid calling @Composable in non-composable DSL blocks if needed,
    // though AzNavRail DSL might handle it, it's safer here.
    val strAppName = stringResource(id = R.string.app_name)
    val strHideHelpers = stringResource(R.string.hide_helpers)
    val strShowHelpers = stringResource(R.string.show_helpers)

    // Render the Navigation Rail.
    AzNavRail(
        navController = null, // We handle navigation manually via events, not NavController.
        currentDestination = null,
        isLandscape = false // Force portrait layout logic (vertical rail).
    ) {
        // Configure global settings for the rail.
        azSettings(
            displayAppNameInHeader = true,
            headerIconShape = AzHeaderIconShape.CIRCLE
        )

        // --- Header Section ---
        azMenuItem(
            id = "app_name",
            text = strAppName,
            route = "header",
            disabled = true // Header is not clickable.
        )
        azMenuItem(
            id = "version",
            text = versionInfo,
            route = "version",
            disabled = true // Version info is informational only.
        )

        azDivider()

        // --- Core Controls Section ---

        // Toggle: Show/Hide Helper Labels.
        azRailToggle(
            id = "help",
            isChecked = uiState.areHelpersVisible,
            toggleOnText = strHideHelpers,
            toggleOffText = strShowHelpers,
            route = "help",
            onClick = { onEvent(MainScreenEvent.ToggleHelp) }
        )

        // Action: Start the tutorial overlay.
        azMenuItem(
            id = "tutorial",
            text = "Show Tutorial",
            route = "tutorial",
            onClick = { onEvent(MainScreenEvent.StartTutorial) }
        )

        // Toggle: Show/Hide Spin Control widget.
        azRailToggle(
            id = "spin",
            isChecked = uiState.isSpinControlVisible,
            toggleOnText = "Spin Control On",
            toggleOffText = "Spin Control Off",
            route = "spin",
            onClick = { onEvent(MainScreenEvent.ToggleSpinControl) }
        )

        // Expert-only features.
        if (uiState.experienceMode != ExperienceMode.BEGINNER) {
             // Toggle: Banking Mode (Ghost Ball vs Bank Calculation).
             azRailToggle(
                id = "bank",
                isChecked = uiState.isBankingMode,
                toggleOnText = "Ghost Ball Aiming",
                toggleOffText = "Calculate Bank",
                route = "bank",
                onClick = { onEvent(MainScreenEvent.ToggleBankingMode) }
            )

             // Action: Add obstacle ball to table.
             azRailItem(
                id = "add_obstacle",
                text = "Add Obstacle",
                route = "add_obstacle",
                onClick = { onEvent(MainScreenEvent.AddObstacleBall) }
            )
        }

        // Determine text for the Reset/Lock button based on context.
        val lockResetText = when {
            uiState.experienceMode == ExperienceMode.BEGINNER && uiState.isBeginnerViewLocked -> "Unlock View"
            uiState.experienceMode == ExperienceMode.BEGINNER && !uiState.isBeginnerViewLocked -> "Lock View"
            else -> "Reset"
        }

        // Action: Reset State or Lock/Unlock View.
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

        // --- Table & Units Section ---
        if (uiState.experienceMode != ExperienceMode.BEGINNER) {
            // Action: Open Alignment Screen.
            azMenuItem(
                id = "align",
                text = "Table Alignment",
                route = "align",
                onClick = { onEvent(MainScreenEvent.ToggleQuickAlignScreen) }
            )
            // Action: Open Table Size Dialog.
            azMenuItem(
                id = "size",
                text = "Table Size",
                route = "size",
                onClick = { onEvent(MainScreenEvent.ToggleTableSizeDialog) }
            )
            // Toggle: Metric/Imperial Units.
            azMenuItem(
                id = "units",
                text = if (uiState.distanceUnit == DistanceUnit.METRIC) "Use Imperial Units" else "Use Metric Units",
                route = "units",
                onClick = { onEvent(MainScreenEvent.ToggleDistanceUnit) }
            )
            azDivider()
        }

        // --- Appearance Section ---
        if (uiState.experienceMode != ExperienceMode.BEGINNER) {
            // Toggle: Camera visibility.
            azRailToggle(
                id = "cam",
                isChecked = uiState.isCameraVisible,
                toggleOnText = "Turn Camera Off",
                toggleOffText = "Turn Camera On",
                route = "cam",
                onClick = { onEvent(MainScreenEvent.ToggleCamera) }
            )
        }

        // Toggle: Orientation Lock.
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

        // Action: Open Luminance Dialog.
        azMenuItem(
            id = "luminance",
            text = "Luminance",
            route = "luminance",
            onClick = { onEvent(MainScreenEvent.ToggleLuminanceDialog) }
        )

        // Action: Open Advanced (Developer) Options.
        if (uiState.experienceMode != ExperienceMode.BEGINNER) {
            azMenuItem(
                id = "advanced",
                text = "Too Advanced Options",
                route = "advanced",
                onClick = { onEvent(MainScreenEvent.ToggleAdvancedOptionsDialog) }
            )
        }

        azDivider()

        // --- Meta/App Info Section ---
         azMenuItem(
            id = "mode",
            text = "Mode: ${
                uiState.experienceMode?.name?.lowercase()
                    ?.replaceFirstChar { it.titlecase() }
            }",
            route = "mode",
            onClick = { onEvent(MainScreenEvent.ToggleExperienceModeSelection) }
        )

        // Action: Open GitHub.
        azMenuItem(
            id = "about",
            text = "About",
            route = "about",
            onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://github.com/hereliesaz/$appName".toUri()
                )
                context.startActivity(intent)
            }
        )

        // Action: Send Feedback Email.
        azMenuItem(
            id = "feedback",
            text = "Feedback",
            route = "feedback",
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = "mailto:hereliesaz@gmail.com".toUri()
                    putExtra(Intent.EXTRA_SUBJECT, "$appName - Feedback")
                }
                context.startActivity(intent)
            }
        )
    }
}
