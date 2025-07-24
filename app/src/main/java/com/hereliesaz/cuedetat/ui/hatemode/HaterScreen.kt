package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.ui.theme.Eight_ball_blue
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HaterScreen(viewModel: HaterViewModel) {
    val haterState by viewModel.state.collectAsState()
    val configuration = LocalConfiguration.current
    val screenHeightPx = configuration.screenHeightDp * LocalConfiguration.current.densityDpi / 160f

    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(screenHeightPx / 4f) }

    LaunchedEffect(haterState.isHaterVisible, haterState.currentAnswer) {
        if (haterState.isHaterVisible && haterState.currentAnswer != null) {
            // Animate in
            launch { scale.animateTo(1f, animationSpec = spring(stiffness = 50f)) }
            launch { alpha.animateTo(1f, animationSpec = tween(durationMillis = 1500)) }
            launch { offsetY.animateTo(0f, animationSpec = spring(stiffness = 40f)) }
        } else {
            // Animate out
            launch { scale.animateTo(0f, animationSpec = tween(250)) }
            launch { alpha.animateTo(0f, animationSpec = tween(250)) }
            launch { offsetY.animateTo(screenHeightPx / 4f, animationSpec = tween(250)) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { viewModel.onEvent(HaterEvent.HideHater) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (haterState.currentAnswer != null) {
            Image(
                painter = painterResource(id = haterState.currentAnswer!!),
                contentDescription = "Hater Mode Response",
                modifier = Modifier
                    .padding(32.dp)
                    .offset { IntOffset(0, offsetY.value.roundToInt()) }
                    .scale(scale.value)
                    .alpha(alpha.value)
                    .drawBehind {
                        val glowRadius = size.minDimension / 1.5f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Eight_ball_blue.copy(alpha = 0.4f),
                                    Color.Transparent
                                ),
                                radius = glowRadius
                            ),
                            radius = glowRadius
                        )
                    }
            )
        }
    }
}