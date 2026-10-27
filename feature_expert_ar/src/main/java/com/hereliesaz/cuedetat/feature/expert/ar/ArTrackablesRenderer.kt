package com.hereliesaz.cuedetat.feature.expert.ar

import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

private const val TAG = "ArTrackablesRenderer"

/**
 * Draws ARCore's setup feedback over the camera feed: detected horizontal planes (translucent
 * fill plus outline) and the feature point cloud. Monochrome, so it reads as scaffolding, not
 * as part of the table overlay.
 *
 * The caller stops drawing it once the table is locked ([ArTableSession.isTableLocked]); it is
 * setup-only feedback and would otherwise sit under the 2D overlay for the whole game.
 *
 * GL thread only: [createOnGlThread] in onSurfaceCreated, [draw] each frame after the background.
 */
class ArTrackablesRenderer {

    private var program = -1
    private var positionHandle = -1
    private var mvpHandle = -1
    private var colorHandle = -1
    private var pointSizeHandle = -1

    private val view = FloatArray(16)
    private val proj = FloatArray(16)
    private val viewProj = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)

    // Plane polygons arrive as (x, z) pairs in plane space; expanded to (x, 0, z) here.
    private var planeBuffer: FloatBuffer = allocate(256 * 3)

    private val vertSrc = """
        uniform mat4 u_Mvp;
        uniform float u_PointSize;
        attribute vec4 a_Position;
        void main() {
            gl_Position = u_Mvp * vec4(a_Position.xyz, 1.0);
            gl_PointSize = u_PointSize;
        }
    """.trimIndent()

    private val fragSrc = """
        precision mediump float;
        uniform vec4 u_Color;
        void main() {
            gl_FragColor = u_Color;
        }
    """.trimIndent()

    /** Must be called on the GL thread during onSurfaceCreated. */
    fun createOnGlThread() {
        val vert = compileShader(GLES20.GL_VERTEX_SHADER, vertSrc)
        val frag = compileShader(GLES20.GL_FRAGMENT_SHADER, fragSrc)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vert)
            GLES20.glAttachShader(it, frag)
            GLES20.glLinkProgram(it)
            val status = IntArray(1)
            GLES20.glGetProgramiv(it, GLES20.GL_LINK_STATUS, status, 0)
            if (status[0] != GLES20.GL_TRUE) Log.e(TAG, "Program link failed: ${GLES20.glGetProgramInfoLog(it)}")
        }
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        mvpHandle = GLES20.glGetUniformLocation(program, "u_Mvp")
        colorHandle = GLES20.glGetUniformLocation(program, "u_Color")
        pointSizeHandle = GLES20.glGetUniformLocation(program, "u_PointSize")
    }

    /** Draws planes then points. Call only while the camera is TRACKING. */
    fun draw(frame: Frame, session: Session) {
        if (program < 0) return
        val camera: Camera = frame.camera
        camera.getViewMatrix(view, 0)
        camera.getProjectionMatrix(proj, 0, 0.05f, 100f)
        Matrix.multiplyMM(viewProj, 0, proj, 0, view, 0)

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glUseProgram(program)
        GLES20.glEnableVertexAttribArray(positionHandle)

        drawPlanes(session)
        drawPointCloud(frame)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    private fun drawPlanes(session: Session) {
        GLES20.glUniform1f(pointSizeHandle, 1f)
        for (plane in session.getAllTrackables(Plane::class.java)) {
            // Subsumed planes have been merged into another; drawing both doubles the fill.
            if (plane.trackingState != TrackingState.TRACKING || plane.subsumedBy != null) continue
            val polygon = plane.polygon ?: continue
            val count = polygon.limit() / 2
            if (count < 3) continue

            if (planeBuffer.capacity() < count * 3) planeBuffer = allocate(count * 3)
            planeBuffer.clear()
            polygon.rewind()
            repeat(count) {
                planeBuffer.put(polygon.get()).put(0f).put(polygon.get())
            }
            planeBuffer.flip()

            plane.centerPose.toMatrix(model, 0)
            Matrix.multiplyMM(mvp, 0, viewProj, 0, model, 0)
            GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, planeBuffer)

            GLES20.glUniform4f(colorHandle, 1f, 1f, 1f, 0.12f)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, count)
            GLES20.glLineWidth(2f)
            GLES20.glUniform4f(colorHandle, 1f, 1f, 1f, 0.55f)
            GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, count)
        }
    }

    private fun drawPointCloud(frame: Frame) {
        frame.acquirePointCloud().use { cloud ->
            val points = cloud.points // x, y, z, confidence per point
            val count = points.limit() / 4
            if (count == 0) return
            points.rewind()
            GLES20.glUniformMatrix4fv(mvpHandle, 1, false, viewProj, 0)
            GLES20.glUniform1f(pointSizeHandle, 6f)
            GLES20.glUniform4f(colorHandle, 1f, 1f, 1f, 0.85f)
            // Stride 16 skips the confidence float; .xyz in the shader ignores w.
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 16, points)
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, count)
        }
    }

    private fun allocate(floats: Int): FloatBuffer =
        ByteBuffer.allocateDirect(floats * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

    private fun compileShader(type: Int, src: String): Int =
        GLES20.glCreateShader(type).also {
            GLES20.glShaderSource(it, src)
            GLES20.glCompileShader(it)
            val status = IntArray(1)
            GLES20.glGetShaderiv(it, GLES20.GL_COMPILE_STATUS, status, 0)
            if (status[0] != GLES20.GL_TRUE) Log.e(TAG, "Shader compile failed: ${GLES20.glGetShaderInfoLog(it)}")
        }
}
