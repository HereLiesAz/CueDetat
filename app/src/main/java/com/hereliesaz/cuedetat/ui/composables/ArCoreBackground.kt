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
import com.hereliesaz.cuedetat.data.ArFrameProcessor
import com.hereliesaz.cuedetat.data.ArTableSession
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private const val TAG = "ArCoreBackground"

/**
 * Full-screen camera background powered by ARCore, used for the whole expert-mode AR flow (corner
 * capture during AR_SETUP and live tracking during AR_ACTIVE).
 *
 * Each frame the GL renderer:
 * 1. Draws the camera feed as a fullscreen quad.
 * 2. Feeds the CPU image to [ArFrameProcessor] for ball detection and felt-colour sampling.
 * 3. Runs [ArTableSession.computeFrameUpdate]: performs any queued corner capture (hit-test at the
 *    screen centre), re-projects the captured anchors, and fits the logical->screen homography,
 *    then emits [MainScreenEvent.ArTableMatrixUpdated] (and [MainScreenEvent.ArCornerCaptured]).
 * 4. Emits a geometry-derived viewing pitch via [MainScreenEvent.ArCameraPoseUpdated] (used as the
 *    perspective hint before four corners are captured).
 *
 * Lifecycle: the [DisposableEffect] drives session resume/pause/close so the ARCore camera is
 * released before CameraX can re-acquire it on a mode switch.
 */
@Composable
fun ArCoreBackground(
    modifier: Modifier = Modifier,
    arTableSession: ArTableSession,
    arFrameProcessor: ArFrameProcessor,
    onEvent: (MainScreenEvent) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val glSurfaceView = remember { GLSurfaceView(context) }

    DisposableEffect(lifecycleOwner) {
        val session = arTableSession.createSession() ?: run {
            Log.w(TAG, "ARCore session unavailable on this device")
            return@DisposableEffect onDispose {}
        }

        glSurfaceView.apply {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        }

        val renderer = ArCoreRenderer(
            session = session,
            arTableSession = arTableSession,
            arFrameProcessor = arFrameProcessor,
            onEvent = onEvent,
        )
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                arTableSession.resume()
                glSurfaceView.onResume()
            }
            override fun onPause(owner: LifecycleOwner) {
                glSurfaceView.onPause()
                arTableSession.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        arTableSession.resume()
        glSurfaceView.onResume()

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glSurfaceView.onPause()
            arTableSession.close()
            Log.d(TAG, "ARCore session closed — camera released")
        }
    }

    AndroidView(factory = { glSurfaceView }, modifier = modifier)
}

// ---------------------------------------------------------------------------

private class ArCoreRenderer(
    private val session: Session,
    private val arTableSession: ArTableSession,
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
        // 0 = natural display rotation; ARCore queries the display internally.
        session.setDisplayGeometry(0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        try {
            val frame: Frame = session.update()
            val camera = frame.camera
            val currentTracking = camera.trackingState

            if (previousTrackingState == TrackingState.TRACKING &&
                currentTracking == TrackingState.PAUSED) {
                // The logical plane shouldn't shatter just because reality blinked.
                // Let ARCore recover; the anchors are retained.
                Log.w(TAG, "Tracking paused. Holding anchors.")
            }
            previousTrackingState = currentTracking

            if (currentTracking != TrackingState.STOPPED) {
                background.draw(frame)
            }

            // Ball detection + felt-colour sampling from the ARCore CPU image.
            arFrameProcessor.processFrame(frame)

            if (currentTracking == TrackingState.TRACKING) {
                // World-anchored table: capture, re-project, fit the homography.
                val update = arTableSession.computeFrameUpdate(frame, surfaceWidth, surfaceHeight)
                onEvent(
                    MainScreenEvent.ArTableMatrixUpdated(
                        matrix = update.matrix,
                        capturedCorners = update.capturedCorners
                    )
                )
                update.capture?.let { c ->
                    onEvent(MainScreenEvent.ArCornerCaptured(hit = c.hit, count = c.count))
                }

                // Pitch hint from plane geometry (used before four corners exist).
                arTableSession.findAndAnchorTablePlane(frame)
                arTableSession.computeCameraAbovePlane(frame)?.let { abovePlane ->
                    onEvent(
                        MainScreenEvent.ArCameraPoseUpdated(
                            pitchDegrees = abovePlane.pitchDegrees,
                            heightAboveSurfaceM = abovePlane.heightM
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "ARCore frame error", e)
        }
    }
}
