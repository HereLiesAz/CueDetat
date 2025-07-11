// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/MainScreen.kt

package com.hereliesaz.cuedetat.ui

import android.graphics.PointF
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hereliesaz.cuedetat.ui.composables.*
import com.hereliesaz.cuedetat.ui.composables.dialogs.CvTuningDialog
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
import kotlin.math.roundToInt

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
        val alphaAnimatable = remember { Animatable(1.0f) }

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

        LaunchedEffect(uiState.lingeringSpinOffset) {
            if (uiState.lingeringSpinOffset != null) {
                alphaAnimatable.snapTo(1.0f)
                delay(5000L)
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
                    CameraBackground(
                        modifier = Modifier.fillMaxSize().zIndex(0f),
                        analyzer = viewModel.visionAnalyzer
                    )
                }

                ProtractorOverlay(
                    uiState = uiState.copy(spinPathsAlpha = alphaAnimatable.value),
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
                if (uiState.isSpinControlVisible && spinControlCenter != null) {
                    val spinControlSizeDp = 120.dp
                    val spinControlSizePx = with(LocalDensity.current) { spinControlSizeDp.toPx() }

                    SpinControl(
                        modifier = Modifier
                            .zIndex(5f)
                            .offset {
                                IntOffset(
                                    (spinControlCenter.x - spinControlSizePx / 2).roundToInt(),
                                    (spinControlCenter.y - spinControlSizePx / 2).roundToInt()
                                )
                            },
                        centerPosition = PointF(0f, 0f),
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
                        .padding(bottom = 8.dp)
                        .zIndex(2f),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.padding(start = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LockColorFab(
                            isColorLocked = uiState.lockedHsvColor != null,
                            onEvent = viewModel::onEvent
                        )
                        ToggleSpinControlFab(
                            uiState = uiState,
                            onEvent = viewModel::onEvent
                        )
                        AddObstacleBallFab(onEvent = viewModel::onEvent)
                        if (!uiState.isBankingMode) {
                            ToggleCueBallFab(
                                uiState = uiState,
                                onEvent = viewModel::onEvent
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.showTable) {
                            TableRotationSlider(
                                uiState = uiState,
                                onEvent = viewModel::onEvent
                            )
                        } else if (!uiState.isBankingMode) {
                            ToggleTableFab(
                                onEvent = viewModel::onEvent
                            )
                        }
                    }

                    ResetFab(
                        uiState = uiState,
                        onEvent = viewModel::onEvent,
                        modifier = Modifier.padding(end = 16.dp)
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

                CvTuningDialog(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    onDismiss = { viewModel.onEvent(MainScreenEvent.ToggleCvTuningDialog) }
                )

                TutorialOverlay(
                    uiState = uiState,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}