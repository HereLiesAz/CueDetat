// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/overlays/TutorialOverlay.kt
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.renderer.warpedBy
import com.hereliesaz.cuedetat.view.state.TutorialHighlightElement

@Composable
fun TutorialOverlay(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit
) {
    if (!uiState.showTutorialOverlay) return

    val tutorialSteps = when (uiState.tutorialType) {
        com.hereliesaz.cuedetat.domain.TutorialType.DYNAMIC_NON_AR -> listOf(
            "Look at you, breaking free. This is Dynamic Mode. The world is your table.",
            "Your phone is now the cue stick. Rotate it horizontally and vertically to aim. The line follows your gaze.",
            "Tap anywhere on the 'floor' to move the Target Ball. It's like magic, but with math. Tap Finish when you're ready."
        )
        com.hereliesaz.cuedetat.domain.TutorialType.DYNAMIC_AR -> listOf(
            "AR mode active. Aim your lens at the table. This is the future, or at least a reasonable approximation.",
            "The shot guide line is anchored to the bottom of your screen. Point and shoot, essentially.",
            "Tap on a physical ball on the table. The Target Ball will 'Snap' right to it. Efficiency is the only virtue here. Tap Finish."
        )
        com.hereliesaz.cuedetat.domain.TutorialType.BEGINNER_STATIC -> listOf(
            "This is a pool protractor with a bubble leveler.",
            "In your head, pick the ball you want to hit in, and the pocket where you want it to go.",
            "Hold your phone over the ball you chose to hit in. Fit it inside the YELLOW CIRCLE.",
            "Keep it there, and point the YELLOW LINE at the pocket you chose.",
            "Put your finger under the center dot of the BLUE CIRCLE. This is where you need to aim the cue ball."
        )
        com.hereliesaz.cuedetat.domain.TutorialType.BEGINNER_DYNAMIC -> listOf(
            "This mode is the exact same thing as the protractor, except you don't need to hold it over the target ball.",
            "Hold your phone upright over the cueball.",
            "Drag the yellow circle over the ball you want to hit in.",
            "Resize the YELLOW CIRCLE to match the target ball, using the zoom slider.",
            "Drag your finger on a clear spot on the screen to aim the YELLOW LINE at your pocket.",
            "The center of the BLUE CIRCLE is where you need to aim the cue ball."
        )
        else -> listOf(
            "Alright, let's get this over with. This isn't a toy. It's a precision instrument. Try to keep up. Tap 'Next'.",
            "That circle with the dot is the Target Ball. Drag it over your object ball. I'll wait.",
            "The crosshairs mark the Ghost Ball—that's where you *should* hit the cue ball. Drag a single finger *anywhere* to rotate the aim. The line should point to a pocket. You know what a pocket is, I assume.",
            "If you insist, you can toggle the 'Cue Ball' from the menu. Drag it to where your real cue ball is. The line between it and the Ghost Ball is the shot you're supposed to be making. It's not rocket science. It's just physics.",
            "The slider on the right zooms. The one on the bottom rotates the table. I'm not going to hold your hand on this one.",
            "In the menu, there's a 'Calculate Bank' option. If you're feeling brave, or foolish, press it. We'll see how you handle something that requires actual thought.",
            "That's the crash course. The rest is a test of your character, or lack thereof. Don't embarrass us both. Press Finish."
        )
    }

    val tps = if (uiState.cameraMode == com.hereliesaz.cuedetat.domain.CameraMode.LITE_AR) null else uiState.lensWarpTps
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
    val highlightColor = Color.Green.copy(alpha = highlightAlpha)

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val matrix = if (uiState.isBeginnerViewLocked) uiState.logicalPlaneMatrix else uiState.pitchMatrix
            if (matrix == null) return@Canvas

            when (uiState.tutorialHighlight ?: TutorialHighlightElement.NONE) {
                TutorialHighlightElement.TARGET_BALL -> {
                    val warpedCenter = uiState.protractorUnit.center.warpedBy(tps)
                    val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(
                        warpedCenter,
                        uiState.protractorUnit.radius,
                        uiState,
                        matrix
                    )
                    val screenPos = DrawingUtils.mapPoint(warpedCenter, matrix)
                    val liftedY = screenPos.y - radiusInfo.lift
                    drawCircle(
                        color = highlightColor,
                        radius = radiusInfo.radius,
                        center = Offset(screenPos.x, liftedY),
                        style = Stroke(width = 4.dp.toPx())
                    )
                }

                TutorialHighlightElement.GHOST_BALL -> {
                    val warpedCenter = uiState.protractorUnit.ghostCueBallCenter.warpedBy(tps)
                    val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(
                        warpedCenter,
                        uiState.protractorUnit.radius,
                        uiState,
                        matrix
                    )
                    val screenPos = DrawingUtils.mapPoint(warpedCenter, matrix)
                    val liftedY = screenPos.y - radiusInfo.lift
                    drawCircle(
                        color = highlightColor,
                        radius = radiusInfo.radius * 0.1f,
                        center = Offset(screenPos.x, liftedY),
                        style = Stroke(width = 4.dp.toPx())
                    )
                }

                TutorialHighlightElement.CUE_BALL -> {
                    uiState.onPlaneBall?.let {
                        val warpedCenter = it.center.warpedBy(tps)
                        val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(
                            warpedCenter,
                            it.radius,
                            uiState,
                            matrix
                        )
                        val screenPos = DrawingUtils.mapPoint(warpedCenter, matrix)
                        val liftedY = screenPos.y - radiusInfo.lift
                        drawCircle(
                            color = highlightColor,
                            radius = radiusInfo.radius,
                            center = Offset(screenPos.x, liftedY),
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                }

                TutorialHighlightElement.AIMING_LINE -> {
                    val ghostWarped = uiState.protractorUnit.ghostCueBallCenter.warpedBy(tps)
                    val targetWarped = uiState.protractorUnit.center.warpedBy(tps)
                    val screenStart = DrawingUtils.mapPoint(ghostWarped, matrix)
                    val screenEnd = DrawingUtils.mapPoint(targetWarped, matrix)

                    drawLine(
                        color = highlightColor,
                        start = Offset(screenStart.x, screenStart.y),
                        end = Offset(screenEnd.x, screenEnd.y),
                        strokeWidth = 8.dp.toPx()
                    )
                }

                TutorialHighlightElement.ZOOM_SLIDER -> {
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

                else -> {}
            }
        }

        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(top = 100.dp, end = 24.dp)
                .zIndex(10f)
                .align(Alignment.TopEnd)
        ) {
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = tutorialSteps.getOrNull(uiState.currentTutorialStep)
                        ?: "The tutorial is over. Go away.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { onEvent(MainScreenEvent.TutorialBack) },
                        enabled = uiState.currentTutorialStep > 0
                    ) {
                        Text("Back")
                    }
                    if (uiState.currentTutorialStep < tutorialSteps.lastIndex) {
                        TextButton(onClick = { onEvent(MainScreenEvent.NextTutorialStep) }) {
                            Text(
                                if (uiState.tutorialType == com.hereliesaz.cuedetat.domain.TutorialType.BEGINNER_STATIC || uiState.tutorialType == com.hereliesaz.cuedetat.domain.TutorialType.BEGINNER_DYNAMIC) "Next" else "Skip"
                            )
                        }
                    } else {
                        TextButton(onClick = { onEvent(MainScreenEvent.EndTutorial) }) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}
