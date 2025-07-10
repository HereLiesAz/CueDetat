package com.hereliesaz.cuedetat.view.gestures

import android.graphics.PointF
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

fun Modifier.detectManualGestures(onEvent: (MainScreenEvent) -> Unit) =
    this.pointerInput(Unit) {
        forEachGesture {
            awaitPointerEventScope {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                onEvent(MainScreenEvent.ScreenGestureStarted(PointF(down.position.x, down.position.y)))

                var zoom = 1f
                var pan = Offset.Zero
                var pastTouchSlop = false

                do {
                    val event = awaitPointerEvent()
                    val canceled = event.changes.any { it.isConsumed }
                    if (!canceled) {
                        val (panChange, zoomChange) = calculatePanAndZoom(event.changes)

                        if (!pastTouchSlop) {
                            zoom *= zoomChange
                            pan += panChange
                            val panMotion = pan.getDistance()

                            if (panMotion > viewConfiguration.touchSlop) {
                                pastTouchSlop = true
                            }
                        }

                        if (pastTouchSlop) {
                            if (zoomChange != 1f) {
                                onEvent(MainScreenEvent.ZoomScaleChanged(zoomChange))
                            }
                            if (panChange != Offset.Zero) {
                                val centroid = calculateCentroid(event.changes, useCurrent = true) ?: event.changes[0].position
                                val previousCentroid = centroid - panChange
                                onEvent(
                                    MainScreenEvent.Drag(
                                        previousPosition = PointF(previousCentroid.x, previousCentroid.y),
                                        currentPosition = PointF(centroid.x, centroid.y)
                                    )
                                )
                            }
                        }
                    }
                    event.changes.forEach { if (it.pressed) it.consume() }
                } while (!canceled && event.changes.any { it.pressed })

                onEvent(MainScreenEvent.GestureEnded)
            }
        }
    }

private fun calculateCentroid(changes: List<PointerInputChange>, useCurrent: Boolean): Offset? {
    if (changes.isEmpty()) return null
    var x = 0f
    var y = 0f
    changes.forEach {
        val pos = if (useCurrent) it.position else it.previousPosition
        x += pos.x
        y += pos.y
    }
    return Offset(x / changes.size, y / changes.size)
}

private fun calculatePanAndZoom(changes: List<PointerInputChange>): Pair<Offset, Float> {
    val oldCentroid = calculateCentroid(changes, useCurrent = false)
    val newCentroid = calculateCentroid(changes, useCurrent = true)

    if (oldCentroid == null || newCentroid == null) {
        return Pair(Offset.Zero, 1f)
    }

    val pan = newCentroid - oldCentroid

    val oldDistance = averageDistance(changes, useCurrent = false)
    val newDistance = averageDistance(changes, useCurrent = true)

    val zoom = if (oldDistance > 0f) newDistance / oldDistance else 1f

    return Pair(pan, zoom)
}

private fun averageDistance(changes: List<PointerInputChange>, useCurrent: Boolean): Float {
    if (changes.size < 2) return 0f
    var totalDistance = 0f
    var pairs = 0
    for (i in changes.indices) {
        for (j in i + 1 until changes.size) {
            val pos1 = if (useCurrent) changes[i].position else changes[i].previousPosition
            val pos2 = if (useCurrent) changes[j].position else changes[j].previousPosition
            totalDistance += (pos1 - pos2).getDistance()
            pairs++
        }
    }
    return totalDistance / pairs
}