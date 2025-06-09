package com.hereliesaz.cuedetat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.common.util.concurrent.ListenableFuture
import com.hereliesaz.cuedetat.ui.MainScreen
import com.hereliesaz.cuedetat.ui.MainViewModel
import com.hereliesaz.cuedetat.ui.MenuAction
import com.hereliesaz.cuedetat.ui.ToastMessage
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme
import com.hereliesaz.cuedetat.view.ProtractorOverlayView
import com.hereliesaz.cuedetat.view.state.OverlayState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.guava.await
import androidx.camera.core.Preview as CameraPreview

@ExperimentalMaterial3ExpressiveApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                setContent { AppContent() }
            } else {
                Log.e("CueD8at", "Camera permission denied.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (hasCameraPermission()) {
            setContent { AppContent() }
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    @Composable
    private fun AppContent() {
        val uiState by viewModel.uiState.collectAsState()
        val dynamicTheme by viewModel.dynamicColorScheme.collectAsState()
        val context = LocalContext.current
        val currentTheme = MaterialTheme.colorScheme

        val view = LocalView.current
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, view).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // Observe toast messages from ViewModel
        LaunchedEffect(Unit) {
            viewModel.toastMessage.onEach { message ->
                message?.let {
                    val text = when (it) {
                        is ToastMessage.StringResource -> context.getString(
                            it.id,
                            *it.formatArgs.toTypedArray()
                        )

                        is ToastMessage.PlainText -> it.text
                    }
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                    viewModel.onToastShown()
                }
            }.launchIn(this)
        }

        val protractorView = remember { ProtractorOverlayView(context) }

        val scaleGestureDetector = remember {
            ScaleGestureDetector(
                context,
                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        val newZoom = uiState.zoomFactor * detector.scaleFactor
                        viewModel.onZoomChange(newZoom.coerceIn(0.1f, 4.0f))
                        return true
                    }
                })
        }
        var lastTouchX = 0f
        val touchHandler = { event: MotionEvent ->
            scaleGestureDetector.onTouchEvent(event)
            if (!scaleGestureDetector.isInProgress) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> lastTouchX = event.x
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.x - lastTouchX
                        viewModel.onRotationChange(uiState.rotationAngle + (dx * 0.3f))
                        lastTouchX = event.x
                    }
                }
            }
            true
        }

        val previewView = remember {
            PreviewView(context).apply {
                setOnTouchListener { _, event ->
                    touchHandler(event)
                }
            }
        }
        LaunchedEffect(Unit) {
            cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val cameraProvider = cameraProviderFuture.await()
            val preview = CameraPreview.Builder().build()
                .also { it.surfaceProvider = previewView.surfaceProvider }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this@MainActivity, cameraSelector, preview)
            } catch (e: Exception) {
                Log.e("CueD8at", "Camera binding failed.", e)
            }
        }

        LaunchedEffect(uiState) { protractorView.updateState(uiState) }
        LaunchedEffect(currentTheme) { protractorView.applyColorScheme(currentTheme) }
        LaunchedEffect(previewView) {
            if (previewView.width > 0 && previewView.height > 0) {
                viewModel.onSizeChanged(previewView.width, previewView.height)
            }
        }


        CueDetatTheme(dynamicColorScheme = dynamicTheme) {
            MainScreen(
                uiState = uiState,
                protractorView = protractorView,
                onZoomChange = viewModel::onZoomChange,
                onMenuAction = { action ->
                    when (action) {
                        is MenuAction.ToggleHelp -> viewModel.onToggleHelp()
                        is MenuAction.CheckForUpdate -> viewModel.onCheckForUpdate()
                        is MenuAction.ViewArt -> {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.instagram.com/hereliesaz")
                            )
                            context.startActivity(intent)
                        }
                        is MenuAction.AdaptTheme -> {
                            viewModel.adaptThemeFromBitmap(previewView.bitmap)
                        }

                        is MenuAction.ResetTheme -> {
                            viewModel.resetTheme()
                        }
                    }
                }
            ) {
                // MODIFIED: This is where we pass the REAL camera view to the MainScreen
                AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
            }
        }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
}

@ExperimentalMaterial3ExpressiveApi
@Preview
@Composable
fun AppContentPreview() {
    val context = LocalContext.current
    val currentTheme = MaterialTheme.colorScheme

    val uiState = OverlayState(
        viewWidth = 1080,
        viewHeight = 1920,
        zoomFactor = 1.0f,
        areHelpersVisible = true
    )

    val protractorView = ProtractorOverlayView(context)
    protractorView.updateState(uiState)
    protractorView.applyColorScheme(currentTheme)

    CueDetatTheme {
        MainScreen(
            uiState = uiState,
            protractorView = protractorView,
            onZoomChange = {},
            onMenuAction = {}
        ) {
            // MODIFIED: For the preview, we pass a simple black box instead of the real camera view.
            // This prevents the crash.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }
    }
}