package com.hereliesaz.cuedetat.view.gestures

import android.graphics.PointF
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.hereliesaz.cuedetat.ui.MainScreenEvent

fun Modifier.detectManualGestures(onEvent: (MainScreenEvent) -> Unit): Modifier {
    return this.pointerInput(Unit) {
        detectTapGestures(
            onPress = { offset ->
                onEvent(MainScreenEvent.ScreenGestureStarted)
                try {
                    awaitRelease()
                } finally {
                    onEvent(MainScreenEvent.GestureEnded)
                }
            },
            onTap = { offset ->
                onEvent(MainScreenEvent.Drag(PointF(offset.x, offset.y)))
            },
            onLongPress = { offset ->
                onEvent(MainScreenEvent.Drag(PointF(offset.x, offset.y), isLongPress = true))
            }
        )
    }.pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
            onDragStart = { offset -> onEvent(MainScreenEvent.Drag(PointF(offset.x, offset.y), isLongPress = true)) },
            onDrag = { change, dragAmount ->
                change.consume()
                onEvent(MainScreenEvent.Drag(PointF(change.position.x, change.position.y), isLongPress = true))
            },
            onDragEnd = { onEvent(MainScreenEvent.GestureEnded) }
        )
    }
}