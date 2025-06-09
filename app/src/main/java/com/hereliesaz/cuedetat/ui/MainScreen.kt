package com.hereliesaz.cuedetat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme
import com.hereliesaz.cuedetat.view.ProtractorOverlayView
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlinx.coroutines.launch

sealed class MenuAction {
    object ToggleHelp : MenuAction()
    object CheckForUpdate : MenuAction()
    object ViewArt : MenuAction()
    object AdaptTheme : MenuAction()
    object ResetTheme : MenuAction()
}

@ExperimentalMaterial3ExpressiveApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: OverlayState,
    protractorView: ProtractorOverlayView,
    onZoomChange: (Float) -> Unit,
    onMenuAction: (MenuAction) -> Unit,
    cameraPreview: @Composable () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        cameraPreview()

        AndroidView({ protractorView }, modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            TopControls(onMenuClick = { showBottomSheet = true })
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.systemBars.asPaddingValues())
            ) {
                // ZOOM CONTROLS
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .height(486.dp)
                        .width(55.dp)
                        .padding(end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_zoom_in_24),
                        contentDescription = stringResource(id = R.string.zoom_icon),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // MODIFIED: Replaced with the native VerticalSlider component
                    val sliderState = rememberSliderState(valueRange = 0.1f..4.0f)

                    // This effect syncs the slider's state FROM the ViewModel's state
                    LaunchedEffect(uiState.zoomFactor) {
                        sliderState.value = uiState.zoomFactor
                    }

                    // This sets the callback to update the ViewModel FROM the slider's state
                    sliderState.onValueChange = { newValue ->
                        onZoomChange(newValue)
                    }

                    VerticalSlider(
                        state = sliderState,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            thumbColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                // RESET FAB
                FloatingActionButton(
                    onClick = { onMenuAction(MenuAction.ToggleHelp) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp),
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_undo_24),
                        contentDescription = stringResource(id = R.string.reset_view),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_launcher_monochrome),
                        contentDescription = "An omen.",
                        modifier = Modifier
                            .height(120.dp)
                            .padding(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "And then what?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    MenuButton(text = "Adapt Theme to View") {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                                onMenuAction(MenuAction.AdaptTheme)
                            }
                        }
                    }
                    MenuButton(text = "Reset Theme") {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                                onMenuAction(MenuAction.ResetTheme)
                            }
                        }
                    }
                    MenuButton(text = "Toggle Helper Text") {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                                onMenuAction(MenuAction.ToggleHelp)
                            }
                        }
                    }
                    MenuButton(text = "See My Art") {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                                onMenuAction(MenuAction.ViewArt)
                            }
                        }
                    }
                    MenuButton(text = "Check for Updates") {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                                onMenuAction(MenuAction.CheckForUpdate)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text)
    }
}

@Composable
fun TopControls(onMenuClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, end = 16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.MoreHoriz,
            contentDescription = "Options",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clickable(onClick = onMenuClick)
                .padding(8.dp)
        )
    }
}


//================================================================================
// PREVIEWS FOR INDIVIDUAL COMPONENTS
//================================================================================

@Preview(showBackground = true, backgroundColor = 0xFF1E1B16)
@Composable
fun TopControlsPreview() {
    CueDetatTheme {
        TopControls(onMenuClick = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1E1B16)
@Composable
fun MenuButtonPreview() {
    CueDetatTheme {
        MenuButton(text = "Check for Updates", onClick = {})
    }
}

@Preview(name = "Bottom Sheet Content", showBackground = true)
@Composable
fun MainScreenBottomSheetPreview() {
    CueDetatTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_launcher_monochrome),
                contentDescription = "An omen.",
                modifier = Modifier
                    .height(120.dp)
                    .padding(16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "And then what?",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            MenuButton(text = "Adapt Theme to View") {}
            MenuButton(text = "Reset Theme") {}
            MenuButton(text = "Toggle Helper Text") {}
            MenuButton(text = "See My Art") {}
            MenuButton(text = "Check for Updates") {}
        }
    }
}