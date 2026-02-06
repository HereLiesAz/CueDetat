// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/CameraPreview.kt

package com.hereliesaz.cuedetat.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * A basic Camera Preview composable.
 *
 * Note: This appears to be a simplified version of [com.hereliesaz.cuedetat.ui.composables.CameraBackground].
 * It lacks the ImageAnalysis binding required for Computer Vision.
 * It might be used in contexts where only a viewfinder is needed, or as a fallback.
 *
 * @param modifier Styling modifier.
 * @param scaleType How the preview should scale to fill the view.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current // This now uses the correct dependency
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                this.scaleType = scaleType
            }
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                } catch (exc: Exception) {
                    // Log error or handle exception
                }
            }, executor)
            previewView
        },
        modifier = modifier
    )
}
