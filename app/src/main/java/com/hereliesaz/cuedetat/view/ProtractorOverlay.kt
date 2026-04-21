// app/src/main/java/com/hereliesaz/cuedetat/view/ProtractorOverlay.kt
package com.hereliesaz.cuedetat.view

import android.graphics.Typeface
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
    onEvent: (MainScreenEvent) -> Unit,
    topDownProgress: Float
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
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

    LaunchedEffect(uiState.luminanceAdjustment, systemIsDark) {
        paints.updateColors(uiState, systemIsDark)
    }

    // The gatekeeper of the Sisyphean Canvas.
    // We only force a redraw when the geometry actually shifts.
    val drawTrigger by remember(uiState) {
        derivedStateOf {
            arrayOf(
                uiState.viewOffset,
                uiState.arDerivedPitch,
                uiState.worldRotationDegrees,
                uiState.zoomSliderPosition,
                uiState.tableScanModel,
                uiState.tableZOffset,
                uiState.onPlaneBall,
                topDownProgress
            ).contentHashCode()
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                onEvent(MainScreenEvent.SizeChanged(size.width, size.height, density))
            }
            .detectManualGestures(uiState, onEvent)
    ) {
        // Read the trigger to inform Compose's invalidation tracker
        drawTrigger
        drawIntoCanvas { canvas ->
            renderer.draw(canvas.nativeCanvas, uiState, paints, barbaroTypeface, context, topDownProgress)
        }
    }
}