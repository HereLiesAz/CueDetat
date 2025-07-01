package com.hereliesaz.cuedetatlite.ui

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.hereliesaz.cuedetatlite.ui.composables.*
import com.hereliesaz.cuedetatlite.ui.theme.CueDetatTheme
import com.hereliesaz.cuedetatlite.utils.ToastMessage
import com.hereliesaz.cuedetatlite.view.ProtractorOverlayView
import kotlinx.coroutines.launch
import com.hereliesaz.cuedetatlite.ui.composables.Action

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val overlayState by viewModel.overlayState.collectAsStateWithLifecycle()
    val kineticWarning by viewModel.kineticWarning.collectAsStateWithLifecycle()
    val isUpdateAvailable by viewModel.isUpdateAvailable.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    toastMessage?.let { msg ->
        val context = LocalContext.current
        LaunchedEffect(msg) {
            val toastText = when (msg) {
                is ToastMessage.StringResource -> context.getString(msg.id, *msg.formatArgs.toTypedArray())
                is ToastMessage.Text -> msg.text
            }
            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
        }
    }

    if (isUpdateAvailable != null) {
        UpdateAvailableDialogue(
            onDismiss = { viewModel.onEvent(MainScreenEvent.OnUpdateDismissed) },
            onConfirm = { viewModel.onEvent(MainScreenEvent.OnDownloadClicked) }
        )
    }

    CueDetatTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                MenuDrawer(
                    onAction = { action ->
                        // Handle menu actions
                    }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    TopControls(
                        onMenuClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        onLightModeClick = { viewModel.onEvent(MainScreenEvent.OnForceLightMode(it)) },
                        onHelpClick = {
                            // Show help
                        }
                    )
                },
                floatingActionButton = {
                    ActionFabs(
                        onUndo = { viewModel.onEvent(MainScreenEvent.OnUndo) },
                        onRedo = { viewModel.onEvent(MainScreenEvent.OnRedo) },
                        onJumpShot = { viewModel.onEvent(MainScreenEvent.OnJumpShot) }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    if (cameraPermissionState.status.isGranted) {
                        CameraPreview(modifier = Modifier.fillMaxSize())
                    } else {
                        CameraBackground(
                            onUpdate = {
                                cameraPermissionState.launchPermissionRequest()
                                viewModel.onEvent(MainScreenEvent.OnUpdate(cameraPermissionState.status.isGranted))
                            }
                        )
                    }

                    overlayState?.let {
                        ProtractorOverlayView(
                            overlayState = it,
                            onEvent = viewModel::onEvent,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    kineticWarning?.let {
                        KineticWarning(
                            message = it,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 80.dp)
                        )
                    }

                    VerticalSlider(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp),
                        onValueChange = { viewModel.onEvent(MainScreenEvent.OnLuminanceChange(it)) }
                    )

                    ZoomControls(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 16.dp, end = 16.dp),
                        onZoomIn = { viewModel.onEvent(MainScreenEvent.OnScale(1.1f)) },
                        onZoomOut = { viewModel.onEvent(MainScreenEvent.OnScale(0.9f)) }
                    )
                }
            }
        }
    }
}
