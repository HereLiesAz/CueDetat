// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/MainScreen.kt

package com.hereliesaz.cuedetat.ui

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hereliesaz.cuedetat.ui.composables.*
import com.hereliesaz.cuedetat.ui.composables.dialogs.GlowStickDialog
import com.hereliesaz.cuedetat.ui.composables.dialogs.LuminanceAdjustmentDialog
import com.hereliesaz.cuedetat.ui.composables.dialogs.TableSizeSelectionDialog
import com.hereliesaz.cuedetat.ui.composables.overlays.KineticWarningOverlay
import com.hereliesaz.cuedetat.ui.composables.overlays.TutorialOverlay
import com.hereliesaz.cuedetat.ui.composables.sliders.TableRotationSlider
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme
import com.hereliesaz.cuedetat.view.ProtractorOverlay
import com.hereliesaz.cuedetat.view.state.ToastMessage
import kotlinx.coroutines.delay
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

        val alphaAnimatable = remember { Animatable(1.0f) }
        LaunchedEffect(uiState.lingeringSpinOffset) {
            if (uiState.lingeringSpinOffset != null) {
                alphaAnimatable.snapTo(1.0f) // Reset alpha for new linger
                delay(5000L) // 5-second linger
                alphaAnimatable.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 5000)
                )
                viewModel.onEvent(MainScreenEvent.ClearSpinState)
            } else {
                alphaAnimatable.snapTo(1.0f)
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
                if (uiState.isCameraVisible) {
                    CameraBackground(modifier = Modifier.fillMaxSize().zIndex(0f))
                }

                ProtractorOverlay(
                    uiState = uiState.copy(spinPathsAlpha = alphaAnimatable.value), // Pass animated alpha
                    systemIsDark = useDarkTheme,
                    onEvent = viewModel::onEvent,
                    modifier = Modifier.fillMaxSize().zIndex(1f)
                )

                TopControls(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier.zIndex(2f)
                )

                val spinControlCenter = uiState.spinControlCenter
                if (!uiState.isBankingMode && uiState.isSpinControlVisible && spinControlCenter != null) {
                    SpinControl(
                        modifier = Modifier.zIndex(5f),
                        centerPosition = spinControlCenter,
                        selectedSpinOffset = uiState.selectedSpinOffset,
                        lingeringSpinOffset = uiState.lingeringSpinOffset,
                        spinPathAlpha = alphaAnimatable.value,
                        onEvent = viewModel::onEvent
                    )
                }


                ZoomControls(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(0.6f)
                        .padding(end = 16.dp)
                        .width(120.dp)
                        .zIndex(5f)
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .zIndex(2f),
                    verticalAlignment = Alignment.Bottom, // Use Bottom alignment
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (!uiState.isBankingMode) {
                        // --- THE RIGHTEOUS FIX ---
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            // Padding added to lift the entire column
                            modifier = Modifier.padding(bottom = 48.dp)
                        ) {
                            ToggleSpinControlFab(
                                uiState = uiState,
                                onEvent = { viewModel.onEvent(MainScreenEvent.ToggleSpinControl) }
                            )
                            // Spacer increased to add more distance between buttons
                            Spacer(modifier = Modifier.height(16.dp))
                            ToggleCueBallFab(
                                uiState = uiState,
                                onEvent = { viewModel.onEvent(MainScreenEvent.ToggleOnPlaneBall) }
                            )
                        }
                        // --- END FIX ---

                    } else {
                        Spacer(Modifier.size(40.dp))
                    }

                    Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                        if (uiState.showTable) {
                            TableRotationSlider(
                                uiState = uiState,
                                onEvent = viewModel::onEvent
                            )
                        } else if (!uiState.isBankingMode) {
                            ToggleTableFab(
                                onEvent = viewModel::onEvent,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
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

                GlowStickDialog(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    onDismiss = { viewModel.onEvent(MainScreenEvent.ToggleGlowStickDialog) }
                )

                TableSizeSelectionDialog(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    onDismiss = { viewModel.onEvent(MainScreenEvent.ToggleTableSizeDialog) }
                )

                TutorialOverlay(
                    uiState = uiState,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}