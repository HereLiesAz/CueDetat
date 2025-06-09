package com.hereliesaz.cuedetat.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.view.ProtractorOverlayView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val context = LocalContext.current

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

    Scaffold(
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = viewModel::onToggleJumpingGhostBall) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_jump_shot),
                            contentDescription = "Toggle Jumping Ghost Ball",
                            tint = if (uiState.isJumpingGhostBallActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row {
                        IconButton(onClick = viewModel::onToggleHelp) {
                            Icon(
                                imageVector = Icons.Outlined.HelpOutline,
                                contentDescription = "Toggle Help"
                            )
                        }
                        IconButton(onClick = viewModel::onReset) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Reset"
                            )
                        }
                        IconButton(onClick = viewModel::onCheckForUpdate) {
                            Icon(
                                imageVector = Icons.Outlined.SystemUpdateAlt,
                                contentDescription = "Check for Update"
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ProtractorOverlayView(
                uiState = uiState,
                onSizeChanged = viewModel::onSizeChanged,
                onRotationChange = viewModel::onRotationChange
            )

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_zoom_in_24),
                    contentDescription = "Zoom"
                )
                val sliderState = rememberSliderState(valueRange = 0.1f..4.0f)

                LaunchedEffect(uiState.zoomFactor) {
                    sliderState.value = uiState.zoomFactor
                }

                sliderState.onValueChange = { newValue ->
                    viewModel.onZoomChange(newValue)
                }

                VerticalSlider(
                    state = sliderState,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}