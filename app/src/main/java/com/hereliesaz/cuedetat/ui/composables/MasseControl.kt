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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.hereliesaz.cuedetat.R
import kotlin.math.roundToInt
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.ui.theme.WarningRed
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
    val context = LocalContext.current
    val barbaroTypeface = remember {
        try { ResourcesCompat.getFont(context, R.font.barbaro) } catch (e: Exception) { null }
    }
    val colorWheelBitmap = rememberSpinColorWheel()
    val labelPaint = rememberSpinLabelPaint()
    val degreePaint = remember(barbaroTypeface) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(204, 255, 255, 255)  // 80% white
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
            typeface = barbaroTypeface
        }
    }
    val stickPath = remember { Path() }
    val masseDescription = stringResource(R.string.masse_control_description)
    val wheelLabel = stringResource(R.string.masse_wheel_label)
    val masseState = stringResource(
        R.string.masse_state,
        spinStateDescription(selectedSpinOffset ?: lingeringSpinOffset),
        elevationAngle.roundToInt(),
    )

    Box(
        modifier = modifier
            .size(120.dp)
            .semantics {
                contentDescription = masseDescription
                stateDescription = masseState
            }
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

                            val radiusPx = size.width / 2f
                            val dx = secondDown.position.x - radiusPx
                            val dy = secondDown.position.y - radiusPx
                            val distSq = dx * dx + dy * dy

                            if (distSq < radiusPx * radiusPx) {
                                // Double tap INSIDE the wheel: RESET
                                onEvent(MainScreenEvent.ClearSpinState)
                            } else {
                                // Double tap OUTSIDE the wheel: MOVE mode
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

                // Color Wheel (cached bitmap — drawn once per density change)
                drawImage(colorWheelBitmap, alpha = spinPathAlpha)
                drawCircle(color = Color.White.copy(alpha = 0.5f * spinPathAlpha), radius = radius, center = center, style = Stroke(width = 2.dp.toPx()))

                // Center label
                val labelTextSize = 12.dp.toPx()
                drawIntoCanvas { canvas ->
                    labelPaint.color = android.graphics.Color.argb((180 * spinPathAlpha).toInt(), 30, 30, 30)
                    labelPaint.textSize = labelTextSize
                    canvas.nativeCanvas.drawText(wheelLabel, center.x, center.y + labelTextSize * 0.35f, labelPaint)
                }

                // Indicators
                lingeringSpinOffset?.let { 
                    drawSpinIndicator(it, center, radius, Color.White.copy(alpha = 0.6f * spinPathAlpha)) 
                }

                selectedSpinOffset?.let { activePos ->
                    drawSpinIndicator(activePos, center, radius, Color.White)
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

                stickPath.apply {
                    reset()
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

            // Degree readout — bottom-right corner, above the baseline
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    "${elevationAngle.roundToInt()}°",
                    size.width - 12.dp.toPx(),
                    size.height - 8.dp.toPx(),
                    degreePaint.apply { textSize = 14.dp.toPx() }
                )
            }
        }
    }
}
