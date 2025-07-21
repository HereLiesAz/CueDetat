// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/ProtractorOverlay.kt

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
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.renderer.OverlayRenderer
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun ProtractorOverlay(
    uiState: OverlayState,
    systemIsDark: Boolean,
    isTestingCvMask: Boolean,
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

    val canvasModifier = if (uiState.isTestingCvMask || uiState.isCalibratingColor) {
        // When testing or calibrating, the canvas should not detect gestures.
        modifier.fillMaxSize()
    } else {
        // The gesture handler is now applied in MainScreen.kt, not here.
        modifier.fillMaxSize()
    }

    Canvas(
        modifier = canvasModifier
            .onSizeChanged { size ->
                onEvent(MainScreenEvent.SizeChanged(size.width, size.height))
            }
    ) {
        drawIntoCanvas { canvas ->
            renderer.draw(canvas.nativeCanvas, uiState, paints, barbaroTypeface)
        }
    }
}