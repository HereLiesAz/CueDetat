// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/CameraBackground.kt
package com.hereliesaz.cuedetat.ui.composables

import android.content.Context
import android.util.Log
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

private const val TAG = "CameraBackground"

@Composable
fun CameraBackground(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    // This effect binds the camera when the composable enters the composition
    // and guarantees it unbinds when the composable leaves. This is the
    // blessed path for CameraX in Compose.
    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        val cameraProviderListener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                Log.d(TAG, "Camera bound to lifecycle.")
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }

        cameraProviderFuture.addListener(cameraProviderListener, mainExecutor)

        onDispose {
            // This is called when uiState.isCameraVisible becomes false,
            // removing this composable from the tree.
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                    Log.d(TAG, "Camera unbound on dispose.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to unbind camera on dispose", e)
                }
            }, mainExecutor)
        }
    }

    AndroidView(
        factory = { previewView.apply { this.scaleType = PreviewView.ScaleType.FILL_CENTER } },
        modifier = modifier
    )
}