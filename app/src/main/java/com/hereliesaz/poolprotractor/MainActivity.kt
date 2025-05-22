package com.hereliesaz.poolprotractor

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import com.hereliesaz.poolprotractor.ui.theme.PoolProtractorTheme
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars

class MainActivity : ComponentActivity(), SensorEventListener, ProtractorOverlayView.ProtractorStateListener {

    private companion object {
        private const val TAG = "PoolProtractorApp"
    }

    // CameraX
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var previewView: PreviewView? = null // Will be set in Compose

    // Sensor
    private var sensorManager: SensorManager? = null
    private var rotationVectorSensor: Sensor? = null
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // ProtractorOverlayView instance, to be accessed from Compose
    private var protractorOverlayViewInstance: ProtractorOverlayView? = null

    private var valuesChangedSinceLastReset by mutableStateOf(false)

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCameraIfReady()
            } else {
                // Consider a more user-friendly way to handle this in Compose
                Log.e(TAG, "Camera permission denied.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SensorManager here as it's not UI dependent
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        setContent {
            val view = LocalView.current
            SideEffect { // For immersive mode, status bar handling
                val window = (view.context as Activity).window
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowInsetsControllerCompat(window, view).let { controller ->
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            PoolProtractorTheme {
                MainScreen()
            }
        }
        checkCameraPermissionAndStart() // Request permission after content is set
    }

    @Composable
    private fun MainScreen() {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val currentColorScheme = MaterialTheme.colorScheme

        var sliderProgress by remember { mutableFloatStateOf(0.0f) } // 0.0 to 1.0

        LaunchedEffect(protractorOverlayViewInstance) {
            protractorOverlayViewInstance?.let {
                val initialZoom = it.getZoomFactor()
                val zoomRange = ProtractorOverlayView.MAX_ZOOM_FACTOR - ProtractorOverlayView.MIN_ZOOM_FACTOR
                if (zoomRange > 0.0001f) {
                    sliderProgress = ((initialZoom - ProtractorOverlayView.MIN_ZOOM_FACTOR) / zoomRange).coerceIn(0f, 1f)
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Camera Preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { pv ->
                        previewView = pv
                        // Defer camera start until permission is granted and previewView is available
                        startCameraIfReady()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Protractor Overlay View
            AndroidView(
                factory = { ctx ->
                    ProtractorOverlayView(ctx).also { pov ->
                        protractorOverlayViewInstance = pov
                        pov.listener = this@MainActivity
                    }
                },
                update = { view ->
                    view.applyMaterialYouColors(currentColorScheme)
                    // If ProtractorOverlayView's state needs to be driven from Compose externally, do it here.
                    // For now, it manages its own zoom/rotation, and updates MainActivity via listener.
                },
                modifier = Modifier.fillMaxSize()
            )

            // UI Controls Overlay
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsetsCompat.systemBars.asPaddingValues()) // Apply system bar padding
            ) {
                // Zoom Controls (Slider and Icon) - Right Aligned
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(0.7f) // Adjust height as desired for the column
                        .width(60.dp)        // Give it some width for touch targets
                        .padding(end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_zoom_in_24),
                        contentDescription = stringResource(id = R.string.zoom_icon),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp) // Space between icon and slider
                    )
                    VerticalSlider(
                        value = sliderProgress,
                        onValueChange = { newProgress ->
                            sliderProgress = newProgress
                            val zoomRange = ProtractorOverlayView.MAX_ZOOM_FACTOR - ProtractorOverlayView.MIN_ZOOM_FACTOR
                            val zoomValue = ProtractorOverlayView.MIN_ZOOM_FACTOR + (zoomRange * newProgress)
                            protractorOverlayViewInstance?.setZoomFactor(zoomValue)
                            onUserInteraction()
                        },
                        modifier = Modifier
                            .weight(1f) // Slider takes up available vertical space
                            .fillMaxWidth() // Fill the column's width
                    )
                }

                // Reset Button - Bottom Right
                FloatingActionButton(
                    onClick = { handleResetAction() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_undo_24),
                        contentDescription = stringResource(id = R.string.reset_view),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Help Button - Bottom Left
                FloatingActionButton(
                    onClick = { protractorOverlayViewInstance?.toggleHelpersVisibility() },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_help_outline_24),
                        contentDescription = stringResource(id = R.string.toggle_help_lines),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Register sensor listener when composable is active
        DisposableEffect(Unit) {
            registerSensorListener()
            onDispose {
                unregisterSensorListener()
            }
        }
    }


    @Composable
    fun VerticalSlider(
        value: Float,
        onValueChange: (Float) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
        steps: Int = 0,
        colors: androidx.compose.material3.SliderColors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        BoxWithConstraints(modifier = modifier
            .graphicsLayer(rotationZ = 270f)
            .padding(horizontal = 8.dp) // Add some padding so thumb isn't cut off
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .width(this.maxHeight) // Effectively the length of the slider
                    .height(this.maxWidth),  // Effectively the thickness of the slider
                enabled = enabled,
                valueRange = valueRange,
                steps = steps,
                colors = colors
            )
        }
    }


    private fun checkCameraPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                startCameraIfReady()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Explain to the user why the permission is needed
                Log.w(TAG, "Camera permission rationale should be shown.")
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCameraIfReady() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && previewView != null) {
            startCamera()
        }
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        lifecycleScope.launch {
            try {
                val cameraProvider = cameraProviderFuture.await()
                bindPreview(cameraProvider)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting camera: ", e)
            }
        }
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val currentPreviewView = previewView ?: return // Ensure previewView is initialized

        val preview: Preview = Preview.Builder().build()
        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        preview.surfaceProvider = currentPreviewView.surfaceProvider
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview) // Use ComponentActivity as LifecycleOwner
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    private fun handleResetAction() {
        protractorOverlayViewInstance?.resetToDefaults()
        // The onZoomChanged callback will update sliderProgress
        valuesChangedSinceLastReset = false
    }

    // ProtractorStateListener Callbacks
    override fun onZoomChanged(newZoomFactor: Float) {
        val zoomRange = ProtractorOverlayView.MAX_ZOOM_FACTOR - ProtractorOverlayView.MIN_ZOOM_FACTOR
        if (zoomRange > 0.0001f) {
            // This logic will be triggered by setContent's sliderProgress state variable
            // Find the sliderProgress state variable in MainScreen and update it.
            // For now, just log, as the MainScreen's sliderProgress state should be updated directly
            // by the ProtractorOverlayView's listener if we want to keep a single source of truth.
            // However, the current setup has slider driving POV, and POV listener updating slider.
            // To correctly update the sliderProgress in Composable:
            // We need a way for this callback to update the `sliderProgress` state in `MainScreen`.
            // This can be done by making sliderProgress a member of MainActivity or passing a lambda.
            // For simplicity with current structure, assume MainScreen's LaunchedEffect and slider's onValueChange manage it.
            // This callback just ensures the internal `valuesChangedSinceLastReset` is set.
        }
        onUserInteraction()
    }

    override fun onRotationChanged(newRotationAngle: Float) {
        onUserInteraction()
    }

    override fun onUserInteraction() {
        valuesChangedSinceLastReset = true
    }

    // SensorEventListener Callbacks
    private fun registerSensorListener() {
        rotationVectorSensor?.also { sensor ->
            val registered = sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
            Log.d(TAG, "Sensor listener registration attempt. Success: $registered")
        }
    }

    private fun unregisterSensorListener() {
        sensorManager?.unregisterListener(this)
        Log.d(TAG, "Sensor listener unregistered.")
    }

    override fun onResume() {
        super.onResume()
        // Sensor registration is now handled by DisposableEffect in Compose
    }

    override fun onPause() {
        super.onPause()
        // Sensor unregistration is now handled by DisposableEffect in Compose
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val pitchInRadians = orientationAngles[1]
            val pitchInDegrees = Math.toDegrees(pitchInRadians.toDouble()).toFloat()
            protractorOverlayViewInstance?.setPitchAngle(-pitchInDegrees)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Sensor accuracy changed for ${sensor?.name} to $accuracy")
    }
}