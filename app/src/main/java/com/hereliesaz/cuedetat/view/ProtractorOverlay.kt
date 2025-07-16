// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/ProtractorOverlay.kt
package com.hereliesaz.cuedetat.view

import android.graphics.Typeface
import android.os.Build
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.magnifier
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.gestures.detectManualGestures
import com.hereliesaz.cuedetat.view.renderer.OverlayRenderer
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun ProtractorOverlay(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    renderer: OverlayRenderer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val barbaroTypeface: Typeface? = remember {
        if (!View(context).isInEditMode) {
            ResourcesCompat.getFont(context, R.font.barbaro)
        } else null
    }

    val magnifierModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && uiState.isMagnifierVisible) {
        Modifier.magnifier(
            sourceCenter = {
                uiState.magnifierSourceCenter ?: Offset.Unspecified
            },
            magnifierCenter = {
                val source = uiState.magnifierSourceCenter
                if (source != null && source != Offset.Unspecified) {
                    val yOffsetPx = with(this) { 100.dp.toPx() }
                    source - Offset(0f, yOffsetPx)
                } else {
                    Offset.Unspecified
                }
            },
            zoom = 2f
        )
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                onEvent(MainScreenEvent.SizeChanged(size.width, size.height))
            }
            .detectManualGestures(onEvent)
            .then(magnifierModifier)
    ) {
        drawIntoCanvas { canvas ->
            renderer.draw(canvas.nativeCanvas, uiState, barbaroTypeface)
        }
    }
}