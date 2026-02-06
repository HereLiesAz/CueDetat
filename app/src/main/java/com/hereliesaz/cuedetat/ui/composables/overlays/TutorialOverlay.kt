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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.TutorialHighlightElement

/**
 * An overlay that guides the user through the application features.
 *
 * It draws attention to specific UI elements (like balls or sliders) by drawing
 * pulsing highlights around them on a Canvas layer sitting above the main view.
 * It uses the [DrawingUtils] to correctly project 2D screen coordinates onto 3D world objects.
 *
 * @param uiState Current application state.
 * @param onEvent Event dispatcher.
 */
@Composable
fun TutorialOverlay(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit
) {
    // The hardcoded tutorial script.
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
        // Animation for the pulsing highlight effect.
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

        // Canvas for drawing highlights over 3D objects.
        Canvas(
            modifier = Modifier
                .zIndex(9f) // Ensure it draws above the GLSurfaceView/Camera.
        ) {
            val matrix = uiState.pitchMatrix
            if (matrix == null) return@Canvas

            // Switch on which element should be highlighted for the current step.
            when (uiState.tutorialHighlight ?: TutorialHighlightElement.NONE) {
                TutorialHighlightElement.TARGET_BALL -> {
                    // Calculate screen position and radius for the target ball.
                    val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(
                        uiState.protractorUnit.center,
                        uiState.protractorUnit.radius,
                        uiState,
                        matrix
                    )
                    val screenPos = DrawingUtils.mapPoint(uiState.protractorUnit.center, matrix)
                    drawCircle(
                        color = highlightColor,
                        radius = radiusInfo.radius * 2.0f, // Draw slightly larger than the ball.
                        center = Offset(screenPos.x, screenPos.y),
                        style = Stroke(width = 4.dp.toPx())
                    )
                }

                TutorialHighlightElement.GHOST_BALL -> {
                    val ghostCenter = uiState.protractorUnit.ghostCueBallCenter
                    val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(
                        ghostCenter,
                        uiState.protractorUnit.radius,
                        uiState,
                        matrix
                    )
                    val screenPos = DrawingUtils.mapPoint(ghostCenter, matrix)
                    drawCircle(
                        color = highlightColor,
                        radius = radiusInfo.radius * 2.0f,
                        center = Offset(screenPos.x, screenPos.y),
                        style = Stroke(width = 4.dp.toPx())
                    )
                }

                TutorialHighlightElement.CUE_BALL -> {
                    uiState.onPlaneBall?.let {
                        val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(
                            it.center,
                            it.radius,
                            uiState,
                            matrix
                        )
                        val screenPos = DrawingUtils.mapPoint(it.center, matrix)
                        drawCircle(
                            color = highlightColor,
                            radius = radiusInfo.radius * 2.0f,
                            center = Offset(screenPos.x, screenPos.y),
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                }

                TutorialHighlightElement.ZOOM_SLIDER -> {
                    // Highlight the area where the zoom slider is typically located (right side).
                    drawRoundRect(
                        color = highlightColor,
                        topLeft = Offset(
                            size.width - 64.dp.toPx(),
                            center.y - (size.height * 0.3f)
                        ),
                        size = Size(60.dp.toPx(), size.height * 0.6f),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
                // Other cases can be added here
                else -> {}
            }
        }

        // Bottom text box for instructions.
        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 96.dp, start = 16.dp, end = 16.dp)
                .zIndex(10f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
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
                    text = tutorialSteps.getOrNull(uiState.currentTutorialStep) ?: "The tutorial is over. Go away.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
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
