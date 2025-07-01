/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hereliesaz.cuedetat.ar.jetpack

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.hereliesaz.cuedetat.MainActivity
import com.hereliesaz.cuedetat.ar.jetpack.helpers.DisplayRotationHelper
import com.hereliesaz.cuedetat.ar.jetpack.rendering.BackgroundRenderer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Renders the camera feed and AR objects.
 */
class ArRenderer(
    val activity: MainActivity,
    private val session: Session,
    private val displayRotationHelper: DisplayRotationHelper
) : GLSurfaceView.Renderer {

    private val backgroundRenderer = BackgroundRenderer()
    // Add other renderers here, e.g., for objects, planes, etc.

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        backgroundRenderer.createOnGlThread()
        // Initialize other renderers here.
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        displayRotationHelper.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Update ARCore session
        displayRotationHelper.updateSessionIfNeeded(session)

        val frame: Frame = try {
            session.update()
        } catch (e: Exception) {
            // Handle exceptions, e.g., CameraNotAvailableException
            return
        }

        // Draw background
        backgroundRenderer.draw(frame)

        // If not tracking, don't draw 3D objects.
        val camera = frame.camera
        if (camera.trackingState != com.google.ar.core.TrackingState.TRACKING) {
            return
        }

        // Get projection matrix.
        val projmtx = FloatArray(16)
        camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f)

        // Get view matrix.
        val viewmtx = FloatArray(16)
        camera.getViewMatrix(viewmtx, 0)

        // Draw virtual objects.
        // e.g., objectRenderer.draw(...)
    }
}
