package com.hereliesaz.cuedetat.ar.jetpack

import android.opengl.GLSurfaceView
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ar.core.Session
import com.hereliesaz.cuedetat.MainActivity
import com.hereliesaz.cuedetat.ar.jetpack.helpers.DisplayRotationHelper
import com.hereliesaz.cuedetat.ui.state.UiState

@Composable
fun ArView(
    modifier: Modifier = Modifier,
    session: Session,
    uiState: UiState,
    onTap: (MotionEvent) -> Unit
) {
    val activity = LocalContext.current as MainActivity
    val glSurfaceView = GLSurfaceView(activity)

    // Pass the UiState to the renderer.
    val renderer = ArRenderer(
        activity = activity,
        session = session,
        displayRotationHelper = DisplayRotationHelper(activity),
        uiState = uiState // Add this
    )

    glSurfaceView.setEGLContextClientVersion(2)
    glSurfaceView.preserveEGLContextOnPause = true
    glSurfaceView.setRenderer(renderer)
    glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    glSurfaceView.setOnTouchListener { _, event ->
        onTap(event)
        true
    }

    AndroidView({ glSurfaceView }, modifier = modifier)
}