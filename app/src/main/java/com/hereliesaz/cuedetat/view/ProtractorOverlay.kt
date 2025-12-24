package com.hereliesaz.cuedetat.view

import android.graphics.Typeface
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.gestures.detectManualGestures
import com.hereliesaz.cuedetat.view.renderer.OverlayRenderer

@Composable
fun ProtractorOverlay(
    uiState: CueDetatState,
    systemIsDark: Boolean,
    isTestingCvMask: Boolean,
    onEvent: (MainScreenEvent) -> Unit
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
        paints.updateColors(systemIsDark)
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                onEvent(MainScreenEvent.SizeChanged(size.width, size.height))
            }
            .detectManualGestures(uiState, onEvent)
    ) {
        drawIntoCanvas { canvas ->
            renderer.draw(canvas.nativeCanvas, uiState, paints, barbaroTypeface)
        }
    }
}