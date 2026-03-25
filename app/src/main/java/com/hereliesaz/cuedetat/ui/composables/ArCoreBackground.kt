package com.hereliesaz.cuedetat.ui.composables

import android.opengl.GLSurfaceView
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.hereliesaz.cuedetat.data.ArBackgroundRenderer
import com.hereliesaz.cuedetat.data.ArDepthSession
import com.hereliesaz.cuedetat.data.ArFrameProcessor
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private const val TAG = "ArCoreBackground"

/**
 * Full-screen camera background powered by ARCore.
 *
 * Replaces [CameraBackground] when [CameraMode.AR_ACTIVE] is active. Uses a [GLSurfaceView] to:
 * 1. Allocate an OES texture and hand its ID to the ARCore session.
 * 2. Render the camera feed as a fullscreen quad each frame.
 * 3. Feed the CPU camera image to [ArFrameProcessor] for OpenCV ball detection.
 * 4. Extract a depth plane and emit [MainScreenEvent.DepthPlaneUpdated].
 *
 * Lifecycle: the composable's [DisposableEffect] drives session resume/pause/close, ensuring
 * the ARCore camera session is released before CameraX can re-acquire it on mode switch.
 */
@Composable
fun ArCoreBackground(
    modifier: Modifier = Modifier,
    arDepthSession: ArDepthSession,
    arFrameProcessor: ArFrameProcessor,
    onEvent: (MainScreenEvent) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val glSurfaceView = remember { GLSurfaceView(context) }

    DisposableEffect(lifecycleOwner) {
        val session = arDepthSession.createSession() ?: run {
            Log.w(TAG, "ARCore session unavailable — depth not supported on this device")
            return@DisposableEffect onDispose {}
        }

        glSurfaceView.apply {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        }

        val renderer = ArCoreRenderer(
            session = session,
            arDepthSession = arDepthSession,
            arFrameProcessor = arFrameProcessor,
            onEvent = onEvent,
        )
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                arDepthSession.resume()
                glSurfaceView.onResume()
            }
            override fun onPause(owner: LifecycleOwner) {
                glSurfaceView.onPause()
                arDepthSession.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        // Trigger initial resume if already in resumed state
        arDepthSession.resume()
        glSurfaceView.onResume()

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glSurfaceView.onPause()
            arDepthSession.close()
            Log.d(TAG, "ARCore session closed — camera released")
        }
    }

    AndroidView(factory = { glSurfaceView }, modifier = modifier)
}

// ---------------------------------------------------------------------------

private class ArCoreRenderer(
    private val session: Session,
    private val arDepthSession: ArDepthSession,
    private val arFrameProcessor: ArFrameProcessor,
    private val onEvent: (MainScreenEvent) -> Unit,
) : GLSurfaceView.Renderer {

    private val background = ArBackgroundRenderer()
    private var surfaceWidth = 1
    private var surfaceHeight = 1
    private var previousTrackingState: TrackingState? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        background.createOnGlThread()
        session.setCameraTextureName(background.getTextureId())
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        // 0 = natural display rotation; ARCore will query the display internally
        session.setDisplayGeometry(0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        try {
            val frame: Frame = session.update()
            val camera = frame.camera
            val currentTracking = camera.trackingState

            if (previousTrackingState == TrackingState.TRACKING &&
                currentTracking == TrackingState.PAUSED) {
                onEvent(MainScreenEvent.ArTrackingLost)
            }
            previousTrackingState = currentTracking

            if (currentTracking != TrackingState.STOPPED) {
                background.draw(frame)
            }

            // Feed camera image to OpenCV ball detection pipeline
            arFrameProcessor.processFrame(frame)

            // Extract depth plane and emit to state machine
            val plane = arDepthSession.processFrame(frame)
            if (plane != null) {
                onEvent(MainScreenEvent.DepthPlaneUpdated(plane))
            }
        } catch (e: Exception) {
            Log.e(TAG, "ARCore frame error", e)
        }
    }
}
