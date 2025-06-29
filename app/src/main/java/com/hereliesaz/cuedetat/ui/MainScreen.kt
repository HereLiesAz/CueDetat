package com.hereliesaz.cuedetat.ui

import android.opengl.GLSurfaceView
import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.hereliesaz.cuedetat.ar.ARCoreRenderer
import com.hereliesaz.cuedetat.ui.composables.MenuDrawer
import com.hereliesaz.cuedetat.ui.state.UiEvent
import com.hereliesaz.cuedetat.ui.state.UiState

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    MenuDrawer(
        uiState = uiState,
        onEvent = viewModel::onEvent
    ) {
        MainScreenContent(
            uiState = uiState,
            onEvent = viewModel::onEvent
        )
    }
}

@Composable
fun MainScreenContent(
    uiState: UiState,
    onEvent: (UiEvent) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val renderer = remember {
        ARCoreRenderer(context) { hitResult ->
            if (hitResult.trackable is Plane) {
                onEvent(UiEvent.OnPlaneTapped(hitResult.pose))
            }
        }
    }

    val glSurfaceView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        val session = Session(context)
                        renderer.arSession = session
                        session.resume()
                        glSurfaceView.onResume()
                        onEvent(UiEvent.SetARSession(session))
                    } catch (e: Exception) {
                        Log.e("MainScreen", "Failed to create or resume AR session", e)
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    glSurfaceView.onPause()
                    renderer.arSession?.pause()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    renderer.arSession?.close()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { glSurfaceView },
            update = { view ->
                // Pass state down to the renderer
                val arRenderer = (view.renderer as? ARCoreRenderer) ?: return@AndroidView
                arRenderer.tablePose = uiState.tablePose
                arRenderer.cueBallState = uiState.cueBallPose
                arRenderer.objectBallState = uiState.objectBallPose
                arRenderer.selectedBallId = uiState.selectedBall
                arRenderer.shotType = uiState.shotType
                arRenderer.spinOffset = uiState.spinOffset
                arRenderer.cueElevation = uiState.cueElevation

                // Check for plane detection
                arRenderer.onPlaneDetected = {
                    onEvent(UiEvent.OnPlanesDetected)
                }
            }
        )
        Text(text = uiState.statusText, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
