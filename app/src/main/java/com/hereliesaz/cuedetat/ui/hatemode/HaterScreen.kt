package com.hereliesaz.cuedetat.ui.hatemode

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HaterScreen(viewModel: HaterViewModel) {
    val triangleState by viewModel.triangleState
    val dieBody = viewModel.dieBody
    val answer by viewModel.answer

    // Animate the scale and alpha of the die for the submerge/emerge effect
    val dieAnimationProgress by animateFloatAsState(
        targetValue = if (triangleState == TriangleState.SUBMERGING || triangleState == TriangleState.EMERGING) 0f else 1f,
        animationSpec = tween(durationMillis = 1000),
        label = "Die Animation"
    )

    // State to track the user's drag gesture for pushing
    var dragStart by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { start -> dragStart = start },
                        onDragEnd = {
                            // Reset on drag end
                            dragStart = Offset.Zero
                        },
                        onDrag = { change, dragAmount ->
                            // Apply a push force based on the drag amount
                            viewModel.pushDie(dragAmount)
                            change.consume()
                        }
                    )
                }
        ) {
            // Center the coordinate system
            val centerX = size.width / 2
            val centerY = size.height / 2

            // 1. Draw the outer black sphere of the 8-ball
            drawCircle(color = Color.Black, radius = size.width / 1.8f)

            // 2. Draw the viewing portal (the "liquid")
            val portalRadius = size.width / 2.5f
            drawCircle(color = Color(0xFF050A1D), radius = portalRadius)


            // 3. Draw the die, its glow, and the answer text
            drawIntoCanvas { canvas ->
                // Save the current state of the canvas
                canvas.save()
                // Translate the canvas to the center
                canvas.translate(centerX, centerY)

                // Get the die's position and rotation from the physics body
                val dieX = dieBody.position.x
                val dieY = dieBody.position.y
                val dieAngle = dieBody.angle * (180f / Math.PI.toFloat())

                // Apply the die's transformations
                canvas.translate(dieX, dieY)
                canvas.rotate(dieAngle)
                // Apply the submerge/emerge animation scale
                if (triangleState == triangleState.SUBMERGING) dieAnimationProgress else if (triangleState == TriangleState.EMERGING) 1 - dieAnimationProgress else 1f
                canvas.scale(dieAnimationProgress, dieAnimationProgress)


                // Create the triangular path for the die
                val trianglePath = Path().apply {
                    moveTo(0f, -50.dp.toPx())
                    lineTo(-43.3f.dp.toPx(), 25.dp.toPx())
                    lineTo(43.3f.dp.toPx(), 25.dp.toPx())
                    close()
                }

                // Paint for the blue glow effect
                val glowPaint = Paint().asFrameworkPaint().apply {
                    color = Color(0x993366FF).toArgb()
                    maskFilter = BlurMaskFilter(30f, BlurMaskFilter.Blur.NORMAL)
                }

                // Paint for the solid blue triangle
                val trianglePaint = Paint().apply {
                    color = Color(0xFF3366FF)
                    style = androidx.compose.ui.graphics.PaintingStyle.Fill
                }

                // Paint for the text
                val textPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = 16.sp.toPx()
                    color = Color.White.toArgb()
                    textAlign = android.graphics.Paint.Align.CENTER
                    alpha = (255 * dieAnimationProgress).toInt()
                }

                // Draw the glow first, then the solid triangle on top
                canvas.nativeCanvas.drawPath(trianglePath.asAndroidPath(), glowPaint)
                canvas.drawPath(path = trianglePath, paint = trianglePaint)

                // Draw the answer text in the center of the triangle
                val textY = 7.dp.toPx() // Small offset to center vertically
                canvas.nativeCanvas.drawText(answer, 0f, textY, textPaint)

                // Restore the canvas to its original state
                canvas.restore()
            }
        }
    }
}
