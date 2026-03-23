package com.hereliesaz.cuedetat.ui.composables

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import com.hereliesaz.cuedetat.view.renderer.util.SpinColorUtils
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun MasseControl(
    modifier: Modifier = Modifier,
    selectedSpinOffset: PointF?,
    lingeringSpinOffset: PointF?,
    spinPathAlpha: Float,
    elevationAngle: Float,
    onEvent: (MainScreenEvent) -> Unit
) {
    var isMoveModeActive by remember { mutableStateOf(false) }
    val moveIconPainter = rememberVectorPainter(image = Icons.Default.OpenWith)
    val viewConfig = LocalViewConfiguration.current

    Box(
        modifier = modifier
            .size(120.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val firstUp = waitForUpOrCancellation()
                    if (firstUp != null) {
                        firstUp.consume()
                        val secondDown = withTimeoutOrNull(viewConfig.doubleTapTimeoutMillis) {
                            awaitFirstDown(requireUnconsumed = false)
                        }
                        if (secondDown != null) {
                            secondDown.consume()
                            isMoveModeActive = true
                            var pointerId = secondDown.id
                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val dragChange = event.changes.find { it.id == pointerId }
                                    if (dragChange == null || !dragChange.pressed) break
                                    val pan = dragChange.positionChange()
                                    if (pan != Offset.Zero) {
                                        onEvent(MainScreenEvent.DragSpinControl(PointF(pan.x, pan.y)))
                                        dragChange.consume()
                                    }
                                }
                            } finally {
                                isMoveModeActive = false
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .size(120.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            onEvent(MainScreenEvent.SpinApplied(PointF(offset.x, offset.y)))
                        },
                        onDragEnd = { onEvent(MainScreenEvent.SpinSelectionEnded) },
                        onDragCancel = { onEvent(MainScreenEvent.SpinSelectionEnded) }
                    ) { change, _ ->
                        onEvent(MainScreenEvent.SpinApplied(PointF(change.position.x, change.position.y)))
                        change.consume()
                    }
                }
        ) {
            val radius = size.minDimension / 2f
            val center = Offset(radius, radius)
            val scaleFactor = if (isMoveModeActive) 1.1f else 1.0f

            withTransform({ scale(scaleFactor, scaleFactor, center) }) {
                if (isMoveModeActive) {
                    drawCircle(color = Color.White.copy(alpha = 0.2f), radius = radius, center = center)
                }

                // Color Wheel
                val numArcs = 72
                val arcAngle = 360f / numArcs
                for (i in 0 until numArcs) {
                    val startAngle = i * arcAngle
                    val color = SpinColorUtils.getColorFromAngleAndDistance(startAngle + (arcAngle / 2), 1.0f)
                    drawArc(color = color, startAngle = startAngle, sweepAngle = arcAngle, useCenter = true, alpha = spinPathAlpha)
                }

                drawCircle(brush = Brush.radialGradient(colors = listOf(Color.White, Color.Transparent), center = center, radius = radius), radius = radius, center = center, alpha = spinPathAlpha)
                drawCircle(color = Color.White.copy(alpha = 0.5f * spinPathAlpha), radius = radius, center = center, style = Stroke(width = 2.dp.toPx()))

                // Center label
                val labelTextSize = 12.dp.toPx()
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        "TOP",
                        center.x,
                        center.y + labelTextSize * 0.35f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.argb((180 * spinPathAlpha).toInt(), 30, 30, 30)
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = labelTextSize
                            isFakeBoldText = true
                            isAntiAlias = true
                        }
                    )
                }

                // Indicators
                lingeringSpinOffset?.let { drawLogicalIndicator(it, color = Color.White.copy(alpha = 0.6f * spinPathAlpha)) }

                selectedSpinOffset?.let { activePos ->
                    drawLogicalIndicator(activePos, color = Color.White)
                }

                if (isMoveModeActive) {
                    with(moveIconPainter) {
                        translate(left = center.x - intrinsicSize.width / 2, top = center.y - intrinsicSize.height / 2) {
                            draw(size = intrinsicSize, colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.8f)))
                        }
                    }
                }
            }
        }

        // Layout Constants
        val stickCanvasWidth = 200.dp
        val stickCanvasHeight = 150.dp
        val tetherOffsetY = 144.dp

        // Side-view Pool Stick for Elevation
        Canvas(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = tetherOffsetY)
                .size(width = stickCanvasWidth, height = stickCanvasHeight)
        ) {
            val tipMargin = 20.dp.toPx()
            val tipX = tipMargin
            val tipY = size.height - tipMargin
            val stickLength = 160.dp.toPx()

            // Draw a baseline for reference (the table surface)
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(tipX - tipMargin, tipY),
                end = Offset(tipX + stickLength, tipY),
                strokeWidth = 2.dp.toPx()
            )

            // Draw the cue stick
            withTransform({
                translate(left = tipX, top = tipY)
                rotate(degrees = -elevationAngle, pivot = Offset.Zero) // Negative visual rotation points it up
            }) {
                val tipHalfThicknessPx = 2.dp.toPx()
                val buttHalfThicknessPx = 6.dp.toPx()

                val stickPath = Path().apply {
                    moveTo(0f, -tipHalfThicknessPx) // Tip top
                    lineTo(stickLength, -buttHalfThicknessPx) // Butt top
                    lineTo(stickLength, buttHalfThicknessPx) // Butt bottom
                    lineTo(0f, tipHalfThicknessPx) // Tip bottom
                    close()
                }
                drawPath(path = stickPath, color = Color.White)

                // Ferrule/Tip detail
                drawRect(
                    color = Color.Blue,
                    topLeft = Offset(0f, -tipHalfThicknessPx),
                    size = androidx.compose.ui.geometry.Size(4.dp.toPx(), tipHalfThicknessPx * 2f)
                )
            }

            // Draw pivot point indicator
            drawCircle(
                color = WarningRed,
                radius = 4.dp.toPx(),
                center = Offset(tipX, tipY)
            )
        }
    }
}

private fun DrawScope.drawLogicalIndicator(pos: PointF, color: Color) {
    drawCircle(color = color, radius = 5.dp.toPx(), center = Offset(pos.x, pos.y))
    drawCircle(color = color, radius = 5.dp.toPx(), center = Offset(pos.x, pos.y), style = Stroke(width = 2.dp.toPx()))
}