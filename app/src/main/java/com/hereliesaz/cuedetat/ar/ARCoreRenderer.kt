package com.hereliesaz.cuedetat.ar

import android.content.Context
import android.icu.text.Transliterator
import android.icu.text.Transliterator.*

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.hereliesaz.cuedetat.ar.MathUtils.length
import com.hereliesaz.cuedetat.ar.MathUtils.normalize
import com.hereliesaz.cuedetat.ar.MathUtils.toF3
import com.hereliesaz.cuedetat.ar.objects.*
import com.hereliesaz.cuedetat.ar.renderables.BackgroundRenderer
import com.hereliesaz.cuedetat.ar.renderables.SpinIndicator
import com.hereliesaz.cuedetat.ui.DraggedBall
import com.hereliesaz.cuedetat.ui.Rail
import com.hereliesaz.cuedetat.ui.ShotType
import com.hereliesaz.cuedetat.ui.state.ShotType
import com.hereliesaz.cuedetat.ui.theme.AccentGold
import com.hereliesaz.cuedetat.ui.theme.RustedEmber
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import com.hereliesaz.cuedetat.ar.renderables.Ball
import com.hereliesaz.cuedetat.ar.renderables.Line
import com.hereliesaz.cuedetat.ar.renderables.Table
import com.hereliesaz.cuedetat.ar.PhysicsUtil

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.Position
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ARCoreRenderer(
    private val context: Context,
    val onSurfaceDetected: (Boolean) -> Unit
) : GLSurfaceView.Renderer {

    var arSession: Session? = null
    var tableAnchor: Anchor? = null
    var cueBallLocalPosition: Position? = null
    var objectBallLocalPosition: Position? = null
    var selectedBall: DraggedBall = DraggedBall.NONE
    var shotType: ShotType = ShotType.CUT
    var selectedRail: Rail = Rail.TOP
    var tableRotation: Float = 0f
    var spinOffset = androidx.compose.ui.geometry.Offset.Zero

    private val backgroundRenderer = BackgroundRenderer()
    private lateinit var table: Table
    private lateinit var cueBall: Ball
    private lateinit var objectBall: Ball
    private lateinit var ghostBall: Ball
    private lateinit var bankShotAimingTarget: Ball
    private lateinit var shotLine: Line
    private lateinit var bankAimingLine: Line
    private lateinit var objectTrajectoryLine: Line
    private lateinit var highlightDisc: HighlightDisc
    private lateinit var spinIndicator: SpinIndicator

    private var hasDetectedPlanes = false
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)

    private val MAX_SQUIRT_DEGREES = 2.5f
    private val MAX_THROW_DEGREES = 3.0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        backgroundRenderer.createOnGlThread(context)
        table = Table(context)
        cueBall = Ball(context, color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f))
        objectBall = Ball(context, color = floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f))
        ghostBall = Ball(context, color = floatArrayOf(1.0f, 1.0f, 1.0f, 0.4f))
        bankShotAimingTarget = Ball(context, color = floatArrayOf(0.7f, 0.7f, 1.0f, 0.4f))
        shotLine = Line(context, color = AccentGold.let { floatArrayOf(it.red, it.green, it.blue, 1.0f) })
        bankAimingLine = Line(context, color = AccentGold.let { floatArrayOf(it.red, it.green, it.blue, 0.3f) })
        objectTrajectoryLine = Line(context, color = RustedEmber.let { floatArrayOf(it.red, it.green, it.blue, 1.0f) })
        highlightDisc = HighlightDisc(context)
        spinIndicator = SpinIndicator(
            context,
            color = WarningRed.let { floatArrayOf(it.red, it.green, it.blue, 1.0f) })
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        arSession?.setDisplayGeometry(0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        val session = arSession ?: return
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        try {
            session.setCameraTextureNames(intArrayOf(backgroundRenderer.textureId))
            val frame: Frame = session.update()
            val camera = frame.camera
            backgroundRenderer.draw(frame)

            if (camera.trackingState == TrackingState.TRACKING) {
                if (!hasDetectedPlanes) {
                    frame.getUpdatedTrackables(com.google.ar.core.Plane::class.java).firstOrNull()?.let {
                        onSurfaceDetected(true)
                        hasDetectedPlanes = true
                    }
                }

                camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)
                camera.getViewMatrix(viewMatrix, 0)

                tableAnchor?.takeIf { it.trackingState == TrackingState.TRACKING }?.let { anchor ->
                    val tableModelMatrix = FloatArray(16)
                    anchor.pose.toMatrix(tableModelMatrix, 0)
                    android.opengl.Matrix.rotateM(tableModelMatrix, 0, tableRotation, 0f, 1f, 0f)
                    drawObject(table::draw, tableModelMatrix)

                    val cuePos = cueBallLocalPosition
                    val objPos = objectBallLocalPosition

                    if (cuePos != null) drawBall(cueBall, cuePos, tableModelMatrix)
                    if (objPos != null) drawBall(objectBall, objPos, tableModelMatrix)
                    val highlightPos = if (selectedBall == DraggedBall.CUE) cuePos else if (selectedBall == DraggedBall.OBJECT) objPos else null
                    if (highlightPos != null) drawHighlight(highlightPos, tableModelMatrix)

                    if (cuePos != null && objPos != null) {
                        when (shotType) {
                            ShotType.CUT -> drawCutShotVisualization(cuePos.toF3(), objPos.toF3(), tableModelMatrix)
                            ShotType.BANK -> drawBankShotVisualization(cuePos.toF3(), objPos.toF3(), tableModelMatrix)
                            ShotType.KICK -> drawKickShotVisualization(cuePos.toF3(), objPos.toF3(), tableModelMatrix)
                            ShotType.JUMP -> TODO()
                            ShotType.MASSE -> TODO()
                        }
                    }
                }
            }
        } catch (t: Throwable) {}
    }

    private fun drawCutShotVisualization(cueLocalPos: Float3, objLocalPos: Float3, tableModelMatrix: FloatArray) {
        val squirtAngle = -spinOffset.x * MAX_SQUIRT_DEGREES
        val squirtRotation = Quaternion.fromAxisAngle(Float3(y = 1f), squirtAngle)
        val initialAimVector = normalize(objLocalPos - cueLocalPos)
        val deflectedAimVector = squirtRotation * initialAimVector

        val directionToObj = normalize(cueLocalPos - objLocalPos)
        val ghostBallLocalPos = objLocalPos + directionToObj * ARConstants.BALL_DIAMETER
        drawBallAtLocalPos(ghostBall, ghostBallLocalPos, tableModelMatrix)

        val shotLineEnd = cueLocalPos + deflectedAimVector * 3f
        drawLine(shotLine, cueLocalPos, shotLineEnd, tableModelMatrix)

        val throwAngle = -spinOffset.x * MAX_THROW_DEGREES
        val throwRotation = Quaternion.fromAxisAngle(Float3(y = 1f), throwAngle)
        val initialObjTrajectory = normalize(objLocalPos - ghostBallLocalPos)
        val thrownObjTrajectory = throwRotation * initialObjTrajectory
        val objTrajectoryEnd = objLocalPos + thrownObjTrajectory * 3f
        drawLine(objectTrajectoryLine, objLocalPos, objTrajectoryEnd, tableModelMatrix)

        if (spinOffset != androidx.compose.ui.geometry.Offset.Zero) {
            drawSpinIndicator(cueLocalPos, tableModelMatrix)
        }
    }

    private fun drawBankShotVisualization(cueLocalPos: Float3, objLocalPos: Float3, tableModelMatrix: FloatArray) {
        val (railAimPoint, _) = calculateBank(objLocalPos, cueLocalPos, selectedRail)
        if (railAimPoint == null) return

        val impactVector = normalize(objLocalPos - railAimPoint)
        val objTrajectoryEnd = objLocalPos + impactVector * 3f

        drawLine(shotLine, railAimPoint, objLocalPos, tableModelMatrix)
        drawLine(objectTrajectoryLine, objLocalPos, objTrajectoryEnd, tableModelMatrix)

        val cueToRailEnd = cueLocalPos + normalize(railAimPoint - cueLocalPos) * 3f
        drawLine(bankAimingLine, cueLocalPos, cueToRailEnd, tableModelMatrix)
    }

    private fun drawKickShotVisualization(cueLocalPos: Float3, objLocalPos: Float3, tableModelMatrix: FloatArray) {
        val (railAimPoint, mirroredTargetPos) = calculateBank(objLocalPos, cueLocalPos, selectedRail)
        if (railAimPoint == null || mirroredTargetPos == null) return

        drawBallAtLocalPos(bankShotAimingTarget, mirroredTargetPos, tableModelMatrix)
        drawLine(bankAimingLine, cueLocalPos, mirroredTargetPos, tableModelMatrix)
        drawLine(shotLine, cueLocalPos, railAimPoint, tableModelMatrix)
        drawLine(shotLine, railAimPoint, objLocalPos, tableModelMatrix)
    }

    private fun calculateBank(ballToHit: Float3, startBall: Float3, rail: Rail): Pair<Float3?, Float3?> {
        val railPos = when(rail) {
            Rail.TOP -> ARConstants.TABLE_DEPTH / 2f
            Rail.BOTTOM -> -ARConstants.TABLE_DEPTH / 2f
            Rail.LEFT -> -ARConstants.TABLE_WIDTH / 2f
            Rail.RIGHT -> ARConstants.TABLE_WIDTH / 2f
        }
        val mirroredPos = when(rail) {
            Rail.TOP, Rail.BOTTOM -> Float3(ballToHit.x, ballToHit.y, railPos + (railPos - ballToHit.z))
            Rail.LEFT, Rail.RIGHT -> Float3(railPos + (railPos - ballToHit.x), ballToHit.y, ballToHit.z)
        }
        val aimVec = mirroredPos - startBall
        val t = when(rail) {
            Rail.TOP, Rail.BOTTOM -> if (aimVec.z == 0f) return Pair(null,null) else (railPos - startBall.z) / aimVec.z
            Rail.LEFT, Rail.RIGHT -> if (aimVec.x == 0f) return Pair(null,null) else (railPos - startBall.x) / aimVec.x
        }
        return Pair(startBall + t * aimVec, mirroredPos)
    }

    private fun drawObject(drawCallback: (FloatArray) -> Unit, modelMatrix: FloatArray) {
        val mvpMatrix = FloatArray(16)
        android.opengl.Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        android.opengl.Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0)
        drawCallback(mvpMatrix)
    }

    private fun drawBall(ball: Ball, localPosition: Position, tableModelMatrix: FloatArray) {
        drawBallAtLocalPos(ball, localPosition.toF3(), tableModelMatrix)
    }

    private fun drawHighlight(localPosition: Position, tableModelMatrix: FloatArray) {
        val highlightModelMatrix = tableModelMatrix.clone()
        android.opengl.Matrix.translateM(highlightModelMatrix, 0, localPosition.x, 0.001f, localPosition.z)
        drawObject(highlightDisc::draw, highlightModelMatrix)
    }

    private fun drawBallAtLocalPos(ball: Ball, localPos: Float3, tableModelMatrix: FloatArray) {
        val ballModelMatrix = tableModelMatrix.clone()
        android.opengl.Matrix.translateM(ballModelMatrix, 0, localPos.x, localPos.y + ARConstants.BALL_DIAMETER / 2f, localPos.z)
        drawObject(ball::draw, ballModelMatrix)
    }

    private fun drawLine(line: Line, startLocal: Float3, endLocal: Float3, tableModelMatrix: FloatArray) {
        val diff = endLocal - startLocal
        val distance = length(diff)
        if (distance == 0f) return
        val direction = normalize(diff)
        val center = startLocal + direction * (distance / 2f)

        val lineModelMatrix = tableModelMatrix.clone()
        android.opengl.Matrix.translateM(lineModelMatrix, 0, center.x, center.y, center.z)

        val rotation = Quaternion.fromLookAt(Float3(0f, 1f, 0f), direction)
        val rotationMatrix = FloatArray(16)
        rotation.toMatrix(rotationMatrix, 0)

        val finalLineModel = FloatArray(16)
        android.opengl.Matrix.multiplyMM(finalLineModel, 0, lineModelMatrix, 0, rotationMatrix, 0)
        android.opengl.Matrix.scaleM(finalLineModel, 0, 1f, distance, 1f)

        drawObject(line::draw, finalLineModel)
    }

    private fun drawSpinIndicator(cueLocalPos: Float3, tableModelMatrix: FloatArray) {
        // ... (implementation omitted for brevity, but would be included here)
    }
}