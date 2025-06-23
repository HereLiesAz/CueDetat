// File: hereliesaz/cuedetat/CueDetat-0e79c22b0410d9f1a9f17c01c9d6c8962449499d/app/src/main/java/com/hereliesaz/cuedetat/ui/MainScreen.kt
package com.hereliesaz.cuedetat.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme // Keep for passing to view.updateState
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
import androidx.compose.ui.Alignment
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
// import com.hereliesaz.cuedetat.ui.theme.AccentGold // Use MaterialTheme.colorScheme.primary
import com.hereliesaz.cuedetat.view.ProtractorOverlayView
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.view.state.ToastMessage
import kotlinx.coroutines.launch

@Composable
fun TableRotationSlider( /* ... (no change) ... */
                         uiState: OverlayState,
                         onEvent: (MainScreenEvent) -> Unit,
                         modifier: Modifier = Modifier
) {
    if (uiState.isBankingMode) {
        val sliderColors = SliderDefaults.colors(
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            thumbColor = Color.Yellow // Changed thumb color to yellow
        )
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 80.dp)
                .padding(bottom = 120.dp) // Adjusted padding to move it up
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
fun LuminanceAdjustmentDialog( /* ... (no change) ... */
                               uiState: OverlayState,
                               onEvent: (MainScreenEvent) -> Unit,
                               onDismiss: () -> Unit
) {
    if (uiState.showLuminanceDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    "Adjust Drawn Elements Luminance",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
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
                TextButton(onClick = onDismiss) {
                    Text(
                        "Done",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }
}

@Composable
fun TutorialOverlay( /* ... (no change) ... */
                     uiState: OverlayState,
                     tutorialMessages: List<String>,
                     onEvent: (MainScreenEvent) -> Unit
) {
    if (uiState.showTutorialOverlay && uiState.currentTutorialStep < tutorialMessages.size) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f))
                .clickable(onClick = { /* Consume clicks */ })
                .zIndex(5f), // Brought to the forefront
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.shapes.medium
                    )
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
    val systemIsDark = isSystemInDarkTheme() // Get system dark mode status here

    // This LaunchedEffect is for initially dispatching the app's control theme to the ViewModel
    val appControlColorScheme = MaterialTheme.colorScheme
    LaunchedEffect(appControlColorScheme) {
        viewModel.onEvent(MainScreenEvent.ThemeChanged(appControlColorScheme))
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            val messageText = when (it) {
                is ToastMessage.StringResource -> context.getString(
                    it.id,
                    *it.formatArgs.toTypedArray()
                )
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
            CameraBackground(modifier = Modifier
                .fillMaxSize()
                .zIndex(0f))

            AndroidView(
                factory = {
                    protractorView.apply {
                        onSizeChanged =
                            { w, h -> viewModel.onEvent(MainScreenEvent.SizeChanged(w, h)) }
                        onProtractorRotationChange =
                            { rot -> viewModel.onEvent(MainScreenEvent.RotationChanged(rot)) }
                        onProtractorUnitMoved =
                            { pos -> viewModel.onEvent(MainScreenEvent.UnitMoved(pos)) }
                        onActualCueBallScreenMoved =
                            { pos -> viewModel.onEvent(MainScreenEvent.ActualCueBallMoved(pos)) }
                        onScale = { scaleFactor ->
                            viewModel.onEvent(
                                MainScreenEvent.ZoomScaleChanged(scaleFactor)
                            )
                        }
                        onGestureStarted = { viewModel.onEvent(MainScreenEvent.GestureStarted) }
                        onGestureEnded = { viewModel.onEvent(MainScreenEvent.GestureEnded) }
                        onBankingAimTargetScreenDrag = { screenPoint ->
                            viewModel.onEvent(
                                MainScreenEvent.BankingAimTargetDragged(screenPoint)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f),
                update = { view ->
                    // Pass the latest uiState and the current system dark mode status
                    // The systemIsDark value is captured from the Composable scope above.
                    view.updateState(uiState, systemIsDark)
                }
            )

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
                    .zIndex(5f) // Brought to the forefront
            )
            TableRotationSlider(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(5f) // Brought to the forefront
            )

            if (!uiState.isBankingMode) {
                ToggleCueBallFab(
                    uiState = uiState,
                    onEvent = { viewModel.onEvent(MainScreenEvent.ToggleActualCueBall) },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .zIndex(2f)
                )
            }
            ResetFab(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .zIndex(2f)
            )
            KineticWarningOverlay(text = uiState.warningText, modifier = Modifier.zIndex(3f))
            LuminanceAdjustmentDialog(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onDismiss = { viewModel.onEvent(MainScreenEvent.ToggleLuminanceDialog) })

            val tutorialMessages = remember { // Defined here or pass from ViewModel
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
            TutorialOverlay(
                uiState = uiState,
                tutorialMessages = tutorialMessages,
                onEvent = viewModel::onEvent
            )
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
        onClick = { onEvent(MainScreenEvent.ToggleSpatialLock) },
        modifier = modifier
            .padding(bottom = 16.dp) // Standard FAB padding
            .navigationBarsPadding(), // Respect navigation bar
        containerColor = if (uiState.isSpatiallyLocked) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (uiState.isSpatiallyLocked) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (uiState.isSpatiallyLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                contentDescription = if (uiState.isSpatiallyLocked) "Unlock Spatial Position" else "Lock Spatial Position"
            )
            // Text can be added here if desired, e.g., " Lock" / " Unlk"
            // if (uiState.areHelpersVisible || true) { // Always show text for this one for clarity
            //     Spacer(modifier = Modifier.width(8.dp))
            //     Text(if (uiState.isSpatiallyLocked) "Locked" else "Lock")
            // }
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

            // FABs Column for bottom alignment
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Align the whole column to bottom center
                    .fillMaxWidth()
                    .padding(bottom = 16.dp) // Padding for the column from screen bottom edge
                    .zIndex(2f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // TableRotationSlider will only appear if isBankingMode is true
                TableRotationSlider(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    // Modifier for TableRotationSlider itself if needed,
                    // e.g., to control its width within the Column
                    modifier = Modifier.fillMaxWidth(0.8f) // Example: take 80% of column width
                        .padding(bottom = 8.dp) // Space between slider and LockFab
                )

                LockFab( // Add the Lock FAB
                    uiState = uiState,
                    onEvent = viewModel::onEvent
                    // modifier for LockFab is handled internally for padding
                )
            }


            // This Row is for the bottom-start and bottom-end FABs
            Row(
                modifier = Modifier
                    .fillMaxSize() // Fill the whole screen to allow alignment to corners
                    .padding(16.dp) // Outer padding for the FABs from screen edges
                    .navigationBarsPadding()
                    .zIndex(2f),
                verticalAlignment = Alignment.Bottom
            ) {
                if (!uiState.isBankingMode) {
                    ToggleCueBallFab(
                        uiState = uiState,
                        onEvent = { viewModel.onEvent(MainScreenEvent.ToggleActualCueBall) }
                        // Modifier here would be for the FAB itself within the Row
                    )
                }
                Spacer(modifier = Modifier.weight(1f)) // Pushes ResetFab to the end
                ResetFab(
                    uiState = uiState,
                    onEvent = viewModel::onEvent
                    // Modifier here would be for the FAB itself within the Row
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