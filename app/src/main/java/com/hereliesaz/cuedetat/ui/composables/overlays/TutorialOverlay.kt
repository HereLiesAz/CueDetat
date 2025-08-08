// --- FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/overlays/TutorialOverlay.kt ---
package com.hereliesaz.cuedetat.ui.composables.overlays

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.TutorialHighlightElement

@Composable
fun TutorialOverlay(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit
) {
    val tutorialSteps = remember {
        listOf(
            "Hold your phone flat and point your camera at the ball you want to hit in.",
            "Fit that ball inside this circle.",
            "Use the Zoom Slider to get it perfect.",
            "Point this line at the pocket.",
            "This is where the cue ball needs to go to put your Target Ball in the pocket."
        )
    }

    if (uiState.showTutorialOverlay) {
        val infiniteTransition = rememberInfiniteTransition(label = "highlight-transition")
        val highlightAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "highlight-alpha"
        )
        val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = highlightAlpha)

        LaunchedEffect(highlightAlpha) {
            onEvent(MainScreenEvent.UpdateHighlightAlpha(highlightAlpha))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp) // Keep horizontal padding
                .zIndex(10f),
        ) {
            val matrix = uiState.pitchMatrix
            if (matrix != null) {
                when (uiState.currentTutorialStep) {
                    0, 1, 2 -> {
                        val targetBallPosition = DrawingUtils.mapPoint(uiState.protractorUnit.center, matrix)
                        TutorialTextBox(
                            text = tutorialSteps[uiState.currentTutorialStep],
                            onNext = { onEvent(MainScreenEvent.NextTutorialStep) },
                            onFinish = { onEvent(MainScreenEvent.EndTutorial) },
                            isLastStep = uiState.currentTutorialStep >= tutorialSteps.lastIndex,
                            modifier = Modifier.offset {
                                IntOffset(targetBallPosition.x.toInt() - 150.dp.roundToPx(), targetBallPosition.y.toInt() - 150.dp.roundToPx())
                            }
                        )
                    }
                    3 -> {
                        // Placeholder for aiming line position
                        val aimingLineEnd = uiState.aimingLineEndPoint ?: uiState.protractorUnit.center
                        val aimingLinePosition = DrawingUtils.mapPoint(aimingLineEnd, matrix)
                        TutorialTextBox(
                            text = tutorialSteps[uiState.currentTutorialStep],
                            onNext = { onEvent(MainScreenEvent.NextTutorialStep) },
                            onFinish = { onEvent(MainScreenEvent.EndTutorial) },
                            isLastStep = uiState.currentTutorialStep >= tutorialSteps.lastIndex,
                            modifier = Modifier.offset {
                                IntOffset(aimingLinePosition.x.toInt(), aimingLinePosition.y.toInt())
                            }
                        )
                    }
                    4 -> {
                        val ghostBallPosition = DrawingUtils.mapPoint(uiState.protractorUnit.ghostCueBallCenter, matrix)
                        TutorialTextBox(
                            text = tutorialSteps[uiState.currentTutorialStep],
                            onNext = { onEvent(MainScreenEvent.NextTutorialStep) },
                            onFinish = { onEvent(MainScreenEvent.EndTutorial) },
                            isLastStep = uiState.currentTutorialStep >= tutorialSteps.lastIndex,
                            modifier = Modifier.offset {
                                IntOffset(ghostBallPosition.x.toInt() - 150.dp.roundToPx(), ghostBallPosition.y.toInt() - 150.dp.roundToPx())
                            }
                        )
                    }
                    else -> {
                        // Default to bottom center if step is out of bounds
                        TutorialTextBox(
                            text = "The tutorial is over. Go away.",
                            onNext = { },
                            onFinish = { onEvent(MainScreenEvent.EndTutorial) },
                            isLastStep = true,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            } else {
                // Fallback if matrix is null
                TutorialTextBox(
                    text = tutorialSteps.getOrNull(uiState.currentTutorialStep) ?: "The tutorial is over. Go away.",
                    onNext = { onEvent(MainScreenEvent.NextTutorialStep) },
                    onFinish = { onEvent(MainScreenEvent.EndTutorial) },
                    isLastStep = uiState.currentTutorialStep >= tutorialSteps.lastIndex,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun TutorialTextBox(
    text: String,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    isLastStep: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Row {
            if (!isLastStep) {
                TextButton(onClick = onNext) {
                    Text("Next")
                }
            }
            TextButton(onClick = onFinish) {
                Text("Finish")
            }
        }
    }
}