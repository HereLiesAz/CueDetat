package com.hereliesaz.cuedetat.ar.jetpack

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.hereliesaz.cuedetat.MainActivity
import com.hereliesaz.cuedetat.ar.jetpack.helpers.DisplayRotationHelper
import com.hereliesaz.cuedetat.ar.jetpack.rendering.BackgroundRenderer
import com.hereliesaz.cuedetat.ar.jetpack.rendering.ObjectRenderer
import com.hereliesaz.cuedetat.ui.state.UiState
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ArRenderer(
    val activity: MainActivity,
    private val session: Session,
    private val displayRotationHelper: DisplayRotationHelper,
    private val uiState: UiState // Receive the UI State
) : GLSurfaceView.Renderer {

    private val backgroundRenderer = BackgroundRenderer()

    // Create renderers for our objects
    private val tableRenderer = ObjectRenderer()
    // private val cueBallRenderer = ObjectRenderer()
    // private val objectBallRenderer = ObjectRenderer()


    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        backgroundRenderer.createOnGlThread()
        tableRenderer.createOnGlThread()
        // cueBallRenderer.createOnGlThread()
        // objectBallRenderer.createOnGlThread()
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        displayRotationHelper.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        displayRotationHelper.updateSessionIfNeeded(session)

        val frame: Frame = try {
            session.update()
        } catch (e: Exception) {
            return
        }

        backgroundRenderer.draw(frame)
        val camera = frame.camera
        if (camera.trackingState != com.google.ar.core.TrackingState.TRACKING) {
            return
        }

        val projmtx = FloatArray(16)
        camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f)
        val viewmtx = FloatArray(16)
        camera.getViewMatrix(viewmtx, 0)

        // Draw the table if the anchor exists in the state
        uiState.table?.let { tableAnchor ->
            tableRenderer.draw(
                anchor = tableAnchor,
                viewMatrix = viewmtx,
                projectionMatrix = projmtx,
                color = floatArrayOf(0.0f, 0.5f, 0.0f, 1.0f), // Green color
                scaleFactor = 0.5f // Example scale
            )
        }

        // TODO: Draw cue ball and object ball anchors here
    }
}