package com.hereliesaz.cuedetat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.cuedetat.ar.ARCoreRenderer
import com.hereliesaz.cuedetat.ui.composables.MenuDrawer
import com.hereliesaz.cuedetat.ui.composables.ShotControls
import com.hereliesaz.cuedetat.ui.state.UiEvent
import kotlinx.coroutines.launch

/**
 * The main screen of the application, orchestrating the AR scene and the UI controls.
 * This composable follows a modern, declarative architecture using Jetpack Compose and androidx.xr.
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    // Collect the latest state from the ViewModel. The UI will automatically recompose
    // whenever this state changes.
    val uiState by viewModel.uiState.collectAsState()

    // State for managing the navigation drawer.
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // The root of the UI, using a ModalNavigationDrawer for the settings menu.
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MenuDrawer(
                onEvent = viewModel::onEvent
            )
        }
    ) {
        // Scaffold provides a standard layout structure (e.g., for a top app bar, FAB).
        Scaffold { paddingValues ->
            // Box is used to layer UI elements. The AR scene is the base layer,
            // and the UI controls are drawn on top of it.
            Box(modifier = Modifier.fillMaxSize()) {
                // The ARCoreRenderer is the composable that handles the AR camera feed and 3D scene.
                ARCoreRenderer(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel
                )

                // The ShotControls composable provides the user interface for interacting
                // with the AR scene (e.g., adjusting spin, elevation).
                ShotControls(
                    modifier = Modifier.fillMaxSize(),
                    uiState = uiState,
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                    },
                    onEvent = viewModel::onEvent
                )

                // You could add other overlays here as needed, like the ProtractorOverlay,
                // passing the relevant state from the ViewModel.
                // For example:
                // if (uiState.showProtractor) {
                //     ProtractorOverlay(
                //         angleDegrees = uiState.protractorAngle,
                //         center = uiState.protractorCenter
                //     )
                // }
            }
        }
    }
}