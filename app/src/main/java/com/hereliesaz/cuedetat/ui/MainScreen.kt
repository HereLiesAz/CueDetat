package com.hereliesaz.cuedetat.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment // Standard Alignment import
import androidx.compose.ui.BiasAlignment // <<<<<<< ADD THIS IMPORT
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.hereliesaz.cuedetat.ui.composables.CameraBackground
import com.hereliesaz.cuedetat.ui.composables.KineticWarningOverlay
import com.hereliesaz.cuedetat.ui.composables.MenuDrawerContent
import com.hereliesaz.cuedetat.ui.composables.ResetFab
import com.hereliesaz.cuedetat.ui.composables.ToggleCueBallFab
import com.hereliesaz.cuedetat.ui.composables.TopControls
import com.hereliesaz.cuedetat.ui.composables.ZoomControls
import com.hereliesaz.cuedetat.view.ProtractorOverlayView
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.view.state.ToastMessage
import kotlinx.coroutines.launch

@Composable
fun TableRotationSlider( /* ... (no changes from previous version) ... */
                         uiState: OverlayState,
                         onEvent: (MainScreenEvent) -> Unit,
                         modifier: Modifier = Modifier
) {
    if (uiState.isBankingMode) {
        val sliderColors = SliderDefaults.colors(
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTickColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            inactiveTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Table Rotation: ${uiState.tableRotationDegrees.toInt()}°",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Slider(
                value = uiState.tableRotationDegrees,
                onValueChange = { onEvent(MainScreenEvent.TableRotationChanged(it)) },
                valueRange = 0f..359f,
                steps = 358,
                modifier = Modifier.fillMaxWidth(),
                colors = sliderColors
            )
        }
    }
}

@Composable
fun LuminanceAdjustmentDialog( /* ... (no changes from previous version) ... */
                               uiState: OverlayState,
                               onEvent: (MainScreenEvent) -> Unit,
                               onDismiss: () -> Unit
) {
    if (uiState.showLuminanceDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Adjust Drawn Elements Luminance", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            text = {
                Column {
                    Text(
                        "Current: ${"%.2f".format(uiState.luminanceAdjustment)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = uiState.luminanceAdjustment,
                        onValueChange = { onEvent(MainScreenEvent.AdjustLuminance(it)) },
                        valueRange = -0.4f..0.4f,
                        steps = 79,
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            thumbColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Done", color = MaterialTheme.colorScheme.primary) }
            }
        )
    }
}

@Composable
fun TutorialOverlay( /* ... (no changes from previous version) ... */
                     uiState: OverlayState,
                     tutorialMessages: List<String>,
                     onEvent: (MainScreenEvent) -> Unit
) {
    if (uiState.showTutorialOverlay && uiState.currentTutorialStep < tutorialMessages.size) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f))
                .clickable(onClick = { /* Consume clicks */ }),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                    .padding(24.dp)
            ) {
                Text(
                    text = tutorialMessages[uiState.currentTutorialStep],
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row {
                    if (uiState.currentTutorialStep > 0) {
                        TextButton(onClick = { /* TODO: Implement PreviousTutorialStep if desired */ }) {
                            Text("Previous", color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    TextButton(onClick = {
                        if (uiState.currentTutorialStep < tutorialMessages.size - 1) {
                            onEvent(MainScreenEvent.NextTutorialStep)
                        } else {
                            onEvent(MainScreenEvent.EndTutorial)
                        }
                    }) {
                        Text(
                            if (uiState.currentTutorialStep < tutorialMessages.size - 1) "Next" else "Got it!",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val protractorView = remember { ProtractorOverlayView(context) }
    val systemIsDark = isSystemInDarkTheme()

    val tutorialMessages = remember {
        listOf(
            "Welcome to Cue D'état!\nTap 'Next' to learn the basics.",
            "PROTRACTOR MODE:\nDrag the Target Ball (center circle) to aim for cut shots.",
            "Rotate the Protractor: Single finger drag left/right (not on a ball).",
            "Zoom View: Pinch to zoom in or out.",
            "Optional Aiming Ball: Toggle with bottom-left FAB to visualize shots from a specific spot.",
            "BANKING MODE:\nSelect 'Calculate Bank' from menu. Table appears.",
            "Drag the Cue Ball on table. Drag elsewhere on screen to set your aim line for bank shots.",
            "Table Rotation: Use bottom slider. Zoom: Use side slider.",
            "Menu: Explore for theme options (for drawn lines), luminance, and this tutorial!"
        )
    }

    val appControlColorScheme = MaterialTheme.colorScheme
    LaunchedEffect(appControlColorScheme) {
        viewModel.onEvent(MainScreenEvent.ThemeChanged(appControlColorScheme))
    }

    LaunchedEffect(toastMessage) { /* ... (no change) ... */ }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            MenuDrawerContent(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraBackground(modifier = Modifier.fillMaxSize().zIndex(0f))

            AndroidView(
                factory = {
                    protractorView.apply {
                        onSizeChanged = { w, h -> viewModel.onEvent(MainScreenEvent.SizeChanged(w, h)) }
                        onProtractorRotationChange = { rot -> viewModel.onEvent(MainScreenEvent.RotationChanged(rot)) }
                        onProtractorUnitMoved = { pos -> viewModel.onEvent(MainScreenEvent.UnitMoved(pos)) }
                        onActualCueBallScreenMoved = { pos -> viewModel.onEvent(MainScreenEvent.ActualCueBallMoved(pos)) }
                        onScale = { scaleFactor -> viewModel.onEvent(MainScreenEvent.ZoomScaleChanged(scaleFactor)) }
                        onGestureStarted = { viewModel.onEvent(MainScreenEvent.GestureStarted) }
                        onGestureEnded = { viewModel.onEvent(MainScreenEvent.GestureEnded) }
                        onBankingAimTargetScreenDrag = { screenPoint -> viewModel.onEvent(MainScreenEvent.BankingAimTargetDragged(screenPoint)) }
                    }
                },
                modifier = Modifier.fillMaxSize().zIndex(1f),
                update = { view -> view.updateState(uiState, systemIsDark) }
            )

            TopControls(uiState = uiState, onMenuClick = { scope.launch { drawerState.open() } }, modifier = Modifier.zIndex(2f))
            ZoomControls(uiState = uiState, onEvent = viewModel::onEvent, modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(0.4f).zIndex(2f))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f),
                contentAlignment = BiasAlignment(horizontalBias = 0f, verticalBias = 0.5f) // Corrected: Use BiasAlignment directly
            ) {
                TableRotationSlider(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    modifier = Modifier
                        .padding(horizontal = 72.dp)
                        .navigationBarsPadding()
                )
            }

            if (!uiState.isBankingMode) {
                ToggleCueBallFab(uiState = uiState, onEvent = { viewModel.onEvent(MainScreenEvent.ToggleActualCueBall) }, modifier = Modifier.align(Alignment.BottomStart).zIndex(2f))
            }
            ResetFab(uiState = uiState, onEvent = viewModel::onEvent, modifier = Modifier.align(Alignment.BottomEnd).zIndex(2f))
            KineticWarningOverlay(text = uiState.warningText, modifier = Modifier.zIndex(3f))
            LuminanceAdjustmentDialog(uiState = uiState, onEvent = viewModel::onEvent, onDismiss = { viewModel.onEvent(MainScreenEvent.ToggleLuminanceDialog) })
            TutorialOverlay(uiState = uiState, tutorialMessages = tutorialMessages, onEvent = viewModel::onEvent)
        }
    }
}