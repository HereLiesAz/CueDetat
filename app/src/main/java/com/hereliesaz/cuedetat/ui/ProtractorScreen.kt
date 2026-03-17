// app/src/main/java/com/hereliesaz/cuedetat/ui/ProtractorScreen.kt
package com.hereliesaz.cuedetat.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.cuedetat.data.CalibrationAnalyzer
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.ui.composables.AzNavRailMenu
import com.hereliesaz.cuedetat.ui.composables.CameraBackground
import com.hereliesaz.cuedetat.ui.composables.SpinControl
import com.hereliesaz.cuedetat.ui.composables.TopControls
import com.hereliesaz.cuedetat.ui.composables.ZoomControls
import com.hereliesaz.cuedetat.ui.composables.calibration.CalibrationScreen
import com.hereliesaz.cuedetat.ui.composables.calibration.CalibrationViewModel
import com.hereliesaz.cuedetat.ui.composables.dialogs.AdvancedOptionsDialog
import com.hereliesaz.cuedetat.ui.composables.dialogs.GlowStickDialog
import com.hereliesaz.cuedetat.ui.composables.dialogs.LuminanceAdjustmentDialog
import com.hereliesaz.cuedetat.ui.composables.dialogs.TableSizeSelectionDialog
import com.hereliesaz.cuedetat.ui.composables.overlays.KineticWarningOverlay
import com.hereliesaz.cuedetat.ui.composables.overlays.TutorialOverlay
import com.hereliesaz.cuedetat.ui.composables.quickalign.QuickAlignAnalyzer
import com.hereliesaz.cuedetat.ui.composables.quickalign.QuickAlignScreen
import com.hereliesaz.cuedetat.ui.composables.quickalign.QuickAlignViewModel
import com.hereliesaz.cuedetat.ui.composables.sliders.TableRotationSlider
import com.hereliesaz.cuedetat.view.ProtractorOverlay

private const val ROUTE_MAIN = "main"
private const val ROUTE_ALIGN = "align"
private const val ROUTE_CALIBRATION = "calibration"

