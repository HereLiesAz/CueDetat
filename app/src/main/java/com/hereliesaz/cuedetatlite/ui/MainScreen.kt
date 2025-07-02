package com.hereliesaz.cuedetatlite.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hereliesaz.cuedetatlite.ui.composables.*
import com.hereliesaz.cuedetatlite.view.ProtractorOverlayView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val overlayState by viewModel.overlayState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val systemIsDark = isSystemInDarkTheme()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MenuDrawerContent(
                uiState = overlayState,
                onEvent = viewModel::onEvent,
                onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopControls(
                    uiState = overlayState,
                    onMenuClick = { coroutineScope.launch { drawerState.open() } }
                )
            },
            floatingActionButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (!overlayState.isBankingMode) {
                        ToggleCueBallFab(
                            uiState = overlayState,
                            onEvent = viewModel::onEvent
                        )
                    } else {
                        Spacer(Modifier) // Keep the layout balanced
                    }

                    ResetFab(
                        uiState = overlayState,
                        onEvent = viewModel::onEvent
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.Center
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                CameraBackground(modifier = Modifier.fillMaxSize())

                AndroidView(
                    factory = { context ->
                        ProtractorOverlayView(context).apply {
                            this.onEvent = { event ->
                                viewModel.onEvent(event)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        view.updateState(overlayState, systemIsDark)
                    }
                )

                KineticWarning(text = uiState.warningMessage)

                if (uiState.showLuminanceDialog) {
                    LuminanceDialog(
                        currentLuminance = overlayState.luminanceAdjustment,
                        onEvent = viewModel::onEvent
                    )
                }

                ZoomControls(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .fillMaxHeight(0.5f),
                    uiState = overlayState,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}