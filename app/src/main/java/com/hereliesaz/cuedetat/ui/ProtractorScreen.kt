// app/src/main/java/com/hereliesaz/cuedetat/ui/ProtractorScreen.kt
package com.hereliesaz.cuedetat.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.camera.core.ImageAnalysis
import com.hereliesaz.cuedetat.data.CalibrationAnalyzer
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.ui.composables.AzNavRailMenu
import com.hereliesaz.cuedetat.ui.composables.ArCoreBackground
import com.hereliesaz.cuedetat.ui.composables.CameraBackground
import com.hereliesaz.cuedetat.ui.composables.CuedetatButton
import com.hereliesaz.cuedetat.ui.composables.SpinControl
import com.hereliesaz.cuedetat.ui.composables.TopControls
import com.hereliesaz.cuedetat.ui.composables.ZoomControls
import com.hereliesaz.cuedetat.ui.composables.calibration.CalibrationScreen
import com.hereliesaz.cuedetat.ui.composables.calibration.CalibrationViewModel
import com.hereliesaz.cuedetat.ui.composables.dialogs.AdvancedOptionsDialog
import com.hereliesaz.cuedetat.ui.composables.dialogs.GlowStickDialog
import com.hereliesaz.cuedetat.ui.composables.dialogs.LuminanceAdjustmentDialog
import com.hereliesaz.cuedetat.ui.composables.dialogs.TableSizeSelectionDialog
import com.hereliesaz.cuedetat.domain.CameraMode
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.ui.composables.overlays.ArSetupPrompt
import com.hereliesaz.cuedetat.ui.composables.MasseControl
import com.hereliesaz.cuedetat.ui.composables.overlays.ArTrackingBadge
import com.hereliesaz.cuedetat.ui.composables.overlays.KineticWarningOverlay
import com.hereliesaz.cuedetat.ui.composables.overlays.TutorialOverlay
import com.hereliesaz.cuedetat.ui.composables.sliders.TableRotationSlider
import com.hereliesaz.cuedetat.ui.composables.tablescan.TableScanAnalyzer
import com.hereliesaz.cuedetat.ui.composables.tablescan.TableScanScreen
import com.hereliesaz.cuedetat.ui.composables.tablescan.TableScanViewModel
import com.hereliesaz.cuedetat.view.ProtractorOverlay

private const val ROUTE_MAIN = "main"
private const val ROUTE_CALIBRATION = "calibration"
private const val ROUTE_SCAN = "scan"

