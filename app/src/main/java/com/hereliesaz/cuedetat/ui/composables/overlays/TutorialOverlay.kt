package com.hereliesaz.cuedetat.ui.composables.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun TutorialOverlay(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit
) {
    val tutorialSteps = remember {
        listOf(
            "Welcome. This is not a toy. It is a tool for geometric enlightenment. Pay attention.",
            "This is Protractor Mode. Use it for cut shots. The elements you see are ghosts in the machine. They live on a logical plane, projected onto your reality.",
            "Drag the 'T' (Target Ball) to align it with your object ball. Drag with one finger to rotate the aiming line to the pocket.",
            "This optional 'A' (Actual Cue Ball) can be dragged to match your real cue ball's position. The line from 'A' to 'G' (Ghost Cue Ball) is the path you must shoot.",
            "The vertical slider on the right controls zoom. The slider on the bottom (in Banking Mode) rotates the table.",
            "Open the menu to find more tools. 'Calculate Bank' switches to Banking Mode, where you can visualize multi-rail shots.",
            "You have been instructed. Now, go and sin no more. Or at least, sin with better geometry. Press Finish."
        )
    }

    if (uiState.showTutorialOverlay) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f))
                .clickable(enabled = true, onClick = {}) // Block clicks
                .zIndex(10f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = tutorialSteps.getOrNull(uiState.currentTutorialStep) ?: "The tutorial is over. Go away.",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(24.dp))
                Row {
                    if (uiState.currentTutorialStep < tutorialSteps.lastIndex) {
                        TextButton(onClick = { onEvent(MainScreenEvent.NextTutorialStep) }) {
                            Text("Next")
                        }
                    }
                    TextButton(onClick = { onEvent(MainScreenEvent.EndTutorial) }) {
                        Text("Finish")
                    }
                }
            }
        }
    }
}