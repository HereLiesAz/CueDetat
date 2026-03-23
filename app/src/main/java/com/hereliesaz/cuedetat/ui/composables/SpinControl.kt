package com.hereliesaz.cuedetat.ui.composables

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.renderer.util.SpinColorUtils
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun SpinControl(
    modifier: Modifier = Modifier,
    selectedSpinOffset: PointF?,
    lingeringSpinOffset: PointF?,
    spinPathAlpha: Float,
    onEvent: (MainScreenEvent) -> Unit
) {
    var isMoveModeActive by remember { mutableStateOf(false) }
    val moveIconPainter = rememberVectorPainter(image = Icons.Default.OpenWith)
    val viewConfiguration = LocalViewConfiguration.current
    val density = LocalDensity.current.density
    val colorWheelBitmap = remember(density) {
        val sizePx = (120f * density).toInt().coerceAtLeast(1)
        val bmp = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
        val bmpCanvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val rectF = android.graphics.RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
        val numArcs = 72
        val arcAngle = 360f / numArcs
        for (i in 0 until numArcs) {
            val startAngle = i * arcAngle
            paint.color = SpinColorUtils.getColorFromAngleAndDistance(startAngle + arcAngle / 2, 1.0f).toArgb()
            bmpCanvas.drawArc(rectF, startAngle, arcAngle, true, paint)
        }
        val center = sizePx / 2f
        val gradientPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        gradientPaint.shader = android.graphics.RadialGradient(
            center, center, center,
            intArrayOf(android.graphics.Color.WHITE, android.graphics.Color.TRANSPARENT),
            null, android.graphics.Shader.TileMode.CLAMP
        )
        bmpCanvas.drawCircle(center, center, center, gradientPaint)
        bmp.asImageBitmap()
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val firstUp = waitForUpOrCancellation()
                    if (firstUp != null) {
                        firstUp.consume()
                        val secondDown = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
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
                    val radiusPx = size.width / 2f
                    detectDragGestures(
                        onDragStart = { offset ->
                            val normalized = PointF((offset.x - radiusPx) / radiusPx, (offset.y - radiusPx) / radiusPx)
                            onEvent(MainScreenEvent.SpinApplied(normalized))
                        },
                        onDragEnd = { onEvent(MainScreenEvent.SpinSelectionEnded) },
                        onDragCancel = { onEvent(MainScreenEvent.SpinSelectionEnded) }
                    ) { change, _ ->
                        val pos = change.position
                        val normalized = PointF((pos.x - radiusPx) / radiusPx, (pos.y - radiusPx) / radiusPx)
                        onEvent(MainScreenEvent.SpinApplied(normalized))
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

                drawImage(colorWheelBitmap, alpha = spinPathAlpha)
                drawCircle(color = Color.White.copy(alpha = 0.5f * spinPathAlpha), radius = radius, center = center, style = Stroke(width = 2.dp.toPx()))

                // Center label
                val labelTextSize = 12.dp.toPx()
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        "SIDE",
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

                lingeringSpinOffset?.let { drawLogicalIndicator(it, center, radius, Color.White.copy(alpha = 0.6f * spinPathAlpha)) }
                selectedSpinOffset?.let { drawLogicalIndicator(it, center, radius, Color.White) }

                if (isMoveModeActive) {
                    with(moveIconPainter) {
                        translate(left = center.x - intrinsicSize.width / 2, top = center.y - intrinsicSize.height / 2) {
                            draw(size = intrinsicSize, colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.8f)))
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawLogicalIndicator(offset: PointF, center: Offset, radius: Float, color: Color) {
    val indicatorX = center.x + (offset.x * radius)
    val indicatorY = center.y + (offset.y * radius)
    drawCircle(color = color, radius = 5.dp.toPx(), center = Offset(indicatorX, indicatorY))
    drawCircle(color = color, radius = 5.dp.toPx(), center = Offset(indicatorX, indicatorY), style = Stroke(width = 2.dp.toPx()))
}