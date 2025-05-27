// app/src/main/java/com/hereliesaz/cuedetat/system/CameraManager.kt
package com.hereliesaz.cuedetat.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.tracking.ball_detector.Ball
import com.hereliesaz.cuedetat.tracking.ball_detector.BallDetector
import com.hereliesaz.cuedetat.tracking.utils.YuvToRgbConverter
import com.hereliesaz.cuedetat.view.MainOverlayView
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class CameraManager(
    private val activity: AppCompatActivity,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val mainOverlayView: MainOverlayView // MainOverlayView instance to pass detected balls
) {
    private companion object {
        private val TAG = AppConfig.TAG + "_CameraManager"
        private const val IMAGE_ANALYSIS_WIDTH = 640 // Resolution for image analysis
        private const val IMAGE_ANALYSIS_HEIGHT = 480 // Resolution for image analysis
    }

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var requestCameraPermissionLauncher: ActivityResultLauncher<String>

    private val analysisExecutor = Executors.newSingleThreadExecutor() // Single thread for image analysis to avoid frame drops
    private lateinit var yuvToRgbConverter: YuvToRgbConverter
    private lateinit var ballDetector: BallDetector
    private var lastFrameBitmap: Bitmap? = null // Reusable bitmap for frame conversion

    init {
        requestCameraPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.i(TAG, "Camera permission granted. Initializing camera.")
                initializeCamera()
            } else {
                Log.w(TAG, "Camera permission denied.")
                Toast.makeText(activity, "Camera permission is required to use this app.", Toast.LENGTH_LONG).show()
                // Optionally, guide user to settings or disable camera-dependent features
            }
        }
    }

    fun checkPermissionsAndSetupCamera() {
        when {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i(TAG, "Camera permission already granted. Initializing camera.")
                initializeCamera()
            }
            activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Log.i(TAG, "Showing rationale for camera permission.")
                Toast.makeText(activity, "Camera access is crucial for the augmented reality features.", Toast.LENGTH_LONG).show()
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                Log.i(TAG, "Requesting camera permission.")
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun initializeCamera() {
        yuvToRgbConverter = YuvToRgbConverter(activity)
        ballDetector = BallDetector(activity) // Initialize BallDetector
        cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        lifecycleOwner.lifecycleScope.launch {
            try {
                val cameraProvider = cameraProviderFuture.await()
                bindCameraUseCases(cameraProvider)
                Log.i(TAG, "Camera initialized and use cases bound.")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing camera provider or binding use cases: ", e)
                Toast.makeText(activity, "Could not initialize camera: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK) // Use back camera
            .build()

        // Connect the Preview use case to the PreviewView
        preview.setSurfaceProvider(previewView.surfaceProvider)

        // Set up ImageAnalysis for processing camera frames
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(IMAGE_ANALYSIS_WIDTH, IMAGE_ANALYSIS_HEIGHT))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_LATEST) // Only analyze the most recent frame
            .build()
            .also {
                it.setAnalyzer(analysisExecutor, BallDetectionAnalyzer()) // Set custom analyzer
            }

        try {
            cameraProvider.unbindAll() // Unbind use cases before rebinding to prevent errors
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis // Bind ImageAnalysis along with Preview
            )
            Log.i(TAG, "Camera preview and image analysis bound to lifecycle.")
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed for camera: ", exc)
            Toast.makeText(activity, "Camera binding failed: ${exc.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Custom ImageAnalysis Analyzer for ball detection
    private inner class BallDetectionAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(imageProxy: ImageProxy) {
            // Create or reuse a Bitmap to convert YUV ImageProxy frame to RGB
            val bitmap = lastFrameBitmap ?: Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
            lastFrameBitmap = bitmap // Store for next frame reuse

            // Convert current image frame to RGB Bitmap
            yuvToRgbConverter.yuvToRgb(imageProxy.image!!, bitmap)

            // Detect balls in the converted Bitmap
            val balls = ballDetector.detectBalls(bitmap)
            // Log.d(TAG, "Detected ${balls.size} balls in frame: ${balls.joinToString()}")

            // Pass detected balls and the original camera frame dimensions to MainOverlayView on the UI thread
            activity.runOnUiThread {
                mainOverlayView.updateTrackedBalls(balls, imageProxy.width, imageProxy.height)
            }
            imageProxy.close() // Important: close the ImageProxy to release the buffer
        }
    }

    /**
     * Shuts down the camera executor and unbinds camera use cases.
     */
    fun shutdown() {
        analysisExecutor.shutdown()
        // It's good practice to unbind all use cases on shutdown
        cameraProviderFuture.get()?.unbindAll()
        Log.i(TAG, "CameraManager shutdown complete.")
    }
}