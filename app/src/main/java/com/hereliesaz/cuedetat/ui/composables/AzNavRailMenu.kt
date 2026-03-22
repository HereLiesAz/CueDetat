// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/AzNavRailMenu.kt

package com.hereliesaz.cuedetat.ui.composables

import android.content.res.Configuration
import android.graphics.PointF
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavHostController
import com.hereliesaz.aznavrail.AzHostActivityLayout
import com.hereliesaz.aznavrail.AzNavHostScope
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.cuedetat.domain.CameraMode
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.DistanceUnit

@Composable
fun AzNavRailMenu(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    navController: NavHostController,
    currentDestination: String?,
    hasTableModel: Boolean = false,
    content: AzNavHostScope.() -> Unit = {},
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isMasseVisible by remember { mutableStateOf(false) }
    var masseElevation by remember { mutableStateOf(0f) }
    var masseImpact by remember { mutableStateOf<PointF?>(null) }

    val translucentBlack = Color(0x80000000)

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
            activeColor = Color.White,
            headerIconShape = if (uiState.areHelpersVisible) AzHeaderIconShape.NONE else AzHeaderIconShape.CIRCLE
        )

        azAdvanced(
            isLoading = false,
            enableRailDragging = false,
            helpEnabled = true,
            onDismissHelp = {}
        )

        if (uiState.experienceMode == ExperienceMode.HATER) {
            azRailItem(
                id = "shake",
                text = "Shake",
                textColor = Color(0xFFFFEB3B), // 1-Ball Yellow
                fillColor = translucentBlack,
                onClick = { onEvent(MainScreenEvent.Shake) }
            )
            azRailItem(
                id = "exit",
                text = "Exit",
                textColor = Color(0xFF2196F3), // 2-Ball Blue
                fillColor = translucentBlack,
                onClick = { onEvent(MainScreenEvent.ExitToSplash) }
            )
            content()
            return@AzHostActivityLayout
        }

        // --- Core Controls ---

        azRailToggle(
            id = "help",
            isChecked = uiState.areHelpersVisible,
            toggleOnText = "Help",
            toggleOffText = "Help",
            textColor = Color(0xFFFFEB3B), // 1-Ball Yellow
            fillColor = translucentBlack,
            onClick = { onEvent(MainScreenEvent.ToggleHelp) }
        )

        azMenuItem(
            id = "tutorial",
            text = "Tutorial",
            textColor = Color(0xFF2196F3), // 2-Ball Blue
            fillColor = translucentBlack,
            onClick = { onEvent(MainScreenEvent.StartTutorial) }
        )

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azRailCycler(
                id = "cam",
                options = listOf("Off", "Cam", "ar"),
                selectedOption = when (uiState.cameraMode) {
                    CameraMode.OFF -> "Off"
                    CameraMode.CAMERA -> "Cam"
                    CameraMode.AR -> "ar"
                },
                textColor = Color(0xFFF44336), // 3-Ball Red
                fillColor = translucentBlack,
                onClick = { onEvent(MainScreenEvent.CycleCameraMode) }
            )
        }
        azDivider()

        if (uiState.experienceMode != ExperienceMode.BEGINNER) {
            azRailToggle(
                id = "spin",
                isChecked = uiState.isSpinControlVisible,
                toggleOnText = "Spin",
                toggleOffText = "Spin",
                textColor = Color(0xFF9C27B0), // 4-Ball Purple
                fillColor = translucentBlack,
                onClick = { onEvent(MainScreenEvent.ToggleSpinControl) }
            )

            azRailToggle(
                id = "masse",
                isChecked = isMasseVisible,
                toggleOnText = "Massé",
                toggleOffText = "Massé",
                textColor = Color(0xFFFF9800), // 5-Ball Orange
                fillColor = translucentBlack,
                onClick = {
                    isMasseVisible = !isMasseVisible
                    if (!isMasseVisible) {
                        masseImpact = null
                        masseElevation = 0f
                    }
                }
            )
        }

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azRailToggle(
                id = "bank",
                isChecked = uiState.isBankingMode,
                toggleOnText = "Aim",
                toggleOffText = "Bank",
                textColor = Color(0xFF4CAF50), // 6-Ball Green
                fillColor = translucentBlack,
                onClick = { onEvent(MainScreenEvent.ToggleBankingMode) }
            )

            azRailItem(
                id = "add_obstacle",
                text = "Add",
                info = "Add Obstacle Ball",
                textColor = Color(0xFFE91E63), // 7-Ball Maroon
                fillColor = translucentBlack,
                onClick = { onEvent(MainScreenEvent.AddObstacleBall) }
            )
        }

        if (uiState.experienceMode == ExperienceMode.BEGINNER) {
            azRailItem(
                id = "static",
                text = "Static",
                textColor = Color(0xFF4CAF50), // 6-Ball Green
                fillColor = translucentBlack,
                onClick = { onEvent(MainScreenEvent.LockBeginnerView) }
            )
            azRailItem(
                id = "dynamic",
                text = "Dynamic",
                textColor = Color(0xFFE91E63), // 7-Ball Maroon
                fillColor = translucentBlack,
                onClick = { onEvent(MainScreenEvent.UnlockBeginnerView) }
            )
        } else {
            azRailItem(
                id = "reset",
                text = "Reset",
                textColor = Color(0xFF00BCD4), // Cyan substitute for 8-ball
                fillColor = translucentBlack,
                onClick = { onEvent(MainScreenEvent.Reset) }
            )
        }

        azDivider()

        // --- Appearance ---

        azMenuItem(
            id = "luminance",
            text = "Luminance",
            textColor = Color(0xFFFFF59D), // 9-Ball Yellow Stripe
            fillColor = translucentBlack,
            onClick = { onEvent(MainScreenEvent.ToggleLuminanceDialog) }
        )

        azMenuItem(
            id = "glow",
            text = "Glow Stick",
            textColor = Color(0xFF64B5F6), // 10-Ball Blue Stripe
            fillColor = translucentBlack,
            onClick = { onEvent(MainScreenEvent.ToggleGlowStickDialog) }
        )

        // --- Table & Units ---

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azMenuItem(
                id = "scan",
                text = "Scan Table",
                textColor = Color(0xFFE57373), // 11-Ball Red Stripe
                fillColor = translucentBlack,
                onClick = { onEvent(MainScreenEvent.ToggleTableScanScreen) }
            )

            if (hasTableModel) {
                azMenuItem(
                    id = "rescan",
                    text = "Rescan Table",
                    textColor = Color(0xFFBA68C8), // 12-Ball Purple Stripe
                    fillColor = translucentBlack,
                    onClick = {
                        onEvent(MainScreenEvent.ClearTableScan)
                        onEvent(MainScreenEvent.ToggleTableScanScreen)
                    }
                )
            }

            azMenuItem(
                id = "size",
                text = "Table Size",
                textColor = Color(0xFFFFB74D), // 13-Ball Orange Stripe
                fillColor = translucentBlack,
                onClick = { onEvent(MainScreenEvent.ToggleTableSizeDialog) }
            )

            azMenuItem(
                id = "units",
                text = if (uiState.distanceUnit == DistanceUnit.METRIC) "Use Imperial Units" else "Use Metric Units",
                textColor = Color(0xFF81C784), // 14-Ball Green Stripe
                fillColor = translucentBlack,
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
            textColor = Color(0xFFF06292), // 15-Ball Maroon Stripe
            fillColor = translucentBlack,
            onClick = { onEvent(MainScreenEvent.ToggleOrientationLock) }
        )

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azMenuItem(
                id = "advanced",
                text = "Too Advanced Options",
                textColor = Color(0xFFFFEB3B), // Back to 1-Ball Yellow
                fillColor = translucentBlack,
                onClick = { onEvent(MainScreenEvent.ToggleAdvancedOptionsDialog) }
            )
        }

        azDivider()

        // --- Meta ---

        azMenuItem(
            id = "mode",
            text = "Mode: ${
                uiState.experienceMode?.name?.lowercase()
                    ?.replaceFirstChar { it.titlecase() }
            }",
            textColor = Color(0xFF2196F3), // Back to 2-Ball Blue
            fillColor = translucentBlack,
            onClick = { onEvent(MainScreenEvent.ToggleExperienceModeSelection) }
        )

        onscreen(alignment = Alignment.CenterStart) {
            if (isMasseVisible) {
                MasseControl(
                    elevationAngle = masseElevation,
                    impactOffset = masseImpact,
                    onElevationChanged = { masseElevation = it },
                    onImpactChanged = { masseImpact = it },
                    onImpactEnded = { }
                )
            }
        }

        content()
    }
}