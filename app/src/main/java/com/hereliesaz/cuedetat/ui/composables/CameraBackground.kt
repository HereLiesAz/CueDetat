// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/CameraBackground.kt
package com.hereliesaz.cuedetat.ui.composables

import android.util.Log
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
 * It also attaches an [ImageAnalysis] use case to feed frames to the CV analyzer.
 *
 * @param modifier Modifier for styling the view (fill size, z-index, etc).
 * @param analyzer The ImageAnalysis.Analyzer that will process incoming frames (Computer Vision).
 */
@Composable
fun CameraBackground(
    modifier: Modifier = Modifier,
    analyzer: ImageAnalysis.Analyzer
) {
    // Get context and lifecycle owner to bind CameraX.
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // Create the PreviewView once and reuse it.
    val previewView = remember { PreviewView(context) }
    // Executor for background analysis tasks.
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Manage the camera lifecycle. Re-runs if lifecycleOwner or analyzer changes.
    DisposableEffect(lifecycleOwner, analyzer) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        val cameraProviderListener = Runnable {
            try {
                // Get the camera provider.
                val cameraProvider = cameraProviderFuture.get()

                // Build the Preview use case.
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                // Build the ImageAnalysis use case.
                val imageAnalysis = ImageAnalysis.Builder()
                    // Drop frames if the analyzer is too slow (keeps UI responsive).
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, analyzer)
                    }

                // Select the back camera.
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Unbind previous use cases before binding new ones.
                cameraProvider.unbindAll()
                // Bind lifecycle and use cases to the camera.
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                Log.d(TAG, "Camera bound to lifecycle with Preview and ImageAnalysis.")

            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }

        // Attach the listener to initialize camera when provider is ready.
        cameraProviderFuture.addListener(cameraProviderListener, mainExecutor)

        // Cleanup on composable disposal.
        onDispose {
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll() // Release camera resources.
                    Log.d(TAG, "Camera unbound on dispose.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to unbind camera on dispose", e)
                } finally {
                    cameraExecutor.shutdown() // Stop the analysis thread.
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
