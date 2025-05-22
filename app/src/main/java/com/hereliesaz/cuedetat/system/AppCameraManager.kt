package com.hereliesaz.cuedetat.system

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import com.hereliesaz.cuedetat.protractor.ProtractorConfig
import com.hereliesaz.cuedetat.protractor.ProtractorPaints
import com.hereliesaz.cuedetat.protractor.ProtractorState
import com.hereliesaz.cuedetat.protractor.ProtractorGestureHandler
import com.hereliesaz.cuedetat.protractor.ProtractorOverlayView
import com.hereliesaz.cuedetat.protractor.drawer.HelperTextDrawer
import com.hereliesaz.cuedetat.protractor.drawer.ProtractorDrawingCoordinator
import com.hereliesaz.cuedetat.protractor.drawer.GhostBallDrawer
import com.hereliesaz.cuedetat.protractor.drawer.ProtractorPlaneDrawer

class AppCameraManager(
    private val activity: AppCompatActivity, // For registering launcher and context
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {
    private companion object {
        private const val TAG = "AppCameraManager"
    }

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var requestCameraPermissionLauncher: ActivityResultLauncher<String>

    init {
        requestCameraPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                initializeCamera()
            } else {
                Toast.makeText(activity, "Camera permission is required to use this app.", Toast.LENGTH_LONG).show()
                // Optionally, disable camera-dependent features or close the app
            }
        }
    }

    fun checkPermissionsAndSetupCamera() {
        when {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                initializeCamera()
            }
            activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Show an educational UI explaining why permission is needed, then request
                Toast.makeText(activity, "Camera access is crucial for this app.", Toast.LENGTH_LONG).show()
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun initializeCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        lifecycleOwner.lifecycleScope.launch {
            try {
                val cameraProvider = cameraProviderFuture.await()
                bindPreview(cameraProvider)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing camera: ", e)
                Toast.makeText(activity, "Could not initialize camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview: Preview = Preview.Builder().build()
        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.surfaceProvider = previewView.surfaceProvider

        try {
            cameraProvider.unbindAll() // Unbind use cases before rebinding
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview
            )
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
            Toast.makeText(activity, "Camera binding failed: ${exc.message}", Toast.LENGTH_LONG).show()
        }
    }
}