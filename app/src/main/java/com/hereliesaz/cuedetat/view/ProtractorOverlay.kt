// app/src/main/java/com/hereliesaz/cuedetat/view/ProtractorOverlay.kt
package com.hereliesaz.cuedetat.view

import android.graphics.Typeface
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
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

/**
 * The main AR/2D overlay canvas.
 *
 * Redraw policy: there is no gate. The Canvas draw lambda closes over [uiState] and
 * [topDownProgress], so Compose redraws whenever this composable recomposes with new
 * values. A previous `DrawSignature`/`derivedStateOf` "gatekeeper" claimed to suppress
 * redraws but read plain (non-snapshot) data inside `remember(uiState)`, so it was rebuilt
 * on every state change and suppressed nothing; it was removed rather than kept as a
 * comment that lied.
 */
@Composable
fun ProtractorOverlay(
    uiState: CueDetatState,
    systemIsDark: Boolean,
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

    // systemIsDark is intentionally not a key: overlay colours do not depend on it
    // (see PaintCache.updateColors).
    LaunchedEffect(uiState.luminanceAdjustment) {
        paints.updateColors(uiState.luminanceAdjustment)
    }

    DisposableEffect(renderer) {
        onDispose { renderer.release() }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                onEvent(MainScreenEvent.SizeChanged(size.width, size.height, density))
            }
            .detectManualGestures(uiState, onEvent)
    ) {
        drawIntoCanvas { canvas ->
            // Renderer crashes here force-close the app because Compose's drawscope
            // doesn't isolate frame errors. Catch and log so a transient null/NaN
            // in geometry on the first frame after mode-switch doesn't kill the app.
            try {
                renderer.draw(canvas.nativeCanvas, uiState, paints, barbaroTypeface, context, topDownProgress)
            } catch (e: Exception) {
                // Catch Exception (not Throwable) so JVM Errors like OutOfMemoryError
                // still propagate — recovering from those is unsafe.
                android.util.Log.e("ProtractorOverlay", "renderer.draw failed", e)
            }
        }
    }
}