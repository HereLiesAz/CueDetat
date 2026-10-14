package com.hereliesaz.cuedetat.feature.expert.ar

import android.hardware.display.DisplayManager
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Surface
import java.util.concurrent.atomic.AtomicInteger
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
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private const val TAG = "ArCoreBackground"

/**
 * Full-screen camera background powered by ARCore, used for the whole expert-mode AR flow.
 *
 * Each frame the GL renderer:
 * 1. Draws the camera feed as a fullscreen quad.
 * 2. Feeds the CPU image to [ArFrameProcessor] for ball detection and felt-colour sampling.
 * 3. Runs [ArTableSession.computeFrameUpdate]: anchors the table if Lock was tapped, fits the
 *    logical->screen homography from the metric preview or locked corners, then emits
 *    [MainScreenEvent.ArTableMatrixUpdated] (null until tracking) and, on a lock attempt,
 *    [MainScreenEvent.ArTableLockResult].
 * 4. Emits a geometry-derived viewing pitch via [MainScreenEvent.ArCameraPoseUpdated] (used as the
 *    perspective hint before the table is locked).
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

        // ARCore's camera projection and the OES camera background MUST
        // share the actual display orientation. Zero is ROTATION_0, not a
        // universal "natural" rotation: it is wrong on landscape displays.
        val displayManager = context.getSystemService(DisplayManager::class.java)
        val displayId = glSurfaceView.display?.displayId ?: Display.DEFAULT_DISPLAY
        val displayRotation = AtomicInteger(
            displayManager.getDisplay(displayId)?.rotation ?: Surface.ROTATION_0,
        )
        val displayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
            override fun onDisplayChanged(changedDisplayId: Int) {
                if (changedDisplayId == displayId) {
                    displayRotation.set(
                        displayManager.getDisplay(displayId)?.rotation ?: Surface.ROTATION_0,
                    )
                }
            }
        }
        displayManager.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))

        val renderer = ArCoreRenderer(
            displayRotation = displayRotation,
            session = session,
            arTableSession = arTableSession,
            arFrameProcessor = arFrameProcessor,
            onEvent = onEvent,
        )
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                displayRotation.set(displayManager.getDisplay(displayId)?.rotation ?: Surface.ROTATION_0)
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
            displayManager.unregisterDisplayListener(displayListener)
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
    private val displayRotation: AtomicInteger,
    private val session: Session,
    private val arTableSession: ArTableSession,
    private val arFrameProcessor: ArFrameProcessor,
    private val onEvent: (MainScreenEvent) -> Unit,
) : GLSurfaceView.Renderer {

    private val background = ArBackgroundRenderer()
    private var surfaceWidth = 1
    private var surfaceHeight = 1
    private var previousTrackingState: TrackingState? = null
    private var lastGeometryRotation = -1

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        background.createOnGlThread()
        session.setCameraTextureName(background.getTextureId())
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        val rotation = displayRotation.get()
        session.setDisplayGeometry(rotation, width, height)
        lastGeometryRotation = rotation
    }

    override fun onDrawFrame(gl: GL10?) {
        try {
            val rotation = displayRotation.get()
            if (rotation != lastGeometryRotation) {
                // Display rotation can change without another GL surface resize.
                // Update before session.update so AR projection and camera UVs
                // both use the SAME geometry for this frame.
                session.setDisplayGeometry(rotation, surfaceWidth, surfaceHeight)
                lastGeometryRotation = rotation
            }
            val frame: Frame = session.update()
            val camera = frame.camera
            val currentTracking = camera.trackingState

            if (previousTrackingState == TrackingState.TRACKING &&
                currentTracking != TrackingState.TRACKING) {
                // ARCore's physical anchor remains in memory, but its last
                // screen projection is no longer trustworthy while the camera
                // continues moving. Clear that projection until tracking
                // resumes instead of painting a stale spinning overlay.
                Log.w(TAG, "AR tracking paused. Keeping world anchor, hiding stale projection.")
                onEvent(MainScreenEvent.ArTableMatrixUpdated(matrix = null))
            }
            previousTrackingState = currentTracking

            if (currentTracking != TrackingState.STOPPED) {
                background.draw(frame)
            }

            // Ball detection + felt-colour sampling from the ARCore CPU image.
            arFrameProcessor.processFrame(frame)

            if (currentTracking == TrackingState.TRACKING) {
                // World-anchored table: serve any Lock tap, then fit the homography from the anchors.
                val update = arTableSession.computeFrameUpdate(frame, surfaceWidth, surfaceHeight)
                onEvent(MainScreenEvent.ArTableMatrixUpdated(matrix = update.matrix))
                update.lockResult?.let { onEvent(MainScreenEvent.ArTableLockResult(it)) }
                update.cloudFeedback.forEach { feedback ->
                    onEvent(MainScreenEvent.CloudPlacementResult(feedback.message, feedback.restored))
                }

                // Pitch hint from plane geometry (used before the table is locked).
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
