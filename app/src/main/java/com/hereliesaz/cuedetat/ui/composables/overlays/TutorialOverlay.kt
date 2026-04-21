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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hereliesaz.cuedetat.R
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
        com.hereliesaz.cuedetat.domain.TutorialType.DYNAMIC_NON_AR -> stringArrayResource(id = R.array.tutorial_dynamic_non_ar).toList()
        com.hereliesaz.cuedetat.domain.TutorialType.DYNAMIC_AR -> stringArrayResource(id = R.array.tutorial_dynamic_ar).toList()
        com.hereliesaz.cuedetat.domain.TutorialType.BEGINNER_STATIC -> stringArrayResource(id = R.array.tutorial_beginner_static).toList()
        com.hereliesaz.cuedetat.domain.TutorialType.BEGINNER_DYNAMIC -> stringArrayResource(id = R.array.tutorial_beginner_dynamic).toList()
        else -> stringArrayResource(id = R.array.tutorial_general).toList()
    }

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

    val isLocked = uiState.isBeginnerViewLocked
    val matrix = if (isLocked) uiState.logicalPlaneMatrix else uiState.pitchMatrix
    val tps = if (uiState.cameraMode == com.hereliesaz.cuedetat.domain.CameraMode.LITE_AR || isLocked) null else uiState.lensWarpTps

    // Optimize: Pre-calculate highlight parameters to avoid redundant math in the draw loop
    val highlightParams = remember(uiState.tutorialHighlight, matrix, tps, uiState.protractorUnit, uiState.onPlaneBall, uiState.aimingLineEndPoint, uiState.viewWidth, uiState.viewHeight, isLocked, uiState.cameraMatrix, uiState.distCoeffs) {
        if (matrix == null) return@remember emptyList<HighlightParams>()
        
        when (uiState.tutorialHighlight ?: TutorialHighlightElement.NONE) {
            TutorialHighlightElement.TARGET_BALL -> {
                val warpedCenter = uiState.protractorUnit.center.warpedBy(tps)
                val screenPos = DrawingUtils.mapPoint(warpedCenter, matrix)
                val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(warpedCenter, uiState.protractorUnit.radius, uiState, matrix)
                val liftedY = if (isLocked) screenPos.y else screenPos.y - radiusInfo.lift
                listOf(HighlightParams.Circle(Offset(screenPos.x, liftedY), radiusInfo.radius))
            }
            TutorialHighlightElement.GHOST_BALL -> {
                val warpedCenter = uiState.protractorUnit.ghostCueBallCenter.warpedBy(tps)
                val screenPos = DrawingUtils.mapPoint(warpedCenter, matrix)
                val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(warpedCenter, uiState.protractorUnit.radius, uiState, matrix)
                val liftedY = if (isLocked) screenPos.y else screenPos.y - radiusInfo.lift
                listOf(HighlightParams.Circle(Offset(screenPos.x, liftedY), radiusInfo.radius * 0.15f))
            }
            TutorialHighlightElement.CUE_BALL -> {
                uiState.onPlaneBall?.let {
                    val warpedCenter = it.center.warpedBy(tps)
                    val screenPos = DrawingUtils.mapPoint(warpedCenter, matrix)
                    val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(warpedCenter, it.radius, uiState, matrix)
                    val liftedY = if (isLocked) screenPos.y else screenPos.y - radiusInfo.lift
                    listOf(HighlightParams.Circle(Offset(screenPos.x, liftedY), radiusInfo.radius))
                } ?: emptyList()
            }
            TutorialHighlightElement.AIMING_LINE -> {
                val camMat = uiState.cameraMatrix
                val distMat = uiState.distCoeffs
                val camArray = if (camMat != null && !camMat.empty()) DoubleArray(camMat.total().toInt()).also { camMat.get(0, 0, it) } else null
                val distArray = if (distMat != null && !distMat.empty()) DoubleArray(distMat.total().toInt()).also { distMat.get(0, 0, it) } else null

                val ghostCenter = uiState.protractorUnit.ghostCueBallCenter
                val targetCenter = uiState.protractorUnit.center
                val rawPath = uiState.aimingLineBankPath ?: listOf(ghostCenter, targetCenter)
                val logicalPath = rawPath.map { it.warpedBy(tps) }
                
                val screenPath = android.graphics.Path()
                if (logicalPath.size >= 2) {
                    for (i in 0 until logicalPath.size - 1) {
                        val start = logicalPath[i]
                        val end = logicalPath[i+1]
                        val segment = DrawingUtils.buildDistortedLinePath(start, end, matrix, camArray, distArray)
                        screenPath.addPath(segment)
                    }
                }

                val measure = android.graphics.PathMeasure(screenPath, false)
                val pathLen = measure.length
                
                val visibleData = mutableListOf<TriangleHighlight>()
                var d = 0f
                val step = 10f
                val pos = floatArrayOf(0f, 0f)
                val tan = floatArrayOf(0f, 0f)
                
                while (d <= pathLen && visibleData.size < 1000) {
                    if (measure.getPosTan(d, pos, tan)) {
                        if (pos[0] in 0f..uiState.viewWidth.toFloat() && pos[1] in 0f..uiState.viewHeight.toFloat()) {
                            val angle = Math.toDegrees(kotlin.math.atan2(tan[1].toDouble(), tan[0].toDouble())).toFloat()
                            visibleData.add(TriangleHighlight(Offset(pos[0], pos[1]), angle))
                        }
                    }
                    d += step
                }
                
                val selectedTriangles = mutableListOf<TriangleHighlight>()
                if (visibleData.size > 10) {
                    for (i in 1..3) {
                        val idx = (i * visibleData.size / 4).coerceIn(0, visibleData.lastIndex)
                        selectedTriangles.add(visibleData[idx])
                    }
                }
                
                listOf(HighlightParams.AimingTriangles(selectedTriangles))
            }
            TutorialHighlightElement.ZOOM_SLIDER -> {
                val topLeft = Offset(uiState.viewWidth - 64.dp.value * uiState.screenDensity, uiState.viewHeight * 0.2f)
                val size = Size(60.dp.value * uiState.screenDensity, uiState.viewHeight * 0.6f)
                listOf(HighlightParams.Rect(topLeft, size))
            }
            TutorialHighlightElement.TARGET_BALL_AND_ZOOM_SLIDER -> {
                val warpedCenter = uiState.protractorUnit.center.warpedBy(tps)
                val screenPos = DrawingUtils.mapPoint(warpedCenter, matrix)
                val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(warpedCenter, uiState.protractorUnit.radius, uiState, matrix)
                val liftedY = if (isLocked) screenPos.y else screenPos.y - radiusInfo.lift
                
                val topLeft = Offset(uiState.viewWidth - 64.dp.value * uiState.screenDensity, uiState.viewHeight * 0.2f)
                val size = Size(60.dp.value * uiState.screenDensity, uiState.viewHeight * 0.6f)
                
                listOf(
                    HighlightParams.Circle(Offset(screenPos.x, liftedY), radiusInfo.radius),
                    HighlightParams.Rect(topLeft, size)
                )
            }
            else -> emptyList()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            highlightParams.forEach { param ->
                when (param) {
                    is HighlightParams.Circle -> {
                        drawCircle(
                            color = highlightColor,
                            radius = param.radius,
                            center = param.center,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                    is HighlightParams.AimingTriangles -> {
                        val triPath = Path().apply {
                            moveTo(32.dp.toPx(), 0f)
                            lineTo(-20.dp.toPx(), 26.dp.toPx())
                            lineTo(-20.dp.toPx(), -26.dp.toPx())
                            close()
                        }
                        param.triangles.forEach { tri ->
                            withTransform({
                                translate(tri.center.x, tri.center.y)
                                rotate(tri.angleDeg)
                            }) {
                                drawPath(
                                    path = triPath,
                                    color = Color.Green.copy(alpha = highlightAlpha),
                                    style = Fill
                                )
                            }
                        }
                    }
                    is HighlightParams.Rect -> {
                        drawRoundRect(
                            color = highlightColor,
                            topLeft = param.topLeft,
                            size = param.size,
                            cornerRadius = CornerRadius(16.dp.toPx()),
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .zIndex(10f)
                .align(Alignment.Center)
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
                        ?: stringResource(id = R.string.tutorial_over),
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
                        Text(stringResource(id = R.string.tutorial_back))
                    }
                    if (uiState.currentTutorialStep < tutorialSteps.lastIndex) {
                        TextButton(onClick = { onEvent(MainScreenEvent.NextTutorialStep) }) {
                            Text(
                                if (uiState.tutorialType == com.hereliesaz.cuedetat.domain.TutorialType.BEGINNER_STATIC || uiState.tutorialType == com.hereliesaz.cuedetat.domain.TutorialType.BEGINNER_DYNAMIC) {
                                    stringResource(id = R.string.tutorial_next)
                                } else {
                                    stringResource(id = R.string.tutorial_skip)
                                }
                            )
                        }
                    } else {
                        TextButton(onClick = { onEvent(MainScreenEvent.EndTutorial) }) {
                            Text(stringResource(id = R.string.tutorial_done))
                        }
                    }
                }
            }
        }
    }
}

private data class TriangleHighlight(val center: Offset, val angleDeg: Float)

private sealed class HighlightParams {
    data class Circle(val center: Offset, val radius: Float) : HighlightParams()
    data class AimingTriangles(val triangles: List<TriangleHighlight>) : HighlightParams()
    data class Rect(val topLeft: Offset, val size: Size) : HighlightParams()
}
