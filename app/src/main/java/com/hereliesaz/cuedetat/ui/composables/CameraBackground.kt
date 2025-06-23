// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/CameraBackground.kt
package com.hereliesaz.cuedetat.ui.composables

import android.content.Context
import android.util.Log
import android.util.Range // Import Range for setTargetFrameRate
import androidx.camera.core.CameraSelector
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
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "CameraBackground"

@Composable
fun CameraBackground(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    // Create the PreviewView once and remember it across recompositions
    val previewView = remember { PreviewView(context) }

    // Use DisposableEffect to manage camera lifecycle
    DisposableEffect(lifecycleOwner) { // Re-bind if lifecycleOwner changes
        startCamera(context, lifecycleOwner, cameraExecutor, previewView)
        onDispose {
            cameraExecutor.shutdown()
            // Ensure camera is unbound when the composable leaves the composition
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            }, ContextCompat.getMainExecutor(context))
        }
    }

    // Embed the Android PreviewView into Compose UI
    AndroidView(
        factory = {
            previewView.apply {
                this.scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = modifier
    )
}

private fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
    previewView: PreviewView // The PreviewView instance from Compose
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            // First bind a minimal preview to get cameraInfo and check capabilities
            // Then unbind it to prepare for the final binding with stabilization settings
            val cameraForInfo = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
            val cameraInfo = cameraForInfo.cameraInfo
            cameraProvider.unbindAll() // Unbind all use cases before rebinding with new settings

            val previewBuilder = Preview.Builder()

            // Check for preview stabilization capabilities and apply if supported
            val previewCapabilities = Preview.getPreviewCapabilities(cameraInfo)
            if (previewCapabilities.isStabilizationSupported) {
                previewBuilder.setPreviewStabilizationEnabled(true)
                Log.d(TAG, "Requested Preview Stabilization to be enabled on the Preview.Builder")
            } else {
                Log.w(TAG, "Preview Stabilization is NOT supported on this device.")
            }
            // Attempt to set a higher frame rate for smoother preview
            // Corrected: setTargetFrameRate expects a Range<Int>
            previewBuilder.setTargetFrameRate(Range(60, 60)) // Use Range for target frame rate

            // Build the Preview use case with configured settings
            val previewUseCase = previewBuilder.build()
            // Set the surface provider for the Preview use case to the PreviewView
            previewUseCase.setSurfaceProvider(previewView.surfaceProvider)

            // Re-bind use cases with the configured preview use case
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                previewUseCase
            )

        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }

    }, ContextCompat.getMainExecutor(context))
}