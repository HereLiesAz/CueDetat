// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/MainScreen.kt

package com.hereliesaz.cuedetat.ui

import android.graphics.PointF
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hereliesaz.cuedetat.data.CalibrationAnalyzer
import com.hereliesaz.cuedetat.ui.composables.CameraBackground
import com.hereliesaz.cuedetat.ui.composables.CuedetatButton
import com.hereliesaz.cuedetat.ui.composables.MenuDrawerContent
import com.hereliesaz.cuedetat.ui.composables.SpinControl
import com.hereliesaz.cuedetat.ui.composables.TopControls
import com.hereliesaz.cuedetat.ui.composables.ZoomControls
import com.hereliesaz.cuedetat.ui.composables.calibration.CalibrationScreen
import com.hereliesaz.cuedetat.ui.composables.calibration.CalibrationViewModel
import com.hereliesaz.cuedetat.ui.composables.dialogs.AdvancedOptionsDialog
import com.hereliesaz.cuedetat.ui.composables.dialogs.GlowStickDialog
import com.hereliesaz.cuedetat.ui.composables.dialogs.LuminanceAdjustmentDialog
import com.hereliesaz.cuedetat.ui.composables.dialogs.TableSizeSelectionDialog
import com.hereliesaz.cuedetat.ui.composables.overlays.KineticWarningOverlay
import com.hereliesaz.cuedetat.ui.composables.overlays.TutorialOverlay
import com.hereliesaz.cuedetat.ui.composables.quickalign.QuickAlignScreen
import com.hereliesaz.cuedetat.ui.composables.quickalign.QuickAlignViewModel
import com.hereliesaz.cuedetat.ui.composables.sliders.TableRotationSlider
import com.hereliesaz.cuedetat.ui.hatemode.HaterScreen
import com.hereliesaz.cuedetat.ui.hatemode.HaterViewModel
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme
import com.hereliesaz.cuedetat.ui.theme.LightBlue400
import com.hereliesaz.cuedetat.view.ProtractorOverlay
import com.hereliesaz.cuedetat.view.gestures.detectManualGestures
import com.hereliesaz.cuedetat.view.state.ExperienceMode
import com.hereliesaz.cuedetat.view.state.ToastMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    calibrationViewModel: CalibrationViewModel,
    quickAlignViewModel: QuickAlignViewModel,
    haterViewModel: HaterViewModel,
    calibrationAnalyzer: CalibrationAnalyzer
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val toastMessage by mainViewModel.toastMessage.collectAsState()
    val context = LocalContext.current
    var isDrawerOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isSystemDark = isSystemInDarkTheme()

    val useDarkTheme = when (uiState.isForceLightMode) {
        true -> false
        false -> true
        null -> isSystemDark
    }

    LaunchedEffect(quickAlignViewModel) {
        mainViewModel.listenToQuickAlign(quickAlignViewModel)
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
                mainViewModel.onEvent(MainScreenEvent.ToastShown)
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
                mainViewModel.onEvent(MainScreenEvent.ClearSpinState)
            } else {
                alphaAnimatable.snapTo(1.0f)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {

            // --- UI ROUTER ---
            if (uiState.experienceMode == ExperienceMode.HATER) {
                HaterScreen(viewModel = haterViewModel)
            } else { // EXPERT and BEGINNER modes share the main UI
                when {
                    uiState.showCalibrationScreen -> {
                        CalibrationScreen(
                            uiState = uiState,
                            onEvent = mainViewModel::onEvent,
                            viewModel = calibrationViewModel,
                            analyzer = calibrationAnalyzer
                        )
                    }

                    uiState.showQuickAlignScreen -> {
                        QuickAlignScreen(
                            uiState = uiState,
                            analyzer = mainViewModel.visionAnalyzer,
                            onEvent = mainViewModel::onEvent,
                            viewModel = quickAlignViewModel
                        )
                    }

                    else -> {
                        val mainBoxModifier = Modifier
                            .fillMaxSize()
                            .pointerInput(uiState.isCalibratingColor, uiState.isTestingCvMask) {
                                detectTapGestures { offset ->
                                    if (uiState.isCalibratingColor) {
                                        mainViewModel.onEvent(
                                            MainScreenEvent.SampleColorAt(
                                                offset
                                            )
                                        )
                                    } else if (uiState.isTestingCvMask) {
                                        mainViewModel.onEvent(MainScreenEvent.ExitCvMaskTestMode)
                                    }
                                }
                            }

                        Box(modifier = mainBoxModifier) {
                            if (uiState.isCameraVisible) {
                                CameraBackground(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .zIndex(0f),
                                    analyzer = mainViewModel.visionAnalyzer
                                )
                            }

                            ProtractorOverlay(
                                uiState = uiState.copy(spinPathsAlpha = alphaAnimatable.value),
                                systemIsDark = useDarkTheme,
                                isTestingCvMask = uiState.isTestingCvMask,
                                onEvent = mainViewModel::onEvent,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .detectManualGestures(uiState, mainViewModel::onEvent)
                                    .zIndex(1f)
                            )

                            if (uiState.isCalibratingColor) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp)
                                        .zIndex(6f),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Aim at the table felt",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Crosshair",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Tap to sample color",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // The main UI controls are only visible if not in a special CV mode
                            if (!uiState.isTestingCvMask && !uiState.isCalibratingColor) {
                                val spinControlCenter = uiState.spinControlCenter
                                if (uiState.isSpinControlVisible && spinControlCenter != null) {
                                    val spinControlSizeDp = 120.dp
                                    val spinControlSizePx =
                                        with(LocalDensity.current) { spinControlSizeDp.toPx() }

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
                                        onEvent = mainViewModel::onEvent
                                    )
                                }

                                ZoomControls(
                                    uiState = uiState,
                                    onEvent = mainViewModel::onEvent,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .fillMaxHeight(0.6f)
                                        .width(60.dp)
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
                                        if (uiState.experienceMode == ExperienceMode.EXPERT) {
                                            CuedetatButton(
                                                onClick = { mainViewModel.onEvent(MainScreenEvent.AddObstacleBall) },
                                                text = "Add\nBall",
                                                color = LightBlue400
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(80.dp)
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (uiState.table.isVisible) {
                                            TableRotationSlider(
                                                uiState = uiState,
                                                onEvent = mainViewModel::onEvent
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.padding(end = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (uiState.experienceMode == ExperienceMode.EXPERT) {
                                            CuedetatButton(
                                                onClick = { mainViewModel.onEvent(MainScreenEvent.ToggleSpinControl) },
                                                text = "Spin",
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                        when {
                                            uiState.experienceMode == ExperienceMode.EXPERT -> CuedetatButton(
                                                onClick = { mainViewModel.onEvent(MainScreenEvent.Reset) },
                                                text = "Reset\nView",
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.7f
                                                )
                                            )

                                            uiState.experienceMode == ExperienceMode.BEGINNER && uiState.isBeginnerViewLocked -> CuedetatButton(
                                                onClick = { mainViewModel.onEvent(MainScreenEvent.UnlockBeginnerView) },
                                                text = "Unlock\nView",
                                                color = MaterialTheme.colorScheme.primary
                                            )

                                            uiState.experienceMode == ExperienceMode.BEGINNER && !uiState.isBeginnerViewLocked -> CuedetatButton(
                                                onClick = { mainViewModel.onEvent(MainScreenEvent.LockBeginnerView) },
                                                text = "Lock\nView",
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.7f
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            KineticWarningOverlay(
                                text = uiState.warningText,
                                modifier = Modifier.zIndex(3f)
                            )

                            ExperienceModeSelectionDialog(
                                uiState = uiState,
                                onEvent = mainViewModel::onEvent
                            )

                            LuminanceAdjustmentDialog(
                                uiState = uiState,
                                onEvent = mainViewModel::onEvent,
                                onDismiss = { mainViewModel.onEvent(MainScreenEvent.ToggleLuminanceDialog) }
                            )

                            GlowStickDialog(
                                uiState = uiState,
                                onEvent = mainViewModel::onEvent,
                                onDismiss = { mainViewModel.onEvent(MainScreenEvent.ToggleGlowStickDialog) }
                            )

                            TableSizeSelectionDialog(
                                uiState = uiState,
                                onEvent = mainViewModel::onEvent,
                                onDismiss = { mainViewModel.onEvent(MainScreenEvent.ToggleTableSizeDialog) }
                            )

                            AdvancedOptionsDialog(
                                uiState = uiState,
                                onEvent = mainViewModel::onEvent,
                                onDismiss = { mainViewModel.onEvent(MainScreenEvent.ToggleAdvancedOptionsDialog) }
                            )

                            TutorialOverlay(
                                uiState = uiState,
                                onEvent = mainViewModel::onEvent
                            )
                        }
                    }
                }
            }

            // --- Global UI Elements (Visible in all modes) ---
            TopControls(
                uiState = uiState,
                onEvent = mainViewModel::onEvent,
                onMenuClick = { scope.launch { isDrawerOpen = true } },
                modifier = Modifier.zIndex(10f)
            )

            AnimatedVisibility(
                visible = isDrawerOpen,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { isDrawerOpen = false }
                        .zIndex(20f)
                )
            }

            AnimatedVisibility(
                visible = isDrawerOpen,
                modifier = Modifier.zIndex(21f),
                enter = expandVertically(
                    animationSpec = tween(durationMillis = 400),
                    expandFrom = Alignment.Top
                ),
                exit = slideOutHorizontally(
                    animationSpec = tween(durationMillis = 300),
                    targetOffsetX = { -it }
                ) + shrinkVertically(animationSpec = tween(durationMillis = 300))
            ) {
                MenuDrawerContent(
                    uiState = uiState,
                    onEvent = mainViewModel::onEvent,
                    onCloseDrawer = {
                        isDrawerOpen = false
                        mainViewModel.onEvent(MainScreenEvent.MenuClosed)
                    }
                )
            }
        }
    }
}

@Composable
fun ExperienceModeSelectionDialog(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit
) {
    if (uiState.showExperienceModeSelection) {
        AlertDialog(
            onDismissRequest = { onEvent(MainScreenEvent.ToggleExperienceModeSelection) },
            title = { Text("Select Experience Mode", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExperienceMode.entries.forEach { mode ->
                        TextButton(
                            onClick = {
                                onEvent(MainScreenEvent.SetExperienceMode(mode))
                                onEvent(MainScreenEvent.ToggleExperienceModeSelection)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = mode.name.lowercase().replaceFirstChar { it.titlecase() },
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onEvent(MainScreenEvent.ToggleExperienceModeSelection) }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}