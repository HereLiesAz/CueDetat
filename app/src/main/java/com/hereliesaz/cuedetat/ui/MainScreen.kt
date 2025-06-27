package com.hereliesaz.cuedetat.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.hereliesaz.cuedetat.ui.composables.*
import com.hereliesaz.cuedetat.ui.overlay.ProtractorOverlayView
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.view.state.ToastMessage
import kotlinx.coroutines.launch

@Composable
fun TableRotationSlider(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isBankingMode) {
        val sliderColors = SliderDefaults.colors(
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            thumbColor = Color.Yellow
        )
        Column(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
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
fun LuminanceAdjustmentDialog(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    onDismiss: () -> Unit
) {
    if (uiState.showLuminanceDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Adjust Drawn Elements Luminance", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            text = {
                Column {
                    Text("Current: ${"%.2f".format(uiState.luminanceAdjustment)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = MaterialTheme.colorScheme.primary) } }
        )
    }
}

@Composable
fun TutorialOverlay(
    uiState: OverlayState,
    tutorialMessages: List<String>,
    onEvent: (MainScreenEvent) -> Unit
) {
    if (uiState.showTutorialOverlay && uiState.currentTutorialStep < tutorialMessages.size) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                .clickable(onClick = { /* Consume clicks */ })
                .zIndex(5f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
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
                        // TODO: Implement PreviousTutorialStep if desired
                    }
                    Spacer(modifier = Modifier.weight(1f))

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
fun LockFab(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = { onEvent(MainScreenEvent.ToggleSpatialLock(isLocked = !uiState.isSpatiallyLocked)) },
        modifier = modifier.navigationBarsPadding(),
        containerColor = if (uiState.isSpatiallyLocked) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (uiState.isSpatiallyLocked) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (uiState.isSpatiallyLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                contentDescription = if (uiState.isSpatiallyLocked) "Unlock Spatial Position" else "Lock Spatial Position"
            )
            if (uiState.areHelpersVisible) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (uiState.isSpatiallyLocked) "Locked" else "Lock")
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
    val systemIsDark = isSystemInDarkTheme()

    val density = LocalDensity.current.density
    LaunchedEffect(density) {
        val width = (context.resources.displayMetrics.widthPixels)
        val height = (context.resources.displayMetrics.heightPixels)
        viewModel.onEvent(MainScreenEvent.SizeChanged(width, height))
    }

    val appControlColorScheme = MaterialTheme.colorScheme
    LaunchedEffect(appControlColorScheme) {
        viewModel.onEvent(MainScreenEvent.ThemeChanged(appControlColorScheme))
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            val messageText = when (it) {
                is ToastMessage.StringResource -> context.getString(it.id, *it.formatArgs.toTypedArray())
                is ToastMessage.PlainText -> it.text
            }
            Toast.makeText(context, messageText, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(MainScreenEvent.ToastShown)
        }
    }

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
            if (uiState.isSpatiallyLocked) {
                ArCoreScene(
                    modifier = Modifier.fillMaxSize().zIndex(0f),
                    arSession = uiState.arSession,
                    uiState = uiState,
                    onEvent = viewModel::onEvent
                )
            } else {
                CameraPreview(modifier = Modifier.fillMaxSize().zIndex(0f))

                AndroidView(
                    factory = {
                        ProtractorOverlayView(it).apply {
                            onSizeChanged = { w, h -> viewModel.onEvent(MainScreenEvent.SizeChanged(w, h)) }
                            onProtractorRotationChange = { rot -> viewModel.onEvent(MainScreenEvent.RotationChanged(rot)) }
                            onScale = { scale -> viewModel.onEvent(MainScreenEvent.ZoomScaleChanged(scale)) }
                            onProtractorUnitMoved = { pos -> viewModel.onEvent(MainScreenEvent.UnitMoved(pos)) }
                            onActualCueBallScreenMoved = { pos -> viewModel.onEvent(MainScreenEvent.ActualCueBallMoved(pos)) }
                            onBankingAimTargetScreenDrag = { pos -> viewModel.onEvent(MainScreenEvent.BankingAimTargetDragged(pos)) }
                            onGestureStarted = { viewModel.onEvent(MainScreenEvent.GestureStarted) }
                            onGestureEnded = { viewModel.onEvent(MainScreenEvent.GestureEnded) }
                        }
                    },
                    update = { view ->
                        view.updateState(uiState, systemIsDark)
                    },
                    modifier = Modifier.fillMaxSize().zIndex(1f)
                )
            }

            TopControls(
                uiState = uiState,
                onMenuClick = { scope.launch { drawerState.open() } },
                modifier = Modifier.zIndex(2f)
            )
            ZoomControls(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(0.4f)
                    .padding(end = 8.dp)
                    .zIndex(5f)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .navigationBarsPadding()
                    .zIndex(2f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TableRotationSlider(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(bottom = if (uiState.isBankingMode) 8.dp else 0.dp)
                )

                LockFab(
                    uiState = uiState,
                    onEvent = viewModel::onEvent
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .navigationBarsPadding()
                    .zIndex(2f),
                verticalAlignment = Alignment.Bottom
            ) {
                if (!uiState.isBankingMode) {
                    ToggleCueBallFab(
                        uiState = uiState,
                        onEvent = { viewModel.onEvent(MainScreenEvent.ToggleActualCueBall) }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                ResetFab(
                    uiState = uiState,
                    onEvent = viewModel::onEvent
                )
            }

            KineticWarningOverlay(text = uiState.warningText, modifier = Modifier.zIndex(3f))
            LuminanceAdjustmentDialog(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onDismiss = { viewModel.onEvent(MainScreenEvent.ToggleLuminanceDialog) })

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
                    "LOCK BUTTON (Bottom Center):\nTap 'Lock' to fix elements in real space. Move your phone; they should stay put relative to the table. Tap 'Unlock' to adjust again.",
                    "Menu: Explore for theme options (for drawn lines), luminance, and this tutorial!"
                )
            }
            TutorialOverlay(
                uiState = uiState,
                tutorialMessages = tutorialMessages,
                onEvent = viewModel::onEvent
            )
        }
    }
}
