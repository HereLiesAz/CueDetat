package com.hereliesaz.cuedetat.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.view.ProtractorOverlayView
import com.hereliesaz.cuedetat.view.state.OverlayState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val toastMessage: ToastMessage? by viewModel.toastMessage.collectAsState()
    val context = LocalContext.current

    val sheetState = rememberModalBottomSheetState()
    rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    val protractorView = remember {
        ProtractorOverlayView(context).apply {
            onSizeChanged = viewModel::onSizeChanged
            onRotationChange = viewModel::onRotationChange
            onUnitMove = viewModel::onUnitMoved
            onActualCueBallMoved = viewModel::onActualCueBallMoved
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    LaunchedEffect(colorScheme) {
        viewModel.onThemeChanged(colorScheme)
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
            viewModel.onToastShown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(modifier = Modifier.fillMaxSize())
        AndroidView({ protractorView }, modifier = Modifier.fillMaxSize()) { view ->
            view.updateState(uiState)
        }
        TopControls(uiState = uiState, onMenuClick = { showBottomSheet = true })
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(0.4f)
                .padding(end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_zoom_in_24),
                contentDescription = stringResource(id = R.string.zoom_icon),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            VerticalSlider(
                value = uiState.zoomSliderPosition,
                onValueChange = viewModel::onZoomSliderChange,
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxHeight(),
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    thumbColor = MaterialTheme.colorScheme.primary
                )
            )
        }
        FloatingActionButton(
            onClick = viewModel::onToggleActualCueBall,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .navigationBarsPadding(),
            containerColor = if (uiState.isActualCueBallVisible) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_jump_shot),
                contentDescription = "Toggle Actual Cue Ball",
                tint = if (uiState.isActualCueBallVisible) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FloatingActionButton(
            onClick = viewModel::onReset,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_undo_24),
                contentDescription = "Reset View"
            )
        }
    }
    if (showBottomSheet) {
        ModalBottomSheet(onDismissRequest = { showBottomSheet = false }, sheetState = sheetState) {
            MenuItem(
                icon = ImageVector.vectorResource(R.drawable.ic_help_outline_24),
                text = stringResource(if (uiState.areHelpersVisible) R.string.hide_helpers else R.string.show_helpers),
                onClick = viewModel::onToggleHelp
            )
            MenuItem(
                icon = ImageVector.vectorResource(R.drawable.ic_launcher_monochrome),
                text = "Check for Updates",
                onClick = { viewModel.onCheckForUpdate() })
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
@Composable
fun TopControls(uiState: OverlayState, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (uiState.areHelpersVisible) {
            Text(
                text = "Cue D’état",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.clickable(onClick = onMenuClick)
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_monochrome),
                contentDescription = "Menu",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onMenuClick)
            )
        }
    }
}
@Composable
fun MenuItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = text, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}