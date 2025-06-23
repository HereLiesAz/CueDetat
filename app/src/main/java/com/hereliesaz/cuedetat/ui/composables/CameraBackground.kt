package com.hereliesaz.cuedetat.ui.composables

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import android.util.Log

@Composable
fun CameraBackground(
    modifier: Modifier = Modifier,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val tag = "CameraBackground"

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                this.scaleType = scaleType
            }
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val previewUseCaseBuilder = Preview.Builder()

                    previewUseCaseBuilder.setPreviewStabilizationEnabled(true)
                    Log.i(tag, "Requested Preview Stabilization to be enabled on the Preview.Builder.")

                    val preview = previewUseCaseBuilder.build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)

                    // Check general support on CameraInfo after binding
                    if (camera.cameraInfo.isPreviewStabilizationSupported) {
                        Log.i(tag, "Camera (via CameraInfo) reports general support for Preview Stabilization.")
                        // Note: This doesn't confirm it's *active* on the Preview instance itself for all CameraX versions.
                        // For CameraX 1.3.0-alpha04+, you could check `preview.isPreviewStabilizationEnabled` (getter).
                        // For older versions, if supported and requested, CameraX does its best.
                    } else {
                        Log.w(tag, "Camera (via CameraInfo) reports NO general support for Preview Stabilization.")
                    }

                } catch (exc: Exception) {
                    Log.e(tag, "Use case binding or stabilization setup failed", exc)
                }
            }, executor)
            previewView
        },
        modifier = modifier
    )
}