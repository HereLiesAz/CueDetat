package com.hereliesaz.cuedetatlite.ui

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetatlite.ui.composables.*
import com.hereliesaz.cuedetatlite.view.ProtractorOverlayView
import com.hereliesaz.cuedetatlite.view.state.OverlayState
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

    // Update available dialog
    if (uiState.isUpdateAvailable) {
        UpdateAvailableDialog(
            onDismiss = { viewModel.onEvent(MainScreenEvent.DismissUpdateDialog) },
            onConfirm = { viewModel.onEvent(MainScreenEvent.DownloadUpdate) }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MenuDrawer(
                onActionSelected = { action ->
                    coroutineScope.launch { drawerState.close() }
                    // Handle menu actions here, mapping them to events
                    when (action) {
                        MenuAction.TOGGLE_HELP -> viewModel.onEvent(MainScreenEvent.ToggleHelp)
                        MenuAction.FORCE_LIGHT_MODE -> viewModel.onEvent(MainScreenEvent.ForceLightMode(!(uiState.isForceLightMode ?: false)))
                        MenuAction.SHOW_LUMINANCE_DIALOG -> viewModel.onEvent(MainScreenEvent.ShowLuminanceDialog)
                        MenuAction.START_TUTORIAL -> viewModel.onEvent(MainScreenEvent.StartTutorial)
                        MenuAction.TOGGLE_BANKING_MODE -> viewModel.onEvent(MainScreenEvent.ToggleBankingMode)
                    }
                }
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopControls(
                    onAction = { action ->
                        when(action) {
                            MenuAction.OPEN_DRAWER -> coroutineScope.launch { drawerState.open() }
                            else -> { /* Other top bar actions if any */ }
                        }
                    },
                    uiState = uiState
                )
            },
            floatingActionButton = {
                ActionFabs(
                    onUndo = { viewModel.onEvent(MainScreenEvent.Reset) },
                    onRedo = { viewModel.onEvent(MainScreenEvent.Redo) },
                    onJumpShot = { viewModel.onEvent(MainScreenEvent.JumpShot) },
                    uiState = uiState
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val activity = LocalContext.current as Activity
                CameraBackground(activity = activity)

                ProtractorOverlayView(
                    overlayState = overlayState,
                    onEvent = viewModel::onEvent,
                    modifier = Modifier.fillMaxSize()
                )

                KineticWarning(warningText = uiState.warningMessage)

                if (uiState.showLuminanceDialog) {
                    AlertDialog(
                        onDismissRequest = { viewModel.onEvent(MainScreenEvent.DismissLuminanceDialog) },
                        title = { Text("Adjust Luminance") },
                        text = {
                            VerticalSlider(
                                value = overlayState.luminanceAdjustment,
                                onValueChange = { viewModel.onEvent(MainScreenEvent.LuminanceChanged(it)) }
                            )
                        },
                        confirmButton = {
                            Button(onClick = { viewModel.onEvent(MainScreenEvent.DismissLuminanceDialog) }) {
                                Text("OK")
                            }
                        }
                    )
                }

                ZoomControls(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(16.dp),
                    uiState = uiState,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}
