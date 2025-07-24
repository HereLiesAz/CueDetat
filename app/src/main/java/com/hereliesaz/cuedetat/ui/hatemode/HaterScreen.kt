package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.ui.theme.HaterIconMap
import com.hereliesaz.cuedetat.ui.theme.VoidBlack
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun HaterScreen(
    viewModel: HaterViewModel = hiltViewModel(),
    onMenuClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val constraints = this.constraints
        val triangleSize = (constraints.maxWidth * 0.6f).coerceAtMost(800f)

        // Set initial/new random position when triggered
        LaunchedEffect(uiState.triggerNewAnswer) {
            if (uiState.trianglePosition == null) {
                val newX = Random.nextFloat() * (constraints.maxWidth - triangleSize)
                val newY = Random.nextFloat() * (constraints.maxHeight - triangleSize)
                viewModel.setInitialTrianglePosition(Offset(newX, newY))
            }
        }

        val animatedAlpha = remember { Animatable(0f) }
        val animatedPosition = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

        LaunchedEffect(uiState.isTriangleVisible) {
            animatedAlpha.animateTo(
                targetValue = if (uiState.isTriangleVisible) 1f else 0f,
                animationSpec = tween(durationMillis = 1000)
            )
        }

        LaunchedEffect(uiState.trianglePosition) {
            uiState.trianglePosition?.let {
                // Instantly update the target for dragging, the animation handles the smoothing
                animatedPosition.animateTo(
                    targetValue = it,
                    animationSpec = tween(durationMillis = 500) // Slow, weighty response to drag
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = VoidBlack)
        }

        // Only compose the triangle if there's an answer and position
        if (uiState.currentAnswer != null && uiState.trianglePosition != null) {
            // This Box is the touchable, draggable, and animated container
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            animatedPosition.value.x.roundToInt(),
                            animatedPosition.value.y.roundToInt()
                        )
                    }
                    .size(triangleSize.dp)
                    .alpha(animatedAlpha.value)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val currentPos = animatedPosition.value
                            viewModel.onTriangleDragged(currentPos + dragAmount)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures {
                            viewModel.onTriangleTapped()
                        }
                    }
            ) {
                // The visual representation of the triangle goes inside the Box.
                // It's looked up from the map using the key provided by the ViewModel.
                val icon = HaterIconMap[uiState.currentAnswer]!!
                Icon(
                    imageVector = icon,
                    contentDescription = uiState.currentAnswer,
                    modifier = Modifier.fillMaxSize(),
                    tint = Color.Unspecified // Use the colors defined in the ImageVector
                )
            }
        }

        Image(
            painter = painterResource(id = R.drawable.ic_launcher),
            contentDescription = "Menu",
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 16.dp)
                .size(64.dp)
                .clip(CircleShape)
                .clickable(onClick = onMenuClick)
        )
    }
}