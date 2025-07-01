package com.hereliesaz.cuedetat.ui

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hereliesaz.cuedetat.MainActivity
import com.hereliesaz.cuedetat.ar.jetpack.helpers.DisplayRotationHelper
import com.hereliesaz.cuedetat.ar.rendering.OpenXrRenderer
import com.hereliesaz.cuedetat.ui.composables.*
import com.hereliesaz.cuedetat.ui.state.UiEvent
import com.hereliesaz.cuedetat.ui.state.UiState
import kotlinx.coroutines.launch

@SuppressLint("ClickableViewAccessibility")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: UiState,
    onEvent: (UiEvent) -> Unit,
    activity: MainActivity?
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var renderer by remember { mutableStateOf<OpenXrRenderer?>(null) }

    renderer?.updateState(uiState)

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            // AR View is the background
            if (activity?.arSession != null) {
                AndroidView(
                    factory = { context ->
                        GLSurfaceView(context).apply {
                            preserveEGLContextOnPause = true
                            setEGLContextClientVersion(2)
                            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                            val newRenderer = OpenXrRenderer(
                                session = activity.arSession!!,
                                displayRotationHelper = DisplayRotationHelper(activity),
                                uiState = uiState
                            )
                            renderer = newRenderer
                            setRenderer(newRenderer)
                            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                            setOnTouchListener { _, event ->
                                if (event.action == MotionEvent.ACTION_DOWN) {
                                    onEvent(UiEvent.OnScreenTap(Offset(event.x, event.y)))
                                }
                                true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // UI on top
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = { Text(uiState.instructionText, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, "Menu") } },
                        actions = {
                            IconButton(onClick = { activity?.toggleFlashlight() }) { Icon(Icons.Default.FlashOn, "Toggle Flashlight") }
                            IconButton(onClick = { onEvent(UiEvent.ToggleHelpDialog) }) { Icon(Icons.Default.HelpOutline, "Help") }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.3f), titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White)
                    )
                },
                contentWindowInsets = WindowInsets.systemBars
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    if (uiState.table != null) {
                        Column(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SpinControl(onSpinChanged = { onEvent(UiEvent.SetSpin(it)) })
                            Spacer(Modifier.height(8.dp))
                            Slider(value = uiState.shotPower, onValueChange = { onEvent(UiEvent.SetShotPower(it)) }, modifier = Modifier.fillMaxWidth(0.8f))
                        }
                    }
                }
            }

            if (uiState.showHelp) {
                HelpDialog { onEvent(UiEvent.ToggleHelpDialog) }
            }
        }
    }
}