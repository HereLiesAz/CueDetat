package com.hereliesaz.cuedetat.ui

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hereliesaz.cuedetat.MainActivity
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
    activity: MainActivity?,
    glSurfaceView: GLSurfaceView?
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MenuDrawer(
                onClose = { scope.launch { drawerState.close() } },
                onEvent = onEvent
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (glSurfaceView != null) {
                AndroidView({ glSurfaceView }, modifier = Modifier.fillMaxSize())
            }

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
                    } else {
                        Row(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom=32.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(onClick = { onEvent(UiEvent.ToggleArMode) }) {
                                Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(if (uiState.isArMode) "Use Manual Placement" else "Use AR Scan")
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showHelp) {
        HelpDialog { onEvent(UiEvent.ToggleHelpDialog) }
    }
}