package com.hereliesaz.cuedetat.ui.composables

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.hereliesaz.aznavrail.AzHostActivityLayout
import com.hereliesaz.aznavrail.AzNavHostScope
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
 * The [content] lambda receives an [AzNavHostScope], allowing callers to add
 * [AzNavHostScope.background] layers (full-screen) and [AzNavHostScope.onscreen] elements
 * (safe-area HUD) in addition to any extra rail items.
 *
 * @param uiState The current state of the application.
 * @param onEvent Callback to dispatch events when menu items are clicked.
 * @param navController The NavHostController used for screen routing.
 * @param currentDestination The current back-stack route, used to highlight the active item.
 * @param content DSL block for adding background layers and onscreen HUD elements.
 */
@Composable
fun AzNavRailMenu(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    navController: NavHostController,
    currentDestination: String?,
    content: AzNavHostScope.() -> Unit = {},
) {
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
            packButtons = false,
            showFooter = true
        )

        azTheme(
            defaultShape = AzButtonShape.CIRCLE,
            activeColor = activeColor,
            headerIconShape = if (uiState.areHelpersVisible) AzHeaderIconShape.NONE else AzHeaderIconShape.CIRCLE
        )

        azAdvanced(
            isLoading = false,
            enableRailDragging = false,
            helpEnabled = false,
            onDismissHelp = {}
        )

        // --- Core Controls Section ---

        azRailToggle(
            id = "help",
            isChecked = uiState.areHelpersVisible,
            toggleOnText = strHideHelpers,
            toggleOffText = strShowHelpers,
            onClick = { onEvent(MainScreenEvent.ToggleHelp) }
        )

        azMenuItem(
            id = "tutorial",
            text = "Tutorial",
            onClick = { onEvent(MainScreenEvent.StartTutorial) }
        )

        azRailToggle(
            id = "spin",
            isChecked = uiState.isSpinControlVisible,
            toggleOnText = "Spin",
            toggleOffText = "Spin",
            onClick = { onEvent(MainScreenEvent.ToggleSpinControl) }
        )

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azRailToggle(
                id = "bank",
                isChecked = uiState.isBankingMode,
                toggleOnText = "Aim",
                toggleOffText = "Bank",
                onClick = { onEvent(MainScreenEvent.ToggleBankingMode) }
            )

            azRailItem(
                id = "add_obstacle",
                text = "Add",
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
        azMenuItem(
                id = "luminance",
        text = "Luminance",
        onClick = { onEvent(MainScreenEvent.ToggleLuminanceDialog) }
        )

        azMenuItem(
            id = "glow",
            text = "Glow Stick",
            onClick = { onEvent(MainScreenEvent.ToggleGlowStickDialog) }
        )
        // --- Table & Units Section ---
        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azMenuItem(
                id = "align",
                text = "Table Alignment",
                onClick = { onEvent(MainScreenEvent.ToggleQuickAlignScreen) }
            )
            azMenuItem(
                id = "size",
                text = "Table Size",
                onClick = { onEvent(MainScreenEvent.ToggleTableSizeDialog) }
            )
            azMenuItem(
                id = "units",
                text = if (uiState.distanceUnit == DistanceUnit.METRIC) "Use Imperial Units" else "Use Metric Units",
                onClick = { onEvent(MainScreenEvent.ToggleDistanceUnit) }
            )
            azDivider()
        }



        azMenuItem(
            id = "orientation",
            text = when (uiState.pendingOrientationLock ?: uiState.orientationLock) {
                CueDetatState.OrientationLock.AUTOMATIC -> "Orientation: Auto"
                CueDetatState.OrientationLock.PORTRAIT -> "Orientation: Portrait"
                CueDetatState.OrientationLock.LANDSCAPE -> "Orientation: Landscape"
            },
            onClick = { onEvent(MainScreenEvent.ToggleOrientationLock) }
        )



        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azMenuItem(
                id = "advanced",
                text = "Too Advanced Options",
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
            onClick = { onEvent(MainScreenEvent.ToggleExperienceModeSelection) }
        )

        // Caller-provided backgrounds and onscreen elements
        content()
    }
}
