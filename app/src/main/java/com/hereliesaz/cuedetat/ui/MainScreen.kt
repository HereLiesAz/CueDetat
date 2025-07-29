package com.hereliesaz.cuedetat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.domain.CueDetatAction
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.OverlayState
import com.hereliesaz.cuedetat.ui.hatemode.HaterScreen
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()

    CueDetatTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (state.experienceMode) {
                    ExperienceMode.NORMAL -> NormalScreen(state, viewModel::dispatch)
                    ExperienceMode.HATER -> HaterScreen(state.haterState, viewModel::dispatch)
                }

                // Render overlays on top of the current screen
                OverlayRenderer(state, viewModel::dispatch)
            }
        }
    }
}

@Composable
fun NormalScreen(state: CueDetatState, dispatch: (CueDetatAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Normal Mode")
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = { dispatch(CueDetatAction.ToggleExperienceMode) }) {
            Text("Change Mode")
        }
    }
}

@Composable
fun OverlayRenderer(state: CueDetatState, dispatch: (CueDetatAction) -> Unit) {
    // A function to determine if the experience mode selection should be shown
    val showExperienceModeSelection = state.overlay is OverlayState.ExperienceModeSelection

    AnimatedVisibility(
        visible = showExperienceModeSelection,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Clicking the background dismisses the overlay
                    dispatch(CueDetatAction.ToggleExperienceMode)
                },
            contentAlignment = Alignment.Center
        ) {
            if (state.overlay is OverlayState.ExperienceModeSelection) {
                ExperienceModeSelectionDialog(
                    currentMode = state.overlay.currentMode,
                    dispatch = dispatch
                )
            }
        }
    }
}

@Composable
fun ExperienceModeSelectionDialog(
    currentMode: ExperienceMode,
    dispatch: (CueDetatAction) -> Unit
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
            .padding(24.dp)
            // Stop click propagation to the background
            .clickable(enabled = false) {},
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Select Experience Mode", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { dispatch(CueDetatAction.ApplyPendingExperienceMode(ExperienceMode.NORMAL)) },
                enabled = currentMode != ExperienceMode.NORMAL
            ) {
                Text("Normal")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = { dispatch(CueDetatAction.ApplyPendingExperienceMode(ExperienceMode.HATER)) },
                enabled = currentMode != ExperienceMode.HATER
            ) {
                Text("Hater")
            }
        }
    }
}