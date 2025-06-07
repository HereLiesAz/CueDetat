package com.hereliesaz.cuedetat

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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import com.hereliesaz.cuedetat.ui.theme.PoolProtractorTheme
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.hereliesaz.poolprotractor.ProtractorOverlayView

class MainActivity : ComponentActivity(), SensorEventListener, ProtractorOverlayView.ProtractorStateListener {

    private companion object {
        private const val TAG = "PoolProtractorApp"
    }

    // CameraX
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var previewView: PreviewView? = null

    // Sensor
    private var sensorManager: SensorManager? = null
    private var rotationVectorSensor: Sensor? = null
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // ProtractorOverlayView instance
    private var protractorOverlayViewInstance: ProtractorOverlayView? = null
    private var valuesChangedSinceLastReset by mutableStateOf(false)

    // State for the insulting warning message
    private val warningMessage = mutableStateOf("")
    private val warningVisible = mutableStateOf(false)


    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCameraIfReady()
            } else {
                Log.e(TAG, "Camera permission denied.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        setContent {
            PoolProtractorTheme {
                val view = LocalView.current
                SideEffect {
                    val window = (view.context as Activity).window
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    WindowInsetsControllerCompat(window, view).let { controller ->
                        controller.hide(WindowInsetsCompat.Type.statusBars())
                        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                MainScreen(warningMessage.value, warningVisible.value)
            }
        }
        checkCameraPermissionAndStart()
    }

    @Composable
    private fun MainScreen(currentWarning: String, isWarningVisible: Boolean) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val currentColorScheme = MaterialTheme.colorScheme
        var sliderProgress by remember { mutableFloatStateOf(0.0f) }

        LaunchedEffect(protractorOverlayViewInstance) {
            protractorOverlayViewInstance?.let {
                val initialZoom = it.getZoomFactor()
                val zoomRange = ProtractorOverlayView.Companion.MAX_ZOOM_FACTOR - ProtractorOverlayView.Companion.MIN_ZOOM_FACTOR
                sliderProgress = ((initialZoom - ProtractorOverlayView.Companion.MIN_ZOOM_FACTOR) / zoomRange).coerceIn(0f, 1f)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(factory = { ctx ->
                PreviewView(ctx).also { pv ->
                    previewView = pv
                    startCameraIfReady()
                }
            }, modifier = Modifier.fillMaxSize())

            AndroidView(factory = { ctx ->
                ProtractorOverlayView(ctx).also { pov ->
                    protractorOverlayViewInstance = pov
                    pov.listener = this@MainActivity
                }
            }, update = { view ->
                view.applyMaterialYouColors(currentColorScheme)
            }, modifier = Modifier.fillMaxSize())

            // UI Controls and Warnings Overlay
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
            ) {
                // Insulting Warning Text - Top Center
                val warningAreaDesc = stringResource(id = R.string.warning_message_area)
                AnimatedVisibility(
                    visible = isWarningVisible,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp),
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    Text(
                        text = currentWarning,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .background(
                                MaterialTheme.colorScheme.onError.copy(alpha = 0.7f),
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .semantics { contentDescription = warningAreaDesc }
                    )
                }


                // Zoom Controls - Right
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(0.7f)
                        .width(60.dp)
                        .padding(end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_zoom_in_24),
                        contentDescription = stringResource(id = R.string.zoom_icon),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    VerticalSlider(
                        value = sliderProgress,
                        onValueChange = { newProgress ->
                            sliderProgress = newProgress
                            val zoomRange = ProtractorOverlayView.Companion.MAX_ZOOM_FACTOR - ProtractorOverlayView.Companion.MIN_ZOOM_FACTOR
                            val zoomValue = ProtractorOverlayView.Companion.MIN_ZOOM_FACTOR + (zoomRange * newProgress)
                            protractorOverlayViewInstance?.setZoomFactor(zoomValue)
                            onUserInteraction()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }

                // FABs - Bottom
                FloatingActionButton(
                    onClick = { handleResetAction() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp),
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_undo_24),
                        contentDescription = stringResource(id = R.string.reset_view),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                FloatingActionButton(
                    onClick = { protractorOverlayViewInstance?.toggleHelpersVisibility() },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp),
                    shape = MaterialTheme.shapes.large,
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

        DisposableEffect(Unit) {
            registerSensorListener()
            onDispose { unregisterSensorListener() }
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
        colors: SliderColors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        BoxWithConstraints(modifier = modifier
            .graphicsLayer(rotationZ = 270f)
            .padding(horizontal = 8.dp)
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .width(this.maxHeight)
                    .height(this.maxWidth),
                enabled = enabled,
                valueRange = valueRange,
                steps = steps,
                colors = colors
            )
        }
    }

    private fun checkCameraPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> startCameraIfReady()
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Log.w(TAG, "Camera permission rationale should be shown.")
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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
        val currentPreviewView = previewView ?: return
        val preview: Preview = Preview.Builder().build()
        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        preview.surfaceProvider = currentPreviewView.surfaceProvider
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview)
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    private fun handleResetAction() {
        protractorOverlayViewInstance?.resetToDefaults()
        valuesChangedSinceLastReset = false
    }

    // ProtractorStateListener Callbacks
    override fun onZoomChanged(newZoomFactor: Float) {
        onUserInteraction()
    }

    override fun onRotationChanged(newRotationAngle: Float) {
        onUserInteraction()
    }

    override fun onUserInteraction() {
        valuesChangedSinceLastReset = true
    }

    override fun onWarningStateChanged(isWarning: Boolean) {
        if (isWarning) {
            val warnings = resources.getStringArray(R.array.insulting_warnings)
            warningMessage.value = warnings[Random.nextInt(warnings.size)]
            warningVisible.value = true
        } else {
            warningVisible.value = false
        }
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
    }

    override fun onPause() {
        super.onPause()
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
