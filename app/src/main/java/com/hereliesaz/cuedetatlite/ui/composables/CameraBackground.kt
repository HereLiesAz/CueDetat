package com.hereliesaz.cuedetatlite.ui.composables

import android.util.Log
import android.util.Range
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

private const val TAG = "CameraBackground"

@Composable
fun CameraBackground(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor: Executor = remember { ContextCompat.getMainExecutor(context) }

    LaunchedEffect(cameraProviderFuture) {
        val cameraProvider = cameraProviderFuture.get()
        cameraProviderFuture.addListener({
            try {
                // Must unbind the use-cases before rebinding them.
                cameraProvider.unbindAll()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                val cameraForInfo = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
                val cameraInfo = cameraForInfo.cameraInfo
                cameraProvider.unbindAll()

                val previewBuilder = Preview.Builder()
                    .setTargetFrameRate(Range(60, 60))

                val previewCapabilities = Preview.getPreviewCapabilities(cameraInfo)
                if (previewCapabilities.isStabilizationSupported) {
                    previewBuilder.setPreviewStabilizationEnabled(true)
                    Log.d(TAG, "Preview Stabilization has been enabled.")
                } else {
                    Log.w(TAG, "Preview Stabilization is NOT supported on this device.")
                }

                val preview = previewBuilder.build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, cameraExecutor)
    }

    AndroidView(
        factory = {
            previewView.apply {
                this.scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = modifier
    )
}