package com.hereliesaz.cuedetat.ar.jetpack

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log // <-- Add this import
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.hereliesaz.cuedetat.MainActivity
import com.hereliesaz.cuedetat.ar.ARConstants
import com.hereliesaz.cuedetat.ar.jetpack.helpers.DisplayRotationHelper
import com.hereliesaz.cuedetat.ar.jetpack.rendering.*
import com.hereliesaz.cuedetat.ui.state.UiState
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ArRenderer(
    val activity: MainActivity,
    private val session: Session,
    private val displayRotationHelper: DisplayRotationHelper,
    private val uiState: UiState
) : GLSurfaceView.Renderer {

    // ... (rest of the class properties are the same)
    private val backgroundRenderer = BackgroundRenderer()
    private val tableRenderer = ObjectRenderer()
    private val cueBallRenderer = SphereRenderer()
    private val objectBallRenderer = SphereRenderer()
    private val shotLineRenderer = LineRenderer()
    private val objectBallPathRenderer = LineRenderer()


    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        backgroundRenderer.createOnGlThread()
        tableRenderer.createOnGlThread()
        cueBallRenderer.createOnGlThread()
        objectBallRenderer.createOnGlThread()
        shotLineRenderer.createOnGlThread()
        objectBallPathRenderer.createOnGlThread()
        session.setCameraTextureName(backgroundRenderer.textureId)
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
            Log.e("ArRenderer", "Failed to update session", e)
            return
        }

        backgroundRenderer.draw(frame)
        val camera = frame.camera

        // *** THIS IS THE DEBUGGING LINE ***
        Log.d("ArRenderer", "Camera Tracking State: ${camera.trackingState}")

        if (camera.trackingState != com.google.ar.core.TrackingState.TRACKING) {
            // If it's not tracking, we print the failure reason to understand why.
            if(camera.trackingState == com.google.ar.core.TrackingState.PAUSED) {
                Log.d("ArRenderer", "Tracking Failure Reason: ${camera.trackingFailureReason}")
            }
            return
        }

        // ... (rest of the drawing logic remains the same)

        val projmtx = FloatArray(16)
        camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f)
        val viewmtx = FloatArray(16)
        camera.getViewMatrix(viewmtx, 0)

        val ballScale = ARConstants.BALL_RADIUS * 2

        uiState.table?.let { tableAnchor ->
            val tableWidth = ARConstants.TABLE_WIDTH
            val tableDepth = ARConstants.TABLE_DEPTH
            val tableModelMatrix = FloatArray(16)
            tableAnchor.pose.toMatrix(tableModelMatrix, 0)
            Matrix.scaleM(tableModelMatrix, 0, tableWidth, ARConstants.TABLE_HEIGHT, tableDepth)

            tableRenderer.draw(
                modelMatrix = tableModelMatrix,
                viewMatrix = viewmtx,
                projectionMatrix = projmtx,
                color = floatArrayOf(0.0f, 0.4f, 0.15f, 1.0f)
            )
        }

        val cueBallPose = uiState.cueBall?.pose
        val objectBallPose = uiState.objectBall?.pose

        if (cueBallPose != null && objectBallPose != null && uiState.isAiming) {
            val cueBallPosition = cueBallPose.translation
            val objectBallPosition = objectBallPose.translation
            shotLineRenderer.updateLineVertices(cueBallPosition, objectBallPosition)
            shotLineRenderer.draw(
                viewMatrix = viewmtx,
                projectionMatrix = projmtx,
                color = floatArrayOf(1.0f, 1.0f, 1.0f, 0.8f) // Translucent white
            )
            val direction = FloatArray(3).apply {
                this[0] = objectBallPosition[0] - cueBallPosition[0]
                this[1] = objectBallPosition[1] - cueBallPosition[1]
                this[2] = objectBallPosition[2] - cueBallPosition[2]
            }
            val length = Matrix.length(direction[0], direction[1], direction[2])
            direction[0] /= length
            direction[1] /= length
            direction[2] /= length
            val endPoint = FloatArray(3).apply {
                this[0] = objectBallPosition[0] + direction[0] * 2.0f
                this[1] = objectBallPosition[1]
                this[2] = objectBallPosition[2] + direction[2] * 2.0f
            }
            objectBallPathRenderer.updateLineVertices(objectBallPosition, endPoint)
            objectBallPathRenderer.draw(
                viewMatrix = viewmtx,
                projectionMatrix = projmtx,
                color = floatArrayOf(1.0f, 0.84f, 0.0f, 0.8f) // Translucent yellow
            )
        }

        cueBallPose?.let { pose ->
            val modelMatrix = FloatArray(16)
            pose.toMatrix(modelMatrix, 0)
            Matrix.scaleM(modelMatrix, 0, ballScale, ballScale, ballScale)

            cueBallRenderer.draw(
                modelMatrix = modelMatrix,
                viewMatrix = viewmtx,
                projectionMatrix = projmtx,
                color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f) // White
            )
        }

        objectBallPose?.let { pose ->
            val modelMatrix = FloatArray(16)
            pose.toMatrix(modelMatrix, 0)
            Matrix.scaleM(modelMatrix, 0, ballScale, ballScale, ballScale)

            objectBallRenderer.draw(
                modelMatrix = modelMatrix,
                viewMatrix = viewmtx,
                projectionMatrix = projmtx,
                color = floatArrayOf(1.0f, 0.84f, 0.0f, 1.0f) // Yellow/Gold
            )
        }
    }
}