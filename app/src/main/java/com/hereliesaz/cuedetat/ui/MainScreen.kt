package com.hereliesaz.cuedetat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.google.ar.core.Session
import com.hereliesaz.cuedetat.ar.jetpack.ArView
import com.hereliesaz.cuedetat.ui.composables.HelpDialog
import com.hereliesaz.cuedetat.ui.composables.InstructionText
import com.hereliesaz.cuedetat.ui.composables.MenuDrawer
import com.hereliesaz.cuedetat.ui.composables.ShotControls
import com.hereliesaz.cuedetat.ui.state.ShotType
import com.hereliesaz.cuedetat.ui.state.UiEvent
import com.hereliesaz.cuedetat.ui.state.UiState
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: UiState,
    onEvent: (UiEvent) -> Unit,
    arSession: Session?
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MenuDrawer(
                onClose = {
                    scope.launch { drawerState.close() }
                },
                onEvent = onEvent
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ArView is the background, filling the whole screen, ignoring insets.
            if (arSession != null) {
                ArView(
                    modifier = Modifier.fillMaxSize(),
                    session = arSession,
                    uiState = uiState,
                    onTap = { motionEvent ->
                        onEvent(UiEvent.OnScreenTap(Offset(motionEvent.x, motionEvent.y)))
                    }
                )
            }

            // Scaffold sits on top of the ArView. It's transparent and manages UI placement.
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent, // Make scaffold background transparent
                topBar = {
                    // This content will be automatically placed below the status bar/notch.
                    InstructionText(text = uiState.instructionText)
                },
                bottomBar = {
                    // This content will be automatically placed above the navigation bar.
                    ShotControls(
                        shotPower = uiState.shotPower,
                        spin = uiState.cueballSpin,
                        onEvent = onEvent,
                        onMenuClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }
                    )
                }
            ) { innerPadding ->
                // The main content area of the scaffold. We can leave it empty
                // as the ArView is already filling the background.
                // The innerPadding contains the insets handled by the Scaffold.
                Box(modifier = Modifier.padding(innerPadding))
            }

            // The HelpDialog will overlay everything.
            if (uiState.showHelp) {
                HelpDialog(onDismiss = { onEvent(UiEvent.ToggleHelpDialog) })
            }
        }
    }
}

@Preview
@Composable
private fun MainScreenPreview() {
    CueDetatTheme {
        MainScreen(uiState = UiState(shotType = ShotType.CUT), onEvent = {}, arSession = null)
    }
}