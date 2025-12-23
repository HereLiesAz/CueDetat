package com.hereliesaz.cuedetat.ar.jetpack.rendering

import android.opengl.GLES20
import android.opengl.Matrix
import com.hereliesaz.cuedetat.ar.jetpack.rendering.ShaderUtil.checkGLError
import com.hereliesaz.cuedetat.ar.jetpack.rendering.ShaderUtil.loadGLShader
import com.hereliesaz.cuedetat.ar.jetpack.rendering.ObjectRenderer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Renders a line in 3D space.
 */
class LineRenderer {
    private val TAG = LineRenderer::class.java.simpleName

    // Use the same simple shaders as the ObjectRenderer.
    private val VERTEX_SHADER_NAME = "shaders/object.vert"
    private val FRAGMENT_SHADER_NAME = "shaders/object.frag"

    private var program = 0
    private var positionAttrib = 0
    private var modelViewProjectionUniform = 0
    private var colorUniform = 0

    private var vertexBuffer: FloatBuffer? = null
    private val modelViewProjectionMatrix = FloatArray(16)

    fun createOnGlThread() {
        val vertexShader = loadGLShader(TAG, GLES20.GL_VERTEX_SHADER, ObjectRenderer.VERTEX_SHADER)
        val fragmentShader = loadGLShader(TAG, GLES20.GL_FRAGMENT_SHADER, ObjectRenderer.FRAGMENT_SHADER)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        GLES20.glUseProgram(program)

        checkGLError(TAG, "Program creation")

        positionAttrib = GLES20.glGetAttribLocation(program, "a_Position")
        modelViewProjectionUniform = GLES20.glGetUniformLocation(program, "u_ModelViewProjection")
        colorUniform = GLES20.glGetUniformLocation(program, "u_Color")

        checkGLError(TAG, "Program parameters")
    }

    /**
     * Updates the vertex buffer data for the line.
     * @param start The starting point of the line in world coordinates.
     * @param end The ending point of the line in world coordinates.
     */
    fun updateLineVertices(start: FloatArray, end: FloatArray) {
        val lineVertices = floatArrayOf(
            start[0], start[1], start[2],
            end[0], end[1], end[2]
        )
        val bb = ByteBuffer.allocateDirect(lineVertices.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer?.put(lineVertices)
        vertexBuffer?.position(0)
    }

    fun draw(
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        color: FloatArray,
        lineWidth: Float = 5.0f
    ) {
        if (vertexBuffer == null) return

        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        GLES20.glUseProgram(program)
        GLES20.glLineWidth(lineWidth)

        GLES20.glUniform4fv(colorUniform, 1, color, 0)
        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0)

        GLES20.glEnableVertexAttribArray(positionAttrib)
        GLES20.glVertexAttribPointer(positionAttrib, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2)

        GLES20.glDisableVertexAttribArray(positionAttrib)

        checkGLError(TAG, "Drawing line")
    }
}