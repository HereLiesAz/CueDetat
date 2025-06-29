package com.hereliesaz.cuedetat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.hereliesaz.cuedetat.ar.scene.BilliardsScene
import com.hereliesaz.cuedetat.ui.composables.MenuDrawer
import com.hereliesaz.cuedetat.ui.state.UiState
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    uiState: UiState
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MenuDrawer(
                onClose = {
                    scope.launch {
                        drawerState.close()
                    }
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isArMode) {
                BilliardsScene(uiState = uiState)
            } else {
                // Non-AR UI would go here
            }
            // Other UI elements like controls can be added here
        }
    }
}