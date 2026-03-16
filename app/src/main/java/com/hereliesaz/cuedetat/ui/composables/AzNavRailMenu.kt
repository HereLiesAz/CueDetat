package com.hereliesaz.cuedetat.ui.composables

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.hereliesaz.aznavrail.AzHostActivityLayout
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDockingSide
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
    val context = LocalContext.current
    val appName: String = context.packageManager.getApplicationLabel(context.applicationInfo).toString()
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
            dockingSide = AzDockingSide.LEFT
        )

        azTheme(
            defaultShape = AzButtonShape.RECTANGLE,
            activeColor = activeColor
        )

        azAdvanced(
            isLoading = false,
            enableRailDragging = true,
            helpEnabled = false,
            onDismissHelp = {}
        )

        // --- Header Section ---
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

        // --- Core Controls Section ---

        azRailToggle(
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

        // --- Table & Units Section ---
        if (uiState.experienceMode != ExperienceMode.BEGINNER) {
            // Navigates to the Quick Align screen via navController routing.
            azMenuItem(
                id = "align",
                text = "Table Alignment",
                route = "align",
                onClick = {
                    if (navController.currentBackStackEntry?.destination?.route != "align") {
                        navController.navigate("align") { launchSingleTop = true }
                        onEvent(MainScreenEvent.ToggleQuickAlignScreen)
                    }
                }
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

        // --- Appearance Section ---
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

        // --- Background Layer: main screen content ---
        background(weight = 0) {
            content()
        }
    }
}
