package com.hereliesaz.cuedetatlite.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.hereliesaz.cuedetatlite.ui.composables.*
import com.hereliesaz.cuedetatlite.view.ProtractorOverlayView
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val showUpdateAvailable by viewModel.showUpdateAvailable.collectAsState()
    val showKineticWarning by viewModel.showKineticWarning.collectAsState()
    val hasCameraPermission by viewModel.hasCameraPermission.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MenuDrawer(
                uiState = uiState,
                onAction = { action ->
                    handleMenuAction(action, viewModel, context)
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasCameraPermission) {
                CameraBackground(modifier = Modifier.fillMaxSize())
            }

            AndroidView(
                factory = {
                    ProtractorOverlayView(it).apply {
                        this.setViewModel(viewModel)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            TopControls(
                onMenuClick = { scope.launch { drawerState.open() } }
            )

            ActionFabs(
                uiState = uiState,
                onSwitchModes = { viewModel.onEvent(MainScreenEvent.SwitchModes) },
                onUndo = { viewModel.onEvent(MainScreenEvent.Undo) }
            )

            ZoomControls(
                uiState = uiState,
                onZoomChange = { viewModel.onEvent(MainScreenEvent.ZoomChanged(it)) }
            )

            if (showUpdateAvailable) {
                UpdateAvailableDialog(
                    onConfirm = { openUrl(context, "https://github.com/hereliesaz/cuedetat/releases") },
                    onDismiss = { viewModel.dismissUpdateDialog() }
                )
            }

            if (showKineticWarning) {
                KineticWarning(onDismiss = { viewModel.dismissKineticWarning() })
            }
        }
    }
}

private fun handleMenuAction(action: MenuAction, viewModel: MainViewModel, context: Context) {
    when (action) {
        is MenuAction.ToggleDarkMode -> viewModel.onEvent(MainScreenEvent.ToggleDarkMode)
        is MenuAction.ToggleJumpShot -> viewModel.onEvent(MainScreenEvent.ToggleJumpShot)
        is MenuAction.ToggleProtractorCueBall -> viewModel.onEvent(MainScreenEvent.ToggleProtractorCueBall)
        is MenuAction.ToggleActualCueBall -> viewModel.onEvent(MainScreenEvent.ToggleActualCueBall)
        is MenuAction.ShowHelp -> openUrl(context, "https://github.com/hereliesaz/cuedetat/wiki")
    }
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse(url)
    context.startActivity(intent)
}