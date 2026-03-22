package com.hereliesaz.cuedetat.ui.composables

import android.content.res.Configuration
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavHostController
import com.hereliesaz.aznavrail.AzHostActivityLayout
import com.hereliesaz.aznavrail.AzNavHostScope
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment
import com.hereliesaz.cuedetat.domain.CameraMode
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.DistanceUnit

/**
 * AZNAVRAIL Menu for CueDetat.
 * * Implements strict mode isolation:
 * - Expert features (Scanning/Geometry) are gated by ExperienceMode and felt-locking.
 * - Rail item colors are mapped to the standard 1-15 billiard ball sequence.
 * - Horizontal nested rail handles Masse interaction logic.
 */
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

    // Billiard Ball Palette for Rail Items
    val ball1Yellow = Color(0xFFFFEB3B)
    val ball2Blue = Color(0xFF2196F3)
    val ball3Red = Color(0xFFF44336)
    val ball4Purple = Color(0xFF9C27B0)
    val ball5Orange = Color(0xFFFF9800)
    val ball6Green = Color(0xFF4CAF50)
    val ball7Maroon = Color(0xFFE91E63)
    val ball8Black = Color(0xFF212121)
    val ball9YellowStripe = Color(0xFFFFF59D)
    val ball10BlueStripe = Color(0xFF64B5F6)
    val ball11RedStripe = Color(0xFFE57373)
    val ball12PurpleStripe = Color(0xFFBA68C8)
    val ball13OrangeStripe = Color(0xFFFFB74D)
    val ball14GreenStripe = Color(0xFF81C784)
    val ball15MaroonStripe = Color(0xFFF06292)

    AzHostActivityLayout(
        navController = navController,
        modifier = Modifier,
        currentDestination = currentDestination,
        isLandscape = isLandscape,
        initiallyExpanded = false
    ) {
        // --- Trajectory Rendering (Background Layer) ---
        background(weight = 0) {
            val cueBallPos = uiState.onPlaneBall?.center
            if (cueBallPos != null && !uiState.spinPaths.isNullOrEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    uiState.spinPaths.forEach { (color, points) ->
                        if (points.size > 1) {
                            val path = Path().apply {
                                moveTo(cueBallPos.x + points[0].x, cueBallPos.y + points[0].y)
                                for (i in 1 until points.size) {
                                    lineTo(cueBallPos.x + points[i].x, cueBallPos.y + points[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = color.copy(alpha = uiState.spinPathsAlpha),
                                style = Stroke(width = 3f)
                            )
                        }
                    }
                }
            }
        }

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

        // --- Hater Mode: A monochromatic prison of utility ---
        if (uiState.experienceMode == ExperienceMode.HATER) {
            azRailItem(
                id = "shake",
                text = "Shake",
                fillColor = ball1Yellow,
                textColor = Color.Black,
                onClick = { onEvent(MainScreenEvent.Shake) }
            )
            azRailItem(
                id = "exit",
                text = "Exit",
                fillColor = ball2Blue,
                textColor = Color.White,
                onClick = { onEvent(MainScreenEvent.ExitToSplash) }
            )
            content()
            return@AzHostActivityLayout
        }

        // --- Core Navigation ---
        azRailToggle(
            id = "help",
            isChecked = uiState.areHelpersVisible,
            toggleOnText = "Help",
            toggleOffText = "Help",
            fillColor = ball1Yellow,
            textColor = Color.Black,
            onClick = { onEvent(MainScreenEvent.ToggleHelp) }
        )

        azMenuItem(
            id = "tutorial",
            text = "Tutorial",
            fillColor = ball2Blue,
            textColor = Color.White,
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
                fillColor = ball3Red,
                textColor = Color.White,
                onClick = { onEvent(MainScreenEvent.CycleCameraMode) }
            )
        }
        azDivider()

        // Spin and Masse Isolation
        if (uiState.experienceMode != ExperienceMode.BEGINNER) {
            azRailToggle(
                id = "spin",
                isChecked = uiState.isSpinControlVisible,
                toggleOnText = "Spin",
                toggleOffText = "Spin",
                fillColor = ball4Purple,
                textColor = Color.White,
                onClick = { onEvent(MainScreenEvent.ToggleSpinControl) }
            )

            azNestedRail(
                id = "masse_rail",
                text = "Massé",
                alignment = AzNestedRailAlignment.HORIZONTAL
            ) {
                azRailToggle(
                    id = "masse_toggle",
                    isChecked = uiState.isMasseModeActive,
                    toggleOnText = "Done",
                    toggleOffText = "Set",
                    fillColor = ball5Orange,
                    textColor = Color.Black,
                    onClick = { onEvent(MainScreenEvent.ToggleMasseMode) }
                )
            }
        }

        // Action controls
        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azRailToggle(
                id = "bank",
                isChecked = uiState.isBankingMode,
                toggleOnText = "Aim",
                toggleOffText = "Bank",
                fillColor = ball6Green,
                textColor = Color.White,
                onClick = { onEvent(MainScreenEvent.ToggleBankingMode) }
            )

            azRailItem(
                id = "add_obstacle",
                text = "Add",
                fillColor = ball7Maroon,
                textColor = Color.White,
                onClick = { onEvent(MainScreenEvent.AddObstacleBall) }
            )
        }

        if (uiState.experienceMode == ExperienceMode.BEGINNER) {
            azRailItem(
                id = "static",
                text = "Static",
                fillColor = ball6Green,
                textColor = Color.White,
                onClick = { onEvent(MainScreenEvent.LockBeginnerView) }
            )
            azRailItem(
                id = "dynamic",
                text = "Dynamic",
                fillColor = ball7Maroon,
                textColor = Color.White,
                onClick = { onEvent(MainScreenEvent.UnlockBeginnerView) }
            )
        } else {
            azRailItem(
                id = "reset",
                text = "Reset",
                fillColor = ball8Black,
                textColor = Color.White,
                onClick = { onEvent(MainScreenEvent.Reset) }
            )
        }

        azDivider()

        // --- Expert Alignment (Gated by Prerequisite) ---
        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            if (uiState.lockedHsvColor != null) {
                azMenuItem(
                    id = "scan",
                    text = "Scan Table",
                    fillColor = ball11RedStripe,
                    textColor = Color.Black,
                    onClick = { onEvent(MainScreenEvent.ToggleTableScanScreen) }
                )

                if (hasTableModel) {
                    azMenuItem(
                        id = "rescan",
                        text = "Rescan Table",
                        fillColor = ball12PurpleStripe,
                        textColor = Color.White,
                        onClick = {
                            onEvent(MainScreenEvent.ClearTableScan)
                            onEvent(MainScreenEvent.ToggleTableScanScreen)
                        }
                    )
                }
            }

            azMenuItem(
                id = "luminance",
                text = "Luminance",
                fillColor = ball9YellowStripe,
                textColor = Color.Black,
                onClick = { onEvent(MainScreenEvent.ToggleLuminanceDialog) }
            )

            azMenuItem(
                id = "glow",
                text = "Glow Stick",
                fillColor = ball10BlueStripe,
                textColor = Color.Black,
                onClick = { onEvent(MainScreenEvent.ToggleGlowStickDialog) }
            )

            azMenuItem(
                id = "size",
                text = "Table Size",
                fillColor = ball13OrangeStripe,
                textColor = Color.Black,
                onClick = { onEvent(MainScreenEvent.ToggleTableSizeDialog) }
            )

            azMenuItem(
                id = "units",
                text = if (uiState.distanceUnit == DistanceUnit.METRIC) "Metric" else "Imperial",
                fillColor = ball14GreenStripe,
                textColor = Color.Black,
                onClick = { onEvent(MainScreenEvent.ToggleDistanceUnit) }
            )
            azDivider()
        }

        azMenuItem(
            id = "orientation",
            text = "Orientation",
            fillColor = ball15MaroonStripe,
            textColor = Color.Black,
            onClick = { onEvent(MainScreenEvent.ToggleOrientationLock) }
        )

        if (uiState.experienceMode == ExperienceMode.EXPERT) {
            azMenuItem(
                id = "advanced",
                text = "Advanced",
                fillColor = ball1Yellow,
                textColor = Color.Black,
                onClick = { onEvent(MainScreenEvent.ToggleAdvancedOptionsDialog) }
            )
        }

        azDivider()
        azMenuItem(
            id = "mode",
            text = "Mode: ${uiState.experienceMode?.name}",
            fillColor = ball2Blue,
            textColor = Color.White,
            onClick = { onEvent(MainScreenEvent.ToggleExperienceModeSelection) }
        )

        // --- Onscreen Content Layer ---
        onscreen(alignment = Alignment.CenterStart) {
            if (uiState.isMasseModeActive) {
                MasseControl(
                    elevationAngle = uiState.pitchAngle,
                    impactOffset = uiState.selectedSpinOffset,
                    onElevationChanged = { },
                    onImpactChanged = { onEvent(MainScreenEvent.SpinApplied(it)) },
                    onImpactEnded = { onEvent(MainScreenEvent.SpinSelectionEnded) }
                )
            }
        }

        content()
    }
}