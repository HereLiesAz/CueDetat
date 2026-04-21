// app/src/main/java/com/hereliesaz/cuedetat/ui/ProtractorScreen.kt
package com.hereliesaz.cuedetat.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
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
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.ui.composables.AzNavRailMenu
import com.hereliesaz.cuedetat.ui.composables.ArCoreBackground
import com.hereliesaz.cuedetat.ui.composables.CameraBackground
import com.hereliesaz.cuedetat.ui.composables.CuedetatButton
import com.hereliesaz.cuedetat.ui.composables.SpinControl
import com.hereliesaz.cuedetat.ui.composables.TopControls
import com.hereliesaz.cuedetat.ui.composables.ZoomControls
import com.hereliesaz.cuedetat.ui.composables.dialogs.GlowStickDialog
import com.hereliesaz.cuedetat.ui.composables.dialogs.LuminanceAdjustmentDialog
import com.hereliesaz.cuedetat.ui.composables.dialogs.TableSizeSelectionDialog
import com.hereliesaz.cuedetat.domain.CameraMode
import com.hereliesaz.cuedetat.domain.ExperienceMode
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

@Composable
fun ProtractorScreen(
    mainViewModel: MainViewModel,
    tableScanViewModel: TableScanViewModel
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val systemIsDark = isSystemInDarkTheme()
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    val tableScanAnalyzer = remember(tableScanViewModel) { TableScanAnalyzer(tableScanViewModel::onFrame, tableScanViewModel::onFeltColorSampled, tableScanViewModel.pocketDetector) }

    val isOnMain = currentRoute == ROUTE_MAIN || currentRoute == null

    // --- Top-level animation for top-down snap view ---
    val topDownProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (uiState.isTopDownViewActive) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 600),
        label = "topDownProgress"
    )

    AzNavRailMenu(
        uiState = uiState,
        onEvent = { event ->
            if (event is MainScreenEvent.StartManualHoleCapture) {
                tableScanViewModel.startManualHoleCapture()
            }
            mainViewModel.onEvent(event)
        },
        navController = navController,
        currentDestination = if (uiState.experienceMode == ExperienceMode.BEGINNER && isOnMain) {
            if (uiState.isBeginnerViewLocked) "static" else "dynamic"
        } else currentRoute,
    ) {
        // --- Background layer 0: Camera ---
        // AR mode with ARCore depth uses ArCoreBackground (manages its own GL + session).
        // All other camera modes use the standard CameraX-backed CameraBackground.
        background(weight = 0) {
            when {
                uiState.cameraMode == CameraMode.AR_ACTIVE
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
                        uiState.showTableScanScreen -> tableScanAnalyzer
                        else -> mainViewModel.visionAnalyzer
                    }
                    CameraBackground(
                        modifier = Modifier.fillMaxSize(),
                        analyzer = activeAnalyzer,
                    )
                }
            }
        }


        background(weight = 1) {
            if (isOnMain) {
                ProtractorOverlay(
                    uiState = uiState,
                    systemIsDark = systemIsDark,
                    isTestingCvMask = uiState.isTestingCvMask,
                    onEvent = mainViewModel::onEvent,
                    topDownProgress = topDownProgress
                )
            }
        }

        // --- Background layer 2: Navigation host (full-screen screens) ---
        background(weight = 2) {
            NavHost(navController = navController, startDestination = ROUTE_MAIN) {
                composable(ROUTE_MAIN) { /* AR and HUD handled by background/onscreen blocks */ }
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

        // --- Onscreen HUD: Zoom slider (main route only, hidden during felt capture) ---
        onscreen(alignment = Alignment.CenterEnd) {
            if (isOnMain && !uiState.showTableScanScreen && !(uiState.experienceMode == ExperienceMode.BEGINNER && uiState.isBeginnerViewLocked)) {
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

        // --- Onscreen HUD: Table rotation slider (main route only, hidden during felt capture) ---
        onscreen(alignment = Alignment.BottomCenter) {
            if (isOnMain && !uiState.showTableScanScreen) {
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

        // --- Onscreen: Kinetic warning overlay ---
        onscreen(alignment = Alignment.Center) {
            val showWarning = uiState.warningText != null && !uiState.isMasseModeActive && !uiState.isSpinControlVisible
            if (showWarning) {
                KineticWarningOverlay(text = uiState.warningText)
            }
        }

        // --- Onscreen: Tutorial overlay (main route only) ---
        onscreen(alignment = Alignment.TopStart) {
            if (isOnMain) {
                TutorialOverlay(uiState = uiState, onEvent = mainViewModel::onEvent)
            }
        }

        // --- Onscreen: Inline felt capture overlay ---
        onscreen(alignment = Alignment.TopStart) {
            if (isOnMain && uiState.showTableScanScreen) {
                TableScanScreen(
                    onEvent = mainViewModel::onEvent,
                    uiState = uiState,
                    viewModel = tableScanViewModel
                )
            }
        }

        // --- Onscreen: AR tracking badge (bottom-start, when AR is actively tracking) ---
        onscreen(alignment = Alignment.BottomStart) {
            if (isOnMain && uiState.cameraMode == CameraMode.AR_ACTIVE
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