package com.hereliesaz.cuedetat.view.gestures

import android.graphics.PointF
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import com.hereliesaz.cuedetat.ui.MainScreenEvent

fun Modifier.detectManualGestures(
    onEvent: (MainScreenEvent) -> Unit
): Modifier = this.pointerInput(Unit) {
    detectTapGestures(
        onPress = { offset ->
            onEvent(MainScreenEvent.ScreenGestureStarted(PointF(offset.x, offset.y)))
            tryAwaitRelease()
        }
    )
}.pointerInput(Unit) {
    detectDragGestures(
        onDrag = { change, dragAmount ->
            change.consume()
            val prevPos = change.position - dragAmount
            onEvent(
                MainScreenEvent.Drag(
                    PointF(prevPos.x, prevPos.y),
                    PointF(change.position.x, change.position.y)
                )
            )
        },
        onDragEnd = { onEvent(MainScreenEvent.GestureEnded) }
    )
}
