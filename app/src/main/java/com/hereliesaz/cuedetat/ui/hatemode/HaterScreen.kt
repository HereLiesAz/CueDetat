package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.ui.theme.Eight_ball_blue
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HaterScreen(viewModel: HaterViewModel) {
    val haterState by viewModel.state.collectAsState()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenWidthDp = configuration.screenWidthDp.dp

    // Animation states for emergence
    val emergenceScale = remember { Animatable(0f) }
    val emergenceAlpha = remember { Animatable(0f) }
    val emergenceOffsetY = remember { Animatable(screenHeightPx / 10f) }
    val emergenceRotation = remember { Animatable(0f) }

    // Unified animatable for the triangle's current position, combining all forces.
    val currentOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val currentRotation = remember { Animatable(0f) }

    LaunchedEffect(haterState.isHaterVisible) {
        if (haterState.isHaterVisible) {
            emergenceRotation.snapTo(haterState.randomRotation - 45f)
            launch {
                emergenceScale.animateTo(
                    1f,
                    animationSpec = spring(stiffness = 10f, dampingRatio = 0.7f)
                )
            }
            launch {
                emergenceAlpha.animateTo(
                    1f,
                    animationSpec = tween(durationMillis = 2000, delayMillis = 1000)
                )
            }
            launch { emergenceOffsetY.animateTo(0f, animationSpec = spring(stiffness = 8f)) }
            launch {
                emergenceRotation.animateTo(
                    haterState.randomRotation,
                    animationSpec = spring(stiffness = 5f)
                )
            }
        } else {
            launch { emergenceScale.animateTo(0f, animationSpec = tween(1500)) }
            launch { emergenceAlpha.animateTo(0f, animationSpec = tween(500)) }
            launch { emergenceOffsetY.animateTo(screenHeightPx / 10f, animationSpec = tween(1500)) }
        }
    }

    // This is the core physics loop
    LaunchedEffect(
        haterState.isUserDragging,
        haterState.gravityTargetOffset,
        haterState.touchDrivenOffset
    ) {
        val gravityTarget = haterState.gravityTargetOffset + haterState.randomOffset.copy(
            x = haterState.randomOffset.x * screenWidthPx,
            y = haterState.randomOffset.y * screenHeightPx
        )

        if (haterState.isUserDragging) {
            // While dragging, snap directly to the combined position
            launch { currentOffset.snapTo(gravityTarget + haterState.touchDrivenOffset) }
        } else {
            // When not dragging, animate smoothly towards the gravity target
            launch {
                currentOffset.animateTo(
                    gravityTarget,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 10f)
                )
            }
        }

        // Always animate rotation towards gravity target
        val targetRotation = haterState.gravityTargetOffset.x * 0.2f
        launch {
            currentRotation.animateTo(
                targetRotation,
                animationSpec = spring(stiffness = 10f)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent),
                        start = haterState.gradientStart,
                        end = haterState.gradientEnd
                    )
                )
        )

        if (haterState.currentAnswer != null) {
            // Define movement boundaries
            val imageWidthPx = with(density) { (screenWidthDp / 2).toPx() }
            val xBounds = (screenWidthPx / 2f) - (imageWidthPx / 2f)
            val yBounds = (screenHeightPx / 2f) - (imageWidthPx / 2f) // Assuming roughly square

            val clampedX = currentOffset.value.x.coerceIn(-xBounds, xBounds)
            val clampedY = currentOffset.value.y.coerceIn(-yBounds, yBounds)
            val finalOffset = Offset(clampedX, clampedY)

            Image(
                painter = painterResource(id = haterState.currentAnswer!!),
                contentDescription = "Hater Mode Response",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(screenWidthDp / 2)
                    .offset {
                        IntOffset(
                            finalOffset.x.roundToInt(),
                            (emergenceOffsetY.value + finalOffset.y).roundToInt()
                        )
                    }
                    .scale(emergenceScale.value)
                    .rotate(emergenceRotation.value + currentRotation.value)
                    .alpha(emergenceAlpha.value)
                    .drawBehind {
                        val glowRadius = size.minDimension
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Eight_ball_blue.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                radius = glowRadius
                            ),
                            radius = glowRadius
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { viewModel.onEvent(HaterEvent.DragTriangleStart) },
                            onDragEnd = { viewModel.onEvent(HaterEvent.DragTriangleEnd) }
                        ) { change, dragAmount ->
                            change.consume()
                            viewModel.onEvent(HaterEvent.DragTriangle(dragAmount))
                        }
                    }
            )
        }
    }
}