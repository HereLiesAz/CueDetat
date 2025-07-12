// --- FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/overlays/TutorialOverlay.kt ---
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
            "Alright, let's get this over with. This isn't a toy. It's a precision instrument. Try to keep up. Tap 'Next'.",
            "That circle with the dot is the Target Ball. Drag it over your object ball. I'll wait.",
            "The crosshairs mark the Ghost Ball—that's where you *should* hit the cue ball. Drag a single finger *anywhere* to rotate the aim. The line should point to a pocket. You know what a pocket is, I assume.",
            "If you insist, you can toggle the 'Cue Ball' from the menu. Drag it to where your real cue ball is. The line between it and the Ghost Ball is the shot you're supposed to be making. It's not rocket science. It's just physics.",
            "The slider on the right zooms. The one on the bottom rotates the table. I'm not going to hold your hand on this one.",
            "In the menu, there's a 'Calculate Bank' option. If you're feeling brave, or foolish, press it. We'll see how you handle something that requires actual thought.",
            "That's the crash course. The rest is a test of your character, or lack thereof. Don't embarrass us both. Press Finish."
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