// app/src/main/java/com/hereliesaz/cuedetat/ui/ProtractorScreen.kt
package com.hereliesaz.cuedetat.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.cuedetat.data.CalibrationAnalyzer
import com.hereliesaz.cuedetat.ui.composables.AzNavRailMenu
import com.hereliesaz.cuedetat.ui.composables.CameraBackground
import com.hereliesaz.cuedetat.ui.composables.calibration.CalibrationScreen
import com.hereliesaz.cuedetat.ui.composables.quickalign.QuickAlignAnalyzer
import com.hereliesaz.cuedetat.ui.composables.quickalign.QuickAlignScreen
import com.hereliesaz.cuedetat.ui.composables.quickalign.QuickAlignViewModel
import com.hereliesaz.cuedetat.ui.composables.calibration.CalibrationViewModel
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

    // Sync state-driven navigation (from AdvancedOptionsDialog, events, etc.) → navController.
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

    AzNavRailMenu(
        uiState = uiState,
        onEvent = mainViewModel::onEvent,
        navController = navController,
        currentDestination = currentRoute
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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

            NavHost(navController = navController, startDestination = ROUTE_MAIN) {
                composable(ROUTE_MAIN) {
                    MainLayout(uiState = uiState, onEvent = mainViewModel::onEvent) {
                        ProtractorOverlay(
                            uiState = uiState,
                            systemIsDark = systemIsDark,
                            isTestingCvMask = uiState.isTestingCvMask,
                            onEvent = mainViewModel::onEvent
                        )
                    }
                }

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
    }
}
