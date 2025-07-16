// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/overlays/TutorialOverlay.kt
package com.hereliesaz.cuedetat.ui.composables.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
            "Welcome. This is not a toy. It is a tool for geometric enlightenment. Pay attention. Tap 'Next'.",
            "This is Protractor Mode. Use it for cut shots. The elements you see are ghosts in the machine. They live on a logical plane, projected onto your reality.",
            "Drag the 'T' (Target Ball) to align it with your object ball. Drag with one finger to rotate the aiming line to the pocket. The line should point to a pocket. You know what a pocket is, I assume.",
            "If you insist, you can toggle the 'Cue Ball' from the menu. Drag it to where your real cue ball is. The line from 'A' to 'G' (Ghost Cue Ball) is the path you must shoot. It's not rocket science. It's just physics.",
            "The vertical slider on the right controls zoom. The slider on the bottom (in Banking Mode) rotates the table. I'm not going to hold your hand on this one.",
            "Open the menu to find more tools. 'Calculate Bank' switches to Banking Mode, where you can visualize multi-rail shots.",
            "That's the crash course. The rest is a test of your character, or lack thereof. Don't embarrass us both. Press Finish."
        )
    }

    if (uiState.isTutorialVisible) {
        // This Box now only serves to position the content, it does not block interactions.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f), // Keep it on top
            contentAlignment = Alignment.BottomCenter // Position the tutorial at the bottom
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.9f)) // Opaque background for readability
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = tutorialSteps.getOrNull(uiState.tutorialStep) ?: "The tutorial is over. Go away.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))
                Row {
                    if (uiState.tutorialStep < tutorialSteps.lastIndex) {
                        TextButton(onClick = { onEvent(MainScreenEvent.NextTutorialStep) }) {
                            Text("Next")
                        }
                    }
                    TextButton(onClick = { onEvent(MainScreenEvent.FinishTutorial) }) {
                        Text("Finish")
                    }
                }
            }
        }
    }
}