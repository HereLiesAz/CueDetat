package com.hereliesaz.cuedetat.ar

import android.content.Context
import android.opengl.GLES30
import android.opengl.Matrix
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.hereliesaz.cuedetat.ar.renderables.*
import com.hereliesaz.cuedetat.ui.state.BallState
import com.hereliesaz.cuedetat.ui.state.ShotType
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.Renderer
import io.github.sceneview.math.toMat4

class ARCoreRenderer(private val context: Context) : Renderer {

    // Rendering-specific state
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewProjectionMatrix = FloatArray(16)

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

    override fun onSurfaceCreated() {
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

    override fun onSurfaceChanged(width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onSessionCreated(session: Session) {}

    override fun onFrame(session: Session, frame: Frame) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        backgroundRenderer.draw(frame)

        val camera = frame.camera
        camera.getViewMatrix(viewMatrix, 0)
        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        tablePose?.let {
            val tableModelMatrix = it.toMat4().toFloatArray()
            drawTable(tableModelMatrix)

            val currentCueState = cueBallState
            val currentObjectState = objectBallState

            currentCueState?.let { cue ->
                drawBall(cueBall, cue.pose.translation.toF3(), tableModelMatrix, selectedBallId == 0)
            }

            currentObjectState?.let { obj ->
                drawBall(objectBall, obj.pose.translation.toF3(), tableModelMatrix, selectedBallId == 1)
            }

            if (currentCueState != null && currentObjectState != null) {
                val shotData = when (shotType) {
                    ShotType.CUT -> ShotVisualization.calculateCutShot(currentCueState.pose, currentObjectState.pose)
                    ShotType.BANK -> ShotVisualization.calculateBankShot(currentCueState.pose, currentObjectState.pose)
                    else -> null
                }
                shotData?.let { drawShotVisualization(it, tableModelMatrix) }
            }
        }
    }

    private fun drawShotVisualization(shotData: ShotData, tableModelMatrix: FloatArray) {
        shotData.ghostBallPose?.let {
            drawBall(ghostBall, it.translation.toF3(), tableModelMatrix)
        }
        if (shotData.pocketingLinePoints.size == 2) {
            drawLine(pocketingLine, shotData.pocketingLinePoints[0], shotData.pocketingLinePoints[1], tableModelMatrix)
        }
        if (shotData.tangentLinePoints.size == 2) {
            drawLine(tangentLine, shotData.tangentLinePoints[0], shotData.tangentLinePoints[1], tableModelMatrix)
        }
        if (shotData.cueBallPathPoints.size == 2) {
            drawLine(cueBallPathLine, shotData.cueBallPathPoints[0], shotData.cueBallPathPoints[1], tableModelMatrix)
        }
    }

    private fun drawTable(modelMatrix: FloatArray) {
        table.draw(viewProjectionMatrix, modelMatrix)
    }

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
        val lineModelMatrix = tableMatrix.clone()
        line.update(start, end)
        line.draw(viewProjectionMatrix, lineModelMatrix)
    }
}
