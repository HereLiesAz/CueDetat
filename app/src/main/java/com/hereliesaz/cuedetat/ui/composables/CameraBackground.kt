// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/CameraBackground.kt
package com.hereliesaz.cuedetat.ui.composables

import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

private const val TAG = "CameraBackground"

/**
 * A Composable that renders the device's camera feed as the background.
 *
 * It uses CameraX to bind the camera lifecycle to the Compose lifecycle.
 *
 * @param modifier Modifier for styling the view (fill size, z-index, etc).
 * @param analyzer The CV analyzer to attach to the ImageAnalysis use case.
 *                 Pass **null** to run preview-only (no ImageAnalysis bound at all) —
 *                 use this for static beginner mode where CV must not run.
 */
@Composable
fun CameraBackground(
    modifier: Modifier = Modifier,
    analyzer: ImageAnalysis.Analyzer?
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    // Executor only needed when an analyzer is attached.
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Re-runs when lifecycleOwner or analyzer changes (null ↔ non-null causes rebind).
    DisposableEffect(lifecycleOwner, analyzer) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        val cameraProviderListener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()

                if (analyzer != null) {
                    // Full binding: preview + CV analysis.
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(Size(640, 480))
                        .build()
                        .also { it.setAnalyzer(cameraExecutor, analyzer) }
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                    Log.d(TAG, "Camera bound: Preview + ImageAnalysis.")
                } else {
                    // Preview-only binding: no ImageAnalysis, no CV frames delivered.
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                    Log.d(TAG, "Camera bound: Preview only (CV disabled).")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }

        cameraProviderFuture.addListener(cameraProviderListener, mainExecutor)

        onDispose {
            cameraProviderFuture.addListener({
                try {
                    cameraProviderFuture.get().unbindAll()
                    Log.d(TAG, "Camera unbound on dispose.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to unbind camera on dispose", e)
                } finally {
                    cameraExecutor.shutdown()
                }
            }, mainExecutor)
        }
    }

    // Render the Android View (PreviewView) inside Compose layout.
    AndroidView(
        factory = { previewView.apply { this.scaleType = PreviewView.ScaleType.FILL_CENTER } },
        modifier = modifier
    )
}
