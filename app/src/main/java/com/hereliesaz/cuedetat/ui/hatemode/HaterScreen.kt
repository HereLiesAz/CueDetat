// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterScreen.kt

package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.animation.core.Animatable
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

            val clampedX = haterState.position.x.coerceIn(-xBounds, xBounds)
            val clampedY = haterState.position.y.coerceIn(-yBounds, yBounds)

            Image(
                painter = painterResource(id = haterState.currentAnswer!!),
                contentDescription = "Hater Mode Response",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(screenWidthDp / 2)
                    .offset {
                        IntOffset(
                            clampedX.roundToInt(),
                            (emergenceOffsetY.value + clampedY).roundToInt()
                        )
                    }
                    .scale(emergenceScale.value)
                    .rotate(emergenceRotation.value + haterState.angle)
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
                            onDragEnd = { viewModel.onEvent(HaterEvent.DragTriangleEnd) },
                            onDragCancel = { viewModel.onEvent(HaterEvent.DragTriangleEnd) }
                        ) { change, dragAmount ->
                            change.consume()
                            viewModel.onEvent(HaterEvent.DragTriangle(dragAmount))
                        }
                    }
            )
        }
    }
}