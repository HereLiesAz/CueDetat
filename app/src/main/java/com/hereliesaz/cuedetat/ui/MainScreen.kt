// app/src/main/java/com/hereliesaz/cuedetat/ui/MainScreen.kt
package com.hereliesaz.cuedetat.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hereliesaz.cuedetat.ui.composables.*
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme
import com.hereliesaz.cuedetat.view.ProtractorOverlay
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.view.state.ToastMessage
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isSystemDark = isSystemInDarkTheme()

    val useDarkTheme = when (uiState.isForceLightMode) {
        true -> false
        false -> true
        null -> isSystemDark
    }

    CueDetatTheme(darkTheme = useDarkTheme) {
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
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                CameraBackground(modifier = Modifier.fillMaxSize().zIndex(0f))

                ProtractorOverlay(
                    uiState = uiState,
                    systemIsDark = useDarkTheme,
                    onEvent = viewModel::onEvent,
                    modifier = Modifier.fillMaxSize().zIndex(1f)
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
                        .fillMaxHeight(0.6f) // Slider container is 60% of screen height
                        .padding(end = 16.dp)
                        .width(90.dp)
                        .zIndex(5f)
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .zIndex(2f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (!uiState.isBankingMode) {
                        ToggleCueBallFab(
                            uiState = uiState,
                            onEvent = { viewModel.onEvent(MainScreenEvent.ToggleOnPlaneBall) }
                        )
                    } else {
                        Spacer(Modifier.size(40.dp)) // Mini FAB size is 40dp
                    }

                    Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                        TableRotationSlider(
                            uiState = uiState,
                            onEvent = viewModel::onEvent
                        )
                    }

                    ResetFab(
                        uiState = uiState,
                        onEvent = viewModel::onEvent
                    )
                }


                KineticWarningOverlay(text = uiState.warningText, modifier = Modifier.zIndex(3f))
                LuminanceAdjustmentDialog(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    onDismiss = { viewModel.onEvent(MainScreenEvent.ToggleLuminanceDialog) }
                )
                TutorialOverlay(
                    uiState = uiState,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}

@Composable
fun TableRotationSlider(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.showTable) {
        val sliderColors = SliderDefaults.colors(
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            thumbColor = Color.Yellow
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
                valueRange = -179f..180f, // Centered range
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f),
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
    onEvent: (MainScreenEvent) -> Unit
) {
    val tutorialSteps = remember {
        listOf(
            "Welcome. This is not a toy. It is a tool for geometric enlightenment. Pay attention.",
            "This is Protractor Mode. Use it for cut shots. The elements you see are ghosts in the machine. They live on a logical plane, projected onto your reality.",
            "Drag the 'T' (Target Ball) to align it with your object ball. Drag with one finger to rotate the aiming line to the pocket.",
            "This optional 'A' (Actual Cue Ball) can be dragged to match your real cue ball's position. The line from 'A' to 'G' (Ghost Cue Ball) is the path you must shoot.",
            "The vertical slider on the right controls zoom. The slider on the bottom (in Banking Mode) rotates the table.",
            "Open the menu to find more tools. 'Calculate Bank' switches to Banking Mode, where you can visualize multi-rail shots.",
            "You have been instructed. Now, go and sin no more. Or at least, sin with better geometry. Press Finish."
        )
    }

    if (uiState.showTutorialOverlay) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f))
                .clickable(enabled = true, onClick = {}) // Block clicks
                .zIndex(10f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = tutorialSteps.getOrNull(uiState.currentTutorialStep) ?: "The tutorial is over. Go away.",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(24.dp))
                Row {
                    if (uiState.currentTutorialStep < tutorialSteps.lastIndex) {
                        TextButton(onClick = { onEvent(MainScreenEvent.NextTutorialStep) }) {
                            Text("Next")
                        }
                    }
                    TextButton(onClick = { onEvent(MainScreenEvent.EndTutorial) }) {
                        Text("Finish")
                    }
                }
            }
        }
    }
}