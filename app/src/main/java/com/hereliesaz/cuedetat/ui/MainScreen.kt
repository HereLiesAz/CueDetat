package com.hereliesaz.cuedetat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.xr.compose.Subspace
import com.hereliesaz.cuedetat.ar.BilliardsScene
import com.hereliesaz.cuedetat.ui.composables.MenuDrawer
import com.hereliesaz.cuedetat.ui.composables.ShotControls
import com.hereliesaz.cuedetat.ui.state.UiEvent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = if (uiState.isDrawerOpen) DrawerValue.Open else DrawerValue.Closed)

    MenuDrawer(drawerState = drawerState, scope = scope) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Cue D'état") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onEvent(UiEvent.ToggleDrawer) }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Subspace {
                    BilliardsScene(
                        uiState = uiState,
                        onTap = { hitResult ->
                            viewModel.onEvent(UiEvent.OnTap(hitResult))
                        }
                    )
                }
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    ShotControls(
                        power = uiState.shotPower,
                        spin = uiState.cueballSpin,
                        onPowerChange = { power -> viewModel.onEvent(UiEvent.SetShotPower(power)) },
                        onSpinChange = { spin -> viewModel.onEvent(UiEvent.SetSpin(spin)) },
                        onExecuteShot = { viewModel.onEvent(UiEvent.ExecuteShot) }
                    )
                }
            }
        }
    }

    // Effect to open/close drawer based on state
    scope.launch {
        if (uiState.isDrawerOpen) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }
}