@Composable
fun ProtractorScreen(
    mainViewModel: MainViewModel,
    calibrationViewModel: CalibrationViewModel,
    tableScanViewModel: TableScanViewModel,
    calibrationAnalyzer: CalibrationAnalyzer
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val systemIsDark = isSystemInDarkTheme()
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    val tableScanAnalyzer = remember(tableScanViewModel) { TableScanAnalyzer(tableScanViewModel::onFrame, tableScanViewModel::onFeltColorSampled, tableScanViewModel.pocketDetector) }

    LaunchedEffect(uiState.showCalibrationScreen) {
        val route = navController.currentBackStackEntry?.destination?.route
        if (uiState.showCalibrationScreen && route != ROUTE_CALIBRATION) {
            navController.navigate(ROUTE_CALIBRATION) { launchSingleTop = true }
        } else if (!uiState.showCalibrationScreen && route == ROUTE_CALIBRATION) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(uiState.showTableScanScreen) {
        val route = navController.currentBackStackEntry?.destination?.route
        if (uiState.showTableScanScreen && route != ROUTE_SCAN) {
            navController.navigate(ROUTE_SCAN) { launchSingleTop = true }
        } else if (!uiState.showTableScanScreen && route == ROUTE_SCAN) {
            navController.popBackStack()
        }
    }

    val isOnMain = currentRoute == ROUTE_MAIN || currentRoute == null
    val isOnCalibration = currentRoute == ROUTE_CALIBRATION
    val isOnScan = currentRoute == ROUTE_SCAN

    AzNavRailMenu(
        uiState = uiState,
        onEvent = mainViewModel::onEvent,
        navController = navController,
        currentDestination = if (uiState.experienceMode == ExperienceMode.BEGINNER && isOnMain) {
            if (uiState.isBeginnerViewLocked) "static" else "dynamic"
        } else currentRoute,
        hasTableModel = uiState.tableScanModel != null
    ) {
        // --- Background layer 0: Camera ---
        // AR mode with ARCore depth uses ArCoreBackground (manages its own GL + session).
        // All other camera modes use the standard CameraX-backed CameraBackground.
        background(weight = 0) {
            when {
                uiState.cameraMode == CameraMode.AR
                        && uiState.depthCapability == com.hereliesaz.cuedetat.domain.DepthCapability.DEPTH_API
                        && isOnMain -> {
                    ArCoreBackground(
                        modifier = Modifier.fillMaxSize(),
                        arDepthSession = mainViewModel.arDepthSession,
                        arFrameProcessor = mainViewModel.arFrameProcessor,
                        onEvent = mainViewModel::onEvent,
                    )
                }
                uiState.cameraMode != CameraMode.OFF -> {
                    // Static beginner mode shows the camera as a background but CV must not run.
                    // Pass null → CameraBackground binds Preview only, no ImageAnalysis use case.
                    val activeAnalyzer: ImageAnalysis.Analyzer? = when {
                        uiState.isBeginnerViewLocked -> null
                        currentRoute == ROUTE_CALIBRATION -> calibrationAnalyzer
                        currentRoute == ROUTE_SCAN -> tableScanAnalyzer
                        else -> mainViewModel.visionAnalyzer
                    }
                    CameraBackground(
                        modifier = Modifier.fillMaxSize(),
                        analyzer = activeAnalyzer,
                    )
                }
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
                composable(ROUTE_CALIBRATION) {
                    CalibrationScreen(
                        uiState = uiState,
                        onEvent = mainViewModel::onEvent,
                        viewModel = calibrationViewModel,
                        analyzer = calibrationAnalyzer
                    )
                }
                composable(ROUTE_SCAN) {
                    TableScanScreen(
                        onEvent = mainViewModel::onEvent,
                        uiState = uiState,
                        viewModel = tableScanViewModel
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
            if (isOnMain && !(uiState.experienceMode == ExperienceMode.BEGINNER && uiState.isBeginnerViewLocked)) {
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
        // Positioned absolutely at spinControlCenter (screen pixels). Double-tap+drag to reposition.
        onscreen(alignment = Alignment.TopStart) {
            if (isOnMain && uiState.isSpinControlVisible && !uiState.isMasseModeActive && uiState.spinControlCenter != null) {
                val center = uiState.spinControlCenter!!
                SpinControl(
                    selectedSpinOffset = uiState.selectedSpinOffset,
                    lingeringSpinOffset = uiState.lingeringSpinOffset,
                    spinPathAlpha = uiState.spinPathsAlpha,
                    onEvent = mainViewModel::onEvent,
                    modifier = Modifier.absoluteOffset {
                        IntOffset(
                            (center.x - 60.dp.roundToPx()).toInt(),
                            (center.y - 60.dp.roundToPx()).toInt()
                        )
                    }
                )
            }
        }

        // --- Onscreen HUD: Masse control (main route only) ---
        // Positioned absolutely at spinControlCenter (screen pixels). Double-tap+drag to reposition.
        onscreen(alignment = Alignment.TopStart) {
            if (isOnMain && uiState.isMasseModeActive && uiState.spinControlCenter != null) {
                val center = uiState.spinControlCenter!!
                MasseControl(
                    elevationAngle = (90f - kotlin.math.abs(uiState.pitchAngle)).coerceIn(0f, 90f),
                    selectedSpinOffset = uiState.selectedSpinOffset,
                    lingeringSpinOffset = uiState.lingeringSpinOffset,
                    spinPathAlpha = uiState.spinPathsAlpha,
                    onEvent = mainViewModel::onEvent,
                    modifier = Modifier.absoluteOffset {
                        IntOffset(
                            (center.x - 60.dp.roundToPx()).toInt(),
                            (center.y - 60.dp.roundToPx()).toInt()
                        )
                    }
                )
            }
        }

        // --- Onscreen HUD: Calibration Controls ---
        onscreen(alignment = Alignment.TopCenter) {
            if (isOnCalibration) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Camera Calibration",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Show the camera a 4x11 circle grid pattern from various angles. " +
                                "Capture at least 10-15 images for an accurate calibration. " +
                                "A green overlay will indicate a successful pattern detection.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        onscreen(alignment = Alignment.BottomCenter) {
            if (isOnCalibration) {
                val capturedImageCount by calibrationViewModel.capturedImageCount.collectAsState()
                val detectedPattern by calibrationViewModel.detectedPattern.collectAsState()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Images: $capturedImageCount/15",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    CuedetatButton(
                        onClick = { calibrationViewModel.capturePattern() },
                        text = "Capture",
                        color = if (detectedPattern != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.5f
                        )
                    )

                    TextButton(onClick = { calibrationViewModel.onCalibrationFinished() }) {
                        Text("Finish", color = Color.White)
                    }
                }
            }
        }

        // --- Onscreen HUD: Table Scan Controls ---
        onscreen(alignment = Alignment.BottomCenter) {
            if (isOnScan) {
                val scanProgress by tableScanViewModel.scanProgress.collectAsState()
                val foundCount = scanProgress.count { it.value }
                val allFound = foundCount >= 6

                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$foundCount / 6 pockets found — pan across the table",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = {
                            tableScanViewModel.resetScan()
                        }) { Text("Reset") }
                        Button(
                            onClick = { mainViewModel.onEvent(MainScreenEvent.ToggleTableScanScreen) },
                            enabled = allFound
                        ) { Text("Done") }
                    }
                }
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

        // --- Onscreen: AR setup prompt (center, when AR active but no scan) ---
        onscreen(alignment = Alignment.Center) {
            if (isOnMain) {
                ArSetupPrompt(
                    visible = uiState.cameraMode == CameraMode.AR && uiState.tableScanModel == null
                )
            }
        }

        // --- Onscreen: AR tracking badge (bottom-start, when AR is actively tracking) ---
        onscreen(alignment = Alignment.BottomStart) {
            if (isOnMain && uiState.cameraMode == CameraMode.AR
                && (uiState.tableScanModel != null || uiState.depthPlane != null)) {
                ArTrackingBadge(
                    modifier = Modifier.padding(start = 16.dp, bottom = 72.dp),
                    hasDepth = uiState.depthCapability == com.hereliesaz.cuedetat.domain.DepthCapability.DEPTH_API,
                    distanceMeters = uiState.depthPlane?.distanceMeters,
                )
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