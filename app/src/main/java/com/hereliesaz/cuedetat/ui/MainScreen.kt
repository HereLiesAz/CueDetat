// app/src/main/java/com/hereliesaz/cuedetat/ui/MainScreen.kt
package com.hereliesaz.cuedetat.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
fun TableRotationSlider(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isBankingMode) {
        val sliderColors = SliderDefaults.colors(
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            thumbColor = MaterialTheme.colorScheme.primary
        )
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 80.dp)
                .padding(bottom = 32.dp)
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
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val protractorView = remember {
        ProtractorOverlayView(context).apply {
            onSizeChanged = { w, h -> viewModel.onEvent(MainScreenEvent.SizeChanged(w, h)) }
            // Corrected assignments for ProtractorOverlayView callbacks:
            onProtractorRotationChange =
                { rot -> viewModel.onEvent(MainScreenEvent.RotationChanged(rot)) }
            onProtractorUnitMoved = { pos -> viewModel.onEvent(MainScreenEvent.UnitMoved(pos)) }
            onActualCueBallScreenMoved =
                { pos -> viewModel.onEvent(MainScreenEvent.ActualCueBallMoved(pos)) }
            onScale =
                { scaleFactor -> viewModel.onEvent(MainScreenEvent.ZoomScaleChanged(scaleFactor)) }
            onGestureStarted = { viewModel.onEvent(MainScreenEvent.GestureStarted) }
            onGestureEnded = { viewModel.onEvent(MainScreenEvent.GestureEnded) }
            onBankingAimTargetScreenDrag = { screenPoint ->
                viewModel.onEvent(
                    MainScreenEvent.BankingAimTargetDragged(screenPoint)
                )
            }
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    LaunchedEffect(colorScheme) {
        viewModel.onEvent(MainScreenEvent.ThemeChanged(colorScheme))
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
            CameraBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
            )

            AndroidView(
                factory = { protractorView },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            ) { view ->
                view.updateState(uiState)
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
                    .zIndex(2f)
            )

            TableRotationSlider(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(2f)
            )

            if (!uiState.isBankingMode) {
                ToggleCueBallFab(
                    uiState = uiState,
                    onEvent = { viewModel.onEvent(MainScreenEvent.ToggleActualCueBall) }, // Pass correct event
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

            KineticWarningOverlay(
                text = uiState.warningText,
                modifier = Modifier.zIndex(3f)
            )
        }
    }
}