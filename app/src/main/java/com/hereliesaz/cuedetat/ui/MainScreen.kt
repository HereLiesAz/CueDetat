// app/src/main/java/com/hereliesaz/cuedetat/ui/MainScreen.kt
package com.hereliesaz.cuedetat.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
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
import com.hereliesaz.cuedetat.view.state.ToastMessage
import kotlinx.coroutines.launch

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
            onRotationChange = { rot -> viewModel.onEvent(MainScreenEvent.RotationChanged(rot)) }
            onUnitMove = { pos -> viewModel.onEvent(MainScreenEvent.UnitMoved(pos)) }
            onActualCueBallMoved =
                { pos -> viewModel.onEvent(MainScreenEvent.ActualCueBallMoved(pos)) }
            onScale =
                { scaleFactor -> viewModel.onEvent(MainScreenEvent.ZoomScaleChanged(scaleFactor)) }
            onGestureStarted = { viewModel.onEvent(MainScreenEvent.GestureStarted) }
            onGestureEnded = { viewModel.onEvent(MainScreenEvent.GestureEnded) }
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

            ToggleCueBallFab(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .zIndex(2f)
            )

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