@Composable
fun ProtractorScreen(
    mainViewModel: MainViewModel,
    calibrationViewModel: CalibrationViewModel,
    quickAlignViewModel: QuickAlignViewModel,
    calibrationAnalyzer: CalibrationAnalyzer
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val systemIsDark = isSystemInDarkTheme()
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    val quickAlignAnalyzer = remember(quickAlignViewModel) { QuickAlignAnalyzer(quickAlignViewModel) }

    LaunchedEffect(uiState.showQuickAlignScreen) {
        val route = navController.currentBackStackEntry?.destination?.route
        if (uiState.showQuickAlignScreen && route != ROUTE_ALIGN) {
            navController.navigate(ROUTE_ALIGN) { launchSingleTop = true }
        } else if (!uiState.showQuickAlignScreen && route == ROUTE_ALIGN) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(uiState.showCalibrationScreen) {
        val route = navController.currentBackStackEntry?.destination?.route
        if (uiState.showCalibrationScreen && route != ROUTE_CALIBRATION) {
            navController.navigate(ROUTE_CALIBRATION) { launchSingleTop = true }
        } else if (!uiState.showCalibrationScreen && route == ROUTE_CALIBRATION) {
            navController.popBackStack()
        }
    }

    val isOnMain = currentRoute == ROUTE_MAIN || currentRoute == null

    AzNavRailMenu(
        uiState = uiState,
        onEvent = mainViewModel::onEvent,
        navController = navController,
        currentDestination = currentRoute
    ) {
        // --- Background layer 0: Camera ---
        background(weight = 0) {
            if (uiState.isCameraVisible) {
                val activeAnalyzer = when (currentRoute) {
                    ROUTE_CALIBRATION -> calibrationAnalyzer
                    ROUTE_ALIGN -> quickAlignAnalyzer
                    else -> mainViewModel.visionAnalyzer
                }
                CameraBackground(
                    modifier = Modifier.fillMaxSize(),
                    analyzer = activeAnalyzer
                )
            }
        }

        // --- Background layer 1: AR protractor overlay (main route only) ---
        background(weight = 1) {
            if (isOnMain) {
                ProtractorOverlay(
                    uiState = uiState,
                    systemIsDark = systemIsDark,
                    isTestingCvMask = uiState.isTestingCvMask,
                    onEvent = mainViewModel::onEvent
                )
            }
        }

        // --- Background layer 2: Navigation host (full-screen screens) ---
        background(weight = 2) {
            NavHost(navController = navController, startDestination = ROUTE_MAIN) {
                composable(ROUTE_MAIN) { /* AR and HUD handled by background/onscreen blocks */ }
                composable(ROUTE_ALIGN) {
                    QuickAlignScreen(
                        onEvent = mainViewModel::onEvent,
                        viewModel = quickAlignViewModel,
                        analyzer = quickAlignAnalyzer
                    )
                }
                composable(ROUTE_CALIBRATION) {
                    CalibrationScreen(
                        uiState = uiState,
                        onEvent = mainViewModel::onEvent,
                        viewModel = calibrationViewModel,
                        analyzer = calibrationAnalyzer
                    )
                }
            }
        }

        // --- Onscreen HUD: Top status bar (main route only) ---
        onscreen(alignment = Alignment.TopEnd) {
            if (isOnMain) {
                TopControls(
                    experienceMode = uiState.experienceMode,
                    isTableVisible = uiState.table.isVisible,
                    tableSizeFeet = uiState.table.size.feet,
                    isBeginnerViewLocked = uiState.isBeginnerViewLocked,
                    targetBallDistance = uiState.targetBallDistance,
                    distanceUnit = uiState.distanceUnit,
                    onCycleTableSize = { mainViewModel.onEvent(MainScreenEvent.CycleTableSize) }
                )
            }
        }

        // --- Onscreen HUD: Zoom slider (main route only) ---
        onscreen(alignment = Alignment.CenterEnd) {
            if (isOnMain) {
                ZoomControls(
                    zoomSliderPosition = uiState.zoomSliderPosition,
                    onZoomChange = { mainViewModel.onEvent(MainScreenEvent.ZoomSliderChanged(it)) },
                    modifier = Modifier
                        .fillMaxHeight(0.6f)
                        .padding(end = 12.dp)
                        .width(48.dp)
                )
            }
        }

        // --- Onscreen HUD: Table rotation slider (main route only) ---
        onscreen(alignment = Alignment.BottomCenter) {
            if (isOnMain) {
                TableRotationSlider(
                    isVisible = uiState.table.isVisible,
                    worldRotationDegrees = uiState.worldRotationDegrees,
                    onRotationChange = { mainViewModel.onEvent(MainScreenEvent.TableRotationChanged(it)) },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                )
            }
        }

        // --- Onscreen HUD: Spin control (main route only) ---
        onscreen(alignment = Alignment.BottomEnd) {
            if (isOnMain && uiState.isSpinControlVisible && uiState.spinControlCenter != null) {
                SpinControl(
                    centerPosition = uiState.spinControlCenter!!,
                    selectedSpinOffset = uiState.selectedSpinOffset,
                    lingeringSpinOffset = uiState.lingeringSpinOffset,
                    spinPathAlpha = uiState.spinPathsAlpha,
                    onEvent = mainViewModel::onEvent,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp, end = 16.dp)
                )
            }
        }

        // --- Onscreen: Kinetic warning overlay ---
        onscreen(alignment = Alignment.Center) {
            KineticWarningOverlay(text = uiState.warningText)
        }

        // --- Onscreen: Tutorial overlay (main route only) ---
        onscreen(alignment = Alignment.Center) {
            if (isOnMain) {
                TutorialOverlay(uiState = uiState, onEvent = mainViewModel::onEvent)
            }
        }

        // --- Onscreen: Dialogs ---
        onscreen(alignment = Alignment.Center) {
            AdvancedOptionsDialog(
                uiState = uiState,
                onEvent = mainViewModel::onEvent,
                onDismiss = { mainViewModel.onEvent(MainScreenEvent.ToggleAdvancedOptionsDialog) }
            )
        }

        onscreen(alignment = Alignment.Center) {
            LuminanceAdjustmentDialog(
                uiState = uiState,
                onEvent = mainViewModel::onEvent,
                onDismiss = { mainViewModel.onEvent(MainScreenEvent.ToggleLuminanceDialog) }
            )
        }

        onscreen(alignment = Alignment.Center) {
            GlowStickDialog(
                uiState = uiState,
                onEvent = mainViewModel::onEvent,
                onDismiss = { mainViewModel.onEvent(MainScreenEvent.ToggleGlowStickDialog) }
            )
        }

        onscreen(alignment = Alignment.Center) {
            TableSizeSelectionDialog(
                uiState = uiState,
                onEvent = mainViewModel::onEvent,
                onDismiss = { mainViewModel.onEvent(MainScreenEvent.ToggleTableSizeDialog) }
            )
        }
    }
}
