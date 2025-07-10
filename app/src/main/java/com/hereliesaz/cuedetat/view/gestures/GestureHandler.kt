package com.hereliesaz.cuedetat.view.gestures

import android.graphics.PointF
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import com.hereliesaz.cuedetat.ui.MainScreenEvent

fun Modifier.detectManualGestures(onEvent: (MainScreenEvent) -> Unit) =
    this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            onEvent(MainScreenEvent.ScreenGestureStarted(PointF(down.position.x, down.position.y)))
            down.consume()

            var lastCentroid: Offset = down.position

            do {
                val event = awaitPointerEvent()
                val canceled = event.changes.any { it.isConsumed }
                if (!canceled) {
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()
                    val centroid = event.calculateCentroid()

                    if (zoom != 1f) {
                        onEvent(MainScreenEvent.ZoomScaleChanged(zoom))
                    }

                    if (pan != Offset.Zero) {
                        val previousPosition = lastCentroid
                        val currentPosition = centroid
                        onEvent(
                            MainScreenEvent.Drag(
                                previousPosition = PointF(previousPosition.x, previousPosition.y),
                                currentPosition = PointF(currentPosition.x, currentPosition.y)
                            )
                        )
                        lastCentroid = centroid
                    }
                }
                event.changes.forEach { if (it.pressed) it.consume() }
            } while (!canceled && event.changes.any { it.pressed })

            onEvent(MainScreenEvent.GestureEnded)
        }
    }