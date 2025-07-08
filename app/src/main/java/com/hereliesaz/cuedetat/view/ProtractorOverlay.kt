package com.hereliesaz.cuedetat.view

import android.graphics.PointF
import android.graphics.Typeface
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.renderer.OverlayRenderer
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun ProtractorOverlay(
    uiState: OverlayState,
    systemIsDark: Boolean,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val paints = remember { PaintCache() }
    val renderer = remember { OverlayRenderer() }
    val barbaroTypeface: Typeface? = remember {
        if (!View(context).isInEditMode) {
            ResourcesCompat.getFont(context, R.font.barbaro)
        } else null
    }

    LaunchedEffect(barbaroTypeface) {
        paints.setTypeface(barbaroTypeface)
    }

    LaunchedEffect(uiState, systemIsDark) {
        paints.updateColors(uiState, systemIsDark)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                onEvent(MainScreenEvent.SizeChanged(size.width, size.height))
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onEvent(MainScreenEvent.ScreenGestureStarted(PointF(it.x, it.y)))
                        val released = tryAwaitRelease()
                        onEvent(MainScreenEvent.GestureEnded)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onEvent(MainScreenEvent.ScreenGestureStarted(PointF(offset.x, offset.y)))
                    },
                    onDragEnd = { onEvent(MainScreenEvent.GestureEnded) },
                    onDragCancel = { onEvent(MainScreenEvent.GestureEnded) }
                ) { change, dragAmount ->
                    onEvent(MainScreenEvent.Drag(dragAmount))
                    change.consume()
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    onEvent(MainScreenEvent.ZoomScaleChanged(zoom))
                }
            }
    ) {
        drawContext.canvas.nativeCanvas.also { canvas ->
            renderer.draw(canvas, uiState, paints, barbaroTypeface)
        }
    }
}