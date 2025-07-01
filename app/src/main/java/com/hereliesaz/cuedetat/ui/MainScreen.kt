package com.hereliesaz.cuedetat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.hereliesaz.cuedetat.ar.scene.BilliardsScene
import com.hereliesaz.cuedetat.ui.composables.MenuDrawer
import com.hereliesaz.cuedetat.ui.state.UiEvent
import com.hereliesaz.cuedetat.ui.state.UiState
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    uiState: UiState
) {
    val drawerState = rememberDrawerState(initialValue = if (uiState.isDrawerOpen) DrawerValue.Open else DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.isDrawerOpen) {
        if (uiState.isDrawerOpen) {
            scope.launch { drawerState.open() }
        } else {
            scope.launch { drawerState.close() }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MenuDrawer(
                onEvent = viewModel::onEvent,
                onClose = { viewModel.onEvent(UiEvent.ToggleDrawer) }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isArMode) {
                BilliardsScene(
                    modifier = Modifier.fillMaxSize(),
                    onSceneCreated = viewModel::onSceneCreated,
                    onTap = { hitResult -> viewModel.onEvent(UiEvent.OnTap(hitResult)) }
                )
            } else {
                // Non-AR UI would go here
            }
            // Other UI elements like controls can be added here
        }
    }
}
