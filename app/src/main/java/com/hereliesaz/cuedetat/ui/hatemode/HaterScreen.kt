// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterScreen.kt

package com.hereliesaz.cuedetat.ui.hatemode

import android.graphics.BlurMaskFilter
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.max

@Composable
fun HaterScreen(viewModel: HaterViewModel) {
    val state by viewModel.haterState.collectAsStateWithLifecycle()
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        viewModel.onEvent(HaterEvent.EnterHaterMode)
    }

    val dieAnimationProgress by animateFloatAsState(
        targetValue = when (state.triangleState) {
            TriangleState.SUBMERGING -> 0f
            TriangleState.EMERGING -> 1f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 1000),
        label = "Die Animation"
    )

    val glowPaint = remember {
        Paint().asFrameworkPaint().apply {
            color = Color(0x993366FF).toArgb()
            maskFilter = BlurMaskFilter(30f, BlurMaskFilter.Blur.NORMAL)
        }
    }
    val trianglePaint = remember { Paint().apply { color = Color(0xFF3366FF) } }
    val textPaint = remember {
        TextPaint().apply {
            isAntiAlias = true
            color = Color.White.toArgb()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { viewModel.onEvent(HaterEvent.DragEnd) },
                        onDrag = { change, dragAmount ->
                            viewModel.onEvent(HaterEvent.Dragging(dragAmount))
                            change.consume()
                        }
                    )
                }
        ) {
            viewModel.setupBoundaries(size.width, size.height)
            viewModel.updateDieAndText(state.answer, density.density)

            val centerX = size.width / 2
            val centerY = size.height / 2

            drawRect(color = Color(0xFF050A1D))

            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.translate(centerX, centerY)

                val dieX = state.diePosition.x
                val dieY = state.diePosition.y
                val dieAngle = state.dieAngle

                canvas.translate(dieX, dieY)
                canvas.rotate(dieAngle)
                canvas.scale(dieAnimationProgress, dieAnimationProgress)

                val text = state.answer
                val layoutWidth = (size.width * 0.7f).toInt()
                textPaint.textSize = 16 * density.fontScale * density.density

                val staticLayout =
                    StaticLayout.Builder.obtain(text, 0, text.length, textPaint, layoutWidth)
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                        .build()

                val textWidth = staticLayout.width.toFloat()
                val textHeight = staticLayout.height.toFloat()

                val padding = 30 * density.density
                val triangleHeight = textHeight + padding
                val sideLength = (triangleHeight / (kotlin.math.sqrt(3.0) / 2.0)).toFloat()
                val triangleWidth = max(textWidth + padding, sideLength)

                val halfHeight = triangleHeight / 2
                val halfWidth = triangleWidth / 2

                val trianglePath = Path().apply {
                    moveTo(0f, -halfHeight)
                    lineTo(-halfWidth, halfHeight)
                    lineTo(halfWidth, halfHeight)
                    close()
                }

                textPaint.alpha = (255 * dieAnimationProgress).toInt()

                canvas.nativeCanvas.drawPath(trianglePath.asAndroidPath(), glowPaint)
                canvas.drawPath(path = trianglePath, paint = trianglePaint)

                canvas.save()
                canvas.translate(-staticLayout.width / 2f, -staticLayout.height / 2f)
                staticLayout.draw(canvas.nativeCanvas)
                canvas.restore()

                canvas.restore()
            }
        }
    }
}