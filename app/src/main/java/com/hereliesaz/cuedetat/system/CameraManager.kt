package com.hereliesaz.cuedetat.system

import android.Manifest
import android.content.pm.PackageManager
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
import com.hereliesaz.cuedetat.system.*
import com.hereliesaz.cuedetat.tracking.ball_detector.BallDetector
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
    private lateinit var ballDetector: BallDetector

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
        ballDetector = BallDetector() // Initialize ML Kit BallDetector
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
        preview.surfaceProvider = previewView.surfaceProvider

        // Set up ImageAnalysis for processing camera frames
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(IMAGE_ANALYSIS_WIDTH, IMAGE_ANALYSIS_HEIGHT))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Only analyze the most recent frame
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
        } catch (exception: Exception) {
            Log.e(TAG, "Use case binding failed for camera: ", exception)
            Toast.makeText(activity, "Camera binding failed: ${exception.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Custom ImageAnalysis Analyzer for ball detection using ML Kit
    private inner class BallDetectionAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(imageProxy: ImageProxy) {
            ballDetector.detectBalls(
                imageProxy,
                onDetectionSuccess = { balls ->
                    // Pass detected balls and the original camera frame dimensions to MainOverlayView on the UI thread
                    activity.runOnUiThread {
                        mainOverlayView.updateTrackedBalls(balls, imageProxy.width, imageProxy.height)
                    }
                },
                onDetectionFailure = { e ->
                    Log.e(TAG, "Ball detection failed: $e")
                    // You might want to clear tracked balls or show a message on UI
                    activity.runOnUiThread {
                        mainOverlayView.updateTrackedBalls(emptyList(), imageProxy.width, imageProxy.height)
                    }
                }
            )
            // ImageProxy is closed within BallDetector.detectBalls on completion listener.
        }
    }

    /**
     * Shuts down the camera executor and unbinds camera use cases.
     */
    fun shutdown() {
        analysisExecutor.shutdown()
        ballDetector.shutdown() // Shutdown ML Kit detector
        // It's good practice to unbind all use cases on shutdown
        cameraProviderFuture.get()?.unbindAll()
        Log.i(TAG, "CameraManager shutdown complete.")
    }
}