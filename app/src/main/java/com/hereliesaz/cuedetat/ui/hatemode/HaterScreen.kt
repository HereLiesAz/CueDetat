package com.hereliesaz.cuedetat.ui.hatemode

import android.graphics.BlurMaskFilter
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.ui.composables.AzNavRailMenu
import com.hereliesaz.cuedetat.ui.composables.TopControls
import kotlin.math.sqrt

@Composable
fun HaterScreen(
    haterViewModel: HaterViewModel,
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit
) {
    val state by haterViewModel.haterState.collectAsStateWithLifecycle()
    LocalDensity.current

    LaunchedEffect(Unit) {
        haterViewModel.onEvent(HaterEvent.EnterHaterMode)
    }

    val glowPaint = remember {
        Paint().asFrameworkPaint().apply {
            color = Color(0x993366FF).toArgb()
            maskFilter = BlurMaskFilter(30f, BlurMaskFilter.Blur.NORMAL)
        }
    }
    val trianglePaint = remember { Paint().apply { color = Color(0xFF3366FF) } }
    val particleColor = remember { Color(0xAA013FE8) }
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
                        onDrag = { change, dragAmount ->
                            change.consume()
                            haterViewModel.onEvent(HaterEvent.Dragging(dragAmount))
                        },
                        onDragEnd = { haterViewModel.onEvent(HaterEvent.DragEnd) }
                    )
                }
        ) {
            haterViewModel.setupBoundaries(size.width, size.height)
            val centerX = size.width / 2
            val centerY = size.height / 2

            drawRect(color = Color.Black)

            // Draw Particles using DrawScope API
            state.particles.forEach { particleOffset ->
                drawCircle(
                    color = particleColor,
                    radius = 4.dp.toPx(),
                    center = Offset(centerX + particleOffset.x, centerY + particleOffset.y)
                )
            }

            // Draw Die and Text using native canvas for complex path/text rendering
            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.translate(centerX + state.diePosition.x, centerY + state.diePosition.y)
                canvas.rotate(state.dieAngle)

                // Calculate triangle shape based on text size
                val text = state.answer
                textPaint.textSize = 22.sp.toPx()
                val layoutWidth = (200.dp.toPx()).toInt()
                val staticLayout = StaticLayout.Builder
                    .obtain(text, 0, text.length, textPaint, layoutWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .build()
                val textHeight = staticLayout.height.toFloat()
                val padding = 30.dp.toPx()
                val triangleHeight = textHeight + padding
                val sideLength = (triangleHeight / (sqrt(3.0) / 2.0)).toFloat()
                val halfWidth = sideLength / 2.0f
                val topY = -(2.0f / 3.0f) * triangleHeight
                val bottomY = (1.0f / 3.0f) * triangleHeight

                val trianglePath = Path().apply {
                    moveTo(0f, topY)
                    lineTo(-halfWidth, bottomY)
                    lineTo(halfWidth, bottomY)
                    close()
                }

                canvas.nativeCanvas.drawPath(trianglePath.asAndroidPath(), glowPaint)
                canvas.drawPath(path = trianglePath, paint = trianglePaint)

                // Draw Text inside die
                canvas.save()
                canvas.translate(-staticLayout.width / 2f, -staticLayout.height / 2f)
                staticLayout.draw(canvas.nativeCanvas)
                canvas.restore()

                canvas.restore()
            }
        }

        TopControls(
            uiState = uiState,
            onEvent = onEvent,
            onMenuClick = { onEvent(MainScreenEvent.ToggleNavigationRail) }
        )

        AzNavRailMenu ( uiState = uiState, onEvent = onEvent)
    }
}