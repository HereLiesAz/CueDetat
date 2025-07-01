package com.hereliesaz.cuedetat.ar.jetpack

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.google.ar.core.*
import com.hereliesaz.cuedetat.ar.ARConstants
import com.hereliesaz.cuedetat.ar.jetpack.helpers.DisplayRotationHelper
import com.hereliesaz.cuedetat.ar.jetpack.rendering.*
import com.hereliesaz.cuedetat.ui.state.UiState
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ArRenderer(
    private val session: Session,
    private val displayRotationHelper: DisplayRotationHelper,
    private val onTrackingStateChanged: (TrackingState, TrackingFailureReason?) -> Unit
) : GLSurfaceView.Renderer {

    private var uiState: UiState = UiState()

    private val backgroundRenderer = BackgroundRenderer()
    private val tableRenderer = ObjectRenderer()
    private val cueBallRenderer = SphereRenderer()
    private val objectBallRenderer = SphereRenderer()
    private val shotLineRenderer = LineRenderer()
    private val objectBallPathRenderer = LineRenderer()
    private val planeMarkerRenderer = SphereRenderer()

    private var lastTrackedState: TrackingState? = null
    private var lastFailureReason: TrackingFailureReason? = null

    fun updateState(newState: UiState) {
        this.uiState = newState
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        backgroundRenderer.createOnGlThread()
        tableRenderer.createOnGlThread()
        cueBallRenderer.createOnGlThread()
        objectBallRenderer.createOnGlThread()
        shotLineRenderer.createOnGlThread()
        objectBallPathRenderer.createOnGlThread()
        planeMarkerRenderer.createOnGlThread()
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
        } catch (t: Throwable) {
            Log.e("ArRenderer", "Exception on ArCore session update", t)
            return
        }

        val camera = frame.camera

        val currentTrackingState = camera.trackingState
        val currentFailureReason = if (currentTrackingState == TrackingState.PAUSED) camera.trackingFailureReason else null
        if (currentTrackingState != lastTrackedState || currentFailureReason != lastFailureReason) {
            onTrackingStateChanged.invoke(currentTrackingState, currentFailureReason)
            lastTrackedState = currentTrackingState
            lastFailureReason = currentFailureReason
        }

        backgroundRenderer.draw(frame)

        if (camera.trackingState == TrackingState.PAUSED) {
            return
        }

        val projmtx = FloatArray(16)
        camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f)
        val viewmtx = FloatArray(16)
        camera.getViewMatrix(viewmtx, 0)

        if (uiState.table == null) {
            val planes = session.getAllTrackables(Plane::class.java)
            for (plane in planes) {
                if (plane.trackingState == TrackingState.TRACKING && plane.subsumedBy == null) {
                    val modelMatrix = FloatArray(16)
                    plane.centerPose.toMatrix(modelMatrix, 0)
                    Matrix.scaleM(modelMatrix, 0, 0.02f, 0.02f, 0.02f)
                    planeMarkerRenderer.draw(
                        modelMatrix = modelMatrix,
                        viewMatrix = viewmtx,
                        projectionMatrix = projmtx,
                        color = floatArrayOf(0.0f, 0.5f, 1.0f, 0.5f)
                    )
                }
            }
        }

        val ballScale = ARConstants.BALL_RADIUS * 2

        uiState.table?.let { tableAnchor ->
            if (tableAnchor.trackingState != TrackingState.TRACKING) return@let
            val tableModelMatrix = FloatArray(16)
            tableAnchor.pose.toMatrix(tableModelMatrix, 0)
            val tableWidth = ARConstants.TABLE_WIDTH
            val tableDepth = ARConstants.TABLE_DEPTH
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
                color = floatArrayOf(1.0f, 1.0f, 1.0f, 0.8f)
            )
            val direction = FloatArray(3).apply {
                this[0] = objectBallPosition[0] - cueBallPosition[0]
                this[1] = objectBallPosition[1] - cueBallPosition[1]
                this[2] = objectBallPosition[2] - cueBallPosition[2]
            }
            val length = Matrix.length(direction[0], direction[1], direction[2])
            if (length > 0) {
                direction[0] /= length
                direction[1] /= length
                direction[2] /= length
            }
            val endPoint = FloatArray(3).apply {
                this[0] = objectBallPosition[0] + direction[0] * 2.0f
                this[1] = objectBallPosition[1]
                this[2] = objectBallPosition[2] + direction[2] * 2.0f
            }
            objectBallPathRenderer.updateLineVertices(objectBallPosition, endPoint)
            objectBallPathRenderer.draw(
                viewMatrix = viewmtx,
                projectionMatrix = projmtx,
                color = floatArrayOf(1.0f, 0.84f, 0.0f, 0.8f)
            )
        }

        uiState.cueBall?.let { anchor ->
            if (anchor.trackingState != TrackingState.TRACKING) return@let
            val modelMatrix = FloatArray(16)
            anchor.pose.toMatrix(modelMatrix, 0)
            Matrix.scaleM(modelMatrix, 0, ballScale, ballScale, ballScale)

            cueBallRenderer.draw(
                modelMatrix = modelMatrix,
                viewMatrix = viewmtx,
                projectionMatrix = projmtx,
                color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
            )
        }

        uiState.objectBall?.let { anchor ->
            if (anchor.trackingState != TrackingState.TRACKING) return@let
            val modelMatrix = FloatArray(16)
            anchor.pose.toMatrix(modelMatrix, 0)
            Matrix.scaleM(modelMatrix, 0, ballScale, ballScale, ballScale)

            objectBallRenderer.draw(
                modelMatrix = modelMatrix,
                viewMatrix = viewmtx,
                projectionMatrix = projmtx,
                color = floatArrayOf(1.0f, 0.84f, 0.0f, 1.0f)
            )
        }
    }
}