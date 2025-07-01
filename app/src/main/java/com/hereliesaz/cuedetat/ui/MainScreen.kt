package com.hereliesaz.cuedetat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.ar.core.Session
import com.hereliesaz.cuedetat.ar.jetpack.ArView
import com.hereliesaz.cuedetat.ui.composables.HelpDialog
import com.hereliesaz.cuedetat.ui.composables.MenuDrawer
import com.hereliesaz.cuedetat.ui.composables.ShotControls
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MenuDrawer(
                onClose = {
                    scope.launch {
                        drawerState.close()
                    }
                },
                onEvent = onEvent,
                uiState = uiState
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (arSession != null) {
                ArView(arSession, Modifier.fillMaxSize())
            }

            ShotControls(
                uiState = uiState,
                onEvent = onEvent,
                onMenuClick = {
                    scope.launch {
                        drawerState.open()
                    }
                }
            )

            if (uiState.isHelpDialogVisible) {
                HelpDialog(onDismiss = { onEvent(UiEvent.ToggleHelpDialog) })
            }
        }
    }
}

@Preview
@Composable
private fun MainScreenPreview() {
    CueDetatTheme {
        MainScreen(uiState = UiState(), onEvent = {}, arSession = null)
    }
}
