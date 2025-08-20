package com.hereliesaz.cuedetat.ui.composables

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.hereliesaz.aznavrail.model.NavItem
import com.hereliesaz.aznavrail.model.NavItemData
import com.hereliesaz.aznavrail.model.NavRailHeader
import com.hereliesaz.aznavrail.model.NavRailMenuSection
import com.hereliesaz.aznavrail.model.PredefinedAction
import com.hereliesaz.aznavrail.ui.AzNavRail
import com.hereliesaz.cuedetat.BuildConfig
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
    val context = LocalContext.current
    val appName: String = context.packageManager.getApplicationLabel(context.applicationInfo).toString() // appName is still defined but not used in AzNavRail call

    AzNavRail(
        useAppIconAsHeader = true,
        header = NavRailHeader { /* ... */ },
        menuSections = createMenuSections(uiState, onEvent),
        showDefaultPredefinedItems = false,
        onPredefinedAction = { action ->
            when (action) {
                PredefinedAction.ABOUT -> {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/hereliesaz/$appName")
                    )
                    context.startActivity(intent)
                }
                PredefinedAction.FEEDBACK -> {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:hereliesaz@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "$appName - Feedback")
                    }
                    context.startActivity(intent)
                }
                else -> {}
            }
        }
    )
}

@Composable
private fun createMenuSections(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit
): List<NavRailMenuSection> {
    val versionInfo =
        "v${BuildConfig.VERSION_NAME}" + (uiState.latestVersionName?.let { " (latest: $it)" } ?: "")

    return listOf(
        NavRailMenuSection(
            title = stringResource(id = R.string.app_name),
            items = listOf(
                NavItem(
                    text = versionInfo,
                    data = NavItemData.Action(onClick = {})
                )
            )
        ),
        NavRailMenuSection(
            title = "Core Controls",
            items = listOf(
                NavItem(
                    text = stringResource(if (uiState.areHelpersVisible) R.string.hide_helpers else R.string.show_helpers),
                    data = NavItemData.Action(onClick = { onEvent(MainScreenEvent.ToggleHelp) }),
                    showOnRail = true,
                    railButtonText = "Help"
                ),
                NavItem(
                    text = "Show Tutorial",
                    data = NavItemData.Action(onClick = { onEvent(MainScreenEvent.StartTutorial) })
                ),
                NavItem(
                    text = "Spin",
                    data = NavItemData.Action(onClick = { onEvent(MainScreenEvent.ToggleSpinControl) }),
                    showOnRail = true
                ),
                NavItem(
                    text = if (uiState.isBankingMode) "Ghost Ball Aiming" else "Calculate Bank",
                    data = NavItemData.Action(onClick = { onEvent(MainScreenEvent.ToggleBankingMode) }),
                    showOnRail = uiState.experienceMode != ExperienceMode.BEGINNER,
                    railButtonText = "Bank"
                ),
                NavItem(
                    text = "Add Obstacle",
                    data = NavItemData.Action(onClick = { onEvent(MainScreenEvent.AddObstacleBall) }),
                    showOnRail = uiState.experienceMode != ExperienceMode.BEGINNER,
                    railButtonText = "Add"
                ),
                NavItem(
                    text = when {
                        uiState.experienceMode == ExperienceMode.BEGINNER && uiState.isBeginnerViewLocked -> "Unlock View"
                        uiState.experienceMode == ExperienceMode.BEGINNER && !uiState.isBeginnerViewLocked -> "Lock View"
                        else -> "Reset"
                    },
                    data = NavItemData.Action(onClick = {
                        val event = when {
                            uiState.experienceMode == ExperienceMode.BEGINNER && uiState.isBeginnerViewLocked -> MainScreenEvent.UnlockBeginnerView
                            uiState.experienceMode == ExperienceMode.BEGINNER && !uiState.isBeginnerViewLocked -> MainScreenEvent.LockBeginnerView
                            else -> MainScreenEvent.Reset
                        }
                        onEvent(event)
                    }),
                    showOnRail = true,
                    railButtonText = when {
                        uiState.experienceMode == ExperienceMode.BEGINNER && uiState.isBeginnerViewLocked -> "Unlock"
                        uiState.experienceMode == ExperienceMode.BEGINNER && !uiState.isBeginnerViewLocked -> "Lock"
                        else -> "Reset"
                    }
                ),
            )
        ),
        NavRailMenuSection(
            title = "Table & Units",
            items = listOf(
                NavItem(
                    text = "Table Alignment",
                    data = NavItemData.Action(onClick = { onEvent(MainScreenEvent.ToggleQuickAlignScreen) }),
                    enabled = uiState.experienceMode != ExperienceMode.BEGINNER
                ),
                NavItem(
                    text = "Table Size",
                    data = NavItemData.Action(onClick = { onEvent(MainScreenEvent.ToggleTableSizeDialog) }),
                    enabled = uiState.experienceMode != ExperienceMode.BEGINNER
                ),
                NavItem(
                    text = if (uiState.distanceUnit == DistanceUnit.METRIC) "Use Imperial Units" else "Use Metric Units",
                    data = NavItemData.Action(onClick = { onEvent(MainScreenEvent.ToggleDistanceUnit) }),
                    enabled = uiState.experienceMode != ExperienceMode.BEGINNER
                ),
            )
        ),
        NavRailMenuSection(
            title = "Appearance",
            items = listOf(
                NavItem(
                    text = if (uiState.isCameraVisible) "Turn Camera Off" else "Turn Camera On",
                    data = NavItemData.Action(onClick = { onEvent(MainScreenEvent.ToggleCamera) }),
                    showOnRail = uiState.experienceMode != ExperienceMode.BEGINNER,
                    railButtonText = "Cam"
                ),
                NavItem(
                    text = when (uiState.pendingOrientationLock ?: uiState.orientationLock) {
                        CueDetatState.OrientationLock.AUTOMATIC -> "Orientation: Auto"
                        CueDetatState.OrientationLock.PORTRAIT -> "Orientation: Portrait"
                        CueDetatState.OrientationLock.LANDSCAPE -> "Orientation: Landscape"
                    },
                    data = NavItemData.Action(onClick = { onEvent(MainScreenEvent.ToggleOrientationLock) })
                ),
                NavItem(
                    text = "Luminance",
                    data = NavItemData.Action(onClick = { onEvent(MainScreenEvent.ToggleLuminanceDialog) })
                ),
                NavItem(
                    text = "Too Advanced Options",
                    data = NavItemData.Action(onClick = { onEvent(MainScreenEvent.ToggleAdvancedOptionsDialog) }),
                    enabled = uiState.experienceMode != ExperienceMode.BEGINNER
                ),
            )
        ),
        NavRailMenuSection(
            title = "Meta",
            items = listOf(
                NavItem(
                    text = "Mode: ${
                        uiState.experienceMode?.name?.lowercase()
                            ?.replaceFirstChar { it.titlecase() }
                    }",
                    data = NavItemData.Action(onClick = { onEvent(MainScreenEvent.ToggleExperienceModeSelection) })
                ),
                NavItem(
                    text = "About",
                    data = NavItemData.Action(onClick = { onEvent(MainScreenEvent.ViewAboutPage) })
                ),
                NavItem(
                    text = "Feedback",
                    data = NavItemData.Action(onClick = { onEvent(MainScreenEvent.SendFeedback) })
                )
            )
        )
    )
}
