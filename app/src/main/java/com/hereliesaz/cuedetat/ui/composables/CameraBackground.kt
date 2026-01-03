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
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

private const val TAG = "CameraBackground"

@Composable
fun CameraBackground(
    modifier: Modifier = Modifier,
    analyzer: ImageAnalysis.Analyzer
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(lifecycleOwner, analyzer) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        val cameraProviderListener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, analyzer)
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                Log.d(TAG, "Camera bound to lifecycle with Preview and ImageAnalysis.")

            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }

        cameraProviderFuture.addListener(cameraProviderListener, mainExecutor)

        onDispose {
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                    Log.d(TAG, "Camera unbound on dispose.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to unbind camera on dispose", e)
                } finally {
                    cameraExecutor.shutdown()
                }
            }, mainExecutor)
        }
    }

    AndroidView(
        factory = { previewView.apply { this.scaleType = PreviewView.ScaleType.FILL_CENTER } },
        modifier = modifier
    )
}