package com.hereliesaz.cuedetat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

@Composable
fun MainScreen(
    uiState: UiState,
    onEvent: (UiEvent) -> Unit,
    arSession: Session?
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            // AR View is the background, filling the entire screen.
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

            // This is the container for ALL UI elements.
            // We apply padding that respects the system bars (status bar, navigation bar).
            // This creates the "global margin" you requested.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                // All UI elements go inside this Box and will be constrained by the padding.
                InstructionText(
                    text = uiState.instructionText,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                ShotControls(
                    shotPower = uiState.shotPower,
                    spin = uiState.cueballSpin,
                    onEvent = onEvent,
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter) // Align to bottom of safe area
                )
            }

            // The Modal Drawer will handle its own positioning correctly.
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
                // The content of the drawer is the main screen content, which we've already defined in the outer Box.
                // So, this lambda can be empty.
            }


            // Help dialog is a popup, it can stay outside the main layout container.
            if (uiState.showHelp) {
                HelpDialog(onDismiss = { onEvent(UiEvent.ToggleHelpDialog) })
            }
        }
    }
}

// Update ShotControls to accept a modifier
@Composable
fun ShotControls(
    modifier: Modifier = Modifier, // Add modifier parameter
    shotPower: Float,
    spin: Offset,
    onEvent: (UiEvent) -> Unit,
    onMenuClick: () -> Unit
) {
    Column(
        modifier = modifier, // Apply the modifier here
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TODO: Re-implement the full shot controls UI using onEvent, including a menu button that calls onMenuClick
        Slider(value = shotPower, onValueChange = { onEvent(UiEvent.SetShotPower(it)) })
        Button(onClick = { onEvent(UiEvent.ExecuteShot) }) {
            Text("Shoot")
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