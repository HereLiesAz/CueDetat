package com.hereliesaz.cuedetat.ar.rendering

import android.opengl.GLES20
import android.util.Log
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.hereliesaz.cuedetat.ui.state.UiState
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.opengl.GLSurfaceView
import com.hereliesaz.cuedetat.ar.jetpack.helpers.DisplayRotationHelper
import com.hereliesaz.cuedetat.ar.jetpack.rendering.BackgroundRenderer

class OpenXrRenderer(
    private val session: Session,
    private val displayRotationHelper: DisplayRotationHelper,
    private var uiState: UiState
) : GLSurfaceView.Renderer {

    private val backgroundRenderer = BackgroundRenderer()
    // We will add object renderers here later.

    // A method to update the state from the Composable
    fun updateState(newState: UiState) {
        this.uiState = newState
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        backgroundRenderer.createOnGlThread()
        session.setCameraTextureName(backgroundRenderer.textureId)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        displayRotationHelper.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        displayRotationHelper.updateSessionIfNeeded(session)

        val frame: Frame = try {
            session.update()
        } catch (e: Exception) {
            Log.e("OpenXrRenderer", "Failed to update session", e)
            return
        }

        backgroundRenderer.draw(frame)
        // TODO: Draw objects based on uiState anchors
    }
}