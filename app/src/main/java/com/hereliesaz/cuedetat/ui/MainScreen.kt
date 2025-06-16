// app/src/main/java/com/hereliesaz/cuedetat/ui/MainScreen.kt
package com.hereliesaz.cuedetat.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.hereliesaz.cuedetat.ui.composables.CameraBackground
import com.hereliesaz.cuedetat.ui.composables.KineticWarningOverlay
import com.hereliesaz.cuedetat.ui.composables.MenuBottomSheet
import com.hereliesaz.cuedetat.ui.composables.ResetFab
import com.hereliesaz.cuedetat.ui.composables.TableControls
import com.hereliesaz.cuedetat.ui.composables.ToggleCueBallFab
import com.hereliesaz.cuedetat.ui.composables.TopControls
import com.hereliesaz.cuedetat.ui.composables.ZoomControls
import com.hereliesaz.cuedetat.view.ProtractorOverlayView
import com.hereliesaz.cuedetat.view.state.ToastMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val context = LocalContext.current

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    val protractorView = remember {
        ProtractorOverlayView(context).apply {
            onSizeChanged = { w, h -> viewModel.onEvent(MainScreenEvent.SizeChanged(w, h)) }
            onRotationChange = { rot -> viewModel.onEvent(MainScreenEvent.RotationChanged(rot)) }
            onUnitMove = { pos -> viewModel.onEvent(MainScreenEvent.UnitMoved(pos)) }
            onActualCueBallMoved =
                { pos -> viewModel.onEvent(MainScreenEvent.ActualCueBallMoved(pos)) }
            onScale =
                { scaleFactor -> viewModel.onEvent(MainScreenEvent.ZoomScaleChanged(scaleFactor)) }
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

    Box(modifier = Modifier.fillMaxSize()) {
        CameraBackground(modifier = Modifier
            .fillMaxSize()
            .zIndex(0f))

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
            onMenuClick = { showBottomSheet = true },
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

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .zIndex(2f)
        ) {
            TableControls(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .zIndex(2f)
        ) {
            ToggleCueBallFab(
                uiState = uiState,
                onEvent = viewModel::onEvent
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .zIndex(2f)
        ) {
            ResetFab(
                uiState = uiState,
                onEvent = viewModel::onEvent
            )
        }


        KineticWarningOverlay(
            text = uiState.warningText,
            modifier = Modifier.zIndex(3f)
        )
    }

    if (showBottomSheet) {
        MenuBottomSheet(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            sheetState = sheetState,
            onDismiss = { showBottomSheet = false }
        )
    }
}
