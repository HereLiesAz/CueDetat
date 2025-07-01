package com.hereliesaz.cuedetat.ar.jetpack.rendering

import android.opengl.GLES20
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState // Import added
import com.hereliesaz.cuedetat.ar.jetpack.rendering.ShaderUtil.checkGLError
import com.hereliesaz.cuedetat.ar.jetpack.rendering.ShaderUtil.loadGLShader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class PlaneRenderer {
    private val TAG = PlaneRenderer::class.java.simpleName

    private var program = 0
    private var positionAttrib = 0
    private var modelViewProjectionUniform = 0
    private var colorUniform = 0
    private var planeCenterUniform = 0

    private var vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(1000 * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private var indexBuffer: ShortBuffer =
        ByteBuffer.allocateDirect(1000 * 3 * 2).order(ByteOrder.nativeOrder()).asShortBuffer()

    fun createOnGlThread() {
        val vertexShader = loadGLShader(TAG, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = loadGLShader(TAG, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        GLES20.glUseProgram(program)
        checkGLError(TAG, "Program creation")

        positionAttrib = GLES20.glGetAttribLocation(program, "a_Position")
        colorUniform = GLES20.glGetUniformLocation(program, "u_Color")
        modelViewProjectionUniform = GLES20.glGetUniformLocation(program, "u_ModelViewProjection")
        planeCenterUniform = GLES20.glGetUniformLocation(program, "u_PlaneCenter")
        checkGLError(TAG, "Program parameters")
    }

    private fun updatePlane(plane: Plane) {
        val polygon = plane.polygon
        if (polygon.remaining() == 0) return

        vertexBuffer.rewind()
        vertexBuffer.put(polygon)
        vertexBuffer.position(0)

        // Create a simple fan triangulation
        indexBuffer.rewind()
        val numVertices = polygon.remaining() / 2
        for (i in 1 until numVertices - 1) {
            indexBuffer.put(0.toShort())
            indexBuffer.put(i.toShort())
            indexBuffer.put((i + 1).toShort())
        }
        indexBuffer.position(0)
    }


    fun draw(
        pose: Pose,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray
    ) {
        val modelMatrix = FloatArray(16)
        pose.toMatrix(modelMatrix, 0)

        val modelViewProjectionMatrix = FloatArray(16)
        android.opengl.Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        android.opengl.Matrix.multiplyMM(modelViewProjectionMatrix, 0, modelViewProjectionMatrix, 0, modelMatrix, 0)


        GLES20.glUseProgram(program)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        GLES20.glVertexAttribPointer(
            positionAttrib, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer
        )
        GLES20.glUniform4f(colorUniform, 0.0f, 0.5f, 1.0f, 0.25f) // Light blue, 25% transparent
        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0)

        GLES20.glEnableVertexAttribArray(positionAttrib)
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES, indexBuffer.remaining(), GLES20.GL_UNSIGNED_SHORT, indexBuffer
        )
        GLES20.glDisableVertexAttribArray(positionAttrib)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    fun drawPlanes(
        planes: Collection<Plane>,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray
    ) {
        for (plane in planes) {
            // Corrected this line
            if (plane.trackingState == TrackingState.TRACKING && plane.subsumedBy == null) {
                updatePlane(plane)
                draw(plane.centerPose, viewMatrix, projectionMatrix)
            }
        }
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 u_ModelViewProjection;
            attribute vec2 a_Position;
            void main() {
                gl_Position = u_ModelViewProjection * vec4(a_Position.x, 0.0, a_Position.y, 1.0);
            }
            """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                gl_FragColor = u_Color;
            }
            """
    }
}