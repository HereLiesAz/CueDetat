package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.ui.theme.Eight_ball_blue
import com.hereliesaz.cuedetat.ui.theme.VoidBlack
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalTextApi::class)
@Composable
fun HaterScreen(
    viewModel: HaterViewModel = hiltViewModel(),
    onMenuClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val textMeasurer = rememberTextMeasurer()

    val bubbleAlpha = remember { Animatable(0f) }
    val diePositionY = remember { Animatable(0.6f) }
    val dieRotation = remember { Animatable(Random.nextFloat() * 360f) }

    LaunchedEffect(uiState.isShaking) {
        if (uiState.isShaking) {
            // Shake starts: bubbles appear, die drops and spins
            bubbleAlpha.animateTo(1f, animationSpec = tween(300, easing = LinearEasing))
            dieRotation.animateTo(
                dieRotation.value + (Random.nextInt(-180, 180)),
                animationSpec = tween(2000)
            )
            diePositionY.animateTo(0.6f, animationSpec = tween(300, easing = FastOutSlowInEasing))

            // Shake ends: bubbles fade, die rises
            bubbleAlpha.animateTo(
                0f,
                animationSpec = tween(1500, delayMillis = 500, easing = LinearEasing)
            )
            diePositionY.animateTo(
                0f,
                animationSpec = tween(1200, delayMillis = 800, easing = FastOutSlowInEasing)
            )
        }
    }


    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        this.constraints

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val radius = size.minDimension / 2.2f

            // Draw the 8-ball body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.DarkGray, VoidBlack),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Draw the dynamic highlight based on gyroscope
            val highlightOffsetX = uiState.orientation.roll * 2.5f
            val highlightOffsetY = uiState.orientation.pitch * 2.5f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
                    center = center.copy(
                        x = center.x + highlightOffsetX,
                        y = center.y + highlightOffsetY
                    ),
                    radius = radius * 0.8f
                ),
                radius = radius,
                center = center
            )

            // Draw the answer window
            val windowRadius = radius * 0.6f
            drawCircle(color = VoidBlack, radius = windowRadius, center = center)
            drawCircle(color = Eight_ball_blue, radius = windowRadius * 0.9f, center = center)

            // Draw the die and text inside the window
            val dieSize = windowRadius * 0.8f
            val diePath = Path().apply {
                moveTo(center.x, center.y - dieSize) // Top point
                lineTo(
                    center.x + dieSize * cos(Math.toRadians(30.0).toFloat()),
                    center.y + dieSize * sin(Math.toRadians(30.0).toFloat())
                )
                lineTo(
                    center.x - dieSize * cos(Math.toRadians(30.0).toFloat()),
                    center.y + dieSize * sin(Math.toRadians(30.0).toFloat())
                )
                close()
            }

            clipPath(Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        center,
                        windowRadius * 0.9f
                    )
                )
            }) {
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.translate(0f, diePositionY.value * windowRadius)
                    canvas.nativeCanvas.rotate(dieRotation.value, center.x, center.y)

                    drawPath(path = diePath, color = Color(0xFF0D064F))

                    // Draw text on the die
                    uiState.currentAnswer?.let {
                        val textLayoutResult = textMeasurer.measure(
                            it,
                            style = TextStyle(
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = (dieSize / 6).sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            ),
                            softWrap = true,
                            constraints = Constraints(maxWidth = (dieSize * 1.5f).toInt())
                        )
                        drawText(
                            textLayoutResult,
                            topLeft = Offset(
                                x = center.x - textLayoutResult.size.width / 2,
                                y = center.y - dieSize * 0.3f - textLayoutResult.size.height / 2
                            )
                        )
                    }

                    canvas.nativeCanvas.restore()
                }

                // Draw bubbles
                drawBubbles(bubbleAlpha.value, windowRadius)
            }
        }

        Image(
            painter = painterResource(id = R.drawable.ic_launcher),
            contentDescription = "Menu",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(64.dp)
                .clip(CircleShape)
                .clickable(onClick = onMenuClick)
        )
    }
}

private fun DrawScope.drawBubbles(alpha: Float, windowRadius: Float) {
    if (alpha <= 0f) return
    for (i in 0..50) {
        val bubbleRadius = Random.nextFloat() * (windowRadius * 0.1f)
        val angle = Random.nextFloat() * 2 * Math.PI
        val distance = Random.nextFloat() * windowRadius * 0.9f
        val x = center.x + (distance * cos(angle)).toFloat()
        val y = center.y + (distance * sin(angle)).toFloat()
        drawCircle(
            color = Color.White,
            radius = bubbleRadius,
            center = Offset(x, y),
            alpha = alpha * Random.nextFloat() * 0.4f
        )
    }
}