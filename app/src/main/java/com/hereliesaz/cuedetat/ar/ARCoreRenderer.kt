package com.hereliesaz.cuedetat.ar

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import com.google.ar.core.*
import com.hereliesaz.cuedetat.ar.renderables.*
import com.hereliesaz.cuedetat.ui.state.BallState
import com.hereliesaz.cuedetat.ui.state.ShotType
import dev.romainguy.kotlin.math.Float3
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ARCoreRenderer(
    private val context: Context,
    private val onTap: (HitResult) -> Unit
) : GLSurfaceView.Renderer {

    var arSession: Session? = null
    var onPlaneDetected: (() -> Unit)? = null

    // Rendering-specific state
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewProjectionMatrix = FloatArray(16)
    private var viewportWidth = 0
    private var viewportHeight = 0
    private var tapToProcess: MotionEvent? = null

    // Renderables
    private lateinit var backgroundRenderer: BackgroundRenderer
    private lateinit var table: Table
    private lateinit var cueBall: Ball
    private lateinit var objectBall: Ball
    private lateinit var ghostBall: Ball
    private lateinit var pocketingLine: Line
    private lateinit var tangentLine: Line
    private lateinit var cueBallPathLine: Line
    private lateinit var selectionDisc: HighlightDisc

    // Public properties updated from the UI State
    var tablePose: Pose? = null
    var cueBallState: BallState? = null
    var objectBallState: BallState? = null
    var selectedBallId: Int? = null
    var shotType: ShotType = ShotType.CUT
    var spinOffset: Float3 = Float3()
    var cueElevation: Float = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        backgroundRenderer = BackgroundRenderer()
        backgroundRenderer.createOnGlThread()
        table = Table(context)
        cueBall = Ball(context, ArConstants.CUE_BALL_COLOR)
        objectBall = Ball(context, ArConstants.OBJECT_BALL_COLOR)
        ghostBall = Ball(context, ArConstants.GHOST_BALL_COLOR)
        pocketingLine = Line(context, ArConstants.POCKET_LINE_COLOR)
        tangentLine = Line(context, ArConstants.TANGENT_LINE_COLOR)
        cueBallPathLine = Line(context, ArConstants.CUE_PATH_COLOR)
        selectionDisc = HighlightDisc(context, ArConstants.SELECTION_COLOR)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        viewportWidth = width
        viewportHeight = height
        arSession?.setDisplayGeometry(0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        val session = arSession ?: return
        session.setCameraTextureName(backgroundRenderer.textureId)

        val frame = session.update()
        if (frame.timestamp == 0L) return

        handleTap(frame)

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        backgroundRenderer.draw(frame)

        val camera = frame.camera
        if (camera.trackingState != TrackingState.TRACKING) return

        camera.getViewMatrix(viewMatrix, 0)
        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Plane detection notification
        if (tablePose == null && frame.getUpdatedTrackables(Plane::class.java).any { it.trackingState == TrackingState.TRACKING }) {
            onPlaneDetected?.invoke()
        }

        tablePose?.let {
            val tableModelMatrix = it.toM4()
            drawTable(tableModelMatrix)

            val currentCueState = cueBallState
            val currentObjectState = objectBallState

            currentCueState?.let { cue -> drawBall(cueBall, cue.pose.translation.toF3(), tableModelMatrix, selectedBallId == 0) }
            currentObjectState?.let { obj -> drawBall(objectBall, obj.pose.translation.toF3(), tableModelMatrix, selectedBallId == 1) }

            if (currentCueState != null && currentObjectState != null) {
                val shotData = ShotVisualization.calculateCutShot(currentCueState.pose, currentObjectState.pose)
                drawShotVisualization(shotData, tableModelMatrix)
            }
        }
    }

    fun queueTap(e: MotionEvent) {
        tapToProcess = e
    }

    private fun handleTap(frame: Frame) {
        val tap = tapToProcess ?: return
        tapToProcess = null
        if (frame.camera.trackingState == TrackingState.TRACKING) {
            frame.hitTest(tap).firstOrNull()?.let { hitResult ->
                if(hitResult.trackable is Plane) {
                    onTap(hitResult)
                }
            }
        }
    }

    private fun drawShotVisualization(shotData: ShotData, tableModelMatrix: FloatArray) {
        shotData.ghostBallPose?.let { drawBall(ghostBall, it.translation.toF3(), tableModelMatrix) }
        if (shotData.pocketingLinePoints.size == 2) drawLine(pocketingLine, shotData.pocketingLinePoints[0], shotData.pocketingLinePoints[1], tableModelMatrix)
        if (shotData.tangentLinePoints.size == 2) drawLine(tangentLine, shotData.tangentLinePoints[0], shotData.tangentLinePoints[1], tableModelMatrix)
        if (shotData.cueBallPathPoints.size == 2) drawLine(cueBallPathLine, shotData.cueBallPathPoints[0], shotData.cueBallPathPoints[1], tableModelMatrix)
    }

    private fun drawTable(modelMatrix: FloatArray) = table.draw(viewProjectionMatrix, modelMatrix)

    private fun drawBall(ball: Ball, localPosition: Float3, tableMatrix: FloatArray, isSelected: Boolean = false) {
        val ballModelMatrix = tableMatrix.clone()
        Matrix.translateM(ballModelMatrix, 0, localPosition.x, ArConstants.BALL_RADIUS, localPosition.z)
        ball.draw(viewProjectionMatrix, ballModelMatrix)
        if (isSelected) {
            val discModelMatrix = tableMatrix.clone()
            Matrix.translateM(discModelMatrix, 0, localPosition.x, 0.001f, localPosition.z)
            selectionDisc.draw(viewProjectionMatrix, discModelMatrix)
        }
    }

    private fun drawLine(line: Line, start: Float3, end: Float3, tableMatrix: FloatArray) {
        line.update(start, end)
        line.draw(viewProjectionMatrix, tableMatrix)
    }

    private fun Pose.toM4(): FloatArray = FloatArray(16).apply { this@toM4.toMatrix(this, 0) }
}
