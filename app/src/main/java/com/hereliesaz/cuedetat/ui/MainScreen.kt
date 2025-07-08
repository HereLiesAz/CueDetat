// app/src/main/java/com/hereliesaz/cuedetat/ui/MainScreen.kt
package com.hereliesaz.cuedetat.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.zIndex
import com.hereliesaz.cuedetat.ui.composables.CameraBackground
import com.hereliesaz.cuedetat.ui.composables.KineticWarningOverlay
import com.hereliesaz.cuedetat.ui.composables.MenuDrawerContent
import com.hereliesaz.cuedetat.ui.composables.ResetFab
import com.hereliesaz.cuedetat.ui.composables.ToggleCueBallFab
import com.hereliesaz.cuedetat.ui.composables.TopControls
import com.hereliesaz.cuedetat.ui.composables.ZoomControls
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
            Box(modifier = Modifier.fillMaxSize()) {
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
                        .fillMaxHeight(0.6f)
                        .padding(end = 8.dp)
                        .zIndex(5f)
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TableRotationSlider(
                        uiState = uiState,
                        onEvent = viewModel::onEvent,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .padding(bottom = if (uiState.isBankingMode) 8.dp else 0.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .navigationBarsPadding()
                        .zIndex(2f),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (!uiState.isBankingMode) {
                        ToggleCueBallFab(
                            uiState = uiState,
                            onEvent = { viewModel.onEvent(MainScreenEvent.ToggleActualCueBall) }
                        )
                    } else {
                        Spacer(Modifier)
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
    if (uiState.isBankingMode) {
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